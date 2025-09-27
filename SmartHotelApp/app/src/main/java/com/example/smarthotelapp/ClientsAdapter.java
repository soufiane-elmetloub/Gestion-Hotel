package com.example.smarthotelapp;

import com.example.smarthotelapp.models.Client;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClientsAdapter extends RecyclerView.Adapter<ClientsAdapter.ClientViewHolder> {
    
    private List<Client> clients;
    private OnClientClickListener listener;
    
    public interface OnClientClickListener {
        void onClientClick(Client client);
        void onClientMenuClick(Client client, View view);
    }
    
    public ClientsAdapter(List<Client> clients, OnClientClickListener listener) {
        this.clients = clients;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ClientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_client, parent, false);
        return new ClientViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ClientViewHolder holder, int position) {
        Client client = clients.get(position);
        holder.bind(client);
    }
    
    @Override
    public int getItemCount() {
        return clients.size();
    }
    
    public void setClients(List<Client> clients) {
        this.clients = clients;
        notifyDataSetChanged();
    }
    
    class ClientViewHolder extends RecyclerView.ViewHolder {
        private TextView clientInitials;
        private TextView clientName;
        private TextView clientPhone;
        private TextView clientStatus;
        private TextView clientDate;
        private ImageView clientMenuButton;
        
        public ClientViewHolder(@NonNull View itemView) {
            super(itemView);
            
            clientInitials = itemView.findViewById(R.id.clientInitials);
            clientName = itemView.findViewById(R.id.clientName);
            clientPhone = itemView.findViewById(R.id.clientPhone);
            clientStatus = itemView.findViewById(R.id.clientStatus);
            clientDate = itemView.findViewById(R.id.clientDate);
            clientMenuButton = itemView.findViewById(R.id.clientMenuButton);
            
            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onClientClick(clients.get(position));
                    }
                }
            });
            
            clientMenuButton.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onClientMenuClick(clients.get(position), v);
                    }
                }
            });
        }
        
        public void bind(Client client) {
            // Set initials
            clientInitials.setText(client.getInitials());
            
            // Set name
            clientName.setText(client.getFullName());
            
            // Set phone
            clientPhone.setText(client.getPhone());
            
            // Set status with appropriate background
            clientStatus.setText(client.getStatus());
            setStatusBackground(client.getStatus());
            
            // Set formatted date
            clientDate.setText(formatDate(client.getCreatedAt()));
        }
        
        private void setStatusBackground(String status) {
            int backgroundRes;
            switch (status.toLowerCase()) {
                case "نشط":
                case "active":
                    backgroundRes = R.drawable.status_active_bg;
                    break;
                case "غير نشط":
                case "inactive":
                    backgroundRes = R.drawable.status_inactive_bg;
                    break;
                case "في الانتظار":
                case "pending":
                    backgroundRes = R.drawable.status_pending_bg;
                    break;
                default:
                    backgroundRes = R.drawable.status_default_bg;
                    break;
            }
            clientStatus.setBackgroundResource(backgroundRes);
        }
        
        private String formatDate(String dateString) {
            try {
                // Assuming the date comes in format "yyyy-MM-dd HH:mm:ss"
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date date = inputFormat.parse(dateString);
                return outputFormat.format(date);
            } catch (Exception e) {
                // If parsing fails, return the original string
                return dateString;
            }
        }
    }
}
