/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.view.PinView
import java.util.*

class KeyPad(private val keypadLayout: LinearLayout, private val pinView: PinView) : View.OnClickListener {
    private var del: TextView? = null
    private var ok: LinearLayout? = null
    private val keys: ArrayList<TextView>

    private val keyIds: List<Int>
    private var passCode = ""

    private var listener: KeyPadListener? = null

    init {
        val temp = arrayOf(R.id.keypad_0, R.id.keypad_1, R.id.keypad_2, R.id.keypad_3, R.id.keypad_4, R.id.keypad_5, R.id.keypad_6, R.id.keypad_7, R.id.keypad_8, R.id.keypad_9)
        this.keyIds = Arrays.asList(*temp)
        keys = ArrayList(keyIds.size)

        init()
    }

    private fun init() {
        for (i in keyIds.indices) {
            val key = keypadLayout.findViewById<TextView>(keyIds[i])
            key.setOnClickListener(this)
            keys.add(i, key)
        }

        del = keypadLayout.findViewById(R.id.keypad_del)
        del!!.setOnClickListener(this)

        ok = keypadLayout.findViewById(R.id.keypad_ok)
        ok!!.setOnClickListener(this)
    }

    fun disable() {
        keypadLayout.isEnabled = false
    }

    fun enable() {
        keypadLayout.isEnabled = true
    }

    fun reset() {
        passCode = ""
        pinView.passCodeLength = passCode.length
    }

    override fun onClick(v: View) {
        if (keyIds.indexOf(v.id) != -1) {
            val key = v as TextView
            passCode += key.text.toString()
            pinView.passCodeLength = passCode.length
            if (listener != null) {
                listener!!.onPinEnter(key.text.toString(), passCode)
            }
        } else if (R.id.keypad_del == v.id) {
            if (passCode.isNotEmpty()) {
                passCode = passCode.substring(0, passCode.length - 1)
                pinView.passCodeLength = passCode.length
            }

            if (listener != null) {
                listener!!.onPinEnter(null, passCode)
            }
        } else if (R.id.keypad_ok == v.id) {
            if(passCode == Constants.EMPTY_STRING){
                return
            }
            if (listener != null) {
                listener!!.onPassCodeCompleted(passCode)
            }
        }
    }

    fun setKeyListener(listener: KeyPadListener) {
        this.listener = listener
    }

    interface KeyPadListener {
        fun onPassCodeCompleted(passCode: String)
        fun onPinEnter(pin: String?, passCode: String)
    }

}
