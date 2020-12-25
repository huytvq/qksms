/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.moez.QKSMS.feature.main

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import com.moez.QKSMS.R
import com.moez.QKSMS.common.Navigator
import com.moez.QKSMS.common.androidxcompat.drawerOpen
import com.moez.QKSMS.common.base.QkThemedActivity
import com.moez.QKSMS.common.util.DateFormatter
import com.moez.QKSMS.common.util.extensions.*
import com.moez.QKSMS.databinding.MainActivityBinding
import com.moez.QKSMS.feature.Constants
import com.moez.QKSMS.feature.SharedPreferenceHelper
import com.moez.QKSMS.feature.blocking.BlockingDialog
import com.moez.QKSMS.feature.changelog.ChangelogDialog
import com.moez.QKSMS.feature.conversations.ConversationAdapterNew
import com.moez.QKSMS.feature.conversations.ConversationItemTouchCallback
import com.moez.QKSMS.feature.conversations.ConversationNew
import com.moez.QKSMS.feature.conversations.ConversationsAdapter
import com.moez.QKSMS.feature.conversations.date.DateHeaderConversationAdapter
import com.moez.QKSMS.feature.conversations.date.OnSelectedDateListener
import com.moez.QKSMS.manager.ChangelogManager
import com.moez.QKSMS.repository.SyncRepository
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import dagger.android.AndroidInjection
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.*
import javax.inject.Inject
import kotlin.collections.LinkedHashSet


class MainActivity : QkThemedActivity(), MainView, OnSelectedDateListener {

    @Inject
    lateinit var blockingDialog: BlockingDialog

    @Inject
    lateinit var disposables: CompositeDisposable

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var conversationsAdapter: ConversationsAdapter

    @Inject
    lateinit var conversationAdapterNew: ConversationAdapterNew

    @Inject
    lateinit var drawerBadgesExperiment: DrawerBadgesExperiment

    @Inject
    lateinit var searchAdapter: SearchAdapter

    @Inject
    lateinit var itemTouchCallback: ConversationItemTouchCallback

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var dateFormatter: DateFormatter

    override val onNewIntentIntent: Subject<Intent> = PublishSubject.create()
    override val activityResumedIntent: Subject<Boolean> = PublishSubject.create()
    override val queryChangedIntent by lazy { binding.toolbarSearch.textChanges() }
    override val composeIntent by lazy { binding.compose.clicks() }
    override val drawerOpenIntent: Observable<Boolean> by lazy {
        binding.drawerLayout
                .drawerOpen(Gravity.START)
                .doOnNext { dismissKeyboard() }
    }
    override val homeIntent: Subject<Unit> = PublishSubject.create()
    override val navigationIntent: Observable<NavItem> by lazy {
        Observable.merge(listOf(
                backPressedSubject,
                binding.drawer.inbox.clicks().map { NavItem.INBOX },
                binding.drawer.archived.clicks().map { NavItem.ARCHIVED },
                binding.drawer.backup.clicks().map { NavItem.BACKUP },
                binding.drawer.scheduled.clicks().map { NavItem.SCHEDULED },
                binding.drawer.blocking.clicks().map { NavItem.BLOCKING },
                binding.drawer.settings.clicks().map { NavItem.SETTINGS },
                binding.drawer.plus.clicks().map { NavItem.PLUS },
                binding.drawer.help.clicks().map { NavItem.HELP },
                binding.drawer.invite.clicks().map { NavItem.INVITE }))
    }
    override val optionsItemIntent: Subject<Int> = PublishSubject.create()
    override val plusBannerIntent by lazy { binding.drawer.plusBanner.clicks() }
    override val dismissRatingIntent by lazy { binding.drawer.rateDismiss.clicks() }
    override val rateIntent by lazy { binding.drawer.rateOkay.clicks() }
    override val conversationsSelectedIntent by lazy { conversationAdapterNew.selectionChanges }
    override val confirmDeleteIntent: Subject<List<Long>> = PublishSubject.create()
    override val swipeConversationIntent by lazy { itemTouchCallback.swipes }
    override val changelogMoreIntent by lazy { changelogDialog.moreClicks }
    override val undoArchiveIntent: Subject<Unit> = PublishSubject.create()
    override val snackbarButtonIntent by lazy { binding.snackbar.button.clicks() }

