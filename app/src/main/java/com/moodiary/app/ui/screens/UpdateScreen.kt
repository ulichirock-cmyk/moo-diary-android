package com.moodiary.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moodiary.app.R
import com.moodiary.app.data.DiaryViewModel.UpdateState
import com.moodiary.app.data.UpdateInfo
import com.moodiary.app.ui.components.Eyebrow
import com.moodiary.app.ui.components.MoodiaryCard
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.ui.theme.MoodiaryType

/**
 * 12 版本更新.
 *
 * [update] is null when the installed build is current — the design only draws the
 * "new version" state, so the up-to-date copy is ours. The data is the latest GitHub
 * Release (see [com.moodiary.app.data.GitHubUpdateChecker]); 立即更新 downloads that
 * APK and hands it to the system installer, so the button doubles as the progress
 * readout — the design has nowhere else to put it.
 */
@Composable
fun UpdateScreen(
    currentVersion: String,
    update: UpdateInfo?,
    state: UpdateState,
    onBack: () -> Unit,
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
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
                stringResource(R.string.update_title),
                style = MoodiaryType.TitleSmall,
                color = MoodiaryColors.TextPrimary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.width(48.dp))
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .padding(bottom = 8.dp)
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MoodiaryColors.AccentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_update),
                    contentDescription = null,
                    tint = MoodiaryColors.Accent,
                    modifier = Modifier.size(26.dp),
                )
            }
            Text(
                text = update?.let { stringResource(R.string.update_found, it.version) }
                    ?: stringResource(R.string.update_up_to_date),
                style = MoodiaryType.UpdateTitle,
                color = MoodiaryColors.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = update?.let {
                    stringResource(R.string.update_current, currentVersion, it.downloadSize)
                } ?: stringResource(R.string.update_current_only, currentVersion),
                style = MoodiaryType.LabelMedium,
                color = MoodiaryColors.TextMuted,
            )
        }

        if (update != null) {
            MoodiaryCard(
                // Full width, like the 立即更新 block below it: the design gives both the
                // same 20px inset, and without fillMaxWidth the card shrinks to its
                // longest release note.
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 34.dp),
                padding = PaddingValues(20.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Eyebrow(stringResource(R.string.update_notes))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        update.notes.forEach { note ->
                            Text(
                                note,
                                style = MoodiaryType.ListItemSmall,
                                color = MoodiaryColors.TextSecondary,
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val downloading = state as? UpdateState.Downloading
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MoodiaryColors.Accent)
                        .clickable(enabled = downloading == null, onClick = onUpdate)
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when {
                            downloading != null -> stringResource(
                                R.string.update_downloading,
                                (downloading.progress * 100).toInt(),
                            )
                            state is UpdateState.Failed -> stringResource(R.string.update_retry)
                            else -> stringResource(R.string.update_now)
                        },
                        style = MoodiaryType.SheetRowStrong,
                        color = Color.White,
                    )
                }
                if (state is UpdateState.Failed) {
                    Text(
                        state.message,
                        style = MoodiaryType.ListItemSmall,
                        color = MoodiaryColors.TextMuted,
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    stringResource(R.string.update_later),
                    style = MoodiaryType.ListItemSmall,
                    color = MoodiaryColors.TextMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onBack)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}
