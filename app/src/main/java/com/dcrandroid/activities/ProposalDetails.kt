/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.dcrandroid.BuildConfig
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.data.Proposal
import com.dcrandroid.util.Deserializer
import com.dcrandroid.util.PreferenceUtil
import com.dcrandroid.util.QueryAPI
import com.dcrandroid.util.Utils
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_proposal_details.*
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import kotlin.text.Charsets.UTF_8

class ProposalDetails : AppCompatActivity(), QueryAPI.QueryAPICallback {

    private var proposal: Proposal? = null

    private var util: PreferenceUtil? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val decorView = window.decorView
            decorView.systemUiVisibility = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        title = getString(R.string.proposal_details)
        setContentView(R.layout.activity_proposal_details)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        util = PreferenceUtil(this)

        proposal = intent.getSerializableExtra(Constants.PROPOSAL) as Proposal
        loadProposal()
    }

    private fun loadProposal() {
        proposal_name.text = proposal!!.name

        val meta = getString(R.string.proposal_meta_format, Utils.calculateTime(System.currentTimeMillis() / 1000 - proposal!!.timestamp, this),
                proposal!!.username, proposal!!.version, proposal!!.numComments)
        tv_meta.setText(meta, TextView.BufferType.SPANNABLE)

        progressBar!!.visibility = View.VISIBLE

        val userAgent = util!!.get(Constants.USER_AGENT, Constants.EMPTY_STRING)
        val proposalUrl = getString(R.string.proposal_url, BuildConfig.PoliteiaHost, proposal!!.censorshipRecord!!.token)
        QueryAPI(proposalUrl, userAgent, this).execute()
    }

    private fun loadContent() {
        val description = StringBuilder()
        for (file in proposal!!.files!!) {
            if (file.name == "index.md") {
                try {
                    val payload = file.payload!!
                    val data = Base64.decode(payload, Base64.DEFAULT)
                    description.append(String(data, UTF_8))
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }

            }
        }

        progressBar!!.visibility = View.GONE

        tv_description.text = description.toString()
    }

    override fun onQueryAPISuccess(result: String?) {

        val gson = GsonBuilder()
                .registerTypeHierarchyAdapter(Array<Proposal.PoliteiaFile>::class.java, Deserializer.ProposalDeserializer())
                .registerTypeHierarchyAdapter(Proposal.VoteStatus.OptionsResult::class.java, Deserializer.OptionsResultDeserializer())
                .create()
        val parentObj = JSONObject(result)
        proposal = gson.fromJson(parentObj.getJSONObject(Constants.PROPOSAL).toString(), Proposal::class.java)
        loadContent()

    }

    override fun onQueryAPIError(e: Exception) {
        print("Proposal Error ${e.message}")
        e.printStackTrace()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.proposal_details_menu, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.share_proposal -> {
                val share = Intent(Intent.ACTION_SEND)
                share.type = "text/plain"
                share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
                share.putExtra(Intent.EXTRA_SUBJECT, proposal!!.name)

                share.putExtra(Intent.EXTRA_TEXT, "http://${BuildConfig.PoliteiaHost}/proposals/" + proposal!!.censorshipRecord!!.token!!)
                startActivity(Intent.createChooser(share, getString(R.string.share_proposal_link)))
                return true
            }
            R.id.open_proposal -> {
                val url = "http://${BuildConfig.PoliteiaHost}/proposals/" + proposal!!.censorshipRecord!!.token!!
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent!!.action != null && intent.action == Constants.NEW_POLITEIA_PROPOSAL_NOTIFICATION) {
            proposal = intent.getSerializableExtra(Constants.PROPOSAL) as Proposal
            loadProposal()
        }
    }
}