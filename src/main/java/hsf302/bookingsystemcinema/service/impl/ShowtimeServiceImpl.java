package hsf302.bookingsystemcinema.service.impl;

import hsf302.bookingsystemcinema.dto.ShowtimeRequest;
import hsf302.bookingsystemcinema.dto.ShowtimeResponse;
import hsf302.bookingsystemcinema.entity.Movie;
import hsf302.bookingsystemcinema.entity.Room;
import hsf302.bookingsystemcinema.entity.Showtime;
import hsf302.bookingsystemcinema.exception.ResourceNotFoundException;
import hsf302.bookingsystemcinema.repository.MovieRepository;
import hsf302.bookingsystemcinema.repository.RoomRepository;
import hsf302.bookingsystemcinema.repository.ShowtimeRepository;
import hsf302.bookingsystemcinema.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ShowtimeServiceImpl implements ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getAllShowtimes() {
        return showtimeRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getShowtimesByMovie(Long movieId) {
        return showtimeRepository.findByMovieId(movieId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeResponse getShowtimeById(Long id) {
        return toResponse(findShowtimeOrThrow(id));
    }

    @Override
    public ShowtimeResponse createShowtime(ShowtimeRequest request) {
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie", "id", request.getMovieId()));
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", request.getRoomId()));

        Showtime showtime = Showtime.builder()
                .movie(movie)
                .room(room)
                .startTime(request.getStartTime())
                .endTime(request.getStartTime().plusMinutes(movie.getDuration()))
                .basePrice(request.getBasePrice())
                .build();

        return toResponse(showtimeRepository.save(showtime));
    }

    @Override
    public ShowtimeResponse updateShowtime(Long id, ShowtimeRequest request) {
        Showtime showtime = findShowtimeOrThrow(id);

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie", "id", request.getMovieId()));
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", request.getRoomId()));

        showtime.setMovie(movie);
        showtime.setRoom(room);
        showtime.setStartTime(request.getStartTime());
        showtime.setEndTime(request.getStartTime().plusMinutes(movie.getDuration()));
        showtime.setBasePrice(request.getBasePrice());

        return toResponse(showtimeRepository.save(showtime));
    }

    @Override
    public void deleteShowtime(Long id) {
        Showtime showtime = findShowtimeOrThrow(id);
        showtimeRepository.delete(showtime);
    }

    private Showtime findShowtimeOrThrow(Long id) {
        return showtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime", "id", id));
    }

    private ShowtimeResponse toResponse(Showtime showtime) {
        return ShowtimeResponse.builder()
                .id(showtime.getId())
                .movieId(showtime.getMovie().getId())
                .movieTitle(showtime.getMovie().getTitle())
                .roomId(showtime.getRoom().getId())
                .roomName(showtime.getRoom().getName())
                .cinemaName(showtime.getRoom().getCinema().getName())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .basePrice(showtime.getBasePrice())
                .build();
    }
}
