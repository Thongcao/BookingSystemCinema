package hsf302.bookingsystemcinema.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // ── Public: Auth endpoints ──
                .requestMatchers("/api/auth/**").permitAll()

                // ── Public: Read-only APIs (movies, seat-map, hold/release seats) ──
                .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/public/showtimes/*/hold-seats").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/public/showtimes/*/release-seats").permitAll()

                // ── Authenticated: Booking checkout ──
                .requestMatchers(HttpMethod.POST, "/api/public/bookings").authenticated()

                // ── Admin: All admin APIs ──
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // ── Thymeleaf views: Open to all ──
                .requestMatchers("/", "/movie/**", "/booking/**", "/login", "/register").permitAll()

                // ── Static resources ──
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()

                // ── Everything else requires authentication ──
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
