package com.moodiary.app.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
)

/** Coarse is enough to centre a map; fine is welcome but never required. */
fun Context.hasLocationPermission(): Boolean = LOCATION_PERMISSIONS.any {
    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
}

/**
 * Where the device is, or null.
 *
 * Deliberately built on the platform [LocationManager] rather than
 * `play-services-location`: the phones this app is aimed at often ship without Google
 * Play services, and a fused-provider dependency would turn "centre the map" into a
 * hard failure on exactly those devices.
 *
 * A recent last-known fix is returned immediately — asking a cold GPS for a fresh one
 * costs tens of seconds and the map only needs to know which city you are in.
 */
@SuppressLint("MissingPermission")
suspend fun Context.deviceLocation(): Location? {
    if (!hasLocationPermission()) return null
    val manager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = runCatching { manager.getProviders(true) }.getOrNull().orEmpty()

    val known = providers
        .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
    if (known != null && System.currentTimeMillis() - known.time < FRESH_ENOUGH_MS) return known

    val provider = providers.firstOrNull { it == LocationManager.NETWORK_PROVIDER }
        ?: providers.firstOrNull { it == LocationManager.GPS_PROVIDER }
        ?: return known
    return withTimeoutOrNull(TIMEOUT_MS) { manager.singleUpdate(provider) } ?: known
}

@SuppressLint("MissingPermission")
private suspend fun LocationManager.singleUpdate(provider: String): Location? =
    suspendCancellableCoroutine { continuation ->
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                removeUpdates(this)
                if (continuation.isActive) continuation.resume(location)
            }

            // The three-argument overloads are abstract below API 30.
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }
        continuation.invokeOnCancellation { removeUpdates(listener) }
        runCatching {
            requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        }.onFailure {
            if (continuation.isActive) continuation.resume(null)
        }
    }

private const val FRESH_ENOUGH_MS = 10 * 60 * 1000L
private const val TIMEOUT_MS = 8_000L
