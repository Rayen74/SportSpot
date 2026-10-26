package com.example.sportspot;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SportSpotHero";
    private static final String ALLOWED_DOMAIN = "assets.sportspot.local";
    private static final String TARGET_SCHEME = "sportspot";
    private static final String ACTION_FIND_FIELDS = "find-fields";

    private WebView webView;
    private WebViewAssetLoader assetLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        applyEdgeToEdge();
        initAssetLoader();
        initWebView();
        setupBackNavigation();
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView != null) {
                    webView.evaluateJavascript(
                        "(function() { " +
                        "  var btnBack = document.getElementById('btn-back'); " +
                        "  var viewJourney = document.getElementById('view-journey'); " +
                        "  if (viewJourney && viewJourney.classList.contains('active')) { " +
                        "    if (btnBack) { btnBack.click(); return true; } " +
                        "  } " +
                        "  return false; " +
                        "})();",
                        value -> {
                            if (!"true".equals(value)) {
                                setEnabled(false);
                                getOnBackPressedDispatcher().onBackPressed();
                                setEnabled(true);
                            }
                        }
                    );
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });
    }

    private void applyEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                // Ensure dark status bar with white text
                controller.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
    }

    private void initAssetLoader() {
        // Load local assets via a secure, origin-safe virtual domain
        assetLoader = new WebViewAssetLoader.Builder()
                .setDomain(ALLOWED_DOMAIN)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        webView = findViewById(R.id.heroWebView);
        webView.setBackgroundColor(Color.parseColor("#050507"));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        // Security: Avoid unrestricted addJavascriptInterface to prevent arbitrary native execution
        webView.setWebViewClient(new SecureHeroClient());

        // Load entry point via secure virtual HTTPS origin
        webView.loadUrl("https://" + ALLOWED_DOMAIN + "/assets/sportspot/index.html");
    }

    private class SecureHeroClient extends WebViewClientCompat {
        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            // Serve assets locally through the WebViewAssetLoader
            return assetLoader.shouldInterceptRequest(request.getUrl());
        }

        @Override
        public boolean shouldOverrideUrlLoading(@NonNull WebView view, @NonNull WebResourceRequest request) {
            Uri uri = request.getUrl();

            // Intercept only custom SportSpot action schemes
            if (TARGET_SCHEME.equalsIgnoreCase(uri.getScheme())) {
                String host = uri.getHost();
                if (ACTION_FIND_FIELDS.equalsIgnoreCase(host)) {
                    navigateToNativeFieldDiscovery();
                    return true;
                } else {
                    Log.w(TAG, "Unrecognized SportSpot action: " + host);
                    return true;
                }
            }

            // Strictly reject unexpected external navigations inside this trusted hero
            if (request.isForMainFrame() && !ALLOWED_DOMAIN.equalsIgnoreCase(uri.getHost())) {
                Log.w(TAG, "Blocked unauthorized navigation attempt to: " + uri);
                return true;
            }

            return false;
        }
    }

    /**
     * Handover point into the existing native SportSpot booking screen.
     * Replace this placeholder with your native Activity, Navigation Component or Fragment call.
     */
    private void navigateToNativeFieldDiscovery() {
        Log.i(TAG, "Navigating to native Booking/Field Discovery Activity...");

        // Example integration:
        // Intent intent = new Intent(this, FieldDiscoveryActivity.class);
        // startActivity(intent);

        Toast.makeText(this, "Navigating to Native Field Discovery...", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}