package com.moodiary.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moodiary.app.R
import com.moodiary.app.data.DiaryViewModel
import com.moodiary.app.data.Mood
import com.moodiary.app.ui.Fmt
import com.moodiary.app.ui.components.Eyebrow
import com.moodiary.app.ui.components.ImageShape
import com.moodiary.app.ui.components.MoodDot
import com.moodiary.app.ui.components.PillShape
import com.moodiary.app.ui.components.Pill
import com.moodiary.app.ui.components.SquarePhoto
import com.moodiary.app.ui.components.dashedBorder
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.ui.theme.MoodiaryType
import java.time.LocalDateTime

/**
 * 02 发布 / 编辑 — write first, then photos, mood and tags.
 *
 * Unlike the static design the editor opens empty; the draft lives in
 * [DiaryViewModel] so leaving and coming back really does restore it, which is what
 * the "草稿已自动保存" footer promises.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    vm: DiaryViewModel,
    onDismiss: () -> Unit,
    onPublished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = remember { LocalDateTime.now() }
    var showTagDialog by remember { mutableStateOf(false) }
    val bodyFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { bodyFocus.requestFocus() }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(9),
    ) { uris -> vm.addDraftPhotos(uris.map { it.toString() }) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        EditorTopBar(
            dateLabel = Fmt.monthDay(now.toLocalDate()) + " " + Fmt.weekday(now.toLocalDate()),
            timeLabel = Fmt.time(now),
            canPublish = vm.canPublish,
            onCancel = onDismiss,
            onPublish = {
                vm.publishDraft()
                onPublished()
            },
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(0.dp))

            BasicTextField(
                value = vm.draftText,
                onValueChange = vm::onDraftTextChange,
                textStyle = MoodiaryType.BodyEditor.copy(color = MoodiaryColors.TextPrimary),
                cursorBrush = SolidColor(MoodiaryColors.Accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 120.dp)
                    .focusRequester(bodyFocus),
                decorationBox = { inner ->
                    Box {
                        if (vm.draftText.isEmpty()) {
                            Text(
                                stringResource(R.string.editor_placeholder),
                                style = MoodiaryType.BodyEditor,
                                color = MoodiaryColors.TextMuted,
                            )
                        }
                        inner()
                    }
                },
            )

            PhotoGrid(
                photos = vm.draftPhotos,
                onRemove = vm::removeDraftPhoto,
                onAdd = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Eyebrow(stringResource(R.string.editor_section_mood))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Mood.entries.forEach { mood ->
                        MoodChoiceChip(
                            mood = mood,
                            selected = vm.draftMood == mood,
                            onClick = { vm.onDraftMoodClick(mood) },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Eyebrow(stringResource(R.string.editor_section_tags))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.editorTagOptions().forEach { tag ->
                        TagChoiceChip(
                            tag = tag,
                            selected = tag in vm.draftTags,
                            onClick = { vm.onDraftTagClick(tag) },
                        )
                    }
                    Pill(
                        border = MoodiaryColors.BorderDashed,
                        padding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                        onClick = { showTagDialog = true },
                    ) {
                        Text(
                            stringResource(R.string.editor_new_tag),
                            style = MoodiaryType.Meta,
                            color = MoodiaryColors.TextMuted,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        DraftFooter(visible = vm.draftIsDirty)
        Spacer(Modifier.height(32.dp))
    }

    if (showTagDialog) {
        NewTagDialog(
            onDismiss = { showTagDialog = false },
            onConfirm = { tag ->
                showTagDialog = false
                val clean = tag.trim().removePrefix("#")
                if (clean.isNotEmpty() && clean !in vm.draftTags) vm.onDraftTagClick(clean)
            },
        )
    }
}

@Composable
private fun EditorTopBar(
    dateLabel: String,
    timeLabel: String,
    canPublish: Boolean,
    onCancel: () -> Unit,
    onPublish: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.editor_cancel),
            style = MoodiaryType.Label,
            color = MoodiaryColors.TextTertiary,
            modifier = Modifier.clip(PillShape).clickable(onClick = onCancel).padding(6.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                dateLabel,
                style = MoodiaryType.TitleSmall,
                color = MoodiaryColors.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Text(timeLabel, style = MoodiaryType.Caption, color = MoodiaryColors.TextMuted)
        }
        Pill(
            background = if (canPublish) MoodiaryColors.Accent else MoodiaryColors.Accent.copy(alpha = 0.4f),
            padding = PaddingValues(horizontal = 16.dp, vertical = 7.dp),
            onClick = if (canPublish) onPublish else null,
        ) {
            Text(
                stringResource(R.string.editor_publish),
                style = MoodiaryType.LabelStrong,
                color = Color.White,
            )
        }
    }
}

/** Three-up photo grid: existing photos, then the dashed "添加照片" tile. */
@Composable
private fun PhotoGrid(
    photos: List<String>,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val cells: List<String?> = photos + listOf(null)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { cell ->
                    if (cell == null) {
                        AddPhotoTile(Modifier.weight(1f), onAdd)
                    } else {
                        SquarePhoto(
                            cell,
                            Modifier.weight(1f).clickable { onRemove(cell) },
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AddPhotoTile(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(ImageShape)
            .dashedBorder(MoodiaryColors.BorderDashed, ImageShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_image),
                contentDescription = null,
                tint = MoodiaryColors.TextMuted,
                modifier = Modifier.size(20.dp),
            )
            Text(
                stringResource(R.string.editor_add_photo),
                style = MoodiaryType.Tiny,
                color = MoodiaryColors.TextMuted,
            )
        }
    }
}

@Composable
private fun MoodChoiceChip(mood: Mood, selected: Boolean, onClick: () -> Unit) {
    Pill(
        background = if (selected) MoodiaryColors.AccentSoft else Color.Transparent,
        border = if (selected) MoodiaryColors.Accent else MoodiaryColors.BorderStrong,
        borderWidth = if (selected) 1.5.dp else 1.dp,
        padding = PaddingValues(horizontal = 13.dp, vertical = 6.dp),
        onClick = onClick,
    ) {
        MoodDot(mood)
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(mood.labelRes),
            style = if (selected) MoodiaryType.ChipStrong else MoodiaryType.Chip,
            color = if (selected) MoodiaryColors.AccentText else MoodiaryColors.TextSecondary,
        )
    }
}

@Composable
private fun TagChoiceChip(tag: String, selected: Boolean, onClick: () -> Unit) {
    Pill(
        background = if (selected) MoodiaryColors.AccentTint else Color.Transparent,
        border = if (selected) MoodiaryColors.AccentOutline else MoodiaryColors.BorderStrong,
        padding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
        onClick = onClick,
    ) {
        Text(
            "#$tag",
            style = MoodiaryType.Meta,
            color = if (selected) MoodiaryColors.AccentText else MoodiaryColors.TextTertiary,
        )
    }
}

@Composable
private fun DraftFooter(visible: Boolean) {
    if (!visible) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check),
            contentDescription = null,
            tint = MoodiaryColors.TextMuted,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(R.string.editor_draft_saved),
            style = MoodiaryType.Meta,
            color = MoodiaryColors.TextMuted,
        )
    }
}

@Composable
private fun NewTagDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MoodiaryColors.Surface,
        shape = CircleShape.copy(all = androidx.compose.foundation.shape.CornerSize(14.dp)),
        title = { Text(stringResource(R.string.editor_new_tag), style = MoodiaryType.TitleSmall) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.editor_new_tag_hint)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text(stringResource(R.string.editor_confirm), color = MoodiaryColors.AccentText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.editor_cancel), color = MoodiaryColors.TextTertiary)
            }
        },
    )
}
