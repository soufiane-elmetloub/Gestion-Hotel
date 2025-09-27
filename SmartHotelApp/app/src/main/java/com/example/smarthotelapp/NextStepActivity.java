package com.example.smarthotelapp;

import android.os.Bundle;
import android.app.DatePickerDialog;
import android.widget.DatePicker;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.chip.Chip;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.smarthotelapp.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NextStepActivity extends AppCompatActivity {

    private TextInputEditText etCheckInDate;
    private TextInputEditText etCheckOutDate;
    private TextInputLayout tilCheckIn;
    private TextInputLayout tilCheckOut;
    private TabLayout tabLayout;
    private String currentFilter = "All";
    private ChipGroup cgCapacity;
    private Chip chipSingle, chipDouble, chipFamily;
    private String selectedCapacity = null;
    private android.widget.LinearLayout roomsRowsContainerNext;

    private RequestQueue requestQueue;
    private final List<Room> roomsList = new ArrayList<>();
    private final List<Room> filteredRooms = new ArrayList<>();
    private SessionManager sessionManager;

    private final Calendar checkInCal = Calendar.getInstance();
    private final Calendar checkOutCal = Calendar.getInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next_step);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Next Step");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        // Find views
        etCheckInDate = findViewById(R.id.etCheckInDate);
        etCheckOutDate = findViewById(R.id.etCheckOutDate);
        tilCheckIn = findViewById(R.id.tilCheckIn);
        tilCheckOut = findViewById(R.id.tilCheckOut);
        tabLayout = findViewById(R.id.tabLayoutRooms);
        cgCapacity = findViewById(R.id.cgCapacity);
        chipSingle = findViewById(R.id.chipSingle);
        chipDouble = findViewById(R.id.chipDouble);
        chipFamily = findViewById(R.id.chipFamily);
        roomsRowsContainerNext = findViewById(R.id.roomsRowsContainerNext);

        if (tabLayout != null) {
            setupTabs();
        }

        setupNetwork();
        sessionManager = new SessionManager(this);
        loadRoomsData();

        // Capacity chip listeners
        if (chipSingle != null) {
            chipSingle.setOnCheckedChangeListener((button, isChecked) -> {
                if (isChecked) { selectedCapacity = "Single"; uncheckOthers(chipSingle); toastCapacity(); }
                else if (selectedCapacity != null && selectedCapacity.equals("Single")) { selectedCapacity = null; }
                applyFilterAndDisplay();
            });
        }
        if (chipDouble != null) {
            chipDouble.setOnCheckedChangeListener((button, isChecked) -> {
                if (isChecked) { selectedCapacity = "Double"; uncheckOthers(chipDouble); toastCapacity(); }
                else if (selectedCapacity != null && selectedCapacity.equals("Double")) { selectedCapacity = null; }
                applyFilterAndDisplay();
            });
        }
        if (chipFamily != null) {
            chipFamily.setOnCheckedChangeListener((button, isChecked) -> {
                if (isChecked) { selectedCapacity = "Family"; uncheckOthers(chipFamily); toastCapacity(); }
                else if (selectedCapacity != null && selectedCapacity.equals("Family")) { selectedCapacity = null; }
                applyFilterAndDisplay();
            });
        }

        // No default selection for capacity chips (user must choose)
        selectedCapacity = null;

        // Set listeners
        if (etCheckInDate != null) {
            etCheckInDate.setOnClickListener(v -> showCheckInPicker());
        }
        if (etCheckOutDate != null) {
            etCheckOutDate.setOnClickListener(v -> showCheckOutPicker());
        }
    }

    private void showCheckInPicker() {
        final Calendar init = (Calendar) checkInCal.clone();
        DatePickerDialog dlg = new DatePickerDialog(
                this,
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    checkInCal.set(Calendar.YEAR, year);
                    checkInCal.set(Calendar.MONTH, month);
                    checkInCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    // If checkout is before new checkin, clear checkout
                    if (checkOutCal.before(checkInCal)) {
                        etCheckOutDate.setText("");
                        if (tilCheckOut != null) tilCheckOut.setError(null);
                    }
                    etCheckInDate.setText(formatDate(checkInCal));
                },
                init.get(Calendar.YEAR),
                init.get(Calendar.MONTH),
                init.get(Calendar.DAY_OF_MONTH)
        );
        // Prevent picking past dates if desired (optional)
        // dlg.getDatePicker().setMinDate(System.currentTimeMillis());
        dlg.show();
    }

    private void showCheckOutPicker() {
        final Calendar init = (Calendar) (etCheckOutDate.getText() != null && etCheckOutDate.getText().length() > 0
                ? checkOutCal.clone() : checkInCal.clone());

        DatePickerDialog dlg = new DatePickerDialog(
                this,
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    checkOutCal.set(Calendar.YEAR, year);
                    checkOutCal.set(Calendar.MONTH, month);
                    checkOutCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    if (!checkOutCal.after(checkInCal)) {
                        if (tilCheckOut != null) {
                            tilCheckOut.setError("Check-out must be after check-in");
                        }
                        etCheckOutDate.setText("");
                        Toast.makeText(this, "Please select a later date", Toast.LENGTH_SHORT).show();
                    } else {
                        if (tilCheckOut != null) tilCheckOut.setError(null);
                        etCheckOutDate.setText(formatDate(checkOutCal));
                    }
                },
                init.get(Calendar.YEAR),
                init.get(Calendar.MONTH),
                init.get(Calendar.DAY_OF_MONTH)
        );

        // Ensure checkout can't be before or same as checkin
        dlg.getDatePicker().setMinDate(checkInCal.getTimeInMillis() + 24L * 60 * 60 * 1000);
        dlg.show();
    }

    private String formatDate(Calendar cal) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(cal.getTime());
    }

    private boolean hasSelectedDates() {
        CharSequence in = etCheckInDate != null ? etCheckInDate.getText() : null;
        CharSequence out = etCheckOutDate != null ? etCheckOutDate.getText() : null;
        boolean ok = in != null && in.length() > 0 && out != null && out.length() > 0;
        if (!ok) {
            Toast.makeText(this, "Please select check-in and check-out dates", Toast.LENGTH_SHORT).show();
            if (tilCheckIn != null) tilCheckIn.setError(in == null || in.length() == 0 ? "Required" : null);
            if (tilCheckOut != null) tilCheckOut.setError(out == null || out.length() == 0 ? "Required" : null);
        } else {
            if (tilCheckIn != null) tilCheckIn.setError(null);
            if (tilCheckOut != null) tilCheckOut.setError(null);
        }
        return ok;
    }

    private void setupTabs() {
        // Mirror RoomsFragment tab labels
        tabLayout.removeAllTabs();
        TabLayout.Tab tAll = tabLayout.newTab().setText("All");
        tAll.setTag("tabAll");
        tabLayout.addTab(tAll);
        TabLayout.Tab tStd = tabLayout.newTab().setText("Standard");
        tStd.setTag("tabStandard");
        tabLayout.addTab(tStd);
        TabLayout.Tab tDel = tabLayout.newTab().setText("Deluxe");
        tDel.setTag("tabDeluxe");
        tabLayout.addTab(tDel);
        TabLayout.Tab tSuite = tabLayout.newTab().setText("Suite");
        tSuite.setTag("tabSuite");
        tabLayout.addTab(tSuite);
        TabLayout.Tab tVip = tabLayout.newTab().setText("VIP");
        tVip.setTag("tabVIP");
        tabLayout.addTab(tVip);

        // Default select first tab
        TabLayout.Tab first = tabLayout.getTabAt(0);
        if (first != null) first.select();

        // Selection behavior: mirror RoomsFragment's filtering trigger
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentFilter = String.valueOf(tab.getText());
                Toast.makeText(NextStepActivity.this, "Filter: " + currentFilter, Toast.LENGTH_SHORT).show();
                applyFilterAndDisplay();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {
                currentFilter = String.valueOf(tab.getText());
                Toast.makeText(NextStepActivity.this, "Filter: " + currentFilter, Toast.LENGTH_SHORT).show();
                applyFilterAndDisplay();
            }
        });
    }

    private void uncheckOthers(Chip keep) {
        if (chipSingle != null && chipSingle != keep) chipSingle.setChecked(false);
        if (chipDouble != null && chipDouble != keep) chipDouble.setChecked(false);
        if (chipFamily != null && chipFamily != keep) chipFamily.setChecked(false);
    }

    private void toastCapacity() {
        if (selectedCapacity != null) {
            Toast.makeText(this, "Capacity: " + selectedCapacity, Toast.LENGTH_SHORT).show();
        }
    }

    // Networking and data
    private void setupNetwork() {
        requestQueue = Volley.newRequestQueue(this);
    }

    private void loadRoomsData() {
        String base = com.example.smarthotelapp.NetworkConfig.getRoomsUrl();
        String empId = sessionManager != null ? sessionManager.getUserId() : "";
        String section = sessionManager != null ? sessionManager.getAssignedSection() : "";
        StringBuilder sb = new StringBuilder(base);
        if (!base.contains("?")) sb.append("?"); else sb.append("&");
        boolean appended = false;
        if (empId != null && !empId.isEmpty() && !"-1".equals(empId)) {
            sb.append("employee_id=").append(android.net.Uri.encode(empId));
            appended = true;
        }
        if (section != null && !section.isEmpty() && !"Not Assigned".equalsIgnoreCase(section) && !"غير محدد".equals(section)) {
            if (sb.charAt(sb.length()-1) != '?' && appended) sb.append("&");
            if (!appended && sb.charAt(sb.length()-1) != '?') sb.append("&");
            sb.append("section=").append(android.net.Uri.encode(section));
        }
        String url = sb.toString();
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            parseRoomsData(response.getJSONArray("rooms"));
                            applyFilterAndDisplay();
                        } else {
                            Toast.makeText(this, "فشل تحميل الغرف", Toast.LENGTH_SHORT).show();
                        }
                    } catch (org.json.JSONException e) {
                        Toast.makeText(this, "خطأ في تحليل البيانات", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "خطأ في الاتصال بالخادم", Toast.LENGTH_SHORT).show()
        );
        requestQueue.add(request);
    }

    private void parseRoomsData(org.json.JSONArray roomsArray) throws org.json.JSONException {
        roomsList.clear();
        for (int i = 0; i < roomsArray.length(); i++) {
            org.json.JSONObject roomObj = roomsArray.getJSONObject(i);
            Room room = new Room(
                    roomObj.getInt("id"),
                    roomObj.getString("room_number"),
                    roomObj.getString("room_type"),
                    roomObj.optString("capacity", ""),
                    roomObj.getString("status"),
                    roomObj.optString("status_color", "#E8F5E8"),
                    roomObj.optBoolean("is_available",
                            roomObj.optString("status", "").trim().equalsIgnoreCase("Available"))
            );
            roomsList.add(room);
        }
    }

    private void applyFilterAndDisplay() {
        filteredRooms.clear();
        // Only available rooms
        for (Room r : roomsList) {
            if (!r.isAvailable) continue;
            // Filter by tab
            if (currentFilter != null && !"All".equalsIgnoreCase(currentFilter)) {
                if (r.roomType == null || !r.roomType.trim().equalsIgnoreCase(currentFilter)) continue;
            }
            // Filter by capacity chip if selected
            if (selectedCapacity != null && !selectedCapacity.isEmpty()) {
                if (r.capacity == null || !r.capacity.trim().equalsIgnoreCase(selectedCapacity)) continue;
            }
            filteredRooms.add(r);
        }
        displayRooms();
    }

    private void displayRooms() {
        if (roomsRowsContainerNext == null) return;
        roomsRowsContainerNext.removeAllViews();

        // Always use the filtered list; do NOT fallback to all rooms to avoid confusing the staff
        List<Room> list = new ArrayList<>(filteredRooms);
        // keep only available (extra safety; filtered already enforces availability)
        list.removeIf(r -> !r.isAvailable);

        if (list.isEmpty()) {
            // Show empty state message with icon (English)
            android.widget.LinearLayout emptyWrap = new android.widget.LinearLayout(this);
            emptyWrap.setOrientation(android.widget.LinearLayout.VERTICAL);
            emptyWrap.setPadding(dp(16), dp(24), dp(16), dp(24));
            emptyWrap.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

            android.widget.ImageView iv = new android.widget.ImageView(this);
            iv.setImageResource(android.R.drawable.ic_dialog_info);
            iv.setColorFilter(android.graphics.Color.parseColor("#9E9E9E"));
            android.widget.LinearLayout.LayoutParams ivLp = new android.widget.LinearLayout.LayoutParams(dp(48), dp(48));
            ivLp.bottomMargin = dp(8);
            emptyWrap.addView(iv, ivLp);

            android.widget.TextView title = new android.widget.TextView(this);
            title.setText("No rooms match your filters");
            title.setTextSize(16);
            title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
            title.setTextColor(android.graphics.Color.parseColor("#555555"));
            title.setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER);
            emptyWrap.addView(title, new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            android.widget.TextView subtitle = new android.widget.TextView(this);
            subtitle.setText("Try changing room type or capacity to see available options.");
            subtitle.setTextSize(13);
            subtitle.setTextColor(android.graphics.Color.parseColor("#777777"));
            subtitle.setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER);
            android.widget.LinearLayout.LayoutParams subLp = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
            subLp.topMargin = dp(4);
            emptyWrap.addView(subtitle, subLp);

            roomsRowsContainerNext.addView(emptyWrap, new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            return;
        }

        // sort by numeric room number
        list.sort((a, b) -> Integer.compare(parseRoomNumber(a.roomNumber), parseRoomNumber(b.roomNumber)));

        android.widget.LinearLayout currentRow = null;
        for (int i = 0; i < list.size(); i++) {
            if (i % 5 == 0) {
                currentRow = createRowContainer();
            }
            Room room = list.get(i);
            android.view.View card = createRoomCard(room);
            if (currentRow != null) currentRow.addView(card);
        }
    }

    private int parseRoomNumber(String txt) {
        if (txt == null) return Integer.MAX_VALUE;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < txt.length(); i++) {
            char c = txt.charAt(i);
            if (Character.isDigit(c)) sb.append(c);
        }
        try { return sb.length() > 0 ? Integer.parseInt(sb.toString()) : Integer.MAX_VALUE; }
        catch (NumberFormatException e) { return Integer.MAX_VALUE; }
    }

    private android.widget.LinearLayout createRowContainer() {
        android.widget.HorizontalScrollView hsv = new android.widget.HorizontalScrollView(this);
        hsv.setLayoutParams(new android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setPadding(dp(8), dp(12), dp(8), dp(12));

        android.widget.LinearLayout row = new android.widget.LinearLayout(this);
        row.setLayoutParams(new android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setClipToPadding(false);
        hsv.addView(row);

        android.widget.LinearLayout wrapper = new android.widget.LinearLayout(this);
        wrapper.setLayoutParams(new android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        wrapper.setOrientation(android.widget.LinearLayout.VERTICAL);
        wrapper.addView(hsv);

        roomsRowsContainerNext.addView(wrapper);
        return row;
    }

    private android.view.View createRoomCard(Room room) {
        androidx.cardview.widget.CardView card = new androidx.cardview.widget.CardView(this);
        androidx.cardview.widget.CardView.LayoutParams cardParams = new androidx.cardview.widget.CardView.LayoutParams(
                (int) (110 * getResources().getDisplayMetrics().density),
                (int) (130 * getResources().getDisplayMetrics().density)
        );
        cardParams.setMargins(dp(4), 0, dp(4), 0);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(12));
        card.setCardElevation(dp(3));

        int cardColor;
        try { cardColor = android.graphics.Color.parseColor(room.statusColor); }
        catch (Exception e) { cardColor = android.graphics.Color.parseColor("#E8F5E8"); }
        card.setCardBackgroundColor(cardColor);

        android.widget.FrameLayout frame = new android.widget.FrameLayout(this);
        frame.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
        ));

        android.widget.LinearLayout content = new android.widget.LinearLayout(this);
        content.setOrientation(android.widget.LinearLayout.VERTICAL);
        content.setPadding(dp(10), dp(10), dp(10), dp(38));
        content.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
        ));

        android.widget.TextView tvNumber = new android.widget.TextView(this);
        tvNumber.setText("Room " + room.roomNumber);
        tvNumber.setTextSize(14);
        tvNumber.setTextColor(android.graphics.Color.parseColor("#333333"));
        tvNumber.setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER);
        tvNumber.setTypeface(tvNumber.getTypeface(), android.graphics.Typeface.BOLD);
        content.addView(tvNumber, new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

        android.widget.TextView tvType = new android.widget.TextView(this);
        tvType.setText(room.roomType);
        tvType.setTextSize(12);
        tvType.setTextColor(android.graphics.Color.parseColor("#333333"));
        tvType.setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER);
        tvType.setTypeface(tvType.getTypeface(), android.graphics.Typeface.BOLD);
        android.widget.LinearLayout.LayoutParams lpType = new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        lpType.topMargin = dp(1);
        content.addView(tvType, lpType);

        String capacityLabel = room.capacity == null || room.capacity.trim().isEmpty() ? "" : room.capacity;
        android.widget.TextView tvCapacity = new android.widget.TextView(this);
        tvCapacity.setText(capacityLabel);
        tvCapacity.setTextSize(11);
        tvCapacity.setTextColor(android.graphics.Color.parseColor("#666666"));
        tvCapacity.setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER);
        android.widget.LinearLayout.LayoutParams lpCap = new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        lpCap.topMargin = dp(1);
        content.addView(tvCapacity, lpCap);

        android.widget.TextView tvStatus = new android.widget.TextView(this);
        tvStatus.setText(room.status);
        tvStatus.setTextSize(11);
        int statusTextColor = android.graphics.Color.parseColor("#666666");
        String st = room.status == null ? "" : room.status.trim().toLowerCase(java.util.Locale.ROOT);
        if (st.contains("available") || st.contains("متاحة") || st.contains("فارغة")) {
            statusTextColor = android.graphics.Color.parseColor("#4CAF50");
        } else if (st.contains("reserved") || st.contains("محجوز") || st.contains("محجوزة") || st.contains("occupied")) {
            statusTextColor = android.graphics.Color.parseColor("#FF5722");
        }
        tvStatus.setTextColor(statusTextColor);
        tvStatus.setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER);
        android.widget.LinearLayout.LayoutParams lpStatus = new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        lpStatus.topMargin = dp(1);
        content.addView(tvStatus, lpStatus);

        frame.addView(content);

        android.widget.ImageButton eyeBtn = new android.widget.ImageButton(this);
        android.widget.FrameLayout.LayoutParams eyeLp = new android.widget.FrameLayout.LayoutParams(dp(88), dp(36));
        eyeLp.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
        eyeLp.setMargins(dp(8), dp(8), dp(8), dp(8));
        eyeBtn.setLayoutParams(eyeLp);
        eyeBtn.setBackgroundResource(com.example.smarthotelapp.R.drawable.eye_button_bg);
        android.graphics.drawable.Drawable bg = eyeBtn.getBackground();
        if (bg instanceof android.graphics.drawable.GradientDrawable) {
            ((android.graphics.drawable.GradientDrawable) bg).setColor(cardColor);
        }
        eyeBtn.setImageResource(android.R.drawable.ic_menu_view);
        eyeBtn.setColorFilter(android.graphics.Color.BLACK);
        eyeBtn.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        eyeBtn.setPadding(dp(12), dp(8), dp(12), dp(8));
        eyeBtn.setContentDescription("View details");
        eyeBtn.setOnClickListener(v -> {
            if (!hasSelectedDates()) return;
            android.content.Intent intent = new android.content.Intent(this, com.example.smarthotelapp.RoomSelectionActivity.class);
            intent.putExtra("roomNumber", room.roomNumber);
            intent.putExtra("roomType", room.roomType);
            intent.putExtra("capacity", room.capacity);
            intent.putExtra("status", room.status);
            java.util.ArrayList<Integer> ids = new java.util.ArrayList<>();
            ids.add(room.id);
            intent.putIntegerArrayListExtra("room_ids", ids);
            if (etCheckInDate != null) intent.putExtra("check_in_date", etCheckInDate.getText() != null ? etCheckInDate.getText().toString() : "");
            if (etCheckOutDate != null) intent.putExtra("check_out_date", etCheckOutDate.getText() != null ? etCheckOutDate.getText().toString() : "");
            // Forward main visitor name if provided from AddBookingActivity
            android.content.Intent src = getIntent();
            if (src != null) {
                String fn = src.getStringExtra("first_name");
                String ln = src.getStringExtra("last_name");
                if (fn != null) intent.putExtra("first_name", fn);
                if (ln != null) intent.putExtra("last_name", ln);
            }
            startActivity(intent);
        });
        frame.addView(eyeBtn);

        card.addView(frame);
        card.setOnClickListener(v -> {
            if (!hasSelectedDates()) return;
            android.content.Intent intent = new android.content.Intent(this, com.example.smarthotelapp.RoomSelectionActivity.class);
            intent.putExtra("roomNumber", room.roomNumber);
            intent.putExtra("roomType", room.roomType);
            intent.putExtra("capacity", room.capacity);
            intent.putExtra("status", room.status);
            java.util.ArrayList<Integer> ids = new java.util.ArrayList<>();
            ids.add(room.id);
            intent.putIntegerArrayListExtra("room_ids", ids);
            if (etCheckInDate != null) intent.putExtra("check_in_date", etCheckInDate.getText() != null ? etCheckInDate.getText().toString() : "");
            if (etCheckOutDate != null) intent.putExtra("check_out_date", etCheckOutDate.getText() != null ? etCheckOutDate.getText().toString() : "");
            // Forward main visitor name if provided from AddBookingActivity
            android.content.Intent src = getIntent();
            if (src != null) {
                String fn = src.getStringExtra("first_name");
                String ln = src.getStringExtra("last_name");
                if (fn != null) intent.putExtra("first_name", fn);
                if (ln != null) intent.putExtra("last_name", ln);
            }
            startActivity(intent);
        });

        return card;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density);
    }

    private static class Room {
        final int id;
        final String roomNumber;
        final String roomType;
        final String capacity;
        final String status;
        final String statusColor;
        final boolean isAvailable;
        Room(int id, String roomNumber, String roomType, String capacity, String status, String statusColor, boolean isAvailable) {
            this.id = id; this.roomNumber = roomNumber; this.roomType = roomType; this.capacity = capacity;
            this.status = status; this.statusColor = statusColor; this.isAvailable = isAvailable;
        }
    }
}
