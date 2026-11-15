package com.example.sportspot.auth;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private static final String PREF_NAME = "sportspot_auth_prefs";
    private static final String KEY_JWT_TOKEN = "jwt_token";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";

    private final SharedPreferences prefs;

    public TokenManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_JWT_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_JWT_TOKEN, null);
    }

    public boolean hasToken() {
        String token = getToken();
        return token != null && !token.trim().isEmpty();
    }

    public void saveUserInfo(String name, String email) {
        prefs.edit()
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_EMAIL, email)
                .apply();
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
