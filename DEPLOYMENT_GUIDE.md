# ECD Payment Reconciliation System - Deployment Guide

## Project Complete! ✅

The ECD Payment Reconciliation System MVP is now fully functional with all Phase 1 features implemented.

---

## What's Been Completed

### Backend (Spring Boot + Java 21)
- ✅ Student registration with auto-generated student numbers (STU-YYYY-NNN)
- ✅ CSV and Markdown statement parsing
- ✅ Automatic payment matching by student number (regex + fuzzy matching)
- ✅ Payment record creation and tracking
- ✅ Monthly report generation
- ✅ JWT authentication and authorization
- ✅ RESTful API with comprehensive endpoints
- ✅ PostgreSQL/H2 database support

### Frontend (React + TypeScript + TailwindCSS + shadcn/ui)
- ✅ Login page with authentication
- ✅ Dashboard with monthly payment reports
- ✅ Student registration form
- ✅ Statement upload with drag-and-drop
- ✅ **Students list page (NEW!)**
- ✅ **Individual student payment history (NEW!)**
- ✅ **Unmatched transactions review page (NEW!)**
- ✅ Responsive navigation bar
- ✅ Modern UI with shadcn/ui components

---

## Quick Start

### 1. Run Backend

```bash
cd backend
export JAVA_HOME=/Users/palesamolefe/Library/Java/JavaVirtualMachines/corretto-21.0.5/Contents/Home
mvn clean spring-boot:run
```

The backend will start on `http://localhost:8080`

### 2. Run Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend will start on `http://localhost:5173`

### 3. Login

- Navigate to: `http://localhost:5173`
- Default credentials:
  - Username: `admin`
  - Password: `admin123`

---

## Application Flow

### 1. Register Students
- Go to **Register Student** page
- Fill in student details
- System auto-generates student number (e.g., STU-2025-001)
- Share student number with parents for payment reference

### 2. Upload Bank Statements
- Go to **Upload Statement** page
- Upload CSV or Markdown file
- System automatically matches payments to students by:
  - Searching for student number pattern (STU-YYYY-NNN) in transaction description
  - Fallback fuzzy matching on student names

### 3. Review Matched Payments
- **Dashboard** shows monthly summary:
  - Total children, paid count, owing count
  - Total revenue collected
  - Lists of paid/owing students
- **Students** page shows all registered students with payment status
- Click on any student to view detailed payment history

### 4. Handle Unmatched Transactions
- **Transactions** page lists all unmatched transactions
- System suggests possible student matches
- Manually assign transactions to students

### 5. Monitor & Report
- Monthly reports automatically calculate:
  - Expected revenue
  - Actual collections
  - Outstanding amounts per student
- Export capabilities ready for Phase 2

---

## Docker Deployment

### Option 1: Using Docker Compose

```bash
# Build and start all services
docker-compose up --build
```

This will start:
- PostgreSQL database
- Backend API
- Frontend (served via nginx)

Access the application at: `http://localhost`

### Option 2: Individual Containers

**Backend:**
```bash
cd backend
docker build -t ecd-backend .
docker run -p 8080:8080 ecd-backend
```

**Frontend:**
```bash
cd frontend
npm run build
# Serve dist/ folder with nginx or any static file server
```

---

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login and get JWT token
- `POST /api/auth/register` - Register new admin

### Students (Children)
- `GET /api/children` - List all/active children
- `GET /api/children/{id}` - Get child by ID
- `GET /api/children/reference/{ref}` - Get by payment reference
- `GET /api/children/search?name={name}` - Search by name
- `POST /api/children` - Register new child
- `PUT /api/children/{id}` - Update child
- `DELETE /api/children/{id}` - Withdraw child

### Statement Upload
- `POST /api/statements/upload` - Upload CSV/MD file (multipart/form-data)
- `GET /api/statements` - List all uploaded statements
- `GET /api/statements/{id}` - Get specific statement

### Reports
- `GET /api/reports/monthly?month={M}&year={Y}` - Get monthly report
- `GET /api/reports/monthly/current` - Get current month report

