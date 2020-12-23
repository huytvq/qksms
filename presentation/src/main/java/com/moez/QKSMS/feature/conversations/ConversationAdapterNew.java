package com.moez.QKSMS.feature.conversations;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.moez.QKSMS.R;
import com.moez.QKSMS.common.util.DateFormatter;
import com.moez.QKSMS.common.widget.GroupAvatarView;
import com.moez.QKSMS.common.widget.QkTextView;
import com.moez.QKSMS.manager.ActiveConversationManager;
import com.moez.QKSMS.model.Conversation;

import java.util.ArrayList;

public class ConversationAdapterNew extends RecyclerView.Adapter {

    private Context context;
    private ArrayList<ConversationNew> conversations;
    private DateFormatter dateFormatter;

    public ConversationAdapterNew(Context context) {
        this.context = context;
        dateFormatter = new DateFormatter(context);
    }

    public void setData(ArrayList<ConversationNew> conversations) {
        this.conversations = conversations;
    }

    @Override
    public int getItemViewType(int position) {
        switch (conversations.get(position).getType()) {
            case 0:
                return ConversationNew.TYPE_NONE;
            case 1:
                return ConversationNew.TYPE_GROUP;
            default:
                return -1;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case ConversationNew.TYPE_NONE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.conversation_list_item, parent, false);
                return new NoneViewHolder(view);
            case ConversationNew.TYPE_GROUP:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.conversation_list_item, parent, false);
                return new GroupViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ConversationNew conversationNew = conversations.get(position);
        if (conversationNew != null) {
            switch (conversationNew.getType()) {
                case ConversationNew.TYPE_NONE:
                    ((NoneViewHolder) holder).cardRecyclerView.setVisibility(View.GONE);
                    ((NoneViewHolder) holder).layoutItemNone.setVisibility(View.VISIBLE);
                    ((NoneViewHolder) holder).title.setText(conversationNew.getConversation().getLastMessage().getAddress());
                    ((NoneViewHolder) holder).snippet.setText(conversationNew.getConversation().getSnippet());
                    ((NoneViewHolder) holder).date.setText(dateFormatter.getConversationTimestamp(conversationNew.getDate()));
                    ((NoneViewHolder) holder).dateHeader.setText(dateFormatter.getFormatHeaderConversationTimestamp(conversationNew.getDate()));
                    break;
                case ConversationNew.TYPE_GROUP:
                    ((GroupViewHolder) holder).cardRecyclerView.setVisibility(View.VISIBLE);
                    ((GroupViewHolder) holder).layoutItemNone.setVisibility(View.GONE);
                    ((GroupViewHolder) holder).groupAvatarView.setVisibility(View.GONE);
                    ((GroupViewHolder) holder).dateHeader.setVisibility(View.GONE);

                    GroupConversationAdapter groupConversationAdapter = new GroupConversationAdapter(context);
                    groupConversationAdapter.setData((ArrayList<Conversation>) conversationNew.getConversations());
                    ((GroupViewHolder) holder).recyclerView.setAdapter(groupConversationAdapter);

                    break;
            }
        }
    }

    public static class NoneViewHolder extends RecyclerView.ViewHolder {
        CardView cardRecyclerView;
        GroupAvatarView groupAvatarView;
        QkTextView title, snippet, date, dateHeader;
        ConstraintLayout layoutItemNone;

        public NoneViewHolder(View itemView) {
            super(itemView);

            cardRecyclerView = itemView.findViewById(R.id.cardRecyclerView);
            groupAvatarView = itemView.findViewById(R.id.avatars);
            title = itemView.findViewById(R.id.title);
            snippet = itemView.findViewById(R.id.snippet);
            date = itemView.findViewById(R.id.date);
            dateHeader = itemView.findViewById(R.id.dateHeader);
            layoutItemNone = itemView.findViewById(R.id.layout_item_none);
        }
    }

    public static class GroupViewHolder extends RecyclerView.ViewHolder {
        RecyclerView recyclerView;
        CardView cardRecyclerView;
        GroupAvatarView groupAvatarView;
        QkTextView dateHeader;
        ConstraintLayout layoutItemNone;

        public GroupViewHolder(View itemView) {
            super(itemView);

            recyclerView = itemView.findViewById(R.id.numbers);
            cardRecyclerView = itemView.findViewById(R.id.cardRecyclerView);
            groupAvatarView = itemView.findViewById(R.id.avatars);
            dateHeader = itemView.findViewById(R.id.dateHeader);
            layoutItemNone = itemView.findViewById(R.id.layout_item_none);
        }
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }
}
