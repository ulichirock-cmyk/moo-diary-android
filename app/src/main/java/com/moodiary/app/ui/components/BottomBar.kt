package com.moodiary.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moodiary.app.R
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.ui.theme.MoodiaryType

/** The four tabs of the bottom bar, in design order. The FAB sits between 日历 and 洞察. */
enum class Tab(val route: String, val iconRes: Int, val labelRes: Int) {
    TIMELINE("timeline", R.drawable.ic_home, R.string.tab_timeline),
    CALENDAR("calendar", R.drawable.ic_calendar, R.string.tab_calendar),
    INSIGHTS("insights", R.drawable.ic_sparkles, R.string.tab_insights),
    PROFILE("profile", R.drawable.ic_person, R.string.tab_profile),
}

/** Height of the bar itself, excluding the system navigation inset. */
private val BarContentHeight = 60.dp

/** How far the FAB rides above the bar — the design's `margin-top:-14px`. */
private val FabOverhang = 14.dp

/**
 * Bottom padding a scrolling screen needs so its last card clears the translucent bar.
 * The bar floats over the content exactly as it does in the design, so screens pad
 * rather than shrink.
 */
@Composable
fun bottomBarContentPadding(): Dp =
    BarContentHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp

@Composable
fun MoodiaryBottomBar(
    current: Tab,
    onSelect: (Tab) -> Unit,
    onCompose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val hairline = MoodiaryColors.TextPrimary.copy(alpha = 0.08f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(MoodiaryColors.BarBackground)
                drawRect(hairline, size = size.copy(height = 1.dp.toPx()))
            }
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = navInset.coerceAtLeast(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TabItem(Tab.TIMELINE, current, onSelect)
        TabItem(Tab.CALENDAR, current, onSelect)
        ComposeFab(onClick = onCompose)
        TabItem(Tab.INSIGHTS, current, onSelect)
        TabItem(Tab.PROFILE, current, onSelect)
    }
}

@Composable
private fun TabItem(tab: Tab, current: Tab, onSelect: (Tab) -> Unit) {
    val selected = tab == current
    val tint = if (selected) MoodiaryColors.TextPrimary else MoodiaryColors.TextMuted
    Column(
        modifier = Modifier
            .width(52.dp)
            .clip(RowShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 32.dp),
            ) { onSelect(tab) }
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            painter = painterResource(tab.iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(21.dp),
        )
        Text(
            text = stringResource(tab.labelRes),
            style = MoodiaryType.Tiny.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = tint,
        )
    }
}

/**
 * The orange compose button. It is laid out inside the bar but drawn 14dp higher and
 * reports zero extra height, so it overhangs the bar the way the design shows.
 */
@Composable
private fun ComposeFab(onClick: () -> Unit) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(0, -with(density) { FabOverhang.roundToPx() })
                }
            }
            .size(48.dp)
            .shadow(8.dp, CircleShape, spotColor = MoodiaryColors.AccentText)
            .clip(CircleShape)
            .background(MoodiaryColors.Accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = stringResource(R.string.action_new_entry),
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}
