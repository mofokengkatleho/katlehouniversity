package com.katlehouniversity.ecd.controller;

import com.katlehouniversity.ecd.dto.MyUpdatesWebhookPayload;
import com.katlehouniversity.ecd.service.WebhookProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for receiving Standard Bank MyUpdates webhook notifications.
 * Receives email notifications forwarded via Zapier, Make.com, or custom email forwarding.
 */
@RestController
@RequestMapping("/api/webhook")
@Slf4j
@Tag(name = "Webhook", description = "Endpoints for receiving transaction notifications from Standard Bank MyUpdates")
public class WebhookController {

    @Autowired
    private WebhookProcessingService webhookService;

    @Value("${webhook.myupdates.api-key:default-secret-key}")
    private String webhookApiKey;

    /**
     * Endpoint for receiving MyUpdates email notifications
     */
    @Operation(
        summary = "Receive MyUpdates email notification",
        description = """
            Receives Standard Bank MyUpdates email notifications forwarded from Zapier, Make.com, or Gmail Apps Script.

            The system will:
            1. Validate the API key and sender email
            2. Parse the email to extract transaction details
            3. Check for duplicates
            4. Attempt automatic matching to students
            5. Create payment records or flag for manual review

            **Authentication:** API key via X-API-Key header or in request body
            """,
        security = @SecurityRequirement(name = "webhookApiKey")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Notification accepted and queued for processing",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "accepted",
                      "message": "Notification queued for processing",
                      "email_id": "unique-id"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid API key",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "error": "Invalid API key"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid sender or missing required fields",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "error": "Invalid sender email. Must be from standardbank.co.za or sbsa.co.za"
                    }
                    """)
            )
        )
    })
    @PostMapping("/myupdates")
    public ResponseEntity<?> handleMyUpdatesNotification(
            @Parameter(description = "Webhook payload with email details", required = true,
                content = @Content(examples = @ExampleObject(value = """
                    {
                      "email_id": "unique-email-id",
                      "received_at": "2025-01-15T14:30:00Z",
                      "sender": "noreply@standardbank.co.za",
                      "subject": "Transaction Notification",
                      "body": "You have received a payment\\nDate: 15/01/2025\\nAmount: R 1,500.00\\nReference: STU-2025-001 January Fee\\nBalance: R 45,230.50",
                      "api_key": "your-secret-key"
                    }
                    """)))
            @Valid @RequestBody MyUpdatesWebhookPayload payload,
            @Parameter(description = "API key for authentication (alternative to body)")
            @RequestHeader(value = "X-API-Key", required = false) String headerApiKey) {

        log.info("Received MyUpdates webhook notification from: {}", payload.getSender());

        try {
            // Authenticate webhook request
            String apiKey = headerApiKey != null ? headerApiKey : payload.getApiKey();
            if (!isValidApiKey(apiKey)) {
                log.warn("Invalid API key in webhook request from: {}", payload.getSender());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "status", "error",
                        "error", "Invalid API key"
                    ));
            }

            // Validate sender email
            if (!isValidStandardBankSender(payload.getSender())) {
                log.warn("Invalid sender email: {}", payload.getSender());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "status", "error",
                        "error", "Invalid sender email. Must be from standardbank.co.za or sbsa.co.za"
                    ));
            }

            // Validate email body
            if (payload.getBody() == null || payload.getBody().trim().isEmpty()) {
                log.warn("Empty email body in webhook request");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "status", "error",
                        "error", "Email body is required"
                    ));
            }

            // Process notification asynchronously
            webhookService.processNotificationAsync(payload);

            log.info("MyUpdates notification queued for processing: {}", payload.getEmailId());

            return ResponseEntity.ok(Map.of(
                "status", "accepted",
                "message", "Notification queued for processing",
                "email_id", payload.getEmailId() != null ? payload.getEmailId() : "unknown"
            ));

        } catch (Exception e) {
            log.error("Error processing webhook notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "error", "Processing failed: " + e.getMessage()
                ));
        }
    }

    /**
     * Test endpoint to verify webhook configuration
     *
     * @param apiKey API key for authentication
     * @return Configuration status
     */
    @GetMapping("/myupdates/test")
    public ResponseEntity<?> testWebhook(@RequestParam(required = false) String apiKey,
                                         @RequestHeader(value = "X-API-Key", required = false) String headerApiKey) {

        String providedKey = headerApiKey != null ? headerApiKey : apiKey;

        if (!isValidApiKey(providedKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                    "status", "error",
                    "error", "Invalid API key"
                ));
        }

        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "message", "Webhook endpoint is configured correctly",
            "endpoint", "/api/webhook/myupdates",
            "method", "POST",
            "authentication", "Valid API key provided"
        ));
    }

    /**
     * Health check endpoint (no authentication required)
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "webhook-receiver",
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Get webhook statistics (requires authentication)
     */
    @GetMapping("/myupdates/stats")
    public ResponseEntity<?> getWebhookStats(
            @RequestParam(required = false) String apiKey,
            @RequestHeader(value = "X-API-Key", required = false) String headerApiKey) {

        String providedKey = headerApiKey != null ? headerApiKey : apiKey;

        if (!isValidApiKey(providedKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("status", "error", "error", "Invalid API key"));
        }

        Map<String, Object> stats = webhookService.getWebhookStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Validate API key
     */
    private boolean isValidApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }
        return webhookApiKey.equals(apiKey.trim());
    }

    /**
     * Validate that sender is from Standard Bank
     */
    private boolean isValidStandardBankSender(String sender) {
        if (sender == null) {
            return false;
        }

        String lowerSender = sender.toLowerCase();
        return lowerSender.contains("standardbank.co.za")
            || lowerSender.contains("sbsa.co.za")
            || lowerSender.contains("standard-bank")
            || lowerSender.contains("standardbank.com");
    }
}
