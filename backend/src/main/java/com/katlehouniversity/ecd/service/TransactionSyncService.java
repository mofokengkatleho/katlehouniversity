package com.katlehouniversity.ecd.service;

import com.katlehouniversity.ecd.entity.Transaction;
import com.katlehouniversity.ecd.integration.StandardBankApiClient;
import com.katlehouniversity.ecd.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionSyncService {

    private final StandardBankApiClient standardBankApiClient;
    private final TransactionRepository transactionRepository;
    private final PaymentMatchingService paymentMatchingService;

    @Transactional
    public void syncTransactions(LocalDate startDate, LocalDate endDate) {
        log.info("Starting transaction sync from {} to {}", startDate, endDate);

        try {
            // Fetch transactions from Standard Bank API
            List<Transaction> transactions = standardBankApiClient.fetchTransactions(startDate, endDate);
            log.info("Fetched {} transactions from API", transactions.size());

            // Save new transactions (skip duplicates)
            int savedCount = 0;
            for (Transaction transaction : transactions) {
                if (!transactionRepository.existsByBankReference(transaction.getBankReference())) {
                    transactionRepository.save(transaction);
                    savedCount++;
                } else {
                    log.debug("Skipping duplicate transaction: {}", transaction.getBankReference());
                }
            }

            log.info("Saved {} new transactions to database", savedCount);

            // Automatically match transactions to payments
            if (savedCount > 0) {
                paymentMatchingService.matchAllUnmatchedTransactions();
            }

            log.info("Transaction sync completed successfully");

        } catch (Exception e) {
            log.error("Error during transaction sync", e);
            throw new RuntimeException("Transaction sync failed", e);
        }
    }

    @Transactional
    public void syncLastNDays(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        syncTransactions(startDate, endDate);
    }
}
