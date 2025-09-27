package com.example.smarthotelapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.DefaultRetryPolicy;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Universal Network Manager - يدير الاتصال بشكل آمن ومضمون
 * يكتشف تلقائياً أفضل طريقة اتصال ويوفر نسخ احتياطية
 */
public class UniversalNetworkManager {
    private static final String TAG = "NetworkManager";
    private final Context context;
    private final RequestQueue requestQueue;
    private String bestUrl;
    private final List<String> testedUrls;
    
    public interface ConnectionCallback {
        void onConnectionFound(String url);
        void onConnectionFailed(String error);
        void onProgressUpdate(String message);
    }
    
    public UniversalNetworkManager(Context context) {
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context);
        this.testedUrls = new ArrayList<>();
        
        // URLs للاختبار التلقائي
        testedUrls.add("http://10.0.2.2/Smart-Hotel/"); // Emulator
        testedUrls.add("http://192.168.1.17/Smart-Hotel/"); // Local IP
        testedUrls.add("http://192.168.1.26/Smart-Hotel/"); // Alternative IP
        testedUrls.add("http://localhost/Smart-Hotel/"); // Localhost
    }
    
    /**
     * اكتشاف تلقائي لأفضل اتصال
     */
    public void discoverBestConnection(ConnectionCallback callback) {
        new Thread(() -> {
            try {
                // 1. اختبار Smart Network Discovery API
                testSmartDiscovery(callback);
                
                // 2. اختبار URLs المحلية
                testLocalUrls(callback);
                
                // 3. استخدام ngrok كحل نهائي
                testNgrokSolution(callback);
                
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> 
                    callback.onConnectionFailed("خطأ في اكتشاف الاتصال: " + e.getMessage()));
            }
        }).start();
    }
    
    private void testSmartDiscovery(ConnectionCallback callback) {
        String discoveryUrl = "http://192.168.1.17/Smart-Hotel/SmartNetworkDiscovery.php";
        
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                discoveryUrl,
                null,
                response -> {
                    try {
                        String bestUrl = response.getString("url");
                        if (bestUrl != null && !bestUrl.isEmpty()) {
                            callback.onConnectionFound(bestUrl);
                            return;
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing discovery response", e);
                    }
                },
                error -> {
                    Log.d(TAG, "Smart discovery failed: " + error.getMessage());
                }
        );
        
        requestQueue.add(request);
    }
    
    private void testLocalUrls(ConnectionCallback callback) {
        CountDownLatch latch = new CountDownLatch(testedUrls.size());
        final String[] foundUrl = {null};
        
        for (String url : testedUrls) {
            testSingleUrl(url, new UrlTestCallback() {
                @Override
                public void onUrlTested(String url, boolean success) {
                    if (success && foundUrl[0] == null) {
                        foundUrl[0] = url;
                        callback.onConnectionFound(url);
                    }
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void testSingleUrl(String url, UrlTestCallback callback) {
        String testUrl = url + "test_connection.php";
        
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                testUrl,
                null,
                response -> callback.onUrlTested(url, true),
                error -> callback.onUrlTested(url, false)
        );
        
        request.setRetryPolicy(new DefaultRetryPolicy(
                3000, // 3 seconds timeout
                1, // 1 retry
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        
        requestQueue.add(request);
    }
    
    private void testNgrokSolution(ConnectionCallback callback) {
        // إرشاد المستخدم لاستخدام ngrok
        new Handler(Looper.getMainLooper()).post(() -> {
            callback.onProgressUpdate("جاري اكتشاف أفضل طريقة اتصال...");
            callback.onProgressUpdate("يمكنك استخدام ngrok لحل مشاكل الشبكة:");
            callback.onProgressUpdate("1. حمل ngrok من: ngrok.com");
            callback.onProgressUpdate("2. شغل: ngrok http 80");
            callback.onProgressUpdate("3. استخدم الرابط في التطبيق");
        });
    }
    
    /**
     * إضافة URL مخصص
     */
    public void addCustomUrl(String url) {
        if (!testedUrls.contains(url)) {
            testedUrls.add(url);
        }
    }
    
    /**
     * التحقق من الاتصال بالإنترنت
     */
    public boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }
    
    /**
     * الحصول على جميع URLs المحلية
     */
    public List<String> getAllTestUrls() {
        return new ArrayList<>(testedUrls);
    }
    
    /**
     * إنشاء ملف إعدادات Android التلقائي
     */
    public String generateAndroidConfig() {
        return "NetworkConfig.java";
    }
    
    private interface UrlTestCallback {
        void onUrlTested(String url, boolean success);
    }
    
    /**
     * نموذج لإعدادات الاتصال الآمنة
     */
    public static class ConnectionSettings {
        public String primaryUrl;
        public String backupUrl;
        public String ngrokUrl;
        public boolean useHttps;
        public int timeout;
        
        public ConnectionSettings() {
            this.timeout = 5000;
            this.useHttps = false; // للتطوير فقط
        }
    }
}
