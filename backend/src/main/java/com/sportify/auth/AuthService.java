package com.sportify.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.sportify.auth.dto.AuthResponse;
import com.sportify.auth.dto.LoginRequest;
import com.sportify.auth.dto.RegisterRequest;
import com.sportify.auth.dto.UserResponse;
import com.sportify.users.User;
import com.sportify.users.UserRepository;

import java.util.Optional;
import java.util.regex.Pattern;

public class AuthService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest req) {
        if (req == null) {
            return new AuthResponse(false, "Invalid request payload.", null, null);
        }

        if (isEmpty(req.getName())) {
            return new AuthResponse(false, "Name is required.", null, null);
        }

        if (isEmpty(req.getEmail()) || !EMAIL_PATTERN.matcher(req.getEmail().trim()).matches()) {
            return new AuthResponse(false, "Please provide a valid email address.", null, null);
        }

        if (isEmpty(req.getPassword()) || req.getPassword().length() < 8) {
            return new AuthResponse(false, "Password must be at least 8 characters long.", null, null);
        }

        String normalizedEmail = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            return new AuthResponse(false, "An account with this email address already exists.", null, null);
        }

        String passwordHash = PasswordHasher.hashPassword(req.getPassword());

        User user = new User();
        user.setName(req.getName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordHash);
        user.setPhone(req.getPhone() != null ? req.getPhone().trim() : null);
        user.setRole("USER"); // Backend enforces default role

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser);

        return new AuthResponse(true, "Account created successfully", token, new UserResponse(savedUser));
    }

    public AuthResponse login(LoginRequest req) {
        if (req == null || isEmpty(req.getEmail()) || isEmpty(req.getPassword())) {
            return new AuthResponse(false, "Email and password are required.", null, null);
        }

        String normalizedEmail = req.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);

        if (userOpt.isEmpty()) {
            // Generic error message to prevent account enumeration
            return new AuthResponse(false, "Invalid email or password.", null, null);
        }

        User user = userOpt.get();
        if (!PasswordHasher.verifyPassword(req.getPassword(), user.getPasswordHash())) {
            return new AuthResponse(false, "Invalid email or password.", null, null);
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(true, "Login successful", token, new UserResponse(user));
    }

    public Optional<UserResponse> getCurrentUser(DecodedJWT jwt) {
        if (jwt == null) return Optional.empty();
        try {
            Long userId = jwtService.extractUserId(jwt);
            return userRepository.findById(userId).map(UserResponse::new);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
