package hsf302.bookingsystemcinema.service.impl;

import hsf302.bookingsystemcinema.dto.CinemaRequest;
import hsf302.bookingsystemcinema.dto.CinemaResponse;
import hsf302.bookingsystemcinema.entity.Cinema;
import hsf302.bookingsystemcinema.exception.ResourceNotFoundException;
import hsf302.bookingsystemcinema.repository.CinemaRepository;
import hsf302.bookingsystemcinema.service.CinemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CinemaServiceImpl implements CinemaService {

    private final CinemaRepository cinemaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CinemaResponse> getAllCinemas() {
        return cinemaRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CinemaResponse getCinemaById(Long id) {
        Cinema cinema = findCinemaOrThrow(id);
        return toResponse(cinema);
    }

    @Override
    public CinemaResponse createCinema(CinemaRequest request) {
        Cinema cinema = Cinema.builder()
                .name(request.getName())
                .address(request.getAddress())
                .build();
        return toResponse(cinemaRepository.save(cinema));
    }

    @Override
    public CinemaResponse updateCinema(Long id, CinemaRequest request) {
        Cinema cinema = findCinemaOrThrow(id);
        cinema.setName(request.getName());
        cinema.setAddress(request.getAddress());
        return toResponse(cinemaRepository.save(cinema));
    }

    @Override
    public void deleteCinema(Long id) {
        Cinema cinema = findCinemaOrThrow(id);
        cinemaRepository.delete(cinema);
    }

    private Cinema findCinemaOrThrow(Long id) {
        return cinemaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema", "id", id));
    }

    private CinemaResponse toResponse(Cinema cinema) {
        return CinemaResponse.builder()
                .id(cinema.getId())
                .name(cinema.getName())
                .address(cinema.getAddress())
                .roomCount(cinema.getRooms() != null ? cinema.getRooms().size() : 0)
                .build();
    }
}
