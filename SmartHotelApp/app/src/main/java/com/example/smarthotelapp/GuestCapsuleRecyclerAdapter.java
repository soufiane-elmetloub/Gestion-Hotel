package com.example.smarthotelapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class GuestCapsuleRecyclerAdapter extends RecyclerView.Adapter<GuestCapsuleRecyclerAdapter.VH> {
    private final List<BookingCapsule> items = new ArrayList<>();
    private OnGuestClickListener clickListener;

    public GuestCapsuleRecyclerAdapter() {
        setHasStableIds(true);
    }

    public void setItems(List<BookingCapsule> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    public List<BookingCapsule> getItems() {
        return new ArrayList<>(items);
    }

    public interface OnGuestClickListener {
        void onGuestClick(BookingCapsule guest);
    }

    public void setOnGuestClickListener(OnGuestClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_guest_capsule, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        BookingCapsule b = items.get(position);
        // Reservation ID on top (English, no # and no space): e.g., Reservation:12
        h.setText(R.id.tvReservationId, "Reservation :" +  b.id);

        // Name and initials
        String name = (safe(b.firstName) + " " + safe(b.lastName)).trim();
        h.tvGuestName.setText(name);
        String initials = (firstLetter(b.firstName) + firstLetter(b.lastName)).trim();
        h.tvAvatarInitials.setText(initials.isEmpty() ? "?" : initials);

        // Click to show details
        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onGuestClick(b);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= items.size()) return RecyclerView.NO_ID;
        return items.get(position).id;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvGuestName;
        TextView tvAvatarInitials;
        VH(@NonNull View itemView) {
            super(itemView);
            tvGuestName = itemView.findViewById(R.id.tvGuestName);
            tvAvatarInitials = itemView.findViewById(R.id.tvAvatarInitials);
        }
        void setText(int id, String value) {
            TextView tv = itemView.findViewById(id);
            if (tv != null) tv.setText(value);
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static String firstLetter(String s) { return (s == null || s.isEmpty()) ? "" : s.substring(0,1).toUpperCase(); }
}
