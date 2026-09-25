package com.example.data.preferences

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureApiStore(context: Context) {
    private val prefs = context.getSharedPreferences("au_notes_secure_api", Context.MODE_PRIVATE)
    private val alias = "au_notes_api_key_v1"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        generator.init(android.security.keystore.KeyGenParameterSpec.Builder(
            alias,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .build())
        return generator.generateKey()
    }

    fun put(name: String, value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val packed = Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
        prefs.edit().putString(name, packed).apply()
    }

    fun get(name: String): String {
        val packed = prefs.getString(name, null) ?: return ""
        return try {
            val bytes = Base64.decode(packed, Base64.NO_WRAP)
            val iv = bytes.copyOfRange(0, 12)
            val encrypted = bytes.copyOfRange(12, bytes.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
        } catch (_: Exception) { "" }
    }

    fun remove(name: String) { prefs.edit().remove(name).apply() }
}
