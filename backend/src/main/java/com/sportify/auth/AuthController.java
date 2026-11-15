package com.sportify.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sportify.auth.dto.ApiResponse;
import com.sportify.auth.dto.AuthResponse;
import com.sportify.auth.dto.ForgotPasswordRequest;
import com.sportify.auth.dto.LoginRequest;
import com.sportify.auth.dto.RegisterRequest;
import com.sportify.auth.dto.ResetPasswordRequest;
import com.sportify.auth.dto.UserResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuthController implements HttpHandler {
    private static final Logger logger = Logger.getLogger(AuthController.class.getName());

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public AuthController(AuthService authService, PasswordResetService passwordResetService, JwtService jwtService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.jwtService = jwtService;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if ("/auth/register".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleRegister(exchange);
            } else if ("/auth/login".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleLogin(exchange);
            } else if ("/auth/me".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleGetCurrentUser(exchange);
            } else if ("/auth/forgot-password".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleForgotPassword(exchange);
            } else if ("/auth/reset-password".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleResetPassword(exchange);
            } else {
                sendJsonResponse(exchange, 404, new ApiResponse(false, "Endpoint not found"));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Internal server error on endpoint: " + path, e);
            sendJsonResponse(exchange, 500, new ApiResponse(false, "An internal server error occurred."));
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        RegisterRequest req = objectMapper.readValue(exchange.getRequestBody(), RegisterRequest.class);
        AuthResponse res = authService.register(req);
        int statusCode = res.isSuccess() ? 201 : (res.getMessage().contains("already exists") ? 409 : 400);
        sendJsonResponse(exchange, statusCode, res);
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        LoginRequest req = objectMapper.readValue(exchange.getRequestBody(), LoginRequest.class);
        AuthResponse res = authService.login(req);
        int statusCode = res.isSuccess() ? 200 : 401;
        sendJsonResponse(exchange, statusCode, res);
    }

    private void handleGetCurrentUser(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendJsonResponse(exchange, 401, new ApiResponse(false, "Authorization header missing or invalid."));
            return;
        }

        String token = authHeader.substring(7);
        Optional<DecodedJWT> jwtOpt = jwtService.verifyToken(token);
        if (jwtOpt.isEmpty()) {
            sendJsonResponse(exchange, 401, new ApiResponse(false, "Invalid or expired JWT token."));
            return;
        }

        Optional<UserResponse> userOpt = authService.getCurrentUser(jwtOpt.get());
        if (userOpt.isEmpty()) {
            sendJsonResponse(exchange, 404, new ApiResponse(false, "Authenticated user account no longer exists."));
            return;
        }

        sendJsonResponse(exchange, 200, userOpt.get());
    }

    private void handleForgotPassword(HttpExchange exchange) throws IOException {
        ForgotPasswordRequest req = objectMapper.readValue(exchange.getRequestBody(), ForgotPasswordRequest.class);
        passwordResetService.requestPasswordReset(req.getEmail());
        // Always return generic response to prevent user enumeration
        sendJsonResponse(exchange, 200, new ApiResponse(true, "If an account exists with this email, password recovery instructions will be sent."));
    }

    private void handleResetPassword(HttpExchange exchange) throws IOException {
        ResetPasswordRequest req = objectMapper.readValue(exchange.getRequestBody(), ResetPasswordRequest.class);
        boolean success = passwordResetService.resetPassword(req.getToken(), req.getNewPassword());
        if (success) {
            sendJsonResponse(exchange, 200, new ApiResponse(true, "Password reset successfully. You can now log in with your new password."));
        } else {
            sendJsonResponse(exchange, 400, new ApiResponse(false, "Invalid, expired, or already used reset token."));
        }
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] bytes = objectMapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
