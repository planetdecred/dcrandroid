/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.InfoDialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import kotlinx.android.synthetic.main.qr_reader_layout.*

class ReaderActivity : BaseActivity(), ActivityCompat.OnRequestPermissionsResultCallback, BarcodeCallback {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.qr_reader_layout)

        val allowed = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (allowed == PackageManager.PERMISSION_DENIED) {
            val dialog = InfoDialog(this)
                    .setDialogTitle(getString(R.string.permission))
                    .setMessage(getString(R.string.camera_permission_scan))
                    .setPositiveButton(getString(R.string.ok), DialogInterface.OnClickListener { _, _ ->
                        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.VIBRATE)
                        ActivityCompat.requestPermissions(this@ReaderActivity, permissions, 200)
                    })
            dialog.setCancelable(false)
            dialog.show()
        } else {
            startCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //permission granted
                startCamera()
            } else {
                Toast.makeText(this, R.string.denied_permission, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        barcode_scanner.pause()
    }

    override fun onResume() {
        super.onResume()
        barcode_scanner.resume()
    }

    override fun barcodeResult(result: BarcodeResult?) {
        if (result != null) {
            val data = Intent()
            data.putExtra(Constants.RESULT, result.text)
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }

    override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}

    private fun startCamera() {
        val formats = listOf(BarcodeFormat.QR_CODE)
        barcode_scanner.barcodeView.decoderFactory = DefaultDecoderFactory(formats)
        barcode_scanner.decodeSingle(this)
        barcode_scanner.setStatusText(getString(R.string.qr_code_scan_hint))
        barcode_scanner.initializeFromIntent(intent)
    }

}
