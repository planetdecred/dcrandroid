package com.dcrandroid.fragments

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.util.DcrConstants
import com.dcrandroid.util.KeyPad
import com.dcrandroid.util.PreferenceUtil
import com.dcrandroid.util.Utils
import kotlinx.android.synthetic.main.passcode.*
import mobilewallet.Mobilewallet

class ChangePinFragment : Fragment(), KeyPad.KeyPadListener {

    private var keyPad: KeyPad? = null
    private var passCode: String? = null
    private var step = 0
    private var pd: ProgressDialog? = null

    private var util: PreferenceUtil? = null

    var isSpendingPassword: Boolean? = null
    var oldPassphrase: String? = null

    private var createHint: String? = null
    private var confirmHint: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.passcode, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        util = PreferenceUtil(context!!)
        if (isSpendingPassword!!) {
            confirmHint = getString(R.string.confirm_spending_pin)
            createHint = getString(R.string.create_spending_pin)
        } else {
            createHint = getString(R.string.create_startup_pin)
            confirmHint = getString(R.string.confirm_startup_pin)
        }

        keypad_instruction.text = createHint

        keyPad = KeyPad(keypad, keypad_pin_view)
        keyPad!!.setKeyListener(this)
    }

    override fun onPassCodeCompleted(passCode: String) {
        if (passCode.isNotEmpty()) {
            if (step == 0) {
                this.passCode = passCode
                keypad_pin_view.postDelayed({
                    keypad_instruction.text = confirmHint
                    keyPad!!.reset()
                    pin_strength.progress = 0
                }, 100)
                step++
            } else if (step == 1) {
                if (this.passCode == passCode) {
                    keyPad!!.disable()
                    changePin()
                } else {
                    keyPad!!.disable()
                    keypad_instruction.setText(R.string.mismatch_passcode)
                    keypad_pin_view.postDelayed({
                        keypad_instruction.text = createHint
                        keyPad!!.reset()
                        this@ChangePinFragment.passCode = ""
                        step = 0
                        pin_strength.progress = 0
                        keyPad!!.enable()
                    }, 1000)
                }
            }
        }
    }

    override fun onPinEnter(pin: String?, passCode: String) {
        val progress = (Utils.getShannonEntropy(passCode) / 4) * 100
        if (progress > 70) {
            pin_strength.progressDrawable = resources.getDrawable(R.drawable.password_strength_bar_strong)
        } else {
            pin_strength.progressDrawable = resources.getDrawable(R.drawable.password_strength_bar_weak)
        }

        pin_strength.progress = progress.toInt()
    }

    private fun changePin() {
        pd = Utils.getProgressDialog(context, false, false, "")
        Thread(Runnable {
            try {
                val wallet = DcrConstants.getInstance().wallet
                        ?: throw NullPointerException(getString(R.string.create_wallet_uninitialized))
                if (isSpendingPassword!!) {
                    show(getString(R.string.changing_pin))
                } else {
                    show(getString(R.string.setting_startup_pin))
                }
                val util = PreferenceUtil(this@ChangePinFragment.context!!)
                if (isSpendingPassword!!) {
                    wallet.changePrivatePassphrase(oldPassphrase!!.toByteArray(), passCode!!.toByteArray())
                    util.set(Constants.SPENDING_PASSPHRASE_TYPE, Constants.PIN)
                } else {
                    wallet.changePublicPassphrase(oldPassphrase!!.toByteArray(), passCode!!.toByteArray())

                    util.set(Constants.STARTUP_PASSPHRASE_TYPE, Constants.PIN)
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
                    if (e.message == Mobilewallet.ErrInvalidPassphrase) {
                        val message = if (util!!.get(Constants.SPENDING_PASSPHRASE_TYPE)
                                == Constants.PASSWORD) getString(R.string.invalid_current_password)
                        else getString(R.string.invalid_current_pin)

                        Toast.makeText(this@ChangePinFragment.context, message, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@ChangePinFragment.context, Utils.translateError(this@ChangePinFragment.context, e), Toast.LENGTH_LONG).show()
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
}