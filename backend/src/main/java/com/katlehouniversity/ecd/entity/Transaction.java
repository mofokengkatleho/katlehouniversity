package com.katlehouniversity.ecd.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_date", columnList = "transactionDate"),
    @Index(name = "idx_bank_reference", columnList = "bankReference"),
    @Index(name = "idx_payment_reference", columnList = "paymentReference")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String bankReference;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Column(length = 200)
    private String paymentReference;

    @Column(length = 200)
    private String description;

    @Column(length = 100)
    private String senderName;

    @Column(length = 50)
    private String senderAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.UNMATCHED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransactionType type = TransactionType.CREDIT;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @Column(length = 500)
    private String matchingNotes;

    @Column(columnDefinition = "boolean default false")
    private boolean manuallyMatched = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_statement_id")
    private UploadedStatement uploadedStatement;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime matchedAt;

    @Column(columnDefinition = "TEXT")
    private String rawData;

    public enum TransactionStatus {
        UNMATCHED,
        MATCHED,
        PARTIALLY_MATCHED,
        IGNORED,
        DISPUTED
    }

    public enum TransactionType {
        CREDIT,
        DEBIT,
        REVERSAL
    }

    public boolean isMatched() {
        return status == TransactionStatus.MATCHED || status == TransactionStatus.PARTIALLY_MATCHED;
    }

    public void markAsMatched(String notes) {
        this.status = TransactionStatus.MATCHED;
        this.matchedAt = LocalDateTime.now();
        this.matchingNotes = notes;
    }

    public void addPayment(Payment payment) {
        payments.add(payment);
        payment.setTransaction(this);
    }
}