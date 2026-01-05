package com.katlehouniversity.ecd.service;

import com.katlehouniversity.ecd.dto.MyUpdatesWebhookPayload;
import com.katlehouniversity.ecd.dto.ParsedEmailNotification;
import com.katlehouniversity.ecd.entity.*;
import com.katlehouniversity.ecd.repository.ChildRepository;
import com.katlehouniversity.ecd.repository.PaymentRepository;
import com.katlehouniversity.ecd.repository.TransactionNotificationRepository;
import com.katlehouniversity.ecd.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for processing webhook notifications asynchronously.
 * Handles parsing, duplicate detection, and automatic payment matching.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookProcessingService {

    private final MyUpdatesEmailParser emailParser;
    private final TransactionNotificationRepository notificationRepository;
    private final TransactionRepository transactionRepository;
    private final ChildRepository childRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Process incoming webhook notification asynchronously
     *
     * @param payload Webhook payload with email details
     */
    @Async
    @Transactional
    public void processNotificationAsync(MyUpdatesWebhookPayload payload) {
        log.info("Processing webhook notification asynchronously: {}", payload.getEmailId());

        try {
            // Parse email body
            ParsedEmailNotification parsed = emailParser.parseMyUpdatesEmail(
                payload.getBody(),
                payload.getSubject()
            );

            if (!parsed.isValid()) {
                log.error("Invalid notification format: {}", parsed.getErrorMessage());
                saveFailedNotification(payload, parsed.getErrorMessage());
                return;
            }

            // Generate duplicate hash
            String dupHash = emailParser.generateDuplicateHash(
                parsed.getTransactionDate(),
                parsed.getAmount(),
                parsed.getReference()
            );

            // Check for duplicate
            if (notificationRepository.existsByDuplicateCheckHash(dupHash)) {
                log.info("Duplicate notification detected, skipping: {}", dupHash);
                return; // Idempotent - return success
            }

            // Create TransactionNotification entity
            TransactionNotification notification = TransactionNotification.builder()
                .receivedAt(LocalDateTime.now())
                .rawPayload(payload.getBody())
                .transactionDate(parsed.getTransactionDate())
                .description(parsed.getDescription())
                .amount(parsed.getAmount())
                .balance(parsed.getBalance())
                .reference(parsed.getReference())
                .duplicateCheckHash(dupHash)
                .webhookSource(detectWebhookSource(payload))
                .emailSubject(payload.getSubject())
                .emailSender(payload.getSender())
                .matchStatus(TransactionNotification.MatchStatus.PENDING)
                .processed(false)
                .build();

            // Save notification first
            notification = notificationRepository.save(notification);
            log.info("Saved notification: {}", notification.getNotificationId());

            // Create Transaction entity (for existing flow compatibility)
            Transaction transaction = createTransactionFromNotification(notification, parsed);
            transaction = transactionRepository.save(transaction);
            log.info("Created transaction: {}", transaction.getId());

            // Link transaction to notification
            notification.setTransaction(transaction);

            // Attempt automatic matching
            boolean matched = attemptAutomaticMatch(notification, transaction, parsed);

            if (matched) {
                log.info("Successfully matched notification to student");
            } else {
                notification.setMatchStatus(TransactionNotification.MatchStatus.UNMATCHED);
                log.info("Could not automatically match notification - flagged for manual review");
            }

            notification.setProcessed(true);
            notification.setProcessedAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("Webhook notification processed successfully. Matched: {}", matched);

        } catch (Exception e) {
            log.error("Error processing webhook notification", e);
            saveFailedNotification(payload, e.getMessage());
        }
    }

    /**
     * Attempt to automatically match notification to student and create payment
     *
     * @param notification TransactionNotification entity
     * @param transaction  Transaction entity
     * @param parsed       Parsed email data
     * @return true if successfully matched
     */
    private boolean attemptAutomaticMatch(
            TransactionNotification notification,
            Transaction transaction,
            ParsedEmailNotification parsed) {

        // Strategy 1: Try to extract and match by student number (STU-YYYY-NNN)
        String studentNumber = emailParser.extractStudentNumber(parsed.getReference());

        if (studentNumber != null) {
            log.info("Found student number in reference: {}", studentNumber);
            Optional<Child> childOpt = childRepository.findByStudentNumber(studentNumber);

            if (childOpt.isPresent()) {
                Child child = childOpt.get();
                log.info("Matched to student: {} ({})", child.getFullName(), studentNumber);

                Payment payment = createPaymentRecord(child, transaction, parsed);
                payment.setMatchedAutomatically(true);
                payment = paymentRepository.save(payment);

                // Update notification
                notification.markAsMatched(child, payment);

                // Update transaction
                transaction.markAsMatched("Automatically matched to " + child.getFullName() + " via student number");
                transactionRepository.save(transaction);

                return true;
            }
        }

        // Strategy 2: Try to match by payment reference (legacy)
        if (parsed.getReference() != null) {
            Optional<Child> childOpt = childRepository.findByPaymentReferenceIgnoreCase(
                parsed.getReference().trim()
            );

            if (childOpt.isPresent()) {
                Child child = childOpt.get();
                log.info("Matched to student via payment reference: {} ({})",
                    child.getFullName(), child.getPaymentReference());

                Payment payment = createPaymentRecord(child, transaction, parsed);
                payment.setMatchedAutomatically(true);
                payment = paymentRepository.save(payment);

                notification.markAsMatched(child, payment);
                transaction.markAsMatched("Automatically matched to " + child.getFullName() + " via payment reference");
                transactionRepository.save(transaction);

                return true;
            }
        }

        // Strategy 3: Fuzzy match by name in reference (optional - can be added later)
        // For now, just mark as unmatched

        log.info("No automatic match found for reference: {}", parsed.getReference());
        return false;
    }

    /**
     * Create payment record from matched notification
     */
    private Payment createPaymentRecord(Child child, Transaction transaction, ParsedEmailNotification parsed) {
        // Determine payment month/year from transaction date
        YearMonth transactionMonth = YearMonth.from(parsed.getTransactionDate());
        int month = transactionMonth.getMonthValue();
        int year = transactionMonth.getYear();

        // Check if payment already exists for this month
        Optional<Payment> existingPayment = paymentRepository.findByChildIdAndPaymentMonthAndPaymentYear(
            child.getId(), month, year
        );

        Payment payment;
        if (existingPayment.isPresent()) {
            // Update existing payment (add to amount)
            payment = existingPayment.get();
            payment.setAmountPaid(payment.getAmountPaid().add(parsed.getAmount()));
            log.info("Updated existing payment for {}/{}", month, year);
        } else {
            // Create new payment
            payment = Payment.builder()
                .child(child)
                .paymentMonth(month)
                .paymentYear(year)
                .amountPaid(parsed.getAmount())
                .expectedAmount(child.getMonthlyFee())
                .paymentDate(LocalDate.from(parsed.getTransactionDate()))
                .paymentMethod(Payment.PaymentMethod.BANK_TRANSFER)
                .build();
            log.info("Created new payment for {}/{}", month, year);
        }

        payment.setTransaction(transaction);
        payment.setTransactionReference(transaction.getBankReference());

        return payment;
    }

    /**
     * Create Transaction entity from notification
     */
    private Transaction createTransactionFromNotification(
            TransactionNotification notification,
            ParsedEmailNotification parsed) {

        return Transaction.builder()
            .bankReference(notification.getDuplicateCheckHash()) // Use hash as unique reference
            .amount(parsed.getAmount())
            .transactionDate(LocalDate.from(parsed.getTransactionDate()))
            .paymentReference(parsed.getReference())
            .description(parsed.getDescription())
            .senderName(parsed.getSenderName())
            .status(Transaction.TransactionStatus.UNMATCHED)
            .type(Transaction.TransactionType.CREDIT)
            .rawData(notification.getRawPayload())
            .build();
    }

    /**
     * Save failed notification for review
     */
    private void saveFailedNotification(MyUpdatesWebhookPayload payload, String errorMsg) {
        try {
            TransactionNotification notification = TransactionNotification.builder()
                .receivedAt(LocalDateTime.now())
                .rawPayload(payload.getBody())
                .emailSubject(payload.getSubject())
                .emailSender(payload.getSender())
                .matchStatus(TransactionNotification.MatchStatus.FAILED)
                .processed(false)
                .errorMessage(errorMsg)
                .webhookSource(detectWebhookSource(payload))
                .build();

            notificationRepository.save(notification);
            log.info("Saved failed notification for review");
        } catch (Exception e) {
            log.error("Failed to save failed notification", e);
        }
    }

    /**
     * Detect webhook source from payload
     */
    private String detectWebhookSource(MyUpdatesWebhookPayload payload) {
        if (payload.getSource() != null && !payload.getSource().isEmpty()) {
            return payload.getSource().toUpperCase();
        }

        if (payload.getEmailId() != null) {
            String emailId = payload.getEmailId().toLowerCase();
            if (emailId.contains("zapier")) {
                return "ZAPIER";
            }
            if (emailId.contains("make") || emailId.contains("integromat")) {
                return "MAKE_COM";
            }
            if (emailId.contains("google") || emailId.contains("gmail")) {
                return "GMAIL_SCRIPT";
            }
        }

        return "EMAIL_FORWARD";
    }

    /**
     * Get webhook statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getWebhookStats() {
        Map<String, Object> stats = new HashMap<>();

        // Total notifications
        long totalNotifications = notificationRepository.count();
        stats.put("total_notifications", totalNotifications);

        // Matched vs unmatched
        List<TransactionNotification> matched = notificationRepository.findByMatchStatus(
            TransactionNotification.MatchStatus.MATCHED
        );
        List<TransactionNotification> unmatched = notificationRepository.findByMatchStatus(
            TransactionNotification.MatchStatus.UNMATCHED
        );
        List<TransactionNotification> failed = notificationRepository.findFailedNotifications();

        stats.put("matched_count", matched.size());
        stats.put("unmatched_count", unmatched.size());
        stats.put("failed_count", failed.size());

        // Recent notifications (last 24 hours)
        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
        List<TransactionNotification> recent = notificationRepository.findRecentNotifications(yesterday);
        stats.put("last_24h_count", recent.size());

        // Match rate
        if (totalNotifications > 0) {
            double matchRate = (matched.size() * 100.0) / totalNotifications;
            stats.put("match_rate_percentage", String.format("%.2f", matchRate));
        } else {
            stats.put("match_rate_percentage", "0.00");
        }

        return stats;
    }

    /**
     * Retry failed notifications
     */
    @Transactional
    public int retryFailedNotifications() {
        List<TransactionNotification> failed = notificationRepository.findFailedNotifications();
        log.info("Retrying {} failed notifications", failed.size());

        int successCount = 0;
        for (TransactionNotification notification : failed) {
            try {
                // Reconstruct payload and retry
                MyUpdatesWebhookPayload payload = MyUpdatesWebhookPayload.builder()
                    .body(notification.getRawPayload())
                    .subject(notification.getEmailSubject())
                    .sender(notification.getEmailSender())
                    .emailId(notification.getNotificationId().toString())
                    .build();

                processNotificationAsync(payload);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to retry notification: {}", notification.getNotificationId(), e);
            }
        }

        log.info("Successfully retried {} notifications", successCount);
        return successCount;
    }
}
