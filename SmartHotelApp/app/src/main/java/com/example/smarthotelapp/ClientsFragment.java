package com.example.smarthotelapp;

import com.example.smarthotelapp.models.Client;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ClientsFragment extends Fragment {
    
    private static final String TAG = "ClientsFragment";
    
    private TextView totalClientsCount;
    private TextView activeClientsCount;
    private TextView inactiveClientsCount;
    
    private MaterialButton btnAddClient;
    private MaterialButton btnClientsInfo;
    
    private RecyclerView recyclerViewClients;
    private ClientsAdapter clientsAdapter;
    private LinearLayout emptyStateLayout;
    
    private RequestQueue requestQueue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clients, container, false);
        
        // Initialize views
        totalClientsCount = view.findViewById(R.id.totalClientsCount);
        activeClientsCount = view.findViewById(R.id.activeClientsCount);
        inactiveClientsCount = view.findViewById(R.id.inactiveClientsCount);
        
        btnAddClient = view.findViewById(R.id.btnAddClient);
        btnClientsInfo = view.findViewById(R.id.btnClientsInfo);
        
        recyclerViewClients = view.findViewById(R.id.recyclerViewClients);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        
        // Initialize RecyclerView
        setupRecyclerView();
        
        // Initialize Volley request queue
        requestQueue = Volley.newRequestQueue(requireContext());
        
        // Setup button listeners
        setupButtonListeners();
        
        // Load data
        loadClientStats();
        loadRecentClients();
        
        return view;
    }
    
    private void loadClientStats() {
        String url = NetworkConfig.getClientsStatsUrl();
        Log.d(TAG, "Loading client stats from: " + url);
        
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            JSONObject data = response.getJSONObject("data");
                            
                            int total = data.getInt("total");
                            int active = data.getInt("active");
                            int inactive = data.getInt("inactive");
                            
                            // Update UI with animation
                            updateStatsWithAnimation(total, active, inactive);
                            
                            Log.d(TAG, "Client stats loaded successfully: Total=" + total + ", Active=" + active + ", Inactive=" + inactive);
                        } else {
                            String message = response.getString("message");
                            Log.e(TAG, "Failed to load client stats: " + message);
                            showError("Failed to load client statistics: " + message);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        showError("Data parsing error");
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Network error: " + error.getMessage());
                    showError("Server connection error");
                    
                    // Show default values on error
                    updateStatsWithAnimation(0, 0, 0);
                }
            }
        );
        
        requestQueue.add(request);
    }
    
    private void updateStatsWithAnimation(int total, int active, int inactive) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Animate the numbers
                    animateCounter(totalClientsCount, 0, total, 1000);
                    animateCounter(activeClientsCount, 0, active, 1200);
                    animateCounter(inactiveClientsCount, 0, inactive, 1400);
                }
            });
        }
    }
    
    private void animateCounter(TextView textView, int start, int end, int duration) {
        if (textView == null) return;
        
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(start, end);
        animator.setDuration(duration);
        animator.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(android.animation.ValueAnimator animation) {
                textView.setText(String.valueOf(animation.getAnimatedValue()));
            }
        });
        animator.start();
    }
    
    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupRecyclerView() {
        clientsAdapter = new ClientsAdapter(new ArrayList<>(), new ClientsAdapter.OnClientClickListener() {
            @Override
            public void onClientClick(Client client) {
                showClientDetails(client);
            }
            
            @Override
            public void onClientMenuClick(Client client, View view) {
                // Handle menu click
                showClientMenu(client, view);
            }
        });
        recyclerViewClients.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewClients.setAdapter(clientsAdapter);
    }
    
    private void setupButtonListeners() {
        btnAddClient.setOnClickListener(v -> {
            // Launch AddClientActivity
            Intent intent = new Intent(requireContext(), AddClientActivity.class);
            startActivity(intent);
        });
        
        btnClientsInfo.setOnClickListener(v -> {
            // Handle clients info button click
            showClientsInfo();
        });
    }
    
    private void loadRecentClients() {
        String url = NetworkConfig.getRecentClientsUrl();
        Log.d(TAG, "Loading recent clients from: " + url);
        
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            JSONArray clientsArray = response.getJSONArray("data");
                            List<Client> clients = parseClientsFromJson(clientsArray);
                            
                            // Update UI
                            updateClientsUI(clients);
                            
                            Log.d(TAG, "Recent clients loaded successfully: " + clients.size() + " clients");
                        } else {
                            String message = response.getString("message");
                            Log.e(TAG, "Failed to load recent clients: " + message);
                            showEmptyState();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        showEmptyState();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Network error loading clients: " + error.getMessage());
                    showEmptyState();
                }
            }
        );
        
        requestQueue.add(request);
    }
    
    private List<Client> parseClientsFromJson(JSONArray clientsArray) throws JSONException {
        List<Client> clients = new ArrayList<>();
        
        for (int i = 0; i < clientsArray.length(); i++) {
            JSONObject clientObj = clientsArray.getJSONObject(i);
            
            Client client = new Client();
            client.setId(clientObj.getInt("id"));
            client.setFirstName(clientObj.getString("first_name"));
            client.setLastName(clientObj.getString("last_name"));
            client.setPhone(clientObj.getString("phone"));
            client.setNationalId(clientObj.optString("national_id", ""));
            client.setEmail(clientObj.optString("email", ""));
            client.setCreatedAt(clientObj.getString("created_at"));
            client.setStatus(clientObj.optString("status", "نشط"));
            
            clients.add(client);
        }
        
        return clients;
    }
    
    private void updateClientsUI(List<Client> clients) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (clients.isEmpty()) {
                    showEmptyState();
                } else {
                    hideEmptyState();
                    clientsAdapter.setClients(clients);
                }
            });
        }
    }
    
    private void showEmptyState() {
        if (emptyStateLayout != null && recyclerViewClients != null) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerViewClients.setVisibility(View.GONE);
        }
    }
    
    private void hideEmptyState() {
        if (emptyStateLayout != null && recyclerViewClients != null) {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerViewClients.setVisibility(View.VISIBLE);
        }
    }
    
    private void showClientDetails(Client client) {
        // Show client details - for now just a toast
        String message = "Client: " + client.getFullName() + "\nPhone: " + client.getPhone();
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }
    
    private void showClientMenu(Client client, View view) {
        // Show client menu options
        Toast.makeText(getContext(), "Client menu: " + client.getFullName(), Toast.LENGTH_SHORT).show();
    }
    
    private void showClientsInfo() {
        // Open the Clients list screen
        if (getContext() != null) {
            Intent intent = new Intent(requireContext(), ClientsListActivity.class);
            startActivity(intent);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        loadClientStats();
        loadRecentClients();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }
}
