package com.dcrandroid.fragments.more

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dcrandroid.R
import com.dcrandroid.adapter.ProposalAdapter
import com.dcrandroid.data.Constants
import com.dcrandroid.data.Proposal
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.fragments.BaseFragment
import com.dcrandroid.util.Deserializer
import com.dcrandroid.util.SnackBar
import com.google.gson.GsonBuilder
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.fragment_politeia.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList

class PoliteiaFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener, OnItemSelectedListener, ViewTreeObserver.OnScrollChangedListener {
    private var proposals = ArrayList<Proposal>()
    private var proposalAdapter: ProposalAdapter? = null
    private var layoutManager: LinearLayoutManager? = null

    private val gson = GsonBuilder().registerTypeHierarchyAdapter(ArrayList::class.java, Deserializer.ProposalDeserializer()).create()

    private var newestProposalsFirst = true
    private var loadedAll = false
    private var loadedAllInDiscussionProposals = false
    private var loadedAllActiveProposals = false
    private var loadedAllApprovedProposals = false
    private var loadedAllRejectedProposals = false
    private var loadedAllAbandonedProposals = false

    private val loading = AtomicBoolean(false)
    private val initialLoadingDone = AtomicBoolean(false)

    private val availableProposalTypes = ArrayList<String>()

    private var categorySortAdapter: ArrayAdapter<String>? = null

    private var progressStatus = 0
    private var currentCategory: Int = 0
    private val handler = Handler()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_politeia, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        swipe_refresh_layout.setOnRefreshListener(this)
        layoutManager = LinearLayoutManager(context!!)
        recycler_view.layoutManager = layoutManager
        proposalAdapter = ProposalAdapter(proposals, context!!)
        recycler_view.adapter = proposalAdapter
        recycler_view.viewTreeObserver.addOnScrollChangedListener(this)

