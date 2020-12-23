package com.moez.QKSMS.feature.conversations

import android.view.ViewGroup
import com.moez.QKSMS.common.base.QkAdapter
import com.moez.QKSMS.common.base.QkViewHolder
import com.moez.QKSMS.databinding.ConversationListItemBinding
import com.moez.QKSMS.model.Conversation

class ConvesationListAdapter : QkAdapter<Conversation, ConversationListItemBinding>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder<ConversationListItemBinding> {
        return QkViewHolder(parent, ConversationListItemBinding::inflate)
    }

    override fun onBindViewHolder(holder: QkViewHolder<ConversationListItemBinding>, position: Int) {
        val conversation = getItem(position)

        holder.binding.title.text = conversation.getTitle()
        holder.binding.snippet.text = conversation.snippet
    }

}