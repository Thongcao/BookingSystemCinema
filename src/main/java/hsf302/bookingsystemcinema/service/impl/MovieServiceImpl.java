package hsf302.bookingsystemcinema.service.impl;

import hsf302.bookingsystemcinema.dto.MovieRequest;
import hsf302.bookingsystemcinema.dto.MovieResponse;
import hsf302.bookingsystemcinema.entity.Movie;
import hsf302.bookingsystemcinema.exception.ResourceNotFoundException;
import hsf302.bookingsystemcinema.repository.MovieRepository;
import hsf302.bookingsystemcinema.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MovieResponse> getAllMovies() {
        return movieRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MovieResponse getMovieById(Long id) {
        return toResponse(findMovieOrThrow(id));
    }

    @Override
    public MovieResponse createMovie(MovieRequest request) {
        Movie movie = Movie.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .duration(request.getDuration())
                .posterUrl(request.getPosterUrl())
                .build();
        return toResponse(movieRepository.save(movie));
    }

    @Override
    public MovieResponse updateMovie(Long id, MovieRequest request) {
        Movie movie = findMovieOrThrow(id);
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setDuration(request.getDuration());
        movie.setPosterUrl(request.getPosterUrl());
        return toResponse(movieRepository.save(movie));
    }

    @Override
    public void deleteMovie(Long id) {
        Movie movie = findMovieOrThrow(id);
        movieRepository.delete(movie);
    }

    private Movie findMovieOrThrow(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", "id", id));
    }

    private MovieResponse toResponse(Movie movie) {
        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .duration(movie.getDuration())
                .posterUrl(movie.getPosterUrl())
                .build();
    }
}
