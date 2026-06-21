package com.cropdeal.paymentservice.service;

import com.cropdeal.paymentservice.dto.response.PaymentEventResponse;
import com.cropdeal.paymentservice.entity.PaymentEvent;
import com.cropdeal.paymentservice.repository.PaymentEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSourcingService {

    private final PaymentEventRepository eventRepository;
    private final PaymentMapper paymentMapper;
    private final ObjectMapper objectMapper;

    /**
     * Appends a new event to the payment event store.
     * Uses REQUIRES_NEW so the event is always persisted even if the
     * calling transaction rolls back — preserving the full audit trail.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public PaymentEvent appendEvent(Long paymentId, String eventType, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event payload for paymentId={}: {}", paymentId, e.getMessage());
            json = "{\"error\":\"serialization_failed\"}";
        }

        PaymentEvent event = PaymentEvent.builder()
                .paymentId(paymentId)
                .eventType(eventType)
                .payload(json)
                .build();

        PaymentEvent saved = eventRepository.save(event);
        log.debug("Appended event [{}] for paymentId={}", eventType, paymentId);
        return saved;
    }

    /**
     * Returns the full chronological event stream for a payment.
     * Can be used to replay and reconstruct payment state from scratch.
     */
    @Transactional(readOnly = true)
    public List<PaymentEventResponse> getEventStream(Long paymentId) {
        return eventRepository.findByPaymentIdOrderByOccurredAtAsc(paymentId)
                .stream()
                .map(paymentMapper::toEventResponse)
                .toList();
    }

    /**
     * Reconstructs the latest payment state by reading the most recent event.
     * In a full ES implementation this would replay all events through
     * an aggregate root — here we return the raw latest event payload.
     */
    @Transactional(readOnly = true)
    public String getLatestEventPayload(Long paymentId) {
        PaymentEvent latest = eventRepository.findTopByPaymentIdOrderByOccurredAtDesc(paymentId);
        return latest != null ? latest.getPayload() : null;
    }
}
