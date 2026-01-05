package com.katlehouniversity.ecd.repository;

import com.katlehouniversity.ecd.entity.TransactionNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionNotificationRepository extends JpaRepository<TransactionNotification, UUID> {

    /**
     * Check if a notification with this duplicate hash already exists
     * @param hash SHA-256 hash of date+amount+reference
     * @return true if duplicate exists
     */
    boolean existsByDuplicateCheckHash(String hash);

    /**
     * Find notification by duplicate hash
     */
    Optional<TransactionNotification> findByDuplicateCheckHash(String hash);

    /**
     * Find all unmatched notifications
     */
    List<TransactionNotification> findByMatchStatus(TransactionNotification.MatchStatus matchStatus);

    /**
     * Find all unprocessed notifications
     */
    List<TransactionNotification> findByProcessedFalse();

    /**
     * Count unmatched notifications
     */
    @Query("SELECT COUNT(n) FROM TransactionNotification n WHERE n.matchStatus = 'UNMATCHED'")
    long countUnmatched();

    /**
     * Find notifications by date range
     */
    @Query("SELECT n FROM TransactionNotification n WHERE n.transactionDate BETWEEN :startDate AND :endDate ORDER BY n.transactionDate DESC")
    List<TransactionNotification> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find notifications by webhook source
     */
    List<TransactionNotification> findByWebhookSource(String webhookSource);

    /**
     * Find notifications received in the last N hours (for monitoring)
     */
    @Query("SELECT n FROM TransactionNotification n WHERE n.receivedAt >= :since ORDER BY n.receivedAt DESC")
    List<TransactionNotification> findRecentNotifications(@Param("since") LocalDateTime since);

    /**
     * Find failed notifications for retry/review
     */
    @Query("SELECT n FROM TransactionNotification n WHERE n.matchStatus = 'FAILED' ORDER BY n.receivedAt DESC")
    List<TransactionNotification> findFailedNotifications();
}
