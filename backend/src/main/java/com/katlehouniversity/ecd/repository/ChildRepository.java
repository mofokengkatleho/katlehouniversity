package com.katlehouniversity.ecd.repository;

import com.katlehouniversity.ecd.entity.Child;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChildRepository extends JpaRepository<Child, Long> {

    Optional<Child> findByStudentNumber(String studentNumber);

    Optional<Child> findByPaymentReference(String paymentReference);

    Optional<Child> findByPaymentReferenceIgnoreCase(String paymentReference);

    List<Child> findByAcademicYear(String academicYear);

    @Query("SELECT c FROM Child c WHERE c.status = 'ACTIVE'")
    List<Child> findByActiveTrue();

    @Query("SELECT c FROM Child c WHERE c.status <> 'ACTIVE'")
    List<Child> findByActiveFalse();

    @Query("SELECT c FROM Child c WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Child> searchByName(@Param("name") String name);

    @Query("SELECT c FROM Child c WHERE LOWER(CONCAT(c.firstName, ' ', c.lastName)) " +
           "LIKE LOWER(CONCAT('%', :fullName, '%'))")
    List<Child> searchByFullName(@Param("fullName") String fullName);

    boolean existsByPaymentReference(String paymentReference);

    @Query("SELECT COUNT(c) FROM Child c WHERE c.status = 'ACTIVE'")
    long countActiveChildren();

    @Query("SELECT c FROM Child c LEFT JOIN FETCH c.payments WHERE c.id = :id")
    Optional<Child> findByIdWithPayments(@Param("id") Long id);
}