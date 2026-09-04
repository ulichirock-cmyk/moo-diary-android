package com.moodiary.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design tokens lifted verbatim from `Moodiary 设计稿.dc.html`.
 * Light-only, as the design canvas specifies ("深色模式" is listed as a next step).
 */
object MoodiaryColors {
    /** Screen background — `#F5F1EB`. */
    val Background = Color(0xFFF5F1EB)

    /** Canvas / inset surface behind chips — `#EDE7DC`. */
    val Canvas = Color(0xFFEDE7DC)

    /** Card surface — `#FAF7F2`. */
    val Surface = Color(0xFFFAF7F2)

    /** Search field fill — `#FFFFFF`. */
    val Field = Color(0xFFFFFFFF)

    /** Hairline border — `rgba(31,30,29,0.08)`. */
    val Border = Color(0xFF1F1E1D).copy(alpha = 0.08f)

    /** Stronger outline used by unselected chips — `rgba(31,30,29,0.14)`. */
    val BorderStrong = Color(0xFF1F1E1D).copy(alpha = 0.14f)

    /** Dashed placeholder outline — `rgba(31,30,29,0.20)`. */
    val BorderDashed = Color(0xFF1F1E1D).copy(alpha = 0.20f)

    val TextPrimary = Color(0xFF1F1E1D)
    val TextSecondary = Color(0xFF46443F)
    val TextTertiary = Color(0xFF6B675F)
    val TextMuted = Color(0xFF8C8579)

    /** Brand orange — FAB, publish button, caret, today pill. */
    val Accent = Color(0xFFD97757)

    /** Darker orange used for text on soft backgrounds. */
    val AccentText = Color(0xFFBC5A3C)

    /** Soft orange fill — streak pill, avatar, selected mood chip. */
    val AccentSoft = Color(0xFFF5E6DD)

    /** Tag fill — `rgba(217,119,87,0.09)`. */
    val AccentTint = Color(0xFFD97757).copy(alpha = 0.09f)

    /** Tag outline in the editor — `rgba(217,119,87,0.5)`. */
    val AccentOutline = Color(0xFFD97757).copy(alpha = 0.5f)

    /** Search-hit highlight — `#F5E08C`. */
    val Highlight = Color(0xFFF5E08C)

    /** Bottom bar. The design turned this opaque; it used to be 94% translucent. */
    val BarBackground = Color(0xFFF5F1EB)

    /** Destructive text and buttons — 删除这篇日记 / 删除. `#B54A3C`. */
    val Destructive = Color(0xFFB54A3C)

    /** Scrim behind the action sheet and the delete dialog — `rgba(31,30,29,0.4)`. */
    val Scrim = Color(0xFF1F1E1D).copy(alpha = 0.4f)

    /** Disabled / future day numerals and inactive hints — `#C9C2B5`. */
    val Faint = Color(0xFFC9C2B5)

    /** Placeholder behind images while they load — `#EDE7DC`. */
    val ImagePlaceholder = Color(0xFFEDE7DC)
}
