package com.moodiary.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moodiary.app.R
import com.moodiary.app.data.Block
import com.moodiary.app.data.DiaryEntry
import com.moodiary.app.ui.Fmt
import com.moodiary.app.ui.components.EntryPhoto
import com.moodiary.app.ui.components.PhotoCaption
import com.moodiary.app.ui.components.TagChip
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.ui.theme.MoodiaryType

/**
 * 07 日记详情 — one entry in full, with 08 更多操作 and 09 删除确认 layered on top of
 * it as the design draws them (both are modal state on this screen, not separate
 * destinations).
 */
@Composable
fun DetailScreen(
    entry: DiaryEntry,
    older: DiaryEntry?,
    newer: DiaryEntry?,
    sheetOpen: Boolean,
    confirmingDelete: Boolean,
    onBack: () -> Unit,
    onOpenSheet: () -> Unit,
    onDismissSheet: () -> Unit,
    onEdit: () -> Unit,
    onAskDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onNavigate: (DiaryEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            DetailTopBar(onBack = onBack, onMore = onOpenSheet)

            Column(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        Fmt.monthDay(entry.date),
                        style = MoodiaryType.DetailDate,
                        color = MoodiaryColors.TextPrimary,
                    )
                    Text(
                        text = entry.place?.let {
                            stringResource(
                                R.string.detail_meta_place,
                                Fmt.weekday(entry.date),
                                Fmt.time(entry.createdAt),
                                it,
                            )
                        } ?: stringResource(
                            R.string.detail_meta,
                            Fmt.weekday(entry.date),
                            Fmt.time(entry.createdAt),
                        ),
                        style = MoodiaryType.Meta,
                        color = MoodiaryColors.TextMuted,
                    )
                }

                // 文中图: paragraphs and photos in the entry's own order. A run of
                // photos is stacked tight; the 24dp of the outer column separates runs
                // from prose.
                entry.blocks.runs().forEach { run ->
                    when (run) {
                        is Block.Text -> Text(
                            run.text,
                            style = MoodiaryType.BodyDetail,
                            color = MoodiaryColors.TextPrimary,
                        )
                        is PhotoRun -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            run.photos.forEach { photo ->
                                EntryPhoto(
                                    photo.uri,
                                    Modifier
                                        .fillMaxWidth()
                                        .height(236.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                )
                                photo.caption?.let { PhotoCaption(it) }
                            }
                        }
                    }
                }

                if (entry.tags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        entry.tags.forEach { TagChip(it) }
                    }
                }

                NeighbourRow(older = older, newer = newer, onNavigate = onNavigate)
            }
            Spacer(Modifier.height(40.dp))
        }

        if (sheetOpen) {
            MoreActionsSheet(
                onDismiss = onDismissSheet,
                onEdit = onEdit,
                onDelete = onAskDelete,
            )
        }

        if (confirmingDelete) {
            DeleteConfirmDialog(
                entry = entry,
                onDismiss = onDismissDelete,
                onConfirm = onConfirmDelete,
            )
        }
    }
}

@Composable
private fun DetailTopBar(onBack: () -> Unit, onMore: () -> Unit) {
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
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onMore) {
            Icon(
                painterResource(R.drawable.ic_more),
                contentDescription = stringResource(R.string.detail_more),
                tint = MoodiaryColors.TextPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** "‹ 9月1日" on the left, the newer neighbour or "没有更新的了" on the right. */
@Composable
private fun NeighbourRow(
    older: DiaryEntry?,
    newer: DiaryEntry?,
    onNavigate: (DiaryEntry) -> Unit,
) {
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(MoodiaryColors.Border))
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (older != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigate(older) }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_chevron_left),
                        contentDescription = null,
                        tint = MoodiaryColors.TextTertiary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        Fmt.monthDay(older.date),
                        style = MoodiaryType.LabelMedium,
                        color = MoodiaryColors.TextTertiary,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (newer != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigate(newer) }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        Fmt.monthDay(newer.date),
                        style = MoodiaryType.LabelMedium,
                        color = MoodiaryColors.TextTertiary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = MoodiaryColors.TextTertiary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            } else {
                Text(
                    stringResource(R.string.detail_no_newer),
                    style = MoodiaryType.Meta,
                    color = MoodiaryColors.Faint,
                )
            }
        }
    }
}

/** 08 更多操作 — the action sheet. Delete lives only here, to keep it off the card. */
@Composable
private fun MoreActionsSheet(
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Scrim(onDismiss)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        SheetCard {
            SheetRow(R.drawable.ic_edit, stringResource(R.string.more_edit), onClick = onEdit)
            SheetDivider()
            SheetRow(
                iconRes = R.drawable.ic_trash,
                label = stringResource(R.string.more_delete),
                tint = MoodiaryColors.Destructive,
                onClick = onDelete,
            )
        }
        Spacer(Modifier.height(8.dp))
        SheetCard {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.action_cancel),
                    style = MoodiaryType.SheetRowStrong,
                    color = MoodiaryColors.TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun SheetCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, spotColor = MoodiaryColors.TextPrimary)
            .clip(shape)
            .background(MoodiaryColors.Surface),
    ) { content() }
}

@Composable
private fun SheetRow(
    iconRes: Int,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MoodiaryColors.TextPrimary,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MoodiaryType.SheetRow, color = tint)
    }
}

@Composable
private fun SheetDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(MoodiaryColors.Border))
}

/** 09 删除确认 —本机删除，照片与文字一并移除. */
@Composable
private fun DeleteConfirmDialog(
    entry: DiaryEntry,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Scrim(onDismiss)
    Box(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(16.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(16.dp, shape, spotColor = MoodiaryColors.TextPrimary)
                .clip(shape)
                .background(MoodiaryColors.Surface),
        ) {
            Column(
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.delete_title, entry.date.monthValue, entry.date.dayOfMonth),
                    style = MoodiaryType.DialogTitle,
                    color = MoodiaryColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = if (entry.photos.isEmpty()) {
                        stringResource(R.string.delete_body_text_only)
                    } else {
                        stringResource(R.string.delete_body_photos, entry.photos.size)
                    },
                    style = MoodiaryType.LabelMedium,
                    color = MoodiaryColors.TextTertiary,
                    textAlign = TextAlign.Center,
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(MoodiaryColors.Border))
            Row(Modifier.fillMaxWidth()) {
                DialogButton(
                    label = stringResource(R.string.action_cancel),
                    color = MoodiaryColors.TextPrimary,
                    strong = false,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                Box(Modifier.width(1.dp).height(50.dp).background(MoodiaryColors.Border))
                DialogButton(
                    label = stringResource(R.string.delete_confirm),
                    color = MoodiaryColors.Destructive,
                    strong = true,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DialogButton(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    strong: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.clickable(onClick = onClick).padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = if (strong) MoodiaryType.SheetRowStrong else MoodiaryType.SheetRow,
            color = color,
        )
    }
}

/** Tap-to-dismiss backdrop, drawn without a ripple so it stays invisible. */
@Composable
private fun Scrim(onDismiss: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MoodiaryColors.Scrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    )
}

/** Consecutive photos of a body, folded into one item so they can be laid out as a group. */
private class PhotoRun(val photos: List<Block.Photo>)

private fun List<Block>.runs(): List<Any> = buildList {
    var pending = ArrayList<Block.Photo>()
    fun flush() {
        if (pending.isNotEmpty()) {
            add(PhotoRun(pending))
            pending = ArrayList()
        }
    }
    for (block in this@runs) {
        when (block) {
            is Block.Photo -> pending.add(block)
            is Block.Text -> {
                flush()
                add(block)
            }
        }
    }
    flush()
}
