package com.katlehouniversity.ecd.service;

import com.katlehouniversity.ecd.dto.ParsedEmailNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to parse Standard Bank MyUpdates email notifications.
 * Extracts transaction details using regex patterns.
 */
@Service
@Slf4j
public class MyUpdatesEmailParser {

    // Regex patterns for Standard Bank email format
    // Matches: "Date: 15/01/2025" or "Date: 2025-01-15" or "Date:15/01/2025"
    private static final Pattern DATE_PATTERN = Pattern.compile(
        "Date:\\s*(\\d{2}[/-]\\d{2}[/-]\\d{4}|\\d{4}-\\d{2}-\\d{2})",
        Pattern.CASE_INSENSITIVE
    );

    // Matches: "Amount: R 1,500.00" or "Amount: 1500.00" or "Amount:R1,500.00"
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
        "Amount:\\s*R?\\s*([\\d,]+\\.\\d{2})",
        Pattern.CASE_INSENSITIVE
    );

    // Matches: "Reference: STU-2025-001 January Fee" (captures everything until newline or specific delimiters)
    private static final Pattern REFERENCE_PATTERN = Pattern.compile(
        "Reference:\\s*(.+?)(?=\\n|\\r|$|Date:|Amount:|Balance:)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Matches: "Balance: R 45,230.50" or "New Balance: 45230.50"
    private static final Pattern BALANCE_PATTERN = Pattern.compile(
        "(?:New )?Balance:\\s*R?\\s*([\\d,]+\\.\\d{2})",
        Pattern.CASE_INSENSITIVE
    );

    // Matches: "Description: Payment received" or similar
    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile(
        "Description:\\s*(.+?)(?=\\n|\\r|$|Date:|Amount:|Reference:)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Matches: "From: John Doe" or "Sender: Jane Smith"
    private static final Pattern SENDER_NAME_PATTERN = Pattern.compile(
        "(?:From|Sender):\\s*(.+?)(?=\\n|\\r|$)",
        Pattern.CASE_INSENSITIVE
    );

    // Matches transaction type indicators
    private static final Pattern CREDIT_PATTERN = Pattern.compile(
        "\\b(credit|deposit|received|payment received)\\b",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DEBIT_PATTERN = Pattern.compile(
        "\\b(debit|withdrawal|payment sent)\\b",
        Pattern.CASE_INSENSITIVE
    );

    // Date formatters
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy")
    };

    /**
     * Parse Standard Bank MyUpdates email notification
     *
     * @param emailBody    Full email body text
     * @param emailSubject Email subject line
     * @return Parsed notification with extracted transaction details
     */
    public ParsedEmailNotification parseMyUpdatesEmail(String emailBody, String emailSubject) {
        log.info("Parsing MyUpdates email notification");

        ParsedEmailNotification notification = ParsedEmailNotification.builder()
            .valid(false)
            .build();

        if (emailBody == null || emailBody.trim().isEmpty()) {
            notification.setErrorMessage("Email body is empty");
            log.warn("Cannot parse empty email body");
            return notification;
        }

        try {
            // Extract transaction date
            LocalDateTime transactionDate = extractDate(emailBody);
            if (transactionDate != null) {
                notification.setTransactionDate(transactionDate);
            } else {
                log.warn("Could not extract transaction date from email");
            }

            // Extract amount
            BigDecimal amount = extractAmount(emailBody);
            if (amount != null) {
                notification.setAmount(amount);
            } else {
                log.warn("Could not extract amount from email");
            }

            // Extract reference
            String reference = extractReference(emailBody);
            if (reference != null && !reference.trim().isEmpty()) {
                notification.setReference(reference.trim());
            } else {
                log.warn("Could not extract reference from email");
            }

            // Extract balance
            BigDecimal balance = extractBalance(emailBody);
            if (balance != null) {
                notification.setBalance(balance);
            }

            // Extract description
            String description = extractDescription(emailBody);
            if (description != null && !description.trim().isEmpty()) {
                notification.setDescription(description.trim());
            } else {
                // Fallback: use subject or first 100 chars of body
                notification.setDescription(
                    emailSubject != null ? emailSubject : emailBody.substring(0, Math.min(100, emailBody.length()))
                );
            }

            // Extract sender name (optional)
            String senderName = extractSenderName(emailBody);
            if (senderName != null) {
                notification.setSenderName(senderName.trim());
            }

            // Detect transaction type
            String transactionType = detectTransactionType(emailBody, emailSubject);
            notification.setTransactionType(transactionType);

            // Validate required fields
            boolean isValid = validateNotification(notification);
            notification.setValid(isValid);

            if (!isValid) {
                notification.setErrorMessage("Missing required fields: " + getMissingFields(notification));
                log.warn("Notification validation failed: {}", notification.getErrorMessage());
            } else {
                log.info("Successfully parsed notification: amount={}, reference={}", amount, reference);
            }

        } catch (Exception e) {
            log.error("Failed to parse MyUpdates email", e);
            notification.setValid(false);
            notification.setErrorMessage("Parsing error: " + e.getMessage());
        }

        return notification;
    }

    private LocalDateTime extractDate(String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            String dateStr = matcher.group(1);
            for (DateTimeFormatter formatter : DATE_FORMATTERS) {
                try {
                    LocalDate date = LocalDate.parse(dateStr, formatter);
                    return date.atStartOfDay(); // Default to midnight
                } catch (DateTimeParseException ignored) {
                    // Try next formatter
                }
            }
        }
        return null;
    }

    private BigDecimal extractAmount(String text) {
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        if (matcher.find()) {
            String amountStr = matcher.group(1);
            return parseCurrency(amountStr);
        }
        return null;
    }

    private String extractReference(String text) {
        Matcher matcher = REFERENCE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private BigDecimal extractBalance(String text) {
        Matcher matcher = BALANCE_PATTERN.matcher(text);
        if (matcher.find()) {
            String balanceStr = matcher.group(1);
            return parseCurrency(balanceStr);
        }
        return null;
    }

    private String extractDescription(String text) {
        Matcher matcher = DESCRIPTION_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String extractSenderName(String text) {
        Matcher matcher = SENDER_NAME_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String detectTransactionType(String emailBody, String emailSubject) {
        String combinedText = (emailBody + " " + (emailSubject != null ? emailSubject : "")).toLowerCase();

        if (CREDIT_PATTERN.matcher(combinedText).find()) {
            return "CREDIT";
        } else if (DEBIT_PATTERN.matcher(combinedText).find()) {
            return "DEBIT";
        }

        return "CREDIT"; // Default to credit for payment reconciliation
    }

    private BigDecimal parseCurrency(String amountStr) {
        try {
            // Remove commas and spaces
            String cleaned = amountStr.replace(",", "").replace(" ", "").trim();
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            log.error("Failed to parse currency: {}", amountStr, e);
            return null;
        }
    }

    private boolean validateNotification(ParsedEmailNotification notification) {
        return notification.getTransactionDate() != null
            && notification.getAmount() != null
            && notification.getAmount().compareTo(BigDecimal.ZERO) > 0
            && notification.getReference() != null
            && !notification.getReference().trim().isEmpty();
    }

    private String getMissingFields(ParsedEmailNotification notification) {
        StringBuilder missing = new StringBuilder();
        if (notification.getTransactionDate() == null) {
            missing.append("date, ");
        }
        if (notification.getAmount() == null || notification.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            missing.append("amount, ");
        }
        if (notification.getReference() == null || notification.getReference().trim().isEmpty()) {
            missing.append("reference, ");
        }
        return missing.length() > 0 ? missing.substring(0, missing.length() - 2) : "none";
    }

    /**
     * Generate SHA-256 hash for duplicate detection
     *
     * @param date      Transaction date
     * @param amount    Transaction amount
     * @param reference Transaction reference
     * @return Base64-encoded SHA-256 hash
     */
    public String generateDuplicateHash(LocalDateTime date, BigDecimal amount, String reference) {
        String combined = date.toString() + "|" + amount.toString() + "|" + reference;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Extract student number from reference if present
     *
     * @param reference Transaction reference text
     * @return Student number (e.g., "STU-2025-001") or null if not found
     */
    public String extractStudentNumber(String reference) {
        if (reference == null) {
            return null;
        }

        // Pattern for student number: STU-YYYY-NNN
        Pattern studentNumberPattern = Pattern.compile("STU-\\d{4}-\\d{3}");
        Matcher matcher = studentNumberPattern.matcher(reference);

        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }
}
