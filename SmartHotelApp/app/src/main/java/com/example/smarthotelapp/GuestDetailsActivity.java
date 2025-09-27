package com.example.smarthotelapp;

import android.os.Bundle;
import android.widget.TextView;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.card.MaterialCardView;

public class GuestDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_details);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Guest Details");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        // Back button in layout (explicit English back)
        android.view.View backBtn = findViewById(R.id.btnBack);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        int reservationId = getIntent().getIntExtra("reservation_id", -1);
        String firstName = getIntent().getStringExtra("first_name");
        String lastName = getIntent().getStringExtra("last_name");
        String checkIn = getIntent().getStringExtra("check_in");
        String checkOut = getIntent().getStringExtra("check_out");
        String roomNumber = getIntent().getStringExtra("room_number");
        String status = getIntent().getStringExtra("status");
        String paymentStatus = getIntent().getStringExtra("payment_status");
        String totalAmount = getIntent().getStringExtra("total_amount");
        int numberOfGuests = getIntent().getIntExtra("number_of_guests", 0);

        TextView tvResId = findViewById(R.id.tvReservationId);
        TextView tvName = findViewById(R.id.tvGuestName);
        TextView tvCheckIn = findViewById(R.id.tvCheckIn);
        TextView tvCheckOut = findViewById(R.id.tvCheckOut);
        TextView tvRoom = findViewById(R.id.tvRoom);
        TextView tvGuests = findViewById(R.id.tvGuests);
        TextView tvStatus = findViewById(R.id.tvStatus);
        TextView tvPayment = findViewById(R.id.tvPayment);
        TextView tvTotal = findViewById(R.id.tvTotal);
        MaterialCardView card = findViewById(R.id.cardGuest);

        // Colors
        int labelColor = Color.parseColor("#111111"); // dark
        int valueColor = Color.parseColor("#616161"); // light

        if (tvResId != null) tvResId.setText(makeLabeledSpan("Reservation :", String.valueOf(reservationId), labelColor, valueColor));
        if (tvName != null) tvName.setText(makeDoubleLabeledSpan(
                "first name :", safe(firstName), "   last name :", safe(lastName), labelColor, valueColor));
        if (tvCheckIn != null) tvCheckIn.setText(makeLabeledSpan("Check-in :", safe(checkIn), labelColor, valueColor));
        if (tvCheckOut != null) tvCheckOut.setText(makeLabeledSpan("Check-out :", safe(checkOut), labelColor, valueColor));
        if (tvRoom != null) tvRoom.setText(makeLabeledSpan("Room :", safe(roomNumber), labelColor, valueColor));
        
        // Handle guests display - show only if there are guests, hide completely if none
        if (tvGuests != null) {
            if (numberOfGuests > 0) {
                tvGuests.setText(makeLabeledSpan("Guests :", String.valueOf(numberOfGuests), labelColor, valueColor));
                tvGuests.setVisibility(android.view.View.VISIBLE);
            } else {
                tvGuests.setVisibility(android.view.View.GONE);
            }
        }
        
        if (tvStatus != null) tvStatus.setText(makeLabeledSpan("Status :", safe(status), labelColor, valueColor));
        if (tvPayment != null) tvPayment.setText(makeLabeledSpan("Payment :", safe(paymentStatus), labelColor, valueColor));
        if (tvTotal != null) tvTotal.setText(makeLabeledSpan("Total :", safe(totalAmount), labelColor, valueColor));

        // Auto color the card based on payment/status
        if (card != null) {
            int bg = colorForStatus(safe(status), safe(paymentStatus));
            card.setCardBackgroundColor(bg);
            // Choose a subtle stroke for contrast
            card.setStrokeWidth(2);
            card.setStrokeColor(adjustStrokeFor(bg));
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private CharSequence makeLabeledSpan(String label, String value, int labelColor, int valueColor) {
        String l = label == null ? "" : label;
        String v = value == null ? "" : value;
        SpannableStringBuilder sb = new SpannableStringBuilder();
        int startL = sb.length();
        sb.append(l);
        sb.setSpan(new ForegroundColorSpan(labelColor), startL, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (!l.endsWith(" ")) sb.append(' ');
        int startV = sb.length();
        sb.append(v);
        sb.setSpan(new ForegroundColorSpan(valueColor), startV, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

    private CharSequence makeDoubleLabeledSpan(String label1, String value1, String label2, String value2, int labelColor, int valueColor) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        int s1 = sb.length();
        sb.append(label1);
        sb.setSpan(new ForegroundColorSpan(labelColor), s1, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (!label1.endsWith(" ")) sb.append(' ');
        int v1 = sb.length();
        sb.append(value1);
        sb.setSpan(new ForegroundColorSpan(valueColor), v1, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(' ');
        int s2 = sb.length();
        sb.append(label2);
        sb.setSpan(new ForegroundColorSpan(labelColor), s2, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (!label2.endsWith(" ")) sb.append(' ');
        int v2 = sb.length();
        sb.append(value2);
        sb.setSpan(new ForegroundColorSpan(valueColor), v2, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

    private int colorForStatus(String status, String payment) {
        // Priority: payment first, then reservation status
        if (equalsAny(payment, "PAID", "Paid")) {
            return Color.parseColor("#E3F2FD"); // light blue
        }
        if (equalsAny(payment, "PARTIAL", "Partial")) {
            return Color.parseColor("#FFEBEE"); // light red
        }
        if (equalsAny(payment, "PENDING", "Pending")) {
            return Color.parseColor("#FFF3E0"); // light orange
        }

        if (equalsAny(status, "RESERVED", "CHECKED_IN", "ACTIVE")) {
            return Color.parseColor("#E8F5E9"); // light green
        }
        if (equalsAny(status, "CANCELLED", "CANCELED")) {
            return Color.parseColor("#ECEFF1"); // light gray
        }
        return Color.WHITE;
    }

    private boolean equalsAny(String v, String... opts) {
        if (v == null) return false;
        for (String o : opts) {
            if (v.equalsIgnoreCase(o)) return true;
        }
        return false;
    }

    private int adjustStrokeFor(int bg) {
        // Return a slightly darker stroke color from background
        int r = (bg >> 16) & 0xFF;
        int g = (bg >> 8) & 0xFF;
        int b = bg & 0xFF;
        r = Math.max(0, r - 32);
        g = Math.max(0, g - 32);
        b = Math.max(0, b - 32);
        return Color.rgb(r, g, b);
    }
}
