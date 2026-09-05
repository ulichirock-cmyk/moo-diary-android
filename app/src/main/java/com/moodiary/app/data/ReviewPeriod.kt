package com.moodiary.app.data

import java.time.LocalDate

/**
 * The three review cards on 05 洞察. Each one is "the period containing today, up to
 * today": the last seven days, this calendar month so far, this calendar year so far.
 */
enum class ReviewPeriod(
    /** Target length of the generated paragraph, in Chinese characters. */
    val minChars: Int,
    val maxChars: Int,
    /** Entry bodies are cut to this many characters before they go into the prompt. */
    val perEntryChars: Int,
    val maxTokens: Int,
) {
    WEEK(minChars = 70, maxChars = 100, perEntryChars = Int.MAX_VALUE, maxTokens = 600),
    MONTH(minChars = 100, maxChars = 150, perEntryChars = 240, maxTokens = 800),
    YEAR(minChars = 160, maxChars = 230, perEntryChars = 120, maxTokens = 1000);

    fun range(today: LocalDate = LocalDate.now()): ClosedRange<LocalDate> = when (this) {
        WEEK -> today.minusDays(6)..today
        MONTH -> today.withDayOfMonth(1)..today
        YEAR -> today.withDayOfYear(1)..today
    }

    /** How the prompt names the span, e.g. "这一周" — used in the system prompt. */
    val label: String
        get() = when (this) {
            WEEK -> "这一周"
            MONTH -> "这个月"
            YEAR -> "这一年"
        }

    /** What the model should pay attention to at this zoom level. */
    val focus: String
        get() = when (this) {
            WEEK -> "哪天写得最长或最短、哪个标签或地点反复出现、时间上的规律"
            MONTH -> "这个月的整体节奏、反复出现的主题或地点、值得记住的一两个瞬间。时间范围到哪天就只谈到哪天,月份还没过完的话不要谈「月中」「月末」或前后半月的对比"
            YEAR -> "这一年的主线和转折、几个反复出现的主题、写日记的习惯是怎么变化的、最值得留下的几个片段。按月份粗略回顾即可,不要逐篇复述;时间范围到哪天就只谈到哪天,还没到的月份不要提"
        }
}
