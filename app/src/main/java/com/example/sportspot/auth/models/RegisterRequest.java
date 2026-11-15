package com.example.sportspot.auth.models;

public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private String phone;

    public RegisterRequest(String name, String email, String password, String phone) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getPhone() { return phone; }
}
