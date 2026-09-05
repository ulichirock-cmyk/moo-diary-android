package com.moodiary.app.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.moodiary.app.R
import com.moodiary.app.data.DiaryEntry
import com.moodiary.app.data.DiaryViewModel.InsightState
import com.moodiary.app.data.ReviewPeriod
import com.moodiary.app.ui.Fmt
import com.moodiary.app.ui.components.MoodiaryCard
import com.moodiary.app.ui.components.bottomBarContentPadding
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.ui.theme.MoodiaryType

/**
 * 05 洞察 — the review cards.
 *
 * The 14-day mood chart and the recurring-themes card were both cut when the design
 * dropped moods; what is left is the 每周回顾 card, repeated for the month and the year.
 * Each paragraph is written by DeepSeek (see [com.moodiary.app.data.InsightGenerator]);
 * the card keeps the design's layout in every state and only swaps the body text.
 */
@Composable
fun InsightsScreen(
    entries: List<DiaryEntry>,
    insights: Map<ReviewPeriod, InsightState>,
    onRefreshAll: () -> Unit,
    onRegenerate: (ReviewPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Ask for reviews whenever the tab is shown; the view model skips the calls when
    // nothing changed since last time.
    LaunchedEffect(entries) { onRefreshAll() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomBarContentPadding()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.insights_title),
            style = MoodiaryType.Wordmark,
            color = MoodiaryColors.TextPrimary,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 10.dp),
        )

        ReviewPeriod.entries.forEach { period ->
            ReviewCard(
                period = period,
                insight = insights[period] ?: InsightState.Idle,
                onRegenerate = { onRegenerate(period) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        Spacer(Modifier.padding(bottom = 12.dp))
    }
}

@Composable
private fun ReviewCard(
    period: ReviewPeriod,
    insight: InsightState,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val range = remember(period) { period.range() }
    val title = stringResource(
        when (period) {
            ReviewPeriod.WEEK -> R.string.insights_weekly_review
            ReviewPeriod.MONTH -> R.string.insights_monthly_review
            ReviewPeriod.YEAR -> R.string.insights_yearly_review
        },
    )
    val span = stringResource(
        when (period) {
            ReviewPeriod.WEEK -> R.string.insights_span_week
            ReviewPeriod.MONTH -> R.string.insights_span_month
            ReviewPeriod.YEAR -> R.string.insights_span_year
        },
    )
    val caption = when (period) {
        ReviewPeriod.YEAR -> range.start.year.toString()
        else -> "${Fmt.shortDate(range.start)} – ${Fmt.shortDate(range.endInclusive)}"
    }

    val body: String
    val bodyColor: Color
    when (insight) {
        is InsightState.Ready -> { body = insight.text; bodyColor = MoodiaryColors.TextPrimary }
        InsightState.Idle, InsightState.Loading -> {
            body = stringResource(R.string.insights_loading, span); bodyColor = MoodiaryColors.TextMuted
        }
        InsightState.NoEntries -> {
            body = stringResource(R.string.insights_empty, span); bodyColor = MoodiaryColors.TextTertiary
        }
        InsightState.NoKey -> { body = stringResource(R.string.insights_no_key); bodyColor = MoodiaryColors.TextTertiary }
        is InsightState.Error -> {
            body = stringResource(R.string.insights_error, insight.message); bodyColor = MoodiaryColors.TextTertiary
        }
    }
    val canRegenerate = insight is InsightState.Ready || insight is InsightState.Error

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
                Text(title, style = MoodiaryType.CardTitleSerif, color = MoodiaryColors.TextPrimary)
                Spacer(Modifier.weight(1f))
                Text(caption, style = MoodiaryType.Caption, color = MoodiaryColors.TextMuted)
            }
            Text(text = body, style = MoodiaryType.BodyReview, color = bodyColor)
            if (canRegenerate) {
                Text(
                    stringResource(R.string.insights_regenerate),
                    style = MoodiaryType.CaptionStrong,
                    color = MoodiaryColors.AccentText,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable(onClick = onRegenerate),
                )
            }
        }
    }
}
