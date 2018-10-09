package com.dcrandroid.fragments

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.dcrandroid.MainActivity
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.util.DcrConstants
import com.dcrandroid.util.PreferenceUtil
import com.dcrandroid.util.Utils
import kotlinx.android.synthetic.main.password.*

class PasswordFragment : Fragment(), View.OnKeyListener {

    var seed: String? = null
    private var pd: ProgressDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.password, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        password.addTextChangedListener(passwordWatcher)
        verifyPassword.addTextChangedListener(passwordWatcher)

        verifyPassword.setOnKeyListener(this)
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode == KeyEvent.KEYCODE_ENTER && event!!.action == KeyEvent.ACTION_UP){
            return if(password.text.toString() == verifyPassword.text.toString()){
                createWallet(password.text.toString())
                true
            }else{
                Snackbar.make(v!!, R.string.mismatch_password, Snackbar.LENGTH_SHORT).show()
                false
            }
        }
        return false
    }

    private fun createWallet(password : String){
        pd = Utils.getProgressDialog(context, false, false, "")
        Thread(Runnable {
            try{
                val wallet = DcrConstants.getInstance().wallet ?: throw NullPointerException(getString(R.string.create_wallet_uninitialized))
                show(getString(R.string.creating_wallet))
                wallet.createWallet(password, seed)
                val util = PreferenceUtil(this@PasswordFragment.context!!)
                util.set(Constants.PASSPHRASE_TYPE, Constants.PASSWORD)
                activity!!.runOnUiThread {
                    pd!!.dismiss()
                    val i = Intent(this@PasswordFragment.context, MainActivity::class.java)
                    i.putExtra(Constants.PASSPHRASE, password)
                    startActivity(i)
                    //Finish all the activities before this
                    ActivityCompat.finishAffinity(this@PasswordFragment.activity!!)
                }
            }catch (e: Exception){
                e.printStackTrace()
                activity!!.runOnUiThread {
                    if (pd!!.isShowing) {
                        pd!!.dismiss()
                    }
                    Toast.makeText(this@PasswordFragment.context, Utils.translateError(this@PasswordFragment.context, e), Toast.LENGTH_LONG).show()
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

    private val passwordWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable) {
            if (verifyPassword.text.toString() == ""){
                passwordMatch.text = ""
            }else{
                if(password.text.toString() != verifyPassword.text.toString()){
                    passwordMatch.setText(R.string.mismatch_password)
                    passwordMatch.setTextColor(Color.parseColor("#FFC84E"))
                }else{
                    passwordMatch.setText(R.string.password_match)
                    passwordMatch.setTextColor(Color.parseColor("#2DD8A3"))
                }
            }
        }
    }
}