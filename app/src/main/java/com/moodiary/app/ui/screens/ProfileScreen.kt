package com.moodiary.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moodiary.app.R
import com.moodiary.app.data.DiaryEntry
import com.moodiary.app.data.OWNER_NAME
import com.moodiary.app.data.photoCount
import com.moodiary.app.data.streak
import com.moodiary.app.ui.components.CardShape
import com.moodiary.app.ui.components.Eyebrow
import com.moodiary.app.ui.components.MoodiaryCard
import com.moodiary.app.ui.components.RowShape
import com.moodiary.app.ui.components.bottomBarContentPadding
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.ui.theme.MoodiaryType

/** 06 我的 — counts, Markdown export, reminder and app lock. */
@Composable
fun ProfileScreen(
    entries: List<DiaryEntry>,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomBarContentPadding()),
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(MoodiaryColors.AccentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    OWNER_NAME.take(1),
                    style = MoodiaryType.Numeral.copy(fontSize = 24.sp),
                    color = MoodiaryColors.AccentText,
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                OWNER_NAME,
                style = MoodiaryType.TitleSmall.copy(fontSize = 18.sp),
                color = MoodiaryColors.TextPrimary,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                value = entries.size.toString(),
                label = stringResource(R.string.profile_entries),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = entries.streak().toString(),
                label = stringResource(R.string.profile_streak),
                accent = true,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = entries.photoCount().toString(),
                label = stringResource(R.string.profile_photos),
                modifier = Modifier.weight(1f),
            )
        }

        Eyebrow(
            stringResource(R.string.profile_section_data),
            Modifier.padding(start = 20.dp, top = 22.dp, bottom = 8.dp),
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .shadow(1.dp, CardShape, spotColor = MoodiaryColors.TextPrimary)
                .clip(CardShape)
                .background(MoodiaryColors.Surface)
                .border(1.dp, MoodiaryColors.Border, CardShape),
        ) {
            SettingRow(
                iconRes = R.drawable.ic_download,
                title = stringResource(R.string.profile_export),
                onClick = onExport,
            )
            RowDivider()
            SettingRow(
                iconRes = R.drawable.ic_clock,
                title = stringResource(R.string.profile_reminder),
                detail = stringResource(R.string.profile_reminder_value),
            )
            RowDivider()
            SettingRow(
                iconRes = R.drawable.ic_lock,
                title = stringResource(R.string.profile_lock),
                detail = stringResource(R.string.profile_lock_value),
                showDivider = false,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    MoodiaryCard(
        modifier = modifier,
        padding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp),
        shape = RowShape,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                value,
                style = MoodiaryType.Numeral,
                color = if (accent) MoodiaryColors.AccentText else MoodiaryColors.TextPrimary,
            )
            Text(label, style = MoodiaryType.Caption, color = MoodiaryColors.TextMuted)
        }
    }
}

@Composable
private fun SettingRow(
    iconRes: Int,
    title: String,
    detail: String? = null,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(iconRes),
            contentDescription = null,
            tint = MoodiaryColors.TextTertiary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            style = if (onClick != null) MoodiaryType.TitleSmall else MoodiaryType.Label,
            color = MoodiaryColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        detail?.let {
            Text(it, style = MoodiaryType.LabelMedium, color = MoodiaryColors.TextMuted)
            Spacer(Modifier.width(6.dp))
        }
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MoodiaryColors.TextMuted,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun RowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MoodiaryColors.Border),
    )
}
