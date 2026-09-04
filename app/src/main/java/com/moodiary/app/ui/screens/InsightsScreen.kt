package com.moodiary.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.moodiary.app.R
import com.moodiary.app.data.DiaryEntry
import com.moodiary.app.data.photoCount
import com.moodiary.app.ui.Fmt
import com.moodiary.app.ui.components.MoodiaryCard
import com.moodiary.app.ui.components.bottomBarContentPadding
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.ui.theme.MoodiaryType
import java.time.LocalDate

/**
 * 05 洞察 — Claude's weekly review.
 *
 * The 14-day mood chart and the recurring-themes card were both cut when the design
 * dropped moods; this screen is now the single review card.
 */
@Composable
fun InsightsScreen(entries: List<DiaryEntry>, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val weekStart = today.minusDays(6)
    val weekEntries = remember(entries, today) {
        entries.filter { it.date >= weekStart && it.date <= today }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomBarContentPadding()),
    ) {
        Text(
            stringResource(R.string.insights_title),
            style = MoodiaryType.Wordmark,
            color = MoodiaryColors.TextPrimary,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp),
        )

        WeeklyReviewCard(
            weekStart = weekStart,
            weekEnd = today,
            entries = weekEntries,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp),
        )
        Spacer(Modifier.padding(bottom = 24.dp))
    }
}

@Composable
private fun WeeklyReviewCard(
    weekStart: LocalDate,
    weekEnd: LocalDate,
    entries: List<DiaryEntry>,
    modifier: Modifier = Modifier,
) {
    val averageLength = if (entries.isEmpty()) 0 else entries.sumOf { it.text.length } / entries.size

    MoodiaryCard(modifier = modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(R.drawable.ic_claude_mark),
                    contentDescription = stringResource(R.string.cd_claude),
                    tint = Color.Unspecified,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.insights_weekly_review),
                    style = MoodiaryType.CardTitleSerif,
                    color = MoodiaryColors.TextPrimary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${Fmt.shortDate(weekStart)} – ${Fmt.shortDate(weekEnd)}",
                    style = MoodiaryType.Caption,
                    color = MoodiaryColors.TextMuted,
                )
            }
            Text(
                text = stringResource(
                    R.string.insights_weekly_body,
                    entries.size,
                    entries.photoCount(),
                    averageLength,
                ),
                style = MoodiaryType.BodyReview,
                color = MoodiaryColors.TextPrimary,
            )
            Text(
                stringResource(R.string.insights_weekly_footnote, entries.size),
                style = MoodiaryType.Caption,
                color = MoodiaryColors.TextMuted,
            )
        }
    }
}
