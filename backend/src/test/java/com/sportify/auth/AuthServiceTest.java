package com.sportify.auth;

import com.sportify.auth.dto.AuthResponse;
import com.sportify.auth.dto.LoginRequest;
import com.sportify.auth.dto.RegisterRequest;
import com.sportify.config.AppConfig;
import com.sportify.users.User;
import com.sportify.users.UserRepository;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AuthServiceTest {

    private AuthService authService;
    private InMemoryUserRepository userRepository;

    @Before
    public void setUp() {
        System.setProperty("JWT_SECRET", "test_secret_key_1234567890_super_secret");
        System.setProperty("JWT_EXPIRATION_MINUTES", "60");
        AppConfig appConfig = new AppConfig();
        JwtService jwtService = new JwtService(appConfig);
        userRepository = new InMemoryUserRepository();
        authService = new AuthService(userRepository, jwtService);
    }

    @Test
    public void testSuccessfulRegistration() {
        RegisterRequest req = new RegisterRequest("John Doe", "john@example.com", "StrongPassword123", "12345678");
        AuthResponse res = authService.register(req);

        assertTrue(res.isSuccess());
        assertEquals("Account created successfully", res.getMessage());
        assertNotNull(res.getToken());
        assertNotNull(res.getUser());
        assertEquals("john@example.com", res.getUser().getEmail());
        assertEquals("USER", res.getUser().getRole());
    }

    @Test
    public void testDuplicateEmailRegistrationFails() {
        RegisterRequest req1 = new RegisterRequest("John Doe", "john@example.com", "StrongPassword123", "12345678");
        authService.register(req1);

        RegisterRequest req2 = new RegisterRequest("John Clone", "JOHN@EXAMPLE.COM", "AnotherPassword123", "87654321");
        AuthResponse res2 = authService.register(req2);

        assertFalse(res2.isSuccess());
        assertTrue(res2.getMessage().contains("already exists"));
    }

    @Test
    public void testInvalidEmailRegistrationFails() {
        RegisterRequest req = new RegisterRequest("John Doe", "invalid-email", "StrongPassword123", "12345678");
        AuthResponse res = authService.register(req);

        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().contains("valid email"));
    }

    @Test
    public void testShortPasswordRegistrationFails() {
        RegisterRequest req = new RegisterRequest("John Doe", "john@example.com", "short", "12345678");
        AuthResponse res = authService.register(req);

        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().contains("at least 8 characters"));
    }

    @Test
    public void testSuccessfulLogin() {
        RegisterRequest regReq = new RegisterRequest("John Doe", "john@example.com", "StrongPassword123", "12345678");
        authService.register(regReq);

        LoginRequest loginReq = new LoginRequest("john@example.com", "StrongPassword123");
        AuthResponse loginRes = authService.login(loginReq);

        assertTrue(loginRes.isSuccess());
        assertEquals("Login successful", loginRes.getMessage());
        assertNotNull(loginRes.getToken());
        assertEquals("john@example.com", loginRes.getUser().getEmail());
    }

    @Test
    public void testIncorrectPasswordLoginFails() {
        RegisterRequest regReq = new RegisterRequest("John Doe", "john@example.com", "StrongPassword123", "12345678");
        authService.register(regReq);

        LoginRequest loginReq = new LoginRequest("john@example.com", "WrongPassword123");
        AuthResponse loginRes = authService.login(loginReq);

        assertFalse(loginRes.isSuccess());
        assertEquals("Invalid email or password.", loginRes.getMessage());
    }

    @Test
    public void testUnknownEmailLoginFails() {
        LoginRequest loginReq = new LoginRequest("unknown@example.com", "StrongPassword123");
        AuthResponse loginRes = authService.login(loginReq);

        assertFalse(loginRes.isSuccess());
        assertEquals("Invalid email or password.", loginRes.getMessage());
    }

    private static class InMemoryUserRepository implements UserRepository {
        private final Map<Long, User> usersById = new HashMap<>();
        private final Map<String, User> usersByEmail = new HashMap<>();
        private long idCounter = 1;

        @Override
        public User save(User user) {
            user.setId(idCounter++);
            usersById.put(user.getId(), user);
            usersByEmail.put(user.getEmail().toLowerCase().trim(), user);
            return user;
        }

        @Override
        public Optional<User> findById(Long id) {
            return Optional.ofNullable(usersById.get(id));
        }

        @Override
        public Optional<User> findByEmail(String email) {
            if (email == null) return Optional.empty();
            return Optional.ofNullable(usersByEmail.get(email.toLowerCase().trim()));
        }

        @Override
        public boolean updatePassword(Long userId, String newPasswordHash) {
            User u = usersById.get(userId);
            if (u != null) {
                u.setPasswordHash(newPasswordHash);
                return true;
            }
            return false;
        }

        @Override
        public boolean existsByEmail(String email) {
            if (email == null) return false;
            return usersByEmail.containsKey(email.toLowerCase().trim());
        }
    }
}
