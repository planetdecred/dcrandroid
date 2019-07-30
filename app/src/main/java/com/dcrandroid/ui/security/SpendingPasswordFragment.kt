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
import kotlinx.android.synthetic.main.fragment_spending_password.*
import kotlinx.android.synthetic.main.password.password_strength


class SpendingPasswordFragment(private var clickListener: DialogButtonListener) : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_spending_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ed_password.addTextChangedListener(passwordWatcher)
        ed_password.addTextChangedListener(passwordStrengthWatcher)
        ed_confirm_password.addTextChangedListener(passwordWatcher)

        btn_cancel.setOnClickListener { clickListener.onClickCancel() }
        btn_create.setOnClickListener { clickListener.onClickOk(ed_password.text.toString()) }
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

        override fun afterTextChanged(editable: Editable) {
            val passwordCount = ed_password.text?.count()
            val confirmPasswordCount = ed_confirm_password.text?.count()
            tv_confirm_password_count.text = confirmPasswordCount.toString()
            tv_password_count.text = passwordCount.toString()

            if (ed_confirm_password.text.toString() == "") {
                til_confirm_password.error = null
            } else {
                if (ed_password.text.toString() != ed_confirm_password.text.toString()) {
                    til_confirm_password.error = getString(R.string.mismatch_password)
                    btn_create.isEnabled = false
                } else {
                    til_confirm_password.error = null
                    btn_create.isEnabled = true
                }
            }
        }
    }

}
