package com.moodiary.app.data

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** One candidate on 地点选择 / 地图选点. [detail] is the distance + address line. */
data class PlaceSuggestion(
    val name: String,
    val detail: String? = null,
)

/**
 * Where place names come from.
 *
 * The two halves have different footing, and the split is deliberate:
 *
 * - [atPin] is real. Turning a coordinate into a name is reverse geocoding, which the
 *   platform does itself through [Geocoder] — no key, no SDK.
 * - [nearby] is still a stub. The design's 附近 list is POI search with distances
 *   ("120 米 · 朝阳区农展馆南路"), and no amount of platform API produces that; it needs
 *   a 高德 / 百度 account. Swap in a real implementation and nothing above this
 *   interface changes.
 */
interface PlaceSource {
    suspend fun nearby(): List<PlaceSuggestion>

    /** Candidates for the pin's current position on the map screen. */
    suspend fun atPin(lat: Double, lng: Double): List<PlaceSuggestion>
}

object StubPlaceSource : PlaceSource {
    override suspend fun nearby() = listOf(
        PlaceSuggestion("朝阳公园", "120 米 · 朝阳区农展馆南路"),
        PlaceSuggestion("蓝色港湾", "450 米"),
        PlaceSuggestion("团结湖地铁站", "1.1 公里"),
    )

    override suspend fun atPin(lat: Double, lng: Double) = listOf(
        PlaceSuggestion("朝阳公园"),
        PlaceSuggestion("公园西门"),
        PlaceSuggestion("湖边草坪"),
    )
}

/**
 * Names the pin using the device's own geocoder.
 *
 * There is no guarantee one exists — [Geocoder.isPresent] is false on a build with no
 * geocoder backend, which is common on phones shipped without Google Play services —
 * so a failure falls back to the coordinates themselves. That is a worse label but a
 * true one, which beats showing a place the user is not standing in.
 */
class GeocoderPlaceSource(context: Context) : PlaceSource {

    private val context = context.applicationContext

    override suspend fun nearby(): List<PlaceSuggestion> = StubPlaceSource.nearby()

    override suspend fun atPin(lat: Double, lng: Double): List<PlaceSuggestion> =
        withContext(Dispatchers.IO) {
            val addresses = if (Geocoder.isPresent()) {
                @Suppress("DEPRECATION")
                runCatching { Geocoder(context, Locale.getDefault()).getFromLocation(lat, lng, 3) }
                    .getOrNull()
                    .orEmpty()
            } else {
                emptyList()
            }

            // One name per address: the geocoder already returns the same coordinate at
            // several granularities (the POI, the park it sits in, the street), which is
            // exactly the choice this screen is asking the user to make.
            val names = addresses
                .mapNotNull { it.featureName ?: it.thoroughfare ?: it.subLocality ?: it.locality }
                .plus(addresses.firstOrNull()?.let { listOfNotNull(it.thoroughfare, it.subLocality) }.orEmpty())
                .map { it.trim().undoubled() }
                .filter { it.isNotEmpty() && it.any { char -> !char.isDigit() } }
                .distinct()
                .take(3)

            names.ifEmpty { listOf(formatCoordinates(lat, lng)) }.map { PlaceSuggestion(it) }
        }
}

/**
 * Chinese geocoders like to hand back a POI as parent + child glued together —
 * "朝阳公园朝阳公园-中心岛" is one string, not two. When a name literally begins with
 * itself, the first copy goes.
 *
 * The two-character floor and the "remainder must be longer" test are what keep
 * genuinely repetitive names (泡泡玛特, 人人网) intact.
 */
private fun String.undoubled(): String {
    for (length in 2..this.length / 2) {
        val rest = substring(length)
        if (rest.length > length && rest.startsWith(take(length))) return rest
    }
    return this
}

/** "39.9450, 116.4750" — about ten metres of precision, which is all a pin needs. */
fun formatCoordinates(lat: Double, lng: Double): String =
    String.format(Locale.US, "%.4f, %.4f", lat, lng)
