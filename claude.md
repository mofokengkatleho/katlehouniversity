# Project: ECD Payment Reconciliation System (MVP)

## Vision
An MVP web application for an Early Childhood Development (ECD) center that automates the process of tracking which children have paid monthly fees, using Standard Bank transaction data.
The goal is to remove manual payment tracking and allow accurate, auditable reconciliation and reporting — with potential to scale and resell to other ECD centers.

---

## 🎯 MVP Goal
The system must:
1. Store all registered students with unique student numbers, assigned fees, and personal details.
2. Receive and process transaction notifications from Standard Bank MyUpdates via email parsing.
3. Automatically match transactions to students based on student number in payment reference.
4. Generate clear monthly reports showing:
    - Paid students
    - Partially paid students
    - Owing students
    - Total collected vs total outstanding

---

## 🧠 Core Idea
When a parent pays via Standard Bank and uses the student number as reference,
the system receives a notification email from Standard Bank MyUpdates service.
The system automatically parses the email, extracts transaction details, matches the payment
to the student, and updates their account standing in real-time.
This removes manual data entry entirely and provides immediate payment confirmation.

---

## 👥 User Journeys

### 1. Student Registration (New Academic Year)
**Actor:** Admin
**Goal:** Register a new student and generate a unique student number
**Steps:**
1. Admin navigates to "Register Student" page
2. Admin enters student details (name, guardian info, grade/class)
3. System auto-generates unique student number (e.g., STU-2025-001)
4. Admin sets monthly fee amount
5. System saves student and displays confirmation with student number
6. Admin shares student number with parent for payment reference

**Technical Requirements:**
- Auto-increment student number per academic year
- Store: student_id, student_number, name, guardian_name, guardian_contact, monthly_fee, registration_date

---

### 2. Notification Webhook Setup
**Actor:** Admin (one-time setup)
**Goal:** Configure email forwarding to enable automatic transaction processing
**Steps:**
1. Admin enables Standard Bank MyUpdates in their banking app
2. Admin configures email alerts for all incoming transactions
3. Admin sets up email forwarding (Gmail, Zapier, Make.com, or custom script) to forward MyUpdates emails to the system webhook endpoint
4. System validates webhook configuration and confirms connectivity
5. Admin sees confirmation that notification processing is active

**Technical Requirements:**
- REST endpoint to receive transaction notification webhooks
- Email parsing logic to extract: date, amount, reference, balance from Standard Bank MyUpdates format
- Webhook authentication/security (API key or HMAC signature)
- Store raw notification for audit trail
- Handle duplicate notifications (idempotency)

---

### 3. Automatic Payment Processing
**Actor:** System (real-time)
**Goal:** Process incoming transaction notifications and match to students
**Steps:**
1. System receives webhook POST request with transaction notification
2. System parses email/notification body to extract transaction details
3. System validates transaction (amount > 0, valid date format)
4. System scans reference field for student number pattern (e.g., STU-2025-001)
5. If matched: System creates payment record and updates student account
6. If unmatched: System flags transaction for manual review
7. System sends confirmation/alert to admin dashboard

**Technical Requirements:**
- Real-time webhook processing (async for performance)
- Regex pattern matching for student numbers in reference field
- Duplicate detection (transaction_id or timestamp+amount hash)
- Payment tracking: payment_id, student_id, transaction_date, amount, reference, matched_automatically (boolean)
- Account standing calculation: total_paid, total_owed, balance
- Dashboard notification for unmatched transactions

---

### 4. Manual Transaction Review
**Actor:** Admin
**Goal:** Review and manually assign unmatched transactions
**Steps:**
1. Admin receives notification of unmatched transaction
2. Admin navigates to "Unmatched Transactions" page
3. Admin sees list of transactions that could not be auto-matched
4. Admin searches for student by name or student number
5. Admin manually assigns transaction to correct student
6. System creates payment record and updates account standing

**Technical Requirements:**
- Unmatched transaction queue with search/filter
- Fuzzy name matching suggestions (optional)
- Manual assignment interface with student search
- Audit trail for manual assignments (who assigned, when)

---

### 5. View Account Standing
**Actor:** Admin
**Goal:** Check payment status for individual or all students
**Steps:**
1. Admin navigates to "Students" page
2. Admin sees list of all students with current account standing
3. Admin can filter by: Paid/Owing/Partial, Grade, Month
4. Admin clicks student to view detailed payment history
5. System displays all payments, dates, amounts, and running balance

