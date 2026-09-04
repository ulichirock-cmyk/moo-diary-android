package com.moodiary.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.moodiary.app.R
import com.moodiary.app.data.PlaceSuggestion
import com.moodiary.app.ui.components.CardShape
import com.moodiary.app.ui.components.Eyebrow
import com.moodiary.app.ui.components.Pill
import com.moodiary.app.ui.components.PillShape
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.ui.theme.MoodiaryType

/**
 * 10 地点选择 — nearby places, frequent places, a free-text name, or none at all.
 *
 * "附近" comes from [com.moodiary.app.data.PlaceSource]; the shipped implementation is
 * a stub because real nearby POIs need a location permission plus a map provider key.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlacePickerScreen(
    nearby: List<PlaceSuggestion>,
    frequent: List<String>,
    selected: String?,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (String?) -> Unit,
    onOpenMap: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.action_cancel),
                style = MoodiaryType.Label,
                color = MoodiaryColors.TextTertiary,
                modifier = Modifier.clip(PillShape).clickable(onClick = onCancel).padding(6.dp),
            )
            Text(
                stringResource(R.string.place_title),
                style = MoodiaryType.TitleSmall,
                color = MoodiaryColors.TextPrimary,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Text(
                stringResource(R.string.action_done),
                style = MoodiaryType.TitleSmall,
                color = MoodiaryColors.Accent,
                modifier = Modifier.clip(PillShape).clickable(onClick = onDone).padding(6.dp),
            )
        }

        SearchBox(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )

        // Typing a name that matches nothing is itself a valid place.
        val custom = query.trim()
        if (custom.isNotEmpty() && nearby.none { it.name == custom }) {
            Pill(
                modifier = Modifier.padding(start = 20.dp, top = 12.dp),
                background = MoodiaryColors.AccentTint,
                padding = PaddingValues(horizontal = 13.dp, vertical = 7.dp),
                onClick = { onSelect(custom) },
            ) {
                Text(
                    stringResource(R.string.place_use_custom, custom),
                    style = MoodiaryType.Chip,
                    color = MoodiaryColors.AccentText,
                )
            }
        }

        Eyebrow(
            stringResource(R.string.place_nearby),
            Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp),
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(CardShape)
                .background(MoodiaryColors.Surface)
                .border(1.dp, MoodiaryColors.Border, CardShape),
        ) {
            nearby.forEachIndexed { index, place ->
                NearbyRow(
                    place = place,
                    selected = place.name == selected,
                    onClick = { onSelect(place.name) },
                )
                if (index != nearby.lastIndex) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MoodiaryColors.Border))
                }
            }
        }

        if (frequent.isNotEmpty()) {
            Eyebrow(
                stringResource(R.string.place_frequent),
                Modifier.padding(start = 20.dp, top = 26.dp, bottom = 8.dp),
            )
            FlowRow(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                frequent.forEach { name ->
                    val isSelected = name == selected
                    Pill(
                        background = if (isSelected) MoodiaryColors.AccentTint else Color.Transparent,
                        border = if (isSelected) MoodiaryColors.AccentOutline else MoodiaryColors.BorderStrong,
                        padding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                        onClick = { onSelect(name) },
                    ) {
                        Text(
                            name,
                            style = MoodiaryType.Chip,
                            color = if (isSelected) MoodiaryColors.AccentText else MoodiaryColors.TextSecondary,
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            TextAction(
                iconRes = R.drawable.ic_map,
                label = stringResource(R.string.place_pick_on_map),
                color = MoodiaryColors.TextTertiary,
                onClick = onOpenMap,
            )
            TextAction(
                iconRes = R.drawable.ic_circle_plus,
                label = stringResource(R.string.place_none),
                color = MoodiaryColors.TextMuted,
                onClick = { onSelect(null) },
            )
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SearchBox(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MoodiaryColors.Canvas)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = MoodiaryColors.TextMuted,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(9.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MoodiaryType.Label.copy(color = MoodiaryColors.TextPrimary),
            cursorBrush = SolidColor(MoodiaryColors.Accent),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            stringResource(R.string.place_search_hint),
                            style = MoodiaryType.Label,
                            color = MoodiaryColors.TextMuted,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun NearbyRow(place: PlaceSuggestion, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(R.drawable.ic_pin),
            contentDescription = null,
            tint = if (selected) MoodiaryColors.Accent else MoodiaryColors.TextMuted,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(place.name, style = MoodiaryType.ListItem, color = MoodiaryColors.TextPrimary)
            place.detail?.let {
                Text(it, style = MoodiaryType.Detail, color = MoodiaryColors.TextMuted)
            }
        }
        if (selected) {
            Icon(
                painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MoodiaryColors.Accent,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@Composable
private fun TextAction(iconRes: Int, label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MoodiaryType.LabelMedium, color = color)
    }
}
