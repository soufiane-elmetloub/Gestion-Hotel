package com.example.smarthotelapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.ColorUtils;

import java.util.ArrayList;
import java.util.List;

public class GuestCapsuleAdapter extends BaseAdapter {
    private final Context context;
    private final LayoutInflater inflater;
    private final List<String> firstNames;
    private final List<String> lastNames;

    public interface OnDeleteClickListener {
        void onDelete(int position, String firstName, String lastName);
    }

    private OnDeleteClickListener deleteClickListener;

    public GuestCapsuleAdapter(@NonNull Context context,
                               @NonNull List<String> firstNames,
                               @NonNull List<String> lastNames) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.firstNames = new ArrayList<>(firstNames);
        this.lastNames = new ArrayList<>(lastNames);
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }

    @Override
    public int getCount() {
        return Math.min(firstNames.size(), lastNames.size());
    }

    @Override
    public Object getItem(int position) {
        return firstNames.get(position) + " " + lastNames.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        CardView card;
        TextView tvGuestName;
        TextView tvAvatarInitials;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_guest_capsule, parent, false);
            holder = new ViewHolder();
            holder.card = (CardView) convertView;
            holder.tvGuestName = convertView.findViewById(R.id.tvGuestName);
            holder.tvAvatarInitials = convertView.findViewById(R.id.tvAvatarInitials);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String f = safe(firstNames.get(position));
        String l = safe(lastNames.get(position));
        if (holder.tvGuestName != null) {
            holder.tvGuestName.setText((f + " " + l).trim());
        }
        if (holder.tvAvatarInitials != null) {
            String initials = (firstLetter(f) + firstLetter(l)).trim();
            holder.tvAvatarInitials.setText(initials.isEmpty() ? "?" : initials);
        }

        // Generate a unique, non-repeating pastel color per position using golden angle
        float hue = (position * 137f) % 360f; // golden-angle approximation for distribution
        float saturation = 0.45f;
        float lightness = 0.82f;
        int color = ColorUtils.HSLToColor(new float[]{hue, saturation, lightness});
        // Background now handled by capsule gradient; you may skip coloring the card
        // holder.card.setCardBackgroundColor(color);

        return convertView;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String firstLetter(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase();
    }

    public void removeAt(int position) {
        if (position >= 0 && position < getCount()) {
            firstNames.remove(position);
            lastNames.remove(position);
            notifyDataSetChanged();
        }
    }

    public ArrayList<String> getFirstNames() {
        return new ArrayList<>(firstNames);
    }

    public ArrayList<String> getLastNames() {
        return new ArrayList<>(lastNames);
    }
}