    private val binding by viewBinding(MainActivityBinding::inflate)
    private val viewModel by lazy { ViewModelProviders.of(this, viewModelFactory)[MainViewModel::class.java] }
    private val toggle by lazy { ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.main_drawer_open_cd, 0) }
    private val itemTouchHelper by lazy { ItemTouchHelper(itemTouchCallback) }
    private val progressAnimator by lazy { ObjectAnimator.ofInt(binding.syncing.progress, "progress", 0, 0) }
    private val changelogDialog by lazy { ChangelogDialog(this) }
    private val snackbar by lazy { findViewById<View>(R.id.snackbar) }
    private val syncing by lazy { findViewById<View>(R.id.syncing) }
    private val backPressedSubject: Subject<NavItem> = PublishSubject.create()

    private var conversationList = arrayListOf<ConversationNew>()
    private var dateList = arrayListOf<String>()
    private var isSelected = 0
    private var isSelectedArchived = 0
    private var isCurrentDate: String = ""
    private var isCurrentPositionDate: Int = 0
    private var isMenuItemSelected: Boolean = false
    private var isMenuItemSelectedArchived: Boolean = false
    private var isClickDrawer: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        viewModel.bindView(this)
        onNewIntentIntent.onNext(intent)

        toggle.syncState()
        binding.toolbar.setNavigationOnClickListener {
            dismissKeyboard()
            homeIntent.onNext(Unit)
            isSelected = 0
            isSelectedArchived = 0
            isMenuItemSelected = false
            isMenuItemSelectedArchived = false
        }

