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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.moodiary.app.R
import com.moodiary.app.data.DiaryEntry
import com.moodiary.app.data.search
import com.moodiary.app.data.tagCounts
import com.moodiary.app.ui.Fmt
import com.moodiary.app.ui.components.Eyebrow
import com.moodiary.app.ui.components.MoodiaryCard
import com.moodiary.app.ui.components.Pill
import com.moodiary.app.ui.components.PillShape
import com.moodiary.app.ui.components.RowShape
import com.moodiary.app.ui.components.ThumbnailPhoto
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.ui.theme.MoodiaryType

/** 04 搜索 / 标签 — full-text search over bodies and tags; tags double as queries. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    entries: List<DiaryEntry>,
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onOpenEntry: (DiaryEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val results = remember(entries, query) { entries.search(query) }
    val tags = remember(entries) { entries.tagCounts() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.action_cancel),
                style = MoodiaryType.Label,
                color = MoodiaryColors.TextTertiary,
                modifier = Modifier.clip(PillShape).clickable(onClick = onDismiss).padding(6.dp),
            )
        }

        if (query.isNotBlank()) {
            Text(
                text = if (results.isEmpty()) {
                    stringResource(R.string.search_no_result, query)
                } else {
                    stringResource(R.string.search_result_count, results.size, query)
                },
                style = MoodiaryType.Meta,
                color = MoodiaryColors.TextMuted,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
            )
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                results.forEach { entry ->
                    SearchResultRow(entry, query) { onOpenEntry(entry) }
                }
            }
        }

        Eyebrow(
            stringResource(R.string.search_common_tags),
            Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp),
        )
        FlowRow(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tags.forEach { (tag, count) ->
                Pill(
                    border = MoodiaryColors.BorderStrong,
                    padding = PaddingValues(horizontal = 13.dp, vertical = 6.dp),
                    onClick = { onQueryChange(tag) },
                ) {
                    Text("#$tag", style = MoodiaryType.Chip, color = MoodiaryColors.TextSecondary)
                    Spacer(Modifier.width(4.dp))
                    Text("$count", style = MoodiaryType.Chip, color = MoodiaryColors.TextMuted)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(MoodiaryColors.Field)
            .border(1.dp, MoodiaryColors.BorderStrong, PillShape)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = MoodiaryColors.TextMuted,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MoodiaryType.Label.copy(color = MoodiaryColors.TextPrimary),
            cursorBrush = SolidColor(MoodiaryColors.Accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            stringResource(R.string.search_hint),
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
private fun SearchResultRow(entry: DiaryEntry, query: String, onClick: () -> Unit) {
    MoodiaryCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        shape = RowShape,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            entry.photos.firstOrNull()?.let {
                ThumbnailPhoto(it)
                Spacer(Modifier.width(12.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    Fmt.monthDay(entry.date) + (entry.place?.let { " · $it" } ?: ""),
                    style = MoodiaryType.Caption,
                    color = MoodiaryColors.TextMuted,
                )
                Text(
                    text = highlight(entry.text, query),
                    style = MoodiaryType.LabelMedium,
                    color = MoodiaryColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Paints every occurrence of [query] with the design's `#F5E08C` marker. */
private fun highlight(text: String, query: String): AnnotatedString {
    val q = query.trim()
    if (q.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var index = 0
        while (true) {
            val hit = text.indexOf(q, index, ignoreCase = true)
            if (hit < 0) {
                append(text.substring(index))
                break
            }
            append(text.substring(index, hit))
            withStyle(SpanStyle(background = MoodiaryColors.Highlight)) {
                append(text.substring(hit, hit + q.length))
            }
            index = hit + q.length
        }
    }
}
