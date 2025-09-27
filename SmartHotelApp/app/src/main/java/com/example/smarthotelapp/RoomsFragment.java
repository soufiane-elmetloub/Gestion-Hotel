package com.example.smarthotelapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.widget.ImageButton;
import android.widget.FrameLayout;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;

import java.util.ArrayList;
import java.util.List;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class RoomsFragment extends Fragment {

    private TextView availableRoomsCount;
    private TextView occupiedRoomsCount;
    private TextView totalRoomsCount;
    
    // Tabs removed from layout; no tab views here
    
    // Dynamic rows container (vertical). Rows are created programmatically.
    private LinearLayout roomsRowsContainer;
    private List<View> allRoomCards = new ArrayList<>();
    private String currentFilter = "All";
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rooms, container, false);
        
        // Initialize views
        availableRoomsCount = view.findViewById(R.id.availableRoomsCount);
        occupiedRoomsCount = view.findViewById(R.id.occupiedRoomsCount);
        totalRoomsCount = view.findViewById(R.id.totalRoomsCount);
        
        // Tabs UI removed from XML; skip any tab initialization

        // Initialize dynamic rows container
        roomsRowsContainer = view.findViewById(R.id.roomsRowsContainer);

        // Session manager for employee info
        sessionManager = new SessionManager(requireContext());
        
        // Load initial data
        loadRoomStatistics();
        
        // جلب الغرف وعرضها ديناميكياً دون تغيير التصميم
        fetchRooms();
        
        return view;
    }

    // Tabs UI removed; filtering can still be invoked programmatically via filterRoomsByType("...")

    private void loadRoomStatistics() {
        // Load actual data from database
        fetchRoomStatistics();
    }
    
    private void fetchRoomStatistics() {
        String url = NetworkConfig.getBaseUrlWithSlash() + "backend/get_room_statistics.php";
        
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
            response -> {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    int total = jsonResponse.getInt("total");
                    int available = jsonResponse.getInt("available");
                    int occupied = jsonResponse.getInt("occupied");
                    
                    availableRoomsCount.setText(String.valueOf(available));
                    occupiedRoomsCount.setText(String.valueOf(occupied));
                    totalRoomsCount.setText(String.valueOf(total));
                    
                } catch (JSONException e) {
                    e.printStackTrace();
                    // Fallback to static values if API fails
                    showStaticValues();
                }
            },
            error -> {
                error.printStackTrace();
                // Fallback to static values if network fails
                showStaticValues();
            });
        
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        queue.add(stringRequest);
    }
    
    private void showStaticValues() {
        // Fallback values
        int available = 15;
        int occupied = 5;
        int total = 20;
        
        availableRoomsCount.setText(String.valueOf(available));
        occupiedRoomsCount.setText(String.valueOf(occupied));
        totalRoomsCount.setText(String.valueOf(total));
    }

    private void initializeRoomCards(View rootView) {
        // لم نعد نعتمد على البطاقات الثابتة، سيتم إنشاءها برمجياً عبر fetchRooms()
    }

    // Populate a single card view using TextViews tagged in XML
    private void populateCard(View card, String roomNumber, String roomType, String capacity, String status) {
        TextView tvRoom = findTextByTag(card, "room_number");
        if (tvRoom != null) tvRoom.setText("Room: " + roomNumber);

        TextView tvType = findTextByTag(card, "room_type");
        // Show type without prefix, only the value (e.g., Standard/Deluxe)
        if (tvType != null) tvType.setText(roomType);

        TextView tvCapacity = findTextByTag(card, "capacity");
        // Show capacity label-only like "Single" / "Double" without prefix
        if (tvCapacity != null) tvCapacity.setText(capacity);

        TextView tvStatus = findTextByTag(card, "status_text");
        TextView tvIndicator = findTextByTag(card, "status_indicator");
        if (tvStatus != null) {
            if ("Available".equalsIgnoreCase(status) || "متاحة".equals(status)) {
                tvStatus.setText("Available");
                tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                if (tvIndicator != null) tvIndicator.setText("🟢");
            } else {
                tvStatus.setText("غير متاحة");
                tvStatus.setTextColor(Color.parseColor("#FF5722"));
                if (tvIndicator != null) tvIndicator.setText("🔴");
            }
        }
    }

    // Recursive search for a TextView with a given android:tag inside a view hierarchy
    @Nullable
    private TextView findTextByTag(View parent, String tag) {
        if (parent instanceof TextView) {
            Object t = parent.getTag();
            if (t != null && tag.equals(t.toString())) {
                return (TextView) parent;
            }
        }
        if (parent instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) parent;
            for (int i = 0; i < vg.getChildCount(); i++) {
                TextView result = findTextByTag(vg.getChildAt(i), tag);
                if (result != null) return result;
            }
        }
        return null;
    }

    private void filterRoomsByType(String roomType) {
        currentFilter = roomType;
        // Use tracked dynamic cards list regardless of container structure
        for (View card : allRoomCards) {
            Object typeTag = card.getTag();
            String cardRoomType = typeTag instanceof String ? (String) typeTag : getRoomTypeFromCard(card);
            if (cardRoomType == null) cardRoomType = "";
            if ("All".equals(roomType) || roomType.equalsIgnoreCase(cardRoomType)) {
                card.setVisibility(View.VISIBLE);
            } else {
                card.setVisibility(View.GONE);
            }
        }
    }

    // Extracts the room type from the TextView tagged as "room_type"
    @Nullable
    private String getRoomTypeFromCard(View card) {
        TextView tvType = findTextByTag(card, "room_type");
        if (tvType == null) return null;
        String text = tvType.getText() != null ? tvType.getText().toString() : "";
        // Support both plain type (e.g., "Deluxe") and labeled (e.g., "Type: Deluxe")
        if (text.contains(":")) {
            String[] parts = text.split(":", 2);
            if (parts.length == 2) return parts[1].trim();
        }
        return text.trim();
    }

    public void updateRoomStatistics(int available, int occupied) {
        availableRoomsCount.setText(String.valueOf(available));
        occupiedRoomsCount.setText(String.valueOf(occupied));
        totalRoomsCount.setText(String.valueOf(available + occupied));
    }

    // ----------------------
    // ديناميكية بطاقات الغرف
    // ----------------------
    private void fetchRooms() {
        // Build URL with employee filtering so each employee sees only assigned rooms
        String base = NetworkConfig.getRoomsUrl();
        String empId = sessionManager != null ? sessionManager.getUserId() : "";
        String section = sessionManager != null ? sessionManager.getAssignedSection() : "";
        StringBuilder sb = new StringBuilder(base);
        if (!base.contains("?")) sb.append("?"); else sb.append("&");
        if (empId != null && !empId.isEmpty() && !"-1".equals(empId)) {
            sb.append("employee_id=").append(android.net.Uri.encode(empId));
        }
        if (section != null && !section.isEmpty() && !"Not Assigned".equalsIgnoreCase(section) && !"غير محدد".equals(section)) {
            if (sb.charAt(sb.length()-1) != '?' ) sb.append("&");
            sb.append("section=").append(android.net.Uri.encode(section));
        }
        String url = sb.toString();

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
            response -> {
                try {
                    // The backend may return either an object {rooms:[...]} or a raw array [...]
                    JSONArray rooms;
                    if (response != null && response.trim().startsWith("{")) {
                        JSONObject obj = new JSONObject(response);
                        if (obj.has("rooms")) {
                            rooms = obj.getJSONArray("rooms");
                        } else if (obj.has("data")) {
                            rooms = obj.getJSONArray("data");
                        } else {
                            // Fallback: try to interpret entire response as array string within object (unlikely)
                            rooms = new JSONArray();
                        }
                    } else {
                        rooms = new JSONArray(response);
                    }
                    // Clear all dynamic rows and cards then add dynamically
                    if (roomsRowsContainer != null) {
                        roomsRowsContainer.removeAllViews();
                    }
                    allRoomCards.clear();

                    int available = 0;
                    int occupied = 0;

                    LinearLayout currentRow = null;
                    for (int i = 0; i < rooms.length(); i++) {
                        if (i % 5 == 0) {
                            currentRow = createRowContainer();
                        }
                        JSONObject item = rooms.getJSONObject(i);
                        String number = item.optString("number", "");
                        String type = item.optString("type", "");
                        String capacity = item.optString("capacity", "");
                        String status = item.optString("status", "Available");

                        View card = buildRoomCard(number, type, capacity, status);
                        if (currentRow != null) currentRow.addView(card);
                        allRoomCards.add(card);

                        if ("Available".equalsIgnoreCase(status) || "متاحة".equals(status)) {
                            available++;
                        } else {
                            occupied++;
                        }
                    }

                    // تحديث الإحصائيات إذا لزم الأمر
                    updateRoomStatistics(available, occupied);

                    // تطبيق آخر فلتر مختار
                    filterRoomsByType(currentFilter);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            },
            error -> {
                error.printStackTrace();
                // في حال الفشل، لا نغيّر التصميم؛ تبقى البطاقات الثابتة كما هي
            });

        RequestQueue queue = Volley.newRequestQueue(getActivity());
        queue.add(stringRequest);
    }

    // إنشاء بطاقة مطابقة للتصميم الحالي (الأبعاد والألوان والنصوص)
    private View buildRoomCard(String roomNumber, String roomType, String capacity, String status) {
        if (getContext() == null) return new View(requireContext());

        // CardView
        CardView card = new CardView(requireContext());
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                dp(110), dp(130)
        );
        cardLp.setMarginStart(dp(4));
        cardLp.setMarginEnd(dp(4));
        card.setLayoutParams(cardLp);
        card.setRadius(dp(12));
        card.setCardElevation(dp(3));
        card.setUseCompatPadding(true);

        // لون الخلفية حسب الحالة لمطابقة التصميم:
        // متاحة: أخضر فاتح #E8F5E8 | غير متاحة: أحمر فاتح #FFE8E8
        boolean isAvailable = "Available".equalsIgnoreCase(status) || "متاحة".equals(status);
        int bgColor = Color.parseColor(isAvailable ? "#E8F5E8" : "#FFE8E8");
        card.setCardBackgroundColor(bgColor);

        // سنستخدم FrameLayout لتمكين تراكب زر العين أسفل البطاقة
        FrameLayout frame = new FrameLayout(requireContext());
        frame.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // المحتوى الداخلي (داخل FrameLayout)
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        // نضيف مسافة سفلية إضافية لإتاحة مكان للزر أسفل المنتصف
        root.setPadding(dp(10), dp(10), dp(10), dp(38));
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // السطر 1: رقم الغرفة
        TextView tvRoom = new TextView(requireContext());
        tvRoom.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        tvRoom.setText("Room " + roomNumber);
        tvRoom.setTextSize(14);
        tvRoom.setTextColor(Color.parseColor("#333333"));
        tvRoom.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tvRoom.setTypeface(tvRoom.getTypeface(), android.graphics.Typeface.BOLD);

        // السطر 2: النوع
        TextView tvType = new TextView(requireContext());
        tvType.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        tvType.setText(roomType);
        tvType.setTextSize(12);
        tvType.setTextColor(Color.parseColor("#333333"));
        tvType.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tvType.setTypeface(tvType.getTypeface(), android.graphics.Typeface.BOLD);
        ((LinearLayout.LayoutParams) tvType.getLayoutParams()).topMargin = dp(1);

        // السطر 3: السعة
        TextView tvCapacity = new TextView(requireContext());
        tvCapacity.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        tvCapacity.setText(capacity);
        tvCapacity.setTextSize(11);
        tvCapacity.setTextColor(Color.parseColor("#666666"));
        tvCapacity.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        ((LinearLayout.LayoutParams) tvCapacity.getLayoutParams()).topMargin = dp(1);

        // السطر 4: حالة + مؤشر دائري
        LinearLayout statusWrap = new LinearLayout(requireContext());
        statusWrap.setOrientation(LinearLayout.VERTICAL);
        statusWrap.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        statusLp.topMargin = dp(8);
        statusWrap.setLayoutParams(statusLp);

        TextView tvStatus = new TextView(requireContext());
        tvStatus.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        tvStatus.setText(isAvailable ? "Available" : "غير متاحة");
        tvStatus.setTextSize(10);
        tvStatus.setTextColor(Color.parseColor(isAvailable ? "#4CAF50" : "#FF5722"));
        tvStatus.setTypeface(tvStatus.getTypeface(), android.graphics.Typeface.BOLD);
        tvStatus.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        TextView tvDot = new TextView(requireContext());
        tvDot.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        tvDot.setText(isAvailable ? "🟢" : "🔴");
        tvDot.setTextSize(12);
        ((LinearLayout.LayoutParams) tvDot.getLayoutParams()).topMargin = dp(1);

        statusWrap.addView(tvStatus);
        statusWrap.addView(tvDot);

        // تجميع العناصر
        root.addView(tvRoom);
        root.addView(tvType);
        root.addView(tvCapacity);
        root.addView(statusWrap);

        // إضافة المحتوى إلى الإطار
        frame.addView(root);

        // زر العين السفلي (شكل Pill) موحّد لكل البطاقات الديناميكية
        ImageButton eyeBtn = new ImageButton(requireContext());
        // Sleeker pill: wider and less tall
        FrameLayout.LayoutParams eyeLp = new FrameLayout.LayoutParams(dp(88), dp(36));
        eyeLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        eyeLp.setMargins(dp(8), dp(8), dp(8), dp(8));
        eyeBtn.setLayoutParams(eyeLp);
        eyeBtn.setBackgroundResource(R.drawable.eye_button_bg);
        // Match eye button fill with card background color
        android.graphics.drawable.Drawable eyeBg = eyeBtn.getBackground();
        if (eyeBg instanceof android.graphics.drawable.GradientDrawable) {
            ((android.graphics.drawable.GradientDrawable) eyeBg).setColor(bgColor);
        }
        eyeBtn.setPadding(dp(12), dp(8), dp(12), dp(8));
        eyeBtn.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        eyeBtn.setImageResource(android.R.drawable.ic_menu_view);
        // تلوين الأيقونة بالأسود كما طُلِب
        eyeBtn.setColorFilter(Color.BLACK);
        eyeBtn.setContentDescription("View details");
        eyeBtn.setClickable(true);
        eyeBtn.setFocusable(true);

        // فتح صفحة تفاصيل الغرفة البيضاء مع تمرير بيانات البطاقة
        eyeBtn.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(requireContext(), com.example.smarthotelapp.RoomDetailsActivity.class);
            intent.putExtra("roomNumber", roomNumber);
            intent.putExtra("roomType", roomType);
            intent.putExtra("capacity", capacity);
            intent.putExtra("status", status);
            startActivity(intent);
        });

        frame.addView(eyeBtn);

        // إضافة الإطار إلى البطاقة
        card.addView(frame);

        // نُسجل النوع على الوسم الافتراضي للبطاقة لسهولة التصفية بدون تعديل الواجهة
        card.setTag(roomType);

        return card;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    // Create a new horizontal row (HorizontalScrollView + LinearLayout) and add to roomsRowsContainer
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

        // Wrapper to keep vertical spacing consistent
        LinearLayout wrapper = new LinearLayout(requireContext());
        wrapper.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(hsv);

        if (roomsRowsContainer != null) roomsRowsContainer.addView(wrapper);
        return row;
    }
}
