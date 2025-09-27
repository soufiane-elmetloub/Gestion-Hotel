package com.example.smarthotelapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class RoomSelectionActivity extends AppCompatActivity {
    private RecyclerView recyclerRooms;
    
    private RoomDetailsAdapter adapter;

    // Summary UI
    private LinearLayout summaryContainer;
    private LinearLayout actionsRow;
    private TextView tvPricePerNightView, tvNightsView, tvGuestsView, tvTotalView;

    // Data
    private final List<RoomDetail> currentRooms = new ArrayList<>();
    private String checkInStr, checkOutStr;
    private String firstName, lastName;
    private RoomDetail lastSelectedRoom;
    private double lastComputedTotal;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_selection);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Room Selection");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        recyclerRooms = findViewById(R.id.recyclerRooms);
        recyclerRooms.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RoomDetailsAdapter(this);
        recyclerRooms.setAdapter(adapter);

        adapter.setOnItemClickListener(item -> showSummary(item));
        adapter.setOnConfirmClickListener(this::onConfirmBooking);

        // Summary UI
        summaryContainer = findViewById(R.id.summaryContainer);
        actionsRow = findViewById(R.id.actionsRow);
        tvPricePerNightView = findViewById(R.id.tvPricePerNight);
        tvNightsView = findViewById(R.id.tvNights);
        tvGuestsView = findViewById(R.id.tvGuests);
        tvTotalView = findViewById(R.id.tvTotal);

        // If specific room_ids were passed, include them
        Intent intent = getIntent();
        checkInStr = intent.getStringExtra("check_in_date");
        checkOutStr = intent.getStringExtra("check_out_date");
        firstName = intent.getStringExtra("first_name");
        lastName = intent.getStringExtra("last_name");
        ArrayList<Integer> selectedRoomIds = intent.getIntegerArrayListExtra("room_ids");
        String roomIdsCsv = intent.getStringExtra("room_ids");
        // Fallback: if guest counts were passed via Intent and session is empty, hydrate session
        if (BookingSession.getTotalGuests() <= 0) {
            int ia = intent.getIntExtra("adults_count", -1);
            int ic = intent.getIntExtra("children_count", -1);
            int ii = intent.getIntExtra("infants_count", -1);
            if (ia >= 0 || ic >= 0 || ii >= 0) {
                BookingSession.setCounts(Math.max(0, ia), Math.max(0, ic), Math.max(0, ii));
            }
        }

        // Wire the 'Bake' button (btnCancel) to go back to NextStepActivity
        MaterialButton btnCancel = findViewById(R.id.btnCancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                Intent back = new Intent(RoomSelectionActivity.this, NextStepActivity.class);
                if (checkInStr != null && !checkInStr.isEmpty()) back.putExtra("check_in_date", checkInStr);
                if (checkOutStr != null && !checkOutStr.isEmpty()) back.putExtra("check_out_date", checkOutStr);
                startActivity(back);
                finish();
            });
        }

        // Wire 'Next Step' to open ConfirmBookingActivity
        MaterialButton btnNext = findViewById(R.id.btnNextStep);
        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                Intent confirm = new Intent(RoomSelectionActivity.this, ConfirmBookingActivity.class);
                if (checkInStr != null && !checkInStr.isEmpty()) confirm.putExtra("check_in_date", checkInStr);
                if (checkOutStr != null && !checkOutStr.isEmpty()) confirm.putExtra("check_out_date", checkOutStr);
                if (firstName != null) confirm.putExtra("first_name", firstName);
                if (lastName != null) confirm.putExtra("last_name", lastName);

                // Fallback to first room if none explicitly selected
                RoomDetail roomToSend = lastSelectedRoom != null ? lastSelectedRoom : (!currentRooms.isEmpty() ? currentRooms.get(0) : null);
                if (roomToSend != null) {
                    confirm.putExtra("room_number", roomToSend.roomNumber);
                    confirm.putExtra("room_type", roomToSend.roomType);
                    confirm.putExtra("capacity", roomToSend.capacity);
                    confirm.putExtra("price_per_night", roomToSend.pricePerNight);
                    confirm.putExtra("floor", roomToSend.floor);
                    // Also pass the unique room_id needed for reservation insert
                    confirm.putExtra("room_id", roomToSend.roomId);
                }

                // If total wasn't computed from summary, compute now as fallback
                if (lastComputedTotal > 0) {
                    confirm.putExtra("total_amount", lastComputedTotal);
                } else if (roomToSend != null && checkInStr != null && !checkInStr.isEmpty() && checkOutStr != null && !checkOutStr.isEmpty()) {
                    int nights = computeNights(checkInStr, checkOutStr);
                    if (nights <= 0) nights = 1;
                    int adults = BookingSession.getAdults();
                    int children = BookingSession.getChildren();
                    double perNightTotal = roomToSend.pricePerNight + (roomToSend.pricePerNight * (adults * 0.5 + children * 0.25));
                    double total = perNightTotal * nights;
                    confirm.putExtra("total_amount", total);
                }
                startActivity(confirm);
            });
        }
        fetchRoomDetails(selectedRoomIds);
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density);
    }

    private void fetchRoomDetails(@Nullable ArrayList<Integer> roomIds) {
        String url = NetworkConfig.getRoomFullDetailsUrl();
        if (roomIds != null && !roomIds.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < roomIds.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(roomIds.get(i));
            }
            url += "?room_ids=" + sb;
        }

        StringRequest req = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override public void onResponse(String response) {
                        try {
                            JSONObject root = new JSONObject(response);
                            boolean success = root.optBoolean("success", false);
                            if (!success) {
                                showEmpty("فشل في جلب البيانات");
                                return;
                            }
                            JSONArray arr = root.optJSONArray("rooms");
                            if (arr == null || arr.length() == 0) {
                                showEmpty("لا توجد غرف للعرض");
                                return;
                            }
                            List<RoomDetail> data = new ArrayList<>();
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.getJSONObject(i);
                                RoomDetail rd = new RoomDetail();
                                rd.id = o.optInt("id");
                                rd.roomId = o.optInt("room_id");
                                rd.roomNumber = o.optString("room_number", "");
                                rd.roomType = o.optString("room_type", "");
                                rd.floor = o.optInt("floor");
                                rd.view = o.optString("view", "");
                                rd.pricePerNight = o.optDouble("price_per_night", 0.0);
                                rd.capacity = o.optString("capacity", "");
                                rd.maxOccupancy = o.optInt("max_occupancy", 1);
                                rd.status = o.optString("status", "");
                                rd.statusColor = o.optString("status_color", "#E8F5E8");
                                rd.isAvailable = o.optBoolean("is_available", false);
                                rd.cardColor = o.optString("card_color", "#F5F5F5");

                                JSONArray f = o.optJSONArray("features");
                                if (f != null) {
                                    for (int j = 0; j < f.length(); j++) {
                                        rd.features.add(f.optString(j));
                                    }
                                }
                                data.add(rd);
                            }
                            currentRooms.clear();
                            currentRooms.addAll(data);
                            adapter.submitList(data);
                            recyclerRooms.setVisibility(View.VISIBLE);
                        } catch (JSONException e) {
                            showEmpty("خطأ في قراءة البيانات");
                        }
                    }
                }, new Response.ErrorListener() {
            @Override public void onErrorResponse(VolleyError error) {
                showEmpty("تعذر الاتصال بالخادم");
            }
        });

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    private void showEmpty(String msg) {
        recyclerRooms.setVisibility(View.GONE);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        if (summaryContainer != null) summaryContainer.setVisibility(View.GONE);
        if (actionsRow != null) actionsRow.setVisibility(View.GONE);
    }

    private void showSummary(RoomDetail room) {
        if (checkInStr == null || checkOutStr == null || checkInStr.isEmpty() || checkOutStr.isEmpty()) {
            Toast.makeText(this, "Please select check-in and check-out dates first", Toast.LENGTH_SHORT).show();
            return;
        }
        // Remember last selected room for confirmation screen
        lastSelectedRoom = room;
        double basePricePerNight = room != null ? room.pricePerNight : (currentRooms.isEmpty() ? 0.0 : currentRooms.get(0).pricePerNight);
        int nights = computeNights(checkInStr, checkOutStr);
        if (nights <= 0) nights = 1;

        int adults = BookingSession.getAdults();
        int children = BookingSession.getChildren();
        int infants = BookingSession.getInfants();
        // Base + per-guest surcharges per night:
        // Adults: +50% each (including first). Children: +25% each. Infants: +0.
        double perNightTotal = basePricePerNight + (basePricePerNight * (adults * 0.5 + children * 0.25));
        double total = perNightTotal * nights;
        lastComputedTotal = total;

        if (tvPricePerNightView != null) tvPricePerNightView.setText("Base/night: " + (int)Math.round(basePricePerNight));
        if (tvNightsView != null) tvNightsView.setText("Nights: " + nights);
        if (tvGuestsView != null) tvGuestsView.setText("Guests: " + (adults + children + infants));
        if (tvTotalView != null) tvTotalView.setText("Total: " + (int)Math.round(total) + " MAD");
        if (summaryContainer != null) summaryContainer.setVisibility(View.VISIBLE);
        if (actionsRow != null) actionsRow.setVisibility(View.VISIBLE);
    }

    private void onConfirmBooking(RoomDetail room) {
        if (checkInStr == null || checkOutStr == null || checkInStr.isEmpty() || checkOutStr.isEmpty()) {
            Toast.makeText(this, "Please select check-in and check-out dates first", Toast.LENGTH_SHORT).show();
            return;
        }
        int totalGuests = BookingSession.getTotalGuests();

        double basePricePerNight = room != null ? room.pricePerNight : (currentRooms.isEmpty() ? 0.0 : currentRooms.get(0).pricePerNight);
        int nights = computeNights(checkInStr, checkOutStr);
        if (nights <= 0) nights = 1;

        int adults = BookingSession.getAdults();
        int children = BookingSession.getChildren();
        int infants = BookingSession.getInfants();

        // Base + per-guest surcharges per night:
        // Adults: +50% each (including first). Children: +25% each. Infants: +0.
        double perNightTotal2 = basePricePerNight + (basePricePerNight * (adults * 0.5 + children * 0.25));
        double total = perNightTotal2 * nights;

        if (tvPricePerNightView != null) tvPricePerNightView.setText("Base/night: " + (int)Math.round(basePricePerNight));
        if (tvNightsView != null) tvNightsView.setText("Nights: " + nights);
        if (tvGuestsView != null) tvGuestsView.setText("Guests: " + (adults + children + infants));
        if (tvTotalView != null) tvTotalView.setText("Total: " + (int)Math.round(total) + " MAD");
        if (summaryContainer != null) summaryContainer.setVisibility(View.VISIBLE);
        if (actionsRow != null) actionsRow.setVisibility(View.VISIBLE);
    }

    private int computeNights(String in, String out) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            long tIn = sdf.parse(in).getTime();
            long tOut = sdf.parse(out).getTime();
            long diff = tOut - tIn;
            return (int) Math.max(0, TimeUnit.MILLISECONDS.toDays(diff));
        } catch (ParseException e) {
            return 0;
        }
    }
}
