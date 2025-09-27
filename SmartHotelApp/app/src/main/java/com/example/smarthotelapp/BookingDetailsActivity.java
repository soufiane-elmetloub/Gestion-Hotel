package com.example.smarthotelapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class BookingDetailsActivity extends AppCompatActivity {

    private TextView tvVisitor, tvCheckIn, tvCheckOut, tvNights, tvGuests, tvRoomInfo, tvFloor, tvTotal, tvPaymentStatus, tvReservationId, tvPricePerNight, tvEmployee;
    private ChipGroup guestsChipGroup, roomChipGroup;
    private LinearLayout detailsContainer;
    private static final int REQ_WRITE_EXTERNAL = 2001;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_details);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Booking Details");
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
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus);
        tvReservationId = findViewById(R.id.tvReservationId);
        tvPricePerNight = findViewById(R.id.tvPricePerNight);
        tvEmployee = findViewById(R.id.tvEmployee);
        guestsChipGroup = findViewById(R.id.guestsChipGroup);
        roomChipGroup = findViewById(R.id.roomChipGroup);
        MaterialButton btnDone = findViewById(R.id.btnDone);
        MaterialButton btnInvoice = findViewById(R.id.btnInvoice);
        detailsContainer = findViewById(R.id.detailsContainer);

        final int labelColor = Color.parseColor("#212121");
        final int valueColor = Color.parseColor("#616161");

        // Read extras
        String first = getIntent().getStringExtra("first_name");
        String last = getIntent().getStringExtra("last_name");
        String checkIn = getIntent().getStringExtra("check_in_date");
        String checkOut = getIntent().getStringExtra("check_out_date");
        String roomNumber = getIntent().getStringExtra("room_number");
        String roomType = getIntent().getStringExtra("room_type");
        String capacity = getIntent().getStringExtra("capacity");
        int floor = getIntent().getIntExtra("floor", Integer.MIN_VALUE);
        double pricePerNight = getIntent().getDoubleExtra("price_per_night", -1);
        double totalAmount = getIntent().getDoubleExtra("total_amount", -1);
        int adults = getIntent().getIntExtra("adults", 0);
        int children = getIntent().getIntExtra("children", 0);
        int infants = getIntent().getIntExtra("infants", 0);
        String paymentStatus = getIntent().getStringExtra("payment_status");
        int reservationId = getIntent().getIntExtra("reservation_id", -1);
        String employeeName = getIntent().getStringExtra("employee_name");

        // Visitor
        String visitorName = (first != null ? first : "").trim() + (last != null && !last.trim().isEmpty() ? (" " + last.trim()) : "");
        if (visitorName.trim().isEmpty()) visitorName = "—";
        setLabelValue(tvVisitor, "Visitor:", (visitorName.isEmpty() ? "—" : " " + visitorName), labelColor, valueColor);

        // Dates and nights
        setLabelValue(tvCheckIn, "Check-in:", " " + (checkIn != null && !checkIn.isEmpty() ? checkIn : "—"), labelColor, valueColor);
        setLabelValue(tvCheckOut, "Check-out:", " " + (checkOut != null && !checkOut.isEmpty() ? checkOut : "—"), labelColor, valueColor);
        int nightsVal = (checkIn != null && !checkIn.isEmpty() && checkOut != null && !checkOut.isEmpty()) ? computeNights(checkIn, checkOut) : 0;
        if (nightsVal <= 0 && checkIn != null && !checkIn.isEmpty() && checkOut != null && !checkOut.isEmpty()) nightsVal = 1;
        setLabelValue(tvNights, "Nights:", " " + (nightsVal > 0 ? String.valueOf(nightsVal) : "—"), labelColor, valueColor);

        // Guests
        boolean hasAnyGuests = (adults + children + infants) > 0;
        String primaryLabel = null;
        if (adults > 0) primaryLabel = adults + "Adults";
        else if (children > 0) primaryLabel = children + "Children";
        else if (infants > 0) primaryLabel = infants + "Infants";

        setLabelValue(tvGuests, "Guests:", "", labelColor, valueColor);
        tvGuests.setVisibility(hasAnyGuests ? View.VISIBLE : View.GONE);
        guestsChipGroup.removeAllViews();
        if (hasAnyGuests && primaryLabel != null) {
            Chip chip = new Chip(this);
            chip.setText(primaryLabel);
            chip.setCheckable(false);
            chip.setClickable(false);
            guestsChipGroup.addView(chip);
            guestsChipGroup.setVisibility(View.VISIBLE);
        } else {
            guestsChipGroup.setVisibility(View.GONE);
        }

        // Room info
        String roomLabel = (roomNumber != null && !roomNumber.isEmpty()) ? roomNumber : "—";
        setLabelValue(tvRoomInfo, "Room:", " " + roomLabel, labelColor, valueColor);
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

        // Floor
        setLabelValue(tvFloor, "Floor:", " " + (floor != Integer.MIN_VALUE ? String.valueOf(floor) : "—"), labelColor, valueColor);

        // Price per night and total
        if (pricePerNight > 0) {
            setLabelValue(tvPricePerNight, "Price/Night:", " " + ((int) Math.round(pricePerNight)) + " MAD", labelColor, valueColor);
        } else {
            setLabelValue(tvPricePerNight, "Price/Night:", " —", labelColor, valueColor);
        }
        String totalTxt = (totalAmount > 0) ? ((int) Math.round(totalAmount)) + " MAD" : "—";
        setLabelValue(tvTotal, "Total:", " " + totalTxt, labelColor, valueColor);

        // Payment status and reservation id
        if (paymentStatus == null || paymentStatus.isEmpty()) paymentStatus = "—";
        setLabelValue(tvPaymentStatus, "Payment:", " " + paymentStatus, labelColor, valueColor);
        if (reservationId > 0) {
            setLabelValue(tvReservationId, "Reservation #:", " " + reservationId, labelColor, valueColor);
        } else {
            setLabelValue(tvReservationId, "Reservation #:", " —", labelColor, valueColor);
        }

        // Employee name
        if (employeeName == null || employeeName.trim().isEmpty()) employeeName = "—";
        setLabelValue(tvEmployee, "Employee:", " " + employeeName, labelColor, valueColor);

        if (btnDone != null) {
            btnDone.setOnClickListener(v -> {
                Intent intent = new Intent(BookingDetailsActivity.this, MainActivity.class);
                intent.putExtra("open_tab", "bookings");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }
        if (btnInvoice != null) {
            btnInvoice.setOnClickListener(v -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_WRITE_EXTERNAL);
                        return;
                    }
                }
                exportDetailsToPdf();
            });
        }
    }

    private void setLabelValue(TextView tv, String label, String value, int labelColor, int valueColor) {
        if (tv == null) return;
        if (label == null) label = "";
        if (value == null) value = "";
        String full = label + value;
        SpannableString sp = new SpannableString(full);
        sp.setSpan(new StyleSpan(Typeface.BOLD), 0, label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sp.setSpan(new ForegroundColorSpan(labelColor), 0, label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
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

    private void exportDetailsToPdf() {
        if (detailsContainer == null) {
            Toast.makeText(this, "Nothing to export", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure the view is measured to full size
        int widthSpec = View.MeasureSpec.makeMeasureSpec(detailsContainer.getWidth() > 0 ? detailsContainer.getWidth() : 1080, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        detailsContainer.measure(widthSpec, heightSpec);
        int contentWidth = detailsContainer.getMeasuredWidth();
        int contentHeight = detailsContainer.getMeasuredHeight();
        if (contentWidth <= 0 || contentHeight <= 0) {
            detailsContainer.post(this::exportDetailsToPdf);
            return;
        }

        // Render the view into a bitmap
        Bitmap bitmap = Bitmap.createBitmap(contentWidth, contentHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        detailsContainer.layout(0, 0, contentWidth, contentHeight);
        detailsContainer.draw(canvas);

        // Create a single-page PDF with same size as content
        PdfDocument pdf = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(contentWidth, contentHeight, 1).create();
        PdfDocument.Page page = pdf.startPage(pageInfo);
        Canvas pdfCanvas = page.getCanvas();
        pdfCanvas.drawBitmap(bitmap, 0, 0, null);
        pdf.finishPage(page);

        // Build a filename
        String fileName = "invoice_" + System.currentTimeMillis() + ".pdf";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Save using MediaStore into Downloads/SmartHotel (no runtime permission needed)
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/SmartHotel");

            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                pdf.close();
                Toast.makeText(this, "Failed to create file", Toast.LENGTH_LONG).show();
                return;
            }
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                pdf.writeTo(os);
                Toast.makeText(this, "Invoice saved to Downloads/SmartHotel", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            } finally {
                pdf.close();
                bitmap.recycle();
            }
        } else {
            // Android 9 and below: Save via File API to public Downloads/SmartHotel (requires WRITE_EXTERNAL_STORAGE)
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File dir = new File(downloads, "SmartHotel");
            if (!dir.exists() && !dir.mkdirs()) {
                pdf.close();
                bitmap.recycle();
                Toast.makeText(this, "Failed to access Downloads/SmartHotel", Toast.LENGTH_LONG).show();
                return;
            }
            File outFile = new File(dir, fileName);
            try (OutputStream os = new FileOutputStream(outFile)) {
                pdf.writeTo(os);
                Toast.makeText(this, "Invoice saved to " + outFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            } finally {
                pdf.close();
                bitmap.recycle();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_WRITE_EXTERNAL) {
            if (grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportDetailsToPdf();
            } else {
                Toast.makeText(this, "Permission denied. Can't save PDF.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
