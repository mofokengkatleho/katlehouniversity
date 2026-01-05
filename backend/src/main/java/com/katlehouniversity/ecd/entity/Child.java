package com.katlehouniversity.ecd.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Child {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String studentNumber; // e.g., STU-2025-001

    @NotBlank(message = "First name is required")
    @Column(nullable = false, length = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Column(nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(length = 50)
    private String studentIdNumber; // National ID or Birth Certificate number

    @Column(columnDefinition = "TEXT")
    private String physicalAddress;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    @Column(unique = true, nullable = false, length = 50)
    private String paymentReference; // Legacy field, will be replaced by studentNumber

    @NotNull(message = "Monthly fee is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Monthly fee must be greater than 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyFee;

    @Min(value = 1, message = "Payment day must be between 1 and 31")
    @Max(value = 31, message = "Payment day must be between 1 and 31")
    @Column(name = "payment_day")
    private Integer paymentDay; // Day of month guardian commits to pay

    @Column(length = 20)
    private String parentPhone;

    @Email(message = "Invalid email format")
    @Column(length = 100)
    private String parentEmail;

    @Email(message = "Invalid email format")
    @Column(length = 100)
    private String guardianEmail;

    @Column(name = "parent_name", length = 200)
    private String parentName;

    @Column(length = 50)
    private String gradeClass;

    @Column(nullable = false, length = 10)
    private String academicYear; // e.g., "2025"

    private LocalDate dateOfBirth;

    private LocalDate enrollmentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StudentStatus status = StudentStatus.ACTIVE;

    @Column(length = 500)
    private String notes;

    @OneToMany(mappedBy = "child", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Gender {
        MALE,
        FEMALE,
        OTHER
    }

    public enum StudentStatus {
        ACTIVE,
        GRADUATED,
        WITHDRAWN,
        SUSPENDED
    }

    @PrePersist
    @PreUpdate
    private void ensurePaymentReference() {
        // Ensure paymentReference is set (for backward compatibility)
        if (this.paymentReference == null || this.paymentReference.isEmpty()) {
            this.paymentReference = this.studentNumber != null ? this.studentNumber : generateLegacyReference();
        }
        // Set default academic year if not set
        if (this.academicYear == null || this.academicYear.isEmpty()) {
            this.academicYear = String.valueOf(LocalDate.now().getYear());
        }
    }

    private String generateLegacyReference() {
        String cleanFirstName = firstName.replaceAll("[^a-zA-Z]", "").toUpperCase();
        String cleanLastName = lastName.replaceAll("[^a-zA-Z]", "").toUpperCase();
        return cleanFirstName + cleanLastName + (id != null ? id : "");
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void addPayment(Payment payment) {
        payments.add(payment);
        payment.setChild(this);
    }

    public void removePayment(Payment payment) {
        payments.remove(payment);
        payment.setChild(null);
    }
}