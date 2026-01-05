package com.katlehouniversity.ecd.scheduler;

import com.katlehouniversity.ecd.service.TransactionSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to sync transactions from Standard Bank API daily
 *
 * Runs at 1 AM daily (configurable via application.yml)
 * Can be disabled by setting scheduler.transaction-sync.enabled=false
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        value = "scheduler.transaction-sync.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class TransactionSyncScheduler {

    private final TransactionSyncService transactionSyncService;

    /**
     * Runs daily at 1 AM to fetch and sync last 7 days of transactions
     */
    @Scheduled(cron = "${scheduler.transaction-sync.cron:0 0 1 * * ?}")
    public void syncDailyTransactions() {
        log.info("Starting scheduled transaction sync job");

        try {
            // Sync last 7 days to catch any delayed transactions
            transactionSyncService.syncLastNDays(7);
            log.info("Scheduled transaction sync completed successfully");

        } catch (Exception e) {
            log.error("Scheduled transaction sync failed", e);
        }
    }
}
