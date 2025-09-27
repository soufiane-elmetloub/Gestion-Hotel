package com.example.smarthotelapp.ui.rooms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.smarthotelapp.R;
import com.example.smarthotelapp.NetworkConfig;
import com.example.smarthotelapp.SessionManager;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RoomsFragment extends Fragment {

    private LinearLayout roomsRowsContainer;
    private TextView totalRoomsText;
    private TextView availableRoomsText;
    private TextView occupiedRoomsText;
    private TabLayout tabLayout;

    private RequestQueue requestQueue;
    private final List<Room> roomsList = new ArrayList<>();
    private final List<Room> filteredRooms = new ArrayList<>();
    private String currentFilter = "All";
    private SessionManager sessionManager;
    // No static rows anymore; rows will be created dynamically inside roomsRowsContainer

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rooms, container, false);

        initializeViews(view);
        setupNetwork();
        sessionManager = new SessionManager(requireContext());
        loadRoomsData();

        return view;
    }

    // Extract numeric part of room number safely (e.g., "Room 101" -> 101, "101" -> 101)
    private int parseRoomNumber(String txt) {
        if (txt == null) return Integer.MAX_VALUE;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < txt.length(); i++) {
            char c = txt.charAt(i);
            if (Character.isDigit(c)) sb.append(c);
        }
        try {
            return sb.length() > 0 ? Integer.parseInt(sb.toString()) : Integer.MAX_VALUE;
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private void initializeViews(View view) {
        roomsRowsContainer = view.findViewById(R.id.roomsRowsContainer);

        totalRoomsText = view.findViewById(R.id.totalRoomsCount);
        availableRoomsText = view.findViewById(R.id.availableRoomsCount);
        occupiedRoomsText = view.findViewById(R.id.occupiedRoomsCount);

        // Tabs UI
        tabLayout = view.findViewById(R.id.tabLayoutRooms);
        if (tabLayout != null) {
            setupTabs();
        }

        // Nothing to discover; rows will be created dynamically when displaying rooms
    }

    private void setupNetwork() {
        requestQueue = Volley.newRequestQueue(requireContext());
    }

    private void loadRoomsData() {
        String base = NetworkConfig.getRoomsUrl();
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
                            applyFilter(currentFilter);
                            displayRooms();
                            updateStatistics();
                        } else {
                            showError("فشل في تحميل بيانات الغرف");
                        }
                    } catch (JSONException e) {
                        showError("خطأ في تحليل البيانات");
                    }
                },
                error -> showError("خطأ في الاتصال بالخادم")
        );

        requestQueue.add(request);
    }

    private void parseRoomsData(JSONArray roomsArray) throws JSONException {
        roomsList.clear();
        for (int i = 0; i < roomsArray.length(); i++) {
            JSONObject roomObj = roomsArray.getJSONObject(i);
            Room room = new Room(
                    roomObj.getInt("id"),
                    roomObj.getString("room_number"),
                    roomObj.getString("room_type"),
                    roomObj.getInt("floor"),
                    roomObj.getDouble("price_per_night"),
                    roomObj.getInt("max_occupancy"),
                    roomObj.optString("capacity", ""),
                    roomObj.getString("description_ar"),
                    roomObj.getString("status"),
                    roomObj.getString("status_color"),
                    roomObj.getBoolean("is_available")
            );
            roomsList.add(room);
        }
    }

    private void displayRooms() {
        if (roomsRowsContainer == null) return;
        roomsRowsContainer.removeAllViews();

        // Sort rooms by their numeric room number
        List<Room> sorted = new ArrayList<>(filteredRooms.isEmpty() ? roomsList : filteredRooms);
        sorted.sort((a, b) -> Integer.compare(parseRoomNumber(a.getRoomNumber()), parseRoomNumber(b.getRoomNumber())));

        // Create rows dynamically: 5 cards per row
        LinearLayout currentRow = null;
        for (int i = 0; i < sorted.size(); i++) {
            if (i % 5 == 0) {
                currentRow = createRowContainer();
            }
            Room room = sorted.get(i);
            View roomCard = createRoomCard(room);
            if (currentRow != null) currentRow.addView(roomCard);
        }
    }

    private void applyFilter(String filter) {
        filteredRooms.clear();
        if (roomsList.isEmpty()) return;
        if (filter == null || filter.equalsIgnoreCase("All")) {
            filteredRooms.addAll(roomsList);
            return;
        }
        String f = filter.trim().toLowerCase(java.util.Locale.ROOT);
        for (Room r : roomsList) {
            if (r.getRoomType() != null && r.getRoomType().trim().toLowerCase(java.util.Locale.ROOT).equals(f)) {
                filteredRooms.add(r);
            }
        }
    }
    
    private void setupTabs() {
        // Ensure known tabs in the desired order and attach listener
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

        // Select according to current filter
        selectTabForCurrentFilter();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                String text = String.valueOf(tab.getText());
                // Map to filter values used by applyFilter
                currentFilter = text; // "All", "Standard", "Deluxe", "Suite", "VIP"
                applyFilter(currentFilter);
                displayRooms();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {
                // Re-apply to scroll to top or refresh if needed
                String text = String.valueOf(tab.getText());
                currentFilter = text;
                applyFilter(currentFilter);
                displayRooms();
            }
        });
    }

    private void selectTabForCurrentFilter() {
        if (tabLayout == null) return;
        String want = currentFilter == null ? "All" : currentFilter;
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null && want.equalsIgnoreCase(String.valueOf(tab.getText()))) {
                tab.select();
                return;
            }
        }
        // default select All
        TabLayout.Tab first = tabLayout.getTabAt(0);
        if (first != null) first.select();
    }

    // Create a new horizontal row wrapped in a HorizontalScrollView
    private LinearLayout createRowContainer() {
        android.widget.HorizontalScrollView hsv = new android.widget.HorizontalScrollView(requireContext());
        hsv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setPadding(dp(8), dp(12), dp(8), dp(12));

        LinearLayout row = new LinearLayout(requireContext());
        row.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setClipToPadding(false);
        hsv.addView(row);

        // Wrap HSV inside a vertical spacer container to match original spacing
        LinearLayout wrapper = new LinearLayout(requireContext());
        wrapper.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(hsv);

        // Add wrapper to main container and return the row for adding cards
        roomsRowsContainer.addView(wrapper);
        return row;
    }

    private View createRoomCard(Room room) {
        // Build a small card programmatically to avoid missing layout file
        CardView card = new CardView(requireContext());
        CardView.LayoutParams cardParams = new CardView.LayoutParams(
                (int) (110 * getResources().getDisplayMetrics().density),
                (int) (130 * getResources().getDisplayMetrics().density)
        );
        cardParams.setMargins(dp(4), 0, dp(4), 0);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(12));
        card.setCardElevation(dp(3));

        int cardColor;
        try {
            cardColor = android.graphics.Color.parseColor(room.getStatusColor());
        } catch (Exception e) {
            cardColor = android.graphics.Color.parseColor("#E8F5E8");
        }
        card.setCardBackgroundColor(cardColor);

        // Use FrameLayout to overlay the eye button at bottom-center
        android.widget.FrameLayout frame = new android.widget.FrameLayout(requireContext());
        frame.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        // Add extra bottom padding to avoid overlapping with the eye button
        content.setPadding(dp(10), dp(10), dp(10), dp(38));
        content.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        TextView tvNumber = new TextView(requireContext());
        tvNumber.setText("Room " + room.getRoomNumber());
        tvNumber.setTextSize(14);
        tvNumber.setTextColor(android.graphics.Color.parseColor("#333333"));
        tvNumber.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tvNumber.setTypeface(tvNumber.getTypeface(), android.graphics.Typeface.BOLD);
        content.addView(tvNumber, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tvType = new TextView(requireContext());
        tvType.setText(room.getRoomType());
        tvType.setTextSize(12);
        tvType.setTextColor(android.graphics.Color.parseColor("#333333"));
        tvType.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tvType.setTypeface(tvType.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lpType = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpType.topMargin = dp(1);
        content.addView(tvType, lpType);

        // Capacity line: prefer API-provided capacity text; fallback to derived label
        String capacityLabel = room.getCapacity();
        if (capacityLabel == null || capacityLabel.trim().isEmpty()) {
            int occ = room.getMaxOccupancy();
            if (occ == 1) capacityLabel = "Single";
            else if (occ == 2) capacityLabel = "Double";
            else capacityLabel = occ + " Guests";
        }
        final String capacityForIntent = capacityLabel;

        TextView tvCapacity = new TextView(requireContext());
        tvCapacity.setText(capacityLabel);
        tvCapacity.setTextSize(11);
        tvCapacity.setTextColor(android.graphics.Color.parseColor("#666666"));
        tvCapacity.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        LinearLayout.LayoutParams lpCap = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpCap.topMargin = dp(1);
        content.addView(tvCapacity, lpCap);

        TextView tvStatus = new TextView(requireContext());
        tvStatus.setText(room.getStatus());
        tvStatus.setTextSize(11);
        // Set status text color: green for Available, red for Reserved/Occupied
        String st = room.getStatus() == null ? "" : room.getStatus().trim().toLowerCase(java.util.Locale.ROOT);
        int statusTextColor = android.graphics.Color.parseColor("#666666");
        if (st.contains("available") || st.contains("متاحة") || st.contains("فارغة")) {
            statusTextColor = android.graphics.Color.parseColor("#4CAF50");
        } else if (st.contains("reserved") || st.contains("محجوز") || st.contains("محجوزة") || st.contains("occupied")) {
            statusTextColor = android.graphics.Color.parseColor("#FF5722");
        }
        tvStatus.setTextColor(statusTextColor);
        tvStatus.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        LinearLayout.LayoutParams lpStatus = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpStatus.topMargin = dp(1);
        content.addView(tvStatus, lpStatus);

        // Add content to frame
        frame.addView(content);

        // Eye button overlay
        android.widget.ImageButton eyeBtn = new android.widget.ImageButton(requireContext());
        // Make button slightly wider and less tall for a sleeker pill look
        android.widget.FrameLayout.LayoutParams eyeLp = new android.widget.FrameLayout.LayoutParams(dp(88), dp(36));
        eyeLp.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
        eyeLp.setMargins(dp(8), dp(8), dp(8), dp(8));
        eyeBtn.setLayoutParams(eyeLp);
        eyeBtn.setBackgroundResource(com.example.smarthotelapp.R.drawable.eye_button_bg);
        // Set background fill to match the card color
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
            android.content.Intent intent = new android.content.Intent(requireContext(), com.example.smarthotelapp.RoomDetailsActivity.class);
            intent.putExtra("roomNumber", room.getRoomNumber());
            intent.putExtra("roomType", room.getRoomType());
            // Use the already computed capacity label as an effectively final variable
            intent.putExtra("capacity", capacityForIntent);
            intent.putExtra("status", room.getStatus());
            startActivity(intent);
        });

        frame.addView(eyeBtn);

        // Add frame to card
        card.addView(frame);

        card.setOnClickListener(v -> Toast.makeText(getContext(),
                "غرفة " + room.getRoomNumber() + " - " + room.getStatus(),
                Toast.LENGTH_SHORT).show());

        return card;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density);
    }

    private void updateStatistics() {
        int total = roomsList.size();
        int available = 0;
        for (Room r : roomsList) if (r.isAvailable()) available++;
        int occupied = total - available;

        totalRoomsText.setText(String.valueOf(total));
        availableRoomsText.setText(String.valueOf(available));
        occupiedRoomsText.setText(String.valueOf(occupied));
    }

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    private static class Room {
        private final int id;
        private final String roomNumber;
        private final String roomType;
        private final int floor;
        private final double pricePerNight;
        private final int maxOccupancy;
        private final String capacity;
        private final String descriptionAr;
        private final String status;
        private final String statusColor;
        private final boolean isAvailable;

        public Room(int id, String roomNumber, String roomType, int floor,
                    double pricePerNight, int maxOccupancy, String capacity, String descriptionAr,
                    String status, String statusColor, boolean isAvailable) {
            this.id = id;
            this.roomNumber = roomNumber;
            this.roomType = roomType;
            this.floor = floor;
            this.pricePerNight = pricePerNight;
            this.maxOccupancy = maxOccupancy;
            this.capacity = capacity;
            this.descriptionAr = descriptionAr;
            this.status = status;
            this.statusColor = statusColor;
            this.isAvailable = isAvailable;
        }

        // Getters
        public int getId() { return id; }
        public String getRoomNumber() { return roomNumber; }
        public String getRoomType() { return roomType; }
        public int getFloor() { return floor; }
        public double getPricePerNight() { return pricePerNight; }
        public int getMaxOccupancy() { return maxOccupancy; }
        public String getCapacity() { return capacity; }
        public String getDescriptionAr() { return descriptionAr; }
        public String getStatus() { return status; }
        public String getStatusColor() { return statusColor; }
        public boolean isAvailable() { return isAvailable; }
    }
}
