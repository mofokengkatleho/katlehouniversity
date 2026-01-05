package com.katlehouniversity.ecd.service;

import com.katlehouniversity.ecd.dto.MonthlyReportDto;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Service for generating PDF reports from monthly report data
 */
@Service
@Slf4j
public class PdfExportService {

    /**
     * Generate PDF report for monthly payment data
     *
     * @param report Monthly report data
     * @param month  Month number (1-12)
     * @param year   Year
     * @return PDF file as byte array
     */
    public byte[] generateMonthlyReportPdf(MonthlyReportDto report, int month, int year) {
        log.info("Generating PDF report for {}/{}", month, year);

        String html = buildHtmlReport(report, month, year);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();

            byte[] pdfBytes = outputStream.toByteArray();
            log.info("PDF report generated successfully. Size: {} bytes", pdfBytes.length);
            return pdfBytes;

        } catch (Exception e) {
            log.error("Failed to generate PDF report", e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build HTML content for the report
     */
    private String buildHtmlReport(MonthlyReportDto report, int month, int year) {
        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        // Calculate percentages
        double paidPercentage = report.getTotalChildren() > 0
            ? (report.getPaidCount() * 100.0) / report.getTotalChildren()
            : 0.0;

        double collectionRate = report.getTotalExpected() != null && report.getTotalExpected().compareTo(BigDecimal.ZERO) > 0
            ? (report.getTotalCollected().multiply(BigDecimal.valueOf(100)).divide(report.getTotalExpected(), 2, java.math.RoundingMode.HALF_UP)).doubleValue()
            : 0.0;

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8"/>
                <style>
                    body {
                        font-family: 'Arial', sans-serif;
                        padding: 40px;
                        color: #333;
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 40px;
                        border-bottom: 3px solid #1e40af;
                        padding-bottom: 20px;
                    }
                    h1 {
                        color: #1e40af;
                        margin: 0;
                        font-size: 28px;
                    }
                    .subtitle {
                        color: #666;
                        font-size: 16px;
                        margin-top: 10px;
                    }
                    .summary {
                        background: #f3f4f6;
                        padding: 20px;
                        border-radius: 8px;
                        margin: 30px 0;
                        border-left: 4px solid #1e40af;
                    }
                    .summary-grid {
                        display: grid;
                        grid-template-columns: 1fr 1fr;
                        gap: 15px;
                        margin-top: 15px;
                    }
                    .summary-item {
                        padding: 10px;
                        background: white;
                        border-radius: 4px;
                    }
                    .summary-label {
                        font-size: 12px;
                        color: #666;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                    }
                    .summary-value {
                        font-size: 24px;
                        font-weight: bold;
                        margin-top: 5px;
                    }
                    .paid { color: #16a34a; }
                    .owing { color: #dc2626; }
                    table {
                        width: 100%%;
                        border-collapse: collapse;
                        margin-top: 20px;
                        background: white;
                    }
                    th {
                        background: #1e40af;
                        color: white;
                        padding: 12px;
                        text-align: left;
                        font-weight: 600;
                        font-size: 14px;
                    }
                    td {
                        padding: 10px 12px;
                        border-bottom: 1px solid #e5e7eb;
                        font-size: 13px;
                    }
                    tr:hover {
                        background: #f9fafb;
                    }
                    .section-title {
                        color: #1e40af;
                        font-size: 20px;
                        margin-top: 40px;
                        margin-bottom: 10px;
                        padding-bottom: 10px;
                        border-bottom: 2px solid #e5e7eb;
                    }
                    .footer {
                        margin-top: 50px;
                        padding-top: 20px;
                        border-top: 2px solid #e5e7eb;
                        text-align: center;
                        color: #666;
                        font-size: 12px;
                    }
                    .amount {
                        text-align: right;
                        font-weight: 600;
                    }
                    .status-badge {
                        padding: 4px 8px;
                        border-radius: 4px;
                        font-size: 11px;
                        font-weight: 600;
                        text-transform: uppercase;
                    }
                    .status-paid {
                        background: #dcfce7;
                        color: #16a34a;
                    }
                    .status-owing {
                        background: #fee2e2;
                        color: #dc2626;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>Katlehong University ECD Center</h1>
                    <div class="subtitle">Monthly Payment Report - %s %d</div>
                </div>

                <div class="summary">
                    <h2 style="margin-top: 0; color: #1e40af;">Summary</h2>
                    <div class="summary-grid">
                        <div class="summary-item">
                            <div class="summary-label">Total Students</div>
                            <div class="summary-value">%d</div>
                        </div>
                        <div class="summary-item">
                            <div class="summary-label">Students Paid</div>
                            <div class="summary-value paid">%d (%.1f%%)</div>
                        </div>
                        <div class="summary-item">
                            <div class="summary-label">Students Owing</div>
                            <div class="summary-value owing">%d</div>
                        </div>
                        <div class="summary-item">
                            <div class="summary-label">Collection Rate</div>
                            <div class="summary-value" style="color: #1e40af;">%.1f%%</div>
                        </div>
                        <div class="summary-item">
                            <div class="summary-label">Total Collected</div>
                            <div class="summary-value paid">R %,.2f</div>
                        </div>
                        <div class="summary-item">
                            <div class="summary-label">Total Outstanding</div>
                            <div class="summary-value owing">R %,.2f</div>
                        </div>
                    </div>
                </div>

                <h2 class="section-title">Paid Students (%d)</h2>
                <table>
                    <thead>
                        <tr>
                            <th>Student Reference</th>
                            <th>Name</th>
                            <th style="text-align: right;">Monthly Fee</th>
                            <th style="text-align: right;">Amount Paid</th>
                            <th>Payment Date</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody>
                        %s
                    </tbody>
                </table>

                <h2 class="section-title">Owing Students (%d)</h2>
                <table>
                    <thead>
                        <tr>
                            <th>Student Reference</th>
                            <th>Name</th>
                            <th style="text-align: right;">Monthly Fee</th>
                            <th style="text-align: right;">Amount Paid</th>
                            <th style="text-align: right;">Outstanding</th>
                        </tr>
                    </thead>
                    <tbody>
                        %s
                    </tbody>
                </table>

                <div class="footer">
                    <p>Generated on %s</p>
                    <p>Katlehong University ECD Center - Payment Reconciliation System</p>
                </div>
            </body>
            </html>
            """,
            monthName, year,
            report.getTotalChildren(),
            report.getPaidCount(), paidPercentage,
            report.getOwingCount(),
            collectionRate,
            report.getTotalCollected(),
            report.getTotalOutstanding(),
            report.getPaidChildren().size(),
            buildPaidTableRows(report.getPaidChildren()),
            report.getOwingChildren().size(),
            buildOwingTableRows(report.getOwingChildren()),
            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm"))
        );
    }

    /**
     * Build HTML table rows for paid students
     */
    private String buildPaidTableRows(List<MonthlyReportDto.ChildPaymentStatus> paidChildren) {
        if (paidChildren.isEmpty()) {
            return "<tr><td colspan='6' style='text-align: center; color: #666;'>No paid students for this period</td></tr>";
        }

        return paidChildren.stream()
            .map(child -> String.format(
                "<tr>" +
                "<td>%s</td>" +
                "<td>%s</td>" +
                "<td class='amount'>R %,.2f</td>" +
                "<td class='amount'>R %,.2f</td>" +
                "<td>%s</td>" +
                "<td><span class='status-badge status-paid'>Paid</span></td>" +
                "</tr>",
                escapeHtml(child.getPaymentReference()),
                escapeHtml(child.getFullName()),
                child.getMonthlyFee(),
                child.getAmountPaid(),
                child.getPaymentDate() != null ? child.getPaymentDate() : "N/A"
            ))
            .collect(Collectors.joining("\n"));
    }

    /**
     * Build HTML table rows for owing students
     */
    private String buildOwingTableRows(List<MonthlyReportDto.ChildPaymentStatus> owingChildren) {
        if (owingChildren.isEmpty()) {
            return "<tr><td colspan='5' style='text-align: center; color: #666;'>All students have paid for this period</td></tr>";
        }

        return owingChildren.stream()
            .map(child -> {
                BigDecimal amountPaid = child.getAmountPaid() != null ? child.getAmountPaid() : BigDecimal.ZERO;
                BigDecimal outstanding = child.getOutstanding() != null ? child.getOutstanding() : child.getMonthlyFee();

                return String.format(
                    "<tr>" +
                    "<td>%s</td>" +
                    "<td>%s</td>" +
                    "<td class='amount'>R %,.2f</td>" +
                    "<td class='amount'>R %,.2f</td>" +
                    "<td class='amount owing'>R %,.2f</td>" +
                    "</tr>",
                    escapeHtml(child.getPaymentReference()),
                    escapeHtml(child.getFullName()),
                    child.getMonthlyFee(),
                    amountPaid,
                    outstanding
                );
            })
            .collect(Collectors.joining("\n"));
    }

    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
