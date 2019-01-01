package com.dcrandroid.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.change_password.*

class EnterPasswordActivity : AppCompatActivity() {

    private var isChange: Boolean? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.change_password)
        val isSpendingPassword = intent.getBooleanExtra(Constants.SPENDING_PASSWORD, true)

        isChange = intent.getBooleanExtra(Constants.CHANGE, false)

        if (isChange!!) {
            if (isSpendingPassword) {
                tv_prompt.text = getString(R.string.enter_current_spending_password)
            } else {
                tv_prompt.text = getString(R.string.enter_current_startup_password)
            }
        } else {
            if (isSpendingPassword) {
                tv_prompt.text = getString(R.string.enter_spending_password)
            } else {
                tv_prompt.text = getString(R.string.enter_startup_password)
            }
        }

        btn_ok.setOnClickListener {
            val passphrase = password.text.toString()
            if (passphrase.isNotEmpty()) {
                if (isChange!!) {
                    val intent = Intent(this, ChangePassphrase::class.java)
                    intent.putExtra(Constants.PASSPHRASE, passphrase)
                    intent.putExtra(Constants.SPENDING_PASSWORD, isSpendingPassword)
                    startActivity(intent)
                    finish()
                } else {
                    val data = Intent()
                    data.putExtra(Constants.PASSPHRASE, passphrase)
                    setResult(Activity.RESULT_OK, data)
                    finish()
                }
            } else {
                Snackbar.make(it, tv_prompt.text, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        if (!intent.getBooleanExtra(Constants.NO_RETURN, false)) {
            super.onBackPressed()
        }
    }

}