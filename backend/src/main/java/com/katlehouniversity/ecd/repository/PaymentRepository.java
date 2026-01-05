package com.katlehouniversity.ecd.repository;

import com.katlehouniversity.ecd.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByChildId(Long childId);

    Optional<Payment> findByChildIdAndPaymentMonthAndPaymentYear(
            Long childId, Integer month, Integer year);

    List<Payment> findByPaymentMonthAndPaymentYear(Integer month, Integer year);

    List<Payment> findByPaymentYear(Integer year);

    List<Payment> findByStatus(Payment.PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.paymentMonth = :month AND p.paymentYear = :year " +
           "AND p.status = :status")
    List<Payment> findByMonthYearAndStatus(
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("status") Payment.PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.child.id = :childId " +
           "ORDER BY p.paymentYear DESC, p.paymentMonth DESC")
    List<Payment> findByChildIdOrderByDateDesc(@Param("childId") Long childId);

    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM Payment p " +
           "WHERE p.paymentMonth = :month AND p.paymentYear = :year")
    BigDecimal getTotalCollectedForMonth(@Param("month") Integer month, @Param("year") Integer year);

    @Query("SELECT COUNT(DISTINCT p.child.id) FROM Payment p " +
           "WHERE p.paymentMonth = :month AND p.paymentYear = :year AND p.status = 'PAID'")
    long countPaidChildrenForMonth(@Param("month") Integer month, @Param("year") Integer year);

    @Query("SELECT p FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate")
    List<Payment> findByPaymentDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    boolean existsByChildIdAndPaymentMonthAndPaymentYear(
            Long childId, Integer month, Integer year);
}