package com.dcrandroid.fragments

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.util.WalletData
import com.dcrandroid.util.PreferenceUtil
import com.dcrandroid.util.Utils
import com.google.android.material.snackbar.Snackbar
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.password.*

class ChangePasswordFragment : Fragment(), View.OnKeyListener {

    var oldPassphrase: String? = null
    var isSpendingPassword: Boolean? = null
    private var pd: ProgressDialog? = null
    private var util: PreferenceUtil? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.password, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        util = PreferenceUtil(context!!)
        if (!isSpendingPassword!!) {
            tv_prompt.setText(R.string.create_startup_password)
        }

        password.addTextChangedListener(passwordWatcher)
        password.addTextChangedListener(passwordStrengthWatcher)
        verifyPassword.addTextChangedListener(passwordWatcher)

        verifyPassword.setOnKeyListener(this)

        btn_ok.setOnClickListener {
            if (password.text.toString() == verifyPassword.text.toString()) {
                changePassword(password.text.toString())
            } else {
                Snackbar.make(it, R.string.mismatch_password, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ENTER && event!!.action == KeyEvent.ACTION_UP) {
            return if (password.text.toString() == verifyPassword.text.toString()) {
                changePassword(password.text.toString())
                true
            } else {
                Snackbar.make(v!!, R.string.mismatch_password, Snackbar.LENGTH_SHORT).show()
                false
            }
        }
        return false
    }

    private fun changePassword(password: String) {
        pd = Utils.getProgressDialog(context, false, false, "")
        Thread(Runnable {
            try {
                val wallet = WalletData.getInstance().wallet
                        ?: throw NullPointerException(getString(R.string.create_wallet_uninitialized))
                if (isSpendingPassword!!) {
                    show(getString(R.string.changing_password))
                } else {
                    show(getString(R.string.setting_startup_password))
                }
                val util = PreferenceUtil(this@ChangePasswordFragment.context!!)
                if (isSpendingPassword!!) {
                    wallet.changePrivatePassphrase(oldPassphrase!!.toByteArray(), password.toByteArray())
                    util.set(Constants.SPENDING_PASSPHRASE_TYPE, Constants.PASSWORD)
                } else {
                    wallet.changePublicPassphrase(oldPassphrase!!.toByteArray(), password.toByteArray())
                    util.set(Constants.STARTUP_PASSPHRASE_TYPE, Constants.PASSWORD)
                }

                activity!!.runOnUiThread {
                    if (pd!!.isShowing) {
                        pd!!.dismiss()
                    }
                    activity!!.setResult(Activity.RESULT_OK, Intent())
                    activity!!.finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity!!.runOnUiThread {
                    if (pd!!.isShowing) {
                        pd!!.dismiss()
                    }

                    if (e.message == Dcrlibwallet.ErrInvalidPassphrase) {
                        val message = if (util!!.get(Constants.SPENDING_PASSPHRASE_TYPE)
                                == Constants.PASSWORD) getString(R.string.invalid_current_password)
                        else getString(R.string.invalid_current_pin)

                        Toast.makeText(this@ChangePasswordFragment.context, message, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@ChangePasswordFragment.context, Utils.translateError(this@ChangePasswordFragment.context, e), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }).start()
    }

    private fun show(message: String) {
        if (activity == null) {
            return
        }
        activity!!.runOnUiThread {
            pd!!.setMessage(message)
            if (!pd!!.isShowing) {
                pd!!.show()
            }
        }
    }

    private val passwordStrengthWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            val progress = (Utils.getShannonEntropy(s.toString()) / 4) * 100
            if (progress > 70) {
                password_strength.progressDrawable = resources.getDrawable(R.drawable.password_strength_bar_strong)
            } else {
                password_strength.progressDrawable = resources.getDrawable(R.drawable.password_strength_bar_weak)
            }

            password_strength.progress = progress.toInt()
        }
    }

    private val passwordWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable) {
            if (verifyPassword.text.toString() == "") {
                passwordMatch.text = ""
            } else {
                if (password.text.toString() != verifyPassword.text.toString()) {
                    passwordMatch.setText(R.string.mismatch_password)
                    passwordMatch.setTextColor(Color.parseColor("#FFC84E"))
                } else {
                    passwordMatch.setText(R.string.password_match)
                    passwordMatch.setTextColor(Color.parseColor("#2DD8A3"))
                }
            }
        }
    }
}