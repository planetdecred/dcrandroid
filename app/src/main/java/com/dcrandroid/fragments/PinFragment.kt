package com.dcrandroid.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.dcrandroid.R
import com.dcrandroid.util.KeyPad
import com.dcrandroid.view.PinView

class PinFragment : Fragment(), KeyPad.KeyPadListener {

    private var pinView: PinView? = null
    private var keyPadLayout: LinearLayout? = null
    private var pinInstruction: TextView? = null
    private var keyPad: KeyPad? = null
    private var passCode: String? = null
    private var step = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val vi = inflater.inflate(R.layout.keypad, container, false)
        pinView = vi.findViewById(R.id.keypad_pin_view)
        pinInstruction = vi.findViewById(R.id.keypad_instruction)
        keyPadLayout = vi.findViewById(R.id.keypad)
        return vi
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        keyPad = KeyPad(keyPadLayout!!, pinView!!)

        keyPad!!.setKeyListener(this)
    }

    override fun onPassCodeCompleted(passCode: String) {
        if (step == 0) {
            this.passCode = passCode
            pinView!!.postDelayed({
                pinInstruction!!.text = "Confirm Security PIN"
                keyPad!!.reset()
            }, 100)
            step++
        } else if (step == 1) {
            if (this.passCode == passCode) {
                keyPad!!.disable()
            } else {
                keyPad!!.disable()
                pinInstruction!!.text = "PINs did not match. Try again"
                pinView!!.postDelayed({
                    pinInstruction!!.text = "Create Security PIN"
                    keyPad!!.reset()
                    this@PinFragment.passCode = ""
                    step = 0
                    keyPad!!.enable()
                }, 1000)
            }
        }
    }
}