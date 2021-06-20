package com.dcrandroid.activities.coincontrol

import android.os.Bundle
import android.view.View
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.adapter.PopupItem
import com.dcrandroid.adapter.PopupUtil
import com.dcrandroid.dialog.InfoDialog
import kotlinx.android.synthetic.main.activity_coin_control.*
import kotlinx.android.synthetic.main.coin_control_input_list_row.*

class CoinControlActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coin_control)

        iv_info.setOnClickListener {
            InfoDialog(this)
                    .setDialogTitle("Select Inputs")
                    .setMessage(getString(R.string.coin_control_info))
                    .setPositiveButton(getString(R.string.got_it), null)
                    .show()
        }

        iv_options.setOnClickListener {
            val items: Array<Any> = arrayOf(
                    PopupItem(R.string.clear_fields)
            )

            PopupUtil.showPopup(it, items) { window, _ ->
                window.dismiss()
                clearFields()
            }
        }

        go_back.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    private fun clearFields() {
        checkBox.isChecked = false
    }
}