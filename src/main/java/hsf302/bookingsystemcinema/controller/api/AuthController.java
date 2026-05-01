package hsf302.bookingsystemcinema.controller.api;

import hsf302.bookingsystemcinema.dto.*;
import hsf302.bookingsystemcinema.entity.User;
import hsf302.bookingsystemcinema.entity.enums.Role;
import hsf302.bookingsystemcinema.repository.UserRepository;
import hsf302.bookingsystemcinema.security.CustomUserPrincipal;
import hsf302.bookingsystemcinema.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info(">>> [Auth] Login attempt for user: {}", request.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);

        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();

        AuthResponse authResponse = AuthResponse.builder()
                .token(jwt)
                .type("Bearer")
                .userId(principal.getUserId())
                .username(principal.getUsername())
                .role(principal.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""))
                .build();

        log.info(">>> [Auth] Login successful for user: {}", request.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info(">>> [Auth] Register attempt for user: {}", request.getUsername());

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Username '" + request.getUsername() + "' is already taken"));
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Email '" + request.getEmail() + "' is already registered"));
        }

        User newUser = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(Role.CUSTOMER)
                .build();
        userRepository.save(newUser);

        // Auto-login after registration
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        String jwt = jwtTokenProvider.generateToken(authentication);
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();

        AuthResponse authResponse = AuthResponse.builder()
                .token(jwt)
                .type("Bearer")
                .userId(principal.getUserId())
                .username(principal.getUsername())
                .role("CUSTOMER")
                .build();

        log.info(">>> [Auth] Registration successful for user: {}", request.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", authResponse));
    }
}
