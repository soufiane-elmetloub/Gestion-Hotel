package com.example.smarthotelapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RevenueTableAdapter extends RecyclerView.Adapter<RevenueTableAdapter.VH> {
    private final List<RevenueTableItem> items = new ArrayList<>();

    public void setItems(List<RevenueTableItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_revenue_table_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        RevenueTableItem it = items.get(position);
        holder.tvDateDM.setText(it.dateDM);
        holder.tvDaily.setText(it.daily + " MAD");
        holder.tvWeekly.setText(it.weekly + " MAD");
        holder.tvMonthly.setText(it.monthly + " MAD");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDateDM, tvDaily, tvWeekly, tvMonthly;
        VH(@NonNull View itemView) {
            super(itemView);
            tvDateDM = itemView.findViewById(R.id.tvDateDM);
            tvDaily = itemView.findViewById(R.id.tvDaily);
            tvWeekly = itemView.findViewById(R.id.tvWeekly);
            tvMonthly = itemView.findViewById(R.id.tvMonthly);
        }
    }
}
