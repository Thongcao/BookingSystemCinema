package hsf302.bookingsystemcinema.entity;

import hsf302.bookingsystemcinema.entity.enums.SeatStatus;
import hsf302.bookingsystemcinema.entity.enums.SeatType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Room room;

    @Column(name = "seat_row", nullable = false, length = 2)
    private String seatRow;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Column(name = "grid_row", nullable = false)
    private Integer gridRow;

    @Column(name = "grid_column", nullable = false)
    private Integer gridColumn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SeatType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private SeatStatus status;
}
