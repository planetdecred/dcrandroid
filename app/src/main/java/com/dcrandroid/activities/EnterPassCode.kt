package com.dcrandroid.activities

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.util.KeyPad
import kotlinx.android.synthetic.main.passcode.*

class EnterPassCode : AppCompatActivity(), KeyPad.KeyPadListener {

    private var keyPad: KeyPad? = null
    private var isChange: Boolean? = null
    private var isSpendingPassword: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val decorView = window.decorView
            decorView.systemUiVisibility = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        setContentView(R.layout.passcode)

        pin_strength.visibility = View.GONE
        tv_pin_strength.visibility = View.GONE

        isChange = intent.getBooleanExtra(Constants.CHANGE, false)
        isSpendingPassword = intent.getBooleanExtra(Constants.SPENDING_PASSWORD, true)

        if (isChange!!) {
            keypad_instruction.setText(R.string.enter_current_pin)
        } else {
            if (isSpendingPassword!!) {
                keypad_instruction.setText(R.string.enter_spending_pin)
            } else {
                keypad_instruction.setText(R.string.enter_startup_pin)
            }
        }

        keyPad = KeyPad(keypad, keypad_pin_view)
        keyPad!!.setKeyListener(this)
    }

    override fun onPassCodeCompleted(passCode: String) {
        if (isChange!!) {
            val intent = Intent(this, ChangePassphrase::class.java)
            intent.putExtra(Constants.PASSPHRASE, passCode)
            intent.putExtra(Constants.SPENDING_PASSWORD, isSpendingPassword)
            startActivity(intent)
            finish()
        } else {
            val data = Intent()
            data.putExtra(Constants.PASSPHRASE, passCode)
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }

    override fun onBackPressed() {
        if (!intent.getBooleanExtra(Constants.NO_RETURN, false)) {
            super.onBackPressed()
        }
    }

    override fun onPinEnter(pin: String?, passCode: String) {}
}