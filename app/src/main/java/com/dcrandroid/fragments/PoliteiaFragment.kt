/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dcrandroid.BuildConfig
import com.dcrandroid.R
import com.dcrandroid.adapter.ProposalAdapter
import com.dcrandroid.data.Constants
import com.dcrandroid.data.Proposal
import com.dcrandroid.util.Deserializer
import com.dcrandroid.util.PreferenceUtil
import com.dcrandroid.util.QueryAPI
import com.google.gson.GsonBuilder
import org.json.JSONObject
import java.util.ArrayList

const val PRE_VOTING = 1
const val ACTIVE_VOTING = 2
const val FINISHED_VOTING = 3
const val ABANDONED_PROPOSALS = 6

class PoliteiaFragment: Fragment(), SwipeRefreshLayout.OnRefreshListener, QueryAPI.QueryAPICallback {

    private val filters = ArrayList<String>()
    private var spinnerAdapter: ArrayAdapter<String>? = null
    private val proposals = ArrayList<Proposal>()
    private val filteredProposals = ArrayList<Proposal>()
    private var recyclerView: RecyclerView? = null
    private var proposalAdapter: ProposalAdapter? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var spinner: Spinner? = null
    private var util: PreferenceUtil? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_politeia, container, false)

        util = PreferenceUtil(requireContext())

        recyclerView = view.findViewById(R.id.recycler_view)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)

        spinner = view.findViewById(R.id.spinnerFilter)

        filters.add(getString(R.string.all))

        spinnerAdapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_item, filters)
        spinnerAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner!!.adapter = spinnerAdapter

        spinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                filterProposals()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        swipeRefreshLayout!!.setOnRefreshListener(this)

        recyclerView!!.layoutManager = LinearLayoutManager(context)
        proposalAdapter = ProposalAdapter(filteredProposals, context!!)
        recyclerView!!.adapter = proposalAdapter

        swipeRefreshLayout!!.post{
            loadProposals()
        }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().setTitle(R.string.politeia)
    }

    private fun loadProposals(){
        swipeRefreshLayout!!.isRefreshing = true
        val vettedProposalsUrl = getString(R.string.vetted_proposals_url, BuildConfig.PoliteiaHost)
        val userAgent = util!!.get(Constants.USER_AGENT, Constants.EMPTY_STRING)
        QueryAPI(vettedProposalsUrl, userAgent, this).execute()
    }

    private fun filterProposals(){
        val pos = spinner!!.selectedItemPosition
        filteredProposals.clear()

        when(pos){
            in 1..3 -> filteredProposals.addAll(proposals.filter { it.voteStatus != null && it.voteStatus!!.status == pos })
            4 -> filteredProposals.addAll(proposals.filter { it.status == ABANDONED_PROPOSALS })
            else -> filteredProposals.addAll(proposals)
        }

        proposalAdapter!!.notifyDataSetChanged()
    }

    override fun onRefresh() {
        loadProposals()
    }

    private fun parseResults(proposalsJson: String, voteStatusJson: String){
        val gson = GsonBuilder()
                .registerTypeHierarchyAdapter(ArrayList::class.java, Deserializer.ProposalDeserializer())
                .registerTypeHierarchyAdapter(Proposal.VoteStatus.OptionsResult::class.java, Deserializer.OptionsResultDeserializer())
                .create()

        var parentObj = JSONObject(proposalsJson)
        val tempProposals = gson.fromJson(parentObj.getJSONArray("proposals").toString(), Array<Proposal>::class.java)
        proposals.clear()
        proposals.addAll(tempProposals)

        parentObj = JSONObject(voteStatusJson)
        val voteStatus = gson.fromJson(parentObj.getJSONArray("votesstatus").toString(), Array<Proposal.VoteStatus>::class.java)

        proposals.map { proposal ->
            val status = voteStatus.find { it.token == proposal.censorshipRecord!!.token }
            if(status != null){
                proposal.voteStatus = status
            }
        }

        val preVoting = voteStatus.count { it.status == PRE_VOTING }
        val activeVoting = voteStatus.count { it.status == ACTIVE_VOTING }
        val finishedVoting = voteStatus.count { it.status == FINISHED_VOTING }
        val abandoned = proposals.count { it.status == ABANDONED_PROPOSALS }

        filters.clear()
        filters.add("All (${proposals.size})")
        filters.add("Pre-Voting ($preVoting)")
        filters.add("Active Voting ($activeVoting)")
        filters.add("Finished Voting ($finishedVoting)")
        filters.add("Abandoned ($abandoned)")
        spinnerAdapter!!.notifyDataSetChanged()
        filterProposals()

        swipeRefreshLayout!!.isRefreshing = false
    }

    override fun onQueryAPISuccess(result: String?) {

        val voteStatusUrl = getString(R.string.proposals_vote_status_url, BuildConfig.PoliteiaHost)
        val userAgent = util!!.get(Constants.USER_AGENT, Constants.EMPTY_STRING)
        val proposalsJson = result

        QueryAPI(voteStatusUrl, userAgent, object : QueryAPI.QueryAPICallback{
            override fun onQueryAPISuccess(result: String?) {
                parseResults(proposalsJson!!, result!!)
            }

            override fun onQueryAPIError(e: Exception) {
                e.printStackTrace()
                activity!!.runOnUiThread {
                    swipeRefreshLayout!!.isRefreshing = false
                    Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }).execute()
    }

    override fun onQueryAPIError(e: Exception) {
        e.printStackTrace()
        activity!!.runOnUiThread {
            swipeRefreshLayout!!.isRefreshing = false
            Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }
}