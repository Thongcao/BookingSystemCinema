package hsf302.bookingsystemcinema.controller.api;

import hsf302.bookingsystemcinema.dto.ApiResponse;
import hsf302.bookingsystemcinema.dto.PaymentWebhookRequest;
import hsf302.bookingsystemcinema.service.PaymentWebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/payment")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    /**
     * POST /api/public/payment/webhook
     * Simulates payment gateway callback.
     * Handles 4 scenarios: PAID, NEEDS_REFUND, DUPLICATE_IGNORED, FAILED.
     */
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<String>> handleWebhook(
            @Valid @RequestBody PaymentWebhookRequest request) {
        String result = paymentWebhookService.processWebhook(request);

        String message = switch (result) {
            case "PAID" -> "Payment confirmed. Booking is now PAID.";
            case "NEEDS_REFUND" -> "Late payment detected. Booking was already cancelled. NEEDS_REFUND created.";
            case "DUPLICATE_IGNORED" -> "Duplicate webhook. Booking is already PAID.";
            case "FAILED" -> "Payment failed. Booking cancelled.";
            default -> "Webhook processed with result: " + result;
        };

        return ResponseEntity.ok(ApiResponse.success(message, result));
    }
}
