package com.example.sms_app.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.Keep
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Utility for encrypting sensitive data using Android KeyStore
 */
@Keep
object CryptoUtils {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SEPARATOR = "]"

    @JvmStatic
    fun encrypt(text: String, keyAlias: String): String {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            // Create a new key if needed
            if (!keyStore.containsAlias(keyAlias)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
                )
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }

            // Get key and initialize cipher for encryption
            val key = keyStore.getKey(keyAlias, null) as SecretKey
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)

            // Perform encryption
            val encryptedBytes = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
            
            // Prepend the IV to the encrypted data
            val iv = cipher.iv
            val ivAndEncryptedBytes = iv + IV_SEPARATOR.toByteArray() + encryptedBytes
            
            // Base64 encode the result
            return Base64.encodeToString(ivAndEncryptedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            // Fallback to basic encoding if encryption fails
            return Base64.encodeToString(scramble(text).toByteArray(), Base64.DEFAULT)
        }
    }

    @JvmStatic
    fun decrypt(encryptedText: String, keyAlias: String): String {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            
            // Check if key exists
            if (!keyStore.containsAlias(keyAlias)) {
                // Key doesn't exist, can't decrypt
                return ""
            }

            // Decode the Base64 text
            val encryptedData = Base64.decode(encryptedText, Base64.DEFAULT)
            
            // Split IV and encrypted data
            val ivEndIndex = indexOf(encryptedData, IV_SEPARATOR.toByteArray())
            if (ivEndIndex == -1) {
                // Malformed data
                return ""
            }
            
            val iv = encryptedData.copyOfRange(0, ivEndIndex)
            val encrypted = encryptedData.copyOfRange(
                ivEndIndex + IV_SEPARATOR.length, encryptedData.size
            )

            // Get key and initialize cipher for decryption
            val key = keyStore.getKey(keyAlias, null) as SecretKey
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            // Perform decryption
            val decryptedBytes = cipher.doFinal(encrypted)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // Fallback to basic decoding if decryption fails
            try {
                val decoded = Base64.decode(encryptedText, Base64.DEFAULT)
                return unscramble(String(decoded))
            } catch (e2: Exception) {
                return "" // Return empty on failure
            }
        }
    }
    
    // Find byte array inside another byte array
    private fun indexOf(source: ByteArray, target: ByteArray): Int {
        outer@ for (i in 0..source.size - target.size) {
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    continue@outer
                }
            }
            return i
        }
        return -1
    }
    
    // Simple fallback scrambling algorithm as last resort
    private fun scramble(text: String): String {
        val key = "S3cur3K3y!#%"
        val result = StringBuilder()
        for (i in text.indices) {
            val c = text[i]
            val k = key[i % key.length]
            result.append((c.code xor k.code).toChar())
        }
        return result.toString()
    }
    
    private fun unscramble(text: String): String {
        // Identical to scramble since XOR is reversible with the same key
        return scramble(text)
    }
} 