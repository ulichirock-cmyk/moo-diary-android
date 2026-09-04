package com.moodiary.app.ui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moodiary.app.R
import com.moodiary.app.data.PlaceSuggestion
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.ui.theme.MoodiaryType

/**
 * 11 地图选点 — drag the map, confirm the name at the bottom.
 *
 * The map surface is a placeholder. The design itself labels this area
 * "系统地图接管渲染", and a real one needs a provider SDK plus an API key (高德 /
 * 百度 on this market) that the project does not have. Everything around it — the pin,
 * the recenter button, the candidate list, the confirm button — is real, so dropping a
 * MapView into [MapSurface] is the only change needed.
 */
@Composable
fun MapPickerScreen(
    candidates: List<PlaceSuggestion>,
    selected: String?,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
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

        MapSurface(Modifier.weight(1f).fillMaxWidth())

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
                    selected ?: candidates.firstOrNull()?.name.orEmpty(),
                    style = MoodiaryType.PlaceTitle,
                    color = MoodiaryColors.TextPrimary,
                )
                Text(
                    stringResource(R.string.map_hint),
                    style = MoodiaryType.Meta,
                    color = MoodiaryColors.TextMuted,
                )
            }

            val shape = RoundedCornerShape(10.dp)
            Column(
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

/** Stand-in for the platform map view. See the class doc for why. */
@Composable
private fun MapSurface(modifier: Modifier = Modifier) {
    Box(modifier.background(MoodiaryColors.Canvas)) {
        Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_map),
                contentDescription = null,
                tint = MoodiaryColors.TextMuted,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                stringResource(R.string.map_placeholder),
                style = MoodiaryType.Detail,
                color = MoodiaryColors.TextMuted,
            )
        }

        // Pin plus its ground shadow, centred on the map like the design draws it.
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
                .border(1.dp, MoodiaryColors.Border, buttonShape),
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
