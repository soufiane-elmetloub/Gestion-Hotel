package com.example.smarthotelapp;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

public class LowFragment extends Fragment {
    private LinearLayout listContainer;
    private RequestQueue requestQueue;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_low, container, false);
        listContainer = view.findViewById(R.id.reportListContainer);
        requestQueue = Volley.newRequestQueue(requireContext());
        sessionManager = new SessionManager(requireContext());
        loadReports();
        return view;
    }

    private void loadReports() {
        if (listContainer == null) return;
        listContainer.removeAllViews();

        String userId = sessionManager != null ? sessionManager.getUserId() : null;
        int employeeId = 0;
        try { if (userId != null && !userId.isEmpty()) employeeId = Integer.parseInt(userId); } catch (NumberFormatException ignored) { }
        if (employeeId <= 0) { addInfoText("No employee selected."); return; }

        String url = NetworkConfig.getBaseUrlWithSlash() + "backend/get_reports.php?employee_id=" + employeeId + "&priority=low";
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            boolean success = response.optBoolean("success", false);
                            if (!success) { addInfoText("No reports found."); return; }
                            JSONObject data = response.optJSONObject("data");
                            JSONArray arr = null;
                            if (data != null) arr = data.optJSONArray("reports");
                            if (arr == null) arr = response.optJSONArray("reports");
                            if (arr == null || arr.length() == 0) { addInfoText("No reports found."); return; }
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject item = arr.optJSONObject(i);
                                if (item != null) addReportCard(item);
                            }
                        } catch (Exception e) {
                            addInfoText("Failed to parse data.");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        addInfoText("Network error.");
                    }
                }
        );
        requestQueue.add(req);
    }

    private void addReportCard(JSONObject obj) {
        if (listContainer == null) return;
        View card = LayoutInflater.from(requireContext()).inflate(R.layout.item_report_card, listContainer, false);

        TextView title = card.findViewById(R.id.reportTitle);
        TextView subtitle = card.findViewById(R.id.reportSubtitle);
        TextView badge = card.findViewById(R.id.reportBadge);
        TextView desc = card.findViewById(R.id.reportDescription);
        TextView meta = card.findViewById(R.id.reportMeta);

        String roomNumber = obj.optString("room_number", "");
        if (roomNumber == null || roomNumber.isEmpty()) {
            roomNumber = String.valueOf(obj.optInt("room_id", 0));
        }
        String status = obj.optString("status", "open");
        String createdAt = obj.optString("created_at", "");

        title.setText(obj.optString("title", "Untitled"));
        subtitle.setText("Room " + roomNumber + " • " + status);
        desc.setText(obj.optString("description", ""));
        meta.setText(createdAt);

        // Style badge for low
        badge.setText("🟢 LOW");
        badge.setTextColor(0xFF388E3C);
        badge.setBackgroundColor(0xFFE8F5E9);

        listContainer.addView(card);
    }

    private void addInfoText(String text) {
        if (listContainer == null) return;
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(0xFF666666);
        tv.setTextSize(14);
        listContainer.addView(tv);
    }
}
