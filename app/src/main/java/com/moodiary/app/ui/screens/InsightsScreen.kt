package com.moodiary.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moodiary.app.R
import com.moodiary.app.data.DiaryViewModel.InsightState
import com.moodiary.app.data.ReviewPeriod
import com.moodiary.app.ui.Fmt
import com.moodiary.app.ui.components.CardShape
import com.moodiary.app.ui.components.MoodiaryCard
import com.moodiary.app.ui.components.PillShape
import com.moodiary.app.ui.components.bottomBarContentPadding
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.ui.theme.MoodiaryType

/**
 * 05 洞察 — the index of reviews.
 *
 * The 14-day mood chart and the recurring-themes card were both cut when the design
 * dropped moods. What is left is one 每周回顾 card, which the app extends to the month
 * and the year. Three long paragraphs stacked on one screen read as a wall, so this
 * screen lists them as rows — the same grouped rows 我的 uses — and each opens
 * [ReviewScreen]. Generating on open also means a review is only paid for when
 * someone wants to read it.
 */
@Composable
fun InsightsScreen(
    insights: Map<ReviewPeriod, InsightState>,
    onOpen: (ReviewPeriod) -> Unit,
    onAsk: () -> Unit,
    modifier: Modifier = Modifier,
) {
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

        // 问问日记 — looks like the search field on 04 搜索, opens ChatScreen.
        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 18.dp)
                .fillMaxWidth()
                .clip(PillShape)
                .background(MoodiaryColors.Field)
                .border(1.dp, MoodiaryColors.BorderStrong, PillShape)
                .clickable(onClick = onAsk)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_sparkles),
                contentDescription = null,
                tint = MoodiaryColors.Accent,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.chat_entry_hint),
                style = MoodiaryType.Label,
                color = MoodiaryColors.TextMuted,
            )
        }

        Column(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                .shadow(1.dp, CardShape, spotColor = MoodiaryColors.TextPrimary)
                .clip(CardShape)
                .background(MoodiaryColors.Surface)
                .border(1.dp, MoodiaryColors.Border, CardShape),
        ) {
            ReviewPeriod.entries.forEachIndexed { index, period ->
                if (index > 0) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MoodiaryColors.Border))
                }
                ReviewRow(
                    period = period,
                    insight = insights[period] ?: InsightState.Idle,
                    onClick = { onOpen(period) },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ReviewRow(period: ReviewPeriod, insight: InsightState, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(R.drawable.ic_claude_mark),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(period.title(), style = MoodiaryType.TitleSmall, color = MoodiaryColors.TextPrimary)
            // One line of the paragraph as a teaser once it exists; the date span before.
            val teaser = (insight as? InsightState.Ready)?.text ?: period.caption()
            Text(
                teaser,
                style = MoodiaryType.Caption,
                color = MoodiaryColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(6.dp))
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MoodiaryColors.TextMuted,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
internal fun ReviewPeriod.title(): String = stringResource(
    when (this) {
        ReviewPeriod.WEEK -> R.string.insights_weekly_review
        ReviewPeriod.MONTH -> R.string.insights_monthly_review
        ReviewPeriod.YEAR -> R.string.insights_yearly_review
    },
)

/** "8.31 – 9.6" for the week and month, the year number for the year. */
internal fun ReviewPeriod.caption(): String {
    val range = range()
    return when (this) {
        ReviewPeriod.YEAR -> range.start.year.toString()
        else -> "${Fmt.shortDate(range.start)} – ${Fmt.shortDate(range.endInclusive)}"
    }
}

/** The review card from the design (05 洞察), in every state. Used by [ReviewScreen]. */
@Composable
internal fun ReviewCard(
    period: ReviewPeriod,
    insight: InsightState,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val span = stringResource(
        when (period) {
            ReviewPeriod.WEEK -> R.string.insights_span_week
            ReviewPeriod.MONTH -> R.string.insights_span_month
            ReviewPeriod.YEAR -> R.string.insights_span_year
        },
    )

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
                Text(period.title(), style = MoodiaryType.CardTitleSerif, color = MoodiaryColors.TextPrimary)
                Spacer(Modifier.weight(1f))
                Text(period.caption(), style = MoodiaryType.Caption, color = MoodiaryColors.TextMuted)
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
