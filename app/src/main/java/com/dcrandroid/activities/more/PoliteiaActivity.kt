package com.dcrandroid.activities.more

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.adapter.ProposalAdapter
import com.dcrandroid.data.Proposal
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.Utils
import com.google.gson.Gson
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.ProposalNotificationListener
import kotlinx.android.synthetic.main.activity_politeia.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

// Corresponding to spinner item position
const val ProposalCategoryPre = 0
const val ProposalCategoryActive = 1
const val ProposalCategoryApproved = 2
const val ProposalCategoryRejected = 3
const val ProposalCategoryAbandoned = 4

class PoliteiaActivity : BaseActivity(), ProposalNotificationListener,
        SwipeRefreshLayout.OnRefreshListener, AdapterView.OnItemSelectedListener, ViewTreeObserver.OnScrollChangedListener {

    private lateinit var notificationManager: NotificationManager

    private var proposals = ArrayList<Proposal>()
    private var proposalAdapter: ProposalAdapter? = null
    private var layoutManager: LinearLayoutManager? = null
    private var newestProposalsFirst = true
    private var loadedAll = false

    private val loading = AtomicBoolean(false)
    private val initialLoadingDone = AtomicBoolean(false)

    private val availableProposalTypes = ArrayList<String>()

    private var categorySortAdapter: ArrayAdapter<String>? = null
    private val currentCategory: Int
    get() {
       return when (category_sort_spinner.selectedItemPosition) {
            ProposalCategoryPre -> Dcrlibwallet.ProposalCategoryPre
            ProposalCategoryActive -> Dcrlibwallet.ProposalCategoryActive
            ProposalCategoryApproved -> Dcrlibwallet.ProposalCategoryApproved
            ProposalCategoryRejected -> Dcrlibwallet.ProposalCategoryRejected
            else -> Dcrlibwallet.ProposalCategoryAbandoned
        }
    }

    private lateinit var rotateAnim: Animation

    override fun onResume() {
        super.onResume()
        multiWallet!!.politeia.addNotificationListener(this, this.javaClass.name)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_politeia)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        rotateAnim = AnimationUtils.loadAnimation(this@PoliteiaActivity, R.anim.rotate)
        swipe_refresh_layout.setOnRefreshListener(this)

        layoutManager = LinearLayoutManager(this)
        proposalAdapter = ProposalAdapter(proposals, this)
        recycler_view.layoutManager = layoutManager
        recycler_view.adapter = proposalAdapter
        recycler_view.viewTreeObserver.addOnScrollChangedListener(this)

        val timestampSortItems = resources.getStringArray(R.array.timestamp_sort)
        val timestampSortAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, timestampSortItems)
        timestampSortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timestamp_sort_spinner.onItemSelectedListener = this
        timestamp_sort_spinner.adapter = timestampSortAdapter

        categorySortAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableProposalTypes)
        categorySortAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        category_sort_spinner.adapter = categorySortAdapter
        category_sort_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                loadProposals(false)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        go_back.setOnClickListener {
            multiWallet!!.politeia.clearSavedProposals()
            finish()
        }

        sync_layout.setOnClickListener {
            if (!multiWallet!!.isConnectedToDecredNetwork) {
                SnackBar.showError(this, R.string.not_connected)
                return@setOnClickListener
            }
            if(multiWallet!!.politeia.isConnected){
                multiWallet!!.politeia.stopSync()
                setSyncButtonState()
            }else{
                syncProposals()
            }

        }

        refreshAvailableProposalCategories()
        setSyncButtonState()
    }

    override fun onStop() {
        super.onStop()
        multiWallet!!.politeia.removeNotificationListener(this.javaClass.name)
    }

    private fun syncProposals() = GlobalScope.launch(Dispatchers.Default) {
        try {
            withContext(Dispatchers.Main){
                tv_sync_label.setText(R.string.stop_sync)
                sync_icon.animation = rotateAnim
            }
            SnackBar.showText(this@PoliteiaActivity, R.string.syncing_proposals)
            multiWallet!!.politeia.sync()
        } catch (e: Exception) {
            e.printStackTrace()
            if (e.message == Dcrlibwallet.ErrContextCanceled) {
                SnackBar.showError(this@PoliteiaActivity, R.string.politeia_sync_stopped)
            }else{
                SnackBar.showError(this@PoliteiaActivity, R.string.error_syncying_proposals)
            }
            setSyncButtonState()
        }
    }

    private fun setSyncButtonState() = GlobalScope.launch(Dispatchers.Main){
        if(multiWallet!!.politeia.isConnected){
            tv_sync_label.setText(R.string.stop_sync)

            if(multiWallet!!.politeia.synced()) {
                sync_icon.animation = null
            }else {
                sync_icon.animation = rotateAnim
            }
        }else{
            tv_sync_label.setText(R.string.sync)
            sync_icon.animation = null
        }
    }

    override fun onScrollChanged() {
        if (proposals.size < 5 || !initialLoadingDone.get()) return

        val firstVisibleItem = layoutManager!!.findFirstCompletelyVisibleItemPosition()
        if (firstVisibleItem != 0) {
            proposals_page_header.elevation = resources.getDimension(R.dimen.app_bar_elevation)
            app_bar.elevation = resources.getDimension(R.dimen.app_bar_elevation)
        } else {
            proposals_page_header.elevation = 0f
            app_bar.elevation = 0f
        }

        val lastVisibleItem = layoutManager!!.findLastCompletelyVisibleItemPosition()
        if (lastVisibleItem >= proposals.size - 1) {
            if (!loadedAll) {
                recycler_view.stopScroll()
                loadProposals(true)
            }
        }
    }

    private fun preLoadTasks() = GlobalScope.launch(Dispatchers.Main) {
        loading.set(true)
        swipe_refresh_layout.isRefreshing = true
    }

    private fun loadProposals(loadMore: Boolean = false) = GlobalScope.launch(Dispatchers.Default) {

        if (loading.get()) {
            return@launch
        }
        preLoadTasks()

        val limit = 10
        val offset = when {
            loadMore -> proposals.size
            else -> 0
        }

        try {
            val jsonResult = multiWallet!!.politeia.getProposals(currentCategory, offset, limit, newestProposalsFirst)
            val tempProposalList = Gson().fromJson(jsonResult, Array<Proposal>::class.java)

            initialLoadingDone.set(true)
            if (tempProposalList.isEmpty()) {
                loadedAll = true
                postLoadTasks()

                if (!loadMore) {
                    proposals.clear()
                }
                return@launch
            }

            loadedAll = tempProposalList.size < limit

            if (loadMore) {
                val positionStart = proposals.size
                proposals.addAll(tempProposalList)
                withContext(Dispatchers.Main) {
                    proposalAdapter?.notifyItemRangeInserted(positionStart, tempProposalList.size)

                    // notify previous last item to remove bottom margin
                    proposalAdapter?.notifyItemChanged(positionStart - 1)
                }

            } else {
                proposals.let {
                    it.clear()
                    it.addAll(tempProposalList)
                }

                withContext(Dispatchers.Main) {
                    proposalAdapter?.notifyDataSetChanged()
                }
            }

            postLoadTasks()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun postLoadTasks() = GlobalScope.launch(Dispatchers.Main) {
        loading.set(false)
        swipe_refresh_layout.isRefreshing = false
        if (proposals.size > 0) {
            swipe_refresh_layout.visibility = View.VISIBLE
        } else {
            swipe_refresh_layout.visibility = View.GONE
        }
    }

    private fun refreshAvailableProposalCategories() = GlobalScope.launch(Dispatchers.Default) {
        availableProposalTypes.clear()

        val preCount = multiWallet!!.politeia.count(Dcrlibwallet.ProposalCategoryPre)
        val activeCount = multiWallet!!.politeia.count(Dcrlibwallet.ProposalCategoryActive)
        val approvedCount = multiWallet!!.politeia.count(Dcrlibwallet.ProposalCategoryApproved)
        val rejectedCount = multiWallet!!.politeia.count(Dcrlibwallet.ProposalCategoryRejected)
        val abandonedCount = multiWallet!!.politeia.count(Dcrlibwallet.ProposalCategoryAbandoned)

        withContext(Dispatchers.Main) {
            availableProposalTypes.add(getString(R.string.proposal_in_discussion, preCount))
            availableProposalTypes.add(getString(R.string.proposal_active, activeCount))
            availableProposalTypes.add(getString(R.string.proposal_approved, approvedCount))
            availableProposalTypes.add(getString(R.string.proposal_rejected, rejectedCount))
            availableProposalTypes.add(getString(R.string.proposal_abandoned, abandonedCount))

            categorySortAdapter?.notifyDataSetChanged()
        }
    }

    override fun onRefresh() {
        loadProposals(false)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

        if (!initialLoadingDone.get()) {
            return
        }

        if (parent!!.id == R.id.timestamp_sort_spinner) {
            val newestFirst = position == 0 // "Newest" is the first item
            if (newestFirst != newestProposalsFirst) {
                newestProposalsFirst = newestFirst
                loadProposals(false)
            }
        }
    }

    override fun onProposalsSynced() {
        refreshAvailableProposalCategories()
        loadProposals()
        setSyncButtonState()
    }

    override fun onNewProposal(proposal: dcrlibwallet.Proposal) {
        Utils.sendProposalNotification(this, notificationManager, proposal, getString(R.string.new_proposal))
    }

    override fun onProposalVoteStarted(proposal: dcrlibwallet.Proposal) {
        Utils.sendProposalNotification(this, notificationManager, proposal, getString(R.string.vote_started))
    }

    override fun onProposalVoteFinished(proposal: dcrlibwallet.Proposal) {
        Utils.sendProposalNotification(this, notificationManager, proposal, getString(R.string.vote_ended))
    }
}