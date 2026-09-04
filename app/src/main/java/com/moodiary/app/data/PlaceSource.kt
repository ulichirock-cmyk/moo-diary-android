package com.moodiary.app.data

/** One candidate on 地点选择 / 地图选点. [detail] is the distance + address line. */
data class PlaceSuggestion(
    val name: String,
    val detail: String? = null,
)

/**
 * Where the "附近" list comes from.
 *
 * The design says these are derived from the device's location ("附近地点由定位得出"),
 * which needs a location permission plus a POI provider — on this market that means
 * 高德 / 百度 with an API key, neither of which this project has. [StubPlaceSource]
 * therefore returns a fixed list matching the design so the screens are complete and
 * navigable; swap in a real implementation and nothing above this interface changes.
 */
interface PlaceSource {
    suspend fun nearby(): List<PlaceSuggestion>

    /** Candidates for the pin's current position on the map screen. */
    suspend fun atPin(): List<PlaceSuggestion>
}

object StubPlaceSource : PlaceSource {
    override suspend fun nearby() = listOf(
        PlaceSuggestion("朝阳公园", "120 米 · 朝阳区农展馆南路"),
        PlaceSuggestion("蓝色港湾", "450 米"),
        PlaceSuggestion("团结湖地铁站", "1.1 公里"),
    )

    override suspend fun atPin() = listOf(
        PlaceSuggestion("朝阳公园"),
        PlaceSuggestion("公园西门"),
        PlaceSuggestion("湖边草坪"),
    )
}
