package hsf302.bookingsystemcinema.service.payment;

import hsf302.bookingsystemcinema.entity.Booking;
import hsf302.bookingsystemcinema.entity.enums.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class MockPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentResult processPayment(Booking booking) {
        // Simulate payment gateway call
        String transactionId = "MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info(">>> [MockPayment] Processing payment for Booking #{} | Amount: {} | TxnId: {}",
                booking.getId(), booking.getTotalAmount(), transactionId);

        // Always return SUCCESS for mock
        return PaymentResult.builder()
                .status(PaymentStatus.SUCCESS)
                .transactionId(transactionId)
                .message("Mock payment processed successfully")
                .build();
    }

    @Override
    public String getMethodName() {
        return "MOCK";
    }
}
