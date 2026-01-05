package com.katlehouniversity.ecd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing parsed transaction notification from Standard Bank MyUpdates email
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedEmailNotification {

    private LocalDateTime transactionDate;
    private String description;
    private BigDecimal amount;
    private BigDecimal balance;
    private String reference;

    /**
     * Indicates if the email was successfully parsed and contains valid data
     */
    private boolean valid;

    /**
     * Error message if parsing failed
     */
    private String errorMessage;

    /**
     * Transaction type (CREDIT/DEBIT) if detected
     */
    private String transactionType;

    /**
     * Sender name if available
     */
    private String senderName;

    /**
     * Account number if available
     */
    private String accountNumber;
}
