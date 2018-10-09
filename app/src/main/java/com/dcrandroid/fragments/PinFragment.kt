package com.dcrandroid.fragments

import android.app.ProgressDialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dcrandroid.R
import com.dcrandroid.util.DcrConstants
import com.dcrandroid.util.KeyPad
import com.dcrandroid.util.Utils
import android.support.v4.app.ActivityCompat
import com.dcrandroid.MainActivity
import android.content.Intent
import com.dcrandroid.data.Constants
import android.widget.Toast
import kotlinx.android.synthetic.main.passcode.*

class PinFragment : Fragment(), KeyPad.KeyPadListener {

    private var keyPad: KeyPad? = null
    private var passCode: String? = null
    private var step = 0
    private var pd: ProgressDialog? = null
    var seed : String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.passcode, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        keyPad = KeyPad(keypad, keypad_pin_view)
        keyPad!!.setKeyListener(this)
    }

    override fun onPassCodeCompleted(passCode: String) {
        if (step == 0) {
            this.passCode = passCode
            keypad_pin_view.postDelayed({
                keypad_instruction.setText(R.string.confirm_security_pin)
                keyPad!!.reset()
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
                    keypad_instruction.setText(R.string.create_security_pin)
                    keyPad!!.reset()
                    this@PinFragment.passCode = ""
                    step = 0
                    keyPad!!.enable()
                }, 1000)
            }
        }
    }

    private fun createWallet(){
        pd = Utils.getProgressDialog(context, false, false, "")
        Thread(Runnable {
            try{
                val wallet = DcrConstants.getInstance().wallet ?: throw NullPointerException(getString(R.string.create_wallet_uninitialized))
                show(getString(R.string.creating_wallet))
                wallet.createWallet(passCode, seed)
                activity!!.runOnUiThread {
                    pd!!.dismiss()
                    val i = Intent(this@PinFragment.context, MainActivity::class.java)
                    i.putExtra(Constants.PASSPHRASE, passCode)
                    startActivity(i)
                    //Finish all the activities before this
                    ActivityCompat.finishAffinity(this@PinFragment.activity!!)
                }
            }catch (e: Exception){
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

    private fun show(message: String){
        if(activity == null){
            return
        }
        activity!!.runOnUiThread {
            pd!!.setMessage(message)
            if (!pd!!.isShowing){
                pd!!.show()
            }
        }
    }
}