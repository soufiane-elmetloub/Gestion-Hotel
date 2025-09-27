package com.example.smarthotelapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class RevenueReportActivity extends AppCompatActivity {

    private TextView tvTitle;
    private TextView tvTodayTotal;
    private ProgressBar progress;
    private TextView tvMonthlyTotal2;
    private TextView tvDailyAverage;
    private TextView tvHighestDailyRevenue;
    private TextInputEditText etReportDate;
    private RecyclerView rvAnalysis;
    private RevenueTableAdapter tableAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue_report);

        tvTitle = findViewById(R.id.tvTitle);
        tvTodayTotal = findViewById(R.id.tvTodayTotal);
        progress = findViewById(R.id.progress);
        tvMonthlyTotal2 = findViewById(R.id.tvMonthlyTotal2);
        tvDailyAverage = findViewById(R.id.tvDailyAverage);
        tvHighestDailyRevenue = findViewById(R.id.tvHighestDailyRevenue);
        etReportDate = findViewById(R.id.etReportDate);
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }
        View btnBackBottom = findViewById(R.id.btnBackBottom);
        if (btnBackBottom != null) {
            btnBackBottom.setOnClickListener(v -> finish());
        }

        // Title
        tvTitle.setText("Financial Report");
        // Load dynamic stats for the first card
        loadRevenueStats();
        // Setup RecyclerView for analysis table
        rvAnalysis = findViewById(R.id.rvAnalysis);
        if (rvAnalysis != null) {
            rvAnalysis.setLayoutManager(new LinearLayoutManager(this));
            tableAdapter = new RevenueTableAdapter();
            rvAnalysis.setAdapter(tableAdapter);
            rvAnalysis.setNestedScrollingEnabled(true);
            // Initial load without filter (last N rows)
            loadRevenueTable(null);
        }

        // Setup date picker for the rounded date field
        if (etReportDate != null) {
            final Calendar calendar = Calendar.getInstance();

            // Keep empty initially; will show DatePicker on click
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            View.OnClickListener openPicker = v -> {
                int y = calendar.get(Calendar.YEAR);
                int m = calendar.get(Calendar.MONTH);
                int d = calendar.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog dlg = new DatePickerDialog(
                        RevenueReportActivity.this,
                        (view, year, month, dayOfMonth) -> {
                            calendar.set(year, month, dayOfMonth);
                            String selected = sdf.format(calendar.getTime());
                            etReportDate.setText(selected);
                            // Reload table filtered by selected date
                            loadRevenueTable(selected);
                        }, y, m, d);
                dlg.show();
            };

            etReportDate.setOnClickListener(openPicker);
            etReportDate.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) openPicker.onClick(v); });

            // Reference the TextInputLayout and hide X initially
            final TextInputLayout til = findViewById(R.id.tilReportDate);
            if (til != null) {
                til.setEndIconVisible(false);
                til.setEndIconOnClickListener(v -> {
                    etReportDate.setText("");
                    // TextWatcher below will trigger table reload and hide icon
                });
            }

            // Toggle X visibility based on text and reload when cleared
            etReportDate.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
                @Override public void afterTextChanged(Editable s) {
                    boolean hasText = s != null && s.length() > 0;
                    if (til != null) til.setEndIconVisible(hasText);
                    if (!hasText) {
                        loadRevenueTable(null);
                    }
                }
            });
        }
    }

    private void loadRevenueStats() {
        if (progress != null) progress.setVisibility(View.VISIBLE);

        String url = NetworkConfig.getRevenueStatsUrl();

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url.toString(),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (progress != null) progress.setVisibility(View.GONE);
                        try {
                            boolean success = response.optBoolean("success", false);
                            if (!success) {
                                setPlaceholders();
                                return;
                            }

                            JSONObject formatted = response.optJSONObject("formatted");
                            String today = formatted != null ? formatted.optString("today_total", null) : null;
                            String monthly = formatted != null ? formatted.optString("monthly_total", null) : null;
                            String avg = formatted != null ? formatted.optString("daily_average", null) : null;
                            String highest = formatted != null ? formatted.optString("highest_daily", null) : null;

                            if (today == null) today = formatMoney(response.optDouble("today_total", 0));
                            if (monthly == null) monthly = formatMoney(response.optDouble("monthly_total", 0));
                            if (avg == null) avg = formatMoney(response.optDouble("daily_average", 0));
                            if (highest == null) highest = formatMoney(response.optDouble("highest_daily", 0));

                            tvTodayTotal.setText(today + " MAD");
                            if (tvMonthlyTotal2 != null) tvMonthlyTotal2.setText(monthly + " MAD");
                            if (tvDailyAverage != null) tvDailyAverage.setText(avg + " MAD");
                            if (tvHighestDailyRevenue != null) tvHighestDailyRevenue.setText(highest + " MAD");
                        } catch (Exception e) {
                            setPlaceholders();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (progress != null) progress.setVisibility(View.GONE);
                        setPlaceholders();
                    }
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void loadRevenueTable(String dateFilter) {
        String base = NetworkConfig.getRevenueTableDataUrl();
        StringBuilder url = new StringBuilder(base);
        boolean hasQuery = false;
        if (dateFilter != null && !dateFilter.isEmpty()) {
            try {
                String enc = URLEncoder.encode(dateFilter, StandardCharsets.UTF_8.name());
                url.append("?date=").append(enc);
                hasQuery = true;
            } catch (Exception ignored) { /* keep base url on failure */ }
        }
        // Request more rows by default
        url.append(hasQuery ? "&" : "?").append("limit=100");

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url.toString(),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (!response.optBoolean("success", false)) {
                                if (tableAdapter != null) tableAdapter.setItems(new ArrayList<>());
                                return;
                            }
                            JSONArray rows = response.optJSONArray("rows");
                            List<RevenueTableItem> items = new ArrayList<>();
                            if (rows != null) {
                                for (int i = 0; i < rows.length(); i++) {
                                    JSONObject r = rows.optJSONObject(i);
                                    if (r == null) continue;
                                    String dateDM = r.optString("date_dm", "");
                                    JSONObject fmt = r.optJSONObject("formatted");
                                    String daily = fmt != null ? fmt.optString("daily_total", formatMoney(r.optDouble("daily_total", 0))) : formatMoney(r.optDouble("daily_total", 0));
                                    String weekly = fmt != null ? fmt.optString("weekly_total", formatMoney(r.optDouble("weekly_total", 0))) : formatMoney(r.optDouble("weekly_total", 0));
                                    String monthly = fmt != null ? fmt.optString("monthly_total", formatMoney(r.optDouble("monthly_total", 0))) : formatMoney(r.optDouble("monthly_total", 0));
                                    items.add(new RevenueTableItem(dateDM, daily, weekly, monthly));
                                }
                            }
                            if (tableAdapter != null) tableAdapter.setItems(items);
                        } catch (Exception ignored) {
                            if (tableAdapter != null) tableAdapter.setItems(new ArrayList<>());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (tableAdapter != null) tableAdapter.setItems(new ArrayList<>());
                    }
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    private void setPlaceholders() {
        tvTodayTotal.setText("0.00 MAD");
        if (tvMonthlyTotal2 != null) tvMonthlyTotal2.setText("0.00 MAD");
        if (tvDailyAverage != null) tvDailyAverage.setText("0.00 MAD");
        if (tvHighestDailyRevenue != null) tvHighestDailyRevenue.setText("0.00 MAD");
    }

    private String formatMoney(double value) {
        return String.format(Locale.getDefault(), "%.2f", value);
    }
}
