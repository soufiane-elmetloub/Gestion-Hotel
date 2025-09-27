package com.example.smarthotelapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

public class GuestListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_list);

        // Optional toolbar if present in theme
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Guest Details");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        TextView title = findViewById(R.id.tvTitle);
        ListView list = findViewById(R.id.listGuests);
        TextView tvGuestsCardTitle = findViewById(R.id.tvGuestsCardTitle);
        TextView tvAdultsCount = findViewById(R.id.tvAdultsCount);
        TextView tvChildrenCount = findViewById(R.id.tvChildrenCount);
        TextView tvInfantsCount = findViewById(R.id.tvInfantsCount);
        TextView tvTotalGuests = findViewById(R.id.tvTotalGuests);
        TextView tvTotalGuestsValue = findViewById(R.id.tvTotalGuestsValue);
        Button btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        ArrayList<String> firstNames = getIntent().getStringArrayListExtra("guest_first_names");
        ArrayList<String> lastNames = getIntent().getStringArrayListExtra("guest_last_names");
        ArrayList<String> combined = getIntent().getStringArrayListExtra("guest_names");
        int adults = getIntent().getIntExtra("adults_count", 0);
        int children = getIntent().getIntExtra("children_count", 0);
        int infants = getIntent().getIntExtra("infants_count", 0);

        boolean hasSeparated = firstNames != null && lastNames != null && firstNames.size() == lastNames.size() && !firstNames.isEmpty();
        if (hasSeparated) {
            // Use capsule adapter with unique colors and split lines
            GuestCapsuleAdapter capsuleAdapter = new GuestCapsuleAdapter(this, firstNames, lastNames);
            list.setAdapter(capsuleAdapter);

            // Handle delete with confirmation and update total
            capsuleAdapter.setOnDeleteClickListener((position, fName, lName) -> {
                new AlertDialog.Builder(this)
                        .setTitle("Remove guest")
                        .setMessage("Are you sure you want to remove this guest?\n" + fName + " " + lName)
                        .setPositiveButton("Remove", (d, w) -> {
                            capsuleAdapter.removeAt(position);
                            if (tvTotalGuestsValue != null) {
                                tvTotalGuestsValue.setText(String.valueOf(capsuleAdapter.getCount()));
                            }
                            Toast.makeText(this, "Guest removed", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            // Store adapter reference for result return
            list.setTag(capsuleAdapter);
        } else {
            // Fallback to a simple one-line list if combined names only
            List<String> rows = new ArrayList<>();
            if (combined != null && !combined.isEmpty()) {
                rows.addAll(combined);
            } else {
                rows.add("No guests available");
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows);
            list.setAdapter(adapter);
            list.setTag(null);
        }

        // Update statistics card UI
        int total = adults + children + infants;
        if (tvGuestsCardTitle != null) {
            // Keep title static; do not show total here.
            tvGuestsCardTitle.setText("Guests ");
        }
        if (tvAdultsCount != null) tvAdultsCount.setText(String.valueOf(adults));
        if (tvChildrenCount != null) tvChildrenCount.setText(String.valueOf(children));
        if (tvInfantsCount != null) tvInfantsCount.setText(String.valueOf(infants));
        if (tvTotalGuests != null) tvTotalGuests.setText("Total Guests");
        if (tvTotalGuests != null && tvTotalGuestsValue != null) {
            tvTotalGuestsValue.setText(String.valueOf(total));
        }
    }

    @Override
    public void onBackPressed() {
        ListView list = findViewById(R.id.listGuests);
        Intent result = new Intent();
        if (list != null && list.getTag() instanceof GuestCapsuleAdapter) {
            GuestCapsuleAdapter adapter = (GuestCapsuleAdapter) list.getTag();
            ArrayList<String> updatedFirsts = adapter.getFirstNames();
            ArrayList<String> updatedLasts = adapter.getLastNames();
            int total = adapter.getCount();

            // Build combined names list "First Last"
            ArrayList<String> combined = new ArrayList<>();
            for (int i = 0; i < updatedFirsts.size() && i < updatedLasts.size(); i++) {
                String f = updatedFirsts.get(i) == null ? "" : updatedFirsts.get(i);
                String l = updatedLasts.get(i) == null ? "" : updatedLasts.get(i);
                combined.add((f + " " + l).trim());
            }

            result.putStringArrayListExtra("guest_first_names", updatedFirsts);
            result.putStringArrayListExtra("guest_last_names", updatedLasts);
            result.putStringArrayListExtra("guest_names", combined);
            result.putExtra("selected_guest_count", total);
            setResult(RESULT_OK, result);
        } else {
            setResult(RESULT_CANCELED, result);
        }
        finish();
    }
}
