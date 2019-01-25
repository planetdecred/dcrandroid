package com.dcrandroid.fragments

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.dcrandroid.MainActivity
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.util.KeyPad
import com.dcrandroid.util.PreferenceUtil
import com.dcrandroid.util.Utils
import com.dcrandroid.util.WalletData
import kotlinx.android.synthetic.main.passcode.*

class PinFragment : Fragment(), KeyPad.KeyPadListener {

    private var keyPad: KeyPad? = null
    private var passCode: String? = null
    private var step = 0
    private var pd: ProgressDialog? = null
    var seed: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.passcode, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        keyPad = KeyPad(keypad, keypad_pin_view)
        keyPad!!.setKeyListener(this)
    }

    override fun onPassCodeCompleted(passCode: String) {
        if (passCode.isNotEmpty()) {
            if (step == 0) {
                this.passCode = passCode
                keypad_pin_view.postDelayed({
                    keypad_instruction.setText(R.string.confirm_spending_pin)
                    keyPad!!.reset()
                    pin_strength.progress = 0
                }, 100)
                step++
            } else if (step == 1) {
                if (this.passCode == passCode) {
                    keyPad!!.disable()
                    createWallet()
                } else {
                    keyPad!!.disable()
                    keypad_instruction.setText(R.string.mismatch_passcode)
                    keypad_pin_view.postDelayed({
                        keypad_instruction.setText(R.string.create_spending_pin)
                        keyPad!!.reset()
                        this@PinFragment.passCode = ""
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

    private fun createWallet() {
        pd = Utils.getProgressDialog(context, false, false, "")
        Thread(Runnable {
            try {
                val wallet = WalletData.getInstance().wallet
                        ?: throw NullPointerException(getString(R.string.create_wallet_uninitialized))
                show(getString(R.string.creating_wallet))
                wallet.createWallet(passCode, seed)
                wallet.unlockWallet(passCode!!.toByteArray())
                val util = PreferenceUtil(this@PinFragment.context!!)
                util.set(Constants.SPENDING_PASSPHRASE_TYPE, Constants.PIN)
                activity!!.runOnUiThread {
                    pd!!.dismiss()
                    val i = Intent(this@PinFragment.context, MainActivity::class.java)
                    i.putExtra(Constants.PASSPHRASE, passCode)
                    startActivity(i)
                    //Finish all the activities before this
                    ActivityCompat.finishAffinity(this@PinFragment.activity!!)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity!!.runOnUiThread {
                    if (pd!!.isShowing) {
                        pd!!.dismiss()
                    }
                    Toast.makeText(this@PinFragment.context, Utils.translateError(this@PinFragment.context, e), Toast.LENGTH_LONG).show()
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