/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import com.dcrandroid.HomeActivity
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import java.math.BigDecimal
import java.math.MathContext
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.ArrayList
import java.util.Locale
import java.util.concurrent.TimeUnit

import dcrlibwallet.Dcrlibwallet
import java.io.*
import android.security.keystore.KeyProperties
import android.security.keystore.KeyGenParameterSpec
import androidx.biometric.BiometricConstants
import androidx.biometric.BiometricPrompt
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.fragment.app.FragmentActivity
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec


object Utils {

    fun getHash(hash: String): ByteArray? {
        val hashList = ArrayList<String>()
        val split = hash.split("".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if ((split.size - 1) % 2 == 0) {
            var d = ""
            var i = 0
            while (i < split.size - 1) {
                d += (split[split.size - 1 - (i + 1)] + split[split.size - 1 - i])
                hashList.add(split[split.size - 1 - (i + 1)] + split[split.size - 1 - i])
                i += 2
            }
            return hexStringToByteArray(d)
        } else {
            System.err.println("Invalid Hash")
        }
        return null
    }

    fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    fun getDaysBehind(seconds: Long, context: Context): String {
        val days = TimeUnit.SECONDS.toDays(seconds)
        return if (days == 1L) {
            context.getString(R.string.one_days_behind)
        } else context.getString(R.string.days_behind, days)

    }

    fun calculateTime(seconds: Long, context: Context): String {
        var secs = seconds
        if (secs > 59) {

            // convert to minutes
            val minutes = secs / 60

            if (minutes > 59) {

                // convert to hours
                val hours =  minutes / 60

                if (hours > 23) {

                    // convert to days
                    val days = hours / 24

                    //days
                    return context.getString(R.string.x_days, days)
                }
                //hour
                return context.getString(R.string.x_hours, hours)
            }

            //minutes
            return context.getString(R.string.x_minutes, minutes)
        }

        if (secs < 0) {
            secs = 0
        }

        //seconds
        return context.getString(R.string.x_seconds, secs)
    }

    fun getSyncTimeRemaining(seconds: Long, percentageCompleted: Int, useLeft: Boolean, ctx: Context): String {
        if (seconds > 1) {

            if (seconds > 60) {
                val minutes = seconds / 60
                return if (useLeft) {
                    ctx.getString(R.string.left_minute_sync_eta, percentageCompleted, minutes)
                } else ctx.getString(R.string.remaining_minute_sync_eta, percentageCompleted, minutes)

            }

            return if (useLeft) {
                ctx.getString(R.string.left_seconds_sync_eta, percentageCompleted, seconds)
            } else ctx.getString(R.string.remaining_seconds_sync_eta, percentageCompleted, seconds)

        }

        return if (useLeft) {
            ctx.getString(R.string.left_sync_eta_less_than_seconds, percentageCompleted)
        } else ctx.getString(R.string.remaining_sync_eta_less_than_seconds, percentageCompleted)

    }

    fun getSyncTimeRemaining(seconds: Long, ctx: Context): String {
        if (seconds > 60) {
            val minutes = seconds / 60

            return ctx.getString(R.string.time_left_minutes, minutes)
        }

        return ctx.getString(R.string.time_left_seconds, seconds)
    }

    fun removeTrailingZeros(dcr: Double): String {
        val format = NumberFormat.getNumberInstance(Locale.ENGLISH) as DecimalFormat
        format.applyPattern("#,###,###,##0.########")
        return format.format(dcr)
    }

    fun formatDecredWithComma(dcr: Long): String {
        val convertedDcr = Dcrlibwallet.amountCoin(dcr)
        val df = NumberFormat.getNumberInstance(Locale.ENGLISH) as DecimalFormat
        df.applyPattern("#,###,###,##0.########")
        return df.format(convertedDcr)
    }

    fun formatDecredWithoutComma(dcr: Long): String {
        val atom = BigDecimal(dcr)
        val amount = atom.divide(BigDecimal.valueOf(1e8), MathContext(100))
        val format = NumberFormat.getNumberInstance(Locale.ENGLISH) as DecimalFormat
        format.applyPattern("#########0.########")
        return format.format(amount)
    }

    private fun saveToClipboard(context: Context, text: String) {

        val sdk = android.os.Build.VERSION.SDK_INT
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.text.ClipboardManager
            clipboard.text = text
        } else {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData
                    .newPlainText(context.getString(R.string.your_address), text)
            clipboard.primaryClip = clip
        }
    }

