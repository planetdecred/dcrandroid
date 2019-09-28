/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import com.dcrandroid.BuildConfig
import com.dcrandroid.R
import com.dcrandroid.adapter.PopupItem
import com.dcrandroid.adapter.PopupUtil
import com.dcrandroid.data.Account
import com.dcrandroid.view.util.AccountCustomSpinner
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.Utils
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.journeyapps.barcodescanner.BarcodeEncoder
import dcrlibwallet.LibWallet
import kotlinx.android.synthetic.main.receive_page_sheet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ReceiveDialog(dismissListener: DialogInterface.OnDismissListener) : FullScreenBottomSheetDialog(dismissListener) {

    private var wallet: LibWallet? = null

    private var selectedAccount: Account? = null

    private val qrHints = HashMap<EncodeHintType, Any>()
    private var generatedUri: Uri? = null

    private lateinit var sourceAccountSpinner: AccountCustomSpinner

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.receive_page_sheet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        qrHints[EncodeHintType.MARGIN] = 0
        qrHints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H

        share_qr_code.setOnClickListener { shareQrImage() }

        tv_address.setOnClickListener { copyAddress() }
        qr_image.setOnClickListener { copyAddress() }

        sourceAccountSpinner = AccountCustomSpinner(activity!!.supportFragmentManager, source_account_spinner, R.string.dest_account_picker_title) {
            selectedAccount = it
            wallet = multiWallet.getWallet(it.walletID)
            setAddress(wallet!!.currentAddress(it.accountNumber))
            return@AccountCustomSpinner Unit
        }
    }

    override fun showInfo() {
        InfoDialog(context!!)
                .setDialogTitle(getString(R.string.receive_dcr))
                .setMessage(getString(R.string.receive_fund_privacy_info))
                .setPositiveButton(getString(R.string.got_it), null)
                .show()
    }

    override fun showOptionsMenu(v: View) {
        val items = arrayOf(
                PopupItem(R.string.generate_new_address)
        )

        PopupUtil.showPopup(v, items){window, _ ->
            window.dismiss()

            generateNewAddress()
        }
    }

    private fun copyAddress(){
        Utils.copyToClipboard(top_bar, tv_address.text.toString(), R.string.address_copy_text)
    }

    private fun setAddress(address: String) = GlobalScope.launch(Dispatchers.Main){
        tv_address.text = address

        // Generate QR Code
        val barcodeEncoder = BarcodeEncoder()
        val generatedQR = barcodeEncoder.encodeBitmap(getString(R.string.decred_qr_address_prefix, address),
                BarcodeFormat.QR_CODE, 1000, 1000, qrHints)

        qr_image.setImageBitmap(generatedQR)

        // generate URI
        withContext(Dispatchers.IO){
            try {
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val now = Date()
                val fileName = formatter.format(now) + ".png"
                val cachePath = File(activity!!.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "wallet_address: $fileName")
                val stream = FileOutputStream(cachePath)
                generatedQR.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.flush()
                stream.close()
                generatedUri = FileProvider.getUriForFile(activity!!.applicationContext, BuildConfig.APPLICATION_ID + ".fileprovider", cachePath)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }


    }

    private fun generateNewAddress(){
        val oldAddress = tv_address.text.toString()
        var newAddress = wallet!!.nextAddress(selectedAccount!!.accountNumber)
        if(oldAddress == newAddress){
            newAddress = wallet!!.nextAddress(selectedAccount!!.accountNumber)
        }

        setAddress(newAddress)
    }

    private fun shareQrImage(){
        if(generatedUri != null){
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.setDataAndType(generatedUri, context!!.contentResolver.getType(generatedUri!!))
            shareIntent.putExtra(Intent.EXTRA_STREAM, generatedUri)
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_address_via)))
        }else{
            SnackBar.showError(context!!, R.string.address_not_found)
        }

    }
}