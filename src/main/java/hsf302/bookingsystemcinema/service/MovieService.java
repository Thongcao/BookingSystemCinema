package hsf302.bookingsystemcinema.service;

import hsf302.bookingsystemcinema.dto.MovieRequest;
import hsf302.bookingsystemcinema.dto.MovieResponse;
import java.util.List;

public interface MovieService {
    List<MovieResponse> getAllMovies();
    MovieResponse getMovieById(Long id);
    MovieResponse createMovie(MovieRequest request);
    MovieResponse updateMovie(Long id, MovieRequest request);
    void deleteMovie(Long id);
}
