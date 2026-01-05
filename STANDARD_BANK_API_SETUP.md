# Standard Bank Business API Integration Guide

This guide explains how to integrate the real Standard Bank Business API once you have obtained the necessary credentials.

## Current Status

The system currently uses a **stub implementation** that generates mock transaction data for testing. This allows you to develop and test the system without needing actual bank API access.

## Prerequisites

Before integrating with the Standard Bank API, you need:

1. **Standard Bank Business Account**
2. **Developer Portal Access**
3. **OAuth2 Client Credentials** (Client ID and Client Secret)
4. **API Documentation** from Standard Bank

## Step 1: Register for API Access

1. Visit the Standard Bank Developer Portal: `https://developer.standardbank.co.za/`
2. Create a developer account or sign in with your business banking credentials
3. Navigate to the API products section
4. Request access to the **Business Banking API** (or equivalent transaction API)
5. Create an application to obtain:
   - Client ID
   - Client Secret
   - Token URL
   - Base API URL

## Step 2: Configure Application

### Backend Configuration

Edit `backend/src/main/resources/application.yml`:

```yaml
standardbank:
  api:
    base-url: https://api.standardbank.co.za/business  # Replace with actual URL
    client-id: ${STANDARDBANK_CLIENT_ID}
    client-secret: ${STANDARDBANK_CLIENT_SECRET}
    token-url: https://api.standardbank.co.za/oauth2/token  # Replace with actual URL
    enabled: true  # Enable the API integration
```

### Environment Variables

Set environment variables (DO NOT commit these to version control):

```bash
export STANDARDBANK_CLIENT_ID="your-actual-client-id"
export STANDARDBANK_CLIENT_SECRET="your-actual-client-secret"
```

Or in production, use your hosting platform's environment variable configuration.

## Step 3: Implement OAuth2 Authentication

Update `backend/src/main/java/com/katlehouniversity/ecd/integration/StandardBankApiClient.java`:

### 3.1 Add Dependencies

Ensure these dependencies are in `pom.xml` (already included):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

### 3.2 Implement Token Acquisition

Replace the stub `getAccessToken()` method:

```java
private String getAccessToken() {
    try {
        // Build OAuth2 token request
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("scope", "read:transactions");  // Adjust scope as needed

        HttpEntity<MultiValueMap<String, String>> request =
            new HttpEntity<>(body, headers);

        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
            tokenUrl,
            request,
            TokenResponse.class
        );

        if (response.getBody() != null) {
            return response.getBody().getAccessToken();
        }

        throw new RuntimeException("Failed to obtain access token");

    } catch (Exception e) {
        log.error("Error obtaining access token", e);
        throw new RuntimeException("OAuth2 authentication failed", e);
    }
}

// Token response DTO
@Data
private static class TokenResponse {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private Integer expiresIn;
}
```

### 3.3 Implement Transaction Fetching

Replace the `fetchTransactions()` method:

```java
public List<Transaction> fetchTransactions(LocalDate startDate, LocalDate endDate) {
    log.info("Fetching transactions from {} to {}", startDate, endDate);

    if (!apiEnabled) {
        log.warn("Standard Bank API is disabled. Using mock data.");
        return generateMockTransactions(startDate, endDate);
    }

    try {
        String accessToken = getAccessToken();

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Build API URL with query parameters
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromHttpUrl(baseUrl + "/transactions")
            .queryParam("startDate", startDate.toString())
            .queryParam("endDate", endDate.toString())
            .queryParam("accountId", getAccountId());  // Configure account ID

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<StandardBankTransactionResponse> response =
            restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                StandardBankTransactionResponse.class
            );

        if (response.getBody() != null && response.getBody().getTransactions() != null) {
            return mapStandardBankTransactions(response.getBody().getTransactions());
        }

        log.warn("No transactions returned from Standard Bank API");
        return new ArrayList<>();

    } catch (Exception e) {
        log.error("Error fetching transactions from Standard Bank API", e);
        throw new RuntimeException("Failed to fetch transactions", e);
    }
}
```

### 3.4 Map API Response to Internal Model

Create DTOs for Standard Bank's response format:

```java
@Data
private static class StandardBankTransactionResponse {
    private List<StandardBankTransaction> transactions;
    private Pagination pagination;
}

@Data
private static class StandardBankTransaction {
    private String transactionId;
    private String accountId;
    private BigDecimal amount;
    private String currency;
    private String transactionDate;
    private String description;
    private String referenceNumber;
    private String debtorName;
    private String debtorAccount;
    private String type;
}

@Data
private static class Pagination {
    private Integer page;
    private Integer totalPages;
    private Integer totalRecords;
}

private List<Transaction> mapStandardBankTransactions(
        List<StandardBankTransaction> apiTransactions) {

    return apiTransactions.stream()
        .filter(t -> "CREDIT".equals(t.getType()))  // Only process credits
        .map(this::mapTransaction)
        .collect(Collectors.toList());
}

private Transaction mapTransaction(StandardBankTransaction apiTx) {
    return Transaction.builder()
        .bankReference(apiTx.getTransactionId())
        .amount(apiTx.getAmount())
        .transactionDate(LocalDate.parse(apiTx.getTransactionDate()))
        .paymentReference(extractPaymentReference(apiTx.getDescription()))
        .description(apiTx.getDescription())
        .senderName(apiTx.getDebtorName())
        .senderAccount(apiTx.getDebtorAccount())
        .status(Transaction.TransactionStatus.UNMATCHED)
        .type(Transaction.TransactionType.CREDIT)
        .rawData(toJson(apiTx))  // Store original data
        .build();
}

private String extractPaymentReference(String description) {
    // Implement logic to extract payment reference from description
    // This depends on how parents format their payment references
    // Example: Extract first word in caps, or match pattern

    if (description == null) return null;

    // Simple extraction: get first uppercase word
    Pattern pattern = Pattern.compile("[A-Z]{5,}");
    Matcher matcher = pattern.matcher(description);

    if (matcher.find()) {
        return matcher.group();
    }

    return description.trim().split("\\s+")[0];
}
```

