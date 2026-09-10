package com.malik.ztesmartmanager.core.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class SavedRouterCredential(
    val routerAddress: String,
    val password: String
)

/**
 * Stores the router administration password encrypted with an Android Keystore key.
 * The password is never written to SharedPreferences as plaintext.
 *
 * save() is called from Dispatchers.IO by the login UI. The successful write is committed before
 * returning true. clear() intentionally uses apply() because it may be triggered directly by a UI
 * checkbox and removing an already-encrypted credential does not need to block the main thread.
 */
class SecureRouterCredentialStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun load(): SavedRouterCredential? = runCatching {
        val address = preferences.getString(KEY_ADDRESS, null)?.trim().orEmpty()
        val encrypted = preferences.getString(KEY_PASSWORD, null).orEmpty()
        val iv = preferences.getString(KEY_IV, null).orEmpty()
        if (address.isBlank() || encrypted.isBlank() || iv.isBlank()) return null

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.decode(iv, Base64.NO_WRAP))
        )
        val plaintext = cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP))
        val password = String(plaintext, StandardCharsets.UTF_8)
        if (password.isBlank()) {
            null
        } else {
            lastSavedFingerprint = fingerprint(address, password)
            SavedRouterCredential(address, password)
        }
    }.getOrElse {
        // A replaced/invalidated device key must never leave unusable credential material behind.
        clear()
        null
    }

    fun save(routerAddress: String, password: String): Boolean = runCatching {
        val normalizedAddress = routerAddress.trim()
        require(normalizedAddress.isNotBlank())
        require(password.isNotBlank())

        val fingerprint = fingerprint(normalizedAddress, password)
        if (
            lastSavedFingerprint == fingerprint &&
            preferences.getString(KEY_ADDRESS, null)?.trim() == normalizedAddress &&
            !preferences.getString(KEY_PASSWORD, null).isNullOrBlank() &&
            !preferences.getString(KEY_IV, null).isNullOrBlank()
        ) {
            return true
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(password.toByteArray(StandardCharsets.UTF_8))

        val committed = preferences.edit()
            .putString(KEY_ADDRESS, normalizedAddress)
            .putString(KEY_PASSWORD, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .commit()
        if (committed) lastSavedFingerprint = fingerprint
        committed
    }.getOrDefault(false)

    fun clear() {
        lastSavedFingerprint = null
        preferences.edit()
            .remove(KEY_ADDRESS)
            .remove(KEY_PASSWORD)
            .remove(KEY_IV)
            .apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private fun fingerprint(address: String, password: String): Int = 31 * address.hashCode() + password.hashCode()

    private companion object {
        @Volatile
        var lastSavedFingerprint: Int? = null

        const val PREFERENCES = "zte_secure_router_credentials"
        const val KEY_ALIAS = "zte_smart_hai_router_password_v1"
        const val KEY_ADDRESS = "router_address"
        const val KEY_PASSWORD = "password_ciphertext"
        const val KEY_IV = "password_iv"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
    }
}
