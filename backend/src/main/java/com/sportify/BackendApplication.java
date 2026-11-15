package com.sportify;

import com.sun.net.httpserver.HttpServer;
import com.sportify.auth.AuthController;
import com.sportify.auth.AuthService;
import com.sportify.auth.JwtService;
import com.sportify.auth.PasswordResetService;
import com.sportify.config.AppConfig;
import com.sportify.config.DatabaseConfig;
import com.sportify.email.ConsoleEmailService;
import com.sportify.email.EmailService;
import com.sportify.email.SmtpEmailService;
import com.sportify.users.UserRepository;
import com.sportify.users.UserRepositoryJdbc;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class BackendApplication {
    private static final Logger logger = Logger.getLogger(BackendApplication.class.getName());

    public static void main(String[] args) {
        try {
            logger.info("Starting Sportify Authentication Backend Application...");

            AppConfig appConfig = new AppConfig();
            DatabaseConfig dbConfig = new DatabaseConfig(appConfig);
            dbConfig.runMigrations();

            UserRepository userRepository = new UserRepositoryJdbc(dbConfig);
            JwtService jwtService = new JwtService(appConfig);

            EmailService emailService = appConfig.getSmtpHost() != null && !appConfig.getSmtpHost().trim().isEmpty()
                    ? new SmtpEmailService(appConfig)
                    : new ConsoleEmailService();

            PasswordResetService passwordResetService = new PasswordResetService(userRepository, dbConfig, emailService);
            AuthService authService = new AuthService(userRepository, jwtService);

            AuthController authController = new AuthController(authService, passwordResetService, jwtService);

            int port = 8080;
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/auth", authController);
            server.setExecutor(Executors.newFixedThreadPool(10));

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down Sportify Backend Server...");
                server.stop(0);
                dbConfig.close();
            }));

            server.start();
            logger.info("Sportify Backend Server is running and listening on port " + port);

        } catch (Exception e) {
            logger.severe("Failed to start Sportify Backend Application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
