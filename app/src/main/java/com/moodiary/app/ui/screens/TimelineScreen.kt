package com.moodiary.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moodiary.app.R
import com.moodiary.app.data.DiaryEntry
import com.moodiary.app.ui.Fmt
import com.moodiary.app.ui.components.EntryCard
import com.moodiary.app.ui.components.bottomBarContentPadding
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.ui.theme.MoodiaryType
import java.time.LocalDate

/**
 * 01 时间线 — the single-column feed of daily entries.
 *
 * The design dropped the 连续 N 天 pill from this header in the latest revision; the
 * streak still shows on 我的.
 */
@Composable
fun TimelineScreen(
    entries: List<DiaryEntry>,
    onSearch: () -> Unit,
    onOpenEntry: (DiaryEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 20.dp,
            bottom = bottomBarContentPadding() + 40.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { TimelineHeader(today = LocalDate.now(), onSearch = onSearch) }
        if (entries.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.timeline_empty),
                    style = MoodiaryType.Label,
                    color = MoodiaryColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
                )
            }
        }
        items(entries, key = { it.id }) { entry ->
            EntryCard(
                entry = entry,
                onClick = { onOpenEntry(entry) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun TimelineHeader(today: LocalDate, onSearch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.app_name),
            style = MoodiaryType.Wordmark,
            color = MoodiaryColors.TextPrimary,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            Fmt.monthDayWeekday(today),
            style = MoodiaryType.Meta,
            color = MoodiaryColors.TextMuted,
        )
        Spacer(Modifier.weight(1f))
        // Not in the static design — screen 04 needs a way in, and the header is the
        // only chrome the timeline has.
        IconButton(onClick = onSearch, modifier = Modifier.size(40.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = stringResource(R.string.action_search),
                tint = MoodiaryColors.TextTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
