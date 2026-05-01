package hsf302.bookingsystemcinema.service.payment;

import hsf302.bookingsystemcinema.entity.Booking;

public interface PaymentStrategy {

    PaymentResult processPayment(Booking booking);

    String getMethodName();
}
