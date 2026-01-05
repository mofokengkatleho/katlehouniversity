package com.katlehouniversity.ecd.service;

import com.katlehouniversity.ecd.dto.MonthlyReportDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Service for generating Excel reports from monthly report data
 */
@Service
@Slf4j
public class ExcelExportService {

    /**
     * Generate Excel report for monthly payment data
     *
     * @param report Monthly report data
     * @param month  Month number (1-12)
     * @param year   Year
     * @return Excel file as byte array
     */
    public byte[] generateMonthlyReportExcel(MonthlyReportDto report, int month, int year) {
        log.info("Generating Excel report for {}/{}", month, year);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Create summary sheet
            Sheet summarySheet = workbook.createSheet("Summary");
            createSummarySheet(summarySheet, report, month, year, workbook);

            // Create paid students sheet
            Sheet paidSheet = workbook.createSheet("Paid Students");
            createPaidStudentsSheet(paidSheet, report.getPaidChildren(), workbook);

            // Create owing students sheet
            Sheet owingSheet = workbook.createSheet("Owing Students");
            createOwingStudentsSheet(owingSheet, report.getOwingChildren(), workbook);

            workbook.write(outputStream);
            byte[] excelBytes = outputStream.toByteArray();
            log.info("Excel report generated successfully. Size: {} bytes", excelBytes.length);
            return excelBytes;

        } catch (Exception e) {
            log.error("Failed to generate Excel report", e);
            throw new RuntimeException("Excel generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Create summary sheet with overall statistics
     */
    private void createSummarySheet(Sheet sheet, MonthlyReportDto report, int month, int year, Workbook workbook) {
        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        // Create styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle labelStyle = createLabelStyle(workbook);
        CellStyle valueStyle = createValueStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Monthly Payment Report - " + monthName + " " + year);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));

        rowNum++; // Empty row

        // Summary section
        createSummaryRow(sheet, rowNum++, "Total Students", report.getTotalChildren(), labelStyle, valueStyle);
        createSummaryRow(sheet, rowNum++, "Students Paid", report.getPaidCount(), labelStyle, valueStyle);
        createSummaryRow(sheet, rowNum++, "Students Owing", report.getOwingCount(), labelStyle, valueStyle);

        rowNum++; // Empty row

        createSummaryCurrencyRow(sheet, rowNum++, "Total Expected", report.getTotalExpected(), labelStyle, currencyStyle);
        createSummaryCurrencyRow(sheet, rowNum++, "Total Collected", report.getTotalCollected(), labelStyle, currencyStyle);
        createSummaryCurrencyRow(sheet, rowNum++, "Total Outstanding", report.getTotalOutstanding(), labelStyle, currencyStyle);

        // Calculate collection rate
        if (report.getTotalExpected() != null && report.getTotalExpected().compareTo(BigDecimal.ZERO) > 0) {
            double collectionRate = report.getTotalCollected()
                .multiply(BigDecimal.valueOf(100))
                .divide(report.getTotalExpected(), 2, java.math.RoundingMode.HALF_UP)
                .doubleValue();

            rowNum++;
            Row collectionRow = sheet.createRow(rowNum++);
            Cell labelCell = collectionRow.createCell(0);
            labelCell.setCellValue("Collection Rate");
            labelCell.setCellStyle(labelStyle);

            Cell valueCell = collectionRow.createCell(1);
            valueCell.setCellValue(String.format("%.2f%%", collectionRate));
            valueCell.setCellStyle(valueStyle);
        }

        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.setColumnWidth(0, 6000);
        sheet.setColumnWidth(1, 4000);
    }

    /**
     * Create paid students sheet
     */
    private void createPaidStudentsSheet(Sheet sheet, List<MonthlyReportDto.ChildPaymentStatus> paidChildren, Workbook workbook) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        // Header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Student Reference", "Name", "Monthly Fee", "Amount Paid", "Payment Date", "Status"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        int rowNum = 1;
        for (MonthlyReportDto.ChildPaymentStatus child : paidChildren) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(child.getPaymentReference());
            row.createCell(1).setCellValue(child.getFullName());

            Cell feeCell = row.createCell(2);
            feeCell.setCellValue(child.getMonthlyFee().doubleValue());
            feeCell.setCellStyle(currencyStyle);

            Cell paidCell = row.createCell(3);
            paidCell.setCellValue(child.getAmountPaid().doubleValue());
            paidCell.setCellStyle(currencyStyle);

            row.createCell(4).setCellValue(child.getPaymentDate() != null ? child.getPaymentDate() : "N/A");
            row.createCell(5).setCellValue("PAID");
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Create owing students sheet
     */
    private void createOwingStudentsSheet(Sheet sheet, List<MonthlyReportDto.ChildPaymentStatus> owingChildren, Workbook workbook) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        CellStyle warningCurrencyStyle = createWarningCurrencyStyle(workbook);

        // Header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Student Reference", "Name", "Monthly Fee", "Amount Paid", "Outstanding"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        int rowNum = 1;
        for (MonthlyReportDto.ChildPaymentStatus child : owingChildren) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(child.getPaymentReference());
            row.createCell(1).setCellValue(child.getFullName());

            Cell feeCell = row.createCell(2);
            feeCell.setCellValue(child.getMonthlyFee().doubleValue());
            feeCell.setCellStyle(currencyStyle);

            BigDecimal amountPaid = child.getAmountPaid() != null ? child.getAmountPaid() : BigDecimal.ZERO;
            Cell paidCell = row.createCell(3);
            paidCell.setCellValue(amountPaid.doubleValue());
            paidCell.setCellStyle(currencyStyle);

            BigDecimal outstanding = child.getOutstanding() != null ? child.getOutstanding() : child.getMonthlyFee();
            Cell outstandingCell = row.createCell(4);
            outstandingCell.setCellValue(outstanding.doubleValue());
            outstandingCell.setCellStyle(warningCurrencyStyle);
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Helper method to create summary row with label and value
     */
    private void createSummaryRow(Sheet sheet, int rowNum, String label, long value, CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowNum);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);

        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(valueStyle);
    }

    /**
     * Helper method to create summary row with currency value
     */
    private void createSummaryCurrencyRow(Sheet sheet, int rowNum, String label, BigDecimal value, CellStyle labelStyle, CellStyle currencyStyle) {
        Row row = sheet.createRow(rowNum);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);

        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value != null ? value.doubleValue() : 0.0);
        valueCell.setCellStyle(currencyStyle);
    }

    /**
     * Create header cell style (blue background, white text, bold)
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    /**
     * Create title style (large, bold)
     */
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        return style;
    }

    /**
     * Create label style (bold)
     */
    private CellStyle createLabelStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    /**
     * Create value style (regular)
     */
    private CellStyle createValueStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        return style;
    }

    /**
     * Create currency style (currency format)
     */
    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("R #,##0.00"));
        return style;
    }

    /**
     * Create warning currency style (red text for outstanding amounts)
     */
    private CellStyle createWarningCurrencyStyle(Workbook workbook) {
        CellStyle style = createCurrencyStyle(workbook);
        Font font = workbook.createFont();
        font.setColor(IndexedColors.RED.getIndex());
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}
