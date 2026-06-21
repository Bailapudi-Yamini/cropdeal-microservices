package com.cropdeal.paymentservice.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RazorpayGateway {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    private RazorpayClient client;

    @PostConstruct
    public void init() throws RazorpayException {
        this.client = new RazorpayClient(keyId, keySecret);
    }

    /**
     * Creates a Razorpay order.
     * Amount must be in paise (multiply INR by 100).
     *
     * @return Razorpay order ID (e.g. "order_ABC123")
     */
    public String createOrder(Long internalPaymentId, Long orderId, Double amountInRupees) {
        try {
            JSONObject options = new JSONObject();
            options.put("amount", (long) (amountInRupees * 100)); // paise
            options.put("currency", "INR");
            options.put("receipt", "order_" + orderId);
            options.put("payment_capture", 1);

            JSONObject notes = new JSONObject();
            notes.put("internalPaymentId", internalPaymentId);
            notes.put("internalOrderId", orderId);
            options.put("notes", notes);

            Order razorpayOrder = client.orders.create(options);
            String razorpayOrderId = razorpayOrder.get("id");
            log.info("Razorpay order created: {} for internalOrderId={}", razorpayOrderId, orderId);
            return razorpayOrderId;
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for orderId={}: {}", orderId, e.getMessage());
            throw new RuntimeException("Razorpay order creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Verifies the HMAC-SHA256 signature from Razorpay webhook/checkout callback.
     * Signature = HMAC_SHA256(razorpayOrderId + "|" + razorpayPaymentId, keySecret)
     */
    public boolean verifySignature(String razorpayOrderId, String razorpayPaymentId,
                                   String razorpaySignature) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", razorpayOrderId);
            attributes.put("razorpay_payment_id", razorpayPaymentId);
            attributes.put("razorpay_signature", razorpaySignature);
            Utils.verifyPaymentSignature(attributes, keySecret);
            return true;
        } catch (RazorpayException e) {
            log.warn("Razorpay signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    public String getKeyId() {
        return keyId;
    }
}
