package com.moodiary.app.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moodiary.app.R
import com.moodiary.app.data.DiaryEntry
import com.moodiary.app.data.Mood
import com.moodiary.app.data.datesWithEntries
import com.moodiary.app.data.dominantMood
import com.moodiary.app.ui.Fmt
import com.moodiary.app.ui.components.Eyebrow
import com.moodiary.app.ui.components.MoodDot
import com.moodiary.app.ui.components.MoodiaryCard
import com.moodiary.app.ui.components.RowShape
import com.moodiary.app.ui.components.bottomBarContentPadding
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.ui.theme.MoodiaryType
import java.time.LocalDate
import java.time.YearMonth

/** 03 日历回顾 — a month grid where days that have entries carry a mood dot. */
@Composable
fun CalendarScreen(
    entries: List<DiaryEntry>,
    month: YearMonth,
    selected: LocalDate,
    onMonthChange: (YearMonth) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val entryDates = entries.datesWithEntries()
    val daysThisMonth = entryDates.count { YearMonth.from(it) == month }
    val dayEntries = entries.filter { it.date == selected }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomBarContentPadding()),
    ) {
        CalendarHeader(month, daysThisMonth, onMonthChange)

        MoodiaryCard(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            padding = PaddingValues(14.dp),
        ) {
            WeekdayRow()
            MonthGrid(
                month = month,
                today = today,
                selected = selected,
                entries = entries,
                entryDates = entryDates,
                onSelectDate = onSelectDate,
            )
        }

        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                Fmt.monthDay(selected) + " " + Fmt.weekday(selected),
                style = MoodiaryType.TitleSmall,
                color = MoodiaryColors.TextPrimary,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (dayEntries.isEmpty()) stringResource(R.string.calendar_no_entry)
                else stringResource(R.string.calendar_entry_count, dayEntries.size),
                style = MoodiaryType.Meta,
                color = MoodiaryColors.TextMuted,
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            dayEntries.forEach { DayEntryRow(it) }
            Spacer(Modifier.height(6.dp))
            MoodLegend()
        }
    }
}

@Composable
private fun CalendarHeader(month: YearMonth, daysThisMonth: Int, onMonthChange: (YearMonth) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.calendar_title),
            style = MoodiaryType.Wordmark,
            color = MoodiaryColors.TextPrimary,
        )
        Spacer(Modifier.width(8.dp))
        // Month stepping is ours — the static design shows a single fixed month.
        IconButton(onClick = { onMonthChange(month.minusMonths(1)) }, modifier = Modifier.size(28.dp)) {
            Icon(
                painterResource(R.drawable.ic_chevron_left),
                contentDescription = stringResource(R.string.calendar_prev_month),
                tint = MoodiaryColors.TextMuted,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            stringResource(R.string.calendar_year_month, month.year, month.monthValue),
            style = MoodiaryType.Meta,
            color = MoodiaryColors.TextMuted,
        )
        IconButton(onClick = { onMonthChange(month.plusMonths(1)) }, modifier = Modifier.size(28.dp)) {
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = stringResource(R.string.calendar_next_month),
                tint = MoodiaryColors.TextMuted,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(R.string.calendar_month_count, daysThisMonth),
            style = MoodiaryType.Meta,
            color = MoodiaryColors.TextMuted,
            modifier = Modifier.padding(end = 8.dp),
        )
    }
}

@Composable
private fun WeekdayRow() {
    val labels = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        labels.forEach { label ->
            Text(
                label,
                style = MoodiaryType.Caption,
                color = MoodiaryColors.TextMuted,
                modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/**
 * Monday-start grid padded with the tail of the previous month and the head of the
 * next one, exactly like the design's `calDays`.
 */
@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate,
    entries: List<DiaryEntry>,
    entryDates: Set<LocalDate>,
    onSelectDate: (LocalDate) -> Unit,
) {
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value - 1 // Monday == 0
    val gridStart = first.minusDays(leading.toLong())
    val cellCount = 42.takeIf { leading + month.lengthOfMonth() > 35 } ?: 35

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        (0 until cellCount).chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                week.forEach { index ->
                    val date = gridStart.plusDays(index.toLong())
                    DayCell(
                        date = date,
                        inMonth = YearMonth.from(date) == month,
                        isToday = date == today,
                        isSelected = date == selected,
                        mood = if (date in entryDates) entries.dominantMood(date) else null,
                        hasEntry = date in entryDates,
                        onClick = { onSelectDate(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    mood: Mood?,
    hasEntry: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = when {
        isToday -> MoodiaryColors.Accent
        isSelected -> MoodiaryColors.AccentSoft
        else -> Color.Transparent
    }
    val textColor = when {
        isToday -> Color.White
        isSelected -> MoodiaryColors.AccentText
        inMonth -> MoodiaryColors.TextPrimary
        else -> Color(0xFFC9C2B5)
    }
    val dotColor = when {
        isToday && hasEntry -> Color.White.copy(alpha = 0.9f)
        mood != null -> mood.color
        else -> Color.Transparent
    }
    Column(
        modifier = modifier
            .height(45.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MoodiaryType.Label.copy(
                fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = textColor,
        )
        Spacer(Modifier.height(3.dp))
        Box(Modifier.size(4.dp).clip(CircleShape).background(dotColor))
    }
}

@Composable
private fun DayEntryRow(entry: DiaryEntry) {
    MoodiaryCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        shape = RowShape,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                val moodLabel = entry.mood?.let { stringResource(it.labelRes) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    entry.mood?.let {
                        MoodDot(it, size = 6.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        Fmt.time(entry.createdAt) + (moodLabel?.let { " · $it" } ?: ""),
                        style = MoodiaryType.Caption,
                        color = MoodiaryColors.TextMuted,
                    )
                }
                Text(
                    entry.text,
                    style = MoodiaryType.LabelMedium,
                    color = MoodiaryColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MoodiaryColors.TextMuted,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoodLegend() {
    FlowRow(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Eyebrow(stringResource(R.string.calendar_legend), Modifier.padding(top = 2.dp))
        Mood.entries.forEach { mood ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoodDot(mood, size = 6.dp)
                Spacer(Modifier.width(5.dp))
                Text(
                    stringResource(mood.labelRes),
                    style = MoodiaryType.Caption,
                    color = MoodiaryColors.TextTertiary,
                )
            }
        }
    }
}
