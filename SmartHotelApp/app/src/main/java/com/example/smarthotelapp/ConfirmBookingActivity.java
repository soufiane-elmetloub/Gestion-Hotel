package com.example.smarthotelapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ConfirmBookingActivity extends AppCompatActivity {

    private TextView tvVisitor, tvCheckIn, tvCheckOut, tvNights, tvGuests, tvRoomInfo, tvFloor, tvTotal;
    private ChipGroup guestsChipGroup, roomChipGroup, paymentStatusChipGroup;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_booking);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Confirm Booking");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        tvVisitor = findViewById(R.id.tvVisitor);
        tvCheckIn = findViewById(R.id.tvCheckIn);
        tvCheckOut = findViewById(R.id.tvCheckOut);
        tvNights = findViewById(R.id.tvNights);
        tvGuests = findViewById(R.id.tvGuests);
        tvRoomInfo = findViewById(R.id.tvRoomInfo);
        tvFloor = findViewById(R.id.tvFloor);
        tvTotal = findViewById(R.id.tvTotal);
        guestsChipGroup = findViewById(R.id.guestsChipGroup);
        roomChipGroup = findViewById(R.id.roomChipGroup);
        paymentStatusChipGroup = findViewById(R.id.paymentStatusChipGroup);

        // Colors for label/value
        final int labelColor = Color.parseColor("#212121"); // Dark
        final int valueColor = Color.parseColor("#616161"); // Light

        // Read extras
        String first = getIntent().getStringExtra("first_name");
        String last = getIntent().getStringExtra("last_name");
        String checkIn = getIntent().getStringExtra("check_in_date");
        String checkOut = getIntent().getStringExtra("check_out_date");
        String roomNumber = getIntent().getStringExtra("room_number");
        String roomType = getIntent().getStringExtra("room_type");
        String capacity = getIntent().getStringExtra("capacity");
        int floor = getIntent().getIntExtra("floor", Integer.MIN_VALUE);
        int roomId = getIntent().getIntExtra("room_id", -1);
        double pricePerNight = getIntent().getDoubleExtra("price_per_night", -1);
        double totalAmount = getIntent().getDoubleExtra("total_amount", -1);

        // Visitor name
        String visitorName = (first != null ? first : "").trim() + (last != null && !last.trim().isEmpty() ? (" " + last.trim()) : "");
        if (visitorName.trim().isEmpty()) visitorName = "—";
        setLabelValue(tvVisitor, "Visitor:", (visitorName.isEmpty() ? "—" : " " + visitorName), labelColor, valueColor);

        // Dates per line
        setLabelValue(tvCheckIn, "Check-in:", " " + (checkIn != null && !checkIn.isEmpty() ? checkIn : "—"), labelColor, valueColor);
        setLabelValue(tvCheckOut, "Check-out:", " " + (checkOut != null && !checkOut.isEmpty() ? checkOut : "—"), labelColor, valueColor);
        int nightsVal = (checkIn != null && !checkIn.isEmpty() && checkOut != null && !checkOut.isEmpty())
                ? computeNights(checkIn, checkOut) : 0;
        if (nightsVal <= 0 && checkIn != null && !checkIn.isEmpty() && checkOut != null && !checkOut.isEmpty()) nightsVal = 1;
        setLabelValue(tvNights, "Nights:", " " + (nightsVal > 0 ? String.valueOf(nightsVal) : "—"), labelColor, valueColor);

        // Guests from BookingSession
        int adults = BookingSession.getAdults();
        int children = BookingSession.getChildren();
        int infants = BookingSession.getInfants();
        int totalGuests = adults + children + infants;
        boolean hasAnyGuests = totalGuests > 0;
        // Prefer concise label like '1Adults' (or 2Children / 1Infants) based on first non-zero group
        String primaryLabel = null;
        if (adults > 0) primaryLabel = adults + "Adults";
        else if (children > 0) primaryLabel = children + "Children";
        else if (infants > 0) primaryLabel = infants + "Infants";

        if (tvGuests != null) {
            // Label only; value shown as chip
            setLabelValue(tvGuests, "Guests:", "", labelColor, valueColor);
            tvGuests.setVisibility(hasAnyGuests ? View.VISIBLE : View.GONE);
        }
        if (guestsChipGroup != null) {
            guestsChipGroup.removeAllViews();
            if (hasAnyGuests && primaryLabel != null) {
                guestsChipGroup.setVisibility(View.VISIBLE);
                Chip chip = new Chip(this);
                chip.setText(primaryLabel);
                chip.setCheckable(false);
                chip.setClickable(false);
                guestsChipGroup.addView(chip);
            } else {
                guestsChipGroup.setVisibility(View.GONE);
            }
        }

        // Room info: show only room number in the label
        String roomLabel = (roomNumber != null && !roomNumber.isEmpty()) ? roomNumber : "—";
        setLabelValue(tvRoomInfo, "Room:", " " + roomLabel, labelColor, valueColor);
        // Type/Capacity as small chips
        if (roomChipGroup != null) {
            roomChipGroup.removeAllViews();
            if (roomType != null && !roomType.isEmpty()) {
                Chip chipType = new Chip(this);
                chipType.setText(roomType);
                chipType.setTextSize(12f);
                chipType.setCheckable(false);
                chipType.setClickable(false);
                roomChipGroup.addView(chipType);
            }
            if (capacity != null && !capacity.isEmpty()) {
                Chip chipCap = new Chip(this);
                chipCap.setText(capacity);
                chipCap.setTextSize(12f);
                chipCap.setCheckable(false);
                chipCap.setClickable(false);
                roomChipGroup.addView(chipCap);
            }
            roomChipGroup.setVisibility((roomChipGroup.getChildCount() > 0) ? View.VISIBLE : View.GONE);
        }

        // Floor on its own line
        if (tvFloor != null) {
            setLabelValue(tvFloor, "Floor:", " " + (floor != Integer.MIN_VALUE ? String.valueOf(floor) : "—"), labelColor, valueColor);
        }

        // Total price: prefer passed total_amount from summary
        String totalTxt = "—";
        if (totalAmount > 0) {
            totalTxt = ((int) Math.round(totalAmount)) + " MAD";
        } else if (pricePerNight > 0 && checkIn != null && checkOut != null && !checkIn.isEmpty() && !checkOut.isEmpty()) {
            int nights = computeNights(checkIn, checkOut);
            if (nights <= 0) nights = 1;
            double perNightTotal = pricePerNight + (pricePerNight * (adults * 0.5 + children * 0.25));
            double total = perNightTotal * nights;
            totalTxt = ((int) Math.round(total)) + " MAD";
        }
        if (tvTotal != null) setLabelValue(tvTotal, "Total:", " " + totalTxt, labelColor, valueColor);

        // Buttons
        MaterialButton btnConfirm = findViewById(R.id.btnConfirmPay);
        MaterialButton btnCancel = findViewById(R.id.btnCancel);
        if (btnCancel != null) btnCancel.setOnClickListener(v -> finish());
        if (btnConfirm != null) btnConfirm.setOnClickListener(v -> {
            // Collect all required fields
            int clientId = BookingSession.getClientId();
            int numberOfGuests = BookingSession.getTotalGuests();

            // Ensure totals
            int nights = (checkIn != null && checkOut != null && !checkIn.isEmpty() && !checkOut.isEmpty()) ? computeNights(checkIn, checkOut) : 0;
            if (nights <= 0) nights = 1;
            double totalAmountLocal = totalAmount;
            if (totalAmountLocal <= 0 && pricePerNight > 0) {
                int adultsL = BookingSession.getAdults();
                int childrenL = BookingSession.getChildren();
                double perNightTotal = pricePerNight + (pricePerNight * (adultsL * 0.5 + childrenL * 0.25));
                totalAmountLocal = perNightTotal * nights;
            }

            // Employee from session
            SessionManager sm = new SessionManager(this);
            int employeeId;
            try {
                employeeId = Integer.parseInt(sm.getUserId());
            } catch (Exception e) {
                employeeId = -1;
            }

            // Payment status from chips (REQUIRED)
            String paymentStatus;
            if (paymentStatusChipGroup != null) {
                int checkedId = paymentStatusChipGroup.getCheckedChipId();
                if (checkedId == View.NO_ID) {
                    Toast.makeText(this, "Please select a payment status", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (checkedId == R.id.chipPaid) paymentStatus = "paid";
                else if (checkedId == R.id.chipPartial) paymentStatus = "partial";
                else paymentStatus = "pending"; // chipPending
            } else {
                Toast.makeText(this, "Please select a payment status", Toast.LENGTH_SHORT).show();
                return;
            }

            // Basic validation (allow zero guests)
            if (roomId <= 0 || clientId <= 0 || checkIn == null || checkOut == null
                    || checkIn.isEmpty() || checkOut.isEmpty() || pricePerNight <= 0 || totalAmountLocal <= 0 || employeeId <= 0) {
                Toast.makeText(this, "Missing required booking data", Toast.LENGTH_SHORT).show();
                return;
            }

            String url = NetworkConfig.getAddReservationUrl();

            // Make effectively-final copies for inner class usage
            final int fRoomId = roomId;
            final int fClientId = clientId;
            final int fGuests = numberOfGuests;
            final String fCheckIn = checkIn;
            final String fCheckOut = checkOut;
            final double fPricePerNight = pricePerNight;
            final double fTotalAmount = totalAmountLocal;
            final int fEmployeeId = employeeId;
            final String fPaymentStatus = paymentStatus;
            StringRequest req = new StringRequest(
                    Request.Method.POST,
                    url,
                    new Response.Listener<String>() {
                        @Override public void onResponse(String response) {
                            try {
                                JSONObject obj = new JSONObject(response);
                                boolean success = obj.optBoolean("success", false);
                                String msg = obj.optString("message", success ? "Reservation created" : "Failed");
                                Toast.makeText(ConfirmBookingActivity.this, msg, Toast.LENGTH_SHORT).show();
                                if (success) {
                                    // Extract reservation_id from data if present
                                    int reservationId = -1;
                                    JSONObject dataObj = obj.optJSONObject("data");
                                    if (dataObj != null) {
                                        reservationId = dataObj.optInt("reservation_id", -1);
                                    }

                                    // Navigate to BookingDetailsActivity with all details
                                    Intent intent = new Intent(ConfirmBookingActivity.this, BookingDetailsActivity.class);
                                    // Employee name from session
                                    String employeeName = sm.getUserName();
                                    intent.putExtra("first_name", first);
                                    intent.putExtra("last_name", last);
                                    intent.putExtra("check_in_date", fCheckIn);
                                    intent.putExtra("check_out_date", fCheckOut);
                                    intent.putExtra("room_number", roomNumber);
                                    intent.putExtra("room_type", roomType);
                                    intent.putExtra("capacity", capacity);
                                    intent.putExtra("floor", floor);
                                    intent.putExtra("price_per_night", fPricePerNight);
                                    intent.putExtra("total_amount", fTotalAmount);
                                    intent.putExtra("adults", BookingSession.getAdults());
                                    intent.putExtra("children", BookingSession.getChildren());
                                    intent.putExtra("infants", BookingSession.getInfants());
                                    intent.putExtra("payment_status", fPaymentStatus);
                                    intent.putExtra("reservation_id", reservationId);
                                    intent.putExtra("employee_name", employeeName);
                                    startActivity(intent);
                                    finish();
                                }
                            } catch (JSONException e) {
                                Toast.makeText(ConfirmBookingActivity.this, "Parse error", Toast.LENGTH_SHORT).show();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override public void onErrorResponse(VolleyError error) {
                            Toast.makeText(ConfirmBookingActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                        }
                    }
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> p = new HashMap<>();
                    p.put("room_id", String.valueOf(fRoomId));
                    p.put("client_id", String.valueOf(fClientId));
                    p.put("number_of_guests", String.valueOf(fGuests));
                    p.put("check_in", fCheckIn);
                    p.put("check_out", fCheckOut);
                    p.put("price_per_night", String.valueOf(fPricePerNight));
                    p.put("total_amount", String.valueOf(fTotalAmount));
                    p.put("employee_id", String.valueOf(fEmployeeId));
                    p.put("status", "reserved");
                    p.put("payment_status", fPaymentStatus);
                    return p;
                }
            };
            VolleySingleton.getInstance(this).addToRequestQueue(req);
        });
    }

    /**
     * Helper to render a label in bold dark color and an adjacent value in lighter color
     * inside the same TextView using a single Spannable string.
     */
    private void setLabelValue(TextView tv, String label, String value, int labelColor, int valueColor) {
        if (tv == null) return;
        if (label == null) label = "";
        if (value == null) value = "";
        String full = label + value;
        SpannableString sp = new SpannableString(full);
        // Label styling: bold + dark color
        sp.setSpan(new StyleSpan(Typeface.BOLD), 0, label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sp.setSpan(new ForegroundColorSpan(labelColor), 0, label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        // Value styling: lighter color (only if there is a value part)
        if (full.length() > label.length()) {
            sp.setSpan(new ForegroundColorSpan(valueColor), label.length(), full.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tv.setText(sp);
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
