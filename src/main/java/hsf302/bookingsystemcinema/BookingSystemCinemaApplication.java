package hsf302.bookingsystemcinema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BookingSystemCinemaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingSystemCinemaApplication.class, args);
    }

}
