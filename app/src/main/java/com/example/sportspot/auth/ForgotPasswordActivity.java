package com.example.sportspot.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sportspot.R;
import com.example.sportspot.auth.api.ApiClient;
import com.example.sportspot.auth.models.ApiResponse;
import com.example.sportspot.auth.models.ForgotPasswordRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnSubmit;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.etEmail);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);

        TextView tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnSubmit.setOnClickListener(v -> attemptForgotPassword());
        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void attemptForgotPassword() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }

        showLoading(true);

        ForgotPasswordRequest req = new ForgotPasswordRequest(email);
        ApiClient.getAuthApiService().forgotPassword(req).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                showLoading(false);
                String msg = "If an account exists with this email, password recovery instructions will be sent.";
                if (response.body() != null && response.body().getMessage() != null) {
                    msg = response.body().getMessage();
                }
                Toast.makeText(ForgotPasswordActivity.this, msg, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(ForgotPasswordActivity.this, "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!loading);
    }
}
