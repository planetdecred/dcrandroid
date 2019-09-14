/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.content.FileProvider
import com.dcrandroid.BuildConfig
import com.dcrandroid.R
import com.dcrandroid.adapter.PopupItem
import com.dcrandroid.adapter.PopupUtil
import com.dcrandroid.data.Account
import com.dcrandroid.extensions.openedWalletsList
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.Utils
import com.dcrandroid.util.WalletData
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import dcrlibwallet.LibWallet
import kotlinx.android.synthetic.main.receive_page_sheet.*
import kotlinx.android.synthetic.main.tx_details_list_header.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.glxn.qrgen.android.MatrixToImageConfig
import net.glxn.qrgen.android.MatrixToImageWriter
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ReceiveDialog: BottomSheetDialogFragment() {

    private val multiWallet = WalletData.getInstance().multiWallet
    private var wallet: LibWallet? = null

    private var selectedAccount: Account? = null

    private val qrHints = HashMap<EncodeHintType, Any>()
    private var generatedUri: Uri? = null

    override fun getTheme(): Int = R.style.BottomSheetDialogStyle

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        val dialog: Dialog = BottomSheetDialog(requireContext(), theme)

        dialog.setOnShowListener {
            val bottomSheetDialog = dialog as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(R.id.design_bottom_sheet)
            val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet!!)

            val wm = context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            wm.defaultDisplay.getMetrics(metrics)

            bottomSheetBehavior.peekHeight = metrics.heightPixels

            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }
            })
        }

        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.receive_page_sheet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        qrHints[EncodeHintType.MARGIN] = 0
        qrHints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H

        go_back.setOnClickListener { dismiss() }

        iv_info.setOnClickListener {
            InfoDialog(context!!)
                    .setDialogTitle(getString(R.string.receive_dcr))
                    .setMessage(getString(R.string.receive_fund_privacy_info))
                    .setPositiveButton(getString(R.string.got_it), null)
                    .show()
        }

        iv_options.setOnClickListener {
            val items = arrayOf(
                    PopupItem(R.string.generate_new_address)
            )

            PopupUtil.showPopup(it, items){window, _ ->
                window.dismiss()

                generateNewAddress()
            }
        }

        share_qr_code.setOnClickListener { shareQrImage() }

        tv_address.setOnClickListener { copyAddress() }
        qr_image.setOnClickListener { copyAddress() }

        account_input_layout.setEndIconOnClickListener{
            AccountPickerDialog {
                selectedAccount = it
                wallet = multiWallet.getWallet(it.walletID)
                setAddress(wallet!!.currentAddress(it.accountNumber))
                return@AccountPickerDialog Unit
            }.show(activity!!.supportFragmentManager, null)
        }

        // Generate address from default account
        // for first wallet on the list
        wallet = multiWallet.openedWalletsList()[0]
        selectedAccount = Account.from(wallet!!.getAccount(0, 2))
        setAddress(wallet!!.currentAddress(selectedAccount!!.accountNumber))
    }

    private fun copyAddress(){
        Utils.copyToClipboard(context, tv_address.text.toString(), R.string.address_copy_text)
    }

    private fun setAddress(address: String) = GlobalScope.launch(Dispatchers.Main){
        tv_address.text = address

        // Generate QR Code
        val qrWriter = QRCodeWriter()
        val matrix = qrWriter.encode(getString(R.string.decred_qr_address_prefix, address),
                BarcodeFormat.QR_CODE,
                1000, 1000,
                qrHints)

        val generatedQR = MatrixToImageWriter.toBitmap(matrix, MatrixToImageConfig(Color.BLACK, Color.TRANSPARENT))
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