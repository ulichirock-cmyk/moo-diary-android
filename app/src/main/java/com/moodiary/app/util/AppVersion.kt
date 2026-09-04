package com.moodiary.app.util

import android.content.Context
import android.content.pm.PackageManager

/** The installed build's versionName, e.g. "0.1.0". */
fun Context.appVersionName(): String = runCatching {
    packageManager.getPackageInfo(packageName, 0).versionName
}.getOrNull() ?: "—"
