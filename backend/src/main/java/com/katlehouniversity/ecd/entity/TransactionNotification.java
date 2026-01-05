package com.katlehouniversity.ecd.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity to track all incoming transaction notifications from MyUpdates webhook.
 * Provides complete audit trail of all notifications received.
 */
@Entity
@Table(name = "transaction_notifications", indexes = {
    @Index(name = "idx_duplicate_hash", columnList = "duplicateCheckHash", unique = true),
    @Index(name = "idx_match_status", columnList = "matchStatus"),
    @Index(name = "idx_processed", columnList = "processed"),
    @Index(name = "idx_received_at", columnList = "receivedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID notificationId;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    /**
     * Raw email body or webhook payload for debugging and audit
     */
    @Column(columnDefinition = "TEXT")
    private String rawPayload;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(precision = 10, scale = 2)
    private BigDecimal balance;

    /**
     * Reference/description field from bank notification
     */
    @Column(length = 500)
    private String reference;

    /**
     * Student that this notification was matched to (null if unmatched)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_to_student_id")
    private Child matchedToStudent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MatchStatus matchStatus = MatchStatus.PENDING;

    /**
     * Whether this notification has been processed by matching engine
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean processed = false;

    private LocalDateTime processedAt;

    /**
     * SHA-256 hash of date+amount+reference for duplicate detection
     */
    @Column(unique = true, length = 100)
    private String duplicateCheckHash;

    /**
     * Source of webhook (ZAPIER, MAKE_COM, EMAIL_FORWARD, etc.)
     */
    @Column(length = 50)
    private String webhookSource;

    /**
     * Payment record created from this notification (if matched)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_payment_id")
    private Payment createdPayment;

    /**
     * Corresponding transaction record (for integration with existing flow)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    /**
     * Email subject line (if from email)
     */
    @Column(length = 200)
    private String emailSubject;

    /**
     * Email sender (for validation)
     */
    @Column(length = 100)
    private String emailSender;

    /**
     * Error message if parsing or processing failed
     */
    @Column(length = 500)
    private String errorMessage;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum MatchStatus {
        PENDING,      // Not yet processed
        MATCHED,      // Successfully matched to student
        UNMATCHED,    // No match found
        MANUAL,       // Manually assigned by admin
        FAILED,       // Processing error
        DUPLICATE     // Duplicate notification (skipped)
    }

    public boolean isProcessed() {
        return processed != null && processed;
    }

    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
    }

    public void markAsMatched(Child student, Payment payment) {
        this.matchStatus = MatchStatus.MATCHED;
        this.matchedToStudent = student;
        this.createdPayment = payment;
        markAsProcessed();
    }

    public void markAsUnmatched() {
        this.matchStatus = MatchStatus.UNMATCHED;
        markAsProcessed();
    }

    public void markAsFailed(String error) {
        this.matchStatus = MatchStatus.FAILED;
        this.errorMessage = error;
        markAsProcessed();
    }
}
