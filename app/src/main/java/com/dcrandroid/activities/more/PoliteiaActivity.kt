package com.dcrandroid.activities.more

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.adapter.ProposalAdapter
import com.dcrandroid.data.Proposal
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.util.Deserializer
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_politeia.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

class PoliteiaActivity : BaseActivity(), SwipeRefreshLayout.OnRefreshListener, OnItemSelectedListener {
    private var proposals = ArrayList<Proposal>()
    private var recyclerView: RecyclerView? = null
    private var proposalAdapter: ProposalAdapter? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var layoutManager: LinearLayoutManager? = null
    private lateinit var emptyList: TextView

    private val gson = GsonBuilder().registerTypeHierarchyAdapter(ArrayList::class.java, Deserializer.ProposalDeserializer()).create()

    private var loading = true
    private var pastVisibleItems: Int = 0
    private var visibleItemCount: Int = 0
    private var totalItemCount: Int = 0

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

        val categorySortItems = this.resources.getStringArray(R.array.proposal_status_sort)
        val categorySortAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorySortItems)
        categorySortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        var selectionCurrent: Int = category_sort_spinner.selectedItemPosition
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

//        loadAllProposals()

        recyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    visibleItemCount = layoutManager!!.childCount
                    totalItemCount = layoutManager!!.itemCount
                    pastVisibleItems = layoutManager!!.findFirstVisibleItemPosition()
                    if (loading) {
                        // Start loading new data on the 12th recycler item.
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount - 8) {
                            loading = false
//                            loadMore(proposals[totalItemCount - 1].censorshipRecord!!.token)
                        }
                    }
                }
            }
        })

        go_back.setOnClickListener {
            finish()
        }
    }

//    fun loadMore(token: String?) = GlobalScope.launch(Dispatchers.Default) {
//        val jsonResult = politeia!!.getProposalsChunk(token)
//        val tempProposalList = gson.fromJson(jsonResult, Array<Proposal>::class.java)
//
//        if (loading) {
//            proposals.let {
//                it.clear()
//                it.addAll(tempProposalList)
//            }
//            withContext(Dispatchers.Main) {
//                proposalAdapter?.notifyDataSetChanged()
//            }
//        } else {
//            val positionStart = proposals.size
//            proposals.addAll(tempProposalList)
//            withContext(Dispatchers.Main) {
//                proposalAdapter?.notifyItemRangeInserted(positionStart, tempProposalList.size)
//
//                // notify previous last item to remove bottom margin
//                proposalAdapter?.notifyItemChanged(positionStart - 1)
//
//                loading = true
//            }
//        }
//    }

//    private fun loadAllProposals() = GlobalScope.launch(Dispatchers.Default) {
//        runOnUiThread {
//            swipeRefreshLayout!!.isRefreshing = true
//        }
//        val jsonResult = politeia!!.getProposalsChunk("")
//
//        var tempProposalList = gson.fromJson(jsonResult, Array<Proposal>::class.java)
//
//        if (tempProposalList == null) {
//            tempProposalList = arrayOf()
//        } else {
//            proposals.let {
//                it.clear()
//                it.addAll(tempProposalList)
//            }
//
//            checkEmptyProposalList("in discussion")
//
//            withContext(Dispatchers.Main) {
//                proposalAdapter?.notifyDataSetChanged()
//            }
//        }
//        swipeRefreshLayout!!.isRefreshing = false
//    }

    private fun loadInDiscussionProposals() = GlobalScope.launch(Dispatchers.Default) {
        runOnUiThread {
            swipeRefreshLayout!!.isRefreshing = true
        }
        val jsonResult = multiWallet!!.politeia.batchPreProposals


        var tempProposalList = gson.fromJson(jsonResult, Array<Proposal>::class.java)

        if (tempProposalList == null) {
            tempProposalList = arrayOf()
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
        swipeRefreshLayout!!.isRefreshing = false
    }

    private fun loadActiveProposals() = GlobalScope.launch(Dispatchers.Default) {
        runOnUiThread {
            swipeRefreshLayout!!.isRefreshing = true
        }
        val jsonResult = multiWallet!!.politeia!!.batchActiveProposals

        var tempProposalList = gson.fromJson(jsonResult, Array<Proposal>::class.java)

        if (tempProposalList == null) {
            Toast.makeText(this@PoliteiaActivity, "Empty list", Toast.LENGTH_LONG).show()

            tempProposalList = arrayOf()
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
        swipeRefreshLayout!!.isRefreshing = false
    }

    private fun loadApprovedProposals() = GlobalScope.launch(Dispatchers.Default) {
        runOnUiThread {
            swipeRefreshLayout!!.isRefreshing = true
        }
        val jsonResult = multiWallet!!.politeia!!.batchApprovedProposals

        var tempProposalList = gson.fromJson(jsonResult, Array<Proposal>::class.java)

        if (tempProposalList == null) {
            tempProposalList = arrayOf()
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
        swipeRefreshLayout!!.isRefreshing = false
    }

    private fun loadRejectedProposals() = GlobalScope.launch(Dispatchers.Default) {
        runOnUiThread {
            swipeRefreshLayout!!.isRefreshing = true
        }
        val jsonResult = multiWallet!!.politeia!!.batchRejectedProposals

        var tempProposalList = gson.fromJson(jsonResult, Array<Proposal>::class.java)

        if (tempProposalList == null) {
            tempProposalList = arrayOf()
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
        swipeRefreshLayout!!.isRefreshing = false
    }

    private fun loadAbandonedProposals() = GlobalScope.launch(Dispatchers.Default) {
        runOnUiThread {
            swipeRefreshLayout!!.isRefreshing = true
        }
        val jsonResult = multiWallet!!.politeia!!.batchAbandonedProposals

        var tempProposalList = gson.fromJson(jsonResult, Array<Proposal>::class.java)

        if (tempProposalList == null) {
            tempProposalList = arrayOf()
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

    override fun onRefresh() {
//        loadAllProposals()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
    }

}