/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog

import android.content.DialogInterface
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.FileProvider
import com.dcrandroid.BuildConfig
import com.dcrandroid.R
import com.dcrandroid.adapter.DisabledAccounts
import com.dcrandroid.adapter.PopupItem
import com.dcrandroid.adapter.PopupUtil
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.Utils
import com.dcrandroid.view.util.AccountCustomSpinner
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.journeyapps.barcodescanner.BarcodeEncoder
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

        val disabledAccounts = EnumSet.of(DisabledAccounts.MixerMixedAccount)
        sourceAccountSpinner = AccountCustomSpinner(activity!!.supportFragmentManager, source_account_spinner, R.string.dest_account_picker_title, disabledAccounts) {
            setAddress(it.getCurrentAddress())
            return@AccountCustomSpinner Unit
        }
    }

    override fun onTxOrBalanceUpdateRequired(walletID: Long?) {
        super.onTxOrBalanceUpdateRequired(walletID)
        GlobalScope.launch(Dispatchers.Main) {
            sourceAccountSpinner.refreshBalance()
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
        val items: Array<Any> = arrayOf(
                PopupItem(R.string.generate_new_address)
        )

        PopupUtil.showPopup(v, items) { window, _ ->
            window.dismiss()

            generateNewAddress()
        }
    }

    private fun copyAddress() {
        Utils.copyToClipboard(top_bar, tv_address.text.toString(), R.string.address_copy_text)
    }

    private fun getLogoBitmap(): Bitmap {
        val logoDrawable = AppCompatResources.getDrawable(context!!, R.drawable.ic_qr_dcr)!!
        val sizePixels = resources.getDimensionPixelOffset(R.dimen.margin_padding_size_80)
        val bitmap = Bitmap.createBitmap(sizePixels, sizePixels, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        logoDrawable.setBounds(0, 0, canvas.width, canvas.height)

        val rect = Rect(0, 0, canvas.width, canvas.height)
        val rectF = RectF(rect)
        val paint = Paint()
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.isAntiAlias = true

        val radius = resources.getDimension(R.dimen.margin_padding_size_4)
        canvas.drawRoundRect(rectF, radius, radius, paint)
        logoDrawable.draw(canvas)


        return bitmap
    }

    private fun overlayLogo(qrBitmap: Bitmap): Bitmap {
        val overlay = Bitmap.createBitmap(qrBitmap.width, qrBitmap.height, qrBitmap.config)

        val logo = getLogoBitmap()
        val canvas = Canvas(overlay)
        canvas.drawBitmap(qrBitmap, 0f, 0f, null)
        canvas.drawBitmap(logo, ((qrBitmap.width - logo.width) / 2).toFloat(), ((qrBitmap.height - logo.height) / 2).toFloat(), null)
        return overlay
    }

    private fun setAddress(address: String) = GlobalScope.launch(Dispatchers.Main) {
        tv_address.text = address

        // Generate QR Code
        val barcodeEncoder = BarcodeEncoder()
        var generatedQR = barcodeEncoder.encodeBitmap(getString(R.string.decred_qr_address_prefix, address),
                BarcodeFormat.QR_CODE, 1000, 1000, qrHints)

        generatedQR = overlayLogo(generatedQR)

        qr_image.setImageBitmap(generatedQR)

        // generate URI
        withContext(Dispatchers.IO) {
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

    private fun generateNewAddress() {
        val oldAddress = tv_address.text.toString()
        var newAddress = sourceAccountSpinner.getNewAddress()
        if (oldAddress == newAddress) {
            newAddress = sourceAccountSpinner.getNewAddress()
        }

        setAddress(newAddress)
    }

    private fun shareQrImage() {
        if (generatedUri != null) {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.setDataAndType(generatedUri, context!!.contentResolver.getType(generatedUri!!))
            shareIntent.putExtra(Intent.EXTRA_STREAM, generatedUri)
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_address_via)))
        } else {
            SnackBar.showError(context!!, R.string.address_not_found)
        }

    }
}