    fun copyToClipboard(v: View, text: String, @StringRes successMessage: Int) {
        saveToClipboard(v.context, text)
        SnackBar.showText(v, successMessage, Toast.LENGTH_SHORT)
    }

    fun copyToClipboard(context: Context, text: String, @StringRes successMessage: Int) {
        saveToClipboard(context, text)
        SnackBar.showText(context, successMessage, Toast.LENGTH_SHORT)
    }

    fun readFromClipboard(context: Context): String {
        val sdk = android.os.Build.VERSION.SDK_INT
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.text.ClipboardManager
            return clipboard.text.toString()
        } else {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            if (clipboard.hasPrimaryClip())
                return clipboard.primaryClip!!.getItemAt(0).text.toString()
        }
        return ""
    }

    fun translateError(ctx: Context, e: Exception): String {
        return when (e.message) {
            Dcrlibwallet.ErrInsufficientBalance -> {
                if (!WalletData.instance.synced) {
                    ctx.getString(R.string.not_enought_funds_synced)
                } else ctx.getString(R.string.not_enough_funds)
            }
            Dcrlibwallet.ErrEmptySeed -> ctx.getString(R.string.empty_seed)
            Dcrlibwallet.ErrNotConnected -> ctx.getString(R.string.not_connected)
            Dcrlibwallet.ErrPassphraseRequired -> ctx.getString(R.string.passphrase_required)
            Dcrlibwallet.ErrWalletNotLoaded -> ctx.getString(R.string.wallet_not_loaded)
            Dcrlibwallet.ErrInvalidPassphrase -> ctx.getString(R.string.invalid_passphrase)
            Dcrlibwallet.ErrNoPeers -> ctx.getString(R.string.err_no_peers)
            else -> e.message!!
        }
    }

    fun restartApp(context: Context) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent != null) {
            val componentName = intent.component
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            context.startActivity(mainIntent)
            Runtime.getRuntime().exit(0)
        }
    }

    fun sendTransactionNotification(context: Context, manager: NotificationManager, amount: String,
                                    nonce: Int, multiWallet: Boolean, walletName: String?) {

        val title: String
        if (multiWallet) {
            title = context.getString(R.string.wallet_new_transaction, walletName)
        } else {
            title = context.getString(R.string.new_transaction)
        }

        val launchIntent = Intent(context, HomeActivity::class.java)
        launchIntent.action = Constants.NEW_TRANSACTION_NOTIFICATION
        val launchPendingIntent = PendingIntent.getActivity(context, 1, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, "new transaction")
                .setContentTitle(title)
                .setContentText(amount)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setOngoing(false)
                .setAutoCancel(true)
                .setGroup(Constants.TRANSACTION_NOTIFICATION_GROUP)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(launchPendingIntent)
                .build()

        val groupSummary = NotificationCompat.Builder(context, "new transaction")
                .setContentTitle(context.getString(R.string.new_transaction))
                .setContentText(context.getString(R.string.new_transaction))
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setGroup(Constants.TRANSACTION_NOTIFICATION_GROUP)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

        manager.notify(nonce, notification)
        manager.notify(Constants.TRANSACTION_SUMMARY_ID, groupSummary)
    }


    @Throws(Exception::class)
    fun readFileToBytes(path: String): ByteArray {
        val fin = FileInputStream(path)
        val out = ByteArrayOutputStream()
        val buff = ByteArray(8192)
        var len = fin.read(buff)

        while (len != -1) {
            out.write(buff, 0, len)
            len = fin.read(buff)
        }

        out.flush()
        fin.close()

        return out.toByteArray()
    }

    @Throws(Exception::class)
    fun writeBytesToFile(output: ByteArray, path: String) {
        val file = File(path)

        val fout = FileOutputStream(file)
        val bin = ByteArrayInputStream(output)

        val buff = ByteArray(8192)
        var len = bin.read(buff)

        while (len != -1) {
            fout.write(buff, 0, len)
            len = bin.read(buff)
        }

        fout.flush()
        fout.close()
        bin.close()
    }
}

