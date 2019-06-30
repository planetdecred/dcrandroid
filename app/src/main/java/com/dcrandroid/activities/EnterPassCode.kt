/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.BiometricDialogV23
import com.dcrandroid.util.KeyPad
import com.dcrandroid.util.PreferenceUtil
import com.dcrandroid.util.Utils
import kotlinx.android.synthetic.main.passcode.*

class EnterPassCode : AppCompatActivity(), KeyPad.KeyPadListener {

    private var keyPad: KeyPad? = null
    private var isChange: Boolean? = null
    private var isSpendingPassword: Boolean? = null

    private var util: PreferenceUtil? = null

    private var biometricDialogV23: BiometricDialogV23? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val decorView = window.decorView
            decorView.systemUiVisibility = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        setContentView(R.layout.passcode)

        util = PreferenceUtil(this)

        pin_strength.visibility = View.GONE
        tv_pin_strength.visibility = View.GONE

        isChange = intent.getBooleanExtra(Constants.CHANGE, false)
        isSpendingPassword = intent.getBooleanExtra(Constants.SPENDING_PASSWORD, true)

        if (isChange!!) {
            keypad_instruction.setText(R.string.enter_current_pin)
        } else {
            if (isSpendingPassword!!) {
                keypad_instruction.setText(R.string.enter_spending_pin)
            } else {
                keypad_instruction.setText(R.string.enter_startup_pin)
            }

            checkBiometric()
        }

        keyPad = KeyPad(keypad, keypad_pin_view)
        keyPad!!.setKeyListener(this)
    }

    override fun onPassCodeCompleted(passCode: String) {
        if (isChange!!) {
            val intent = Intent(this, ChangePassphrase::class.java)
            intent.putExtra(Constants.PASSPHRASE, passCode)
            intent.putExtra(Constants.SPENDING_PASSWORD, isSpendingPassword)
            startActivity(intent)
            finish()
        } else {
            val data = Intent()
            data.putExtra(Constants.PASSPHRASE, passCode)
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }

    override fun onBackPressed() {
        if (!intent.getBooleanExtra(Constants.NO_RETURN, false)) {
            super.onBackPressed()
        }
    }

    override fun onPinEnter(pin: String?, passCode: String) {}

    private fun checkBiometric() {
        if (!util!!.getBoolean(Constants.USE_BIOMETRIC, false)) {
            println("Biometric not enabled in settings")
            return
        }

        val keyName = if (isSpendingPassword!!) Constants.SPENDING_PASSPHRASE_TYPE else Constants.STARTUP_PASSPHRASE_TYPE

        if (Utils.Biometric.isSupportBiometricPrompt(this)) {
            displayBiometricPrompt(keyName)
        } else if (Utils.Biometric.isSupportFingerprint(this)) {
            println("Device does support biometric prompt")
            showFingerprintDialog(keyName)
        }
    }

    @SuppressLint("NewApi")
    private fun displayBiometricPrompt(keyName: String) {
        try {
            Utils.Biometric.generateKeyPair(keyName, true)
            val signature = Utils.Biometric.initSignature(keyName)

            if (signature != null) {

                val biometricPrompt = BiometricPrompt.Builder(this)
                        .setTitle(getString(R.string.authentication_required))
                        .setNegativeButton("Cancel", mainExecutor, DialogInterface.OnClickListener { _, _ ->
                            finishActivity()
                        })
                        .build()

                biometricPrompt.authenticate(BiometricPrompt.CryptoObject(signature), getBiometricCancellationSignal(), mainExecutor, biometricAuthenticationCallback)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showFingerprintDialog(keyName: String) {
        val fingerprintManager = FingerprintManagerCompat.from(this)
        if (fingerprintManager.hasEnrolledFingerprints()) {

            Utils.Biometric.generateKeyPair(keyName, true)
            val signature = Utils.Biometric.initSignature(keyName)

            fingerprintManager.authenticate(FingerprintManagerCompat.CryptoObject(signature!!), 0,
                    getFingerprintCancellationSignal(), fingerprintAuthCallback, null)

            runOnUiThread {

                biometricDialogV23 = BiometricDialogV23(this)
                val cancelListener = object : BiometricDialogV23.CancelListener {
                    override fun onCancel() {
                        finishActivity()
                    }
                }

                biometricDialogV23!!.setCancelListener(cancelListener)
                biometricDialogV23!!.setTitle(R.string.authentication_required)
                biometricDialogV23!!.show()
            }
        }
    }

    @SuppressLint("NewApi")
    private fun getBiometricCancellationSignal(): CancellationSignal {
        // With this cancel signal, we can cancel biometric prompt operation
        val cancellationSignal = CancellationSignal()
        cancellationSignal.setOnCancelListener {
            println("Cancel result, signal triggered")
        }

        return cancellationSignal
    }

    @SuppressLint("NewApi")
    private fun getFingerprintCancellationSignal(): androidx.core.os.CancellationSignal {
        // With this cancel signal, we can cancel biometric prompt operation
        val cancellationSignal = androidx.core.os.CancellationSignal()
        cancellationSignal.setOnCancelListener {
            println("Cancel result, signal triggered")
        }

        return cancellationSignal
    }

    @SuppressLint("NewApi")
    private val biometricAuthenticationCallback = object : BiometricPrompt.AuthenticationCallback() {

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            super.onAuthenticationError(errorCode, errString)
            Toast.makeText(this@EnterPassCode, errString, Toast.LENGTH_LONG).show()
            finishActivity()
        }
    }

    private val fingerprintAuthCallback = object : FingerprintManagerCompat.AuthenticationCallback() {

        override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
            super.onAuthenticationError(errMsgId, errString)
            Toast.makeText(this@EnterPassCode, errString, Toast.LENGTH_LONG).show()
            if (biometricDialogV23 != null) {
                biometricDialogV23!!.dismiss()
            }
            finishActivity()
        }

        override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence?) {
            super.onAuthenticationHelp(helpMsgId, helpString)
            Toast.makeText(this@EnterPassCode, helpString, Toast.LENGTH_SHORT).show()
        }

        override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
            super.onAuthenticationSucceeded(result)
            if (biometricDialogV23 != null) {
                biometricDialogV23!!.dismiss()
            }
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            Toast.makeText(this@EnterPassCode, R.string.biometric_auth_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun finishActivity() {
        if (intent.getBooleanExtra(Constants.NO_RETURN, false)) {
            ActivityCompat.finishAffinity(this)
        } else {
            finish()
        }
    }
}