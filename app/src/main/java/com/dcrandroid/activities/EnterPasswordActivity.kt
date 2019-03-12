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
import android.os.Bundle
import android.os.CancellationSignal
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.BiometricDialogV23
import com.dcrandroid.util.PreferenceUtil
import com.dcrandroid.util.Utils
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.change_password.*

class EnterPasswordActivity : AppCompatActivity() {

    private var isChange: Boolean? = null
    private var isSpendingPassword: Boolean? = null

    private var util: PreferenceUtil? = null

    private var biometricDialogV23: BiometricDialogV23? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.change_password)

        util = PreferenceUtil(this)

        isSpendingPassword = intent.getBooleanExtra(Constants.SPENDING_PASSWORD, true)
        isChange = intent.getBooleanExtra(Constants.CHANGE, false)

        if (isChange!!) {
            if (isSpendingPassword!!) {
                tv_prompt.text = getString(R.string.enter_current_spending_password)
            } else {
                tv_prompt.text = getString(R.string.enter_current_startup_password)
            }
        } else {
            if (isSpendingPassword!!) {
                tv_prompt.text = getString(R.string.enter_spending_password)
            } else {
                tv_prompt.text = getString(R.string.enter_startup_password)
            }

            checkBiometric()
        }

        btn_ok.setOnClickListener {
            val passphrase = password.text.toString()
            if (passphrase.isNotEmpty()) {
                if (isChange!!) {
                    val intent = Intent(this, ChangePassphrase::class.java)
                    intent.putExtra(Constants.PASSPHRASE, passphrase)
                    intent.putExtra(Constants.SPENDING_PASSWORD, isSpendingPassword!!)
                    startActivity(intent)
                    finish()
                } else {
                    val data = Intent()
                    data.putExtra(Constants.PASSPHRASE, passphrase)
                    setResult(Activity.RESULT_OK, data)
                    finish()
                }
            } else {
                password.error = getString(R.string.field_cannot_be_empty)
            }
        }
    }

    override fun onBackPressed() {
        if (!intent.getBooleanExtra(Constants.NO_RETURN, false)) {
            super.onBackPressed()
        }
    }

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
                        .setTitle(getString(R.string.app_name))
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
                biometricDialogV23!!.setTitle(getString(R.string.app_name))
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
            Toast.makeText(this@EnterPasswordActivity, errString, Toast.LENGTH_LONG).show()
            finishActivity()
        }
    }

    private val fingerprintAuthCallback = object : FingerprintManagerCompat.AuthenticationCallback() {

        override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
            super.onAuthenticationError(errMsgId, errString)
            Toast.makeText(this@EnterPasswordActivity, errString, Toast.LENGTH_LONG).show()
            if (biometricDialogV23 != null) {
                biometricDialogV23!!.dismiss()
            }
            finishActivity()
        }

        override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence?) {
            super.onAuthenticationHelp(helpMsgId, helpString)
            Toast.makeText(this@EnterPasswordActivity, helpString, Toast.LENGTH_SHORT).show()
        }

        override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
            super.onAuthenticationSucceeded(result)
            if (biometricDialogV23 != null) {
                biometricDialogV23!!.dismiss()
            }
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            Toast.makeText(this@EnterPasswordActivity, R.string.biometric_auth_failed, Toast.LENGTH_SHORT).show()
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