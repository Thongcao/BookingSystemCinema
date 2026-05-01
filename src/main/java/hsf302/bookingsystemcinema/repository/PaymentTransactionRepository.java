package hsf302.bookingsystemcinema.repository;

import hsf302.bookingsystemcinema.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByBookingId(Long bookingId);
    Optional<PaymentTransaction> findByTransactionId(String transactionId);
}
