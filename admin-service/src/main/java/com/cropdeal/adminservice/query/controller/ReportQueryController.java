package com.cropdeal.adminservice.query.controller;

import com.cropdeal.adminservice.dto.request.ReportFilterRequest;
import com.cropdeal.adminservice.dto.response.*;
import com.cropdeal.adminservice.query.model.AdminReport;
import com.cropdeal.adminservice.query.service.ReportQueryService;
import com.cropdeal.adminservice.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Report Queries", description = "CQRS read-side: order, payment, and dealer reports with Excel export")
@SecurityRequirement(name = "bearerAuth")
public class ReportQueryController {

    private final ReportQueryService reportQueryService;

    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard summary stats")
    public ResponseEntity<ApiResponse<DashboardSummary>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(reportQueryService.getDashboardSummary()));
    }

    // ── Order Report ──────────────────────────────────────────────────────────

    @GetMapping("/orders")
    @Operation(summary = "Paginated order report with optional filters")
    public ResponseEntity<ApiResponse<PagedResponse<OrderReportRow>>> getOrderReport(
            ReportFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "placedAt"));
        return ResponseEntity.ok(ApiResponse.success(
                reportQueryService.getOrderReport(filter, pageable)));
    }

    @GetMapping("/orders/export")
    @Operation(summary = "Export order report as Excel (.xlsx)")
    public ResponseEntity<byte[]> exportOrderReport(
            ReportFilterRequest filter,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        byte[] excel = reportQueryService.exportOrderReportExcel(filter, principal.getUserId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"order-report-" + timestamp() + ".xlsx\"")
                .contentType(MediaType.parseMediaType(XLSX_MIME))
                .body(excel);
    }

    // ── Payment Report ────────────────────────────────────────────────────────

    @GetMapping("/payments")
    @Operation(summary = "Paginated payment report with optional filters")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentReportRow>>> getPaymentReport(
            ReportFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "paidAt"));
        return ResponseEntity.ok(ApiResponse.success(
                reportQueryService.getPaymentReport(filter, pageable)));
    }

    @GetMapping("/payments/export")
    @Operation(summary = "Export payment report as Excel (.xlsx)")
    public ResponseEntity<byte[]> exportPaymentReport(
            ReportFilterRequest filter,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        byte[] excel = reportQueryService.exportPaymentReportExcel(filter, principal.getUserId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"payment-report-" + timestamp() + ".xlsx\"")
                .contentType(MediaType.parseMediaType(XLSX_MIME))
                .body(excel);
    }

    // ── Dealer Performance Report ─────────────────────────────────────────────

    @GetMapping("/dealers")
    @Operation(summary = "Dealer performance summary (revenue, order counts)")
    public ResponseEntity<ApiResponse<List<DealerReportRow>>> getDealerReport(
            ReportFilterRequest filter) {

        return ResponseEntity.ok(ApiResponse.success(reportQueryService.getDealerReport(filter)));
    }

    @GetMapping("/dealers/export")
    @Operation(summary = "Export dealer performance report as Excel (.xlsx)")
    public ResponseEntity<byte[]> exportDealerReport(
            ReportFilterRequest filter,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        byte[] excel = reportQueryService.exportDealerReportExcel(filter, principal.getUserId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"dealer-report-" + timestamp() + ".xlsx\"")
                .contentType(MediaType.parseMediaType(XLSX_MIME))
                .body(excel);
    }

    // ── Report History ────────────────────────────────────────────────────────

    @GetMapping("/history")
    @Operation(summary = "Get previously generated report metadata")
    public ResponseEntity<ApiResponse<PagedResponse<AdminReport>>> getReportHistory(
            @RequestParam(defaultValue = "ORDER_REPORT") String reportType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                reportQueryService.getReportHistory(reportType, pageable)));
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }
}
