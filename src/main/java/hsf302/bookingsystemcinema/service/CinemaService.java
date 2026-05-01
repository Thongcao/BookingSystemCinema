package hsf302.bookingsystemcinema.service;

import hsf302.bookingsystemcinema.dto.CinemaRequest;
import hsf302.bookingsystemcinema.dto.CinemaResponse;
import java.util.List;

public interface CinemaService {
    List<CinemaResponse> getAllCinemas();
    CinemaResponse getCinemaById(Long id);
    CinemaResponse createCinema(CinemaRequest request);
    CinemaResponse updateCinema(Long id, CinemaRequest request);
    void deleteCinema(Long id);
}
