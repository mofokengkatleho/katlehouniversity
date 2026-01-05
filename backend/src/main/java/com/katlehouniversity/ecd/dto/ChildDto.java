package com.katlehouniversity.ecd.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildDto {

    private Long id;

    private String studentNumber;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String gender;

    private String studentIdNumber;

    private String physicalAddress;

    private String allergies;

    private String paymentReference;

    @NotNull(message = "Monthly fee is required")
    @DecimalMin(value = "0.01", message = "Monthly fee must be greater than 0")
    private BigDecimal monthlyFee;

    @Min(value = 1, message = "Payment day must be between 1 and 31")
    @Max(value = 31, message = "Payment day must be between 1 and 31")
    private Integer paymentDay;

    private String parentPhone;

    @Email(message = "Invalid email format")
    private String parentEmail;

    @Email(message = "Invalid email format")
    private String guardianEmail;

    private String parentName;

    private String gradeClass;

    private String academicYear;

    private LocalDate dateOfBirth;

    private LocalDate enrollmentDate;

    private String status;

    private boolean active;

    private String notes;
}