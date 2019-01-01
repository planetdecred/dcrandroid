package com.dcrandroid.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.ImageView
import com.dcrandroid.R

class StakeyDialog(context: Context?) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT))
        val stakey = ImageView(context)
        stakey.setImageResource(R.drawable.stakey_deal_with_it)
        setContentView(stakey)

        Thread(Runnable {
            try {
                Thread.sleep(7000)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            dismiss()
        }).start()
    }
}