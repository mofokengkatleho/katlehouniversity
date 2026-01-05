# ECD Payment Reconciliation System - Backend

Spring Boot REST API for automated payment tracking and reconciliation.

## Technology Stack

- **Java**: 21
- **Framework**: Spring Boot 3.2.0
- **Build Tool**: Maven
- **Database**: PostgreSQL (H2 for dev/testing)
- **ORM**: Spring Data JPA
- **Security**: Spring Security + JWT
- **Testing**: JUnit 5 (Jupiter), Mockito
- **Documentation**: Spring REST Docs (planned)

## Project Structure

```
src/
├── main/
│   ├── java/com/katlehouniversity/ecd/
│   │   ├── config/
│   │   │   ├── DataInitializer.java          # Creates default admin user
│   │   │   └── SecurityConfig.java           # Spring Security config
│   │   ├── controller/
│   │   │   ├── AuthController.java           # Authentication endpoints
│   │   │   ├── ChildController.java          # Child CRUD operations
│   │   │   ├── ReportController.java         # Monthly reports
│   │   │   └── TransactionController.java    # Transaction management
│   │   ├── dto/
│   │   │   ├── AuthDto.java                  # Auth request/response DTOs
│   │   │   ├── ChildDto.java                 # Child DTO
│   │   │   └── MonthlyReportDto.java         # Report DTOs
│   │   ├── entity/
│   │   │   ├── Child.java                    # Child entity
│   │   │   ├── Payment.java                  # Payment entity
│   │   │   ├── Transaction.java              # Transaction entity
│   │   │   └── User.java                     # User entity
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java   # Global error handling
│   │   │   └── ResourceNotFoundException.java
│   │   ├── integration/
│   │   │   └── StandardBankApiClient.java    # Standard Bank API client
│   │   ├── repository/
│   │   │   ├── ChildRepository.java
│   │   │   ├── PaymentRepository.java
│   │   │   ├── TransactionRepository.java
│   │   │   └── UserRepository.java
│   │   ├── scheduler/
│   │   │   └── TransactionSyncScheduler.java # Daily sync job
│   │   ├── security/
│   │   │   ├── CustomUserDetailsService.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   └── JwtUtil.java
│   │   └── service/
│   │       ├── AuthService.java              # Authentication logic
│   │       ├── ChildService.java             # Child management
│   │       ├── PaymentMatchingService.java   # Core matching logic
│   │       ├── ReportService.java            # Report generation
│   │       └── TransactionSyncService.java   # Transaction sync
│   └── resources/
│       └── application.yml                    # Configuration
└── test/
    └── java/com/katlehouniversity/ecd/
        └── service/
            └── ChildServiceTest.java          # Example unit test
```

## Running the Application

### Development Mode (H2 Database)

```bash
mvn spring-boot:run
```

The application will use the `dev` profile by default, which:
- Uses H2 in-memory database
- Enables H2 console at `http://localhost:8080/h2-console`
- Creates database schema on startup
- Shows SQL queries in logs

### Production Mode (PostgreSQL)

1. Create PostgreSQL database:
   ```sql
   CREATE DATABASE ecd_payment_db;
   ```

2. Set environment variables:
   ```bash
   export DB_USERNAME=your_db_user
   export DB_PASSWORD=your_db_password
   export JWT_SECRET=your-secure-256-bit-secret
   ```

3. Run with production profile:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=prod
   ```

## Configuration

### Database Configuration

#### Development (H2)
```yaml
spring:
  profiles:
    active: dev
  datasource:
    url: jdbc:h2:mem:ecd_payment_db
    driver-class-name: org.h2.Driver
    username: sa
    password:
```

#### Production (PostgreSQL)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ecd_payment_db
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
```

### JWT Configuration

```yaml
jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key}
  expiration: 86400000  # 24 hours
```

### Standard Bank API Configuration

```yaml
standardbank:
  api:
    base-url: https://api.standardbank.co.za/business
    client-id: ${STANDARDBANK_CLIENT_ID:your-client-id}
    client-secret: ${STANDARDBANK_CLIENT_SECRET:your-client-secret}
    token-url: https://api.standardbank.co.za/oauth2/token
    enabled: false  # Set to true when credentials are available
```

### Scheduler Configuration

```yaml
scheduler:
  transaction-sync:
    cron: "0 0 1 * * ?"  # Run daily at 1 AM
    enabled: true
```

## API Endpoints

### Authentication
- `POST /api/auth/login` - Admin login
- `POST /api/auth/register` - Create new admin user

### Children
- `GET /api/children` - Get all children
- `GET /api/children/{id}` - Get child by ID
- `GET /api/children/reference/{reference}` - Get child by payment reference
- `GET /api/children/search?name={name}` - Search children by name
- `POST /api/children` - Create new child
- `PUT /api/children/{id}` - Update child
- `DELETE /api/children/{id}` - Deactivate child

