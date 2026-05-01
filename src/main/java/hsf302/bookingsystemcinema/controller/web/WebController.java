package hsf302.bookingsystemcinema.controller.web;

import hsf302.bookingsystemcinema.service.ItemService;
import hsf302.bookingsystemcinema.service.MovieService;
import hsf302.bookingsystemcinema.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final MovieService movieService;
    private final ShowtimeService showtimeService;
    private final ItemService itemService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("movies", movieService.getAllMovies());
        return "home";
    }

    @GetMapping("/movie/{id}")
    public String movieDetail(@PathVariable Long id, Model model) {
        model.addAttribute("movie", movieService.getMovieById(id));
        model.addAttribute("showtimes", showtimeService.getShowtimesByMovie(id));
        return "movie-detail";
    }

    @GetMapping("/booking/showtime/{id}")
    public String seatBooking(@PathVariable Long id, Model model) {
        model.addAttribute("showtime", showtimeService.getShowtimeById(id));
        model.addAttribute("items", itemService.getAllItems());
        return "seat-booking";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }
}
