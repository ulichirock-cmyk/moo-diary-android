package com.moodiary.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moodiary.app.R
import com.moodiary.app.data.PlaceSuggestion
import com.moodiary.app.ui.map.GeoPoint
import com.moodiary.app.ui.map.MapCameraState
import com.moodiary.app.ui.map.TileMap
import com.moodiary.app.ui.map.rememberMapCamera
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.ui.theme.MoodiaryType
import com.moodiary.app.util.LOCATION_PERMISSIONS
import com.moodiary.app.util.deviceLocation
import com.moodiary.app.util.hasLocationPermission
import kotlinx.coroutines.launch

/**
 * 11 地图选点 — drag the map, confirm the name at the bottom.
 *
 * The map is real: [TileMap] draws OpenStreetMap raster tiles, the pin stays pinned to
 * the centre of the viewport as the design draws it, and [onCenterSettled] hands the
 * coordinate back to be named once the camera stops. See [TileMap] for why there is no
 * map SDK behind it.
 *
 * Location is optional. Without the permission the map simply opens on a default
 * position and the hint line says so — the screen still works, you just have to find
 * the spot yourself.
 */
@Composable
fun MapPickerScreen(
    candidates: List<PlaceSuggestion>,
    selected: String?,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onConfirm: () -> Unit,
    onCenterSettled: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val camera = rememberMapCamera(DEFAULT_CENTER)
    var locating by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    suspend fun centreOnDevice() {
        locating = true
        val location = context.deviceLocation()
        locating = false
        if (location != null) {
            // Moving the camera restarts the settle debounce, which names the new spot.
            camera.moveTo(GeoPoint(location.latitude, location.longitude), maxOf(camera.zoom, 16f))
        } else {
            // Nothing moved, and the settle that would have named it was suppressed.
            onCenterSettled(camera.center.lat, camera.center.lng)
        }
    }

    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.values.any { it }) {
            permissionDenied = false
            scope.launch { centreOnDevice() }
        } else {
            permissionDenied = true
        }
    }

    // Opening on the user's own position is the useful default, but only when that
    // costs nothing — a permission dialog on arrival would be rude.
    LaunchedEffect(Unit) {
        if (context.hasLocationPermission()) centreOnDevice()
    }

    fun recenter() {
        if (context.hasLocationPermission()) scope.launch { centreOnDevice() }
        else requestPermission.launch(LOCATION_PERMISSIONS)
    }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painterResource(R.drawable.ic_chevron_left),
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MoodiaryColors.TextPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                stringResource(R.string.map_title),
                style = MoodiaryType.TitleSmall,
                color = MoodiaryColors.TextPrimary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.width(48.dp))
        }

        MapSurface(
            camera = camera,
            // Naming the default centre while a fix is still arriving would put the
            // wrong place under 用这个地点 for the second before the map jumps.
            onCenterSettled = { if (!locating) onCenterSettled(it.lat, it.lng) },
            onRecenter = ::recenter,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MoodiaryColors.Surface)
                .padding(20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    selected
                        ?: candidates.firstOrNull()?.name
                        ?: stringResource(R.string.map_resolving),
                    style = MoodiaryType.PlaceTitle,
                    color = MoodiaryColors.TextPrimary,
                )
                Text(
                    stringResource(
                        when {
                            locating -> R.string.map_locating
                            permissionDenied -> R.string.map_no_location
                            else -> R.string.map_hint
                        },
                    ),
                    style = MoodiaryType.Meta,
                    color = MoodiaryColors.TextMuted,
                )
            }

            val shape = RoundedCornerShape(10.dp)
            if (candidates.isNotEmpty()) Column(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(MoodiaryColors.Background)
                    .border(1.dp, MoodiaryColors.Border, shape),
            ) {
                candidates.forEachIndexed { index, candidate ->
                    CandidateRow(
                        candidate = candidate,
                        selected = candidate.name == selected,
                        onClick = { onSelect(candidate.name) },
                    )
                    if (index != candidates.lastIndex) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(MoodiaryColors.Border))
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(MoodiaryColors.Accent)
                    .clickable(onClick = onConfirm)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.map_use_place),
                    style = MoodiaryType.SheetRowStrong,
                    color = Color.White,
                )
            }
        }
    }
}

/**
 * The map itself plus the three things the design draws on top of it: the fixed centre
 * pin, the 回到当前位置 button, and — in the slot the design reserved for map chrome —
 * the attribution the tile licence requires.
 */
@Composable
private fun MapSurface(
    camera: MapCameraState,
    onCenterSettled: (GeoPoint) -> Unit,
    onRecenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TileMap(
        camera = camera,
        background = MoodiaryColors.Canvas,
        onCameraSettled = onCenterSettled,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MoodiaryColors.Surface.copy(alpha = 0.82f))
                .padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_map),
                contentDescription = null,
                tint = MoodiaryColors.TextMuted,
                modifier = Modifier.size(11.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                stringResource(R.string.map_attribution),
                style = MoodiaryType.Detail,
                color = MoodiaryColors.TextMuted,
            )
        }

        // Pin plus its ground shadow, centred on the map like the design draws it.
        // It does not move: the map slides underneath it.
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painterResource(R.drawable.ic_pin_filled),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(34.dp),
            )
            Box(
                Modifier
                    .offset(y = (-2).dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(MoodiaryColors.TextPrimary.copy(alpha = 0.25f)),
            )
        }

        val buttonShape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp)
                .size(40.dp)
                .clip(buttonShape)
                .background(MoodiaryColors.Surface)
                .border(1.dp, MoodiaryColors.Border, buttonShape)
                .clickable(onClick = onRecenter),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_crosshair),
                contentDescription = stringResource(R.string.map_recenter),
                tint = MoodiaryColors.Accent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun CandidateRow(candidate: PlaceSuggestion, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(R.drawable.ic_pin),
            contentDescription = null,
            tint = if (selected) MoodiaryColors.Accent else MoodiaryColors.TextMuted,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            candidate.name,
            style = MoodiaryType.ListItemSmall,
            color = if (selected) MoodiaryColors.TextPrimary else MoodiaryColors.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MoodiaryColors.Accent,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

/**
 * Where the map opens when the device will not say where it is. 朝阳公园 — the place the
 * sample entries keep going back to, so the screen looks like the design out of the box.
 */
private val DEFAULT_CENTER = GeoPoint(lat = 39.9450, lng = 116.4750)
