package com.example.sms_app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat

data class SimInfo(
    val subscriptionId: Int = 0,
    val simSlotIndex: Int = 0,
    val displayName: String = "",
    val carrierName: String = "",
    val phoneNumber: String? = null
)

object SimManager {
    
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    fun getAvailableSims(context: Context): List<SimInfo> {
        val sims = mutableListOf<SimInfo>()
        
        if (!hasRequiredPermissions(context)) {
            return sims
        }
        
        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

            val subscriptions = subscriptionManager.activeSubscriptionInfoList ?: return sims

            for (subscription in subscriptions) {
                val simInfo = SimInfo(
                    subscriptionId = subscription.subscriptionId,
                    simSlotIndex = subscription.simSlotIndex,
                    displayName = subscription.displayName?.toString() ?: "SIM ${subscription.simSlotIndex + 1}",
                    carrierName = subscription.carrierName?.toString() ?: "Unknown",
                    phoneNumber = subscription.number
                )
                sims.add(simInfo)
            }
        } catch (e: Exception) {
            // Add default SIM if detection fails
            val defaultSim = SimInfo(
                subscriptionId = -1,
                simSlotIndex = 0,
                displayName = "Default SIM",
                carrierName = "Unknown",
                phoneNumber = null
            )
            sims.add(defaultSim)
        }
        
        return sims
    }
    
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    fun hasMultipleSims(context: Context): Boolean {
        return getAvailableSims(context).size > 1
    }
    
    private fun hasRequiredPermissions(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    }
    
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    fun getSimDisplayName(context: Context, subscriptionId: Int): String {
        val sims = getAvailableSims(context)
        return sims.find { it.subscriptionId == subscriptionId }?.displayName ?: "SIM $subscriptionId"
    }
} 