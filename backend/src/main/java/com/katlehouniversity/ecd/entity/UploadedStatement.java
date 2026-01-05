package com.katlehouniversity.ecd.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "uploaded_statements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileType fileType;

    @Column(nullable = false)
    private Integer totalTransactions;

    @Column(nullable = false)
    @Builder.Default
    private Integer matchedCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer unmatchedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProcessingStatus status = ProcessingStatus.PENDING;

    @Column(length = 500)
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @OneToMany(mappedBy = "uploadedStatement", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime uploadDate;

    private LocalDateTime processedDate;

    public enum FileType {
        CSV,
        MARKDOWN,
        PDF
    }

    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    public void markAsProcessing() {
        this.status = ProcessingStatus.PROCESSING;
    }

    public void markAsCompleted() {
        this.status = ProcessingStatus.COMPLETED;
        this.processedDate = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = ProcessingStatus.FAILED;
        this.errorMessage = errorMessage;
        this.processedDate = LocalDateTime.now();
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        transaction.setUploadedStatement(this);
    }

    public void updateCounts() {
        this.totalTransactions = transactions.size();
        this.matchedCount = (int) transactions.stream()
                .filter(Transaction::isMatched)
                .count();
        this.unmatchedCount = this.totalTransactions - this.matchedCount;
    }
}
