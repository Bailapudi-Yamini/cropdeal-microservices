package com.cropdeal.paymentservice.service;

import com.cropdeal.paymentservice.dto.response.PagedResponse;
import com.cropdeal.paymentservice.dto.response.ReceiptResponse;
import com.cropdeal.paymentservice.entity.Payment;
import com.cropdeal.paymentservice.entity.Receipt;
import com.cropdeal.paymentservice.exception.ResourceNotFoundException;
import com.cropdeal.paymentservice.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final PaymentMapper paymentMapper;

    /**
     * Generates a receipt for a successful payment.
     * Receipt number format: RCP-{YYYY}-{6-digit-sequence}
     * e.g. RCP-2024-000001
     */
    @Transactional
    public Receipt generateReceipt(Payment payment, String cropDetails) {
        String receiptNumber = buildReceiptNumber();

        Receipt receipt = Receipt.builder()
                .paymentId(payment.getId())
                .farmerId(payment.getFarmerId())
                .dealerId(payment.getDealerId())
                .receiptNumber(receiptNumber)
                .amount(payment.getAmount())
                .cropDetails(cropDetails)
                .build();

        Receipt saved = receiptRepository.save(receipt);
        log.info("Receipt {} generated for paymentId={}", receiptNumber, payment.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptByPaymentId(Long paymentId) {
        return receiptRepository.findByPaymentId(paymentId)
                .map(paymentMapper::toReceiptResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Receipt not found for paymentId: " + paymentId));
    }

    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptByNumber(String receiptNumber) {
        return receiptRepository.findByReceiptNumber(receiptNumber)
                .map(paymentMapper::toReceiptResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Receipt not found: " + receiptNumber));
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReceiptResponse> getReceiptsForFarmer(Long farmerId, Pageable pageable) {
        return toPagedResponse(receiptRepository.findByFarmerId(farmerId, pageable)
                .map(paymentMapper::toReceiptResponse));
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReceiptResponse> getReceiptsForDealer(Long dealerId, Pageable pageable) {
        return toPagedResponse(receiptRepository.findByDealerId(dealerId, pageable)
                .map(paymentMapper::toReceiptResponse));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildReceiptNumber() {
        int year = LocalDateTime.now().getYear();
        long sequence = receiptRepository.countByYear(year) + 1;
        return String.format("RCP-%d-%06d", year, sequence);
    }

    private <T> PagedResponse<T> toPagedResponse(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
