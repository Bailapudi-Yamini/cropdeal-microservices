package com.cropdeal.adminservice.query.service;

import com.cropdeal.adminservice.dto.response.DealerReportRow;
import com.cropdeal.adminservice.dto.response.OrderReportRow;
import com.cropdeal.adminservice.dto.response.PaymentReportRow;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class ExcelExportService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ── Order Report ──────────────────────────────────────────────────────────

    public byte[] exportOrderReport(List<OrderReportRow> rows) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Order Report");

            CellStyle headerStyle = buildHeaderStyle(wb);
            CellStyle dataStyle   = buildDataStyle(wb);
            CellStyle moneyStyle  = buildMoneyStyle(wb);

            // Title row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("CropDeal — Order Report");
            titleCell.setCellStyle(buildTitleStyle(wb));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 11));

            // Header row
            String[] headers = {"Order ID", "Farmer ID", "Dealer ID", "Crop Name", "Crop Type",
                    "Quantity", "Price/Unit (₹)", "Total (₹)", "Status", "Location",
                    "Placed At", "Completed At"};
            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 2;
            for (OrderReportRow r : rows) {
                Row row = sheet.createRow(rowNum++);
                setCell(row, 0, r.getOrderId(),            dataStyle);
                setCell(row, 1, r.getFarmerId(),           dataStyle);
                setCell(row, 2, r.getDealerId(),           dataStyle);
                setCell(row, 3, r.getCropName(),           dataStyle);
                setCell(row, 4, r.getCropType(),           dataStyle);
                setCell(row, 5, r.getQuantity(),           dataStyle);
                setCell(row, 6, r.getAgreedPricePerUnit(), moneyStyle);
                setCell(row, 7, r.getTotalAmount(),        moneyStyle);
                setCell(row, 8, r.getOrderStatus(),        dataStyle);
                setCell(row, 9, r.getLocation(),           dataStyle);
                setCell(row, 10, r.getPlacedAt()    != null ? r.getPlacedAt().format(FMT)    : "", dataStyle);
                setCell(row, 11, r.getCompletedAt() != null ? r.getCompletedAt().format(FMT) : "", dataStyle);
            }

            autoSizeColumns(sheet, headers.length);
            return toBytes(wb);
        } catch (IOException e) {
            log.error("Failed to generate order report Excel: {}", e.getMessage());
            throw new RuntimeException("Excel generation failed", e);
        }
    }

    // ── Payment Report ────────────────────────────────────────────────────────

    public byte[] exportPaymentReport(List<PaymentReportRow> rows) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Payment Report");

            CellStyle headerStyle = buildHeaderStyle(wb);
            CellStyle dataStyle   = buildDataStyle(wb);
            CellStyle moneyStyle  = buildMoneyStyle(wb);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("CropDeal — Payment Report");
            titleCell.setCellStyle(buildTitleStyle(wb));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

            String[] headers = {"Payment ID", "Order ID", "Farmer ID", "Dealer ID",
                    "Amount (₹)", "Status", "Transaction ID", "Receipt No.", "Paid At"};
            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 2;
            for (PaymentReportRow r : rows) {
                Row row = sheet.createRow(rowNum++);
                setCell(row, 0, r.getPaymentId(),     dataStyle);
                setCell(row, 1, r.getOrderId(),       dataStyle);
                setCell(row, 2, r.getFarmerId(),      dataStyle);
                setCell(row, 3, r.getDealerId(),      dataStyle);
                setCell(row, 4, r.getAmount(),        moneyStyle);
                setCell(row, 5, r.getPaymentStatus(), dataStyle);
                setCell(row, 6, r.getTransactionId(), dataStyle);
                setCell(row, 7, r.getReceiptNumber(), dataStyle);
                setCell(row, 8, r.getPaidAt() != null ? r.getPaidAt().format(FMT) : "", dataStyle);
            }

            autoSizeColumns(sheet, headers.length);
            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Excel generation failed", e);
        }
    }

    // ── Dealer Performance Report ─────────────────────────────────────────────

    public byte[] exportDealerReport(List<DealerReportRow> rows) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Dealer Performance");

            CellStyle headerStyle = buildHeaderStyle(wb);
            CellStyle dataStyle   = buildDataStyle(wb);
            CellStyle moneyStyle  = buildMoneyStyle(wb);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("CropDeal — Dealer Performance Report");
            titleCell.setCellStyle(buildTitleStyle(wb));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            String[] headers = {"Dealer ID", "Total Orders", "Completed", "Cancelled",
                    "Total Revenue (₹)", "Avg Order Value (₹)"};
            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 2;
            for (DealerReportRow r : rows) {
                Row row = sheet.createRow(rowNum++);
                setCell(row, 0, r.getDealerId(),         dataStyle);
                setCell(row, 1, (double) r.getTotalOrders(),     dataStyle);
                setCell(row, 2, (double) r.getCompletedOrders(), dataStyle);
                setCell(row, 3, (double) r.getCancelledOrders(), dataStyle);
                setCell(row, 4, r.getTotalRevenue(),     moneyStyle);
                setCell(row, 5, r.getAverageOrderValue(), moneyStyle);
            }

            // Summary row
            Row summaryRow = sheet.createRow(rowNum + 1);
            CellStyle summaryStyle = buildSummaryStyle(wb);
            Cell sumLabel = summaryRow.createCell(0);
            sumLabel.setCellValue("TOTAL");
            sumLabel.setCellStyle(summaryStyle);
            double grandTotal = rows.stream().mapToDouble(r -> r.getTotalRevenue() != null ? r.getTotalRevenue() : 0).sum();
            Cell sumValue = summaryRow.createCell(4);
            sumValue.setCellValue(grandTotal);
            sumValue.setCellStyle(moneyStyle);

            autoSizeColumns(sheet, headers.length);
            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Excel generation failed", e);
        }
    }

    // ── Style helpers ─────────────────────────────────────────────────────────

    private CellStyle buildTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle buildHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle buildDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle buildMoneyStyle(Workbook wb) {
        CellStyle style = buildDataStyle(wb);
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle buildSummaryStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void setCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else if (value != null) {
            cell.setCellValue(value.toString());
        }
        cell.setCellStyle(style);
    }

    private void autoSizeColumns(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
            // Add a small padding
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 512);
        }
    }

    private byte[] toBytes(XSSFWorkbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }
}
