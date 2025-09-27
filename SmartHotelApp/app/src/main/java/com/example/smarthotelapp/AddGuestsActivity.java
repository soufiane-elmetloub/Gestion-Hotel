package com.example.smarthotelapp;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.button.MaterialButton;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.smarthotelapp.models.Client;
import com.example.smarthotelapp.adapters.ClientSearchAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AddGuestsActivity extends AppCompatActivity {

    private ChipGroup chipGroupCategory;
    private Chip chipAdults, chipChildren, chipInfants;
    private TextView tvCategoryIcon;
    private TextInputLayout tilGuestSearch;
    private TextInputEditText etGuestSearch;
    private MaterialButton btnMinus, btnPlus, btnConfirmGuests;
    private TextView tvCurrentCount, tvAdultsCount, tvChildrenCount, tvInfantsCount, tvGuestsCardTitle;
    private LinearLayout groupAdults, groupChildren, groupInfants;
    private RecyclerView rvGuestSearchResults;
    private ClientSearchAdapter searchAdapter;

    // Search state
    private final Handler searchHandler = new Handler();
    private Runnable searchRunnable;
    private static final long SEARCH_DEBOUNCE_MS = 400;
    private static final String TAG = "AddGuestsActivity";
    private List<Client> searchResults = new ArrayList<>();
    // Batch entry state: how many remain to add for the current counter selection
    private int remainingToAdd = 0;
    private final List<Client> batchGuests = new ArrayList<>();

    private enum Category { ADULTS, CHILDREN, INFANTS }
    private Category selected = Category.ADULTS;

    private int adults = 0;
    private int children = 0;
    private int infants = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_guests);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Add Guests");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        // Bind views
        chipGroupCategory = findViewById(R.id.chipGroupCategory);
        chipAdults = findViewById(R.id.chipAdults);
        chipChildren = findViewById(R.id.chipChildren);
        chipInfants = findViewById(R.id.chipInfants);
        tvCategoryIcon = findViewById(R.id.tvCategoryIcon);
        tilGuestSearch = findViewById(R.id.tilGuestSearch);
        etGuestSearch = findViewById(R.id.etGuestSearch);
        // Optional controls (may be absent in layout)
        btnMinus = findViewById(R.id.btnMinus);
        btnPlus = findViewById(R.id.btnPlus);
        btnConfirmGuests = findViewById(R.id.btnConfirmGuests);
        tvCurrentCount = findViewById(R.id.tvCurrentCount);
        tvAdultsCount = findViewById(R.id.tvAdultsCount);
        tvChildrenCount = findViewById(R.id.tvChildrenCount);
        tvInfantsCount = findViewById(R.id.tvInfantsCount);
        tvGuestsCardTitle = findViewById(R.id.tvGuestsCardTitle);
        // New groups for inputs
        groupAdults = findViewById(R.id.groupAdults);
        groupChildren = findViewById(R.id.groupChildren);
        groupInfants = findViewById(R.id.groupInfants);

        // Setup RecyclerView for search results
        rvGuestSearchResults = findViewById(R.id.rvGuestSearchResults);
        if (rvGuestSearchResults != null) {
            rvGuestSearchResults.setLayoutManager(new LinearLayoutManager(this));
            searchAdapter = new ClientSearchAdapter(client -> {
                // On ✅ confirm: autofill and hide list
                autofillForCurrentCategory(client);
                rvGuestSearchResults.setVisibility(View.GONE);
                Toast.makeText(this, "Client selected", Toast.LENGTH_SHORT).show();
            });
            rvGuestSearchResults.setAdapter(searchAdapter);
        }

        // Initial UI update
        updateSelectedUI();
        updateCountsUI();

        // Chip selection listener
        chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipAdults) selected = Category.ADULTS;
            else if (id == R.id.chipChildren) selected = Category.CHILDREN;
            else if (id == R.id.chipInfants) selected = Category.INFANTS;
            updateSelectedUI();
        });

        // Plus/Minus listeners
        if (btnPlus != null) {
            btnPlus.setOnClickListener(v -> {
                switch (selected) {
                    case ADULTS: adults++; break;
                    case CHILDREN: children++; break;
                    case INFANTS: infants++; break;
                }
                updateCountsUI();
            });
        }
        if (btnMinus != null) {
            btnMinus.setOnClickListener(v -> {
                switch (selected) {
                    case ADULTS: if (adults > 0) adults--; break;
                    case CHILDREN: if (children > 0) children--; break;
                    case INFANTS: if (infants > 0) infants--; break;
                }
                updateCountsUI();
            });
        }

        // Debounced search on last name input (uses same endpoint as bookings)
        if (etGuestSearch != null) {
            etGuestSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                    searchRunnable = () -> performClientSearch(s.toString().trim());
                    searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_MS);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // Confirm button: batch-driven flow based on counter (e.g., 2 => add first, reset, add second, then finish)
        if (btnConfirmGuests != null) {
            btnConfirmGuests.setOnClickListener(v -> {
                // Determine target count based on the currently selected category counter
                int targetCountForThisCategory = getCurrentCategoryCount();

                if (remainingToAdd == 0) {
                    // Start a new batch if user has selected > 1 via counter
                    remainingToAdd = Math.max(targetCountForThisCategory, 0);
                    batchGuests.clear();
                }

                if (remainingToAdd <= 0) {
                    // No batch in progress and count is 0 -> keep previous fallback behavior
                    if (searchResults != null && !searchResults.isEmpty()) {
                        String[] items = new String[searchResults.size()];
                        for (int i = 0; i < searchResults.size(); i++) {
                            Client c = searchResults.get(i);
                            items[i] = c.getFirstName() + " " + c.getLastName() + (TextUtils.isEmpty(c.getNationalId()) ? "" : ("  (" + c.getNationalId() + ")"));
                        }
                        new AlertDialog.Builder(AddGuestsActivity.this)
                                .setTitle("Select client")
                                .setItems(items, (dialog, which) -> {
                                    Client c = searchResults.get(which);
                                    autofillForCurrentCategory(c);
                                    Toast.makeText(AddGuestsActivity.this, "Client selected", Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                        return;
                    }
                    Toast.makeText(AddGuestsActivity.this, "Please set counter and enter guest info", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Validate and construct a guest object from current inputs
                Client current = buildClientFromInputsForCurrentCategory();
                if (current == null) {
                    // Validation failed; message already shown
                    return;
                }

                // "Register" locally for now; backend integration can be added later
                batchGuests.add(current);
                remainingToAdd--;

                if (remainingToAdd > 0) {
                    // Prepare for next person of this batch
                    clearInputsForCurrentCategory();
                    Toast.makeText(AddGuestsActivity.this, remainingToAdd + " remaining...", Toast.LENGTH_SHORT).show();
                } else {
                    // Batch completed: return total count to the caller (booking screen)
                    int total = adults + children + infants;
                    if (total <= 0) {
                        // Fallback to the number actually added in this batch
                        total = batchGuests.size();
                    }
                    android.content.Intent data = new android.content.Intent();
                    data.putExtra("selected_guest_count", total);
                    // Also return breakdown counts for statistics card
                    data.putExtra("adults_count", adults);
                    data.putExtra("children_count", children);
                    data.putExtra("infants_count", infants);
                    // Also return the list of guest names added in this batch (combined and separate)
                    java.util.ArrayList<String> names = new java.util.ArrayList<>();
                    java.util.ArrayList<String> firstNames = new java.util.ArrayList<>();
                    java.util.ArrayList<String> lastNames = new java.util.ArrayList<>();
                    for (com.example.smarthotelapp.models.Client c : batchGuests) {
                        String f = (c.getFirstName() != null ? c.getFirstName() : "");
                        String l = (c.getLastName() != null ? c.getLastName() : "");
                        names.add(f + " " + l);
                        firstNames.add(f);
                        lastNames.add(l);
                    }
                    data.putStringArrayListExtra("guest_names", names);
                    data.putStringArrayListExtra("guest_first_names", firstNames);
                    data.putStringArrayListExtra("guest_last_names", lastNames);
                    setResult(RESULT_OK, data);
                    finish();
                }
            });
        }
    }

    private void updateSelectedUI() {
        String emoji;
        int current;
        String hint;
        switch (selected) {
            case CHILDREN:
                emoji = "👧";
                current = children;
                hint = "Search children...";
                break;
            case INFANTS:
                emoji = "👶";
                current = infants;
                hint = "Search infants...";
                break;
            case ADULTS:
            default:
                emoji = "👨";
                current = adults;
                hint = "Search adults...";
                break;
        }
        tvCategoryIcon.setText(emoji);
        if (tvCurrentCount != null) {
            tvCurrentCount.setText(String.valueOf(current));
        }
        if (!TextUtils.isEmpty(hint)) {
            tilGuestSearch.setHint(hint);
        }

        // Toggle visible group
        if (groupAdults != null && groupChildren != null && groupInfants != null) {
            switch (selected) {
                case ADULTS:
                    groupAdults.setVisibility(View.VISIBLE);
                    groupChildren.setVisibility(View.GONE);
                    groupInfants.setVisibility(View.GONE);
                    break;
                case CHILDREN:
                    groupAdults.setVisibility(View.GONE);
                    groupChildren.setVisibility(View.VISIBLE);
                    groupInfants.setVisibility(View.GONE);
                    break;
                case INFANTS:
                    groupAdults.setVisibility(View.GONE);
                    groupChildren.setVisibility(View.GONE);
                    groupInfants.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

    private void updateCountsUI() {
        tvAdultsCount.setText(String.valueOf(adults));
        tvChildrenCount.setText(String.valueOf(children));
        tvInfantsCount.setText(String.valueOf(infants));
        int total = adults + children + infants;
        tvGuestsCardTitle.setText("Guests 👨‍👩‍👧‍👦 (" + total + ")");
        // Also refresh the current selector count
        updateSelectedUI();
    }

    private int getCurrentCategoryCount() {
        switch (selected) {
            case ADULTS: return adults;
            case CHILDREN: return children;
            case INFANTS: return infants;
            default: return 0;
        }
    }

    private void performClientSearch(String query) {
        if (TextUtils.isEmpty(query) || query.length() < 2) {
            searchResults.clear();
            if (rvGuestSearchResults != null) {
                rvGuestSearchResults.setVisibility(View.GONE);
                if (searchAdapter != null) searchAdapter.setItems(new ArrayList<>());
            }
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
                new Response.Listener<org.json.JSONObject>() {
                    @Override
                    public void onResponse(org.json.JSONObject response) {
                        try {
                            boolean success = response.optBoolean("success", false);
                            searchResults.clear();
                            if (!success) {
                                if (rvGuestSearchResults != null) {
                                    rvGuestSearchResults.setVisibility(View.GONE);
                                    if (searchAdapter != null) searchAdapter.setItems(new ArrayList<>());
                                }
                                return;
                            }
                            JSONArray arr = response.optJSONArray("results");
                            if (arr != null) {
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject o = arr.getJSONObject(i);
                                    searchResults.add(new Client(
                                            o.optInt("id"),
                                            o.optString("first_name"),
                                            o.optString("last_name"),
                                            o.optString("phone"),
                                            o.optString("national_id")
                                    ));
                                }
                            }
                            if (rvGuestSearchResults != null) {
                                if (searchResults.isEmpty()) {
                                    rvGuestSearchResults.setVisibility(View.GONE);
                                } else {
                                    rvGuestSearchResults.setVisibility(View.VISIBLE);
                                }
                                if (searchAdapter != null) searchAdapter.setItems(searchResults);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Parse error", e);
                            if (rvGuestSearchResults != null) {
                                rvGuestSearchResults.setVisibility(View.GONE);
                                if (searchAdapter != null) searchAdapter.setItems(new ArrayList<>());
                            }
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Search error: " + error);
                if (rvGuestSearchResults != null) {
                    rvGuestSearchResults.setVisibility(View.GONE);
                    if (searchAdapter != null) searchAdapter.setItems(new ArrayList<>());
                }
            }
        });
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1.0f));
        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    private void autofillForCurrentCategory(Client c) {
        switch (selected) {
            case ADULTS: {
                TextInputEditText f = findViewById(R.id.etAdultFirstName);
                TextInputEditText l = findViewById(R.id.etAdultLastName);
                TextInputEditText ph = findViewById(R.id.etAdultPhone);
                TextInputEditText id = findViewById(R.id.etAdultIdNumber);
                if (f != null) f.setText(c.getFirstName());
                if (l != null) l.setText(c.getLastName());
                if (ph != null) ph.setText(c.getPhone());
                if (id != null) id.setText(c.getNationalId());
                break;
            }
            case CHILDREN: {
                TextInputEditText f = findViewById(R.id.etChildFirstName);
                TextInputEditText l = findViewById(R.id.etChildLastName);
                if (f != null) f.setText(c.getFirstName());
                if (l != null) l.setText(c.getLastName());
                break;
            }
            case INFANTS: {
                TextInputEditText f = findViewById(R.id.etInfantFirstName);
                TextInputEditText l = findViewById(R.id.etInfantLastName);
                if (f != null) f.setText(c.getFirstName());
                if (l != null) l.setText(c.getLastName());
                break;
            }
        }
    }

    // Build a Client object from visible inputs depending on selected category. Returns null if invalid.
    @Nullable
    private Client buildClientFromInputsForCurrentCategory() {
        switch (selected) {
            case ADULTS: {
                TextInputEditText f = findViewById(R.id.etAdultFirstName);
                TextInputEditText l = findViewById(R.id.etAdultLastName);
                TextInputEditText ph = findViewById(R.id.etAdultPhone);
                TextInputEditText id = findViewById(R.id.etAdultIdNumber);
                String first = f != null ? f.getText().toString().trim() : "";
                String last = l != null ? l.getText().toString().trim() : "";
                String phone = ph != null ? ph.getText().toString().trim() : "";
                String nid = id != null ? id.getText().toString().trim() : "";
                if (first.isEmpty() || last.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(this, "Please fill first, last and phone", Toast.LENGTH_SHORT).show();
                    return null;
                }
                return new Client(0, first, last, phone, nid);
            }
            case CHILDREN: {
                TextInputEditText f = findViewById(R.id.etChildFirstName);
                TextInputEditText l = findViewById(R.id.etChildLastName);
                String first = f != null ? f.getText().toString().trim() : "";
                String last = l != null ? l.getText().toString().trim() : "";
                if (first.isEmpty() || last.isEmpty()) {
                    Toast.makeText(this, "Please fill child first and last name", Toast.LENGTH_SHORT).show();
                    return null;
                }
                return new Client(0, first, last, "", "");
            }
            case INFANTS: {
                TextInputEditText f = findViewById(R.id.etInfantFirstName);
                TextInputEditText l = findViewById(R.id.etInfantLastName);
                String first = f != null ? f.getText().toString().trim() : "";
                String last = l != null ? l.getText().toString().trim() : "";
                if (first.isEmpty() || last.isEmpty()) {
                    Toast.makeText(this, "Please fill infant first and last name", Toast.LENGTH_SHORT).show();
                    return null;
                }
                return new Client(0, first, last, "", "");
            }
        }
        return null;
    }

    // Clear inputs for the current category to prepare for the next person
    private void clearInputsForCurrentCategory() {
        switch (selected) {
            case ADULTS: {
                TextInputEditText f = findViewById(R.id.etAdultFirstName);
                TextInputEditText l = findViewById(R.id.etAdultLastName);
                TextInputEditText ph = findViewById(R.id.etAdultPhone);
                TextInputEditText id = findViewById(R.id.etAdultIdNumber);
                if (f != null) f.setText("");
                if (l != null) l.setText("");
                if (ph != null) ph.setText("");
                if (id != null) id.setText("");
                break;
            }
            case CHILDREN: {
                TextInputEditText f = findViewById(R.id.etChildFirstName);
                TextInputEditText l = findViewById(R.id.etChildLastName);
                if (f != null) f.setText("");
                if (l != null) l.setText("");
                break;
            }
            case INFANTS: {
                TextInputEditText f = findViewById(R.id.etInfantFirstName);
                TextInputEditText l = findViewById(R.id.etInfantLastName);
                if (f != null) f.setText("");
                if (l != null) l.setText("");
                break;
            }
        }
    }
}
