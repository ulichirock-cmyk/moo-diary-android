package com.moodiary.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import com.moodiary.app.ui.Fmt
import com.moodiary.app.ui.components.Eyebrow
import com.moodiary.app.ui.components.ImageShape
import com.moodiary.app.ui.components.Pill
import com.moodiary.app.ui.components.PillShape
import com.moodiary.app.ui.components.SquarePhoto
import com.moodiary.app.ui.components.dashedBorder
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.ui.theme.MoodiaryType
import java.time.LocalDateTime

/**
 * 02 发布 / 编辑 — write first, then photos, place and tags.
 *
 * The design replaced the mood picker with a place row, and removed the
 * "草稿已自动保存" footer. The draft still lives in [DiaryViewModel], which is what
 * lets the place picker cover this screen and hand a name back.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    vm: DiaryViewModel,
    onDismiss: () -> Unit,
    onPublished: () -> Unit,
    onPickPlace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = remember { LocalDateTime.now() }
    val stamp = vm.draftTimestamp(now)
    val editing = vm.editingId != null
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
            dateLabel = Fmt.monthDay(stamp.toLocalDate()) + " " + Fmt.weekday(stamp.toLocalDate()),
            timeLabel = Fmt.time(stamp),
            actionLabel = stringResource(if (editing) R.string.editor_save else R.string.editor_publish),
            canPublish = vm.canPublish,
            onCancel = onDismiss,
            onPublish = {
                vm.publishDraft()
                onPublished()
            },
        )

        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
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
                            // 写作引导: today's question stands in for the placeholder on a
                            // new entry, so the hint is the prompt and nothing else is added.
                            Text(
                                vm.writingPromptHint ?: stringResource(R.string.editor_placeholder),
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
                Eyebrow(stringResource(R.string.editor_section_place))
                PlaceRow(place = vm.draftPlace, onClick = onPickPlace)
            }

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Eyebrow(stringResource(R.string.editor_section_tags))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
        Spacer(Modifier.height(40.dp))
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
    actionLabel: String,
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
            stringResource(R.string.action_cancel),
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
            Text(actionLabel, style = MoodiaryType.LabelStrong, color = Color.White)
        }
    }
}

/** The 地点 field: a filled row that opens the place picker. */
@Composable
private fun PlaceRow(place: String?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MoodiaryColors.Surface)
            .border(1.dp, MoodiaryColors.Border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(R.drawable.ic_pin),
            contentDescription = null,
            tint = MoodiaryColors.Accent,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = place ?: stringResource(R.string.editor_place_empty),
            style = MoodiaryType.Label,
            color = if (place != null) MoodiaryColors.TextPrimary else MoodiaryColors.TextMuted,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MoodiaryColors.Faint,
            modifier = Modifier.size(16.dp),
        )
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
                        SquarePhoto(cell, Modifier.weight(1f).clickable { onRemove(cell) })
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
private fun NewTagDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MoodiaryColors.Surface,
        shape = RoundedCornerShape(CornerSize(14.dp)),
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
                Text(stringResource(R.string.action_confirm), color = MoodiaryColors.AccentText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = MoodiaryColors.TextTertiary)
            }
        },
    )
}