//        itemTouchCallback.adapter = conversationAdapterNew
        conversationAdapterNew.autoScrollToStart(binding.recyclerView)

        // Don't allow clicks to pass through the drawer layout
        binding.drawer.root.clicks().autoDisposable(scope()).subscribe()

        // Set the theme color tint to the recyclerView, progressbar, and FAB
        theme
                .autoDisposable(scope())
                .subscribe { theme ->
                    // Set the color for the drawer icons
                    val states = arrayOf(
                            intArrayOf(android.R.attr.state_activated),
                            intArrayOf(-android.R.attr.state_activated))

                    resolveThemeColor(android.R.attr.textColorSecondary)
                            .let { textSecondary -> ColorStateList(states, intArrayOf(theme.theme, textSecondary)) }
                            .let { tintList ->
                                binding.drawer.inboxIcon.imageTintList = tintList
                                binding.drawer.archivedIcon.imageTintList = tintList
                            }

                    // Miscellaneous views
                    listOf(binding.drawer.plusBadge1, binding.drawer.plusBadge2).forEach { badge ->
                        badge.setBackgroundTint(theme.theme)
                        badge.setTextColor(theme.textPrimary)
                    }
                    binding.syncing.progress.progressTintList = ColorStateList.valueOf(theme.theme)
                    binding.syncing.progress.indeterminateTintList = ColorStateList.valueOf(theme.theme)
                    binding.drawer.plusIcon.setTint(theme.theme)
                    binding.drawer.rateIcon.setTint(theme.theme)
                    binding.compose.setBackgroundTint(theme.theme)

                    // Set the FAB compose icon color
                    binding.compose.setTint(theme.textPrimary)
                }

        // These theme attributes don't apply themselves on API 21
        if (Build.VERSION.SDK_INT <= 22) {
            binding.toolbarSearch.setBackgroundTint(resolveThemeColor(R.attr.bubbleColor))
        }

        //onClick
        binding.cardSelected.setOnClickListener {
            if (binding.cardSpinner.isVisible) {
                binding.cardSpinner.isVisible = false
            } else {
                binding.cardSpinner.isVisible = true
            }
        }

        onClick()
    }

    private fun onClick() {
        binding.rootMain.setOnClickListener {
            if (binding.cardSpinner.isVisible) {
                binding.cardSpinner.isVisible = false
            }
        }
        binding.timeDay.setOnClickListener {
            onSortSelected(binding.timeDay.text.toString())
        }
        binding.timeWeek.setOnClickListener {
            onSortSelected(binding.timeWeek.text.toString())
        }
        binding.timeMonth.setOnClickListener {
            onSortSelected(binding.timeMonth.text.toString())
        }
        binding.timeYear.setOnClickListener {
            onSortSelected(binding.timeYear.text.toString())
        }
    }

    private fun onSortSelected(type: String) {
        SharedPreferenceHelper.getInstance(this).set(Constants.TYPE_SORT, type)
        binding.cardSpinner.isVisible = false
        binding.typeSelected.text = SharedPreferenceHelper.getInstance(this).get(Constants.TYPE_SORT)

        reLoaderData(SharedPreferenceHelper.getInstance(this).get(Constants.TYPE_SORT))
    }

    private fun reLoaderData(type: String?) {
        if (conversationList.size > 0) {
            conversationList.clear()
        }

        if (dateList.size > 0) {
            dateList.clear()
        }
        if (Constants.conversationList.size > 0) {
            for (i in 0 until Constants.conversationList.size) {
                when (type) {
                    "Dayly" -> {
                        dateList.add(dateFormatter.getFormatHeaderConversationDay(Constants.conversationList[i].date))
                    }
                    "Monthly" -> {
                        dateList.add(dateFormatter.getFormatHeaderConversationMonth(Constants.conversationList[i].date))
                    }
                    "Yearly" -> {
                        dateList.add(dateFormatter.getFormatHeaderConversationYear(Constants.conversationList[i].date))
                    }
                    else -> {
                        dateList.add(dateFormatter.getWeekOfYear(Constants.conversationList[i].date))
                    }
                }
            }

            //remove duplicate
            val hashSet: LinkedHashSet<String> = LinkedHashSet(dateList)
            val listDateDuplicates = ArrayList(hashSet)

            isCurrentDate = listDateDuplicates[0].toString()

            //
            val headerDateAdapter = DateHeaderConversationAdapter(this, listDateDuplicates, this)
            binding.recyclerViewDate.adapter = headerDateAdapter

            for (i in 0 until Constants.conversationList.size) {
                when (type) {
                    "Dayly" -> {
                        if (dateFormatter.getFormatHeaderConversationDay(Constants.conversationList[i].date) == listDateDuplicates[0]) {
                            conversationList.add(ConversationNew(Constants.conversationList[i].date, null, Constants.conversationList[i], ConversationNew.TYPE_NONE))
                        }
                    }
                    "Monthly" -> {
                        if (dateFormatter.getFormatHeaderConversationMonth(Constants.conversationList[i].date) == listDateDuplicates[0]) {
                            conversationList.add(ConversationNew(Constants.conversationList[i].date, null, Constants.conversationList[i], ConversationNew.TYPE_NONE))
                        }
                    }
                    "Yearly" -> {
                        if (dateFormatter.getFormatHeaderConversationYear(Constants.conversationList[i].date) == listDateDuplicates[0]) {
                            conversationList.add(ConversationNew(Constants.conversationList[i].date, null, Constants.conversationList[i], ConversationNew.TYPE_NONE))
                        }
                    }
                    else -> {
                        if (dateFormatter.getWeekOfYear(Constants.conversationList[i].date) == listDateDuplicates[0]) {
                            conversationList.add(ConversationNew(Constants.conversationList[i].date, null, Constants.conversationList[i], ConversationNew.TYPE_NONE))
                        }
                    }
                }
            }

            binding.recyclerView.adapter = conversationAdapterNew
            conversationAdapterNew.setData(conversationList, colors, phoneNumberUtils)
            conversationAdapterNew.notifyDataSetChanged()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.run(onNewIntentIntent::onNext)
    }

    override fun render(state: MainState) {
        if (state.hasError) {
            finish()
            return
        }

        val addContact = when (state.page) {
            is Inbox -> state.page.addContact
            is Archived -> state.page.addContact
            else -> false
        }

        val markPinned = when (state.page) {
            is Inbox -> state.page.markPinned
            is Archived -> state.page.markPinned
            else -> true
        }

        val markRead = when (state.page) {
            is Inbox -> state.page.markRead
            is Archived -> state.page.markRead
            else -> true
        }

        val selectedConversations = when (state.page) {
            is Inbox -> state.page.selected
            is Archived -> state.page.selected
            else -> 0
        }

//        binding.toolbarSearch.setVisible(state.page is Inbox && state.page.selected == 0 || state.page is Searching)
        binding.toolbarTitle.setVisible(binding.toolbarSearch.visibility == View.VISIBLE)

        binding.toolbar.menu.findItem(R.id.archive)?.isVisible = state.page is Inbox && selectedConversations != 0
        binding.toolbar.menu.findItem(R.id.unarchive)?.isVisible = state.page is Archived && selectedConversations != 0
        binding.toolbar.menu.findItem(R.id.delete)?.isVisible = selectedConversations != 0
        binding.toolbar.menu.findItem(R.id.add)?.isVisible = addContact && selectedConversations != 0
        binding.toolbar.menu.findItem(R.id.pin)?.isVisible = markPinned && selectedConversations != 0
        binding.toolbar.menu.findItem(R.id.unpin)?.isVisible = !markPinned && selectedConversations != 0
        binding.toolbar.menu.findItem(R.id.read)?.isVisible = markRead && selectedConversations != 0
        binding.toolbar.menu.findItem(R.id.unread)?.isVisible = !markRead && selectedConversations != 0
        binding.toolbar.menu.findItem(R.id.block)?.isVisible = selectedConversations != 0

        listOf(binding.drawer.plusBadge1, binding.drawer.plusBadge2).forEach { badge ->
            badge.isVisible = drawerBadgesExperiment.variant && !state.upgraded
        }
        binding.drawer.plus.isVisible = state.upgraded
        binding.drawer.plusBanner.isVisible = !state.upgraded
        binding.drawer.rateLayout.setVisible(state.showRating)

        binding.compose.setVisible(state.page is Inbox || state.page is Archived)
        conversationsAdapter.emptyView = binding.empty.takeIf { state.page is Inbox || state.page is Archived }
        searchAdapter.emptyView = binding.empty.takeIf { state.page is Searching }

        when (state.page) {
            is Inbox -> {
                showBackButton(state.page.selected > 0)

                binding.recyclerViewDate.isVisible = true
                binding.bgContent.isVisible = true
                binding.empty.isVisible = false
                binding.cardSelected.isVisible = true

                if (state.page.selected > 0) {
                    title = getString(R.string.main_title_selected, state.page.selected)
                    binding.bgToolbar.isVisible = false
                } else {
                    title = "Messages"
                    binding.bgToolbar.isVisible = true
                }

                if (state.page.selected > 0) {
                    isSelected = 1
                }
//                if (binding.recyclerView.adapter !== conversationsAdapter) binding.recyclerView.adapter = conversationsAdapter
//                conversationsAdapter.updateData(state.page.data)
//                binding.recyclerView.adapter = conversationsAdapter
//                Constants.getModelList(state.page.data)

                if (isSelected == 1 && isMenuItemSelected) {

                    Constants.getModelList(state.page.data, false)
                    val typeSort = SharedPreferenceHelper.getInstance(this).get(Constants.TYPE_SORT)

                    if (conversationList.size > 0) {
                        conversationList.clear()
                    }

                    if (dateList.size > 0) {
                        dateList.clear()
                    }
                    if (Constants.conversationList.size > 0) {
                        for (i in 0 until Constants.conversationList.size) {
                            when (typeSort) {
                                "Dayly" -> {
                                    dateList.add(dateFormatter.getFormatHeaderConversationDay(Constants.conversationList[i].date))
                                }
                                "Monthly" -> {
                                    dateList.add(dateFormatter.getFormatHeaderConversationMonth(Constants.conversationList[i].date))
                                }
                                "Yearly" -> {
                                    dateList.add(dateFormatter.getFormatHeaderConversationYear(Constants.conversationList[i].date))
                                }
                                else -> {
                                    dateList.add(dateFormatter.getWeekOfYear(Constants.conversationList[i].date))
                                }
                            }
                        }

                        //remove duplicate
                        val hashSet: LinkedHashSet<String> = LinkedHashSet(dateList)
                        val listDateDuplicates = ArrayList(hashSet)

                        //
                        val headerDateAdapter = DateHeaderConversationAdapter(this, listDateDuplicates, this)
                        binding.recyclerViewDate.adapter = headerDateAdapter

                        headerDateAdapter.setIndex(isCurrentPositionDate)
//                    binding.recyclerViewDate.scrollToPosition(isCurrentPositionDate)

                        onSelectedCurrent(typeSort, isCurrentDate)
                    }
                } else if (isSelected == 0) {
                    if (isCurrentDate.isNotEmpty()) {
                        Constants.getModelList(state.page.data, false)
                        val typeSort = SharedPreferenceHelper.getInstance(this).get(Constants.TYPE_SORT)

                        if (conversationList.size > 0) {
                            conversationList.clear()
                        }

                        if (dateList.size > 0) {
                            dateList.clear()
                        }
                        if (Constants.conversationList.size > 0) {
                            for (i in 0 until Constants.conversationList.size) {
                                when (typeSort) {
                                    "Dayly" -> {
                                        dateList.add(dateFormatter.getFormatHeaderConversationDay(Constants.conversationList[i].date))
                                    }
                                    "Monthly" -> {
                                        dateList.add(dateFormatter.getFormatHeaderConversationMonth(Constants.conversationList[i].date))
                                    }
                                    "Yearly" -> {
                                        dateList.add(dateFormatter.getFormatHeaderConversationYear(Constants.conversationList[i].date))
                                    }
                                    else -> {
                                        dateList.add(dateFormatter.getWeekOfYear(Constants.conversationList[i].date))
                                    }
                                }
                            }

                            //remove duplicate
                            val hashSet: LinkedHashSet<String> = LinkedHashSet(dateList)
                            val listDateDuplicates = ArrayList(hashSet)

                            //
                            val headerDateAdapter = DateHeaderConversationAdapter(this, listDateDuplicates, this)
                            binding.recyclerViewDate.adapter = headerDateAdapter

                            headerDateAdapter.setIndex(isCurrentPositionDate)
//                        binding.recyclerViewDate.scrollToPosition(isCurrentPositionDate)

                            onSelectedCurrent(typeSort, isCurrentDate)
                        }
                    } else {
                        Constants.getModelList(state.page.data, false)
                        SharedPreferenceHelper.getInstance(this).set(Constants.TYPE_SORT, "Weekly")
                        binding.typeSelected.text = SharedPreferenceHelper.getInstance(this).get(Constants.TYPE_SORT)

                        if (conversationList.size > 0) {
                            conversationList.clear()
                        }

                        if (dateList.size > 0) {
                            dateList.clear()
                        }
                        if (Constants.conversationList.size > 0) {
                            for (i in 0 until Constants.conversationList.size) {
                                dateList.add(dateFormatter.getWeekOfYear(Constants.conversationList[i].date))
                            }

                            //remove duplicate
                            val hashSet: LinkedHashSet<String> = LinkedHashSet(dateList)
                            val listDateDuplicates = ArrayList(hashSet)

                            //
                            val headerDateAdapter = DateHeaderConversationAdapter(this, listDateDuplicates, this)
                            binding.recyclerViewDate.adapter = headerDateAdapter

                            for (i in 0 until Constants.conversationList.size) {
                                if (dateFormatter.getWeekOfYear(Constants.conversationList[i].date) == listDateDuplicates[0]) {
                                    isCurrentDate = listDateDuplicates[0].toString()
                                    conversationList.add(ConversationNew(Constants.conversationList[i].date, null, Constants.conversationList[i], ConversationNew.TYPE_NONE))
                                }
                            }

                            if (binding.recyclerView.adapter !== conversationAdapterNew) binding.recyclerView.adapter = conversationAdapterNew
//                    binding.recyclerView.adapter = conversationAdapterNew
                            conversationAdapterNew.setData(conversationList, colors, phoneNumberUtils)
                            conversationAdapterNew.notifyDataSetChanged()
                        }
                    }
                }

//                itemTouchHelper.attachToRecyclerView(binding.recyclerView)
                binding.empty.setText(R.string.inbox_empty_text)
                binding.cardListConversation.isVisible = !binding.empty.isVisible
            }

            is Searching -> {
                showBackButton(true)
                if (binding.recyclerView.adapter !== searchAdapter) binding.recyclerView.adapter = searchAdapter
                searchAdapter.data = state.page.data ?: listOf()
//                itemTouchHelper.attachToRecyclerView(null)
                binding.empty.setText(R.string.inbox_search_empty_text)
            }

            is Archived -> {
                showBackButton(state.page.selected > 0)
                title = when (state.page.selected != 0) {
                    true -> getString(R.string.main_title_selected, state.page.selected)
                    false -> getString(R.string.title_archived)
                }

                binding.bgToolbar.isVisible = state.page.selected <= 0

                if (state.page.selected > 0) {
                    isSelectedArchived = 1
                }
//                if (binding.recyclerView.adapter !== conversationsAdapter) binding.recyclerView.adapter = conversationsAdapter
//                conversationsAdapter.updateData(state.page.data)
//                binding.recyclerView.adapter = conversationsAdapter
//                Constants.getModelList(state.page.data)

                if (isSelectedArchived == 1 && isMenuItemSelectedArchived) {

                    Constants.getModelList(state.page.data, true)
                    val typeSort = SharedPreferenceHelper.getInstance(this).get(Constants.TYPE_SORT)

                    if (conversationList.size > 0) {
                        conversationList.clear()
                    }

                    if (dateList.size > 0) {
                        dateList.clear()
                    }
                    if (Constants.conversationList.size > 0) {
                        for (i in 0 until Constants.conversationList.size) {
                            when (typeSort) {
                                "Dayly" -> {
                                    dateList.add(dateFormatter.getFormatHeaderConversationDay(Constants.conversationList[i].date))
                                }
                                "Monthly" -> {
                                    dateList.add(dateFormatter.getFormatHeaderConversationMonth(Constants.conversationList[i].date))
                                }
                                "Yearly" -> {
                                    dateList.add(dateFormatter.getFormatHeaderConversationYear(Constants.conversationList[i].date))
                                }
                                else -> {
                                    dateList.add(dateFormatter.getWeekOfYear(Constants.conversationList[i].date))
                                }
                            }
                        }

                        //remove duplicate
                        val hashSet: LinkedHashSet<String> = LinkedHashSet(dateList)
                        val listDateDuplicates = ArrayList(hashSet)

                        //
                        val headerDateAdapter = DateHeaderConversationAdapter(this, listDateDuplicates, this)
                        binding.recyclerViewDate.adapter = headerDateAdapter

                        headerDateAdapter.setIndex(isCurrentPositionDate)
//                    binding.recyclerViewDate.scrollToPosition(isCurrentPositionDate)

                        onSelectedCurrent(typeSort, isCurrentDate)
                    } else {
                        binding.recyclerViewDate.isVisible = false
                        binding.bgContent.isVisible = false
                        binding.empty.isVisible = true
                        binding.empty.setText(R.string.inbox_empty_text)
                        binding.cardSelected.isVisible = false
                    }
                } else if (isSelectedArchived == 0) {
                    if (isCurrentDate.isNotEmpty()) {
                        Constants.getModelList(state.page.data, true)
                        val typeSort = SharedPreferenceHelper.getInstance(this).get(Constants.TYPE_SORT)

                        if (conversationList.size > 0) {
                            conversationList.clear()
                        }

                        if (dateList.size > 0) {
                            dateList.clear()
                        }
                        if (Constants.conversationList.size > 0) {
                            for (i in 0 until Constants.conversationList.size) {
                                when (typeSort) {
                                    "Dayly" -> {
                                        dateList.add(dateFormatter.getFormatHeaderConversationDay(Constants.conversationList[i].date))
                                    }
                                    "Monthly" -> {
                                        dateList.add(dateFormatter.getFormatHeaderConversationMonth(Constants.conversationList[i].date))
                                    }
                                    "Yearly" -> {
                                        dateList.add(dateFormatter.getFormatHeaderConversationYear(Constants.conversationList[i].date))
                                    }
                                    else -> {
                                        dateList.add(dateFormatter.getWeekOfYear(Constants.conversationList[i].date))
                                    }
                                }
                            }

                            //remove duplicate
                            val hashSet: LinkedHashSet<String> = LinkedHashSet(dateList)
                            val listDateDuplicates = ArrayList(hashSet)

                            //
                            val headerDateAdapter = DateHeaderConversationAdapter(this, listDateDuplicates, this)
                            binding.recyclerViewDate.adapter = headerDateAdapter

                            headerDateAdapter.setIndex(isCurrentPositionDate)
//                        binding.recyclerViewDate.scrollToPosition(isCurrentPositionDate)

                            onSelectedCurrent(typeSort, isCurrentDate)
                        }else {
                            binding.recyclerViewDate.isVisible = false
                            binding.bgContent.isVisible = false
                            binding.empty.isVisible = true
                            binding.empty.setText(R.string.inbox_empty_text)
                            binding.cardSelected.isVisible = false
                        }
                    } else {
                        Constants.getModelList(state.page.data, true)
                        SharedPreferenceHelper.getInstance(this).set(Constants.TYPE_SORT, "Weekly")
                        binding.typeSelected.text = SharedPreferenceHelper.getInstance(this).get(Constants.TYPE_SORT)

                        if (conversationList.size > 0) {
                            conversationList.clear()
                        }

                        if (dateList.size > 0) {
                            dateList.clear()
                        }
                        if (Constants.conversationList.size > 0) {
                            for (i in 0 until Constants.conversationList.size) {
                                dateList.add(dateFormatter.getWeekOfYear(Constants.conversationList[i].date))
                            }

                            //remove duplicate
                            val hashSet: LinkedHashSet<String> = LinkedHashSet(dateList)
                            val listDateDuplicates = ArrayList(hashSet)

                            //
                            val headerDateAdapter = DateHeaderConversationAdapter(this, listDateDuplicates, this)
                            binding.recyclerViewDate.adapter = headerDateAdapter

                            for (i in 0 until Constants.conversationList.size) {
                                if (dateFormatter.getWeekOfYear(Constants.conversationList[i].date) == listDateDuplicates[0]) {
                                    isCurrentDate = listDateDuplicates[0].toString()
                                    conversationList.add(ConversationNew(Constants.conversationList[i].date, null, Constants.conversationList[i], ConversationNew.TYPE_NONE))
                                }
                            }

                            if (binding.recyclerView.adapter !== conversationAdapterNew) binding.recyclerView.adapter = conversationAdapterNew
//                    binding.recyclerView.adapter = conversationAdapterNew
                            conversationAdapterNew.setData(conversationList, colors, phoneNumberUtils)
                            conversationAdapterNew.notifyDataSetChanged()
                        }else {
                            binding.recyclerViewDate.isVisible = false
                            binding.bgContent.isVisible = false
                            binding.empty.isVisible = true
                            binding.empty.setText(R.string.inbox_empty_text)
                            binding.cardSelected.isVisible = false
                        }
                    }
                }

//                itemTouchHelper.attachToRecyclerView(binding.recyclerView)
                binding.empty.setText(R.string.inbox_empty_text)
                binding.cardListConversation.isVisible = !binding.empty.isVisible

            }
        }

        binding.drawer.inbox.isActivated = state.page is Inbox
        binding.drawer.archived.isActivated = state.page is Archived

        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START) && !state.drawerOpen) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else if (!binding.drawerLayout.isDrawerVisible(GravityCompat.START) && state.drawerOpen) {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        when (state.syncing) {
            is SyncRepository.SyncProgress.Idle -> {
                syncing.isVisible = false
                snackbar.isVisible = !state.defaultSms || !state.smsPermission || !state.contactPermission
            }

            is SyncRepository.SyncProgress.Running -> {
                syncing.isVisible = true
                binding.syncing.progress.max = state.syncing.max
                progressAnimator.apply { setIntValues(binding.syncing.progress.progress, state.syncing.progress) }.start()
                binding.syncing.progress.isIndeterminate = state.syncing.indeterminate
                snackbar.isVisible = false
            }
        }

        when {
            !state.defaultSms -> {
                binding.snackbar.title.setText(R.string.main_default_sms_title)
                binding.snackbar.message.setText(R.string.main_default_sms_message)
                binding.snackbar.button.setText(R.string.main_default_sms_change)
            }

            !state.smsPermission -> {
                binding.snackbar.title.setText(R.string.main_permission_required)
                binding.snackbar.message.setText(R.string.main_permission_sms)
                binding.snackbar.button.setText(R.string.main_permission_allow)
            }

            !state.contactPermission -> {
                binding.snackbar.title.setText(R.string.main_permission_required)
                binding.snackbar.message.setText(R.string.main_permission_contacts)
                binding.snackbar.button.setText(R.string.main_permission_allow)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activityResumedIntent.onNext(true)
    }

    override fun onPause() {
        super.onPause()
        activityResumedIntent.onNext(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    override fun showBackButton(show: Boolean) {
        toggle.onDrawerSlide(binding.drawer.root, if (show) 1f else 0f)
        toggle.drawerArrowDrawable.color = when (show) {
            true -> resolveThemeColor(android.R.attr.textColorSecondary)
            false -> resolveThemeColor(android.R.attr.textColorPrimary)
        }
    }

    override fun requestDefaultSms() {
        navigator.showDefaultSmsDialog(this)
    }

    override fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_CONTACTS), 0)
    }

    override fun clearSearch() {
        dismissKeyboard()
        binding.toolbarSearch.text = null
    }

    override fun clearSelection() {
        conversationAdapterNew.clearSelection()
        isMenuItemSelected = false
        isMenuItemSelectedArchived = false
    }

    override fun themeChanged() {
        binding.recyclerView.scrapViews()
    }

    override fun showBlockingDialog(conversations: List<Long>, block: Boolean) {
        blockingDialog.show(this, conversations, block)
    }

    override fun showDeleteDialog(conversations: List<Long>) {
        val count = conversations.size
        AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(resources.getQuantityString(R.plurals.dialog_delete_message, count, count))
                .setPositiveButton(R.string.button_delete) { _, _ -> confirmDeleteIntent.onNext(conversations) }
                .setNegativeButton(R.string.button_cancel, null)
                .show()
    }

    override fun showChangelog(changelog: ChangelogManager.Changelog) {
        changelogDialog.show(changelog)
    }

    override fun showArchivedSnackbar() {
        Snackbar.make(binding.drawerLayout, R.string.toast_archived, Snackbar.LENGTH_LONG).apply {
            setAction(R.string.button_undo) { undoArchiveIntent.onNext(Unit) }
            setActionTextColor(colors.theme().theme)
            show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        optionsItemIntent.onNext(item.itemId)
        isMenuItemSelected = true
        isMenuItemSelectedArchived = true

        return true
    }

    override fun onBackPressed() {
        backPressedSubject.onNext(NavItem.BACK)
    }

    override fun onSelected(position: Int, date: String?) {
        isCurrentDate = date.toString()
        isCurrentPositionDate = position

        if (conversationList.size > 0) {
            conversationList.clear()
        }

//        object : AsyncTask<Void?, Void?, Void?>() {
//            override fun doInBackground(vararg params: Void?): Void? {
                for (i in 0 until Constants.conversationList.size) {
                    when (SharedPreferenceHelper.getInstance(this@MainActivity).get(Constants.TYPE_SORT)) {
                        "Dayly" -> {
                            if (dateFormatter.getFormatHeaderConversationDay(Constants.conversationList[i].date) == date) {
                                conversationList.add(ConversationNew(Constants.conversationList[i].date, null, Constants.conversationList[i], ConversationNew.TYPE_NONE))
                            }
                        }
                        "Monthly" -> {
                            if (dateFormatter.getFormatHeaderConversationMonth(Constants.conversationList[i].date) == date) {
                                conversationList.add(ConversationNew(Constants.conversationList[i].date, null, Constants.conversationList[i], ConversationNew.TYPE_NONE))
                            }
                        }
                        "Yearly" -> {
                            if (dateFormatter.getFormatHeaderConversationYear(Constants.conversationList[i].date) == date) {
                                conversationList.add(ConversationNew(Constants.conversationList[i].date, null, Constants.conversationList[i], ConversationNew.TYPE_NONE))
                            }
                        }
                        else -> {
                            if (dateFormatter.getWeekOfYear(Constants.conversationList[i].date) == date) {
                                conversationList.add(ConversationNew(Constants.conversationList[i].date, null, Constants.conversationList[i], ConversationNew.TYPE_NONE))
                            }
                        }
                    }
                }
//                return null
//            }

//            override fun onPostExecute(result: Void?) {
//                super.onPostExecute(result)
                binding.recyclerView.adapter = conversationAdapterNew
                conversationAdapterNew.setData(conversationList, colors, phoneNumberUtils)
                conversationAdapterNew.notifyDataSetChanged()
//            }
//        }.execute()
    }


    private fun onSelectedCurrent(type: String, date: String?) {
        if (conversationList.size > 0) {
            conversationList.clear()
        }

//        @SuppressLint ("StaticFieldLeak")
//        object : AsyncTask<Void?, Void?, Void?>() {
//            override fun doInBackground(vararg params: Void?): Void? {
                if (Constants.conversationList.size > 0) {
                    for (i in 0 until Constants.conversationList.size) {
                        when (type) {
                            "Dayly" -> {
                                if (dateFormatter.getFormatHeaderConversationDay(Constants.conversationList[i].date) == date) {
                                    conversationList.add(ConversationNew(Constants.conversationList[i].date, null, Constants.conversationList[i], ConversationNew.TYPE_NONE))
                                }
                            }
                            "Monthly" -> {
                                if (dateFormatter.getFormatHeaderConversationMonth(Constants.conversationList[i].date) == date) {
                                    conversationList.add(ConversationNew(Constants.conversationList[i].date, null, Constants.conversationList[i], ConversationNew.TYPE_NONE))
                                }
                            }
                            "Yearly" -> {
                                if (dateFormatter.getFormatHeaderConversationYear(Constants.conversationList[i].date) == date) {
                                    conversationList.add(ConversationNew(Constants.conversationList[i].date, null, Constants.conversationList[i], ConversationNew.TYPE_NONE))
                                }
                            }
                            else -> {
                                if (dateFormatter.getWeekOfYear(Constants.conversationList[i].date) == date) {
                                    conversationList.add(ConversationNew(Constants.conversationList[i].date, null, Constants.conversationList[i], ConversationNew.TYPE_NONE))
                                }
                            }
                        }
                    }
                }
//                return null
//            }

//            override fun onPostExecute(result: Void?) {
//                super.onPostExecute(result)
//                binding.recyclerView.adapter = conversationAdapterNew
                if (binding.recyclerView.adapter !== conversationAdapterNew) binding.recyclerView.adapter = conversationAdapterNew
                conversationAdapterNew.setData(conversationList, colors, phoneNumberUtils)
                conversationAdapterNew.notifyDataSetChanged()
//            }
//        }.execute()
    }
}
