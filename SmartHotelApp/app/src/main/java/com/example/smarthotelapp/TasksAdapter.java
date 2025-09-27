package com.example.smarthotelapp;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TaskViewHolder> {
    private Context context;
    private List<Task> taskList;

    public TasksAdapter(Context context, List<Task> taskList) {
        this.context = context;
        this.taskList = taskList;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        
        // Set task data
        holder.taskTitle.setText(task.getTitle());
        holder.taskDescription.setText(task.getDescription());
        holder.taskStatus.setText(task.getStatusText());
        holder.taskDate.setText(task.getFormattedDate());
        
        // Set status color for indicator and card
        int statusColor = task.getStatusColorInt();
        holder.taskIndicator.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(statusColor)
        );
        
        // Set card background color based on status
        String cardColor = getCardBackgroundColor(task.getStatus());
        holder.taskCard.setCardBackgroundColor(Color.parseColor(cardColor));
        
        // Set title color based on status
        String titleColor = getTitleColor(task.getStatus());
        holder.taskTitle.setTextColor(Color.parseColor(titleColor));
        
        // Set status background color
        holder.taskStatus.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(statusColor)
        );
        
        // Hide timeline line for last item
        if (position == taskList.size() - 1) {
            holder.itemView.findViewById(R.id.taskIndicator).getLayoutParams().height = 
                (int) (12 * context.getResources().getDisplayMetrics().density);
        }
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    private String getCardBackgroundColor(String status) {
        switch (status) {
            case "pending":
                return "#FFF3E0"; // Light orange
            case "in_progress":
                return "#E3F2FD"; // Light blue
            case "done":
                return "#E8F5E8"; // Light green
            default:
                return "#F5F5F5"; // Light grey
        }
    }

    private String getTitleColor(String status) {
        switch (status) {
            case "pending":
                return "#E65100"; // Dark orange
            case "in_progress":
                return "#1565C0"; // Dark blue
            case "done":
                return "#2E7D32"; // Dark green
            default:
                return "#424242"; // Dark grey
        }
    }

    public void updateTasks(List<Task> newTasks) {
        this.taskList = newTasks;
        notifyDataSetChanged();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskTitle, taskDescription, taskStatus, taskDate;
        View taskIndicator;
        CardView taskCard;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.taskTitle);
            taskDescription = itemView.findViewById(R.id.taskDescription);
            taskStatus = itemView.findViewById(R.id.taskStatus);
            taskDate = itemView.findViewById(R.id.taskDate);
            taskIndicator = itemView.findViewById(R.id.taskIndicator);
            taskCard = itemView.findViewById(R.id.taskCard);
        }
    }
}
