package com.example.smarthotelapp;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class RoomDetailsAdapter extends RecyclerView.Adapter<RoomDetailsAdapter.VH> {

    private final Context context;
    private final List<RoomDetail> items = new ArrayList<>();
    private OnItemClickListener onItemClickListener;
    private OnConfirmClickListener onConfirmClickListener;

    public interface OnItemClickListener {
        void onItemClick(RoomDetail item);
    }

    public interface OnConfirmClickListener {
        void onConfirmClick(RoomDetail item);
    }

    public RoomDetailsAdapter(Context context) {
        this.context = context;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnConfirmClickListener(OnConfirmClickListener listener) {
        this.onConfirmClickListener = listener;
    }

    public void submitList(List<RoomDetail> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_room_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        RoomDetail it = items.get(position);
        h.tvRoomNumber.setText("Room " + (it.roomNumber == null || it.roomNumber.isEmpty() ? ("#" + it.roomId) : it.roomNumber));
        h.tvStatus.setText(it.isAvailable ? "Available" : "Occupied");

        // Type row
        if (it.roomType != null && !it.roomType.isEmpty()) {
            h.tvType.setText("Type: " + it.roomType);
            h.tvType.setVisibility(View.VISIBLE);
        } else {
            h.tvType.setVisibility(View.GONE);
        }

        // Capacity row
        if (it.capacity != null && !it.capacity.isEmpty()) {
            h.tvCapacity.setText("Capacity: " + it.capacity);
            h.tvCapacity.setVisibility(View.VISIBLE);
        } else {
            h.tvCapacity.setVisibility(View.GONE);
        }

        // Price row
        h.tvPrice.setText("Price/night: " + (int)Math.round(it.pricePerNight));
        h.tvPrice.setVisibility(View.VISIBLE);

        // Floor row
        h.tvFloor.setText("Floor: " + it.floor);
        h.tvFloor.setVisibility(View.VISIBLE);

        // View row
        if (it.view != null && !it.view.isEmpty()) {
            h.tvView.setText("View: " + it.view);
            h.tvView.setVisibility(View.VISIBLE);
        } else {
            h.tvView.setVisibility(View.GONE);
        }

        if (it.features != null && !it.features.isEmpty()) {
            h.featuresWrapper.setVisibility(View.VISIBLE);
            h.chipGroupFeatures.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(context);
            for (int i = 0; i < it.features.size(); i++) {
                String label = String.valueOf(it.features.get(i));
                Chip chip = (Chip) inflater.inflate(R.layout.simple_feature_chip, h.chipGroupFeatures, false);
                chip.setText(label);
                chip.setClickable(false);
                chip.setCheckable(false);
                h.chipGroupFeatures.addView(chip);
            }
        } else {
            h.chipGroupFeatures.removeAllViews();
            h.featuresWrapper.setVisibility(View.INVISIBLE);
        }

        // Colors
        try {
            h.cardContainer.setBackgroundColor(Color.parseColor(it.cardColor));
        } catch (Exception ignore) { }
        try {
            h.tvStatus.setBackgroundColor(Color.parseColor(it.statusColor));
        } catch (Exception ignore) { }

        h.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) onItemClickListener.onItemClick(it);
        });
        h.btnConfirm.setOnClickListener(v -> {
            if (onConfirmClickListener != null) {
                onConfirmClickListener.onConfirmClick(it);
            } else if (onItemClickListener != null) {
                // Fallback to item click behavior if confirm listener not set
                onItemClickListener.onItemClick(it);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout cardContainer, featuresWrapper;
        TextView tvRoomNumber, tvStatus, tvType, tvCapacity, tvPrice, tvFloor, tvView;
        ChipGroup chipGroupFeatures;
        MaterialButton btnConfirm;
        VH(@NonNull View itemView) {
            super(itemView);
            cardContainer = itemView.findViewById(R.id.cardContainer);
            tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvType = itemView.findViewById(R.id.tvType);
            tvCapacity = itemView.findViewById(R.id.tvCapacity);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvFloor = itemView.findViewById(R.id.tvFloor);
            tvView = itemView.findViewById(R.id.tvView);
            featuresWrapper = itemView.findViewById(R.id.featuresWrapper);
            chipGroupFeatures = itemView.findViewById(R.id.chipGroupFeatures);
            btnConfirm = itemView.findViewById(R.id.btnConfirm);
        }
    }
}
