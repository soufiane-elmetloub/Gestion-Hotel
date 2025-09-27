package com.example.smarthotelapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.smarthotelapp.adapters.NotificationsAdapter;
import com.example.smarthotelapp.models.Notification;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsActivity extends AppCompatActivity {
    
    private RecyclerView recyclerView;
    private NotificationsAdapter adapter;
    private List<Notification> notifications;
    private TextView emptyStateText;
    private ImageView emptyStateIcon;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.smarthotelapp.R.layout.activity_notifications);

        initializeViews();
        setupToolbar();
        sessionManager = new SessionManager(this);
        loadNotifications();
    }

    private void initializeViews() {
        recyclerView = findViewById(com.example.smarthotelapp.R.id.notificationsRecyclerView);
        emptyStateText = findViewById(com.example.smarthotelapp.R.id.emptyStateText);
        emptyStateIcon = findViewById(com.example.smarthotelapp.R.id.emptyStateIcon);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        notifications = new ArrayList<>();
        adapter = new NotificationsAdapter(this, notifications);
        recyclerView.setAdapter(adapter);

        // On click: mark as read
        adapter.setOnItemClickListener((notification, position) -> {
            String userIdStr = sessionManager != null ? sessionManager.getUserId() : null;
            int employeeId = -1;
            try { employeeId = Integer.parseInt(userIdStr != null ? userIdStr : "-1"); } catch (Exception ignored) {}
            if (employeeId <= 0) return;
            if (notification.isRead()) return; // already read
            markAsRead(notification.getId(), employeeId, position);
        });
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("الإشعارات");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void loadNotifications() {
        notifications.clear();
        adapter.notifyDataSetChanged();

        // Get employee ID from session
        String userIdStr = sessionManager != null ? sessionManager.getUserId() : null;
        int employeeId = -1;
        try { employeeId = Integer.parseInt(userIdStr != null ? userIdStr : "-1"); } catch (Exception ignored) {}
        if (employeeId <= 0) {
            showEmptyState(true, "No user session");
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = NetworkConfig.getNotificationsUrl(employeeId, false, 200);
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        boolean success = response.optBoolean("success", false);
                        if (!success) {
                            showEmptyState(true, "No notifications");
                            return;
                        }
                        JSONArray arr = response.optJSONArray("data");
                        if (arr == null || arr.length() == 0) {
                            showEmptyState(true, "No notifications");
                            return;
                        }
                        int unread = 0;
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.getJSONObject(i);
                            int id = o.optInt("id");
                            String title = o.optString("title", "");
                            String message = o.optString("message", "");
                            String createdAt = o.optString("created_at", "");
                            boolean isRead = o.optInt("is_read", 0) == 1;
                            String timeText = formatTime(createdAt);
                            Notification n = new Notification(id, title, message, timeText);
                            n.setRead(isRead);
                            notifications.add(n);
                            if (!isRead) unread++;
                        }
                        adapter.notifyDataSetChanged();
                        showEmptyState(notifications.isEmpty(), notifications.isEmpty()? "No notifications" : null);
                        updateToolbarCount(unread);
                    } catch (JSONException e) {
                        showEmptyState(true, "Parsing error");
                    }
                },
                error -> {
                    showEmptyState(true, "Network error");
                }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    private void markAsRead(int id, int employeeId, int position){
        try{
            JSONObject body = new JSONObject();
            body.put("id", id);
            body.put("employee_id", employeeId);
            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST,
                    NetworkConfig.getMarkNotificationReadUrl(),
                    body,
                    response -> {
                        // Optimistically update UI
                        if (position >=0 && position < notifications.size()){
                            Notification n = notifications.get(position);
                            n.setRead(true);
                            adapter.notifyItemChanged(position);
                            // recompute unread and update toolbar count
                            int unread = 0;
                            for (Notification item : notifications){ if (!item.isRead()) unread++; }
                            updateToolbarCount(unread);
                        }
                    },
                    error -> {
                        // ignore for now or show toast
                    }
            );
            VolleySingleton.getInstance(this).addToRequestQueue(req);
        }catch(JSONException ignored){}
    }

    private void updateToolbarCount(int unread){
        if (getSupportActionBar() == null) return;
        if (unread > 0){
            getSupportActionBar().setTitle("الإشعارات (" + unread + ")");
        } else {
            getSupportActionBar().setTitle("الإشعارات");
        }
    }

    private void showEmptyState(boolean empty, String msg){
        if (empty) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
            emptyStateIcon.setVisibility(View.VISIBLE);
            if (msg != null) emptyStateText.setText(msg);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
            emptyStateIcon.setVisibility(View.GONE);
        }
    }

    private String formatTime(String createdAt){
        // Attempt to parse 'YYYY-MM-DD HH:MM:SS' and return a simple display
        if (createdAt == null || createdAt.isEmpty()) return "";
        try{
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date d = sdf.parse(createdAt.replace('T',' '));
            if (d == null) return createdAt;
            SimpleDateFormat out = new SimpleDateFormat("dd/MM yyyy HH:mm", Locale.getDefault());
            return out.format(d);
        }catch (ParseException e){ return createdAt; }
    }

    private void updateEmptyState() {
        if (notifications.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
            emptyStateIcon.setVisibility(View.VISIBLE);
            emptyStateText.setText("لا توجد إشعارات حالياً");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
            emptyStateIcon.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
