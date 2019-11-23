/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util

import android.content.Context
import com.dcrandroid.dialog.CollapsedBottomSheetDialog
import com.dcrandroid.dialog.PasswordPromptDialog
import com.dcrandroid.dialog.PinPromptDialog
import dcrlibwallet.Dcrlibwallet

data class PassPromptTitle(val passwordTitle: Int, val pinTitle: Int, val fingerprintTitle: Int)

class PassPromptUtil(val context: Context, val walletID: Long?, val title: PassPromptTitle,
                     val passEntered:(dialog: CollapsedBottomSheetDialog, passphrase: String?) -> Boolean) {

    fun show(){
        val multiWallet = WalletData.multiWallet!!

        val passType = if(walletID != null){
            multiWallet.walletWithID(walletID).privatePassphraseType
        }else{
            multiWallet.readInt32ConfigValueForKey(Dcrlibwallet.StartupSecurityTypeConfigKey, Dcrlibwallet.PassphraseTypePass)
        }

        val isSpendingPass = walletID != null

        if(passType == Dcrlibwallet.PassphraseTypePass){
            val passwordPromptDialog = PasswordPromptDialog(title.passwordTitle, isSpendingPass, passEntered)
            passwordPromptDialog.isCancelable = false
            passwordPromptDialog.show(context)
        }else{
            val pinPromptDialog = PinPromptDialog(title.pinTitle, isSpendingPass, passEntered)
            pinPromptDialog.isCancelable = false
            pinPromptDialog.show(context)
        }
    }
}