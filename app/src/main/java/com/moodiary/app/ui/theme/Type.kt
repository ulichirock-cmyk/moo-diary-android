package com.moodiary.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * The design pairs `Inter Tight` (UI chrome) with `Source Serif 4` / `Noto Serif SC`
 * (wordmark, dates, diary body). We map those onto the platform families so the app
 * ships with no font download and keeps CJK coverage:
 *
 *  - [MoodiaryFonts.Ui]    -> Roboto, the sans used for labels, chips and metadata
 *  - [MoodiaryFonts.Serif] -> Noto Serif + Noto Serif CJK, the serif used for prose
 *
 * To match the design exactly, drop the real `.ttf` files into `res/font/` and swap
 * these two values for `FontFamily(Font(R.font.source_serif_4, ...))` — nothing else
 * in the app needs to change.
 */
object MoodiaryFonts {
    val Ui: FontFamily = FontFamily.SansSerif
    val Serif: FontFamily = FontFamily.Serif
}

private fun serif(size: Int, weight: FontWeight, lineHeight: Double = size * 1.4) = TextStyle(
    fontFamily = MoodiaryFonts.Serif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

private fun ui(size: Double, weight: FontWeight = FontWeight.Normal, lineHeight: Double = size * 1.4) = TextStyle(
    fontFamily = MoodiaryFonts.Ui,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

/** Named styles that mirror the design's font-size / weight pairs one-to-one. */
object MoodiaryType {
    /** 25sp serif medium — the `moodiary` wordmark and screen titles. */
    val Wordmark = serif(25, FontWeight.Medium, 30.0)

    /** 22sp serif semibold — big numerals (day-of-month, profile stats). */
    val Numeral = serif(22, FontWeight.SemiBold, 24.0)

    /** 30sp serif medium — the date heading on 日记详情. */
    val DetailDate = serif(30, FontWeight.Medium, 35.0)

    /** 23sp serif medium — "有新版本 1.0.0". */
    val UpdateTitle = serif(23, FontWeight.Medium, 28.0)

    /** 19sp serif medium — the picked place name on 地图选点. */
    val PlaceTitle = serif(19, FontWeight.Medium, 24.0)

    /** 18sp serif semibold — the delete dialog's question. */
    val DialogTitle = serif(18, FontWeight.SemiBold, 24.0)

    /** 17sp serif semibold — card headings inside 洞察. */
    val CardTitleSerif = serif(17, FontWeight.SemiBold, 24.0)

    /** 15sp serif, 1.8 line height — diary body in the timeline. */
    val Body = TextStyle(
        fontFamily = MoodiaryFonts.Serif,
        fontSize = 15.sp,
        lineHeight = 27.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    /** 16sp serif, 1.9 line height — the editor's text area. */
    val BodyEditor = TextStyle(
        fontFamily = MoodiaryFonts.Serif,
        fontSize = 16.sp,
        lineHeight = 30.sp,
    )

    /** 16.5sp serif, 1.95 line height — the diary body on 日记详情. */
    val BodyDetail = TextStyle(
        fontFamily = MoodiaryFonts.Serif,
        fontSize = 16.5.sp,
        lineHeight = 32.sp,
    )

    /** 14.5sp serif, 1.85 line height — Claude's weekly review paragraph. */
    val BodyReview = TextStyle(
        fontFamily = MoodiaryFonts.Serif,
        fontSize = 14.5.sp,
        lineHeight = 27.sp,
    )

    val SheetRow = ui(15.0)
    val SheetRowStrong = ui(15.0, FontWeight.SemiBold)
    val TitleSmall = ui(14.0, FontWeight.SemiBold)
    val ListItem = ui(14.5)
    val ListItemSmall = ui(13.5)
    val Detail = ui(11.5)
    val Label = ui(14.0)
    val LabelMedium = ui(13.0)
    val LabelStrong = ui(13.0, FontWeight.SemiBold)
    val Chip = ui(12.5)
    val ChipStrong = ui(12.5, FontWeight.SemiBold)
    val Meta = ui(12.0)
    val MetaStrong = ui(12.0, FontWeight.SemiBold)
    val Caption = ui(11.0)
    val CaptionStrong = ui(11.0, FontWeight.SemiBold)
    val Tiny = ui(10.0)
    val TinyStrong = ui(10.0, FontWeight.SemiBold)

    /** 11sp, uppercase, 0.04em tracking — section eyebrows ("今天的心情", "数据"). */
    val Eyebrow = TextStyle(
        fontFamily = MoodiaryFonts.Ui,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.44.sp,
    )
}

internal val MoodiaryTypography = Typography(
    bodyLarge = MoodiaryType.Body,
    bodyMedium = MoodiaryType.Label,
    labelSmall = MoodiaryType.Caption,
)
