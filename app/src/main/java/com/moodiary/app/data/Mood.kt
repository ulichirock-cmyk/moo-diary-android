package com.moodiary.app.data

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.moodiary.app.R

/**
 * The five moods and their dot colours, straight from the design's `moods` map.
 * Order matters — it is the order the chips appear in the editor and the legend.
 */
enum class Mood(@StringRes val labelRes: Int, val color: Color) {
    HAPPY(R.string.mood_happy, Color(0xFFB8D9B8)),
    CALM(R.string.mood_calm, Color(0xFFB8D0E8)),
    FULL(R.string.mood_full, Color(0xFFF5E08C)),
    TIRED(R.string.mood_tired, Color(0xFFC9C2B5)),
    ANXIOUS(R.string.mood_anxious, Color(0xFFF2B8A1)),
}
