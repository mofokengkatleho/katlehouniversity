package com.katlehouniversity.ecd.service;

import com.katlehouniversity.ecd.entity.Child;
import com.katlehouniversity.ecd.entity.Payment;
import com.katlehouniversity.ecd.entity.Transaction;
import com.katlehouniversity.ecd.repository.ChildRepository;
import com.katlehouniversity.ecd.repository.PaymentRepository;
import com.katlehouniversity.ecd.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentMatchingService {

    private final TransactionRepository transactionRepository;
    private final ChildRepository childRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public void matchAllUnmatchedTransactions() {
        log.info("Starting automatic payment matching...");

        List<Transaction> unmatchedTransactions = transactionRepository.findUnmatchedWithReference();
        log.info("Found {} unmatched transactions with payment references", unmatchedTransactions.size());

        int matchedCount = 0;
        for (Transaction transaction : unmatchedTransactions) {
            if (matchTransaction(transaction)) {
                matchedCount++;
            }
        }

        log.info("Matched {} transactions successfully", matchedCount);
    }

    @Transactional
    public boolean matchTransaction(Transaction transaction) {
        if (transaction.getPaymentReference() == null || transaction.getPaymentReference().isEmpty()) {
            log.debug("Transaction {} has no payment reference, skipping", transaction.getId());
            return false;
        }

        // Try to find child by payment reference
        Optional<Child> childOpt = childRepository.findByPaymentReferenceIgnoreCase(
                transaction.getPaymentReference().trim());

        if (childOpt.isEmpty()) {
            log.debug("No child found for payment reference: {}", transaction.getPaymentReference());
            return false;
        }

        Child child = childOpt.get();
        log.info("Matched transaction {} to child: {} ({})",
                transaction.getBankReference(),
                child.getFullName(),
                child.getPaymentReference());

        // Determine payment month/year from transaction date
        YearMonth transactionMonth = YearMonth.from(transaction.getTransactionDate());

        // Create or update payment record
        Payment payment = paymentRepository.findByChildIdAndPaymentMonthAndPaymentYear(
                        child.getId(),
                        transactionMonth.getMonthValue(),
                        transactionMonth.getYear())
                .orElse(Payment.builder()
                        .child(child)
                        .paymentMonth(transactionMonth.getMonthValue())
                        .paymentYear(transactionMonth.getYear())
                        .amountPaid(transaction.getAmount())
                        .build());

        payment.setTransaction(transaction);
        payment.setExpectedAmount(child.getMonthlyFee());
        payment.setPaymentDate(transaction.getTransactionDate());
        payment.setTransactionReference(transaction.getBankReference());
        payment.setPaymentMethod(Payment.PaymentMethod.BANK_TRANSFER);

        paymentRepository.save(payment);

        // Update transaction status
        transaction.markAsMatched("Automatically matched to " + child.getFullName());
        transactionRepository.save(transaction);

        return true;
    }

    @Transactional
    public Payment manuallyMatchTransaction(Long transactionId, Long childId, Integer month, Integer year) {
        log.info("Manually matching transaction {} to child {} for {}/{}",
                transactionId, childId, month, year);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child not found"));

        Payment payment = Payment.builder()
                .child(child)
                .transaction(transaction)
                .paymentMonth(month)
                .paymentYear(year)
                .amountPaid(transaction.getAmount())
                .expectedAmount(child.getMonthlyFee())
                .paymentDate(transaction.getTransactionDate())
                .transactionReference(transaction.getBankReference())
                .paymentMethod(Payment.PaymentMethod.BANK_TRANSFER)
                .notes("Manually matched")
                .build();

        payment = paymentRepository.save(payment);

        transaction.setManuallyMatched(true);
        transaction.markAsMatched("Manually matched to " + child.getFullName());
        transactionRepository.save(transaction);

        log.info("Manual matching completed successfully");
        return payment;
    }

    @Transactional(readOnly = true)
    public List<Transaction> getUnmatchedTransactions() {
        return transactionRepository.findUnmatchedTransactions();
    }

    @Transactional(readOnly = true)
    public long getUnmatchedTransactionCount() {
        return transactionRepository.countUnmatchedTransactions();
    }
}