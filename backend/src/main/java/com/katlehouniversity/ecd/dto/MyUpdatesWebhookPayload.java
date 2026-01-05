package com.katlehouniversity.ecd.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for receiving MyUpdates webhook notifications from email forwarding services
 * (Zapier, Make.com, Gmail Apps Script, etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyUpdatesWebhookPayload {

    /**
     * Unique identifier for this email (from email service)
     */
    @JsonProperty("email_id")
    private String emailId;

    /**
     * Timestamp when email was received by forwarding service
     */
    @JsonProperty("received_at")
    private String receivedAt;

    /**
     * Email sender address (should be from standardbank.co.za)
     */
    @NotBlank(message = "Sender is required")
    private String sender;

    /**
     * Email subject line
     */
    private String subject;

    /**
     * Full email body (plain text or HTML)
     */
    @NotBlank(message = "Email body is required")
    private String body;

    /**
     * API key for webhook authentication (optional if sent via header)
     */
    @JsonProperty("api_key")
    private String apiKey;

    /**
     * Optional: Email recipient (our account email)
     */
    private String recipient;

    /**
     * Optional: Webhook source identifier (ZAPIER, MAKE_COM, etc.)
     */
    private String source;
}
