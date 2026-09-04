package com.moodiary.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moodiary.app.R
import com.moodiary.app.data.DiaryEntry
import com.moodiary.app.data.WEEKLY_REVIEW_BODY
import com.moodiary.app.data.dominantMood
import com.moodiary.app.data.photoCount
import com.moodiary.app.data.tagCounts
import com.moodiary.app.ui.Fmt
import com.moodiary.app.ui.components.MoodiaryCard
import com.moodiary.app.ui.components.Pill
import com.moodiary.app.ui.components.bottomBarContentPadding
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.ui.theme.MoodiaryType
import java.time.LocalDate

/** Height of the tallest bar in the 14-day chart, matching the design's 72px box. */
private val ChartHeight = 72.dp

/** 05 洞察 — the weekly review, the mood chart and the recurring themes. */
@Composable
fun InsightsScreen(entries: List<DiaryEntry>, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val weekStart = today.minusDays(6)
    val weekEntries = remember(entries, today) { entries.filter { it.date >= weekStart && it.date <= today } }
    val themes = remember(entries) { entries.tagCounts().take(5) }

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
            entryCount = weekEntries.size,
            photoCount = weekEntries.photoCount(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        MoodChartCard(
            entries = entries,
            today = today,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        ThemesCard(themes, Modifier.padding(horizontal = 16.dp, vertical = 14.dp))
    }
}

@Composable
private fun WeeklyReviewCard(
    weekStart: LocalDate,
    weekEnd: LocalDate,
    entryCount: Int,
    photoCount: Int,
    modifier: Modifier = Modifier,
) {
    MoodiaryCard(modifier = modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
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
                text = "这一周你写下 $entryCount 篇日记、$photoCount 张照片。$WEEKLY_REVIEW_BODY",
                style = MoodiaryType.BodyReview,
                color = MoodiaryColors.TextPrimary,
            )
            Text(
                stringResource(R.string.insights_weekly_footnote, entryCount),
                style = MoodiaryType.Caption,
                color = MoodiaryColors.TextMuted,
            )
        }
    }
}

/**
 * 14 bars, oldest on the left. A day is coloured by its dominant mood; its height
 * reflects how much was written that day, so an empty day reads as a gap.
 */
@Composable
private fun MoodChartCard(entries: List<DiaryEntry>, today: LocalDate, modifier: Modifier = Modifier) {
    val days = remember(entries, today) {
        (13 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val ofDay = entries.filter { it.date == date }
            val weight = ofDay.sumOf { it.text.length } + ofDay.sumOf { it.photos.size } * 12
            date to (entries.dominantMood(date) to weight)
        }
    }
    val maxWeight = days.maxOf { it.second.second }.coerceAtLeast(1)

    MoodiaryCard(modifier = modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    stringResource(R.string.insights_mood_title),
                    style = MoodiaryType.LabelStrong,
                    color = MoodiaryColors.TextPrimary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.insights_mood_subtitle),
                    style = MoodiaryType.Caption,
                    color = MoodiaryColors.TextMuted,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(ChartHeight),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                days.forEach { (_, value) ->
                    val (mood, weight) = value
                    val fraction = if (mood == null) 0.06f else (0.3f + 0.7f * weight / maxWeight)
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction.coerceIn(0.06f, 1f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                            .background(mood?.color ?: MoodiaryColors.Canvas),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    Fmt.shortDate(today.minusDays(13)),
                    style = MoodiaryType.Tiny,
                    color = MoodiaryColors.TextMuted,
                )
                Text(
                    stringResource(R.string.insights_today),
                    style = MoodiaryType.Tiny,
                    color = MoodiaryColors.TextMuted,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemesCard(themes: List<Pair<String, Int>>, modifier: Modifier = Modifier) {
    MoodiaryCard(modifier = modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                themes.forEachIndexed { index, (tag, count) ->
                    Pill(
                        background = MoodiaryColors.Canvas,
                        padding = PaddingValues(horizontal = 13.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "$tag ×$count",
                            style = MoodiaryType.Chip.copy(
                                fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                            color = MoodiaryColors.TextPrimary,
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.insights_themes_note),
                style = MoodiaryType.Meta,
                color = MoodiaryColors.TextTertiary,
            )
        }
    }
}
