package com.dcrandroid.fragments


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.dcrandroid.R
import com.dcrandroid.activities.SetupWalletActivity
import kotlinx.android.synthetic.main.fragment_setup_wallet_prompt.*

class SetUpWalletPromptFragment : Fragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        tv_get_started.setOnClickListener { navigateToCreateWalletPage() }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_setup_wallet_prompt, container, false)
    }

    private fun navigateToCreateWalletPage() {
        val intent = Intent(activity, SetupWalletActivity::class.java)
        startActivity(intent)
    }
}
