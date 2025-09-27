package com.example.smarthotelapp;

import com.example.smarthotelapp.models.Client;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ClientsListActivity extends AppCompatActivity {
    
    private static final String TAG = "ClientsListActivity";
    
    private RecyclerView clientsRecyclerView;
    private ClientsAdapter clientsAdapter;
    private LinearLayout emptyStateLayout;
    private TextView clientsCountText;
    private TextInputEditText searchEditText;
    private MaterialButton filterButton, sortButton;
    private FloatingActionButton addClientFab;
    
    private RequestQueue requestQueue;
    private List<Client> allClients = new ArrayList<>();
    private List<Client> filteredClients = new ArrayList<>();
    
    // Filter and Sort options
    private StatusFilter currentStatusFilter = StatusFilter.ALL;
    private SortOption currentSortOption = SortOption.NAME_ASC;
    
    public enum StatusFilter {
        ALL("الكل"),
        ACTIVE("نشط"),
        INACTIVE("غير نشط"),
        PENDING("في الانتظار");
        
        private final String displayName;
        
        StatusFilter(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum SortOption {
        NAME_ASC("الاسم (أ-ي)"),
        NAME_DESC("الاسم (ي-أ)"),
        DATE_ASC("التاريخ (الأقدم)"),
        DATE_DESC("التاريخ (الأحدث)");
        
        private final String displayName;
        
        SortOption(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clients_list);
        
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        
        // Initialize Volley
        requestQueue = Volley.newRequestQueue(this);
        
        // Load clients data
        loadClients();
    }
    
    private void initViews() {
        clientsRecyclerView = findViewById(R.id.clientsRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        clientsCountText = findViewById(R.id.clientsCountText);
        searchEditText = findViewById(R.id.searchEditText);
        filterButton = findViewById(R.id.filterButton);
        sortButton = findViewById(R.id.sortButton);
        addClientFab = findViewById(R.id.addClientFab);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }
    
    private void setupRecyclerView() {
        clientsAdapter = new ClientsAdapter(filteredClients, new ClientsAdapter.OnClientClickListener() {
            @Override
            public void onClientClick(Client client) {
                showClientDetails(client);
            }
            
            @Override
            public void onClientMenuClick(Client client, View view) {
                showClientMenu(client, view);
            }
        });
        
        clientsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        clientsRecyclerView.setAdapter(clientsAdapter);
    }
    
    private void setupListeners() {
        // Search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterClients(s.toString());
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Filter button
        filterButton.setOnClickListener(v -> showFilterDialog());
        
        // Sort button
        sortButton.setOnClickListener(v -> showSortDialog());
        
        // Add client FAB
        addClientFab.setOnClickListener(v -> {
            Intent intent = new Intent(ClientsListActivity.this, AddClientActivity.class);
            startActivity(intent);
        });
    }
    
    private void loadClients() {
        String url = NetworkConfig.getAllClientsUrl();
        Log.d(TAG, "Loading clients from: " + url);
        
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
                            allClients = parseClientsFromJson(clientsArray);
                            
                            // Apply current filters and sorting
                            applyFiltersAndSort();
                            
                            Log.d(TAG, "Clients loaded successfully: " + allClients.size() + " clients");
                        } else {
                            String message = response.getString("message");
                            Log.e(TAG, "Failed to load clients: " + message);
                            showError("فشل في تحميل العملاء: " + message);
                            showEmptyState();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        showError("خطأ في معالجة البيانات");
                        showEmptyState();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Network error: " + error.getMessage());
                    showError("خطأ في الاتصال بالخادم");
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
    
    private void filterClients(String searchQuery) {
        filteredClients.clear();
        
        for (Client client : allClients) {
            boolean matchesSearch = searchQuery.isEmpty() || 
                client.getFullName().toLowerCase().contains(searchQuery.toLowerCase()) ||
                client.getPhone().contains(searchQuery) ||
                client.getNationalId().contains(searchQuery);
            
            boolean matchesFilter = currentStatusFilter == StatusFilter.ALL ||
                client.getStatus().equals(currentStatusFilter.getDisplayName());
            
            if (matchesSearch && matchesFilter) {
                filteredClients.add(client);
            }
        }
        
        sortClients();
        updateUI();
    }
    
    private void applyFiltersAndSort() {
        filterClients(searchEditText.getText().toString());
    }
    
    private void sortClients() {
        Collections.sort(filteredClients, new Comparator<Client>() {
            @Override
            public int compare(Client c1, Client c2) {
                switch (currentSortOption) {
                    case NAME_ASC:
                        return c1.getFullName().compareToIgnoreCase(c2.getFullName());
                    case NAME_DESC:
                        return c2.getFullName().compareToIgnoreCase(c1.getFullName());
                    case DATE_ASC:
                        return c1.getCreatedAt().compareTo(c2.getCreatedAt());
                    case DATE_DESC:
                        return c2.getCreatedAt().compareTo(c1.getCreatedAt());
                    default:
                        return 0;
                }
            }
        });
    }
    
    private void updateUI() {
        if (filteredClients.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
            clientsAdapter.notifyDataSetChanged();
        }
        
        // Update count
        String countText = filteredClients.size() + " عميل";
        clientsCountText.setText(countText);
    }
    
    private void showEmptyState() {
        emptyStateLayout.setVisibility(View.VISIBLE);
        clientsRecyclerView.setVisibility(View.GONE);
    }
    
    private void hideEmptyState() {
        emptyStateLayout.setVisibility(View.GONE);
        clientsRecyclerView.setVisibility(View.VISIBLE);
    }
    
    private void showClientDetails(Client client) {
        // Show client details - for now just a toast
        String message = "العميل: " + client.getFullName() + 
                        "\nالجوال: " + client.getPhone() +
                        "\nالحالة: " + client.getStatus();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    
    private void showClientMenu(Client client, View view) {
        // Show popup menu for client actions
        Toast.makeText(this, "قائمة العميل: " + client.getFullName(), Toast.LENGTH_SHORT).show();
    }
    
    private void showFilterDialog() {
        // Show filter options dialog
        String[] filterOptions = new String[StatusFilter.values().length];
        for (int i = 0; i < StatusFilter.values().length; i++) {
            filterOptions[i] = StatusFilter.values()[i].getDisplayName();
        }
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("تصفية العملاء")
               .setSingleChoiceItems(filterOptions, currentStatusFilter.ordinal(), 
                   (dialog, which) -> {
                       currentStatusFilter = StatusFilter.values()[which];
                       applyFiltersAndSort();
                       dialog.dismiss();
                   })
               .setNegativeButton("إلغاء", null)
               .show();
    }
    
    private void showSortDialog() {
        // Show sort options dialog
        String[] sortOptions = new String[SortOption.values().length];
        for (int i = 0; i < SortOption.values().length; i++) {
            sortOptions[i] = SortOption.values()[i].getDisplayName();
        }
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("ترتيب العملاء")
               .setSingleChoiceItems(sortOptions, currentSortOption.ordinal(), 
                   (dialog, which) -> {
                       currentSortOption = SortOption.values()[which];
                       applyFiltersAndSort();
                       dialog.dismiss();
                   })
               .setNegativeButton("إلغاء", null)
               .show();
    }
    
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        loadClients();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }
}
