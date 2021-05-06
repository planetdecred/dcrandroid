/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.preference

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dcrandroid.R
import com.dcrandroid.dialog.FullScreenBottomSheetDialog
import com.dcrandroid.view.util.InputHelper
import kotlinx.android.synthetic.main.edit_text_preference_dialog.*

class EditTextPreference(val context: Context, val key: String, val title: Int, val dialogHint: Int, var errorString: Int? = null,
                         val view: View, val validateInput: ((String) -> Boolean) = { true },
                         val valueChanged: ((newValue: String) -> Unit)? = null) : Preference(context, key, view), View.OnClickListener {

    init {
        view.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        EditTextDialog(title, dialogHint, key, errorString, validateInput, valueChanged).show(context)
    }
}

class EditTextDialog(val title: Int, val dialogHint: Int, val key: String, var errorString: Int? = null,
                     val validateInput: (String) -> Boolean, val valueChanged: ((newValue: String) -> Unit)? = null) : FullScreenBottomSheetDialog() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.edit_text_preference_dialog, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        sheet_title.setText(title)

        val inputHelper = InputHelper(context!!, et_preference, validateInput).apply {
            if (errorString != null) {
                validationMessage = errorString!!
            }
            hidePasteButton()
            hideQrScanner()
            setHint(dialogHint)
        }

        inputHelper.textChanged = {
            btn_confirm.isEnabled = inputHelper.validatedInput != null
        }

        inputHelper.editText.setText(multiWallet.readStringConfigValueForKey(key))

        btn_cancel.setOnClickListener { dismiss() }

        btn_confirm.setOnClickListener {
            val newValue = inputHelper.validatedInput!!
            multiWallet.setStringConfigValueForKey(key, inputHelper.validatedInput)
            valueChanged?.invoke(newValue)
            dismiss()
        }
    }
}