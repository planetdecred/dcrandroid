package com.dcrandroid.fragments

import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dcrandroid.R
import kotlinx.android.synthetic.main.password.*

class PasswordFragment : Fragment(), View.OnKeyListener {

    var seed: String? = null
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
                //Create Wallet
                true
            }else{
                Snackbar.make(v!!, R.string.mismatch_password, Snackbar.LENGTH_SHORT).show()
                false
            }
        }
        return false
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