**Technical Requirements:**
- Dashboard view with student list and payment status indicators
- Color-coded status: Green (Paid), Yellow (Partial), Red (Owing)
- Individual student payment ledger showing all transactions
- Search by student number or name

---

### 6. Monthly Reporting
**Actor:** Admin
**Goal:** Generate monthly payment report for administration/audit
**Steps:**
1. Admin navigates to "Reports" page
2. Admin selects month and year
3. System generates report showing:
   - Total students registered
   - Students paid in full
   - Students with partial payments
   - Students owing
   - Total revenue collected
   - Total outstanding
4. Admin exports report as PDF or Excel

**Technical Requirements:**
- Monthly aggregation queries
- Export to PDF and Excel formats
- Include student-level breakdown with payment dates and amounts
- Summary statistics for board/management reporting

---

## ⚙️ Tech Stack

### Backend
- **Language:** Java 21
- **Framework:** Spring Boot 3 (Maven)
- **Database:** PostgreSQL (H2 for testing and development)
- **ORM:** Spring Data JPA
- **Security:** Basic JWT authentication for admin login + Webhook API key authentication
- **Notification Processing:**
  - REST Controller for webhook endpoint
  - Email/notification body parsing (regex-based)
  - Async processing with Spring @Async or message queue (optional)
- **Integration:** Email forwarding service (Gmail + Zapier/Make.com or custom Node.js/Python script)
- **Environment:** `application.yml` for webhook credentials and secrets
- **Testing:** JUnit 5 (JUPITER), MockMvc for webhook testing

### Frontend
- **Framework:** React (TypeScript, Vite)
- **Styling:** TailwindCSS
- **UI Framework:** shadcn/ui
- **State Management:** React Query for server state
- **Real-time Updates:** WebSocket or polling for live transaction notifications
- **API Calls:** Axios or Fetch API

---

## 📊 Data Model

### Student
```
student_id (PK, UUID)
student_number (UNIQUE, e.g., STU-2025-001)
first_name
last_name
gender (MALE, FEMALE, OTHER)
student_id_number (National/Birth ID number)
physical_address
allergies (TEXT, nullable)
guardian_name
guardian_contact
guardian_email
grade_class
monthly_fee (DECIMAL)
payment_day (1-31, day of month for committed payment)
registration_date
academic_year
status (ACTIVE, GRADUATED, WITHDRAWN)
```

### Payment
```
payment_id (PK, UUID)
student_id (FK)
transaction_date
amount (DECIMAL)
reference (transaction description)
payment_method (BANK_TRANSFER, CASH, OTHER)
matched_automatically (BOOLEAN)
created_at
verified_by (admin user_id, nullable)
```

### TransactionNotification (from MyUpdates webhook)
```
notification_id (PK, UUID)
received_at (TIMESTAMP)
raw_payload (TEXT, original email/webhook body)
transaction_date
description
amount (DECIMAL)
balance (DECIMAL)
reference (TEXT, extracted from notification)
matched_to_student_id (FK, nullable)
match_status (MATCHED, UNMATCHED, MANUAL)
processed (BOOLEAN)
processed_at (TIMESTAMP, nullable)
duplicate_check_hash (unique index on hash of transaction_date + amount + reference)
```

---

---

## 🔗 Email Forwarding Automation Options

### Option 1: Zapier/Make.com (Recommended for MVP)
**Pros:** No-code, quick setup, reliable
**Cons:** Monthly cost (~$20-30 USD)
**Setup:**
1. Create Gmail filter to identify Standard Bank MyUpdates emails
2. Create Zapier/Make.com automation:
   - Trigger: New email matching filter
   - Action: HTTP POST to webhook endpoint
   - Pass email subject, body, sender, date as JSON payload
3. Configure retry logic and error handling

### Option 2: Gmail + Google Apps Script (Free)
**Pros:** Free, full control, integrates with Google Workspace
**Cons:** Requires some JavaScript knowledge
**Setup:**
1. Create Gmail filter/label for MyUpdates emails
2. Write Google Apps Script to:
   - Check labeled emails every 5 minutes
   - Parse email content
   - POST to webhook endpoint
   - Mark email as processed
3. Set up time-based trigger

### Option 3: Custom Node.js/Python Script (Advanced)
**Pros:** Full control, can run on own server
**Cons:** Requires server hosting, maintenance
**Setup:**
1. Use IMAP client to monitor Gmail inbox
2. Parse incoming emails matching Standard Bank sender
3. Extract transaction details
4. POST to webhook endpoint
5. Deploy on VPS or cloud service (DigitalOcean, AWS, etc.)

