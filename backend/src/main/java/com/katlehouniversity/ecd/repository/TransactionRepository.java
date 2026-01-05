package com.katlehouniversity.ecd.repository;

import com.katlehouniversity.ecd.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByBankReference(String bankReference);

    List<Transaction> findByStatus(Transaction.TransactionStatus status);

    List<Transaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT t FROM Transaction t WHERE t.status = 'UNMATCHED' " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findUnmatchedTransactions();

    @Query("SELECT t FROM Transaction t WHERE LOWER(t.paymentReference) " +
           "LIKE LOWER(CONCAT('%', :reference, '%'))")
    List<Transaction> searchByPaymentReference(@Param("reference") String reference);

    @Query("SELECT t FROM Transaction t WHERE t.transactionDate >= :date " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findRecentTransactions(@Param("date") LocalDate date);

    boolean existsByBankReference(String bankReference);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = 'UNMATCHED'")
    long countUnmatchedTransactions();

    @Query("SELECT t FROM Transaction t WHERE t.status = 'UNMATCHED' " +
           "AND t.paymentReference IS NOT NULL AND t.paymentReference != ''")
    List<Transaction> findUnmatchedWithReference();
}