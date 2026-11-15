package com.example.sportspot.auth.models;

public class AuthResponse {
    private boolean success;
    private String message;
    private String token;
    private UserResponse user;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getToken() { return token; }
    public UserResponse getUser() { return user; }
}
