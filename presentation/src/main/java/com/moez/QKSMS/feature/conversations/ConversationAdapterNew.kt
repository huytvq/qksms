package com.moez.QKSMS.feature.conversations

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.moez.QKSMS.R
import com.moez.QKSMS.common.Navigator
import com.moez.QKSMS.common.util.Colors
import com.moez.QKSMS.common.util.DateFormatter
import com.moez.QKSMS.common.util.extensions.resolveThemeColor
import com.moez.QKSMS.common.util.extensions.setTint
import com.moez.QKSMS.common.widget.GroupAvatarView
import com.moez.QKSMS.common.widget.QkTextView
import com.moez.QKSMS.util.PhoneNumberUtils
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.util.*
import javax.inject.Inject

class ConversationAdapterNew @Inject constructor(private val context: Context,
                                                 private val navigator: Navigator)
    : RecyclerView.Adapter<ConversationAdapterNew.ViewHolder>() {

    private var conversations: ArrayList<ConversationNew>? = null
    private val dateFormatter: DateFormatter
    private var colors: Colors? = null
    private var phoneNumberUtils: PhoneNumberUtils? = null

    val selectionChanges: Subject<List<Long>> = BehaviorSubject.create()

    private var selection = listOf<Long>()

    init {
        // This is how we access the threadId for the swipe actions
        setHasStableIds(true)

        dateFormatter = DateFormatter(context)
    }

    protected fun toggleSelection(id: Long, force: Boolean = true): Boolean {
        if (!force && selection.isEmpty()) return false

        selection = when (selection.contains(id)) {
            true -> selection - id
            false -> selection + id
        }

        selectionChanges.onNext(selection)
        return true
    }

    fun clearSelection() {
        selection = listOf()
        selectionChanges.onNext(selection)
        notifyDataSetChanged()
    }

    private fun isSelected(id: Long): Boolean {
        return selection.contains(id)
    }

    fun setData(conversations: ArrayList<ConversationNew>?, colors: Colors?, phoneNumberUtils: PhoneNumberUtils) {
        this.conversations = conversations
        this.colors = colors
        this.phoneNumberUtils = phoneNumberUtils
    }

    fun clearData() {
        if (conversations != null && conversations!!.size > 0) {
            conversations!!.clear()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.conversation_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversation = conversations!![position]
        holder.dateHeader.text = dateFormatter.getFormatHeaderConversationDay(conversation.date)

        val lastMessage = conversation.conversation.lastMessage
        val recipient = when {
            conversation.conversation.recipients.size == 1 || lastMessage == null -> conversation.conversation.recipients.firstOrNull()
            else -> conversation.conversation.recipients.find { recipient ->
                phoneNumberUtils!!.compare(recipient.address, lastMessage.address)
            }
        }
        val theme = colors!!.theme(recipient).theme
        holder.itemView.isActivated = isSelected(conversation.conversation.id)

        val textColorPrimary = context.resolveThemeColor(android.R.attr.textColorPrimary)

        holder.avatars.recipients = conversation.conversation.recipients
        holder.title.text = buildSpannedString {
            append(conversation.conversation.getTitle())
            if (conversation.conversation.draft.isNotEmpty()) {
                color(theme) { append(" " + context.getString(R.string.main_draft)) }
            }
        }
        holder.title.collapseEnabled = conversation.conversation.recipients.size > 1
        holder.date.text = conversation.date.takeIf { it > 0 }?.let(dateFormatter::getConversationTimestamp)
        holder.snippet.text = when {
            conversation.conversation.draft.isNotEmpty() -> conversation.conversation.draft
            conversation.conversation.me -> context.getString(R.string.main_sender_you, conversation.conversation.snippet)
            else -> conversation.conversation.snippet
        }
        holder.pinned.isVisible = conversation.conversation.pinned
        holder.unread.setTint(theme)

        if (conversation.conversation.unread) {
            holder.title.setTypeface(holder.title.typeface, Typeface.BOLD)
            holder.snippet.setTypeface(holder.snippet.typeface, Typeface.BOLD)
            holder.snippet.setTextColor(textColorPrimary)
            holder.snippet.maxLines = 5
            holder.unread.isVisible = true
            holder.date.setTypeface(holder.date.typeface, Typeface.BOLD)
            holder.date.setTextColor(textColorPrimary)
        }

        //onClick
        holder.itemView.setOnClickListener {
            when (toggleSelection(conversation.conversation.id, false)) {
                true -> holder.itemView.isActivated = isSelected(conversation.conversation.id)
                false -> navigator.showConversation(conversation.conversation.id)
            }
        }

        holder.itemView.setOnLongClickListener {
            val conversation2 = conversations!![position].conversation
                    ?: return@setOnLongClickListener true
            toggleSelection(conversation2.id)
            holder.itemView.isActivated = isSelected(conversation2.id)
            true
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var cardRecyclerView: CardView
        var avatars: GroupAvatarView
        var title: QkTextView
        var snippet: QkTextView
        var date: QkTextView
        var dateHeader: QkTextView
        var layoutItemNone: ConstraintLayout
        var pinned: ImageView
        var unread: ImageView

        init {
            cardRecyclerView = itemView.findViewById(R.id.cardRecyclerView)
            avatars = itemView.findViewById(R.id.avatars)
            title = itemView.findViewById(R.id.title)
            snippet = itemView.findViewById(R.id.snippet)
            date = itemView.findViewById(R.id.date)
            dateHeader = itemView.findViewById(R.id.dateHeader)
            layoutItemNone = itemView.findViewById(R.id.layout_item_none)
            pinned = itemView.findViewById(R.id.pinned)
            unread = itemView.findViewById(R.id.unread)
        }
    }

    override fun getItemCount(): Int {
        return conversations!!.size
    }
}
