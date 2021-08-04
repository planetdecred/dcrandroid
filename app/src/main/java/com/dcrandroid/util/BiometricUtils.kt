/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricPrompt
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.fragment.app.FragmentActivity
import com.dcrandroid.data.Constants
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

object BiometricUtils {

    fun getFilePath(context: Context, fileName: String): String {
        val path = File(context.filesDir.toString() + "/auth/")
        if (!path.exists()) {
            path.mkdir()
        }
        return path.absolutePath + "/" + fileName
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
    fun saveToKeystore(context: Context, content: String, alias: String) {
        val keyGenerator = KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, Constants.ANDROID_KEY_STORE)

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        val secretKey = keyGenerator.generateKey()

        val cipher = Cipher.getInstance(Constants.TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        saveToFile(context, Constants.ENCRYPTION_IV + alias, iv)

        val encryption = cipher.doFinal(content.toByteArray(StandardCharsets.UTF_8))
        saveToFile(context, Constants.ENCRYPTION_DATA + alias, encryption)
    }

    @Throws(Exception::class)
    fun readFromKeystore(context: Context, alias: String): String {

        val encryptionIv = readFromFile(context, Constants.ENCRYPTION_IV + alias)
        val encryptedData = readFromFile(context, Constants.ENCRYPTION_DATA + alias)

        val keyStore = KeyStore.getInstance(Constants.ANDROID_KEY_STORE)
        keyStore.load(null)

        val secretKeyEntry = keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry
        val secretKey = secretKeyEntry.secretKey
        val cipher = Cipher.getInstance(Constants.TRANSFORMATION)
        val spec = GCMParameterSpec(128, encryptionIv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decodedData = cipher.doFinal(encryptedData)
        return String(decodedData, StandardCharsets.UTF_8)
    }

    fun isFingerprintEnrolled(context: Context): Boolean {
        val fingerprintManager = FingerprintManagerCompat.from(context)
        return fingerprintManager.hasEnrolledFingerprints()
    }

    fun displayBiometricPrompt(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        callback: BiometricPrompt.AuthenticationCallback
    ): Boolean {
        if (isFingerprintEnrolled(activity)) {
            val executor = Executors.newSingleThreadExecutor()
            val biometricPrompt = BiometricPrompt(activity, executor, callback)

            biometricPrompt.authenticate(promptInfo)
            return true
        }

        return false
    }

    fun getWalletAlias(walletID: Long) = walletID.toString() + Constants.SPENDING_PASSPHRASE
}