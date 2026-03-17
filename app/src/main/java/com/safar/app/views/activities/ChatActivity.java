package com.safar.app.views.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.safar.app.databinding.ActivityChatBinding;
import com.safar.app.models.ChatMessage;
import com.safar.app.presenters.ChatPresenter;
import com.safar.app.utils.AppConstants;
import com.safar.app.utils.SessionManager;
import com.safar.app.views.adapters.ChatAdapter;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity implements ChatPresenter.ChatView {

    private ActivityChatBinding binding;
    private ChatPresenter presenter;
    private ChatAdapter adapter;
    private String currentUid;
    private String chatRoomId;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        chatRoomId = getIntent().getStringExtra(AppConstants.KEY_CHAT_ROOM_ID);
        String otherUserName = getIntent().getStringExtra(AppConstants.KEY_OTHER_USER_NAME);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(otherUserName != null ? otherUserName : "Chat");
        }

        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        session = new SessionManager(this);

        adapter = new ChatAdapter(new ArrayList<>(), currentUid);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);

        presenter = new ChatPresenter(this, chatRoomId);
        presenter.startListening();

        binding.btnSend.setOnClickListener(v -> sendMessage());
        binding.etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void sendMessage() {
        String text = binding.etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        binding.etMessage.setText("");
        ChatMessage msg = new ChatMessage(currentUid, session.getUserName(), text, ChatMessage.TYPE_TEXT);
        presenter.sendMessage(msg);
    }

    @Override
    public void onMessageReceived(ChatMessage message) {
        adapter.addMessage(message);
        binding.recyclerView.scrollToPosition(adapter.getItemCount() - 1);
    }

    @Override
    public void onMessagesLoaded(List<ChatMessage> messages) {}
    @Override
    public void onMessageSent() {}
    @Override
    public void onError(String message) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (presenter != null) presenter.detach();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
