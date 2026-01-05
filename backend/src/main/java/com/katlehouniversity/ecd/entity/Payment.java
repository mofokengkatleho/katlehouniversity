package com.katlehouniversity.ecd.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(name = "payments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"child_id", "payment_month", "payment_year"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @NotNull(message = "Payment month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    @Column(nullable = false)
    private Integer paymentMonth;

    @NotNull(message = "Payment year is required")
    @Column(nullable = false)
    private Integer paymentYear;

    @NotNull(message = "Amount paid is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid;

    @Column(precision = 10, scale = 2)
    private BigDecimal expectedAmount;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.BANK_TRANSFER;

    @Column(length = 100)
    private String transactionReference;

    @Column(columnDefinition = "boolean default false")
    private boolean matchedAutomatically = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum PaymentStatus {
        PENDING,
        PAID,
        PARTIAL,
        OVERPAID,
        REVERSED
    }

    public enum PaymentMethod {
        BANK_TRANSFER,
        CASH,
        CARD,
        OTHER
    }

    public String getPaymentPeriod() {
        return YearMonth.of(paymentYear, paymentMonth).toString();
    }

    public boolean isFullyPaid() {
        if (expectedAmount == null) {
            return false;
        }
        return amountPaid.compareTo(expectedAmount) >= 0;
    }

    public BigDecimal getOutstandingAmount() {
        if (expectedAmount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal outstanding = expectedAmount.subtract(amountPaid);
        return outstanding.max(BigDecimal.ZERO);
    }

    @PrePersist
    @PreUpdate
    private void updatePaymentStatus() {
        if (expectedAmount != null) {
            int comparison = amountPaid.compareTo(expectedAmount);
            if (comparison >= 0) {
                status = PaymentStatus.PAID;
            } else if (amountPaid.compareTo(BigDecimal.ZERO) > 0) {
                status = PaymentStatus.PARTIAL;
            }
        }
    }
}