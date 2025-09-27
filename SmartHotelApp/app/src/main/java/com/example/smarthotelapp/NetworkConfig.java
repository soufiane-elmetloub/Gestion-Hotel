package com.example.smarthotelapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetworkConfig {
    private static final String TAG = "NetworkConfig";
    private static final String EMULATOR_IP = "10.0.2.2";
    private static final String LOCAL_IP = "192.168.1.8"; // Default fallback (current PC IPv4 on Wi‑Fi)
    private static String REAL_DEVICE_IP = LOCAL_IP; // Current detected/cached IP address
    private static final String BASE_URL_FORMAT = "http://%s/Smart-Hotel";
    private static final String PREFS_NAME = "network_prefs";
    private static final String KEY_REAL_DEVICE_IP = "real_device_ip";
    private static volatile boolean probingStarted = false;
    private static android.content.Context appContext;
    
    // Auto-detect and get the best base URL
    public static String getBaseUrl() {
        // If a manual host is configured in Settings, prefer it
        String manual = getManualHost();
        String hostOrIp = (manual != null && !manual.isEmpty()) ? manual : getCurrentIP();
        String baseUrl = String.format(BASE_URL_FORMAT, hostOrIp);
        Log.d(TAG, "Using base URL: " + baseUrl);
        return baseUrl;
    }
    
    // Get base URL with trailing slash for consistency
    public static String getBaseUrlWithSlash() {
        String baseUrl = getBaseUrl();
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    public static String getLoginUrl() {
        return getBaseUrlWithSlash() + "backend/login.php";
    }

    public static String getRegisterUrl() {
        return getBaseUrlWithSlash() + "backend/register.php";
    }

    public static String getRoomsUrl() {
        return getBaseUrlWithSlash() + "backend/get_rooms.php";
    }

    public static String getRoomFullDetailsUrl() {
        return getBaseUrlWithSlash() + "backend/get_room_full_details.php";
    }

    public static String getDebugUrl() {
        return getBaseUrlWithSlash() + "backend/debug.php";
    }

    public static String getTestConnectionUrl() {
        // Use the backend test endpoint that exists in the project
        return getBaseUrlWithSlash() + "backend/test_connection.php";
    }

    public static String getBookingsUrl() {
        return getBaseUrlWithSlash() + "backend/get_bookings.php";
    }

    public static String getCreateBookingUrl() {
        return getBaseUrlWithSlash() + "backend/create_booking.php";
    }

    public static String getClientsStatsUrl() {
        return getBaseUrlWithSlash() + "backend/get_clients_stats.php";
    }

    public static String getRecentClientsUrl() {
        return getBaseUrlWithSlash() + "backend/get_recent_clients.php";
    }

    public static String getAddClientUrl() {
        return getBaseUrlWithSlash() + "backend/add_client.php";
    }

    public static String getAllClientsUrl() {
        return getBaseUrlWithSlash() + "backend/get_all_clients.php";
    }

    public static String getSearchClientsUrl() {
        return getBaseUrlWithSlash() + "backend/search_clients.php";
    }

    public static String getSearchClientUrl() {
        return getBaseUrlWithSlash() + "backend/search_clients.php";
    }

    public static String getBookingStatsUrl() {
        return getBaseUrlWithSlash() + "backend/get_booking_stats.php";
    }

    public static String getDailyRevenueUrl() {
        return getBaseUrlWithSlash() + "backend/get_daily_revenue.php";
    }

    public static String getRevenueStatsUrl() {
        return getBaseUrlWithSlash() + "backend/get_revenue_stats.php";
    }

    public static String getRevenueTableDataUrl() {
        return getBaseUrlWithSlash() + "backend/get_revenue_table_data.php";
    }


    public static String getUnreadReportCountsUrl(int employeeId) {
        return getBaseUrlWithSlash() + "backend/get_unread_report_counts.php?employee_id=" + employeeId;
    }

    public static String getMarkReportsReadUrl(int employeeId, String priority) {
        String url = getBaseUrlWithSlash() + "backend/mark_reports_read.php?employee_id=" + employeeId;
        if (priority != null && !priority.isEmpty()) {
            url += "&priority=" + priority;
        }
        return url;
    }

    public static String getAddReservationUrl() {
        return getBaseUrlWithSlash() + "backend/add_reservation.php";
    }

    public static String getAvailableRoomsBySectionUrl() {
        return getBaseUrlWithSlash() + "backend/get_available_rooms_by_section.php";
    }

    public static String getDeparturesTodayUrl() {
        return getBaseUrlWithSlash() + "backend/get_departures_today.php";
    }

    public static String getNewBookingsTodayUrl() {
        return getBaseUrlWithSlash() + "backend/get_new_bookings_today.php";
    }

    public static String getSubmitSupportRequestUrl() {
        return getBaseUrlWithSlash() + "backend/submit_support_request.php";
    }

    public static String getNotificationsUrl(int employeeId, boolean onlyUnread, int limit) {
        String base = getBaseUrlWithSlash() + "backend/get_notifications.php";
        String params = String.format("?employee_id=%d&only_unread=%s&limit=%d", employeeId, onlyUnread ? "1" : "0", limit);
        return base + params;
    }

    public static String getMarkNotificationReadUrl() {
        return getBaseUrlWithSlash() + "backend/mark_notification_read.php";
    }

    public static String getEmployeeTasksUrl() {
        return getBaseUrlWithSlash() + "backend/get_employee_tasks.php";
    }

    // Method to get current IP being used for debugging
    public static String getCurrentIP() {
        return isEmulator() ? EMULATOR_IP : REAL_DEVICE_IP;
    }

    // Method to set custom IP for real device
    public static void setRealDeviceIP(String ip) {
        REAL_DEVICE_IP = ip;
    }

    // Method to get local IP address for debugging
    public static String getLocalIP() {
        return REAL_DEVICE_IP;
    }

    // Get all possible IP addresses for testing
    public static String[] getAllPossibleIPs() {
        return new String[]{
            EMULATOR_IP,       // Emulator
            REAL_DEVICE_IP,    // Current setting
            "192.168.1.8",    // Current PC WiFi IP
            "192.168.11.228",  // Previous WiFi IP
            "192.168.1.17",    // Default
            "192.168.1.26",    // Alternative
            "192.168.1.1",     // Router
            "192.168.1.100",   // Common range
            "192.168.1.101",   // Common range
            "192.168.1.102",   // Common range
            "192.168.0.1",     // Alternative router
            "192.168.0.100",   // Alternative range
            "192.168.0.101",   // Alternative range
            "192.168.43.1",    // Mobile hotspot
            "172.20.10.2",     // iPhone hotspot
            "10.0.0.1",        // Alternative network
            "10.0.0.100",      // Alternative network
            "127.0.0.1"        // Localhost
        };
    }
    
    // Get prioritized IP addresses (most likely to work first)
    public static String[] getPrioritizedIPs() {
        if (isEmulator()) {
            return new String[]{EMULATOR_IP};
        }
        
        return new String[]{
            REAL_DEVICE_IP,    // Current setting (highest priority)
            "192.168.1.8",    // Current PC WiFi IP
            "192.168.11.228",  // Previous WiFi IP
            "192.168.1.17",    // Default
            "192.168.1.26",    // Alternative
            "192.168.1.1",     // Router
            "192.168.0.1",     // Alternative router
            "192.168.1.100",   // Common range
            "192.168.0.100",   // Alternative range
            "192.168.43.1",    // Mobile hotspot
            "172.20.10.2"      // iPhone hotspot
        };
    }

    // Method to get device info for debugging
    public static String getDeviceInfo() {
        return Build.MANUFACTURER + " " + Build.MODEL + " (API " + Build.VERSION.SDK_INT + ")";
    }

    // Check if running on emulator
    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    // Method to test connectivity with different IPs
    public static String[] getTestUrls() {
        String[] ips = getAllPossibleIPs();
        String[] urls = new String[ips.length];
        for (int i = 0; i < ips.length; i++) {
            urls[i] = "http://" + ips[i] + "/Smart-Hotel/backend/test_connection.php";
        }
        return urls;
    }

    // Get current network configuration summary
    public static String getNetworkSummary() {
        return "Current IP: " + getCurrentIP() +
               "\nIs Emulator: " + isEmulator() +
               "\nBase URL: " + getBaseUrl();
    }

    // Initialize network configuration: load cached IP and start background probing (real device only)
    public static void initialize(Context context) {
        if (context == null) return;
        appContext = context.getApplicationContext();
        if (isEmulator()) return; // Emulator always uses 10.0.2.2

        // Load cached IP if available
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cachedIp = prefs.getString(KEY_REAL_DEVICE_IP, null);
        if (cachedIp != null && !cachedIp.isEmpty()) {
            REAL_DEVICE_IP = cachedIp;
            Log.d(TAG, "Loaded cached real-device IP: " + REAL_DEVICE_IP);
        }

        // Start probing once
        if (probingStarted) return;
        probingStarted = true;

        new Thread(() -> {
            try {
                String[] candidates = getPrioritizedIPs();
                for (String ip : candidates) {
                    if (ip == null || ip.trim().isEmpty()) continue;
                    String testUrl = "http://" + ip + "/Smart-Hotel/backend/test_connection.php";
                    if (isReachable(testUrl, 1200)) { // ~1.2s timeout per candidate
                        if (!ip.equals(REAL_DEVICE_IP)) {
                            REAL_DEVICE_IP = ip;
                            prefs.edit().putString(KEY_REAL_DEVICE_IP, REAL_DEVICE_IP).apply();
                            Log.i(TAG, "Auto-detected working IP: " + REAL_DEVICE_IP);
                        } else {
                            Log.i(TAG, "Confirmed current IP is reachable: " + REAL_DEVICE_IP);
                        }
                        return; // stop after first working IP
                    } else {
                        Log.d(TAG, "IP not reachable: " + ip + " (tested " + testUrl + ")");
                    }
                }
                Log.w(TAG, "No reachable IP found from candidates; using fallback: " + REAL_DEVICE_IP);
            } catch (Exception e) {
                Log.e(TAG, "Error while probing IPs: " + e.getMessage());
            }
        }, "NetworkConfig-Prober").start();
    }

    // Lightweight reachability check for a URL with short timeouts
    private static boolean isReachable(String urlString, int timeoutMs) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            // Accept 200 OK. Optionally read small response to ensure server is ours.
            if (code == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line = br.readLine();
                    // If backend returns JSON starting with {, consider it valid
                    return line != null && line.trim().startsWith("{");
                } catch (Exception ignore) {
                    return true; // If can't read, still treat 200 as success
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // Public helpers for Settings screen and diagnostics
    public static boolean testHostReachable(String host, int timeoutMs) {
        if (host == null || host.trim().isEmpty()) return false;
        String h = host.trim();
        if (h.startsWith("http://")) h = h.substring(7);
        if (h.startsWith("https://")) h = h.substring(8);
        if (h.endsWith("/")) h = h.substring(0, h.length() - 1);
        String url = "http://" + h + "/Smart-Hotel/backend/test_connection.php";
        return isReachable(url, timeoutMs);
    }

    public static boolean testCurrentConfig(int timeoutMs) {
        String url = getTestConnectionUrl();
        return isReachable(url, timeoutMs);
    }

    private static String getManualHost() {
        try {
            if (appContext == null) return "";
            return SettingsManager.getInstance(appContext).getServerHost();
        } catch (Exception e) {
            return "";
        }
    }
}
