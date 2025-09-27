package com.example.smarthotelapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

public class BookingsFragment extends Fragment {

    private TextView totalBookingsCount;
    private TextView tvNewBookingsCount;
    private TextView tvDeparturesTodayCount;
    private TextView tvRoomsOccupiedCount;
    private TextView pendingBookingsCount;
    private TextView activeBookingsCount;
    private TextView dailyRevenueAmount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bookings, container, false);
        
        // Initialize views
        totalBookingsCount = view.findViewById(R.id.totalBookingsCount);
        tvNewBookingsCount = view.findViewById(R.id.tvNewBookingsCount);
        tvDeparturesTodayCount = view.findViewById(R.id.tvDeparturesTodayCount);
        tvRoomsOccupiedCount = view.findViewById(R.id.tvRoomsOccupiedCount);
        pendingBookingsCount = view.findViewById(R.id.pendingBookingsCount);
        activeBookingsCount = view.findViewById(R.id.activeBookingsCount);
        dailyRevenueAmount = view.findViewById(R.id.dailyRevenueAmount);

        // Handle Add Booking button click (opens AddBookingActivity)
        View btnAddBooking = view.findViewById(R.id.btnAddBooking);
        if (btnAddBooking != null) {
            btnAddBooking.setOnClickListener(v -> {
                Intent i = new Intent(getActivity(), AddBookingActivity.class);
                startActivity(i);
            });
        }
        
        // Handle Bookings Info button click (opens BookingsInfoActivity)
        View btnBookingsInfo = view.findViewById(R.id.btnBookingsInfo);
        if (btnBookingsInfo != null) {
            btnBookingsInfo.setOnClickListener(v -> {
                Intent i = new Intent(getActivity(), BookingsInfoActivity.class);
                startActivity(i);
            });
        }

        // Handle Show Revenue click (opens RevenueReportActivity)
        View btnShowRevenue = view.findViewById(R.id.btnShowRevenue);
        if (btnShowRevenue != null) {
            btnShowRevenue.setOnClickListener(v -> {
                Intent i = new Intent(getActivity(), RevenueReportActivity.class);
                startActivity(i);
            });
        }
        
        // Load booking statistics
        loadBookingStatistics();
        // Load daily revenue
        loadDailyRevenue();
        // Load available rooms for employee's section
        fetchAvailableRoomsForSection();
        // Load departures today dynamically
        fetchDeparturesToday();
        // Load new bookings today dynamically
        fetchNewBookingsToday();
        
        return view;
    }

    private void fetchNewBookingsToday() {
        String url = NetworkConfig.getNewBookingsTodayUrl();

        StringRequest req = new StringRequest(Request.Method.GET, url,
            response -> {
                try {
                    JSONObject obj = new JSONObject(response);
                    if (obj.optBoolean("success", false)) {
                        JSONObject data = obj.getJSONObject("data");
                        int newBookings = data.optInt("new_bookings_today", 0);
                        if (tvNewBookingsCount != null) animateCounter(tvNewBookingsCount, 0, newBookings);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            },
            error -> {
                error.printStackTrace();
            }
        );

        RequestQueue q = Volley.newRequestQueue(getActivity());
        q.add(req);
    }

    private void fetchDeparturesToday() {
        String url = NetworkConfig.getDeparturesTodayUrl();

        StringRequest req = new StringRequest(Request.Method.GET, url,
            response -> {
                try {
                    JSONObject obj = new JSONObject(response);
                    if (obj.optBoolean("success", false)) {
                        JSONObject data = obj.getJSONObject("data");
                        int departures = data.optInt("departures_today", 0);
                        if (tvDeparturesTodayCount != null) animateCounter(tvDeparturesTodayCount, 0, departures);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            },
            error -> {
                error.printStackTrace();
            }
        );

        RequestQueue q = Volley.newRequestQueue(getActivity());
        q.add(req);
    }

    private void loadBookingStatistics() {
        fetchBookingStatistics();
    }
    
    private void fetchBookingStatistics() {
        String url = NetworkConfig.getBookingStatsUrl();
        
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
            response -> {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.getBoolean("success")) {
                        JSONObject data = jsonResponse.getJSONObject("data");
                        
                        int totalBookings = data.getInt("total_bookings");
                        int checkedOutGuests = data.getInt("checked_out_guests");
                        int occupiedRooms = data.getInt("occupied_rooms");
                        
                        // عرض الأرقام مع رسوم متحركة
                        animateCounter(totalBookingsCount, 0, totalBookings);
                        // لا نقوم بتحديث tvDeparturesTodayCount هنا؛ سيتم جلبها من endpoint خاص بالمغادرين اليوم
                        // لا نقوم بتحديث tvRoomsOccupiedCount هنا حتى لا نكتب فوق عدد الغرف المتاحة للقسم
                        animateCounter(pendingBookingsCount, 0, occupiedRooms);
                        animateCounter(activeBookingsCount, 0, checkedOutGuests);
                        
                    } else {
                        // في حالة فشل الاستعلام، عرض قيم افتراضية
                        showFallbackValues();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    showFallbackValues();
                }
            },
            error -> {
                error.printStackTrace();
                showFallbackValues();
            });
        
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        queue.add(stringRequest);
    }

    private void loadDailyRevenue() {
        fetchDailyRevenue();
    }

    private void fetchDailyRevenue() {
        String url = NetworkConfig.getDailyRevenueUrl();

        StringRequest req = new StringRequest(Request.Method.GET, url,
            response -> {
                try {
                    JSONObject obj = new JSONObject(response);
                    if (obj.optBoolean("success", false)) {
                        // Prefer raw string from DB to avoid double-formatting (e.g., 5000.00.00)
                        String raw = obj.optString("daily_total_str", null);
                        if (raw == null || raw.isEmpty()) {
                            // Fallbacks
                            if (obj.has("formatted_total")) {
                                raw = obj.optString("formatted_total", "0.00");
                            } else {
                                double val = obj.optDouble("daily_total", 0.0);
                                raw = String.valueOf(val);
                            }
                        }
                        if (dailyRevenueAmount != null) dailyRevenueAmount.setText(raw + " MAD");
                    } else {
                        if (dailyRevenueAmount != null) dailyRevenueAmount.setText("0.00 MAD");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    if (dailyRevenueAmount != null) dailyRevenueAmount.setText("0.00 MAD");
                }
            },
            error -> {
                error.printStackTrace();
                if (dailyRevenueAmount != null) dailyRevenueAmount.setText("0.00 MAD");
            }
        );

        RequestQueue q = Volley.newRequestQueue(getActivity());
        q.add(req);
    }
    
    private void showFallbackValues() {
        // قيم افتراضية في حالة فشل الاتصال
        animateCounter(totalBookingsCount, 0, 5);
        animateCounter(tvDeparturesTodayCount, 0, 0);
        // لا نغير قيمة tvRoomsOccupiedCount هنا حتى لا نكتب فوق قيمة الغرف المتاحة
        animateCounter(pendingBookingsCount, 0, 5);
        animateCounter(activeBookingsCount, 0, 0);
    }

    private void fetchAvailableRoomsForSection() {
        SessionManager sessionManager = new SessionManager(getContext());
        String section = sessionManager.getAssignedSection();
        if (section == null || section.isEmpty() || "Not Assigned".equalsIgnoreCase(section)) {
            // If no section, show 0 to avoid misleading info
            if (tvRoomsOccupiedCount != null) animateCounter(tvRoomsOccupiedCount, 0, 0);
            return;
        }

        String url = NetworkConfig.getAvailableRoomsBySectionUrl() + "?section=" + section;

        StringRequest req = new StringRequest(Request.Method.GET, url,
            response -> {
                try {
                    JSONObject obj = new JSONObject(response);
                    if (obj.optBoolean("success", false)) {
                        JSONObject data = obj.getJSONObject("data");
                        int available = data.optInt("available_count", 0);
                        if (tvRoomsOccupiedCount != null) animateCounter(tvRoomsOccupiedCount, 0, available);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            },
            error -> {
                error.printStackTrace();
            }
        );

        RequestQueue q = Volley.newRequestQueue(getActivity());
        q.add(req);
    }
    
    private void animateCounter(TextView textView, int startValue, int endValue) {
        ValueAnimator animator = ValueAnimator.ofInt(startValue, endValue);
        animator.setDuration(1500); // مدة الرسم المتحرك 1.5 ثانية
        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            textView.setText(String.valueOf(animatedValue));
        });
        animator.start();
    }
}
