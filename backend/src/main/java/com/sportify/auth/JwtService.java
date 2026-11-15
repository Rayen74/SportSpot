package com.sportify.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.sportify.config.AppConfig;
import com.sportify.users.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

public class JwtService {
    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final int expirationMinutes;

    public JwtService(AppConfig appConfig) {
        this.algorithm = Algorithm.HMAC256(appConfig.getJwtSecret());
        this.verifier = JWT.require(algorithm).build();
        this.expirationMinutes = appConfig.getJwtExpirationMinutes();
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        return JWT.create()
                .withSubject(String.valueOf(user.getId()))
                .withClaim("email", user.getEmail())
                .withClaim("role", user.getRole())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiresAt))
                .sign(algorithm);
    }

    public Optional<DecodedJWT> verifyToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            DecodedJWT jwt = verifier.verify(token);
            return Optional.of(jwt);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Long extractUserId(DecodedJWT jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
