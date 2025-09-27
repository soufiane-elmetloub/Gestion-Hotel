package com.example.smarthotelapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class RoomDetailsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_details);

        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvNumber = findViewById(R.id.tvNumber);
        TextView tvType = findViewById(R.id.tvType);
        TextView tvCapacity = findViewById(R.id.tvCapacity);
        TextView tvStatus = findViewById(R.id.tvStatus);

        Intent intent = getIntent();
        String roomNumber = intent.getStringExtra("roomNumber");
        String roomType = intent.getStringExtra("roomType");
        String capacity = intent.getStringExtra("capacity");
        String status = intent.getStringExtra("status");

        tvTitle.setText("Room Details");
        tvNumber.setText("Room Number: " + (roomNumber != null ? roomNumber : "-"));
        tvType.setText("Type: " + (roomType != null ? roomType : "-"));
        tvCapacity.setText("Capacity: " + (capacity != null ? capacity : "-"));
        tvStatus.setText("Status: " + (status != null ? status : "-"));
    }
}
