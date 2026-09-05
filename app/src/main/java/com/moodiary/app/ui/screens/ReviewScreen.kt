package com.moodiary.app.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moodiary.app.R
import com.moodiary.app.data.DiaryViewModel.InsightState
import com.moodiary.app.data.ReviewPeriod
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.ui.theme.MoodiaryType

/**
 * One review, full screen: opened from a row on 05 洞察. The top bar is the one every
 * pushed screen uses (12 版本更新 sets the pattern); the body is the design's review
 * card. Asking for the review here, not on the index, keeps DeepSeek calls to the
 * reviews someone actually opens.
 */
@Composable
fun ReviewScreen(
    period: ReviewPeriod,
    insight: InsightState,
    onRefresh: (force: Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(period) { onRefresh(false) }

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
                period.title(),
                style = MoodiaryType.TitleSmall,
                color = MoodiaryColors.TextPrimary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.width(48.dp))
        }

        ReviewCard(
            period = period,
            insight = insight,
            onRegenerate = { onRefresh(true) },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp),
        )
        Spacer(Modifier.height(40.dp))
    }
}
