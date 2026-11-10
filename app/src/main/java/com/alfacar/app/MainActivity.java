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
                String migration = "(function(){try{" +
                    "var raw=localStorage.getItem('alfacar-v1');if(!raw)return;" +
                    "var db=JSON.parse(raw);if(!db||typeof db!=='object')return;" +
                    "db.quotes=Array.isArray(db.quotes)?db.quotes:[];" +
                    "if(db.quotes.some(function(q){return String(q.num)==='10126';}))return;" +
                    "var sample=[['Lanternagem e pintura da porta dianteira esquerda',800],['Montagem e pintura do retrovisor esquerdo',100],['Retoque do para-choque dianteiro esquerdo',250],['Retoque do para-choque traseiro esquerdo',350],['Lanternagem e pintura do para-lama esquerdo',550],['Pintura da porta traseira esquerda',450],['Pintura da lateral traseira esquerda',450],['Pintura da roda dianteira esquerda',150]];" +
                    "var quote={id:Math.max(0,...db.quotes.map(function(q){return Number(q.id)||0;}))+1,num:'10126',date:'2026-11-09',status:'Em elaboração',client:'Adil',model:'Toyota Yaris Sedan',year:'2022/2023',color:'Prata',plate:'RUK7D98',chassis:'9BRBC5F38P8206701',items:sample.map(function(x){return {name:x[0],price:x[1]};}),payment:'Pagamento dividido em 50% de entrada, no momento da entrada do veículo na oficina, e 50% restantes na entrega do veículo, após a conclusão dos serviços.',deadline:'Prazo estimado: de 7 a 10 dias, sujeito ao andamento dos serviços e a eventuais necessidades identificadas durante a execução.',paid:0,order:false};" +
                    "db.clients=Array.isArray(db.clients)?db.clients:[];if(!db.clients.some(function(c){return String(c.name||'').toLowerCase()==='adil';}))db.clients.push({id:Math.max(0,...db.clients.map(function(c){return Number(c.id)||0;}))+1,name:'Adil',phone:'',doc:''});" +
                    "db.cars=Array.isArray(db.cars)?db.cars:[];if(!db.cars.some(function(c){return String(c.plate||'').toUpperCase()==='RUK7D98';}))db.cars.push({id:Math.max(0,...db.cars.map(function(c){return Number(c.id)||0;}))+1,client:'Adil',model:'Toyota Yaris Sedan',year:'2022/2023',color:'Prata',plate:'RUK7D98',chassis:'9BRBC5F38P8206701'});" +
                    "db.quotes.push(quote);db.next=Math.max(Number(db.next)||1,10127);" +
                    "localStorage.setItem('alfacar-v1',JSON.stringify(db));location.reload();" +
                    "}catch(e){console.error('Falha na recuperação do orçamento Adil',e);}})();";
                view.evaluateJavascript(migration, null);
                view.evaluateJavascript("(function(){var s=document.createElement('script');s.src='file:///android_asset/www/cloud-sync.js';document.head.appendChild(s);})();", null);
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
