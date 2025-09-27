package com.example.smarthotelapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.net.Uri;
import android.text.TextWatcher;
import android.text.Editable;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// PDF imports
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

// Excel imports

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;

import android.widget.TextView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class BookingsInfoActivity extends AppCompatActivity {

    private String currentFilter = "ALL";
    private RecyclerView rv;
    private TextView tvCount;
    private MaterialButton btnAll;
    private GuestCapsuleRecyclerAdapter adapter;
    private RequestQueue queue;
    private MaterialButton btnToggleEnded;
    private boolean showEnded = false;
    private View endedDateContainer;
    private TextInputEditText etEndedDate;
    private TextInputEditText etSearch;
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookings_info);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Bookings Info");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        // Hook end icon (filter) inside the search TextInputLayout
        TextInputLayout tilSearch = findViewById(R.id.tilSearch);
        if (tilSearch != null) {
            updateSearchHintWithFilter(tilSearch);
            tilSearch.setEndIconOnClickListener(v -> {
                String[] options = new String[]{"ALL", "Paid", "Pending", "Partial"};
                int selected = indexOf(options, currentFilter);
                if (selected < 0) selected = 0;

                new AlertDialog.Builder(this)
                        .setTitle("Select status")
                        .setSingleChoiceItems(options, selected, (dialog, which) -> {
                            currentFilter = options[which];
                            updateSearchHintWithFilter(tilSearch);
                            // Trigger filtering with current search query
                            fetchBookings();
                            Toast.makeText(this, "Filter: " + currentFilter, Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            });
        }

        // Dynamic data setup
        rv = findViewById(R.id.rvGuestsWithReservations);
        tvCount = findViewById(R.id.tvGuestsCount);
        btnAll = findViewById(R.id.btnExportPDF);
        btnToggleEnded = findViewById(R.id.btnToggleEnded);
        endedDateContainer = findViewById(R.id.endedDateContainer);
        etEndedDate = findViewById(R.id.etEndedDate);
        etSearch = findViewById(R.id.etSearch);
        
        // Setup search functionality
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    currentSearchQuery = s.toString().trim();
                    fetchBookings();
                }
            });
        }
        
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setNestedScrollingEnabled(true);
            rv.setHasFixedSize(false);
            // Reduce flicker by disabling change animations and increasing cache
            if (rv.getItemAnimator() instanceof androidx.recyclerview.widget.SimpleItemAnimator) {
                androidx.recyclerview.widget.SimpleItemAnimator anim =
                        (androidx.recyclerview.widget.SimpleItemAnimator) rv.getItemAnimator();
                anim.setSupportsChangeAnimations(false);
            }
            rv.setItemViewCacheSize(20);
            rv.getRecycledViewPool().setMaxRecycledViews(0, 20);
            adapter = new GuestCapsuleRecyclerAdapter();
            rv.setAdapter(adapter);
            adapter.setOnGuestClickListener(guest -> {
                Intent i = new Intent(this, GuestDetailsActivity.class);
                i.putExtra("reservation_id", guest.id);
                i.putExtra("first_name", guest.firstName);
                i.putExtra("last_name", guest.lastName);
                i.putExtra("check_in", guest.checkIn);
                i.putExtra("check_out", guest.checkOut);
                i.putExtra("room_number", guest.roomNumber);
                i.putExtra("number_of_guests", guest.numberOfGuests);
                i.putExtra("status", guest.status);
                i.putExtra("payment_status", guest.paymentStatus);
                i.putExtra("total_amount", guest.totalAmount);
                startActivity(i);
            });
        }
        com.google.android.material.button.MaterialButton btnExportPDF = findViewById(R.id.btnExportPDF);
        btnExportPDF.setOnClickListener(v -> exportToPDF());

        if (btnToggleEnded != null) {
            updateToggleButtonText();
            btnToggleEnded.setOnClickListener(v -> {
                showEnded = !showEnded;
                updateToggleButtonText();
                if (endedDateContainer != null) {
                    endedDateContainer.setVisibility(showEnded ? View.VISIBLE : View.GONE);
                }
                if (!showEnded && etEndedDate != null) {
                    etEndedDate.setText("");
                }
                fetchBookings();
            });
        }

        // Click to open a date picker for ended date (field and start icon)
        if (etEndedDate != null) {
            etEndedDate.setOnClickListener(v -> openEndedDatePicker());
        }
        TextInputLayout tilEndedDate = findViewById(R.id.tilEndedDate);
        if (tilEndedDate != null) {
            tilEndedDate.setStartIconOnClickListener(v -> openEndedDatePicker());
        }

        queue = Volley.newRequestQueue(this);
        fetchBookings();
    }

    private void updateSearchHintWithFilter(TextInputLayout til) {
        if (til == null) return;
        String base = "Search...";
        if (currentFilter != null && !currentFilter.isEmpty() && !"ALL".equalsIgnoreCase(currentFilter)) {
            til.setHint(base + " (" + currentFilter + ")");
        } else {
            til.setHint(base);
        }
    }

    private void fetchBookings() {
        // When showEnded is true, fetch only ended bookings; else fetch only active bookings
        String base = NetworkConfig.getBookingsUrl();
        String param = showEnded ? "ended_only=1" : "active_only=1";
        String url = base + (base.contains("?") ? "&" : "?") + param;
        
        // If a date is provided, filter by check-in date for the selected day
        if (etEndedDate != null && etEndedDate.getText() != null) {
            String d = etEndedDate.getText().toString().trim();
            if (!d.isEmpty()) {
                url += "&check_in_date=" + Uri.encode(d);
            }
        }
        
        // Add search parameter if search query is not empty
        if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
            url += "&search_name=" + Uri.encode(currentSearchQuery);
        }
        
        // Add payment filter if not "ALL"
        if (currentFilter != null && !currentFilter.equals("ALL")) {
            url += "&payment_filter=" + Uri.encode(currentFilter);
        }

        // Append logged-in employee_id to restrict results to assigned rooms
        try {
            SessionManager sm = new SessionManager(this);
            String userId = sm.getUserId();
            if (userId != null && !userId.isEmpty() && !"-1".equals(userId)) {
                url += "&employee_id=" + Uri.encode(userId);
            }
        } catch (Exception ignored) { }
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                res -> {
                    try {
                        boolean success = res.optBoolean("success", false);
                        if (!success) {
                            Toast.makeText(this, res.optString("message", "Error fetching data"), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        JSONArray arr = res.optJSONArray("data");
                        List<BookingCapsule> list = new ArrayList<>();
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.getJSONObject(i);
                                BookingCapsule b = new BookingCapsule();
                                b.id = o.optInt("id");
                                b.firstName = o.optString("first_name");
                                b.lastName = o.optString("last_name");
                                b.checkIn = o.optString("check_in");
                                b.checkOut = o.optString("check_out");
                                b.roomNumber = o.optString("room_number");
                                b.numberOfGuests = o.optInt("number_of_guests", 0);
                                b.status = o.optString("status");
                                b.paymentStatus = o.optString("payment_status");
                                b.totalAmount = o.optString("total_amount");
                                list.add(b);
                            }
                        }
                        if (adapter != null) adapter.setItems(list);
                        int count = list.size();
                        if (tvCount != null) tvCount.setText(String.valueOf(count));
                        TextView tvTitle = findViewById(R.id.tvGuestsTitle);
                        if (tvTitle != null) {
                            String phrase = showEnded ? "Guests with Ended Reservations" : "Guests with Reservations";
                            tvTitle.setText(phrase);
                        }
                        if (count == 0) {
                            Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, "Failed to parse data", Toast.LENGTH_SHORT).show();
                    }
                },
                err -> Toast.makeText(this, "Unable to connect to server", Toast.LENGTH_SHORT).show()
        );
        queue.add(req);
    }

    private void updateToggleButtonText() {
        if (btnToggleEnded == null) return;
        // If currently showing ended, offer to show active; else offer to show ended
        btnToggleEnded.setText(showEnded ? "Show Active" : "Show Ended");
        if (endedDateContainer != null) {
            endedDateContainer.setVisibility(showEnded ? View.VISIBLE : View.GONE);
        }
    }

    private void openEndedDatePicker() {
        final Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH);
        int d = c.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog dlg = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            // month is 0-based
            int mm = month + 1;
            String mmStr = (mm < 10 ? "0" : "") + mm;
            String ddStr = (dayOfMonth < 10 ? "0" : "") + dayOfMonth;
            String formatted = year + "-" + mmStr + "-" + ddStr;
            if (etEndedDate != null) etEndedDate.setText(formatted);
            // Optionally refetch with date filter later
            fetchBookings();
        }, y, m, d);
        dlg.show();
    }

    private int indexOf(String[] arr, String value) {
        if (value == null) return -1;
        for (int i = 0; i < arr.length; i++) {
            if (value.equalsIgnoreCase(arr[i])) return i;
        }
        return -1;
    }


    private void exportToPDF() {
        try {
            // Create filename with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "hotel_bookings_" + timestamp + ".pdf";
            
            // Create file in Downloads directory
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File pdfFile = new File(downloadsDir, fileName);
            
            // Create PDF document
            PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            
            // Add title
            document.add(new Paragraph("Smart Hotel - Bookings Report")
                    .setFontSize(18)
                    .setBold());
            
            document.add(new Paragraph("Generated on: " + 
                    new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()))
                    .setFontSize(12));
            
            document.add(new Paragraph(" ")); // Empty line
            
            // Create table with 6 columns
            Table table = new Table(6);
            
            // Add headers
            table.addHeaderCell("Guest Name");
            table.addHeaderCell("Room");
            table.addHeaderCell("Check In");
            table.addHeaderCell("Check Out");
            table.addHeaderCell("Status");
            table.addHeaderCell("Amount");
            
            // Add data from adapter
            if (adapter != null && adapter.getItems() != null) {
                for (BookingCapsule booking : adapter.getItems()) {
                    table.addCell(booking.firstName + " " + booking.lastName);
                    table.addCell(booking.roomNumber);
                    table.addCell(booking.checkIn);
                    table.addCell(booking.checkOut);
                    table.addCell(booking.status);
                    table.addCell(booking.totalAmount);
                }
            }
            
            document.add(table);
            document.close();
            
            Toast.makeText(this, "PDF exported to Downloads: " + fileName, Toast.LENGTH_LONG).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "Error exporting PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
