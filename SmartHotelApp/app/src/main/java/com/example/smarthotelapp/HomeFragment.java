package com.example.smarthotelapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private static final String REQ_TAG = "HOME_STATS";
    private TextView totalRoomsText;
    private TextView totalClientsText;
    private TextView totalBookingsText;
    private RequestQueue requestQueue;
    private SessionManager sessionManager;
    private TextView urgentBadge;
    private TextView mediumBadge;
    private TextView lowBadge;
    
    // Tasks components
    private RecyclerView tasksRecyclerView;
    private LinearLayout emptyStateLayout;
    private TasksAdapter tasksAdapter;
    private List<Task> tasksList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        // Init RequestQueue
        requestQueue = Volley.newRequestQueue(requireContext());
        // Init session manager
        sessionManager = new SessionManager(requireContext());

        // Bind stats TextViews
        totalRoomsText = view.findViewById(R.id.totalRoomsText);
        totalClientsText = view.findViewById(R.id.totalClientsText);
        totalBookingsText = view.findViewById(R.id.totalBookingsText);
        // Badges for unread counts
        urgentBadge = view.findViewById(R.id.urgentBadge);
        mediumBadge = view.findViewById(R.id.mediumBadge);
        lowBadge = view.findViewById(R.id.lowBadge);
        
        // Initialize tasks components
        tasksRecyclerView = view.findViewById(R.id.tasksRecyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        
        // Setup RecyclerView
        tasksList = new ArrayList<>();
        tasksAdapter = new TasksAdapter(requireContext(), tasksList);
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        tasksRecyclerView.setAdapter(tasksAdapter);

        // Set placeholders while loading
        setTextSafe(totalRoomsText, "...");
        setTextSafe(totalClientsText, "...");
        setTextSafe(totalBookingsText, "...");

        // Load dynamic stats
        loadDashboardStats();
        // Load unread report counts (badges)
        fetchUnreadCounts();
        // Load employee tasks
        loadEmployeeTasks();

        // Urgent requests navigation
        View emergencyCard = view.findViewById(R.id.emergencyServiceTab);
        emergencyCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, new UrgentFragment());
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });
        
        // Medium priority requests navigation
        View mediumCard = view.findViewById(R.id.mediumPriorityTab);
        mediumCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, new MediumFragment());
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });
        
        // Low priority requests navigation
        View lowCard = view.findViewById(R.id.lowPriorityTab);
        lowCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, new LowFragment());
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });
        
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancel pending requests to avoid leaks
        if (requestQueue != null) {
            requestQueue.cancelAll(REQ_TAG);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh unread counts whenever returning to home
        fetchUnreadCounts();
    }

    private void loadDashboardStats() {
        fetchClientsTotal();
        fetchBookingsTotal();
        fetchRoomsTotal();
    }

    private void fetchUnreadCounts() {
        String userId = sessionManager != null ? sessionManager.getUserId() : null;
        int employeeId = 0;
        try { if (userId != null && !userId.isEmpty() && !"-1".equals(userId)) employeeId = Integer.parseInt(userId); } catch (NumberFormatException ignored) { }
        if (employeeId <= 0) { setBadge(urgentBadge, 0); setBadge(mediumBadge, 0); setBadge(lowBadge, 0); return; }

        String url = NetworkConfig.getUnreadReportCountsUrl(employeeId);
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            boolean success = response.optBoolean("success", false);
                            if (!success) { setBadge(urgentBadge, 0); setBadge(mediumBadge, 0); setBadge(lowBadge, 0); return; }
                            JSONObject data = response.optJSONObject("data");
                            int u = 0, m = 0, l = 0;
                            if (data != null) {
                                u = data.optInt("urgent", 0);
                                m = data.optInt("medium", 0);
                                l = data.optInt("low", 0);
                            }
                            setBadge(urgentBadge, u);
                            setBadge(mediumBadge, m);
                            setBadge(lowBadge, l);
                        } catch (Exception e) {
                            setBadge(urgentBadge, 0);
                            setBadge(mediumBadge, 0);
                            setBadge(lowBadge, 0);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        setBadge(urgentBadge, 0);
                        setBadge(mediumBadge, 0);
                        setBadge(lowBadge, 0);
                    }
                }
        );
        req.setTag(REQ_TAG);
        requestQueue.add(req);
    }

    private void setBadge(TextView tv, int count) {
        if (tv == null) return;
        if (count > 0) {
            tv.setVisibility(View.VISIBLE);
            tv.setText(String.valueOf(count));
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    private void fetchClientsTotal() {
        String url = NetworkConfig.getClientsStatsUrl();
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            boolean success = response.optBoolean("success", false);
                            if (success) {
                                JSONObject stats = response.optJSONObject("data");
                                if (stats == null) stats = response.optJSONObject("stats");
                                int total = 0;
                                if (stats != null) {
                                    total = stats.optInt("total", 0);
                                } else {
                                    total = response.optInt("total", 0);
                                }
                                setTextSafe(totalClientsText, formatNumber(total));
                            } else {
                                setTextSafe(totalClientsText, "0");
                            }
                        } catch (Exception e) {
                            setTextSafe(totalClientsText, "0");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        setTextSafe(totalClientsText, "0");
                    }
                }
        );
        // Tag for lifecycle cancellation
        req.setTag(REQ_TAG);
        requestQueue.add(req);
    }

    private void fetchBookingsTotal() {
        String url = NetworkConfig.getBookingStatsUrl();
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            boolean success = response.optBoolean("success", false);
                            if (success) {
                                JSONObject data = response.optJSONObject("data");
                                int totalBookings = 0;
                                if (data != null) {
                                    totalBookings = data.optInt("total_bookings", 0);
                                } else {
                                    totalBookings = response.optInt("total_bookings", 0);
                                }
                                setTextSafe(totalBookingsText, formatNumber(totalBookings));
                            } else {
                                setTextSafe(totalBookingsText, "0");
                            }
                        } catch (Exception e) {
                            setTextSafe(totalBookingsText, "0");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        setTextSafe(totalBookingsText, "0");
                    }
                }
        );
        req.setTag(REQ_TAG);
        requestQueue.add(req);
    }

    private void fetchRoomsTotal() {
        String url = NetworkConfig.getRoomsUrl();
        // Append filtering so the count reflects only rooms assigned to the logged-in employee/section
        try {
            String userId = sessionManager != null ? sessionManager.getUserId() : null;
            String section = sessionManager != null ? sessionManager.getAssignedSection() : null;

            StringBuilder sb = new StringBuilder(url);
            boolean first = !url.contains("?");

            if (userId != null && !userId.isEmpty() && !"-1".equals(userId)) {
                sb.append(first ? '?' : '&').append("employee_id=").append(URLEncoder.encode(userId, "UTF-8"));
                first = false;
            }
            if (section != null && !section.isEmpty() && !section.equalsIgnoreCase("Not Assigned")) {
                sb.append(first ? '?' : '&').append("section=").append(URLEncoder.encode(section, "UTF-8"));
            }
            url = sb.toString();
        } catch (UnsupportedEncodingException ignored) { }
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            boolean success = response.optBoolean("success", false);
                            if (success) {
                                // API may return rooms or data as array
                                JSONArray rooms = response.optJSONArray("rooms");
                                if (rooms == null) rooms = response.optJSONArray("data");
                                int total = rooms != null ? rooms.length() : 0;
                                setTextSafe(totalRoomsText, formatNumber(total));
                            } else {
                                setTextSafe(totalRoomsText, "0");
                            }
                        } catch (Exception e) {
                            setTextSafe(totalRoomsText, "0");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        setTextSafe(totalRoomsText, "0");
                    }
                }
        );
        req.setTag(REQ_TAG);
        requestQueue.add(req);
    }

    private void setTextSafe(TextView tv, String text) {
        if (tv == null) return;
        tv.setText(text);
    }

    private String formatNumber(int n) {
        return String.format(java.util.Locale.getDefault(), "%,d", n);
    }

    private void loadEmployeeTasks() {
        String userId = sessionManager != null ? sessionManager.getUserId() : null;
        int employeeId = 0;
        try {
            if (userId != null && !userId.isEmpty() && !"-1".equals(userId)) {
                employeeId = Integer.parseInt(userId);
            }
        } catch (NumberFormatException ignored) {}
        
        if (employeeId <= 0) {
            showEmptyState();
            return;
        }

        String url = NetworkConfig.getEmployeeTasksUrl() + "?employee_id=" + employeeId;
        
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            boolean success = response.optBoolean("success", false);
                            if (success) {
                                JSONArray tasksArray = response.optJSONArray("tasks");
                                if (tasksArray != null && tasksArray.length() > 0) {
                                    List<Task> tasks = new ArrayList<>();
                                    for (int i = 0; i < tasksArray.length(); i++) {
                                        JSONObject taskObj = tasksArray.getJSONObject(i);
                                        Task task = new Task();
                                        task.setId(taskObj.optInt("id", 0));
                                        task.setTitle(taskObj.optString("title", ""));
                                        task.setDescription(taskObj.optString("description", ""));
                                        task.setStatus(taskObj.optString("status", "pending"));
                                        task.setCreatedAt(taskObj.optString("created_at", ""));
                                        task.setUpdatedAt(taskObj.optString("updated_at", ""));
                                        task.setStatusText(taskObj.optString("status_text", ""));
                                        task.setStatusColor(taskObj.optString("status_color", "#9E9E9E"));
                                        task.setPriorityLevel(taskObj.optString("priority_level", "medium"));
                                        tasks.add(task);
                                    }
                                    updateTasksList(tasks);
                                } else {
                                    showEmptyState();
                                }
                            } else {
                                showEmptyState();
                                String message = response.optString("message", "خطأ في تحميل المهام");
                                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            showEmptyState();
                            Toast.makeText(getContext(), "خطأ في معالجة البيانات", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showEmptyState();
                        Toast.makeText(getContext(), "خطأ في الاتصال بالخادم", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        
        req.setTag(REQ_TAG);
        requestQueue.add(req);
    }

    private void updateTasksList(List<Task> tasks) {
        if (tasks != null && !tasks.isEmpty()) {
            tasksList.clear();
            tasksList.addAll(tasks);
            tasksAdapter.updateTasks(tasksList);
            
            // Show RecyclerView, hide empty state
            tasksRecyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        } else {
            showEmptyState();
        }
    }

    private void showEmptyState() {
        tasksRecyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
        tasksList.clear();
        tasksAdapter.updateTasks(tasksList);
    }
}
