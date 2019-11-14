/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util

import android.content.Context
import com.dcrandroid.dialog.PasswordPromptDialog
import com.dcrandroid.dialog.PinPromptDialog
import dcrlibwallet.Dcrlibwallet

data class PassPromptTitle(val passwordTitle: Int, val pinTitle: Int, val fingerprintTitle: Int)
class PassPromptUtil(val context: Context, val walletID: Long, val isSpendingPass: Boolean,
                     val title: PassPromptTitle, val passEntered:(passphrase: String?) -> Unit) {

    fun show(){
        val multiWallet = WalletData.multiWallet!!
        val wallet = multiWallet.walletWithID(walletID)

        val passType = wallet.privatePassphraseType // TODO: Startup security
        if(passType == Dcrlibwallet.PassphraseTypePass){
            val passwordPromptDialog = PasswordPromptDialog(walletID, title.passwordTitle, passEntered)
            passwordPromptDialog.isCancelable = false
            passwordPromptDialog.show(context)
        }else{
            val pinPromptDialog = PinPromptDialog(walletID, title.passwordTitle, isSpendingPass, passEntered)
            pinPromptDialog.isCancelable = false
            pinPromptDialog.show(context)
        }
    }
}