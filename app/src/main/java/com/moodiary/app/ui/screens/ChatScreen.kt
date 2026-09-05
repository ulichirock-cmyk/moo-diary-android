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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moodiary.app.R
import com.moodiary.app.data.ChatMessage
import com.moodiary.app.data.DiaryEntry
import com.moodiary.app.ui.Fmt
import com.moodiary.app.ui.components.Pill
import com.moodiary.app.ui.components.PillShape
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.ui.theme.MoodiaryType

/**
 * 问问日记 — a read-only chat over the diary, opened from 05 洞察.
 *
 * Not in the design: it borrows the pieces that are. The top bar is the pushed-screen
 * bar, the input is the 04 搜索 field, answers sit on the timeline's card surface and
 * the user's own turns on the accent tint. Cited entries become date chips under an
 * answer and open 07 详情.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    input: String,
    busy: Boolean,
    entries: List<DiaryEntry>,
    onInputChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onClear: () -> Unit,
    onOpenEntry: (DiaryEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, busy) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size + if (busy) 1 else 0)
    }

    Column(modifier.fillMaxSize().imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painterResource(R.drawable.ic_chevron_left),
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MoodiaryColors.TextPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                stringResource(R.string.chat_title),
                style = MoodiaryType.TitleSmall,
                color = MoodiaryColors.TextPrimary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                if (messages.isNotEmpty()) {
                    Text(
                        stringResource(R.string.chat_clear),
                        style = MoodiaryType.LabelMedium,
                        color = MoodiaryColors.TextTertiary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onClear)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
            }
        }

        if (messages.isEmpty()) {
            EmptyPrompt(onPick = onSend, modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(messages) { _, message ->
                    if (message.fromUser) {
                        UserBubble(message.text)
                    } else {
                        AssistantBubble(message, entries, onOpenEntry)
                    }
                }
                if (busy) {
                    item {
                        AssistantSurface {
                            Text(
                                stringResource(R.string.chat_thinking),
                                style = MoodiaryType.ListItem,
                                color = MoodiaryColors.TextMuted,
                            )
                        }
                    }
                }
            }
        }

        InputBar(
            value = input,
            busy = busy,
            onValueChange = onInputChange,
            onSend = { onSend(input) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .navigationBarsPadding(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyPrompt(onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    // Just the suggestions, centred in the empty space — no illustration, no caption.
    Box(modifier.fillMaxWidth().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(R.string.chat_suggestion_1, R.string.chat_suggestion_2, R.string.chat_suggestion_3).forEach { res ->
                val text = stringResource(res)
                Pill(border = MoodiaryColors.BorderStrong, onClick = { onPick(text) }) {
                    Text(text, style = MoodiaryType.Chip, color = MoodiaryColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Box(
            Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                .background(MoodiaryColors.AccentSoft)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(text, style = MoodiaryType.ListItem, color = MoodiaryColors.TextPrimary)
        }
    }
}

@Composable
private fun AssistantSurface(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Box(
            Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                .background(MoodiaryColors.Surface)
                .border(1.dp, MoodiaryColors.Border, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssistantBubble(
    message: ChatMessage,
    entries: List<DiaryEntry>,
    onOpenEntry: (DiaryEntry) -> Unit,
) {
    val cited = message.refs.mapNotNull { id -> entries.firstOrNull { it.id == id } }
    AssistantSurface {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                message.text,
                style = MoodiaryType.ListItem,
                color = if (message.isError) MoodiaryColors.TextTertiary else MoodiaryColors.TextPrimary,
            )
            if (cited.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    cited.forEach { entry ->
                        Pill(
                            background = MoodiaryColors.AccentTint,
                            padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            onClick = { onOpenEntry(entry) },
                        ) {
                            Text(
                                buildString {
                                    append(Fmt.monthDay(entry.date))
                                    entry.place?.let { append(" · ").append(it) }
                                },
                                style = MoodiaryType.Caption,
                                color = MoodiaryColors.AccentText,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InputBar(
    value: String,
    busy: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(MoodiaryColors.Field)
            .border(1.dp, MoodiaryColors.BorderStrong, PillShape)
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            maxLines = 4,
            textStyle = MoodiaryType.Label.copy(color = MoodiaryColors.TextPrimary),
            cursorBrush = SolidColor(MoodiaryColors.Accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            modifier = Modifier.weight(1f).padding(vertical = 6.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            stringResource(R.string.chat_input_hint),
                            style = MoodiaryType.Label,
                            color = MoodiaryColors.TextMuted,
                        )
                    }
                    inner()
                }
            },
        )
        Spacer(Modifier.width(8.dp))
        val canSend = value.isNotBlank() && !busy
        Box(
            Modifier
                .size(32.dp)
                .clip(PillShape)
                .background(if (canSend) MoodiaryColors.Accent else MoodiaryColors.Faint)
                .then(if (canSend) Modifier.clickable(onClick = onSend) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = stringResource(R.string.chat_send),
                tint = MoodiaryColors.Surface,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
