package com.safar.app.views.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.safar.app.R;
import com.safar.app.models.JoinRequest;

import java.util.List;

public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.ViewHolder> {

    public interface OnRequestActionListener {
        void onAccept(JoinRequest request);
        void onReject(JoinRequest request);
        void onChat(JoinRequest request);
    }

    private List<JoinRequest> requests;
    private final OnRequestActionListener listener;

    public RequestsAdapter(List<JoinRequest> requests, OnRequestActionListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    public void updateList(List<JoinRequest> list) {
        this.requests = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inbox_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JoinRequest req = requests.get(position);
        holder.tvName.setText(req.getRequestorName());
        holder.tvStatus.setText(req.getStatus());

        if (JoinRequest.STATUS_PENDING.equals(req.getStatus())) {
            holder.btnAccept.setVisibility(View.VISIBLE);
            holder.btnReject.setVisibility(View.VISIBLE);
            holder.btnChat.setVisibility(View.GONE);
            holder.btnAccept.setOnClickListener(v -> listener.onAccept(req));
            holder.btnReject.setOnClickListener(v -> listener.onReject(req));
        } else if (JoinRequest.STATUS_ACCEPTED.equals(req.getStatus())) {
            holder.btnAccept.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
            holder.btnChat.setVisibility(View.VISIBLE);
            holder.btnChat.setOnClickListener(v -> listener.onChat(req));
        } else {
            holder.btnAccept.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
            holder.btnChat.setVisibility(View.GONE);
        }
    }

    @Override public int getItemCount() { return requests.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTime, tvStatus;
        Button btnAccept, btnReject, btnChat;
        ViewHolder(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvRequesterName);
            tvTime = v.findViewById(R.id.tvTime);
            tvStatus = v.findViewById(R.id.tvStatus);
            btnAccept = v.findViewById(R.id.btnAccept);
            btnReject = v.findViewById(R.id.btnReject);
            btnChat = v.findViewById(R.id.btnChat);
        }
    }
}