        val timestampSortItems = resources.getStringArray(R.array.timestamp_sort)
        val timestampSortAdapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_item, timestampSortItems)
        timestampSortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timestamp_sort_spinner.onItemSelectedListener = this
        timestamp_sort_spinner.adapter = timestampSortAdapter

        categorySortAdapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_item, availableProposalTypes)
        categorySortAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        currentCategory = category_sort_spinner.selectedItemPosition
        category_sort_spinner.adapter = categorySortAdapter

        category_sort_spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                currentCategory = position
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

        go_back.setOnClickListener {
            activity!!.finish()
        }

        refreshAvailableProposalCategories()

        sync_layout.setOnClickListener {
            if (!multiWallet.isConnectedToDecredNetwork) {
                SnackBar.showError(context!!, R.string.not_connected)
                return@setOnClickListener
            }
            syncProposals()
        }
    }

    override fun onScrollChanged() {
        when (currentCategory) {
            0 -> {
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
                    if (!loadedAllInDiscussionProposals) {
                        recycler_view.stopScroll()
                        loadInDiscussionProposals(loadMore = true)
                    }
                }
            }
            1 -> {
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
                    if (!loadedAllActiveProposals) {
                        recycler_view.stopScroll()
                        loadActiveProposals(loadMore = true)
                    }
                }
            }
            2 -> {
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
                    if (!loadedAllApprovedProposals) {
                        recycler_view.stopScroll()
                        loadApprovedProposals(loadMore = true)
                    }
                }
            }
            3 -> {
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
                    if (!loadedAllRejectedProposals) {
                        recycler_view.stopScroll()
                        loadRejectedProposals(loadMore = true)
                    }
                }
            }
            4 -> {
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
                    if (!loadedAllAbandonedProposals) {
                        recycler_view.stopScroll()
                        loadAbandonedProposals(loadMore = true)
                    }
                }
            }
        }
    }

    private fun loadAllProposals(loadMore: Boolean = false) = GlobalScope.launch(Dispatchers.Default) {
        activity!!.runOnUiThread {
            showLoadingView()
            swipe_refresh_layout.isRefreshing = true
            swipe_refresh_layout.visibility = View.GONE
            empty_list.visibility = View.GONE
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

        val jsonResult = multiWallet.politeia.getProposals(Dcrlibwallet.ProposalCategoryAll, offset, limit, newestProposalsFirst)

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

        swipe_refresh_layout.isRefreshing = false
    }

    private fun loadInDiscussionProposals(loadMore: Boolean = false) = GlobalScope.launch(Dispatchers.Default) {
        activity!!.runOnUiThread {
            showLoadingView()
            swipe_refresh_layout.isRefreshing = true
            swipe_refresh_layout.visibility = View.GONE
            empty_list.visibility = View.GONE
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

        val jsonResult = multiWallet.politeia.getProposals(Dcrlibwallet.ProposalCategoryPre, offset, limit, newestProposalsFirst)

        // Check if the result object from the json response is null
        val resultObject = JSONObject(jsonResult).get("result")
        val resultObjectString = resultObject.toString()
        val inDiscussionProposalList = if (resultObjectString == "null") {
            gson.fromJson("[]", Array<Proposal>::class.java)
        } else {
            val resultArray = JSONObject(jsonResult).getJSONArray("result")
            val resultArrayString = resultArray.toString()
            gson.fromJson(resultArrayString, Array<Proposal>::class.java)
        }

        initialLoadingDone.set(true)

        if (inDiscussionProposalList == null) {
            loadedAll = true
            loading.set(false)
            hideLoadingView()

            if (!loadMore) {
                proposals.clear()
            }
            return@launch
        }

        if (inDiscussionProposalList.size < limit) {
            loadedAll = true
        }

        if (loadMore) {
            val positionStart = proposals.size
            proposals.addAll(inDiscussionProposalList)
            withContext(Dispatchers.Main) {
                proposalAdapter?.notifyItemRangeInserted(positionStart, inDiscussionProposalList.size)

                // notify previous last item to remove bottom margin
                proposalAdapter?.notifyItemChanged(positionStart - 1)
            }

        } else {
            proposals.let {
                it.clear()
                it.addAll(inDiscussionProposalList)
            }

            checkEmptyProposalList("in discussion")

            withContext(Dispatchers.Main) {
                proposalAdapter?.notifyDataSetChanged()
            }
        }

        loading.set(false)
        hideLoadingView()

        swipe_refresh_layout.isRefreshing = false
    }

    private fun loadActiveProposals(loadMore: Boolean = false) = GlobalScope.launch(Dispatchers.Default) {
        activity!!.runOnUiThread {
            showLoadingView()
            swipe_refresh_layout.isRefreshing = true
            swipe_refresh_layout.visibility = View.GONE
            empty_list.visibility = View.GONE
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

        val jsonResult = multiWallet.politeia.getProposals(Dcrlibwallet.ProposalCategoryActive, offset, limit, newestProposalsFirst)

        // Check if the result object from the json response is null
        val resultObject = JSONObject(jsonResult).get("result")
        val resultObjectString = resultObject.toString()
        val activeProposalList = if (resultObjectString == "null") {
            gson.fromJson("[]", Array<Proposal>::class.java)
        } else {
            val resultArray = JSONObject(jsonResult).getJSONArray("result")
            val resultArrayString = resultArray.toString()
            gson.fromJson(resultArrayString, Array<Proposal>::class.java)
        }

        initialLoadingDone.set(true)

        if (activeProposalList == null) {
            loadedAll = true
            loading.set(false)
            hideLoadingView()

            if (!loadMore) {
                proposals.clear()
            }
            return@launch
        }

        if (activeProposalList.size < limit) {
            loadedAll = true
        }

        if (loadMore) {
            val positionStart = proposals.size
            proposals.addAll(activeProposalList)
            withContext(Dispatchers.Main) {
                proposalAdapter?.notifyItemRangeInserted(positionStart, activeProposalList.size)

                // notify previous last item to remove bottom margin
                proposalAdapter?.notifyItemChanged(positionStart - 1)
            }

        } else {
            proposals.let {
                it.clear()
                it.addAll(activeProposalList)
            }

            checkEmptyProposalList("active")

            withContext(Dispatchers.Main) {
                proposalAdapter?.notifyDataSetChanged()
            }
        }

        loading.set(false)
        hideLoadingView()

        swipe_refresh_layout.isRefreshing = false
    }

    private fun loadApprovedProposals(loadMore: Boolean = false) = GlobalScope.launch(Dispatchers.Default) {
        activity!!.runOnUiThread {
            showLoadingView()
            swipe_refresh_layout.isRefreshing = true
            swipe_refresh_layout.visibility = View.GONE
            empty_list.visibility = View.GONE
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

        val jsonResult = multiWallet.politeia.getProposals(Dcrlibwallet.ProposalCategoryApproved, offset, limit, newestProposalsFirst)

        // Check if the result object from the json response is null
        val resultObject = JSONObject(jsonResult).get("result")
        val resultObjectString = resultObject.toString()
        val approvedProposalList = if (resultObjectString == "null") {
            gson.fromJson("[]", Array<Proposal>::class.java)
        } else {
            val resultArray = JSONObject(jsonResult).getJSONArray("result")
            val resultArrayString = resultArray.toString()
            gson.fromJson(resultArrayString, Array<Proposal>::class.java)
        }

        initialLoadingDone.set(true)

        if (approvedProposalList == null) {

            loadedAllApprovedProposals = true
            loading.set(false)
            hideLoadingView()

            if (!loadMore) {
                proposals.clear()
            }
            return@launch
        }

        if (approvedProposalList.size < limit) {
            loadedAll = true
        }

        if (loadMore) {
            val positionStart = proposals.size
            proposals.addAll(approvedProposalList)
            withContext(Dispatchers.Main) {
                proposalAdapter?.notifyItemRangeInserted(positionStart, approvedProposalList.size)

                // notify previous last item to remove bottom margin
                proposalAdapter?.notifyItemChanged(positionStart - 1)
            }

        } else {
            proposals.let {
                it.clear()
                it.addAll(approvedProposalList)
            }

            checkEmptyProposalList("approved")

            withContext(Dispatchers.Main) {
                proposalAdapter?.notifyDataSetChanged()
            }
        }

        loading.set(false)
        hideLoadingView()

        swipe_refresh_layout.isRefreshing = false
    }

    private fun loadRejectedProposals(loadMore: Boolean = false) = GlobalScope.launch(Dispatchers.Default) {
        activity!!.runOnUiThread {
            showLoadingView()
            swipe_refresh_layout.isRefreshing = true
            swipe_refresh_layout.visibility = View.GONE
            empty_list.visibility = View.GONE
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

        val jsonResult = multiWallet.politeia.getProposals(Dcrlibwallet.ProposalCategoryRejected, offset, limit, newestProposalsFirst)

        // Check if the result object from the json response is null
        val resultObject = JSONObject(jsonResult).get("result")
        val resultObjectString = resultObject.toString()
        val rejectedProposalList = if (resultObjectString == "null") {
            gson.fromJson("[]", Array<Proposal>::class.java)
        } else {
            val resultArray = JSONObject(jsonResult).getJSONArray("result")
            val resultArrayString = resultArray.toString()
            gson.fromJson(resultArrayString, Array<Proposal>::class.java)
        }

        initialLoadingDone.set(true)

        if (rejectedProposalList == null) {
            loadedAll = true
            loading.set(false)
            hideLoadingView()

            if (!loadMore) {
                proposals.clear()
            }
            return@launch
        }

        if (rejectedProposalList.size < limit) {
            loadedAll = true
        }

        if (loadMore) {
            val positionStart = proposals.size
            proposals.addAll(rejectedProposalList)
            withContext(Dispatchers.Main) {
                proposalAdapter?.notifyItemRangeInserted(positionStart, rejectedProposalList.size)

                // notify previous last item to remove bottom margin
                proposalAdapter?.notifyItemChanged(positionStart - 1)
            }

        } else {
            proposals.let {
                it.clear()
                it.addAll(rejectedProposalList)
            }

            checkEmptyProposalList("rejected")

            withContext(Dispatchers.Main) {
                proposalAdapter?.notifyDataSetChanged()
            }
        }

        loading.set(false)
        hideLoadingView()

        swipe_refresh_layout.isRefreshing = false
    }

    private fun loadAbandonedProposals(loadMore: Boolean = false) = GlobalScope.launch(Dispatchers.Default) {
        activity!!.runOnUiThread {
            showLoadingView()
            swipe_refresh_layout.isRefreshing = true
            swipe_refresh_layout.visibility = View.GONE
            empty_list.visibility = View.GONE
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

        val jsonResult = multiWallet.politeia.getProposals(Dcrlibwallet.ProposalCategoryAbandoned, offset, limit, newestProposalsFirst)

        // Check if the result object from the json response is null
        val resultObject = JSONObject(jsonResult).get("result")
        val resultObjectString = resultObject.toString()
        val abandonedProposalList = if (resultObjectString == "null") {
            gson.fromJson("[]", Array<Proposal>::class.java)
        } else {
            val resultArray = JSONObject(jsonResult).getJSONArray("result")
            val resultArrayString = resultArray.toString()
            gson.fromJson(resultArrayString, Array<Proposal>::class.java)
        }

        initialLoadingDone.set(true)

        if (abandonedProposalList == null) {
            loadedAll = true
            loading.set(false)
            hideLoadingView()

            if (!loadMore) {
                proposals.clear()
            }
            return@launch
        }

        if (abandonedProposalList.size < limit) {
            loadedAll = true
        }

        if (loadMore) {
            val positionStart = proposals.size
            proposals.addAll(abandonedProposalList)
            withContext(Dispatchers.Main) {
                proposalAdapter?.notifyItemRangeInserted(positionStart, abandonedProposalList.size)

                // notify previous last item to remove bottom margin
                proposalAdapter?.notifyItemChanged(positionStart - 1)
            }

        } else {
            proposals.let {
                it.clear()
                it.addAll(abandonedProposalList)
            }

            checkEmptyProposalList("abandoned")

            withContext(Dispatchers.Main) {
                proposalAdapter?.notifyDataSetChanged()
            }
        }

        loading.set(false)
        hideLoadingView()

        swipe_refresh_layout.isRefreshing = false
    }

    private fun checkEmptyProposalList(status: String) {
        activity!!.runOnUiThread {
            if (proposals.size > 0) {
                swipe_refresh_layout?.show()
            } else {
                swipe_refresh_layout?.hide()
                empty_list.text = String.format(Locale.getDefault(), "No %s proposals", status)
            }
        }
    }

    private fun refreshAvailableProposalCategories() = GlobalScope.launch(Dispatchers.Default) {
        availableProposalTypes.clear()

        val preCount = multiWallet.politeia.count(Dcrlibwallet.ProposalCategoryPre)
        val activeCount = multiWallet.politeia.count(Dcrlibwallet.ProposalCategoryActive)
        val approvedCount = multiWallet.politeia.count(Dcrlibwallet.ProposalCategoryApproved)
        val rejectedCount = multiWallet.politeia.count(Dcrlibwallet.ProposalCategoryRejected)
        val abandonedCount = multiWallet.politeia.count(Dcrlibwallet.ProposalCategoryAbandoned)

        withContext(Dispatchers.Main) {
            availableProposalTypes.add(getString(R.string.proposal_in_discussion, preCount))
            availableProposalTypes.add(getString(R.string.proposal_active, activeCount))
            availableProposalTypes.add(getString(R.string.proposal_approved, approvedCount))
            availableProposalTypes.add(getString(R.string.proposal_rejected, rejectedCount))
            availableProposalTypes.add(getString(R.string.proposal_abandoned, abandonedCount))

            categorySortAdapter?.notifyDataSetChanged()
        }
    }

    private fun showLoadingView() {
        Thread(Runnable {
            progressStatus = 0

            while (progressStatus < 100) {
                activity!!.runOnUiThread { loading_view.visibility = View.VISIBLE }

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
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }).start()
    }

    private fun hideLoadingView() {
        activity!!.runOnUiThread {
            progressStatus = 100
            progressBar.progress = progressStatus
            textView.text = "" + progressStatus + "/" + progressBar.max
            loading_view.visibility = View.GONE
//            swipe_refresh_layout.visibility = View.VISIBLE
            if (proposals.size > 0) {
                swipe_refresh_layout.visibility = View.VISIBLE
                empty_list.visibility = View.GONE
            } else {
                swipe_refresh_layout.visibility = View.GONE
                empty_list.visibility = View.VISIBLE
            }
        }
    }

    override fun onRefresh() {
        when (currentCategory) {
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
        when (currentCategory) {
            0 -> {
                if (!initialLoadingDone.get()) {
                    return
                }

                if (parent!!.id == R.id.timestamp_sort_spinner) {
                    val newestFirst = position == 0 // "Newest" is the first item
                    if (newestFirst != newestProposalsFirst) {
                        newestProposalsFirst = newestFirst
                        loadInDiscussionProposals()
                    }
                }
            }
            1 -> {
                if (!initialLoadingDone.get()) {
                    return
                }

                if (parent!!.id == R.id.timestamp_sort_spinner) {
                    val newestFirst = position == 0 // "Newest" is the first item
                    if (newestFirst != newestProposalsFirst) {
                        newestProposalsFirst = newestFirst
                        loadActiveProposals()
                    }
                }
            }
            2 -> {
                if (!initialLoadingDone.get()) {
                    return
                }

                if (parent!!.id == R.id.timestamp_sort_spinner) {
                    val newestFirst = position == 0 // "Newest" is the first item
                    if (newestFirst != newestProposalsFirst) {
                        newestProposalsFirst = newestFirst
                        loadApprovedProposals()
                    }
                }
            }
            3 -> {
                if (!initialLoadingDone.get()) {
                    return
                }

                if (parent!!.id == R.id.timestamp_sort_spinner) {
                    val newestFirst = position == 0 // "Newest" is the first item
                    if (newestFirst != newestProposalsFirst) {
                        newestProposalsFirst = newestFirst
                        loadRejectedProposals()
                    }
                }
            }
            4 -> {
                if (!initialLoadingDone.get()) {
                    return
                }

                if (parent!!.id == R.id.timestamp_sort_spinner) {
                    val newestFirst = position == 0 // "Newest" is the first item
                    if (newestFirst != newestProposalsFirst) {
                        newestProposalsFirst = newestFirst
                        loadAbandonedProposals()
                    }
                }
            }
        }
    }
}