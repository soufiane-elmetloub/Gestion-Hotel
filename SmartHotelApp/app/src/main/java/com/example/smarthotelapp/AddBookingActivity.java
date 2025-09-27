package com.example.smarthotelapp;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.DefaultRetryPolicy;
import com.example.smarthotelapp.adapters.ClientSearchAdapter;
import com.example.smarthotelapp.models.Client;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AddBookingActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etPhone, etNationalId;
    private TextInputEditText etSearch;
    private RecyclerView rvSearchResults;
    private ClientSearchAdapter searchAdapter;
    private Button btnNextStep, btnCancel, btnAddGuests;
    private android.widget.TextView tvGuestsAddedBadge;
    private ArrayList<String> addedGuestNames = new ArrayList<>();
    private ArrayList<String> guestFirstNames = new ArrayList<>();
    private ArrayList<String> guestLastNames = new ArrayList<>();
    private int adultsCount = 0;
    private int childrenCount = 0;
    private int infantsCount = 0;

    private static final int REQ_ADD_GUESTS = 101;
    private static final int REQ_VIEW_GUESTS = 102;

    private final Handler searchHandler = new Handler();
    private Runnable searchRunnable;
    private static final long SEARCH_DEBOUNCE_MS = 400;
    private static final String TAG = "AddBookingActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_booking);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Add Booking");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        // Init views
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etPhone = findViewById(R.id.etPhone);
        etNationalId = findViewById(R.id.etNationalId);
        btnNextStep = findViewById(R.id.btnNextStep);
        btnCancel = findViewById(R.id.btnCancel);
        btnAddGuests = findViewById(R.id.btnAddGuests);
        tvGuestsAddedBadge = findViewById(R.id.tvGuestsAddedBadge);
        etSearch = findViewById(R.id.etSearch);
        rvSearchResults = findViewById(R.id.rvSearchResults);

        // Setup RecyclerView
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new ClientSearchAdapter(client -> {
            // On per-item ✅ confirmation: fill fields and hide list
            etFirstName.setText(client.getFirstName());
            etLastName.setText(client.getLastName());
            etPhone.setText(client.getPhone());
            etNationalId.setText(client.getNationalId());
            rvSearchResults.setVisibility(View.GONE);
            Toast.makeText(this, "Client selected", Toast.LENGTH_SHORT).show();
            // Persist selected client id for later steps
            try {
                BookingSession.setClientId(client.getId());
            } catch (Exception ignored) {}
            // Enable Add Guests now that a client is selected
            updateAddGuestsEnabled();
        });
        rvSearchResults.setAdapter(searchAdapter);

        // Debounced search on last name input
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                    searchRunnable = () -> performClientSearch(s.toString().trim());
                    searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_MS);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // Initially disable Add Guests until main client is set
        if (btnAddGuests != null) {
            btnAddGuests.setEnabled(false);
            btnAddGuests.setAlpha(0.5f);
        }

        // When clicking the green badge, open the details page with guest names if available
        if (tvGuestsAddedBadge != null) {
            tvGuestsAddedBadge.setOnClickListener(v -> {
                if ((guestFirstNames != null && !guestFirstNames.isEmpty()) ||
                        (addedGuestNames != null && !addedGuestNames.isEmpty())) {
                    Intent intent = new Intent(AddBookingActivity.this, GuestListActivity.class);
                    if (guestFirstNames != null) intent.putStringArrayListExtra("guest_first_names", guestFirstNames);
                    if (guestLastNames != null) intent.putStringArrayListExtra("guest_last_names", guestLastNames);
                    if (addedGuestNames != null) intent.putStringArrayListExtra("guest_names", addedGuestNames);
                    intent.putExtra("adults_count", adultsCount);
                    intent.putExtra("children_count", childrenCount);
                    intent.putExtra("infants_count", infantsCount);
                    startActivityForResult(intent, REQ_VIEW_GUESTS);
                }
            });
        }

        // Watch required fields to toggle Add Guests availability
        TextWatcher requiredWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateAddGuestsEnabled();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        etFirstName.addTextChangedListener(requiredWatcher);
        etLastName.addTextChangedListener(requiredWatcher);
        etPhone.addTextChangedListener(requiredWatcher);

        // Next Step button
        btnNextStep.setOnClickListener(v -> {
            // Simple validation (room removed)
            String first = etFirstName.getText().toString().trim();
            String last = etLastName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

            if (first.isEmpty() || last.isEmpty() || phone.isEmpty()) {
                Toast.makeText(AddBookingActivity.this, "Please fill required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Launch the next step screen, pass main visitor name
            Intent intent = new Intent(AddBookingActivity.this, NextStepActivity.class);
            intent.putExtra("first_name", first);
            intent.putExtra("last_name", last);
            startActivity(intent);
        });

        // Cancel button
        btnCancel.setOnClickListener(v -> finish());

        // Add Guests button -> open AddGuestsActivity
        if (btnAddGuests != null) {
            btnAddGuests.setOnClickListener(v -> {
                Intent intent = new Intent(AddBookingActivity.this, AddGuestsActivity.class);
                startActivityForResult(intent, REQ_ADD_GUESTS);
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ADD_GUESTS && resultCode == RESULT_OK && data != null) {
            int count = data.getIntExtra("selected_guest_count", 0);
            ArrayList<String> names = data.getStringArrayListExtra("guest_names");
            ArrayList<String> firsts = data.getStringArrayListExtra("guest_first_names");
            ArrayList<String> lasts = data.getStringArrayListExtra("guest_last_names");
            if (names != null) {
                addedGuestNames = names;
            } else {
                addedGuestNames = new ArrayList<>();
            }
            guestFirstNames = firsts != null ? firsts : new ArrayList<>();
            guestLastNames = lasts != null ? lasts : new ArrayList<>();
            adultsCount = data.getIntExtra("adults_count", 0);
            childrenCount = data.getIntExtra("children_count", 0);
            infantsCount = data.getIntExtra("infants_count", 0);
            // Persist globally for pricing/validation later
            BookingSession.setCounts(adultsCount, childrenCount, infantsCount);
            if (count > 0 && tvGuestsAddedBadge != null) {
                tvGuestsAddedBadge.setText("Added " + count + " guest" + (count > 1 ? "s" : ""));
                tvGuestsAddedBadge.setVisibility(View.VISIBLE);
            }
        } else if (requestCode == REQ_VIEW_GUESTS && resultCode == RESULT_OK && data != null) {
            // Returned from GuestListActivity with updated lists after potential deletions
            ArrayList<String> firsts = data.getStringArrayListExtra("guest_first_names");
            ArrayList<String> lasts = data.getStringArrayListExtra("guest_last_names");
            ArrayList<String> combined = data.getStringArrayListExtra("guest_names");
            int count = data.getIntExtra("selected_guest_count", -1);

            if (firsts != null) guestFirstNames = firsts; else guestFirstNames = new ArrayList<>();
            if (lasts != null) guestLastNames = lasts; else guestLastNames = new ArrayList<>();
            if (combined != null) addedGuestNames = combined; else addedGuestNames = new ArrayList<>();

            // Update badge strictly from returned count (fallback to list size if missing)
            int newCount = count >= 0 ? count : Math.max(guestFirstNames.size(), addedGuestNames.size());
            if (tvGuestsAddedBadge != null) {
                if (newCount > 0) {
                    tvGuestsAddedBadge.setText("Added " + newCount + " guest" + (newCount > 1 ? "s" : ""));
                    tvGuestsAddedBadge.setVisibility(View.VISIBLE);
                } else {
                    tvGuestsAddedBadge.setText("");
                    tvGuestsAddedBadge.setVisibility(View.GONE);
                }
            }
            // Keep existing counts unless explicitly returned; recompute conservative total if needed
            // Here we don't have per-category counts returned from GuestListActivity, so we keep previous ones
            BookingSession.setCounts(adultsCount, childrenCount, infantsCount);
        }
    }

    // Enable Add Guests only when a main client has been provided (first name, last name, and phone are filled)
    private void updateAddGuestsEnabled() {
        boolean hasMain = etFirstName != null && etLastName != null && etPhone != null
                && !etFirstName.getText().toString().trim().isEmpty()
                && !etLastName.getText().toString().trim().isEmpty()
                && !etPhone.getText().toString().trim().isEmpty();
        if (btnAddGuests != null) {
            btnAddGuests.setEnabled(hasMain);
            btnAddGuests.setAlpha(hasMain ? 1f : 0.5f);
        }
    }

    private void performClientSearch(String query) {
        if (query.length() < 2) {
            rvSearchResults.setVisibility(View.GONE);
            searchAdapter.setItems(new ArrayList<>());
            return;
        }

        String url = android.net.Uri.parse(NetworkConfig.getSearchClientUrl())
                .buildUpon()
                .appendQueryParameter("last_name", query)
                .build()
                .toString();
        Log.d(TAG, "Search URL: " + url);
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            boolean success = response.optBoolean("success", false);
                            if (!success) {
                                rvSearchResults.setVisibility(View.GONE);
                                searchAdapter.setItems(new ArrayList<>());
                                return;
                            }
                            JSONArray arr = response.optJSONArray("results");
                            List<Client> list = new ArrayList<>();
                            if (arr != null) {
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject o = arr.getJSONObject(i);
                                    list.add(new Client(
                                            o.optInt("id"),
                                            o.optString("first_name"),
                                            o.optString("last_name"),
                                            o.optString("phone"),
                                            o.optString("national_id")
                                    ));
                                }
                            }
                            if (list.isEmpty()) {
                                rvSearchResults.setVisibility(View.GONE);
                            } else {
                                rvSearchResults.setVisibility(View.VISIBLE);
                            }
                            searchAdapter.setItems(list);
                        } catch (JSONException e) {
                            rvSearchResults.setVisibility(View.GONE);
                            Toast.makeText(AddBookingActivity.this, "Parse error", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                rvSearchResults.setVisibility(View.GONE);
                Log.e(TAG, "Search error: " + error);
                Toast.makeText(AddBookingActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1.0f));
        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }
}
