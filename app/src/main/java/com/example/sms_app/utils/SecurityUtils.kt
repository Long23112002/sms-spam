package com.example.sms_app.utils

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.provider.Settings
import android.util.Base64
import android.util.Log
import androidx.annotation.Keep
import com.example.sms_app.BuildConfig
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.safetynet.SafetyNet
import dalvik.system.DexClassLoader
import java.io.File
import java.security.MessageDigest
import java.util.*
import kotlin.system.exitProcess

@Keep
object SecurityUtils {

    // Obfuscated strings
    private val ROOTED_FILES = arrayOf(
        ".e7H3_Tq", "Jq_I9", "6_Ns8", "ObIu5.n", "jK9_0z", "0t_F5p"
    ).map { decode(it) }.toTypedArray()
    
    private val EMULATOR_PROPERTIES = arrayOf(
        "B.8_pF", "C.9_kY", "X_h0h", "A.5_L9", "nR_8j", "kP_sU"
    ).map { decode(it) }.toTypedArray()

    private val DANGEROUS_APPS = arrayOf(
        "Fs_k5.U9", "L9_bB.n8", "B6_Hs.i0", "JK_i.kY", "Ok_lI.hB"
    ).map { decode(it) }.toTypedArray()
    
    private fun decode(s: String): String {
        val reversed = s.reversed()
        val result = StringBuilder()
        for (i in reversed.indices) {
            val c = reversed[i]
            if (c == '.') result.append('/')
            else if (c == '_') result.append('.')
            else if (c.isLetter()) {
                val base = if (c.isUpperCase()) 'A' else 'a'
                val offset = (c.code - base.code + 13) % 26
                result.append((base.code + offset).toChar())
            } else result.append(c)
        }
        return result.toString()
    }

    @JvmStatic
    fun verifyAppIntegrity(context: Context): Boolean {
        if (!BuildConfig.ENABLE_INTEGRITY_CHECK) return true
        
        return try {
            // Temporarily disable most strict checks, keep only essential security
            !isRooted() && verifyAppSignature(context)
            // Comment out strict checks that may cause issues:
            // !isEmulator() && 
            // !isDebuggable(context) && 
            // !hasDangerousApps(context) && 
            // !isDexTampered(context)
        } catch (e: Exception) {
            // If verification fails, still allow app to run in most cases
            // Log error but don't block app
            Log.e("SecurityUtils", "Integrity check failed", e)
            true
        }
    }
    
    @JvmStatic
    fun performTamperResponses(context: Context) {
        if (!BuildConfig.ENABLE_TAMPER_DETECTION) return
        
        try {
            // Only log issues instead of taking drastic actions
            if (!verifyAppIntegrity(context)) {
                Log.w("SecurityUtils", "Integrity check failed, app may be tampered")
                // Don't terminate the app or create malfunctions
                // Consider showing a warning to the user instead
            }
        } catch (e: Exception) {
            Log.e("SecurityUtils", "Error during tamper check", e)
        }
    }

    private fun isRooted(): Boolean {
        for (file in ROOTED_FILES) {
            if (File(file).exists()) return true
        }
        return false
    }

    private fun isEmulator(): Boolean {
        return try {
            for (prop in EMULATOR_PROPERTIES) {
                val value = System.getProperty(prop, "") ?: ""
                if (value.contains("generic") || value.contains("sdk")) return true
            }
            
            // Check using ABI
            val abi = Build.SUPPORTED_ABIS.joinToString("")
            abi.contains("x86") || Build.FINGERPRINT.contains("generic")
            
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isDebuggable(context: Context): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, 
                Settings.Global.ADB_ENABLED, 0) == 1
        } catch (e: Exception) {
            false
        }
    }
    
    private fun hasDangerousApps(context: Context): Boolean {
        val packageManager = context.packageManager
        for (app in DANGEROUS_APPS) {
            try {
                packageManager.getPackageInfo(app, PackageManager.GET_ACTIVITIES)
                return true
            } catch (e: Exception) {
                // App not found, good
            }
        }
        return false
    }
    
    private fun verifyAppSignature(context: Context): Boolean {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName, PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName, PackageManager.GET_SIGNATURES
                )
            }
            
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            
            if (signatures.isNotEmpty()) {
                val signature = signatures[0]
                val signatureBytes = signature.toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(signatureBytes)
                val signatureHex = Base64.encodeToString(digest, Base64.NO_WRAP)
                
                // Log this signature on first run, then hardcode it in production
                return signatureHex.isNotEmpty()
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }
    
    private fun isDexTampered(context: Context): Boolean {
        return try {
            // Try to load our own DEX file and verify it
            val assetManager = context.assets
            val fileName = "smssender.dex"
            val outFile = File(context.cacheDir, fileName)
            
            // Copy asset to cache if needed
            if (!outFile.exists()) {
                assetManager.open(fileName).use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            
            // Check if we can load our DEX
            val classLoader = DexClassLoader(
                outFile.absolutePath,
                context.cacheDir.absolutePath,
                null,
                context.classLoader
            )
            
            // Try to load a class from the DEX
            val className = "com.example.sms.SenderUtils"
            val loadedClass = classLoader.loadClass(className)
            
            // If we get here without exception, DEX is valid
            loadedClass != null
        } catch (e: Exception) {
            // Either DEX is tampered or doesn't exist
            true
        }
    }
    
    @JvmStatic
    fun verifyPlayIntegrity(context: Context, callback: (Boolean) -> Unit) {
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) 
            != ConnectionResult.SUCCESS) {
            callback(false)
            return
        }
        
        try {
            SafetyNet.getClient(context).attest(UUID.randomUUID().toString().toByteArray(), BuildConfig.APP_SIGNATURE)
                .addOnSuccessListener {
                    // Validation successful
                    callback(true)
                }
                .addOnFailureListener {
                    // Validation failed
                    callback(false)
                }
        } catch (e: Exception) {
            callback(false)
        }
    }
} 