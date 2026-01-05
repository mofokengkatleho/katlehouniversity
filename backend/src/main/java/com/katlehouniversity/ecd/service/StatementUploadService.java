package com.katlehouniversity.ecd.service;

import com.katlehouniversity.ecd.entity.*;
import com.katlehouniversity.ecd.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementUploadService {

    private final UploadedStatementRepository uploadedStatementRepository;
    private final TransactionRepository transactionRepository;
    private final ChildRepository childRepository;
    private final PaymentRepository paymentRepository;

    private static final Pattern STUDENT_NUMBER_PATTERN = Pattern.compile("STU-\\d{4}-\\d{3}");
    private static final List<DateTimeFormatter> DATE_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("dd MMM yy"),
            DateTimeFormatter.ofPattern("d MMM yy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    @Transactional
    public UploadedStatement uploadAndProcessStatement(MultipartFile file, User uploadedBy) {
        log.info("Processing statement upload: {}", file.getOriginalFilename());

        UploadedStatement.FileType fileType = determineFileType(file.getOriginalFilename());

        UploadedStatement statement = UploadedStatement.builder()
                .fileName(file.getOriginalFilename())
                .fileType(fileType)
                .totalTransactions(0)
                .uploadedBy(uploadedBy)
                .status(UploadedStatement.ProcessingStatus.PROCESSING)
                .build();

        statement = uploadedStatementRepository.save(statement);

        try {
            List<Transaction> transactions;
            if (fileType == UploadedStatement.FileType.CSV) {
                transactions = parseCSVStatement(file, statement);
            } else if (fileType == UploadedStatement.FileType.PDF) {
                transactions = parsePDFStatement(file, statement);
            } else {
                transactions = parseMarkdownStatement(file, statement);
            }

            log.info("Parsed {} transactions from file", transactions.size());

            // Save all transactions
            transactions = transactionRepository.saveAll(transactions);

            // Match transactions to students
            int matchedCount = matchTransactionsToStudents(transactions);

            statement.setTotalTransactions(transactions.size());
            statement.setMatchedCount(matchedCount);
            statement.setUnmatchedCount(transactions.size() - matchedCount);
            statement.markAsCompleted();

            log.info("Statement processing completed. Matched: {}, Unmatched: {}",
                    matchedCount, statement.getUnmatchedCount());

        } catch (Exception e) {
            log.error("Error processing statement: {}", e.getMessage(), e);
            statement.markAsFailed(e.getMessage());
        }

        return uploadedStatementRepository.save(statement);
    }

    private UploadedStatement.FileType determineFileType(String filename) {
        if (filename.toLowerCase().endsWith(".csv")) {
            return UploadedStatement.FileType.CSV;
        } else if (filename.toLowerCase().endsWith(".md")) {
            return UploadedStatement.FileType.MARKDOWN;
        } else if (filename.toLowerCase().endsWith(".pdf")) {
            return UploadedStatement.FileType.PDF;
        }
        throw new IllegalArgumentException("Unsupported file type. Only CSV, MD, and PDF files are allowed.");
    }

    private List<Transaction> parseCSVStatement(MultipartFile file, UploadedStatement statement) throws Exception {
        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            // Check if this is an SBSA formatted statement
            String firstLine = reader.readLine();
            reader.close();

            if (firstLine != null && firstLine.contains("Customer Care:")) {
                // SBSA format - reopen stream and use custom parser
                try (BufferedReader sbsaReader = new BufferedReader(
                        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                    return parseSBSAStatement(sbsaReader, statement);
                }
            }

            // Standard CSV format with headers
            try (BufferedReader standardReader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
                 CSVParser csvParser = new CSVParser(standardReader, CSVFormat.DEFAULT
                        .withFirstRecordAsHeader()
                        .withIgnoreHeaderCase()
                        .withTrim())) {

                for (CSVRecord record : csvParser) {
                    try {
                        Transaction transaction = parseCSVRecord(record, statement);
                        if (transaction != null) {
                            transactions.add(transaction);
                        }
                    } catch (Exception e) {
                        log.warn("Error parsing CSV record {}: {}", record.getRecordNumber(), e.getMessage());
                    }
                }
            }
        }

        return transactions;
    }

    private List<Transaction> parseSBSAStatement(BufferedReader reader, UploadedStatement statement) throws Exception {
        List<Transaction> transactions = new ArrayList<>();
        String line;
        boolean inTransactionSection = false;

        while ((line = reader.readLine()) != null) {
            // Skip until we find "Date Description"
            if (line.contains("Date Description")) {
                inTransactionSection = true;
                continue;
            }

            if (!inTransactionSection) {
                continue;
            }

            // Skip empty lines and page headers
            if (line.trim().isEmpty() || line.contains("Date Description") ||
                line.contains("STATEMENT") || line.contains("Transaction details")) {
                continue;
            }

            // Parse transaction line - format: "23 May 25 CAPITEC KELEBOGILE XABA 700.00 4,918.02\nCREDIT TRANSFER"
            String cleanLine = line.replaceAll("^\"|\"$", "").trim(); // Remove surrounding quotes
            if (cleanLine.isEmpty()) {
                continue;
            }

            try {
                Transaction transaction = parseSBSALine(cleanLine, statement);
                if (transaction != null) {
                    transactions.add(transaction);
                }
            } catch (Exception e) {
                log.warn("Error parsing SBSA line: {}", cleanLine, e);
            }
        }

        return transactions;
    }

    private Transaction parseSBSALine(String line, UploadedStatement statement) {
        // Format: "23 May 25 CAPITEC KELEBOGILE XABA 700.00 4,918.02" (newline) "CREDIT TRANSFER"
        // Or just: "23 May 25 CAPITEC KELEBOGILE XABA 700.00 4,918.02"

        // Remove newlines and extra whitespace
        line = line.replace("\n", " ").replaceAll("\\s+", " ").trim();

        // Pattern: Date (dd MMM yy) + Description + Amount + Balance
        String[] parts = line.split("\\s+");
        if (parts.length < 5) {
            return null;
        }

        try {
            // Extract date (first 3 parts: "23 May 25")
            String dateStr = parts[0] + " " + parts[1] + " " + parts[2];
            LocalDate transactionDate = parseDate(dateStr);
            if (transactionDate == null) {
                return null;
            }

            // Find amounts (looking for numbers with commas and decimals)
            BigDecimal amount = null;
            BigDecimal balance = null;
            int amountIndex = -1;
            int balanceIndex = -1;

            for (int i = parts.length - 1; i >= 3; i--) {
                String cleanPart = parts[i].replaceAll(",", "");
                try {
                    BigDecimal num = new BigDecimal(cleanPart);
                    if (balance == null) {
                        balance = num;
                        balanceIndex = i;
                    } else if (amount == null) {
                        amount = num;
                        amountIndex = i;
                        break;
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return null; // Skip debits or zero amounts
            }

            // Description is everything between date and amount
            StringBuilder description = new StringBuilder();
            for (int i = 3; i < amountIndex; i++) {
                if (i > 3) description.append(" ");
                description.append(parts[i]);
            }

            return Transaction.builder()
                    .bankReference(generateBankReference(transactionDate, amount, description.toString()))
                    .amount(amount)
                    .transactionDate(transactionDate)
                    .description(description.toString())
                    .paymentReference(extractPaymentReference(description.toString()))
                    .status(Transaction.TransactionStatus.UNMATCHED)
                    .type(Transaction.TransactionType.CREDIT)
                    .uploadedStatement(statement)
                    .rawData(line)
                    .build();

        } catch (Exception e) {
            log.warn("Error parsing SBSA transaction: {}", line, e);
            return null;
        }
    }

    private List<Transaction> parsePDFStatement(MultipartFile file, UploadedStatement statement) throws Exception {
        List<Transaction> transactions = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // Parse the extracted text line by line
            String[] lines = text.split("\n");
            boolean inTransactionSection = false;

            for (String line : lines) {
                // Skip until we find "Date Description" or similar header
                if (line.contains("Date Description") || line.contains("Date") && line.contains("Description")) {
                    inTransactionSection = true;
                    continue;
                }

                if (!inTransactionSection) {
                    continue;
                }

                // Skip empty lines and page headers
                if (line.trim().isEmpty() || line.contains("Date Description") ||
                    line.contains("STATEMENT") || line.contains("Transaction details") ||
                    line.contains("Customer Care")) {
                    continue;
                }

                // Parse transaction line - format: "23 May 25 CAPITEC KELEBOGILE XABA 700.00 4,918.02"
                String cleanLine = line.trim();
                if (cleanLine.isEmpty()) {
                    continue;
                }

                try {
                    Transaction transaction = parseSBSALine(cleanLine, statement);
                    if (transaction != null) {
                        transactions.add(transaction);
                    }
                } catch (Exception e) {
                    log.warn("Error parsing PDF line: {}", cleanLine, e);
                }
            }
        }

        log.info("Parsed {} transactions from PDF", transactions.size());
        return transactions;
    }

    private Transaction parseCSVRecord(CSVRecord record, UploadedStatement statement) {
        // Try to extract date, description, and amount from CSV
        String dateStr = getCSVValue(record, "Date", "date", "Transaction Date");
        String description = getCSVValue(record, "Description", "description", "Narrative", "Details");
        String amountStr = getCSVValue(record, "Deposits", "deposit", "Credit", "Amount", "Debit");
        String balanceStr = getCSVValue(record, "Balance", "balance", "Running Balance");

        if (dateStr == null || description == null || amountStr == null) {
            return null;
        }

        LocalDate transactionDate = parseDate(dateStr);
        if (transactionDate == null) {
            return null;
        }

        BigDecimal amount = parseAmount(amountStr);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null; // Skip debits or zero amounts
        }

        BigDecimal balance = balanceStr != null ? parseAmount(balanceStr) : null;

        return Transaction.builder()
                .bankReference(generateBankReference(transactionDate, amount, description))
                .amount(amount)
                .transactionDate(transactionDate)
                .description(description)
                .paymentReference(extractPaymentReference(description))
                .status(Transaction.TransactionStatus.UNMATCHED)
                .type(Transaction.TransactionType.CREDIT)
                .uploadedStatement(statement)
                .rawData(record.toString())
                .build();
    }

    private List<Transaction> parseMarkdownStatement(MultipartFile file, UploadedStatement statement) throws Exception {
        // Simplified markdown parser for extracted bank statements
        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // Look for lines that appear to be transactions
                // Format: "23 May 25 CAPITEC KELEBOGILE XABA 700.00 4,918.02"
                if (line.trim().matches("^\\d{1,2} \\w{3} \\d{2}.*\\d+\\.\\d{2}.*")) {
                    Transaction transaction = parseMarkdownLine(line, statement);
                    if (transaction != null) {
                        transactions.add(transaction);
                    }
                }
            }
        }

        return transactions;
    }

    private Transaction parseMarkdownLine(String line, UploadedStatement statement) {
        // Parse markdown transaction line
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 4) {
            return null;
        }

        try {
            // Extract date (first 3 parts: "23 May 25")
            String dateStr = parts[0] + " " + parts[1] + " " + parts[2];
            LocalDate transactionDate = parseDate(dateStr);
            if (transactionDate == null) {
                return null;
            }

            // Find amount (second-to-last number)
            BigDecimal amount = null;
            String description = "";

            for (int i = 3; i < parts.length; i++) {
                String cleanPart = parts[i].replace(",", "");
                try {
                    BigDecimal num = new BigDecimal(cleanPart);
                    if (amount == null && num.compareTo(BigDecimal.ZERO) > 0) {
                        amount = num;
                        // Description is everything before the amount
                        description = String.join(" ", Arrays.copyOfRange(parts, 3, i));
                        break;
                    }
                } catch (NumberFormatException ignored) {
                    // Not a number, continue
                }
            }

            if (amount == null || description.isEmpty()) {
                return null;
            }

            return Transaction.builder()
                    .bankReference(generateBankReference(transactionDate, amount, description))
                    .amount(amount)
                    .transactionDate(transactionDate)
                    .description(description)
                    .paymentReference(extractPaymentReference(description))
                    .status(Transaction.TransactionStatus.UNMATCHED)
                    .type(Transaction.TransactionType.CREDIT)
                    .uploadedStatement(statement)
                    .rawData(line)
                    .build();

        } catch (Exception e) {
            log.warn("Error parsing markdown line: {}", line, e);
            return null;
        }
    }

    private int matchTransactionsToStudents(List<Transaction> transactions) {
        int matchedCount = 0;

        for (Transaction transaction : transactions) {
            boolean matched = attemptAutoMatch(transaction);
            if (matched) {
                matchedCount++;
            }
        }

        return matchedCount;
    }

    private boolean attemptAutoMatch(Transaction transaction) {
        // Try to find student number in description
        Matcher matcher = STUDENT_NUMBER_PATTERN.matcher(transaction.getDescription());
        if (matcher.find()) {
            String studentNumber = matcher.group();
            Optional<Child> studentOpt = childRepository.findByStudentNumber(studentNumber);

            if (studentOpt.isPresent()) {
                createPaymentRecord(transaction, studentOpt.get(), true);
                transaction.markAsMatched("Auto-matched by student number: " + studentNumber);
                transactionRepository.save(transaction);
                return true;
            }
        }

        // Fallback: try fuzzy matching by student name
        List<Child> allStudents = childRepository.findByActiveTrue();
        for (Child student : allStudents) {
            String fullName = student.getFullName();
            if (transaction.getDescription().toUpperCase().contains(fullName.toUpperCase())) {
                createPaymentRecord(transaction, student, false);
                transaction.markAsMatched("Auto-matched by name: " + fullName);
                transactionRepository.save(transaction);
                return true;
            }
        }

        return false;
    }

    private void createPaymentRecord(Transaction transaction, Child student, boolean autoMatched) {
        Payment payment = Payment.builder()
                .child(student)
                .transaction(transaction)
                .paymentMonth(transaction.getTransactionDate().getMonthValue())
                .paymentYear(transaction.getTransactionDate().getYear())
                .amountPaid(transaction.getAmount())
                .expectedAmount(student.getMonthlyFee())
                .paymentDate(transaction.getTransactionDate())
                .paymentMethod(Payment.PaymentMethod.BANK_TRANSFER)
                .transactionReference(transaction.getBankReference())
                .matchedAutomatically(autoMatched)
                .build();

        paymentRepository.save(payment);
        log.info("Created payment record for student {} - Amount: {}",
                student.getStudentNumber(), transaction.getAmount());
    }

    private String getCSVValue(CSVRecord record, String... possibleHeaders) {
        for (String header : possibleHeaders) {
            try {
                if (record.isMapped(header) && record.get(header) != null && !record.get(header).trim().isEmpty()) {
                    return record.get(header).trim();
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    private LocalDate parseDate(String dateStr) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate date = LocalDate.parse(dateStr, formatter);
                // Handle 2-digit years
                if (date.getYear() < 100) {
                    date = date.plusYears(2000);
                }
                return date;
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private BigDecimal parseAmount(String amountStr) {
        try {
            String cleaned = amountStr.replaceAll("[^0-9.]", "");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractPaymentReference(String description) {
        Matcher matcher = STUDENT_NUMBER_PATTERN.matcher(description);
        if (matcher.find()) {
            return matcher.group();
        }
        return description.length() > 50 ? description.substring(0, 50) : description;
    }

    private String generateBankReference(LocalDate date, BigDecimal amount, String description) {
        // Use UUID to ensure uniqueness even for duplicate transactions
        String uuid = java.util.UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s-%s-%s",
                date.toString(),
                amount.toString().replace(".", ""),
                uuid);
    }

    @Transactional(readOnly = true)
    public List<UploadedStatement> getAllStatements() {
        return uploadedStatementRepository.findByOrderByUploadDateDesc();
    }

    @Transactional(readOnly = true)
    public UploadedStatement getStatementById(Long id) {
        return uploadedStatementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Statement not found with id: " + id));
    }
}
