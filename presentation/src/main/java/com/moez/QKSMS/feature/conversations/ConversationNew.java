package com.moez.QKSMS.feature.conversations;

import com.moez.QKSMS.model.Conversation;

import java.util.ArrayList;
import java.util.List;

public class ConversationNew {
    public static final int TYPE_GROUP = 1;
    public static final int TYPE_NONE = 0;

    private long date;
    private ArrayList<Conversation> conversations;
    private Conversation conversation;
    private int type;

    public ConversationNew() {
    }

    public ConversationNew(long date, ArrayList<Conversation> conversations, Conversation conversation, int type) {
        this.date = date;
        this.conversations = conversations;
        this.conversation = conversation;
        this.type = type;
    }

    public long getDate() {
        return date;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public List<Conversation> getConversations() {
        return conversations;
    }

    public void setConversations(ArrayList<Conversation> conversations) {
        this.conversations = conversations;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }
}
