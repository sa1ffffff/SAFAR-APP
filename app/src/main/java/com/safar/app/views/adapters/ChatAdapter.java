package com.safar.app.views.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.safar.app.R;
import com.safar.app.models.ChatMessage;
import com.safar.app.utils.DateUtils;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;
    private static final int TYPE_SYSTEM = 3;

    private final List<ChatMessage> messages;
    private final String currentUid;

    public ChatAdapter(List<ChatMessage> messages, String currentUid) {
        this.messages = messages;
        this.currentUid = currentUid;
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = messages.get(position);
        if (ChatMessage.TYPE_SYSTEM.equals(msg.getType())) return TYPE_SYSTEM;
        return currentUid != null && currentUid.equals(msg.getSenderId()) ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SENT) {
            return new SentViewHolder(inflater.inflate(R.layout.item_chat_sent, parent, false));
        } else if (viewType == TYPE_RECEIVED) {
            return new ReceivedViewHolder(inflater.inflate(R.layout.item_chat_received, parent, false));
        } else {
            return new SystemViewHolder(inflater.inflate(R.layout.item_chat_system, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).tvText.setText(msg.getText());
            ((SentViewHolder) holder).tvTime.setText(DateUtils.formatChatTime(msg.getTimestamp()));
        } else if (holder instanceof ReceivedViewHolder) {
            ((ReceivedViewHolder) holder).tvSender.setText(msg.getSenderName());
            ((ReceivedViewHolder) holder).tvText.setText(msg.getText());
            ((ReceivedViewHolder) holder).tvTime.setText(DateUtils.formatChatTime(msg.getTimestamp()));
        } else if (holder instanceof SystemViewHolder) {
            ((SystemViewHolder) holder).tvText.setText(msg.getText());
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvText, tvTime;
        SentViewHolder(@NonNull View v) {
            super(v);
            tvText = v.findViewById(R.id.tvMessage);
            tvTime = v.findViewById(R.id.tvTime);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender, tvText, tvTime;
        ReceivedViewHolder(@NonNull View v) {
            super(v);
            tvSender = v.findViewById(R.id.tvSender);
            tvText = v.findViewById(R.id.tvMessage);
            tvTime = v.findViewById(R.id.tvTime);
        }
    }

    static class SystemViewHolder extends RecyclerView.ViewHolder {
        TextView tvText;
        SystemViewHolder(@NonNull View v) {
            super(v);
            tvText = v.findViewById(R.id.tvSystemMessage);
        }
    }
}
