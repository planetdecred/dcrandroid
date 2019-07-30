package com.dcrandroid.ui.security

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.dcrandroid.R
import com.dcrandroid.util.Utils
import kotlinx.android.synthetic.main.fragment_spending_pin.*
import kotlinx.android.synthetic.main.passcode.pin_strength as pin_strength1


class SpendingPinFragment(private var clickListener: DialogButtonListener) : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_spending_pin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ed_pin.addTextChangedListener(pinWatcher)
        ed_pin.addTextChangedListener(passwordStrengthWatcher)
        ed_confirm_pin.addTextChangedListener(pinWatcher)

        btn_cancel.setOnClickListener { clickListener.onClickCancel() }
        btn_create.setOnClickListener { clickListener.onClickOk(ed_pin.text.toString()) }
    }

    private val passwordStrengthWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            val progress = (Utils.getShannonEntropy(s.toString()) / 4) * 100
            if (progress > 70) {
                pin_strength.progressDrawable = resources.getDrawable(R.drawable.password_strength_bar_strong)
            } else {
                pin_strength.progressDrawable = resources.getDrawable(R.drawable.password_strength_bar_weak)
            }

            pin_strength.progress = progress.toInt()
        }
    }

    private val pinWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(editable: Editable) {
            val pinCount = ed_pin.text?.count()
            val confirmPinCount = ed_confirm_pin.text?.count()
            tv_confirm_pin_count.text = confirmPinCount.toString()
            tv_pin_count.text = pinCount.toString()

            if (ed_confirm_pin.text.toString() == "") {
                til_confirm_pin.error = null
            } else {
                if (ed_pin.text.toString() != ed_confirm_pin.text.toString()) {
                    til_confirm_pin.error = getString(R.string.mismatch_passcode)
                    btn_create.isEnabled = false
                } else {
                    til_confirm_pin.error = null
                    btn_create.isEnabled = true
                }
            }
        }
    }
}