package com.alfacar.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    private WebView webView;
    private LinearLayout loading;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showLoading();
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webView.setBackgroundColor(Color.WHITE);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (loading != null) setContentView(webView);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    String message = error == null ? "Erro ao abrir o aplicativo." : error.getDescription().toString();
                    showFailure(message);
                }
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage message) {
                Log.e("ALFACAR-Web", message.message() + " (linha " + message.lineNumber() + ")");
                return true;
            }
        });
        webView.loadUrl("file:///android_asset/www/index.html");
    }

    private void showLoading() {
        loading = new LinearLayout(this);
        loading.setOrientation(LinearLayout.VERTICAL);
        loading.setGravity(Gravity.CENTER);
        loading.setBackgroundColor(Color.WHITE);
        TextView title = new TextView(this);
        title.setText("ALFACAR");
        title.setTextColor(Color.rgb(23, 69, 117));
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        loading.addView(title);
        ProgressBar progress = new ProgressBar(this);
        loading.addView(progress);
        status = new TextView(this);
        status.setText("Carregando sistema...");
        status.setTextColor(Color.DKGRAY);
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);
        loading.addView(status);
        setContentView(loading);
    }

    private void showFailure(String message) {
        TextView error = new TextView(this);
        error.setText("ALFACAR\n\nNão foi possível abrir o sistema.\n" + message + "\n\nFeche e abra o aplicativo novamente.");
        error.setTextColor(Color.rgb(23, 69, 117));
        error.setTextSize(17);
        error.setGravity(Gravity.CENTER);
        error.setPadding(32, 32, 32, 32);
        error.setBackgroundColor(Color.WHITE);
        setContentView(error);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
