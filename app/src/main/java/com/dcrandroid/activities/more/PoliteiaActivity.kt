package com.dcrandroid.activities.more

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.adapter.ProposalAdapter
import com.dcrandroid.data.Constants
import com.dcrandroid.data.Proposal
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.util.Deserializer
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_politeia.*
import kotlinx.android.synthetic.main.activity_politeia.recycler_view
import kotlinx.android.synthetic.main.activity_politeia.transactions_page_header
import kotlinx.android.synthetic.main.single_wallet_transactions_page.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList


class PoliteiaActivity : BaseActivity(), SwipeRefreshLayout.OnRefreshListener, OnItemSelectedListener, ViewTreeObserver.OnScrollChangedListener  {
    private var proposals = ArrayList<Proposal>()
    private var recyclerView: RecyclerView? = null
    private var proposalAdapter: ProposalAdapter? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var layoutManager: LinearLayoutManager? = null
    private lateinit var emptyList: TextView

    private val gson = GsonBuilder().registerTypeHierarchyAdapter(ArrayList::class.java, Deserializer.ProposalDeserializer()).create()

    private var loading1 = true
    private var newestProposalsFirst = true
    private var loadedAll = false
    private val loading = AtomicBoolean(false)
    private val initialLoadingDone = AtomicBoolean(false)

    private var pastVisibleItems: Int = 0
    private var visibleItemCount: Int = 0
    private var totalItemCount: Int = 0

    private val availableProposalTypes = ArrayList<String>()

    private var categorySortAdapter: ArrayAdapter<String>? = null

//    private val progressBar: ProgressBar
    private var progressStatus = 0
    private lateinit var textView: TextView
    private var selectionCurrent: Int = 0
    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_politeia)

        recyclerView = findViewById(R.id.recycler_view)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        emptyList = findViewById(R.id.empty_list)
        swipeRefreshLayout!!.setOnRefreshListener(this)

        layoutManager = LinearLayoutManager(this)
        recyclerView!!.layoutManager = layoutManager
        proposalAdapter = ProposalAdapter(proposals, this)
        recyclerView!!.adapter = proposalAdapter
        recyclerView!!.viewTreeObserver.addOnScrollChangedListener(this)


//        progressBar = findViewById(R.id.progressBar)
        textView = findViewById(R.id.textView)
//        Thread(Runnable {
//            while (progressStatus < 100) {
//                loading_view.visibility = View.VISIBLE
//                progressStatus += 10
//                // Update the progress bar and display the
//                //current value in the text view
//                handler.post {
//                    progressBar.progress = progressStatus
//                    textView.text = ""+progressStatus + "/" + progressBar.max
//                }
//                try {
//                    // Sleep for 200 milliseconds.
//                    Thread.sleep(200)
//                } catch (e:InterruptedException) {
//                    e.printStackTrace()
//                }
//            }
//        }).start()

