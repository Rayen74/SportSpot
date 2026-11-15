package com.example.sportspot.auth.api;

import com.example.sportspot.auth.models.ApiResponse;
import com.example.sportspot.auth.models.AuthResponse;
import com.example.sportspot.auth.models.ForgotPasswordRequest;
import com.example.sportspot.auth.models.LoginRequest;
import com.example.sportspot.auth.models.RegisterRequest;
import com.example.sportspot.auth.models.ResetPasswordRequest;
import com.example.sportspot.auth.models.UserResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface AuthApiService {

    @POST("auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @GET("auth/me")
    Call<UserResponse> getCurrentUser(@Header("Authorization") String authHeader);

    @POST("auth/forgot-password")
    Call<ApiResponse> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("auth/reset-password")
    Call<ApiResponse> resetPassword(@Body ResetPasswordRequest request);
}
