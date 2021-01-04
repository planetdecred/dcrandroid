package com.dcrandroid.activities.more

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.adapter.ProposalAdapter
import com.dcrandroid.data.Proposal
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.util.Deserializer
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.Utils
import com.google.gson.GsonBuilder
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.ProposalNotificationListener
import kotlinx.android.synthetic.main.activity_politeia.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList

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

    private val gson = GsonBuilder().registerTypeHierarchyAdapter(ArrayList::class.java, Deserializer.ProposalDeserializer()).create()

    private var newestProposalsFirst = true
    private var loadedAll = false

    private val loading = AtomicBoolean(false)
    private val initialLoadingDone = AtomicBoolean(false)

    private val availableProposalTypes = ArrayList<String>()

    private var categorySortAdapter: ArrayAdapter<String>? = null
    private var currentCategory: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_politeia)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
        currentCategory = category_sort_spinner.selectedItemPosition
        category_sort_spinner.adapter = categorySortAdapter

        category_sort_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                currentCategory = position
                val proposalCategory = when (currentCategory) {
                    ProposalCategoryPre -> Dcrlibwallet.ProposalCategoryPre
                    ProposalCategoryActive -> Dcrlibwallet.ProposalCategoryActive
                    ProposalCategoryApproved -> Dcrlibwallet.ProposalCategoryApproved
                    ProposalCategoryRejected -> Dcrlibwallet.ProposalCategoryRejected
                    else -> Dcrlibwallet.ProposalCategoryAbandoned
                }
                loadProposals(true, proposalCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        go_back.setOnClickListener {
            finish()
        }

        sync_layout.setOnClickListener {
            if (!multiWallet!!.isConnectedToDecredNetwork) {
                SnackBar.showError(this, R.string.not_connected)
                return@setOnClickListener
            }
            syncProposals()
        }

        refreshAvailableProposalCategories()
    }

    private fun syncProposals() = GlobalScope.launch(Dispatchers.Default) {
        try {
            SnackBar.showText(this@PoliteiaActivity, R.string.syncing_proposals)
            multiWallet!!.politeia.sync()
        } catch (e: Exception) {
            e.printStackTrace()
            SnackBar.showError(this@PoliteiaActivity, R.string.error_syncying_proposals)
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
            recycler_view.stopScroll()
            val proposalCategory = when (currentCategory) {
                ProposalCategoryPre -> Dcrlibwallet.ProposalCategoryPre
                ProposalCategoryActive -> Dcrlibwallet.ProposalCategoryActive
                ProposalCategoryApproved -> Dcrlibwallet.ProposalCategoryApproved
                ProposalCategoryRejected -> Dcrlibwallet.ProposalCategoryRejected
                else -> Dcrlibwallet.ProposalCategoryAbandoned
            }
            loadProposals(true, proposalCategory)
        }
    }

    private fun loadProposals(loadMore: Boolean = false, proposalCategory: Int) = GlobalScope.launch(Dispatchers.Default) {
        withContext(Dispatchers.Main) {
            //show loading view
            swipe_refresh_layout.isRefreshing = true
        }

        if (loading.get()) {
            return@launch
        }

        loading.set(true)
        val limit = 10
        val offset = when {
            loadMore -> proposals.size
            else -> 0
        }

        val jsonResult = multiWallet!!.politeia.getProposals(proposalCategory, offset, limit, newestProposalsFirst)

        // Check if the result object from the json response is null
        val resultObject = JSONObject(jsonResult).get("result")
        val resultObjectString = resultObject.toString()
        val tempProposalList = if (resultObjectString == "null") {
            gson.fromJson("[]", Array<Proposal>::class.java)
        } else {
            val resultArray = JSONObject(jsonResult).getJSONArray("result")
            val resultArrayString = resultArray.toString()
            gson.fromJson(resultArrayString, Array<Proposal>::class.java)
        }

        initialLoadingDone.set(true)

        if (tempProposalList == null) {
            loadedAll = true
            loading.set(false)
            // hide loading view
            withContext(Dispatchers.Main) {
                swipe_refresh_layout.isRefreshing = false
                if (proposals.size > 0) {
                    swipe_refresh_layout.visibility = View.VISIBLE
                } else {
                    swipe_refresh_layout.visibility = View.GONE
                }
            }

            if (!loadMore) {
                proposals.clear()
            }
            return@launch
        }

        if (tempProposalList.size < limit) {
            loadedAll = true
        }

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

            checkEmptyProposalList("")

            withContext(Dispatchers.Main) {
                proposalAdapter?.notifyDataSetChanged()
            }
        }

        loading.set(false)
        withContext(Dispatchers.Main) {
            swipe_refresh_layout.isRefreshing = false
            if (proposals.size > 0) {
                swipe_refresh_layout.visibility = View.VISIBLE
            } else {
                swipe_refresh_layout.visibility = View.GONE
            }
        }

    }

    private fun checkEmptyProposalList(status: String) = GlobalScope.launch(Dispatchers.Main) {
        if (proposals.size > 0) {
            swipe_refresh_layout?.show()
        } else {
            swipe_refresh_layout?.hide()
            empty_list.text = String.format(Locale.getDefault(), "No %s proposals", status)
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
        val proposalCategory = when (currentCategory) {
            ProposalCategoryPre -> Dcrlibwallet.ProposalCategoryPre
            ProposalCategoryActive -> Dcrlibwallet.ProposalCategoryActive
            ProposalCategoryApproved -> Dcrlibwallet.ProposalCategoryApproved
            ProposalCategoryRejected -> Dcrlibwallet.ProposalCategoryRejected
            else -> Dcrlibwallet.ProposalCategoryAbandoned
        }

        loadProposals(false, proposalCategory)
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

                val proposalCategory = when (currentCategory) {
                    ProposalCategoryPre -> Dcrlibwallet.ProposalCategoryPre
                    ProposalCategoryActive -> Dcrlibwallet.ProposalCategoryActive
                    ProposalCategoryApproved -> Dcrlibwallet.ProposalCategoryApproved
                    ProposalCategoryRejected -> Dcrlibwallet.ProposalCategoryRejected
                    else -> Dcrlibwallet.ProposalCategoryAbandoned
                }
                loadProposals(false, proposalCategory)
            }
        }
    }

    override fun onNewProposal(proposalID: Long, token: String?) {
        Utils.sendProposalNotification(this, notificationManager, proposalID, getString(R.string.new_proposal), token!!)
    }

    override fun onProposalVoteStarted(proposalID: Long, token: String?) {
        Utils.sendProposalNotification(this, notificationManager, proposalID, getString(R.string.vote_started), token!!)
    }

    override fun onProposalVoteFinished(proposalID: Long, token: String?) {
        Utils.sendProposalNotification(this, notificationManager, proposalID, getString(R.string.vote_ended), token!!)
    }
}