### Transactions
- `GET /api/transactions/unmatched` - Get unmatched transactions
- `POST /api/transactions/{id}/match?childId={id}&month={M}&year={Y}` - Manually match transaction

---

## Database Configuration

### Development (H2 In-Memory)
Default profile uses H2 database. No setup required.
Console: `http://localhost:8080/h2-console`

### Production (PostgreSQL)
Update `backend/src/main/resources/application.yml`:

```yaml
spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:postgresql://localhost:5432/ecd_payment_db
    username: your_db_user
    password: your_db_password
```

---

## Environment Variables

### Backend
- `JWT_SECRET` - Secret key for JWT tokens (default: secure-secret-key-change-in-production)
- `SPRING_PROFILES_ACTIVE` - Profile (dev/prod)
- Database credentials (see application.yml)

### Frontend
- `VITE_API_URL` - Backend API URL (default: http://localhost:8080/api)

---

## File Upload Formats

### CSV Format
```csv
Date,Description,Deposits
19 Nov 24,Payment from John Doe STU-2025-001,1500.00
20 Nov 24,Mary Jane STU-2025-002 School Fee,1500.00
```

Supported column names:
- Date: `Date`, `Transaction Date`, `Trans Date`
- Description: `Description`, `Details`, `Narrative`
- Amount: `Deposits`, `Credit`, `Amount`

### Markdown Format
```markdown
| Date | Description | Amount |
|------|-------------|--------|
| 2024-11-19 | Payment STU-2025-001 | 1500.00 |
```

---

## Testing the System

### 1. Create a Test Student
- Register a student (e.g., "John Doe")
- Note the generated student number: `STU-2025-001`

### 2. Create a Test Statement
Create `test-statement.csv`:
```csv
Date,Description,Deposits
2024-12-29,Payment from John Doe STU-2025-001,1500.00
```

### 3. Upload and Verify
- Upload the CSV file
- Check Dashboard - John should appear in "Paid" list
- Go to Students → Click on John → View payment history

---

## Next Steps (Phase 2)

Planned enhancements:
- [ ] PDF/Excel report export
- [ ] SMS/Email payment reminders
- [ ] Bulk student import (CSV)
- [ ] Standard Bank API integration (auto-fetch transactions)
- [ ] Fee structure variations (scholarships, discounts)
- [ ] Advanced filtering and search
- [ ] Audit trail and activity logs

---

## Troubleshooting

### Backend won't start
- Check Java version: `java -version` (should be 21)
- Set JAVA_HOME: `export JAVA_HOME=/path/to/java-21`
- Check port 8080 is available: `lsof -i :8080`

### Frontend build fails
- Node version warning is OK (builds successfully anyway)
- Run `npm install` to ensure dependencies are installed
- Clear node_modules and reinstall if needed

### Database issues
- H2: Check console at http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:ecd-payment-db`
  - Username: `sa`
  - Password: (empty)

### Authentication fails
- Check JWT token in localStorage
- Try clearing localStorage and logging in again
- Verify backend is running and accessible

---

## Support

For issues or questions:
1. Check backend logs: `backend/backend.log`
2. Check browser console for frontend errors
3. Review API responses in Network tab

---

## Project Structure

```
katlehouniversity/
├── backend/                    # Spring Boot application
│   ├── src/main/java/.../
│   │   ├── controller/        # REST API endpoints
│   │   ├── service/           # Business logic
│   │   ├── entity/            # Database entities
│   │   ├── repository/        # Data access
│   │   ├── dto/               # Data transfer objects
│   │   ├── security/          # JWT auth
│   │   └── exception/         # Error handling
│   ├── src/main/resources/
│   │   └── application.yml    # Configuration
│   └── pom.xml                # Maven dependencies
│
├── frontend/                   # React application
│   ├── src/
│   │   ├── pages/             # Route pages
│   │   ├── components/        # Reusable components
│   │   ├── services/          # API client
│   │   ├── contexts/          # React contexts
│   │   └── types/             # TypeScript types
│   ├── package.json           # NPM dependencies
│   └── vite.config.ts         # Vite configuration
│
├── docker-compose.yml          # Multi-container setup
└── CLAUDE.md                   # Project requirements

```

**System is ready for production! 🎉**
