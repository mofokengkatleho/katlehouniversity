package com.katlehouniversity.ecd.controller;

import com.katlehouniversity.ecd.dto.MonthlyReportDto;
import com.katlehouniversity.ecd.service.ExcelExportService;
import com.katlehouniversity.ecd.service.PdfExportService;
import com.katlehouniversity.ecd.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Reports", description = "Monthly payment reports and export endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;
    private final PdfExportService pdfExportService;
    private final ExcelExportService excelExportService;

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyReportDto> getMonthlyReport(
            @RequestParam Integer month,
            @RequestParam Integer year) {
        return ResponseEntity.ok(reportService.generateMonthlyReport(month, year));
    }

    @GetMapping("/monthly/current")
    public ResponseEntity<MonthlyReportDto> getCurrentMonthReport() {
        return ResponseEntity.ok(reportService.getCurrentMonthReport());
    }

    @Operation(
        summary = "Export monthly report as PDF",
        description = "Generate and download a professional PDF report with payment statistics, paid students, and owing students for the specified month."
    )
    @ApiResponse(
        responseCode = "200",
        description = "PDF file generated successfully",
        content = @Content(mediaType = "application/pdf")
    )
    @GetMapping("/monthly/export/pdf")
    public ResponseEntity<byte[]> exportMonthlyReportPdf(
            @Parameter(description = "Month number (1-12)", required = true, example = "1")
            @RequestParam Integer month,
            @Parameter(description = "Year", required = true, example = "2025")
            @RequestParam Integer year) {

        // Generate report data
        MonthlyReportDto report = reportService.generateMonthlyReport(month, year);

        // Generate PDF
        byte[] pdfBytes = pdfExportService.generateMonthlyReportPdf(report, month, year);

        // Set headers for file download
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
            String.format("monthly-report-%d-%02d.pdf", year, month));
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @Operation(
        summary = "Export monthly report as Excel",
        description = "Generate and download an Excel workbook with multiple sheets (Summary, Paid Students, Owing Students) for data analysis."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Excel file generated successfully",
        content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    )
    @GetMapping("/monthly/export/excel")
    public ResponseEntity<byte[]> exportMonthlyReportExcel(
            @Parameter(description = "Month number (1-12)", required = true, example = "1")
            @RequestParam Integer month,
            @Parameter(description = "Year", required = true, example = "2025")
            @RequestParam Integer year) {

        // Generate report data
        MonthlyReportDto report = reportService.generateMonthlyReport(month, year);

        // Generate Excel
        byte[] excelBytes = excelExportService.generateMonthlyReportExcel(report, month, year);

        // Set headers for file download
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment",
            String.format("monthly-report-%d-%02d.xlsx", year, month));
        headers.setContentLength(excelBytes.length);

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }
}
