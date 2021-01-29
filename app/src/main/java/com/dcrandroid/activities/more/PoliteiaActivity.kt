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
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.adapter.ProposalAdapter
import com.dcrandroid.data.Proposal
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
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
        AdapterView.OnItemSelectedListener, ViewTreeObserver.OnScrollChangedListener {

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
            finish()
        }

        refreshAvailableProposalCategories()
        setSyncingState()
    }

    override fun onStop() {
        super.onStop()
        multiWallet!!.politeia.removeNotificationListener(this.javaClass.name)
    }

    private fun setSyncingState() = GlobalScope.launch(Dispatchers.Main) {
        if (multiWallet!!.politeia.isSyncing) {
            sync_icon.animation = rotateAnim
            sync_icon.show()
        } else {
            sync_icon.animation = null
            sync_icon.hide()
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
        if (proposals.size > 0) {
            recycler_view_container.visibility = View.VISIBLE
        } else {
            recycler_view_container.visibility = View.GONE
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
        setSyncingState()
    }

    override fun onNewProposal(proposal: dcrlibwallet.Proposal) {
    }

    override fun onProposalVoteStarted(proposal: dcrlibwallet.Proposal) {
    }

    override fun onProposalVoteFinished(proposal: dcrlibwallet.Proposal) {
    }
}