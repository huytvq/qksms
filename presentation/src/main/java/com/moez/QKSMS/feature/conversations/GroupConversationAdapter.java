package com.moez.QKSMS.feature.conversations;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.moez.QKSMS.R;
import com.moez.QKSMS.common.util.DateFormatter;
import com.moez.QKSMS.common.widget.GroupAvatarView;
import com.moez.QKSMS.common.widget.QkTextView;
import com.moez.QKSMS.model.Conversation;

import java.util.ArrayList;

public class GroupConversationAdapter extends RecyclerView.Adapter<GroupConversationAdapter.ViewHolder> {

    private Context context;
    private ArrayList<Conversation> conversations;
    private DateFormatter dateFormatter;

    public GroupConversationAdapter(Context context) {
        this.context = context;
        dateFormatter = new DateFormatter(context);
    }

    public void setData(ArrayList<Conversation> conversations) {
        this.conversations = conversations;
    }

    @NonNull
    @Override
    public GroupConversationAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.conversation_group_item, parent, false);
        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull GroupConversationAdapter.ViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);
        if (conversation != null) {
            holder.title.setText(conversation.getLastMessage().getAddress());
            holder.date.setText(dateFormatter.getConversationTimestamp(conversation.getDate()));
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        GroupAvatarView groupAvatarView;
        QkTextView title,date;

        public ViewHolder(View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.title);
            date = itemView.findViewById(R.id.date);
            groupAvatarView = itemView.findViewById(R.id.avatars);
        }
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }
}