### Reports
- `GET /api/reports/monthly/current` - Current month report
- `GET /api/reports/monthly?month={month}&year={year}` - Specific month report

### Transactions
- `GET /api/transactions/unmatched` - Get unmatched transactions
- `GET /api/transactions/unmatched/count` - Count unmatched transactions
- `POST /api/transactions/match-all` - Match all unmatched transactions
- `POST /api/transactions/{id}/match` - Manually match a transaction

## Database Schema

### Children Table
```sql
CREATE TABLE children (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    payment_reference VARCHAR(50) UNIQUE NOT NULL,
    monthly_fee DECIMAL(10,2) NOT NULL,
    parent_phone VARCHAR(20),
    parent_email VARCHAR(100),
    parent_name VARCHAR(200),
    date_of_birth DATE,
    enrollment_date DATE,
    active BOOLEAN DEFAULT TRUE,
    notes VARCHAR(500),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### Payments Table
```sql
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    child_id BIGINT NOT NULL REFERENCES children(id),
    transaction_id BIGINT REFERENCES transactions(id),
    payment_month INTEGER NOT NULL,
    payment_year INTEGER NOT NULL,
    amount_paid DECIMAL(10,2) NOT NULL,
    expected_amount DECIMAL(10,2),
    payment_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    transaction_reference VARCHAR(100),
    notes VARCHAR(500),
    created_at TIMESTAMP,
    UNIQUE(child_id, payment_month, payment_year)
);
```

### Transactions Table
```sql
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    bank_reference VARCHAR(100) UNIQUE NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    transaction_date DATE NOT NULL,
    payment_reference VARCHAR(200),
    description VARCHAR(200),
    sender_name VARCHAR(100),
    sender_account VARCHAR(50),
    status VARCHAR(20) NOT NULL,
    type VARCHAR(20) NOT NULL,
    matching_notes VARCHAR(500),
    manually_matched BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    matched_at TIMESTAMP,
    raw_data TEXT
);
```

## Business Logic

### Payment Matching Algorithm

1. **Transaction Fetch**: Daily scheduler fetches new transactions from Standard Bank API
2. **Reference Extraction**: Extract payment reference from transaction
3. **Child Lookup**: Find child with matching payment reference (case-insensitive)
4. **Payment Creation**: Create payment record for current month
5. **Status Update**: Mark transaction as matched

### Manual Matching

For transactions without clear references or multiple matches:
1. Admin reviews unmatched transactions
2. Selects correct child manually
3. System creates payment record
4. Marks transaction as manually matched

## Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test
```bash
mvn test -Dtest=ChildServiceTest
```

### Test Coverage
```bash
mvn jacoco:report
```
Report available at: `target/site/jacoco/index.html`

## Building for Production

### Create JAR
```bash
mvn clean package -DskipTests
```

### Run JAR
```bash
java -jar target/ecd-payment-reconciliation-0.0.1-SNAPSHOT.jar
```

### Docker Deployment (Optional)

Create `Dockerfile`:
```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run:
```bash
docker build -t ecd-payment-api .
docker run -p 8080:8080 \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=secret \
  -e JWT_SECRET=your-secret \
  ecd-payment-api
```

## Security Considerations

1. **Change Default Credentials**: Update admin password immediately after first login
2. **JWT Secret**: Use a strong, random 256-bit key in production
3. **Database Credentials**: Never commit credentials to version control
4. **HTTPS**: Always use HTTPS in production
5. **CORS**: Configure allowed origins appropriately

## Standard Bank API Integration

Current implementation is a stub. To integrate:

1. **Get Credentials**: Register at Standard Bank Developer Portal
2. **Implement OAuth2**: Update `StandardBankApiClient.java` with:
   - Token acquisition
   - Refresh token handling
   - API endpoint calls
3. **Parse Responses**: Map Standard Bank transaction format to our `Transaction` entity
4. **Enable**: Set `standardbank.api.enabled=true`

## Troubleshooting

### Application won't start
- Check Java version: `java -version`
- Verify port 8080 is available: `lsof -i :8080`
- Check database connection settings

### Tests failing
- Ensure H2 dependency is available
- Clear Maven cache: `mvn clean`
- Check test logs: `target/surefire-reports/`

### JWT errors
- Verify JWT secret length (minimum 256 bits)
- Check token expiration settings
- Ensure time synchronization on server

## Performance Optimization

- Use pagination for large datasets
- Add database indexes on frequently queried fields
- Enable second-level cache for read-heavy entities
- Monitor query performance with SQL logging

## Maintenance

### Database Migrations

For production, use Flyway or Liquibase for schema versioning:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

### Monitoring

Add Spring Boot Actuator for health checks:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Endpoints:
- `/actuator/health` - Application health
- `/actuator/metrics` - Application metrics
- `/actuator/info` - Application info

## Contributors

Development team at Katlego University
