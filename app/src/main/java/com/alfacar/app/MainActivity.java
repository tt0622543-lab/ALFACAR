package com.alfacar.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Color;
import android.util.Log;
import android.util.Base64;
import android.view.Gravity;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private WebView webView;
    private LinearLayout loading;
    private TextView status;
    private static final int CREATE_PDF_REQUEST = 9101;
    private byte[] pendingPdfBytes;
    private String pendingPdfName;

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
        webView.addJavascriptInterface(new AlfacarAndroidBridge(), "ALFACARAndroid");
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
                String nativePdf = "(function(){if(window.__alfacarNativePdfReady)return;window.__alfacarNativePdfReady=true;async function makePdf(mode){try{var q=db.quotes.find(function(x){return x.id===cur;});if(!q){alert('Abra um orçamento primeiro.');return;}var el=document.getElementById('paper');if(!el||!el.innerText.trim()){alert('Não encontrei o documento para gerar o PDF.');return;}if(typeof html2pdf==='undefined'){alert('O gerador de PDF não carregou. Confira a internet e tente novamente.');return;}var filename='ALFACAR_'+(q.order?'OS':'Orcamento')+'_'+String(q.num).replace(/[^a-z0-9_-]/gi,'_')+'.pdf';var caption='Olá, '+(q.client||'tudo bem')+'! Segue em anexo o '+(q.order?'Ordem de Serviço':'orçamento')+' ALFACAR nº '+q.num+'. Valor total: '+fmt(sum(q))+'.';var worker=html2pdf().set({margin:[10,10,10,10],filename:filename,image:{type:'jpeg',quality:0.98},html2canvas:{scale:2,useCORS:true,backgroundColor:'#ffffff'},jsPDF:{unit:'mm',format:'a4',orientation:'portrait'}}).from(el).toPdf();var dataUri=await worker.output('datauristring');var base64=dataUri.split(',')[1];if(mode==='save')ALFACARAndroid.savePdf(base64,filename);else ALFACARAndroid.sharePdf(base64,filename,caption);}catch(e){console.error('ALFACAR PDF',e);alert('Não foi possível gerar o PDF: '+(e.message||e));}}window.shareWhatsAppPDF=function(){makePdf('share');};window.shareEmail=function(){makePdf('share');};window.print=function(){makePdf('save');};})();";
                view.evaluateJavascript(nativePdf, null);
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


    private class AlfacarAndroidBridge {
        @JavascriptInterface public void savePdf(String base64, String filename) {
            try {
                pendingPdfBytes = Base64.decode(base64, Base64.DEFAULT);
                pendingPdfName = safePdfName(filename);
                runOnUiThread(() -> {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/pdf");
                    intent.putExtra(Intent.EXTRA_TITLE, pendingPdfName);
                    startActivityForResult(intent, CREATE_PDF_REQUEST);
                });
            } catch (Exception e) {
                Log.e("ALFACAR-PDF", "Falha ao preparar PDF para salvar", e);
                runOnUiThread(() -> android.widget.Toast.makeText(MainActivity.this, "Não foi possível preparar o PDF.", android.widget.Toast.LENGTH_LONG).show());
            }
        }
        @JavascriptInterface public void sharePdf(String base64, String filename, String caption) {
            try {
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                String safeName = safePdfName(filename);
                runOnUiThread(() -> {
                    try {
                        File dir = new File(getCacheDir(), "shared");
                        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Não foi possível criar a pasta temporária.");
                        File file = new File(dir, safeName);
                        try (FileOutputStream out = new FileOutputStream(file)) { out.write(bytes); }
                        Uri uri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".fileprovider", file);
                        Intent send = new Intent(Intent.ACTION_SEND);
                        send.setType("application/pdf");
                        send.putExtra(Intent.EXTRA_STREAM, uri);
                        if (caption != null && !caption.isEmpty()) send.putExtra(Intent.EXTRA_TEXT, caption);
                        send.setClipData(ClipData.newUri(getContentResolver(), "PDF ALFACAR", uri));
                        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(send, "Compartilhar PDF ALFACAR"));
                    } catch (Exception e) {
                        Log.e("ALFACAR-PDF", "Falha ao compartilhar PDF", e);
                        android.widget.Toast.makeText(MainActivity.this, "Não foi possível compartilhar o PDF: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e("ALFACAR-PDF", "PDF inválido", e);
                runOnUiThread(() -> android.widget.Toast.makeText(MainActivity.this, "Arquivo PDF inválido.", android.widget.Toast.LENGTH_LONG).show());
            }
        }
    }

    private String safePdfName(String filename) {
        String name = filename == null ? "ALFACAR_documento.pdf" : filename.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!name.toLowerCase().endsWith(".pdf")) name += ".pdf";
        return name;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_PDF_REQUEST) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingPdfBytes != null) {
                try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                    if (out == null) throw new IOException("Não foi possível abrir o destino escolhido.");
                    out.write(pendingPdfBytes);
                    android.widget.Toast.makeText(this, "PDF ALFACAR salvo com sucesso.", android.widget.Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Log.e("ALFACAR-PDF", "Falha ao salvar PDF", e);
                    android.widget.Toast.makeText(this, "Erro ao salvar PDF: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                }
            }
            pendingPdfBytes = null;
            pendingPdfName = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
