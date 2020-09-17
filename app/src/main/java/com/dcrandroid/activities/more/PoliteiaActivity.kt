package com.dcrandroid.activities.more

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.fragments.more.PoliteiaFragment
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.Utils
import dcrlibwallet.ProposalNotificationListener

class PoliteiaActivity : BaseActivity(), ProposalNotificationListener {
    private lateinit var currentFragment: Fragment
    private lateinit var notificationManager: NotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_politeia)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        switchFragment()
    }

    private fun switchFragment() {
        currentFragment = PoliteiaFragment()
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, currentFragment)
                .commit()
    }

    fun syncProposals() {
        Thread {
            try {
                runOnUiThread { SnackBar.showText(this, R.string.syncing_proposals) }
                multiWallet!!.politeia.sync()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                runOnUiThread { SnackBar.showError(this, R.string.error_syncying_proposals) }
            }
        }.start()
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