package com.example.sms_app.utils

import android.content.Context

fun Context.getAppVersion(): String = runCatching {
    packageManager.getPackageInfo(packageName, 0)?.versionName ?: "N/A"
}.getOrDefault("N/A")