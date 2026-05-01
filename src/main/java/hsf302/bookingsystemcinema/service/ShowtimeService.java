package hsf302.bookingsystemcinema.service;

import hsf302.bookingsystemcinema.dto.ShowtimeRequest;
import hsf302.bookingsystemcinema.dto.ShowtimeResponse;
import java.util.List;

public interface ShowtimeService {
    List<ShowtimeResponse> getAllShowtimes();
    List<ShowtimeResponse> getShowtimesByMovie(Long movieId);
    ShowtimeResponse getShowtimeById(Long id);
    ShowtimeResponse createShowtime(ShowtimeRequest request);
    ShowtimeResponse updateShowtime(Long id, ShowtimeRequest request);
    void deleteShowtime(Long id);
}
