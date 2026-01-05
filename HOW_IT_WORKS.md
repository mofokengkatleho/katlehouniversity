# How the ECD Payment Reconciliation System Works
## Technical Deep Dive & Implementation Guide

---

## Table of Contents
1. [System Overview](#system-overview)
2. [Core Workflows](#core-workflows)
3. [Component Architecture](#component-architecture)
4. [Payment Matching Algorithm](#payment-matching-algorithm)
5. [Database Design](#database-design)
6. [API Specification](#api-specification)
7. [Security Implementation](#security-implementation)
8. [Deployment Architecture](#deployment-architecture)
9. [Troubleshooting Guide](#troubleshooting-guide)

---

## System Overview

### What Problem Does It Solve?

ECD centers receive payments from parents via bank transfer. Currently, administrators must:
1. Manually log into their bank account
2. Download transaction statements
3. Cross-reference each transaction with their child registry
4. Update payment records in a spreadsheet
5. Identify which children have paid and which haven't

**This takes 4-6 hours per week and is error-prone.**

### How Does This System Solve It?

The system automates this entire process:

```
┌─────────────────────────────────────────────────────────────────┐
│                    BEFORE (Manual Process)                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Parent pays → 2. Bank receives → 3. Admin logs in →       │
│  4. Download CSV → 5. Open Excel → 6. Match manually →         │
│  7. Update spreadsheet → 8. Generate report → 9. Follow up     │
│                                                                 │
│  Time: 5-6 hours/week                                          │
│  Accuracy: 85-90%                                              │
│  Real-time visibility: NO                                      │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    AFTER (Automated System)                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Parent pays → 2. Bank receives → 3. System syncs →        │
│  4. Auto-match → 5. Dashboard updates → 6. Admin reviews      │
│                                                                 │
│  Time: 30 minutes/week                                         │
│  Accuracy: 95%+                                                │
│  Real-time visibility: YES                                     │
└─────────────────────────────────────────────────────────────────┘
```

---

## Core Workflows

### Workflow 1: Child Registration

**Actors**: ECD Administrator
**Frequency**: As needed (new enrollments)
**Duration**: 2-3 minutes per child

```
┌─────────────────────────────────────────────────────────────────┐
│                     Child Registration Flow                     │
└─────────────────────────────────────────────────────────────────┘

┌──────────┐        ┌──────────┐        ┌──────────┐
│  Admin   │        │ Frontend │        │ Backend  │
└────┬─────┘        └────┬─────┘        └────┬─────┘
     │                   │                   │
     │ 1. Fill form      │                   │
     │──────────────────>│                   │
     │   - First name    │                   │
     │   - Last name     │                   │
     │   - Monthly fee   │                   │
     │   - Payment ref   │                   │
     │                   │                   │
     │                   │ 2. POST /api/children
     │                   │──────────────────>│
     │                   │   + Validate data │
     │                   │   + Check duplicate ref
     │                   │   + Save to DB    │
     │                   │                   │
     │                   │ 3. Return child   │
     │                   │<──────────────────│
     │                   │                   │
     │ 4. Show success + │                   │
     │    payment ref    │                   │
     │<──────────────────│                   │
     │                   │                   │
     │ 5. Share ref with │                   │
     │    parent         │                   │
     │                   │                   │
```

**Key Validation Rules**:
- Payment reference must be unique
- Monthly fee must be positive number
- First name and last name required
- Email and phone validated (if provided)

**Database Changes**:
```sql
INSERT INTO children (
    first_name,
    last_name,
    payment_reference,
    monthly_fee,
    parent_email,
    active,
    created_at
) VALUES (
    'Thabo',
    'Molefe',
    'THABO_MOLEFE',
    500.00,
    'parent@example.com',
    true,
    NOW()
);
```

---

### Workflow 2: Automatic Transaction Sync

**Actors**: System Scheduler (automated)
**Frequency**: Daily at 1:00 AM
**Duration**: 30 seconds - 2 minutes

```
┌─────────────────────────────────────────────────────────────────┐
│              Daily Transaction Sync Workflow                    │
└─────────────────────────────────────────────────────────────────┘

     1:00 AM
        │
        ▼
┌────────────────────┐
│ Scheduler triggers │
│ TransactionSync-   │
│ Scheduler.java     │
└────────┬───────────┘
         │
         ▼
┌────────────────────────────────────────────────────────────────┐
│ Is sync enabled?                                               │
│ (scheduler.transaction-sync.enabled = true)                    │
└─────────────┬──────────────────────────────────────────────────┘
              │ YES
              ▼
┌─────────────────────────────────────────────────────────────────┐
│ TransactionSyncService.syncTransactions()                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Step 1: Authenticate with Standard Bank                       │
│  ┌──────────────────────────────────────────────────┐          │
│  │ GET https://api.standardbank.co.za/oauth2/token  │          │
│  │ Body: {                                          │          │
│  │   grant_type: "client_credentials",              │          │
│  │   client_id: "...",                              │          │
│  │   client_secret: "..."                           │          │
│  │ }                                                │          │
│  │                                                  │          │
│  │ Response: {                                      │          │
│  │   access_token: "eyJhbGci...",                   │          │
│  │   expires_in: 3600                               │          │
│  │ }                                                │          │
│  └──────────────────────────────────────────────────┘          │
│                                                                 │
│  Step 2: Fetch yesterday's transactions                        │
│  ┌──────────────────────────────────────────────────┐          │
│  │ GET /business/accounts/{accountId}/transactions  │          │
│  │ Headers: { Authorization: "Bearer ..." }         │          │
│  │ Params: {                                        │          │
│  │   fromDate: "2025-11-17",                        │          │
│  │   toDate: "2025-11-17"                           │          │
│  │ }                                                │          │
│  │                                                  │          │
│  │ Response: {                                      │          │
│  │   transactions: [                                │          │
│  │     {                                            │          │
│  │       reference: "TRX123456",                    │          │
│  │       amount: 500.00,                            │          │
│  │       date: "2025-11-17",                        │          │
│  │       paymentReference: "THABO_MOLEFE",          │          │
│  │       senderName: "John Doe"                     │          │
│  │     },                                           │          │
│  │     ...                                          │          │
│  │   ]                                              │          │
│  │ }                                                │          │
│  └──────────────────────────────────────────────────┘          │
│                                                                 │
│  Step 3: Save each transaction to database                     │
│  ┌──────────────────────────────────────────────────┐          │
│  │ FOR EACH transaction:                            │          │
│  │   - Check if already exists (by bank reference)  │          │
│  │   - If new: Save to transactions table           │          │
│  │   - Set status = UNMATCHED                       │          │
│  │   - Store raw JSON for debugging                 │          │
│  └──────────────────────────────────────────────────┘          │
│                                                                 │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│ PaymentMatchingService.matchAllUnmatchedTransactions()         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Step 1: Find all UNMATCHED transactions                       │
│  Step 2: For each transaction, attempt to match                │
│  Step 3: Update transaction status                             │
│  Step 4: Create payment records                                │
│                                                                 │
│  (See "Payment Matching Algorithm" section for details)        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                       │
                       ▼
               ┌───────────────┐
               │ Sync complete │
               │ Log summary:  │
               │ - 15 fetched  │
               │ - 12 matched  │
               │ - 3 unmatched │
               └───────────────┘
```

**Error Handling**:
- **Network failure**: Retry 3 times with exponential backoff
- **Authentication failure**: Log error, alert admin, skip sync
- **Duplicate transaction**: Skip silently (already processed)
- **Invalid data**: Log warning, save transaction as-is

---

### Workflow 3: Payment Matching (Automatic)

**Actors**: System (triggered after sync)
**Frequency**: After each sync + on-demand
**Duration**: 1-2 seconds per transaction

```
┌─────────────────────────────────────────────────────────────────┐
│              Automatic Payment Matching Logic                   │
└─────────────────────────────────────────────────────────────────┘

Input: Transaction {
  id: 456,
  bankReference: "TRX123456",
  amount: 500.00,
  transactionDate: "2025-11-17",
  paymentReference: "THABO_MOLEFE",
  senderName: "John Doe"
}

┌─────────────────────────────────────────────────────────────────┐
│ Step 1: Validate payment reference exists                      │
└─────────────────────────────────────────────────────────────────┘
         │
         ├─ NO → Skip, leave as UNMATCHED
         │
         └─ YES
              │
              ▼
┌─────────────────────────────────────────────────────────────────┐
│ Step 2: Search for child by payment reference                  │
│                                                                 │
│ SQL:                                                            │
│ SELECT * FROM children                                          │
│ WHERE LOWER(payment_reference) = LOWER('THABO_MOLEFE')         │
│   AND active = true                                             │
└─────────────────────────────────────────────────────────────────┘
         │
         ├─ NOT FOUND → Skip, leave as UNMATCHED
         │
         └─ FOUND: Child { id: 123, name: "Thabo Molefe", ... }
              │
              ▼
┌─────────────────────────────────────────────────────────────────┐
│ Step 3: Determine payment month/year                            │
│                                                                 │
│ transactionDate = 2025-11-17                                    │
│ → month = 11                                                    │
│ → year = 2025                                                   │
└─────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ Step 4: Check if payment already exists for this month         │
│                                                                 │
│ SQL:                                                            │
│ SELECT * FROM payments                                          │
│ WHERE child_id = 123                                            │
│   AND payment_month = 11                                        │
│   AND payment_year = 2025                                       │
└─────────────────────────────────────────────────────────────────┘
         │
         ├─ EXISTS → Update existing payment (add to amount)
         │
         └─ NOT EXISTS → Create new payment
              │
              ▼
┌─────────────────────────────────────────────────────────────────┐
│ Step 5: Create payment record                                  │
│                                                                 │
│ Payment {                                                       │
│   childId: 123,                                                 │
│   transactionId: 456,                                           │
│   paymentMonth: 11,                                             │
│   paymentYear: 2025,                                            │
│   amountPaid: 500.00,                                           │
│   expectedAmount: 500.00, (from child.monthlyFee)               │
│   paymentDate: 2025-11-17,                                      │
│   transactionReference: "TRX123456",                            │
│   paymentMethod: BANK_TRANSFER,                                 │
│   status: PAID (if amountPaid >= expectedAmount)                │
│ }                                                               │
│                                                                 │
│ SQL:                                                            │
│ INSERT INTO payments (...) VALUES (...)                         │
└─────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ Step 6: Update transaction status                              │
│                                                                 │
│ SQL:                                                            │
│ UPDATE transactions                                             │
│ SET status = 'MATCHED',                                         │
│     matching_notes = 'Matched to Thabo Molefe',                 │
│     matched_at = NOW()                                          │
│ WHERE id = 456                                                  │
└─────────────────────────────────────────────────────────────────┘
         │
         ▼
    ┌────────┐
    │ SUCCESS│
    └────────┘
```

**Matching Success Rate**:
- **Exact match**: 90-95% (parent uses correct reference)
- **No reference**: 3-5% (parent forgets reference)
- **Wrong reference**: 1-2% (parent uses wrong name)

**Unmatched Scenarios**:
1. **Parent forgets reference**: Transaction saved, admin matches manually
2. **Typo in reference**: "THABBO_MOLEFE" vs "THABO_MOLEFE" → Unmatched (Phase 2: fuzzy matching)
3. **New child not registered**: Reference for child not in system
4. **Test payments**: Small amounts for testing

---

### Workflow 4: Manual Transaction Matching

**Actors**: ECD Administrator
**Frequency**: As needed (for unmatched transactions)
**Duration**: 1-2 minutes per transaction

```
┌─────────────────────────────────────────────────────────────────┐
│              Manual Transaction Matching Flow                   │
└─────────────────────────────────────────────────────────────────┘

Scenario: Parent forgot to include payment reference

┌──────────┐        ┌──────────┐        ┌──────────┐
│  Admin   │        │ Frontend │        │ Backend  │
└────┬─────┘        └────┬─────┘        └────┬─────┘
     │                   │                   │
     │ 1. See alert:     │                   │
     │    "3 unmatched   │                   │
     │     transactions" │                   │
     │                   │                   │
     │ 2. Click alert    │                   │
     │──────────────────>│                   │
     │                   │                   │
     │                   │ 3. GET /api/transactions/unmatched
     │                   │──────────────────>│
     │                   │                   │
     │                   │ 4. Return list    │
     │                   │<──────────────────│
     │                   │                   │
     │ 5. Show unmatched │                   │
     │    transactions:  │                   │
     │    ┌────────────┐ │                   │
     │    │ R500.00    │ │                   │
     │    │ 2025-11-17 │ │                   │
     │    │ John Doe   │ │                   │
     │    │ No ref     │ │                   │
     │    │ [Match]    │ │                   │
     │    └────────────┘ │                   │
     │<──────────────────│                   │
     │                   │                   │
     │ 6. Click [Match]  │                   │
     │──────────────────>│                   │
     │                   │                   │
     │ 7. Show modal:    │                   │
     │    ┌────────────┐ │                   │
     │    │ Select     │ │                   │
     │    │ Child:     │ │                   │
     │    │ [dropdown] │ │                   │
     │    │ Month: Nov │ │                   │
     │    │ Year: 2025 │ │                   │
     │    │ [Confirm]  │ │                   │
     │    └────────────┘ │                   │
     │<──────────────────│                   │
     │                   │                   │
     │ 8. Select child:  │                   │
     │    "Thabo Molefe" │                   │
     │    Click [Confirm]│                   │
     │──────────────────>│                   │
     │                   │                   │
     │                   │ 9. POST /api/transactions/456/match
     │                   │──────────────────>│
     │                   │   Body: {         │
     │                   │     childId: 123, │
     │                   │     month: 11,    │
     │                   │     year: 2025    │
     │                   │   }               │
     │                   │                   │
     │                   │   + Create payment│
     │                   │   + Mark manually │
     │                   │     matched = true│
     │                   │   + Update trans  │
     │                   │     status        │
     │                   │                   │
     │                   │ 10. Return success│
     │                   │<──────────────────│
     │                   │                   │
     │ 11. Show success  │                   │
     │     Remove from   │                   │
     │     unmatched list│                   │
     │<──────────────────│                   │
```

**Why Manual Matching is Needed**:
- Parents forget to include payment reference (most common)
- Multiple children from same parent (need to split payment)
- Partial payments (need to specify which month)
- Historical data import (bulk matching)

---

### Workflow 5: Monthly Report Generation

**Actors**: ECD Administrator
**Frequency**: Monthly (end of month)
**Duration**: 2-3 seconds

```
┌─────────────────────────────────────────────────────────────────┐
│                Monthly Report Generation                        │
└─────────────────────────────────────────────────────────────────┘

Admin clicks "Generate Report" → Backend processes:

┌─────────────────────────────────────────────────────────────────┐
│ ReportService.generateMonthlyReport(month=11, year=2025)        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ Step 1: Get all active children                                │
│ ┌───────────────────────────────────────────────────────┐      │
│ │ SELECT * FROM children WHERE active = true            │      │
│ │ Result: 50 children                                   │      │
│ └───────────────────────────────────────────────────────┘      │
│                                                                 │
│ Step 2: Get all payments for this month                        │
│ ┌───────────────────────────────────────────────────────┐      │
│ │ SELECT * FROM payments                                │      │
│ │ WHERE payment_month = 11 AND payment_year = 2025      │      │
│ │ Result: 42 payments                                   │      │
│ └───────────────────────────────────────────────────────┘      │
│                                                                 │
│ Step 3: Match children to payments                             │
│ ┌───────────────────────────────────────────────────────┐      │
│ │ FOR EACH child:                                       │      │
│ │   payment = payments.find(p => p.childId == child.id) │      │
│ │   IF payment exists:                                  │      │
│ │     child.status = PAID                               │      │
│ │     child.amountPaid = payment.amountPaid             │      │
│ │   ELSE:                                               │      │
│ │     child.status = OWING                              │      │
│ │     child.amountOwed = child.monthlyFee               │      │
│ └───────────────────────────────────────────────────────┘      │
│                                                                 │
│ Step 4: Calculate summary statistics                           │
│ ┌───────────────────────────────────────────────────────┐      │
│ │ totalChildren = 50                                    │      │
│ │ paidCount = 42                                        │      │
│ │ owingCount = 8                                        │      │
│ │ totalCollected = SUM(payments.amountPaid) = R21,000   │      │
│ │ totalOutstanding = owingCount * avgFee = R4,000       │      │
│ │ collectionRate = (42/50) * 100 = 84%                  │      │
│ └───────────────────────────────────────────────────────┘      │
│                                                                 │
│ Step 5: Return report DTO                                      │
│ ┌───────────────────────────────────────────────────────┐      │
│ │ MonthlyReportDto {                                    │      │
│ │   month: 11,                                          │      │
│ │   year: 2025,                                         │      │
│ │   totalChildren: 50,                                  │      │
│ │   paidCount: 42,                                      │      │
│ │   owingCount: 8,                                      │      │
│ │   totalCollected: 21000.00,                           │      │
│ │   totalOutstanding: 4000.00,                          │      │
│ │   collectionRate: 84.0,                               │      │
│ │   paidChildren: [ ... ],                              │      │
│ │   owingChildren: [ ... ]                              │      │
│ │ }                                                     │      │
│ └───────────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────────┘
```

**Report Output Example**:
```json
{
  "month": 11,
  "year": 2025,
  "summary": {
    "totalChildren": 50,
    "paidCount": 42,
    "owingCount": 8,
    "totalCollected": 21000.00,
    "totalOutstanding": 4000.00,
    "collectionRate": 84.0
  },
  "paidChildren": [
    {
      "id": 123,
      "name": "Thabo Molefe",
      "monthlyFee": 500.00,
      "amountPaid": 500.00,
      "paymentDate": "2025-11-17",
      "transactionReference": "TRX123456"
    },
    ...
  ],
  "owingChildren": [
    {
      "id": 789,
      "name": "Sipho Ndlovu",
      "monthlyFee": 500.00,
      "amountOwed": 500.00,
      "parentContact": "0821234567"
    },
    ...
  ]
}
```

---

## Component Architecture

### Backend Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Backend Layers                           │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ 1. PRESENTATION LAYER (Controllers)                             │
├─────────────────────────────────────────────────────────────────┤
│ - AuthController: /api/auth/* (login, register)                 │
│ - ChildController: /api/children/* (CRUD operations)            │
│ - TransactionController: /api/transactions/* (sync, match)      │
│ - ReportController: /api/reports/* (monthly reports)            │
│                                                                 │
│ Responsibilities:                                               │
│ - HTTP request/response handling                                │
│ - Input validation (@Valid)                                     │
│ - DTO mapping                                                   │
│ - Exception handling                                            │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. SECURITY LAYER                                               │
├─────────────────────────────────────────────────────────────────┤
│ - JwtAuthenticationFilter: Intercepts requests                  │
│ - JwtUtil: Token generation and validation                      │
│ - CustomUserDetailsService: Load user by username               │
│ - SecurityConfig: Configure security rules                      │
│                                                                 │
│ Responsibilities:                                               │
│ - JWT authentication                                            │
│ - Authorization checks                                          │
│ - Password encryption                                           │
│ - CORS configuration                                            │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. SERVICE LAYER (Business Logic)                               │
├─────────────────────────────────────────────────────────────────┤
│ - AuthService: User registration, login                         │
│ - ChildService: Child management operations                     │
│ - TransactionSyncService: Fetch from bank API                   │
│ - PaymentMatchingService: Match transactions to children        │
│ - ReportService: Generate monthly reports                       │
│                                                                 │
│ Responsibilities:                                               │
│ - Business logic                                                │
│ - Transaction management                                        │
│ - Data validation                                               │
│ - Service orchestration                                         │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. REPOSITORY LAYER (Data Access)                               │
├─────────────────────────────────────────────────────────────────┤
│ - ChildRepository: extends JpaRepository<Child, Long>           │
│ - PaymentRepository: extends JpaRepository<Payment, Long>       │
│ - TransactionRepository: extends JpaRepository<Transaction, ...>│
│ - UserRepository: extends JpaRepository<User, Long>             │
│                                                                 │
│ Custom Queries:                                                 │
│ - findByPaymentReferenceIgnoreCase(...)                         │
│ - findUnmatchedTransactions()                                   │
│ - findByChildIdAndPaymentMonthAndPaymentYear(...)               │
│                                                                 │
│ Responsibilities:                                               │
│ - Database CRUD operations                                      │
│ - Custom query methods                                          │
│ - JPA entity management                                         │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. PERSISTENCE LAYER (Database)                                 │
├─────────────────────────────────────────────────────────────────┤
│ - PostgreSQL (production)                                       │
│ - H2 (development/testing)                                      │
│                                                                 │
│ Tables:                                                         │
│ - children                                                      │
│ - payments                                                      │
│ - transactions                                                  │
│ - users                                                         │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ 6. INTEGRATION LAYER                                            │
├─────────────────────────────────────────────────────────────────┤
│ - StandardBankApiClient: OAuth2 + REST calls                    │
│                                                                 │
│ Responsibilities:                                               │
│ - External API communication                                    │
│ - Token management                                              │
│ - Error handling and retries                                    │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ 7. SCHEDULING LAYER                                             │
├─────────────────────────────────────────────────────────────────┤
│ - TransactionSyncScheduler: @Scheduled cron job                 │
│                                                                 │
│ Responsibilities:                                               │
│ - Trigger daily sync                                            │
│ - Automatic matching                                            │
│ - Error notification                                            │
└─────────────────────────────────────────────────────────────────┘
```

### Frontend Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      Frontend Architecture                      │
└─────────────────────────────────────────────────────────────────┘

src/
├── main.tsx                    # Application entry point
├── App.tsx                     # Root component + routing
│
├── pages/                      # Page components
│   ├── Login.tsx               # Login page
│   ├── Dashboard.tsx           # Main dashboard
│   ├── Children.tsx            # Child management (future)
│   ├── Transactions.tsx        # Transaction review (future)
│   └── Reports.tsx             # Report generation (future)
│
├── components/                 # Reusable UI components
│   ├── Navbar.tsx
│   ├── ChildForm.tsx
│   ├── TransactionCard.tsx
│   ├── ReportTable.tsx
│   └── Alert.tsx
│
├── contexts/                   # React Context for state
│   └── AuthContext.tsx         # Authentication state
│
├── services/                   # API communication
│   └── api.ts                  # Axios instance + endpoints
│
├── types/                      # TypeScript type definitions
│   └── index.ts                # Child, Payment, Transaction types
│
├── utils/                      # Utility functions
│   ├── formatters.ts           # Date, currency formatters
│   └── validators.ts           # Form validation
│
└── styles/                     # Global styles
    └── index.css               # TailwindCSS directives
```

**Key Files**:

**api.ts** (API Communication):
```typescript
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
});

// Add JWT token to all requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const authApi = {
  login: (credentials) => api.post('/auth/login', credentials),
  register: (data) => api.post('/auth/register', data),
};

export const childApi = {
  getAll: () => api.get('/children'),
  getById: (id) => api.get(`/children/${id}`),
  create: (child) => api.post('/children', child),
  update: (id, child) => api.put(`/children/${id}`, child),
  delete: (id) => api.delete(`/children/${id}`),
};

export const reportApi = {
  getCurrentMonth: () => api.get('/reports/monthly/current'),
  getByMonth: (month, year) => api.get(`/reports/monthly?month=${month}&year=${year}`),
};

export const transactionApi = {
  getUnmatched: () => api.get('/transactions/unmatched'),
  matchManually: (id, data) => api.post(`/transactions/${id}/match`, data),
};
```

**AuthContext.tsx** (Authentication State):
```typescript
import React, { createContext, useState, useContext } from 'react';

interface AuthContextType {
  isAuthenticated: boolean;
  user: User | null;
  login: (token: string, user: User) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType>(null!);

export const AuthProvider: React.FC = ({ children }) => {
  const [isAuthenticated, setIsAuthenticated] = useState(
    !!localStorage.getItem('token')
  );
  const [user, setUser] = useState<User | null>(null);

  const login = (token: string, user: User) => {
    localStorage.setItem('token', token);
    setIsAuthenticated(true);
    setUser(user);
  };

  const logout = () => {
    localStorage.removeItem('token');
    setIsAuthenticated(false);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ isAuthenticated, user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
```

---

## Payment Matching Algorithm

### Algorithm Implementation (Java)

```java
@Service
@Transactional
public class PaymentMatchingService {

    /**
     * Core matching algorithm
     *
     * Input: Transaction with payment reference
     * Output: true if matched, false otherwise
     *
     * Time Complexity: O(1) - Database index lookup
     * Space Complexity: O(1) - Single transaction processing
     */
    public boolean matchTransaction(Transaction transaction) {
        // ================================================
        // STEP 1: Validate payment reference exists
        // ================================================
        if (transaction.getPaymentReference() == null ||
            transaction.getPaymentReference().isEmpty()) {
            log.debug("Transaction {} has no payment reference",
                transaction.getId());
            return false;
        }

        // ================================================
        // STEP 2: Find child by payment reference
        // ================================================
        // Case-insensitive search with trimming
        String cleanedReference = transaction.getPaymentReference().trim();

        Optional<Child> childOpt = childRepository
            .findByPaymentReferenceIgnoreCase(cleanedReference);

        if (childOpt.isEmpty()) {
            log.debug("No child found for reference: {}",
                cleanedReference);
            return false;
        }

        Child child = childOpt.get();
        log.info("✓ Matched transaction {} to child: {} ({})",
            transaction.getBankReference(),
            child.getFullName(),
            child.getPaymentReference());

        // ================================================
        // STEP 3: Determine payment month/year
        // ================================================
        YearMonth transactionMonth = YearMonth.from(
            transaction.getTransactionDate()
        );

        // ================================================
        // STEP 4: Create or update payment record
        // ================================================
        Payment payment = paymentRepository
            .findByChildIdAndPaymentMonthAndPaymentYear(
                child.getId(),
                transactionMonth.getMonthValue(),
                transactionMonth.getYear())
            .orElse(Payment.builder()
                .child(child)
                .paymentMonth(transactionMonth.getMonthValue())
                .paymentYear(transactionMonth.getYear())
                .amountPaid(BigDecimal.ZERO)
                .build());

        // Link transaction to payment
        payment.setTransaction(transaction);
        payment.setExpectedAmount(child.getMonthlyFee());
        payment.setPaymentDate(transaction.getTransactionDate());
        payment.setTransactionReference(transaction.getBankReference());
        payment.setPaymentMethod(Payment.PaymentMethod.BANK_TRANSFER);

        // Add to existing amount (supports partial payments)
        payment.setAmountPaid(
            payment.getAmountPaid().add(transaction.getAmount())
        );

        // Determine status
        if (payment.getAmountPaid()
                .compareTo(payment.getExpectedAmount()) >= 0) {
            payment.setStatus(Payment.Status.PAID);
        } else {
            payment.setStatus(Payment.Status.PARTIAL);
        }

        paymentRepository.save(payment);

        // ================================================
        // STEP 5: Update transaction status
        // ================================================
        transaction.setStatus(Transaction.Status.MATCHED);
        transaction.setMatchingNotes("Automatically matched to " +
            child.getFullName());
        transaction.setMatchedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        return true;
    }
}
```

### Matching Edge Cases

| Scenario | Handling | Example |
|----------|----------|---------|
| **Exact match** | Direct match | Reference: "THABO_MOLEFE" → Child found |
| **Case mismatch** | Case-insensitive search | "thabo_molefe" → Matches "THABO_MOLEFE" |
| **Extra whitespace** | Trim before search | " THABO_MOLEFE " → Matches |
| **No reference** | Leave unmatched | Reference: null → Admin reviews |
| **Wrong reference** | Leave unmatched | Reference: "JOHN_DOE" → Child not found |
| **Duplicate payment** | Add to amount | Two R250 payments → Total R500 |
| **Partial payment** | Mark as PARTIAL | R200 of R500 → Status: PARTIAL |
| **Overpayment** | Mark as PAID | R600 of R500 → Status: PAID (credit future) |

---

## Database Design

### Entity Relationship Diagram (ERD)

```
┌──────────────────────────────────────────────────────────────────┐
│                       Database Schema                            │
└──────────────────────────────────────────────────────────────────┘

┌─────────────────────────┐
│        USERS            │
├─────────────────────────┤
│ id (PK)          BIGINT │
│ username         VARCHAR│
│ password         VARCHAR│ (BCrypt hashed)
│ email            VARCHAR│
│ role             VARCHAR│ (ADMIN, USER)
│ active           BOOLEAN│
│ created_at       TIMESTAMP│
│ updated_at       TIMESTAMP│
└─────────────────────────┘

┌─────────────────────────┐                  ┌─────────────────────────┐
│      CHILDREN           │                  │      TRANSACTIONS       │
├─────────────────────────┤                  ├─────────────────────────┤
│ id (PK)          BIGINT │                  │ id (PK)          BIGINT │
│ first_name       VARCHAR│                  │ bank_reference   VARCHAR│ (UNIQUE)
│ last_name        VARCHAR│                  │ amount           DECIMAL│
│ payment_reference VARCHAR│ (UNIQUE)         │ transaction_date DATE   │
│ monthly_fee      DECIMAL│                  │ payment_reference VARCHAR│
│ parent_phone     VARCHAR│                  │ description      VARCHAR│
│ parent_email     VARCHAR│                  │ sender_name      VARCHAR│
│ parent_name      VARCHAR│                  │ sender_account   VARCHAR│
│ date_of_birth    DATE   │                  │ status           VARCHAR│ (MATCHED, UNMATCHED)
│ enrollment_date  DATE   │                  │ type             VARCHAR│ (CREDIT, DEBIT)
│ active           BOOLEAN│                  │ manually_matched BOOLEAN│
│ notes            VARCHAR│                  │ matching_notes   VARCHAR│
│ created_at       TIMESTAMP│                │ created_at       TIMESTAMP│
│ updated_at       TIMESTAMP│                │ matched_at       TIMESTAMP│
└──────────┬──────────────┘                  └──────────┬──────────────┘
           │                                            │
           │         ┌──────────────────────┐           │
           │         │      PAYMENTS        │           │
           │         ├──────────────────────┤           │
           └────────>│ id (PK)       BIGINT │<──────────┘
                     │ child_id (FK) BIGINT │
                     │ transaction_id (FK) BIGINT │
                     │ payment_month  INT   │
                     │ payment_year   INT   │
                     │ amount_paid    DECIMAL│
                     │ expected_amount DECIMAL│
                     │ payment_date   DATE  │
                     │ status         VARCHAR│ (PAID, PARTIAL, OVERDUE)
                     │ payment_method VARCHAR│ (BANK_TRANSFER, CASH)
                     │ transaction_ref VARCHAR│
                     │ notes          VARCHAR│
                     │ created_at     TIMESTAMP│
                     └──────────────────────┘

Relationships:
- One child can have many payments (1:N)
- One transaction can match one payment (1:1)
- Unique constraint: (child_id, payment_month, payment_year)
```

### Key Indexes

```sql
-- Children table
CREATE INDEX idx_children_payment_ref ON children(LOWER(payment_reference));
CREATE INDEX idx_children_active ON children(active);

-- Payments table
CREATE INDEX idx_payments_child_month_year ON payments(child_id, payment_month, payment_year);
CREATE INDEX idx_payments_status ON payments(status);

-- Transactions table
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_date ON transactions(transaction_date);
CREATE UNIQUE INDEX idx_transactions_bank_ref ON transactions(bank_reference);
```

---

## API Specification

### Authentication Endpoints

#### POST /api/auth/login
**Description**: Authenticate user and receive JWT token

**Request**:
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response (200 OK)**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "admin",
  "email": "admin@example.com",
  "expiresAt": "2025-11-19T10:00:00Z"
}
```

**Errors**:
- 401: Invalid credentials
- 403: Account disabled

---

#### POST /api/auth/register
**Description**: Create new admin user

**Request**:
```json
{
  "username": "newadmin",
  "password": "securepassword123",
  "email": "newadmin@example.com"
}
```

**Response (201 Created)**:
```json
{
  "id": 2,
  "username": "newadmin",
  "email": "newadmin@example.com",
  "createdAt": "2025-11-18T10:00:00Z"
}
```

---

### Child Management Endpoints

#### GET /api/children
**Description**: Get all children

**Query Parameters**:
- `search` (optional): Search by name
- `active` (optional): Filter by active status

**Response (200 OK)**:
```json
[
  {
    "id": 123,
    "firstName": "Thabo",
    "lastName": "Molefe",
    "fullName": "Thabo Molefe",
    "paymentReference": "THABO_MOLEFE",
    "monthlyFee": 500.00,
    "parentPhone": "0821234567",
    "parentEmail": "parent@example.com",
    "dateOfBirth": "2020-05-15",
    "enrollmentDate": "2023-01-10",
    "active": true
  },
  ...
]
```

---

#### POST /api/children
**Description**: Create new child

**Request**:
```json
{
  "firstName": "Sipho",
  "lastName": "Ndlovu",
  "paymentReference": "SIPHO_NDLOVU",
  "monthlyFee": 500.00,
  "parentPhone": "0827654321",
  "parentEmail": "sipho.parent@example.com",
  "parentName": "Mr. Ndlovu",
  "dateOfBirth": "2021-03-20",
  "enrollmentDate": "2025-01-01"
}
```

**Response (201 Created)**:
```json
{
  "id": 124,
  "firstName": "Sipho",
  "lastName": "Ndlovu",
  ...
}
```

**Errors**:
- 400: Validation error (e.g., duplicate payment reference)
- 401: Unauthorized

---

### Report Endpoints

#### GET /api/reports/monthly/current
**Description**: Get current month payment report

**Response (200 OK)**:
```json
{
  "month": 11,
  "year": 2025,
  "totalChildren": 50,
  "paidCount": 42,
  "owingCount": 8,
  "totalCollected": 21000.00,
  "totalOutstanding": 4000.00,
  "collectionRate": 84.0,
  "paidChildren": [...],
  "owingChildren": [...]
}
```

---

### Transaction Endpoints

#### GET /api/transactions/unmatched
**Description**: Get all unmatched transactions

**Response (200 OK)**:
```json
[
  {
    "id": 456,
    "bankReference": "TRX123456",
    "amount": 500.00,
    "transactionDate": "2025-11-17",
    "paymentReference": null,
    "senderName": "John Doe",
    "status": "UNMATCHED",
    "createdAt": "2025-11-18T01:05:00Z"
  },
  ...
]
```

---

#### POST /api/transactions/{id}/match
**Description**: Manually match transaction to child

**Request**:
```json
{
  "childId": 123,
  "month": 11,
  "year": 2025
}
```

**Response (200 OK)**:
```json
{
  "id": 789,
  "childId": 123,
  "transactionId": 456,
  "paymentMonth": 11,
  "paymentYear": 2025,
  "amountPaid": 500.00,
  "expectedAmount": 500.00,
  "status": "PAID"
}
```

---

## Security Implementation

### JWT Token Structure

**Header**:
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload**:
```json
{
  "sub": "admin",
  "iat": 1700308800,
  "exp": 1700395200,
  "roles": ["ADMIN"]
}
```

**Signature**:
```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret
)
```

### Authentication Flow

```
1. User submits credentials (POST /api/auth/login)
   ↓
2. Backend validates username/password
   ↓
3. Backend generates JWT token (24h expiry)
   ↓
4. Frontend stores token in localStorage
   ↓
5. Frontend adds token to all requests:
   Authorization: Bearer <token>
   ↓
6. JwtAuthenticationFilter extracts token
   ↓
7. JwtUtil validates token (signature, expiry)
   ↓
8. SecurityContext populated with user details
   ↓
9. Controller method executes
```

### Password Security

```java
// Password encoding (BCrypt)
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10); // Cost factor: 10
}

// Registration
String hashedPassword = passwordEncoder.encode(plainPassword);
user.setPassword(hashedPassword);

// Login
boolean matches = passwordEncoder.matches(plainPassword, user.getPassword());
```

---

## Deployment Architecture

### Production Deployment (Docker)

```
┌─────────────────────────────────────────────────────────────────┐
│                     Docker Compose Setup                        │
└─────────────────────────────────────────────────────────────────┘

docker-compose.yml:

services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: ecd_payment_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  backend:
    build: ./backend
    environment:
      DB_USERNAME: postgres
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      STANDARDBANK_CLIENT_ID: ${STANDARDBANK_CLIENT_ID}
      STANDARDBANK_CLIENT_SECRET: ${STANDARDBANK_CLIENT_SECRET}
    ports:
      - "8080:8080"
    depends_on:
      - postgres

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  pgdata:
```

### Environment Variables

```bash
# .env file (NEVER commit to Git)
DB_PASSWORD=super-secure-password
JWT_SECRET=your-256-bit-secret-key-here-make-it-very-long
STANDARDBANK_CLIENT_ID=your-client-id
STANDARDBANK_CLIENT_SECRET=your-client-secret
```

---

## Troubleshooting Guide

### Common Issues

#### 1. Transaction Not Matching Automatically

**Symptom**: Transaction appears as "UNMATCHED" despite correct reference

**Possible Causes**:
- Reference has extra spaces: "THABO_MOLEFE " → Trim automatically
- Case mismatch: "thabo_molefe" → Fixed with case-insensitive search
- Child not in system: Add child first
- Child deactivated: Reactivate child

**Solution**:
```sql
-- Check if child exists
SELECT * FROM children WHERE LOWER(payment_reference) = LOWER('THABO_MOLEFE');

-- Check transaction reference
SELECT id, payment_reference FROM transactions WHERE id = 456;

-- Manually match via API or dashboard
```

---

#### 2. JWT Token Expired

**Symptom**: API returns 401 Unauthorized

**Cause**: Token expired (24h lifetime)

**Solution**:
- Frontend: Detect 401, redirect to login
- User: Re-login to get new token

---

#### 3. Database Connection Failed

**Symptom**: Application won't start, error: "Connection refused"

**Possible Causes**:
- PostgreSQL not running
- Wrong credentials
- Wrong database name

**Solution**:
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Check logs
docker logs ecd-postgres

# Verify environment variables
echo $DB_USERNAME
echo $DB_PASSWORD

# Test connection manually
psql -h localhost -U postgres -d ecd_payment_db
```

---

## Conclusion

This document provides a comprehensive technical overview of the ECD Payment Reconciliation System. For additional information:

- **Backend Code**: `/backend/src/main/java/com/katlehouniversity/ecd/`
- **Frontend Code**: `/frontend/src/`
- **API Documentation**: See backend README.md
- **Deployment Guide**: See DOCKER_DEPLOYMENT.md

For questions or issues, refer to the GitHub repository or contact the development team.

---

**Document Version**: 1.0
**Last Updated**: 2025-11-18
**Target Audience**: Developers, Technical Stakeholders