## Step 4: Test the Integration

### 4.1 Test OAuth2 Authentication

Create a test endpoint to verify authentication:

```java
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final StandardBankApiClient standardBankApiClient;

    @GetMapping("/test-bank-auth")
    public ResponseEntity<String> testBankAuth() {
        try {
            List<Transaction> transactions = standardBankApiClient.fetchTransactions(
                LocalDate.now().minusDays(7),
                LocalDate.now()
            );
            return ResponseEntity.ok("Success! Fetched " + transactions.size() + " transactions");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
```

### 4.2 Test Transaction Fetching

1. Start the backend with real credentials configured
2. Call the test endpoint: `GET http://localhost:8080/api/test/test-bank-auth`
3. Check logs for any errors
4. Verify transactions are being fetched correctly

### 4.3 Test Payment Matching

1. Add a test child with a known payment reference
2. Make a test payment through Standard Bank with that reference
3. Wait for the scheduler to run or manually trigger sync:
   ```bash
   curl -X POST http://localhost:8080/api/transactions/match-all \
     -H "Authorization: Bearer YOUR_JWT_TOKEN"
   ```
4. Check if the payment was matched correctly

## Step 5: Error Handling

Add proper error handling for common scenarios:

```java
// Handle rate limiting
if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
    log.warn("Rate limit exceeded, waiting before retry...");
    Thread.sleep(5000);
    return fetchTransactions(startDate, endDate);  // Retry
}

// Handle token expiration
if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
    log.info("Token expired, refreshing...");
    this.cachedToken = null;  // Clear cached token
    return fetchTransactions(startDate, endDate);  // Retry with new token
}

// Handle service unavailable
if (response.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
    log.error("Standard Bank API is currently unavailable");
    // Fall back to manual processing or retry later
}
```

## Step 6: Add Token Caching

Optimize by caching the access token:

```java
private String cachedToken;
private LocalDateTime tokenExpiry;

private String getAccessToken() {
    // Return cached token if still valid
    if (cachedToken != null && tokenExpiry != null &&
        LocalDateTime.now().isBefore(tokenExpiry.minusMinutes(5))) {
        return cachedToken;
    }

    // Fetch new token
    TokenResponse tokenResponse = fetchNewToken();
    cachedToken = tokenResponse.getAccessToken();
    tokenExpiry = LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn());

    return cachedToken;
}
```

## Step 7: Production Deployment

### Security Checklist

- [ ] Store credentials in environment variables, NOT in code
- [ ] Enable HTTPS for all API communications
- [ ] Use secrets management system (AWS Secrets Manager, Azure Key Vault, etc.)
- [ ] Implement request logging (but redact sensitive data)
- [ ] Set up monitoring and alerts for API failures
- [ ] Configure appropriate timeout values
- [ ] Implement retry logic with exponential backoff
- [ ] Test failover scenarios

### Monitoring

Add logging for:
- Successful API calls
- Failed API calls with error details
- Number of transactions fetched
- Number of transactions matched
- OAuth2 token refresh events

## Troubleshooting

### Authentication Fails

**Error**: `401 Unauthorized`

**Solutions**:
- Verify Client ID and Client Secret are correct
- Check if credentials have expired or been revoked
- Ensure correct scope is requested
- Verify token URL is correct

### No Transactions Returned

**Error**: Empty transaction list

**Solutions**:
- Verify date range is correct
- Check if account ID is configured correctly
- Ensure account has transactions in the specified period
- Review API documentation for required parameters

### SSL/TLS Errors

**Error**: Certificate validation failed

**Solutions**:
- Ensure Standard Bank's SSL certificate is trusted
- Update Java truststore if needed
- Check for proxy/firewall issues

### Rate Limiting

**Error**: `429 Too Many Requests`

**Solutions**:
- Implement rate limiting in your scheduler
- Add delays between requests
- Request higher rate limits from Standard Bank

## Support

For Standard Bank API issues:
- **Developer Portal**: https://developer.standardbank.co.za/
- **Support Email**: apisupport@standardbank.co.za (example)
- **Documentation**: Check the developer portal for latest API docs

For application issues:
- Check logs in `backend/logs/`
- Enable debug logging: `logging.level.com.katlehouniversity.ecd=DEBUG`
- Contact your development team

## Next Steps

Once integration is complete:

1. Monitor for a few days to ensure stability
2. Adjust payment reference matching logic based on actual data
3. Train staff on how to instruct parents to include references
4. Set up alerts for unmatched transactions
5. Create reporting on matching accuracy
6. Consider implementing webhook support if available

## Additional Resources

- [OAuth 2.0 Specification](https://oauth.net/2/)
- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [RestTemplate Documentation](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/client/RestTemplate.html)
