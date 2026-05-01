package hsf302.bookingsystemcinema.service;

import hsf302.bookingsystemcinema.dto.PaymentWebhookRequest;

public interface PaymentWebhookService {

    String processWebhook(PaymentWebhookRequest request);
}
