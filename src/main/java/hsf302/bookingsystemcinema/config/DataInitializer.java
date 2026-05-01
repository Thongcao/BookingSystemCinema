package hsf302.bookingsystemcinema.config;

import hsf302.bookingsystemcinema.entity.*;
import hsf302.bookingsystemcinema.entity.enums.*;
import hsf302.bookingsystemcinema.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CinemaRepository cinemaRepository;
    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;
    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ItemRepository itemRepository;
    private final VoucherRepository voucherRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // ── Seed Users (always ensure they exist, idempotent) ──
        seedUsers();

        if (cinemaRepository.count() > 0) {
            log.info(">>> Database already seeded. Skipping initialization.");
            return;
        }

        log.info(">>> Seeding database with sample data...");

        // ── 2. Cinema ──
        Cinema cinema = cinemaRepository.save(Cinema.builder()
                .name("Galaxy Cinema Nguyen Du")
                .address("116 Nguyen Du, District 1, Ho Chi Minh City")
                .build());

        log.info("   ✔ Created cinema: {}", cinema.getName());

        // ── 3. Room ──
        Room room = roomRepository.save(Room.builder()
                .cinema(cinema)
                .name("Room 1")
                .totalSeats(50)
                .build());

        log.info("   ✔ Created room: {}", room.getName());

        // ── 4. Seats (5 rows x 10 columns = 50 seats) ──
        List<Seat> seats = new ArrayList<>();
        String[] rows = {"A", "B", "C", "D", "E"};
        for (int r = 0; r < rows.length; r++) {
            for (int c = 1; c <= 10; c++) {
                SeatType type;
                if (r >= 3) {
                    type = SeatType.VIP;
                } else {
                    type = SeatType.NORMAL;
                }

                seats.add(Seat.builder()
                        .room(room)
                        .seatRow(rows[r])
                        .seatNumber(c)
                        .gridRow(r)
                        .gridColumn(c - 1)
                        .type(type)
                        .status(SeatStatus.AVAILABLE)
                        .build());
            }
        }
        seatRepository.saveAll(seats);
        log.info("   ✔ Created {} seats (rows A-E, 10 per row)", seats.size());

        // ── 5. Movies ──
        Movie movie1 = movieRepository.save(Movie.builder()
                .title("Avengers: Doomsday")
                .description("The Avengers face their ultimate threat when Doctor Doom reshapes reality itself.")
                .duration(150)
                .posterUrl("https://placehold.co/300x450?text=Avengers")
                .build());

        Movie movie2 = movieRepository.save(Movie.builder()
                .title("Lật Mặt 8")
                .description("Phần tiếp theo của loạt phim hành động Việt Nam đình đám.")
                .duration(130)
                .posterUrl("https://placehold.co/300x450?text=LatMat8")
                .build());

        log.info("   ✔ Created {} movies", 2);

        // ── 6. Showtimes ──
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(19).withMinute(0).withSecond(0).withNano(0);

        showtimeRepository.save(Showtime.builder()
                .movie(movie1)
                .room(room)
                .startTime(tomorrow)
                .endTime(tomorrow.plusMinutes(movie1.getDuration()))
                .basePrice(new BigDecimal("90000"))
                .build());

        showtimeRepository.save(Showtime.builder()
                .movie(movie2)
                .room(room)
                .startTime(tomorrow.plusHours(3))
                .endTime(tomorrow.plusHours(3).plusMinutes(movie2.getDuration()))
                .basePrice(new BigDecimal("75000"))
                .build());

        log.info("   ✔ Created {} showtimes", 2);

        // ── 7. F&B Items ──
        itemRepository.save(Item.builder().name("Popcorn (L)").price(new BigDecimal("55000")).type(ItemType.FOOD).build());
        itemRepository.save(Item.builder().name("Popcorn (M)").price(new BigDecimal("39000")).type(ItemType.FOOD).build());
        itemRepository.save(Item.builder().name("Coca-Cola (L)").price(new BigDecimal("32000")).type(ItemType.BEVERAGE).build());
        itemRepository.save(Item.builder().name("Pepsi (M)").price(new BigDecimal("25000")).type(ItemType.BEVERAGE).build());
        itemRepository.save(Item.builder().name("Combo Couple").price(new BigDecimal("109000")).type(ItemType.COMBO).build());

        log.info("   ✔ Created {} F&B items", 5);

        // ── 8. Vouchers ──
        voucherRepository.save(Voucher.builder()
                .code("WELCOME10")
                .discountPercent(new BigDecimal("10.00"))
                .maxDiscount(new BigDecimal("30000"))
                .validUntil(LocalDateTime.now().plusMonths(3))
                .usageLimit(100)
                .build());

        voucherRepository.save(Voucher.builder()
                .code("SUMMER25")
                .discountPercent(new BigDecimal("25.00"))
                .maxDiscount(new BigDecimal("50000"))
                .validUntil(LocalDateTime.now().plusMonths(1))
                .usageLimit(50)
                .build());

        log.info("   ✔ Created {} vouchers", 2);
        log.info(">>> Database seeding COMPLETE.");
    }

    private void seedUsers() {
        // Admin account: admin / 123
        userRepository.findByUsername("admin").ifPresentOrElse(
                existingUser -> {
                    existingUser.setPassword(passwordEncoder.encode("123"));
                    existingUser.setRole(Role.ADMIN);
                    userRepository.save(existingUser);
                    log.info("   ✔ Updated ADMIN password (BCrypt): admin");
                },
                () -> {
                    userRepository.save(User.builder()
                            .username("admin")
                            .password(passwordEncoder.encode("123"))
                            .email("admin@cinema.vn")
                            .role(Role.ADMIN)
                            .build());
                    log.info("   ✔ Created ADMIN user: admin");
                }
        );

        // Demo account: demo / 123
        userRepository.findByUsername("demo").ifPresentOrElse(
                existingUser -> {
                    existingUser.setPassword(passwordEncoder.encode("123"));
                    existingUser.setRole(Role.CUSTOMER);
                    userRepository.save(existingUser);
                    log.info("   ✔ Updated CUSTOMER password (BCrypt): demo");
                },
                () -> {
                    userRepository.save(User.builder()
                            .username("demo")
                            .password(passwordEncoder.encode("123"))
                            .email("demo@gmail.com")
                            .role(Role.CUSTOMER)
                            .build());
                    log.info("   ✔ Created CUSTOMER user: demo");
                }
        );
    }
}