### Webhook Payload Format
```json
{
  "email_id": "unique-email-id",
  "received_at": "2025-01-15T14:30:00Z",
  "sender": "noreply@standardbank.co.za",
  "subject": "Transaction Notification",
  "body": "You have received a payment\nDate: 15/01/2025...",
  "api_key": "your-webhook-secret-key"
}
```

---

## 📧 Standard Bank MyUpdates Email Format

### Sample Notification Email
Standard Bank MyUpdates emails typically contain:
- **Subject:** Transaction notification or account alert
- **Body:** Contains transaction details in plain text or HTML format
- **Key fields to extract:**
  - Transaction date and time
  - Transaction type (Credit/Debit)
  - Amount
  - Reference/Description (contains student number)
  - Account balance after transaction

### Email Parsing Strategy
```
1. Identify email sender (verify it's from Standard Bank)
2. Extract transaction details using regex patterns:
   - Date: Look for date patterns (DD/MM/YYYY, YYYY-MM-DD)
   - Amount: Look for currency amounts (R 1,500.00 or 1500.00)
   - Reference: Extract description/reference field
   - Balance: Extract new balance amount
3. Clean and normalize extracted data
4. Validate required fields are present
5. Create TransactionNotification record
```

### Example Email Body Pattern
```
You have received a payment
Date: 15/01/2025
Amount: R 1,500.00
Reference: STU-2025-001 January Fee
Balance: R 45,230.50
```

### Regex Patterns for Parsing
- Student Number: `STU-\d{4}-\d{3}`
- Amount: `R?\s*[\d,]+\.\d{2}`
- Date: `\d{2}/\d{2}/\d{4}` or `\d{4}-\d{2}-\d{2}`
- Reference Line: `Reference:\s*(.+)`

---

## 🔄 System Workflows

### Student Number Generation
- Format: `STU-{YEAR}-{SEQUENCE}`
- Example: `STU-2025-001`, `STU-2025-002`
- Sequence resets each academic year
- Generated automatically on student creation

### MyUpdates Notification Processing Flow
1. Parent makes payment to Standard Bank account with student number in reference
2. Standard Bank sends MyUpdates email notification (within 30-60 seconds)
3. Email forwarding service (Gmail filter + Zapier/Make.com) triggers webhook POST to system
4. System receives webhook and validates authenticity (API key check)
5. System parses notification body:
   - Extract transaction date, amount, reference, balance
   - Generate duplicate check hash (date + amount + reference)
6. System checks for duplicates:
   - If hash exists, skip processing (idempotency)
   - If new, store in `TransactionNotification` table
7. Matching engine runs:
   - Search for student number pattern (STU-YYYY-NNN) in reference
   - If found, create Payment record and link to student
   - If not found, flag as UNMATCHED for manual review
8. System updates student account standing (if matched)
9. System sends real-time notification to admin dashboard
10. Admin reviews unmatched transactions and manually assigns if needed

### Payment Matching Logic
1. **Primary:** Regex match for `STU-YYYY-NNN` pattern in description
2. **Fallback:** Fuzzy string match on student name in description (threshold 80%)
3. **Manual:** Admin can manually assign unmatched transaction to student

### Account Standing Calculation
For each student, calculate:
- **Total Expected:** `monthly_fee × months_in_year`
- **Total Paid:** `SUM(payments.amount)` for current academic year
- **Balance:** `Total Paid - Total Expected`
- **Status:**
  - Paid: `Balance >= 0`
  - Partial: `Balance < 0 AND Total Paid > 0`
  - Owing: `Total Paid = 0`

---

## 🎯 MVP Features Priority

### Phase 1 (Core MVP)
1. Student registration with auto-generated student numbers
2. Webhook endpoint for receiving transaction notifications
3. MyUpdates email notification parsing
4. Automatic payment matching by student number
5. Student list with account standing dashboard
6. Individual student payment history view
7. Unmatched transaction review interface
8. Basic monthly report (text/table view)

### Phase 2 (Enhancements)
1. Email forwarding automation setup guide/script
2. Manual transaction assignment with fuzzy name matching
3. Real-time dashboard updates (WebSocket/polling)
4. PDF/Excel report export
5. Payment reminder notifications (SMS/Email)
6. Bulk student import (CSV upload for registration)
7. Duplicate transaction detection and handling

### Phase 3 (Advanced)
1. Multi-year student tracking (graduation, re-enrollment)
2. Fee structure variations (scholarships, discounts)
3. Multi-currency support
4. Role-based access (Admin, Accountant, Viewer)
5. Audit trail and activity logs
6. Mobile app for parent payment confirmation viewing
