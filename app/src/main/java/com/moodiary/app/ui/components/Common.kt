package com.moodiary.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.ui.theme.MoodiaryType

/** Card radius used everywhere in the design (`border-radius:14px`). */
val CardShape = RoundedCornerShape(14.dp)

/** The smaller radius used by compact list rows (`border-radius:12px`). */
val RowShape = RoundedCornerShape(12.dp)

/** Image radius (`border-radius:10px`). */
val ImageShape = RoundedCornerShape(10.dp)

/** `border-radius:999px`. */
val PillShape = CircleShape

/**
 * The design's standard card: `#FAF7F2` on a hairline border with a barely-there
 * shadow (`0 1px 2px rgba(31,30,29,0.04)`).
 */
@Composable
fun MoodiaryCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    shape: RoundedCornerShape = CardShape,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = 1.dp,
                shape = shape,
                ambientColor = MoodiaryColors.TextPrimary,
                spotColor = MoodiaryColors.TextPrimary,
            )
            .clip(shape)
            .background(MoodiaryColors.Surface)
            .border(1.dp, MoodiaryColors.Border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Section eyebrow — 11sp, uppercase-ish tracking, muted. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MoodiaryType.Eyebrow,
        color = MoodiaryColors.TextMuted,
        modifier = modifier,
    )
}

/**
 * A rounded rect drawn with a dashed stroke — the "添加照片" tile and the "+ 新标签"
 * chip in the editor.
 */
fun Modifier.dashedBorder(
    color: Color,
    shape: RoundedCornerShape,
    width: Dp = 1.5.dp,
    dash: Dp = 4.dp,
    gap: Dp = 4.dp,
): Modifier = drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    val stroke = Stroke(
        width = width.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx())),
    )
    when (outline) {
        is androidx.compose.ui.graphics.Outline.Rounded ->
            drawPath(androidx.compose.ui.graphics.Path().apply { addRoundRect(outline.roundRect) }, color, style = stroke)
        is androidx.compose.ui.graphics.Outline.Generic -> drawPath(outline.path, color, style = stroke)
        is androidx.compose.ui.graphics.Outline.Rectangle -> drawRect(color, style = stroke)
    }
}

/**
 * Generic pill. Every chip in the design is this shape with a different
 * fill / outline / text colour combination.
 */
@Composable
fun Pill(
    modifier: Modifier = Modifier,
    background: Color = Color.Transparent,
    border: Color? = null,
    borderWidth: Dp = 1.dp,
    padding: PaddingValues = PaddingValues(horizontal = 13.dp, vertical = 6.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(background)
            .then(if (border != null) Modifier.border(borderWidth, border, PillShape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Read-only tag chip as it appears on a timeline card — orange text on a 9% tint. */
@Composable
fun TagChip(tag: String, modifier: Modifier = Modifier) {
    Pill(
        modifier = modifier,
        background = MoodiaryColors.AccentTint,
        padding = PaddingValues(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text("#$tag", style = MoodiaryType.Caption, color = MoodiaryColors.AccentText)
    }
}
