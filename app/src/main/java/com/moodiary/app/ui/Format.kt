package com.moodiary.app.ui

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Chinese date/time formatting, matching the strings written into the design. */
object Fmt {

    private val WEEKDAYS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    private val MONTHS = listOf(
        "一月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "十一月", "十二月",
    )

    /** `周三` */
    fun weekday(date: LocalDate): String = WEEKDAYS[date.dayOfWeek.value - 1]

    /** `九月` — the small label under the big day numeral on a timeline card. */
    fun monthName(date: LocalDate): String = MONTHS[date.monthValue - 1]

    /** `02` */
    fun dayNumeral(date: LocalDate): String = "%02d".format(date.dayOfMonth)

    /** `08:14` */
    fun time(time: LocalTime): String = "%02d:%02d".format(time.hour, time.minute)

    fun time(dateTime: LocalDateTime): String = time(dateTime.toLocalTime())

    /** `9月2日` */
    fun monthDay(date: LocalDate): String = "${date.monthValue}月${date.dayOfMonth}日"

    /** `9月2日 · 周三` */
    fun monthDayWeekday(date: LocalDate, separator: String = " · "): String =
        monthDay(date) + separator + weekday(date)

    /** `8.24` — the compact form used by the insights week range and chart axis. */
    fun shortDate(date: LocalDate): String = "${date.monthValue}.${date.dayOfMonth}"
}
