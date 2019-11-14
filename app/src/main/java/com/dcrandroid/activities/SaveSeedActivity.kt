/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.adapter.SaveSeedAdapter
import com.dcrandroid.data.Constants
import com.dcrandroid.util.WalletData
import kotlinx.android.synthetic.main.save_seed_page.*
import kotlin.math.*

const val SEEDS_PER_ROW = 17

class SaveSeedActivity : BaseActivity() {

    private var walletId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.save_seed_page)

        try {
            val vto = scroll_view_seeds.viewTreeObserver
            if(vto.isAlive){
                vto.addOnScrollChangedListener {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        app_bar.elevation = if (scroll_view_seeds.scrollY != 0) {
                            resources.getDimension(R.dimen.app_bar_elevation)
                        } else {
                            0f
                        }
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        populateList()

        step_2.setOnClickListener {
            val verifySeedIntent = Intent(this, VerifySeedActivity::class.java)
            verifySeedIntent.putExtra(Constants.WALLET_ID, walletId)
            verifySeedIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            startActivity(verifySeedIntent)
        }

        go_back.setOnClickListener {
            finish()
        }
    }

    private fun populateList(){
        walletId = intent.getLongExtra(Constants.WALLET_ID, -1)
        val wallet = WalletData.multiWallet!!.walletWithID(walletId!!)

        val seed = wallet.seed
        if(seed.isBlank()){
            finish()
            return
        }

        val items = seed!!.split(Constants.NBSP.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        val layoutManager = GridLayoutManager(applicationContext, SEEDS_PER_ROW, GridLayoutManager.HORIZONTAL, false)

        val verticalDivider = VerticalDividerItemDecoration(resources.getDimensionPixelSize(R.dimen.margin_padding_size_8), true)
        val horizontalItemDecoration = VerticalDividerItemDecoration(resources.getDimensionPixelSize(R.dimen.seed_horizontal_margin), false)

        recycler_view_seeds.isNestedScrollingEnabled = false
        recycler_view_seeds.layoutManager = layoutManager
        recycler_view_seeds.addItemDecoration(verticalDivider)
        recycler_view_seeds.addItemDecoration(horizontalItemDecoration)
        recycler_view_seeds.adapter = SaveSeedAdapter(items)
    }

    inner class VerticalDividerItemDecoration(private val space: Int, private val verticalOrientation: Boolean) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView,
                                    state: RecyclerView.State) {
            if(verticalOrientation){
                outRect.set(0, 0, 0, space)
            }else{
                outRect.set(0, 0, space, 0)
            }
        }
    }

}