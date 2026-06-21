package com.cropdeal.adminservice;

import com.cropdeal.adminservice.command.model.AddOn;
import com.cropdeal.adminservice.command.model.AddOnRepository;
import com.cropdeal.adminservice.command.service.AddOnCommandService;
import com.cropdeal.adminservice.consumer.AdminEventConsumer;
import com.cropdeal.adminservice.dto.request.AddOnRequest;
import com.cropdeal.adminservice.dto.request.ReportFilterRequest;
import com.cropdeal.adminservice.dto.response.*;
import com.cropdeal.adminservice.event.OrderPlacedEvent;
import com.cropdeal.adminservice.event.PaymentFailedEvent;
import com.cropdeal.adminservice.event.PaymentSuccessEvent;
import com.cropdeal.adminservice.exception.DuplicateEntryException;
import com.cropdeal.adminservice.exception.ResourceNotFoundException;
import com.cropdeal.adminservice.query.model.OrderReadModel;
import com.cropdeal.adminservice.query.model.PaymentReadModel;
import com.cropdeal.adminservice.query.repository.AdminReportRepository;
import com.cropdeal.adminservice.query.repository.OrderReadModelRepository;
import com.cropdeal.adminservice.query.repository.PaymentReadModelRepository;
import com.cropdeal.adminservice.query.service.ExcelExportService;
import com.cropdeal.adminservice.query.service.ReportQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    // ── AddOnCommandService ───────────────────────────────────────────────────
    @Mock AddOnRepository addOnRepository;
    @InjectMocks AddOnCommandService addOnCommandService;

    // ── AdminEventConsumer ────────────────────────────────────────────────────
    @Mock OrderReadModelRepository orderReadModelRepository;
    @Mock PaymentReadModelRepository paymentReadModelRepository;
    @InjectMocks AdminEventConsumer adminEventConsumer;

    // ── ReportQueryService ────────────────────────────────────────────────────
    @Mock AdminReportRepository adminReportRepository;
    @Mock ExcelExportService excelExportService;
    @Mock ObjectMapper objectMapper;
    @InjectMocks ReportQueryService reportQueryService;

    private OrderReadModel sampleOrder;
    private PaymentReadModel samplePayment;

    @BeforeEach
    void setUp() {
        // Wire shared mocks into ReportQueryService
        ReflectionTestUtils.setField(reportQueryService, "orderRepo", orderReadModelRepository);
        ReflectionTestUtils.setField(reportQueryService, "paymentRepo", paymentReadModelRepository);

        sampleOrder = OrderReadModel.builder()
                .orderId(1L).farmerId(10L).dealerId(20L)
                .quantity(100.0).agreedPricePerUnit(50.0).totalAmount(5000.0)
                .orderStatus("COMPLETED").cropType("VEGETABLE").cropName("Tomato")
                .placedAt(LocalDateTime.now().minusDays(1))
                .completedAt(LocalDateTime.now())
                .build();

        samplePayment = PaymentReadModel.builder()
                .paymentId(100L).orderId(1L).farmerId(10L).dealerId(20L)
                .amount(5000.0).paymentStatus("SUCCESS")
                .transactionId("TXN-ABC").receiptNumber("RCP-2024-000001")
                .paidAt(LocalDateTime.now())
                .build();
    }

    // ── AddOnCommandService ───────────────────────────────────────────────────

    @Test
    void createAddOn_success() {
        AddOnRequest request = new AddOnRequest();
        request.setName("Premium Listing");
        request.setDescription("Boost your crop listing");

        AddOn saved = AddOn.builder().id(1L).name("Premium Listing")
                .description("Boost your crop listing").active(true).build();

        when(addOnRepository.existsByName("Premium Listing")).thenReturn(false);
        when(addOnRepository.save(any())).thenReturn(saved);

        AddOnResponse result = addOnCommandService.create(request);

        assertThat(result.getName()).isEqualTo("Premium Listing");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void createAddOn_duplicate_throwsException() {
        AddOnRequest request = new AddOnRequest();
        request.setName("Premium Listing");

        when(addOnRepository.existsByName("Premium Listing")).thenReturn(true);

        assertThatThrownBy(() -> addOnCommandService.create(request))
                .isInstanceOf(DuplicateEntryException.class)
                .hasMessageContaining("Premium Listing");
    }

    @Test
    void toggleAddOn_flipsActiveStatus() {
        AddOn addOn = AddOn.builder().id(1L).name("Test").active(true).build();
        when(addOnRepository.findById(1L)).thenReturn(Optional.of(addOn));
        when(addOnRepository.save(any())).thenReturn(addOn);

        addOnCommandService.toggleActive(1L);

        assertThat(addOn.isActive()).isFalse();
        verify(addOnRepository).save(addOn);
    }

    @Test
    void deleteAddOn_notFound_throwsException() {
        when(addOnRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> addOnCommandService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── AdminEventConsumer (CQRS projection) ──────────────────────────────────

    @Test
    void onOrderPlaced_createsOrderReadModel() {
        OrderPlacedEvent event = new OrderPlacedEvent();
        event.setOrderId(1L);
        event.setCropListingId(100L);
        event.setFarmerId(10L);
        event.setDealerId(20L);
        event.setQuantity(100.0);
        event.setAgreedPricePerUnit(50.0);
        event.setTotalAmount(5000.0);
        event.setPlacedAt(LocalDateTime.now());

        when(orderReadModelRepository.existsByOrderId(1L)).thenReturn(false);

        adminEventConsumer.onOrderPlaced(event);

        ArgumentCaptor<OrderReadModel> captor = ArgumentCaptor.forClass(OrderReadModel.class);
        verify(orderReadModelRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(1L);
        assertThat(captor.getValue().getOrderStatus()).isEqualTo("PENDING");
    }

    @Test
    void onOrderPlaced_duplicate_skipsProjection() {
        OrderPlacedEvent event = new OrderPlacedEvent();
        event.setOrderId(1L);

        when(orderReadModelRepository.existsByOrderId(1L)).thenReturn(true);

        adminEventConsumer.onOrderPlaced(event);

        verify(orderReadModelRepository, never()).save(any());
    }

    @Test
    void onPaymentSuccess_updatesOrderStatusToCompleted() {
        PaymentSuccessEvent event = new PaymentSuccessEvent();
        event.setPaymentId(100L);
        event.setOrderId(1L);
        event.setFarmerId(10L);
        event.setDealerId(20L);
        event.setAmount(5000.0);
        event.setTransactionId("TXN-ABC");
        event.setReceiptNumber("RCP-2024-000001");
        event.setPaidAt(LocalDateTime.now());

        when(paymentReadModelRepository.existsByPaymentId(100L)).thenReturn(false);
        when(orderReadModelRepository.findByOrderId(1L)).thenReturn(Optional.of(sampleOrder));

        adminEventConsumer.onPaymentSuccess(event);

        verify(paymentReadModelRepository).save(any(PaymentReadModel.class));
        assertThat(sampleOrder.getOrderStatus()).isEqualTo("COMPLETED");
        verify(orderReadModelRepository).save(sampleOrder);
    }

    @Test
    void onPaymentFailed_createsFailedPaymentReadModel() {
        PaymentFailedEvent event = new PaymentFailedEvent();
        event.setPaymentId(101L);
        event.setOrderId(1L);
        event.setFarmerId(10L);
        event.setDealerId(20L);
        event.setAmount(5000.0);
        event.setTransactionId("TXN-FAIL");
        event.setFailureReason("INSUFFICIENT_FUNDS");
        event.setFailedAt(LocalDateTime.now());

        when(paymentReadModelRepository.existsByPaymentId(101L)).thenReturn(false);

        adminEventConsumer.onPaymentFailed(event);

        ArgumentCaptor<PaymentReadModel> captor = ArgumentCaptor.forClass(PaymentReadModel.class);
        verify(paymentReadModelRepository).save(captor.capture());
        assertThat(captor.getValue().getPaymentStatus()).isEqualTo("FAILED");
        assertThat(captor.getValue().getPaymentId()).isEqualTo(101L);
    }

    // ── ReportQueryService ────────────────────────────────────────────────────

    @Test
    void getDashboardSummary_returnsCorrectCounts() {
        when(orderReadModelRepository.count()).thenReturn(100L);
        when(orderReadModelRepository.countByStatus("COMPLETED")).thenReturn(70L);
        when(orderReadModelRepository.countByStatus("PENDING")).thenReturn(20L);
        when(orderReadModelRepository.countByStatus("CANCELLED")).thenReturn(10L);
        when(paymentReadModelRepository.count()).thenReturn(80L);
        when(paymentReadModelRepository.countByStatus("SUCCESS")).thenReturn(70L);
        when(paymentReadModelRepository.countByStatus("FAILED")).thenReturn(10L);
        when(orderReadModelRepository.totalRevenue()).thenReturn(500000.0);

        DashboardSummary summary = reportQueryService.getDashboardSummary();

        assertThat(summary.getTotalOrders()).isEqualTo(100L);
        assertThat(summary.getCompletedOrders()).isEqualTo(70L);
        assertThat(summary.getTotalRevenue()).isEqualTo(500000.0);
    }

    @Test
    void getOrderReport_returnsPagedRows() {
        when(orderReadModelRepository.findByFilters(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleOrder)));

        PagedResponse<OrderReportRow> result = reportQueryService
                .getOrderReport(new ReportFilterRequest(), PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOrderId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getCropType()).isEqualTo("VEGETABLE");
    }

    // ── ExcelExportService ────────────────────────────────────────────────────

    @Test
    void exportOrderReport_producesValidXlsx() throws Exception {
        ExcelExportService service = new ExcelExportService();

        List<OrderReportRow> rows = List.of(
                OrderReportRow.builder()
                        .orderId(1L).farmerId(10L).dealerId(20L)
                        .cropName("Tomato").cropType("VEGETABLE")
                        .quantity(100.0).agreedPricePerUnit(50.0).totalAmount(5000.0)
                        .orderStatus("COMPLETED").location("Pune")
                        .placedAt(LocalDateTime.now())
                        .build());

        byte[] result = service.exportOrderReport(rows);

        assertThat(result).isNotEmpty();
        // Verify it's a valid XLSX workbook
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(1);
            assertThat(wb.getSheetAt(0).getSheetName()).isEqualTo("Order Report");
            // Row 0 = title, Row 1 = header, Row 2 = first data row
            assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(2);
        }
    }

    @Test
    void exportDealerReport_includesSummaryRow() throws Exception {
        ExcelExportService service = new ExcelExportService();

        List<DealerReportRow> rows = List.of(
                DealerReportRow.builder()
                        .dealerId(20L).totalOrders(10).completedOrders(8)
                        .cancelledOrders(2).totalRevenue(40000.0).averageOrderValue(5000.0)
                        .build(),
                DealerReportRow.builder()
                        .dealerId(21L).totalOrders(5).completedOrders(4)
                        .cancelledOrders(1).totalRevenue(20000.0).averageOrderValue(5000.0)
                        .build());

        byte[] result = service.exportDealerReport(rows);

        assertThat(result).isNotEmpty();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            // Row 0 = title, Row 1 = header, Row 2-3 = data, Row 5 = summary (rowNum+1)
            assertThat(wb.getSheetAt(0).getLastRowNum()).isGreaterThanOrEqualTo(4);
        }
    }
}
