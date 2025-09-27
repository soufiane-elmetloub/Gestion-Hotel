package com.example.smarthotelapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smarthotelapp.R;
import com.example.smarthotelapp.models.Client;
import java.util.ArrayList;
import java.util.List;

public class ClientSearchAdapter extends RecyclerView.Adapter<ClientSearchAdapter.ViewHolder> {
    
    private List<Client> clients = new ArrayList<>();
    private OnClientClickListener listener;
    
    public interface OnClientClickListener {
        void onClientClick(Client client);
    }
    
    public ClientSearchAdapter(OnClientClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_client_search, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Client client = clients.get(position);
        holder.bind(client);
    }
    
    @Override
    public int getItemCount() {
        return clients.size();
    }
    
    public void updateClients(List<Client> newClients) {
        this.clients.clear();
        this.clients.addAll(newClients);
        notifyDataSetChanged();
    }

    public void setItems(List<Client> newClients) {
        this.clients.clear();
        this.clients.addAll(newClients);
        notifyDataSetChanged();
    }
    
    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText;
        private TextView phoneText;
        private TextView nationalIdText;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.clientName);
            phoneText = itemView.findViewById(R.id.clientPhone);
            nationalIdText = itemView.findViewById(R.id.clientNationalId);
            
            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onClientClick(clients.get(getAdapterPosition()));
                }
            });
        }
        
        public void bind(Client client) {
            nameText.setText(client.getFullName());
            phoneText.setText(client.getPhone());
            nationalIdText.setText(client.getNationalId());
        }
    }
}
