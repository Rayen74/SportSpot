package com.example.sportspot.auth;

import android.content.Intent;
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
import com.example.sportspot.auth.models.ResetPasswordRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etResetToken, etNewPassword, etConfirmPassword;
    private Button btnResetPassword;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        etResetToken = findViewById(R.id.etResetToken);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        progressBar = findViewById(R.id.progressBar);

        TextView tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnResetPassword.setOnClickListener(v -> attemptResetPassword());
        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void attemptResetPassword() {
        String token = etResetToken.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(token)) {
            etResetToken.setError("Reset Token is required");
            return;
        }

        if (TextUtils.isEmpty(newPassword) || newPassword.length() < 8) {
            etNewPassword.setError("Password must be at least 8 characters");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        showLoading(true);

        ResetPasswordRequest req = new ResetPasswordRequest(token, newPassword);
        ApiClient.getAuthApiService().resetPassword(req).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(ResetPasswordActivity.this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String msg = "Invalid or expired reset token.";
                    if (response.body() != null && response.body().getMessage() != null) {
                        msg = response.body().getMessage();
                    }
                    Toast.makeText(ResetPasswordActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(ResetPasswordActivity.this, "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnResetPassword.setEnabled(!loading);
    }
}
