package com.moodiary.app.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

/** Remembers a camera across configuration changes so a rotation keeps the position. */
@Composable
fun rememberMapCamera(center: GeoPoint, zoom: Float = 15f): MapCameraState =
    rememberSaveable(saver = MapCameraSaver) { MapCameraState(center, zoom) }

private val MapCameraSaver = listSaver<MapCameraState, Float>(
    save = { listOf(it.center.lat.toFloat(), it.center.lng.toFloat(), it.zoom) },
    restore = { MapCameraState(GeoPoint(it[0].toDouble(), it[1].toDouble()), it[2]) },
)

/**
 * A slippy map: raster tiles laid out in Web Mercator, dragged and pinched directly.
 *
 * There is no map SDK behind this and no API key. Google Play services are not a
 * given on this market's devices, and 高德 / 百度 both want a registered key plus a
 * coordinate conversion (their tiles are GCJ-02, so raw GPS would land the pin a few
 * hundred metres off in Beijing). OpenStreetMap tiles are WGS-84, which is what the
 * platform's location provider hands us — the two agree with no fudging.
 *
 * Callers must show the attribution the licence requires; [MapAttribution] is next to
 * the map on 地图选点.
 */
@Composable
fun TileMap(
    camera: MapCameraState,
    modifier: Modifier = Modifier,
    background: Color = Color(0xFFEDE7DC),
    onCameraSettled: (GeoPoint) -> Unit = {},
    content: @Composable BoxScope.() -> Unit = {},
) {
    val context = LocalContext.current
    val cache = remember(context) { TileCache(context.applicationContext) }
    val tiles = remember { mutableStateMapOf<TileKey, ImageBitmap>() }

    // Tiles are authored for 256 CSS pixels. Drawn 1:1 on a 3x screen the labels are
    // unreadable; drawn at 256dp they are a blurry mess. Somewhere near 1.7x is the
    // compromise every hand-rolled map lands on.
    val density = LocalDensity.current.density
    val baseTilePx = TILE_PX * (density / 1.6f).coerceIn(1f, 2f)

    var viewport by remember { mutableStateOf(IntSize.Zero) }

    val z = floor(camera.zoom).toInt()
    val n = 1 shl z
    val tilePx = baseTilePx * 2f.pow(camera.zoom - z)
    val halfW = viewport.width / 2f
    val halfH = viewport.height / 2f
    val cx = camera.world.x * n
    val cy = camera.world.y * n

    val x0 = floor(cx - halfW / tilePx).toInt()
    val x1 = floor(cx + halfW / tilePx).toInt()
    val y0 = floor(cy - halfH / tilePx).toInt().coerceAtLeast(0)
    val y1 = floor(cy + halfH / tilePx).toInt().coerceAtMost(n - 1)

    // Keyed on the tile range, not the camera, so a drag inside one tile fetches nothing.
    LaunchedEffect(z, x0, x1, y0, y1) {
        if (viewport == IntSize.Zero) return@LaunchedEffect
        // Panning at one zoom would otherwise grow this without bound — every tile
        // scrolled past stays referenced. Over the cap, keep what is on screen plus the
        // ancestors drawTile falls back to; anything dropped that is still wanted comes
        // straight back through the loads below.
        if (tiles.size > 200) {
            val keep = HashSet<TileKey>()
            for (x in x0..x1) {
                for (y in y0..y1) {
                    var key = TileKey(z, x.wrap(n), y)
                    repeat(4) {
                        keep += key
                        key = TileKey(key.z - 1, key.x / 2, key.y / 2)
                    }
                }
            }
            tiles.keys.retainAll(keep)
        }
        for (x in x0..x1) {
            for (y in y0..y1) {
                val key = TileKey(z, x.wrap(n), y)
                if (tiles.containsKey(key)) continue
                launch { cache.load(key)?.let { tiles[key] = it } }
            }
        }
    }

    val settled by rememberUpdatedState(onCameraSettled)
    LaunchedEffect(camera.world, camera.zoom) {
        delay(400)
        settled(camera.center)
    }

    Box(
        modifier
            .background(background)
            // The tile grid always overshoots the viewport, and a Canvas is free to
            // paint outside its own bounds — without this the map covers the header.
            .clipToBounds()
            .onSizeChanged { viewport = it }
            // Both blocks read `size` off the pointer scope rather than closing over the
            // measured viewport: `pointerInput(Unit)` is set up once, when that is still
            // zero, and a zoom anchored on (0, 0) drifts away from the fingers.
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        camera.zoomBy(
                            factor = 2f,
                            focus = MapCameraState.Offsets(
                                offset.x, offset.y, size.width / 2f, size.height / 2f,
                            ),
                            baseTilePx = baseTilePx,
                        )
                    },
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, gestureZoom, _ ->
                    camera.pan(pan.x, pan.y, baseTilePx)
                    if (gestureZoom != 1f) {
                        camera.zoomBy(
                            factor = gestureZoom,
                            focus = MapCameraState.Offsets(
                                centroid.x, centroid.y, size.width / 2f, size.height / 2f,
                            ),
                            baseTilePx = baseTilePx,
                        )
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            for (x in x0..x1) {
                for (y in y0..y1) {
                    // Edges are rounded from the same continuous position function, so
                    // neighbouring tiles share a boundary pixel instead of leaving a seam.
                    val left = (halfW + (x - cx) * tilePx).roundToInt()
                    val top = (halfH + (y - cy) * tilePx).roundToInt()
                    val right = (halfW + (x + 1 - cx) * tilePx).roundToInt()
                    val bottom = (halfH + (y + 1 - cy) * tilePx).roundToInt()
                    drawTile(
                        tiles = tiles,
                        key = TileKey(z, x.wrap(n), y),
                        dstOffset = IntOffset(left, top),
                        dstSize = IntSize(right - left, bottom - top),
                    )
                }
            }
        }
        content()
    }
}

/**
 * Draws one tile, falling back to the matching quadrant of an already-loaded parent
 * when the exact tile has not arrived yet. That is what keeps a zoom from flashing
 * empty canvas before the new level downloads.
 */
private fun DrawScope.drawTile(
    tiles: Map<TileKey, ImageBitmap>,
    key: TileKey,
    dstOffset: IntOffset,
    dstSize: IntSize,
) {
    var level = key.z
    var x = key.x
    var y = key.y
    var fx = 0f
    var fy = 0f
    var span = 1f
    while (level >= key.z - 3 && level >= 0) {
        val bitmap = tiles[TileKey(level, x, y)]
        if (bitmap != null) {
            drawImage(
                image = bitmap,
                srcOffset = IntOffset((fx * bitmap.width).roundToInt(), (fy * bitmap.height).roundToInt()),
                srcSize = IntSize(
                    (span * bitmap.width).roundToInt().coerceAtLeast(1),
                    (span * bitmap.height).roundToInt().coerceAtLeast(1),
                ),
                dstOffset = dstOffset,
                dstSize = dstSize,
                filterQuality = FilterQuality.Low,
            )
            return
        }
        fx = (x % 2 + fx) / 2f
        fy = (y % 2 + fy) / 2f
        span /= 2f
        x /= 2
        y /= 2
        level--
    }
}

/** Columns wrap around the antimeridian; [n] is the tile count at this zoom. */
private fun Int.wrap(n: Int) = ((this % n) + n) % n
