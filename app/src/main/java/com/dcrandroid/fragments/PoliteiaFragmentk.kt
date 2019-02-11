package com.dcrandroid.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dcrandroid.R
import com.dcrandroid.adapter.ProposalAdapter
import com.dcrandroid.data.Proposal
import com.dcrandroid.util.NetworkTask
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class PoliteiaFragmentk : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    internal var proposals = ArrayList<Proposal>()
    internal var recyclerView: RecyclerView? = null
    internal var proposalAdapter: ProposalAdapter? = null
    internal var swipeRefreshLayout: SwipeRefreshLayout? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_politeia, container, false)

        recyclerView = view.findViewById(R.id.recycler_view)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        swipeRefreshLayout!!.setOnRefreshListener(this)

        recyclerView!!.layoutManager = LinearLayoutManager(context)
        proposalAdapter = ProposalAdapter(proposals, context!!)
        recyclerView!!.adapter = proposalAdapter

        swipeRefreshLayout!!.post { loadProposals() }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (activity != null)
            activity!!.title = getString(R.string.proposals)
    }

    private fun loadProposals() {
        swipeRefreshLayout!!.isRefreshing = true
        val config = HashMap<String, String>()
        config["url"] = getString(R.string.politeia_proposals_url)
        NetworkTask(config, object : NetworkTask.AsyncResponse {
            override fun onResponse(response: String) {
                swipeRefreshLayout!!.isRefreshing = false
                try {
                    proposals.clear()
                    val jsonObject = JSONObject(response)
                    val jsonArray = jsonObject.getJSONArray("proposals")
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject1 = jsonArray.get(i) as JSONObject
                        val proposal = Proposal()
                        proposal.name = jsonObject1.getString("name")
                        proposal.state = jsonObject1.getInt("state")
                        proposal.status = jsonObject1.getInt("status")
                        proposal.timestamp = jsonObject1.getLong("timestamp")
                        proposal.userid = jsonObject1.getString("userid")
                        proposal.username = jsonObject1.getString("username")
                        proposal.publickey = jsonObject1.getString("publickey")
                        proposal.signature = jsonObject1.getString("signature")
                        proposal.version = jsonObject1.getString("version")
                        proposal.setNumcomments(jsonObject1.getInt("numcomments"))
                        val jsonArray1 = jsonObject1.getJSONArray("files")
                        val files = ArrayList<Proposal.File>()
                        for (j in 0 until jsonArray1.length()) {
                            val jsonObject2 = jsonArray1.get(j) as JSONObject
                            val file = proposal.File()
                            file.name = jsonObject2.getString("name")
                            file.mime = jsonObject2.getString("mime")
                            file.digest = jsonObject2.getString("digest")
                            file.payload = jsonObject2.getString("payload")
                            files.add(file)
                        }
                        val record = proposal.CensorshipRecord()
                        record.token = jsonObject1.getJSONObject("censorshiprecord").getString("token")
                        record.merkle = jsonObject1.getJSONObject("censorshiprecord").getString("merkle")
                        record.signature = jsonObject1.getJSONObject("censorshiprecord").getString("signature")

                        proposal.files = files
                        proposal.censorshipRecord = record
                        proposals.add(proposal)

                    }
                    if (activity != null) {
                        activity!!.runOnUiThread { proposalAdapter!!.notifyDataSetChanged() }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(t: Throwable) {
                swipeRefreshLayout!!.isRefreshing = false
                t.printStackTrace()
                if (activity != null) {
                    activity!!.runOnUiThread { Toast.makeText(activity, t.message, Toast.LENGTH_SHORT).show() }
                }
            }
        }).execute()
    }

    override fun onRefresh() {
        loadProposals()
    }
}
