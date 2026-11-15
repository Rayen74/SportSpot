package com.example.sportspot.auth.models;

public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String role;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
}
