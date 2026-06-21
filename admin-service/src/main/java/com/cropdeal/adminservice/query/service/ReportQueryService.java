package com.cropdeal.adminservice.query.service;

import com.cropdeal.adminservice.dto.request.ReportFilterRequest;
import com.cropdeal.adminservice.dto.response.*;
import com.cropdeal.adminservice.query.model.AdminReport;
import com.cropdeal.adminservice.query.model.OrderReadModel;
import com.cropdeal.adminservice.query.model.PaymentReadModel;
import com.cropdeal.adminservice.query.repository.AdminReportRepository;
import com.cropdeal.adminservice.query.repository.OrderReadModelRepository;
import com.cropdeal.adminservice.query.repository.PaymentReadModelRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportQueryService {

    private final OrderReadModelRepository orderRepo;
    private final PaymentReadModelRepository paymentRepo;
    private final AdminReportRepository reportRepo;
    private final ExcelExportService excelExportService;
    private final ObjectMapper objectMapper;

    // ── Dashboard ─────────────────────────────────────────────────────────────

    public DashboardSummary getDashboardSummary() {
        return DashboardSummary.builder()
                .totalOrders(orderRepo.count())
                .completedOrders(orderRepo.countByStatus("COMPLETED"))
                .pendingOrders(orderRepo.countByStatus("PENDING"))
                .cancelledOrders(orderRepo.countByStatus("CANCELLED"))
                .totalPayments(paymentRepo.count())
                .successfulPayments(paymentRepo.countByStatus("SUCCESS"))
                .failedPayments(paymentRepo.countByStatus("FAILED"))
                .totalRevenue(nullSafe(orderRepo.totalRevenue()))
                .build();
    }

    // ── Order Report ──────────────────────────────────────────────────────────

    public PagedResponse<OrderReportRow> getOrderReport(ReportFilterRequest filter, Pageable pageable) {
        Page<OrderReadModel> page = orderRepo.findByFilters(
                filter.getStatus(), filter.getCropType(),
                filter.getDealerId(), filter.getFarmerId(),
                toStartOfDay(filter.getDateFrom()), toEndOfDay(filter.getDateTo()),
                pageable);
        return toPagedResponse(page.map(this::toOrderRow));
    }

    public byte[] exportOrderReportExcel(ReportFilterRequest filter, Long adminId) {
        List<OrderReadModel> rows = orderRepo.findAllByFilters(
                filter.getStatus(), filter.getCropType(),
                filter.getDealerId(), filter.getFarmerId(),
                toStartOfDay(filter.getDateFrom()), toEndOfDay(filter.getDateTo()));

        List<OrderReportRow> reportRows = rows.stream().map(this::toOrderRow).toList();
        persistReport("ORDER_REPORT", filter, reportRows, adminId);
        return excelExportService.exportOrderReport(reportRows);
    }

    // ── Payment Report ────────────────────────────────────────────────────────

    public PagedResponse<PaymentReportRow> getPaymentReport(ReportFilterRequest filter, Pageable pageable) {
        Page<PaymentReadModel> page = paymentRepo.findByFilters(
                filter.getStatus(), filter.getDealerId(), filter.getFarmerId(),
                toStartOfDay(filter.getDateFrom()), toEndOfDay(filter.getDateTo()),
                pageable);
        return toPagedResponse(page.map(this::toPaymentRow));
    }

    public byte[] exportPaymentReportExcel(ReportFilterRequest filter, Long adminId) {
        List<PaymentReadModel> rows = paymentRepo.findAllByFilters(
                filter.getStatus(), filter.getDealerId(), filter.getFarmerId(),
                toStartOfDay(filter.getDateFrom()), toEndOfDay(filter.getDateTo()));

        List<PaymentReportRow> reportRows = rows.stream().map(this::toPaymentRow).toList();
        persistReport("PAYMENT_REPORT", filter, reportRows, adminId);
        return excelExportService.exportPaymentReport(reportRows);
    }

    // ── Dealer Performance Report ─────────────────────────────────────────────

    public List<DealerReportRow> getDealerReport(ReportFilterRequest filter) {
        List<Object[]> summary = orderRepo.dealerRevenueSummary(
                toStartOfDay(filter.getDateFrom()), toEndOfDay(filter.getDateTo()));

        // Group all orders by dealer for cancelled/total counts
        Map<Long, List<OrderReadModel>> byDealer = orderRepo
                .findAllByFilters(null, null, null, null,
                        toStartOfDay(filter.getDateFrom()), toEndOfDay(filter.getDateTo()))
                .stream().collect(Collectors.groupingBy(OrderReadModel::getDealerId));

        return summary.stream().map(row -> {
            Long dealerId     = (Long)   row[0];
            Double revenue    = (Double) row[1];
            Long completed    = (Long)   row[2];
            List<OrderReadModel> dealerOrders = byDealer.getOrDefault(dealerId, List.of());
            long cancelled    = dealerOrders.stream().filter(o -> "CANCELLED".equals(o.getOrderStatus())).count();
            long total        = dealerOrders.size();
            return DealerReportRow.builder()
                    .dealerId(dealerId)
                    .totalOrders(total)
                    .completedOrders(completed)
                    .cancelledOrders(cancelled)
                    .totalRevenue(nullSafe(revenue))
                    .averageOrderValue(completed > 0 ? nullSafe(revenue) / completed : 0.0)
                    .build();
        }).toList();
    }

    public byte[] exportDealerReportExcel(ReportFilterRequest filter, Long adminId) {
        List<DealerReportRow> rows = getDealerReport(filter);
        persistReport("DEALER_REPORT", filter, rows, adminId);
        return excelExportService.exportDealerReport(rows);
    }

    // ── Report history ────────────────────────────────────────────────────────

    public PagedResponse<AdminReport> getReportHistory(String reportType, Pageable pageable) {
        Page<AdminReport> page = reportRepo.findByReportTypeOrderByGeneratedAtDesc(reportType, pageable);
        return toPagedResponse(page);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private OrderReportRow toOrderRow(OrderReadModel o) {
        return OrderReportRow.builder()
                .orderId(o.getOrderId()).farmerId(o.getFarmerId()).dealerId(o.getDealerId())
                .cropName(o.getCropName()).cropType(o.getCropType())
                .quantity(o.getQuantity()).agreedPricePerUnit(o.getAgreedPricePerUnit())
                .totalAmount(o.getTotalAmount()).orderStatus(o.getOrderStatus())
                .location(o.getLocation()).placedAt(o.getPlacedAt()).completedAt(o.getCompletedAt())
                .build();
    }

    private PaymentReportRow toPaymentRow(PaymentReadModel p) {
        return PaymentReportRow.builder()
                .paymentId(p.getPaymentId()).orderId(p.getOrderId())
                .farmerId(p.getFarmerId()).dealerId(p.getDealerId())
                .amount(p.getAmount()).paymentStatus(p.getPaymentStatus())
                .transactionId(p.getTransactionId()).receiptNumber(p.getReceiptNumber())
                .paidAt(p.getPaidAt())
                .build();
    }

    @Transactional
    private void persistReport(String type, ReportFilterRequest filter, Object data, Long adminId) {
        try {
            reportRepo.save(AdminReport.builder()
                    .reportType(type)
                    .filterBy(objectMapper.writeValueAsString(filter))
                    .reportData(objectMapper.writeValueAsString(data))
                    .generatedBy(adminId)
                    .build());
        } catch (JsonProcessingException e) {
            log.warn("Could not persist report metadata: {}", e.getMessage());
        }
    }

    private LocalDateTime toStartOfDay(java.time.LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    private LocalDateTime toEndOfDay(java.time.LocalDate date) {
        return date != null ? date.atTime(LocalTime.MAX) : null;
    }

    private double nullSafe(Double value) {
        return value != null ? value : 0.0;
    }

    private <T> PagedResponse<T> toPagedResponse(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent()).page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages())
                .last(page.isLast()).build();
    }
}