object BiometricUtils {

    fun getFilePath(context: Context, fileName: String): String {
        val path = File(context.filesDir.toString() + "/auth/")
        if (!path.exists()) {
            path.mkdir()
        }
        return path.absolutePath + fileName
    }

    @Throws(Exception::class)
    fun readFromFile(context: Context, fileName: String): ByteArray {
        val path = getFilePath(context, fileName)
        return Utils.readFileToBytes(path)
    }

    @Throws(Exception::class)
    fun saveToFile(context: Context, fileName: String, output: ByteArray) {
        val path = getFilePath(context, fileName)
        Utils.writeBytesToFile(output, path)
    }

    @Throws(Exception::class)
    fun savePassToKeystore(context: Context, pass: String, alias: String) {
        val keyGenerator = KeyGenerator
                .getInstance(KeyProperties.KEY_ALGORITHM_AES, Constants.ANDROID_KEY_STORE)

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()

        keyGenerator.init(keyGenParameterSpec)
        val secretKey = keyGenerator.generateKey()

        val cipher = Cipher.getInstance(Constants.TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.getIV()
        saveToFile(context, Constants.ENCRYPTION_IV, iv)

        val encryption = cipher.doFinal(pass.toByteArray(StandardCharsets.UTF_8))
        saveToFile(context, Constants.ENCRYPTION_DATA, encryption)
    }

    @Throws(Exception::class)
    fun getPassFromKeystore(context: Context, alias: String): String {

        val encryptionIv = readFromFile(context, Constants.ENCRYPTION_IV)
        val encryptedData = readFromFile(context, Constants.ENCRYPTION_DATA)

        val keyStore = KeyStore.getInstance(Constants.ANDROID_KEY_STORE)
        keyStore.load(null)

        val secretKeyEntry = keyStore.getEntry(alias, null)  as KeyStore.SecretKeyEntry
        val secretKey = secretKeyEntry.secretKey
        val cipher = Cipher.getInstance(Constants.TRANSFORMATION)
        val spec =  GCMParameterSpec(128, encryptionIv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decodedData = cipher.doFinal(encryptedData)
        return String(decodedData, StandardCharsets.UTF_8)
    }

     fun isFingerprintEnrolled(context: Context) :Boolean {
        val fingerprintManager = FingerprintManagerCompat.from(context)
        return fingerprintManager.hasEnrolledFingerprints()
     }


    fun displayBiometricPrompt(activity: FragmentActivity, callback: BiometricPrompt.AuthenticationCallback): Boolean {
        if (isFingerprintEnrolled(activity)) {
            val executor = Executors.newSingleThreadExecutor()
            val biometricPrompt = BiometricPrompt(activity, executor, callback);
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(activity.getString(R.string.authentication_required))
                    .setNegativeButtonText(activity.getString(R.string.cancel))
                    .build()

            biometricPrompt.authenticate(promptInfo)
            return true
        }

        return false
    }

    fun translateError(context: Context, errorCode: Int): String? {
        return when (errorCode) {
            BiometricConstants.ERROR_LOCKOUT ->  context.getString(R.string.biometric_lockout_error)
            BiometricConstants.ERROR_LOCKOUT_PERMANENT -> context.getString(R.string.biometric_permanent_lockout_error)
            else -> null
        }
    }
}