//        loadInDiscussionProposals()

        categorySortAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableProposalTypes)
        categorySortAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        selectionCurrent = category_sort_spinner.selectedItemPosition
        category_sort_spinner.adapter = categorySortAdapter

        category_sort_spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                selectionCurrent = position
                when (position) {
                    0 -> {
                        loadInDiscussionProposals()
                    }
                    1 -> {
                        loadActiveProposals()
                    }
                    2 -> {
                        loadApprovedProposals()
                    }
                    3 -> {
                        loadRejectedProposals()
                    }
                    4 -> {
                        loadAbandonedProposals()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        recyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    visibleItemCount = layoutManager!!.childCount
                    totalItemCount = layoutManager!!.itemCount
                    pastVisibleItems = layoutManager!!.findFirstVisibleItemPosition()
                    if (loading1) {
                        // Start loading new data on the 12th recycler item.
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount - 8) {
                            loading1 = false
//                            loadMore(proposals[totalItemCount - 1].censorshipRecord!!.token)
                        }
                    }
                }
            }
        })

        go_back.setOnClickListener {
            finish()
        }
        refreshAvailableProposalCategories()

    }

    override fun onScrollChanged() {
        if (proposals.size < 5 || !initialLoadingDone.get()) return

        val firstVisibleItem = layoutManager!!.findFirstCompletelyVisibleItemPosition()
        transactions_page_header.elevation = if (firstVisibleItem != 0) resources.getDimension(R.dimen.app_bar_elevation)
        else 0f

        val lastVisibleItem = layoutManager!!.findLastCompletelyVisibleItemPosition()
        if (lastVisibleItem >= proposals.size) {
            if (!loadedAll) {
                recycler_view.stopScroll()
                when (selectionCurrent) {
                    0 -> {
                        loadInDiscussionProposals(loadMore = true)
                    }
                    1 -> {
                        loadActiveProposals(loadMore = true)
                    }
                    2 -> {
                        loadApprovedProposals(loadMore = true)
                    }
                    3 -> {
                        loadRejectedProposals(loadMore = true)
                    }
                    4 -> {
                        loadAbandonedProposals(loadMore = true)
                    }
                }
            }
        }
    }

    private fun loadAllProposals(loadMore: Boolean = false) = GlobalScope.launch(Dispatchers.Default) {
        runOnUiThread {
            showLoadingView()
            swipeRefreshLayout!!.isRefreshing = true
            swipe_refresh_layout.visibility = View.GONE
            emptyList.visibility = View.GONE
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

        val jsonResult = multiWallet!!.politeia.getProposals(Constants.PROPOSALS_ALL, offset, limit, newestProposalsFirst)

        val resultObject = JSONObject(jsonResult).getJSONArray("result")
        val resultString: String = resultObject.toString()

        var tempProposalList = gson.fromJson(resultString, Array<Proposal>::class.java)

        initialLoadingDone.set(true)

        if (tempProposalList == null) {
            loadedAll = true
            loading.set(false)
            hideLoadingView()

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
        hideLoadingView()

        swipeRefreshLayout!!.isRefreshing = false
    }

    private fun loadInDiscussionProposals(loadMore: Boolean = false) = GlobalScope.launch(Dispatchers.Default) {
        runOnUiThread {
            showLoadingView()
            swipeRefreshLayout!!.isRefreshing = true
            swipe_refresh_layout.visibility = View.GONE
            emptyList.visibility = View.GONE
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

        val jsonResult = multiWallet!!.politeia.getProposals(Constants.PROPOSALS_PRE, offset, limit, newestProposalsFirst)

        val resultObject = JSONObject(jsonResult).getJSONArray("result")
        val resultString: String = resultObject.toString()

        var tempProposalList = gson.fromJson(resultString, Array<Proposal>::class.java)

        initialLoadingDone.set(true)

        if (tempProposalList == null) {
            loadedAll = true
            loading.set(false)
            hideLoadingView()

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

            checkEmptyProposalList("in discussion")

            withContext(Dispatchers.Main) {
                proposalAdapter?.notifyDataSetChanged()
            }
        }

        loading.set(false)
        hideLoadingView()

        swipeRefreshLayout!!.isRefreshing = false
    }

    private fun loadActiveProposals(loadMore: Boolean = false) = GlobalScope.launch(Dispatchers.Default) {
        runOnUiThread {
            showLoadingView()
            swipeRefreshLayout!!.isRefreshing = true
            swipe_refresh_layout.visibility = View.GONE
            emptyList.visibility = View.GONE
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

        val jsonResult = multiWallet!!.politeia.getProposals(Constants.PROPOSALS_ACTIVE, offset, limit, newestProposalsFirst)

        val resultObject = JSONObject(jsonResult).getJSONArray("result")
        val resultString: String = resultObject.toString()

        var tempProposalList = gson.fromJson(resultString, Array<Proposal>::class.java)

        initialLoadingDone.set(true)

        if (tempProposalList == null) {
            loadedAll = true
            loading.set(false)
            hideLoadingView()

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

            checkEmptyProposalList("active")

            withContext(Dispatchers.Main) {
                proposalAdapter?.notifyDataSetChanged()
            }
        }

        loading.set(false)
        hideLoadingView()

        swipeRefreshLayout!!.isRefreshing = false
    }

    private fun loadApprovedProposals(loadMore: Boolean = false) = GlobalScope.launch(Dispatchers.Default) {
        runOnUiThread {
            showLoadingView()
            swipeRefreshLayout!!.isRefreshing = true
            swipe_refresh_layout.visibility = View.GONE
            emptyList.visibility = View.GONE
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

        val jsonResult = multiWallet!!.politeia.getProposals(Constants.PROPOSALS_APPROVED, offset, limit, newestProposalsFirst)

        val resultObject = JSONObject(jsonResult).getJSONArray("result")
        val resultString: String = resultObject.toString()

        var tempProposalList = gson.fromJson(resultString, Array<Proposal>::class.java)

        initialLoadingDone.set(true)

        if (tempProposalList == null) {
            loadedAll = true
            loading.set(false)
            hideLoadingView()

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

            checkEmptyProposalList("approved")

            withContext(Dispatchers.Main) {
                proposalAdapter?.notifyDataSetChanged()
            }
        }

        loading.set(false)
        hideLoadingView()

        swipeRefreshLayout!!.isRefreshing = false
    }

    private fun loadRejectedProposals(loadMore: Boolean = false) = GlobalScope.launch(Dispatchers.Default) {
        runOnUiThread {
            showLoadingView()
            swipeRefreshLayout!!.isRefreshing = true
            swipe_refresh_layout.visibility = View.GONE
            emptyList.visibility = View.GONE
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

        val jsonResult = multiWallet!!.politeia.getProposals(Constants.PROPOSALS_REJECTED, offset, limit, newestProposalsFirst)

        val resultObject = JSONObject(jsonResult).getJSONArray("result")
        val resultString: String = resultObject.toString()

        var tempProposalList = gson.fromJson(resultString, Array<Proposal>::class.java)

        initialLoadingDone.set(true)

        if (tempProposalList == null) {
            loadedAll = true
            loading.set(false)
            hideLoadingView()

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

            checkEmptyProposalList("rejected")

            withContext(Dispatchers.Main) {
                proposalAdapter?.notifyDataSetChanged()
            }
        }

        loading.set(false)
        hideLoadingView()

        swipeRefreshLayout!!.isRefreshing = false
    }

    private fun loadAbandonedProposals(loadMore: Boolean = false) = GlobalScope.launch(Dispatchers.Default) {
        runOnUiThread {
            showLoadingView()
            swipeRefreshLayout!!.isRefreshing = true
            swipe_refresh_layout.visibility = View.GONE
            emptyList.visibility = View.GONE
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

        val jsonResult = multiWallet!!.politeia.getProposals(Constants.PROPOSALS_ABANDONED, offset, limit, newestProposalsFirst)

        val resultObject = JSONObject(jsonResult).getJSONArray("result")
        val resultString: String = resultObject.toString()

        var tempProposalList = gson.fromJson(resultString, Array<Proposal>::class.java)

        initialLoadingDone.set(true)

        if (tempProposalList == null) {
            loadedAll = true
            loading.set(false)
            hideLoadingView()

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

            checkEmptyProposalList("abandoned")

            withContext(Dispatchers.Main) {
                proposalAdapter?.notifyDataSetChanged()
            }
        }

        loading.set(false)
        hideLoadingView()

        swipeRefreshLayout!!.isRefreshing = false
    }

    private fun checkEmptyProposalList(status: String) {
        runOnUiThread {
            if (proposals.size > 0) {
                swipe_refresh_layout?.show()
            } else {
                swipe_refresh_layout?.hide()
                emptyList.text = String.format(Locale.getDefault(), "No %s proposals", status)
            }
        }
    }

    private fun refreshAvailableProposalCategories() = GlobalScope.launch(Dispatchers.Default) {
        availableProposalTypes.clear()

        val preCount = multiWallet!!.politeia.count(Constants.PROPOSALS_PRE)
        val activeCount = multiWallet!!.politeia.count(Constants.PROPOSALS_ACTIVE)
        val approvedCount = multiWallet!!.politeia.count(Constants.PROPOSALS_APPROVED)
        val rejectedCount = multiWallet!!.politeia.count(Constants.PROPOSALS_REJECTED)
        val abandonedCount = multiWallet!!.politeia.count(Constants.PROPOSALS_ABANDONED)

        withContext(Dispatchers.Main) {
            availableProposalTypes.add(getString(R.string.proposal_in_discussion, preCount))
            availableProposalTypes.add(getString(R.string.proposal_active, activeCount))
            availableProposalTypes.add(getString(R.string.proposal_approved, approvedCount))
            availableProposalTypes.add(getString(R.string.proposal_rejected, rejectedCount))
            availableProposalTypes.add(getString(R.string.proposal_abandoned, abandonedCount))

            categorySortAdapter?.notifyDataSetChanged()
        }
    }

    private fun showLoadingView(){
        Thread(Runnable {
            progressStatus = 0

            while (progressStatus < 100) {
                runOnUiThread { loading_view.visibility = View.VISIBLE }

                progressStatus += 10
                // Update the progress bar and display the
                //current value in the text view
                handler.post {
                    progressBar.progress = progressStatus
                    textView.text = "Loading proposals"
                }
                try {
                    // Sleep for 200 milliseconds.
                    Thread.sleep(200)
                } catch (e:InterruptedException) {
                    e.printStackTrace()
                }
            }
        }).start()
    }

    private fun hideLoadingView(){
        runOnUiThread {
            progressStatus = 100
            progressBar.progress = progressStatus
            textView.text = ""+progressStatus + "/" + progressBar.max
            loading_view.visibility = View.GONE
            emptyList.visibility = View.VISIBLE
        }

    }

    override fun onRefresh() {
        when (selectionCurrent) {
            0 -> {
                loadInDiscussionProposals()
            }
            1 -> {
                loadActiveProposals()
            }
            2 -> {
                loadApprovedProposals()
            }
            3 -> {
                loadRejectedProposals()
            }
            4 -> {
                loadAbandonedProposals()
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
    }

}