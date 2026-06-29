package com.example.securetalk

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.nio.charset.StandardCharsets

object EncryptionManager {
    private const val KEYSET_NAME = "master_keyset"
    private const val PREFERENCE_FILE = "master_key_preference"
    private const val MASTER_KEY_URI = "android-keystore://master_key"

    private lateinit var aead: Aead

    fun init(context: Context) {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREFERENCE_FILE)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        
        aead = keysetHandle.getPrimitive(Aead::class.java)
    }

    fun encrypt(text: String): String {
        return try {
            val ciphertext = aead.encrypt(text.toByteArray(StandardCharsets.UTF_8), null)
            Base64.encodeToString(ciphertext, Base64.DEFAULT)
        } catch (e: Exception) {
            text // Fallback to plain text if encryption fails
        }
    }

    fun decrypt(encryptedText: String): String {
        return try {
            val ciphertext = Base64.decode(encryptedText, Base64.DEFAULT)
            val decrypted = aead.decrypt(ciphertext, null)
            String(decrypted, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            encryptedText // Return original if decryption fails (e.g., if it wasn't encrypted)
        }
    }
}
