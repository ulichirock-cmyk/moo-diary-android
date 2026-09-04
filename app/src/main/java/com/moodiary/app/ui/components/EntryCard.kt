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
import com.moodiary.app.data.Mood
import com.moodiary.app.ui.Fmt
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.ui.theme.MoodiaryType

/** One card in the timeline: date rail, mood pill, photos, prose, tags, place. */
@Composable
fun EntryCard(entry: DiaryEntry, modifier: Modifier = Modifier) {
    MoodiaryCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EntryCardHeader(entry)

            if (entry.photos.isNotEmpty()) {
                EntryPhotoBlock(entry.photos, Modifier.fillMaxWidth())
            }

            if (entry.text.isNotBlank()) {
                Text(entry.text, style = MoodiaryType.Body, color = MoodiaryColors.TextPrimary)
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
        Spacer(Modifier.weight(1f))
        entry.mood?.let { MoodPill(it) }
    }
}

/** Outlined mood pill — dot plus label, as on the 疲惫 card in the design. */
@Composable
fun MoodPill(mood: Mood, modifier: Modifier = Modifier) {
    Pill(
        modifier = modifier,
        border = MoodiaryColors.BorderStrong,
        padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
    ) {
        MoodDot(mood)
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(mood.labelRes),
            style = MoodiaryType.Caption,
            color = MoodiaryColors.TextSecondary,
        )
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
