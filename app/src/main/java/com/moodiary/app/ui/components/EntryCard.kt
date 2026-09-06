package com.moodiary.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moodiary.app.R
import com.moodiary.app.data.DiaryEntry
import com.moodiary.app.ui.Fmt
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.ui.theme.MoodiaryType

/**
 * One card in the timeline: date rail, photos, prose, tags, place.
 *
 * The design removed the mood pill that used to sit on the right of the header, so
 * the header row now ends after the weekday/time column.
 */
@Composable
fun EntryCard(entry: DiaryEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val photos = entry.photos
    val text = entry.text
    MoodiaryCard(
        modifier = modifier.fillMaxWidth(),
        padding = PaddingValues(20.dp),
        onClick = onClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            EntryCardHeader(entry)

            if (photos.isNotEmpty()) {
                EntryPhotoBlock(photos, Modifier.fillMaxWidth())
            }

            if (text.isNotBlank()) {
                Text(text, style = MoodiaryType.Body, color = MoodiaryColors.TextPrimary)
            }

            if (entry.tags.isNotEmpty() || entry.place != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    entry.tags.forEach { TagChip(it) }
                    Spacer(Modifier.weight(1f))
                    entry.place?.let { PlaceLabel(it) }
                }
            }
        }
    }
}

@Composable
private fun EntryCardHeader(entry: DiaryEntry) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier.width(38.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                Fmt.dayNumeral(entry.date),
                style = MoodiaryType.Numeral,
                color = MoodiaryColors.TextPrimary,
            )
            Text(
                Fmt.monthName(entry.date),
                style = MoodiaryType.Tiny.copy(letterSpacing = 0.6.sp),
                color = MoodiaryColors.TextMuted,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                Fmt.weekday(entry.date),
                style = MoodiaryType.LabelStrong,
                color = MoodiaryColors.TextSecondary,
            )
            Text(
                Fmt.time(entry.createdAt),
                style = MoodiaryType.Caption,
                color = MoodiaryColors.TextMuted,
            )
        }
    }
}

@Composable
fun PlaceLabel(place: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_pin),
            contentDescription = stringResource(R.string.cd_location),
            tint = MoodiaryColors.TextMuted,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(place, style = MoodiaryType.Caption, color = MoodiaryColors.TextMuted)
    }
}
