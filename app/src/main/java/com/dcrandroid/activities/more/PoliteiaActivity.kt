package com.dcrandroid.activities.more

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.adapter.ProposalAdapter
import com.dcrandroid.data.Proposal
import com.dcrandroid.util.Deserializer
import com.google.gson.GsonBuilder
import dcrlibwallet.Politeia
import kotlinx.android.synthetic.main.activity_politeia.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PoliteiaActivity : BaseActivity(), SwipeRefreshLayout.OnRefreshListener {
    private var proposals = ArrayList<Proposal>()
    private var recyclerView: RecyclerView? = null
    private var proposalAdapter: ProposalAdapter? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var layoutManager: LinearLayoutManager? = null

    private var politeia: Politeia? = Politeia()
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
        swipeRefreshLayout!!.setOnRefreshListener(this)

        layoutManager = LinearLayoutManager(this)
        recyclerView!!.layoutManager = layoutManager
        proposalAdapter = ProposalAdapter(proposals, this)
        recyclerView!!.adapter = proposalAdapter

        loadProposals()

        recyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    visibleItemCount = layoutManager!!.childCount
                    totalItemCount = layoutManager!!.itemCount
                    pastVisibleItems = layoutManager!!.findFirstVisibleItemPosition()
                    if (loading) {
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount - 8) {
                            loading = false
                            loadMore(proposals[totalItemCount - 1].censorshipRecord!!.token)
                        }
                    }
                }
            }
        })

        go_back.setOnClickListener {
            finish()
        }
    }

    fun loadMore(token: String?) = GlobalScope.launch(Dispatchers.Default) {
        val jsonResult = politeia!!.getProposalsChunk(token)
        val tempProposalList = gson.fromJson(jsonResult, Array<Proposal>::class.java)

        if (loading) {
            proposals.let {
                it.clear()
                it.addAll(tempProposalList)
            }
            withContext(Dispatchers.Main) {
                proposalAdapter?.notifyDataSetChanged()
            }
        } else {
            val positionStart = proposals.size
            proposals.addAll(tempProposalList)
            withContext(Dispatchers.Main) {
                proposalAdapter?.notifyItemRangeInserted(positionStart, tempProposalList.size)

                // notify previous last item to remove bottom margin
                proposalAdapter?.notifyItemChanged(positionStart - 1)

                loading = true
            }
        }
    }

    private fun loadProposals() = GlobalScope.launch(Dispatchers.Default) {
        swipeRefreshLayout!!.isRefreshing = true
        val jsonResult = politeia!!.getProposalsChunk("")
        var tempProposalList = gson.fromJson(jsonResult, Array<Proposal>::class.java)

        if (tempProposalList == null) {
            tempProposalList = arrayOf()
        } else {
            proposals.let {
                it.clear()
                it.addAll(tempProposalList)
            }
            withContext(Dispatchers.Main) {
                proposalAdapter?.notifyDataSetChanged()
            }
        }
        swipeRefreshLayout!!.isRefreshing = false
    }

    override fun onRefresh() {
        loadProposals()
    }

}