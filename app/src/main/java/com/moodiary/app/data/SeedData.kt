package com.moodiary.app.data

import java.time.LocalDate
import java.time.LocalTime

/**
 * The sample diary the app starts with. It reproduces the entries written into
 * `Moodiary 设计稿.dc.html` and fills in the surrounding days so the calendar, the
 * profile counters and the tag/place lists have real material to compute from.
 *
 * Dates are anchored to *today* rather than to September 2026 so the app never looks
 * frozen: `daysAgo = 0` is today, and the 12-day unbroken run (0..11) keeps the
 * profile's 连续天数 at 12 on whatever day the app is opened. Day 12 is left empty on
 * purpose — a real diary has gaps, and the calendar should show one.
 *
 * Every entry carries a place: the design made 地点 the field moods used to be.
 */
private data class Seed(
    val daysAgo: Long,
    val time: LocalTime,
    val text: String,
    val photoSeeds: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val place: String? = null,
)

private fun photo(seed: String) = "https://picsum.photos/seed/$seed/800/600"

private val SEEDS = listOf(
    Seed(
        daysAgo = 0, time = LocalTime.of(8, 14),
        text = "六点半醒,楼下的桂花开了第一茬。给自己煮了燕麦,窗边写了半小时晨间笔记。新的一个月,慢慢来。",
        photoSeeds = listOf("mood-morning7"), tags = listOf("晨间", "桂花"), place = "家",
    ),
    Seed(
        daysAgo = 1, time = LocalTime.of(21, 47),
        text = "下午把拖了两周的方案初稿收尾,奖励自己一杯冰美式。",
        photoSeeds = listOf("mood-desk3", "mood-coffee5"), tags = listOf("工作"), place = "望京",
    ),
    Seed(
        daysAgo = 2, time = LocalTime.of(23, 2),
        text = "加班到十点。回家的地铁上把《夜晚的潜水艇》读完了,有几个句子想抄下来。",
        tags = listOf("阅读", "加班"), place = "公司",
    ),
    Seed(
        daysAgo = 3, time = LocalTime.of(19, 20),
        text = "和小七去朝阳公园野餐,她带的柠檬茶,我带的三明治。草地上一直躺到太阳偏西。",
        photoSeeds = listOf("mood-picnic2", "mood-park9"), tags = listOf("周末", "朋友"), place = "朝阳公园",
    ),
    Seed(
        daysAgo = 4, time = LocalTime.of(7, 5),
        text = "早上六点半起来跑了五公里,回来还赶上做早饭。原来一天可以这么长。",
        photoSeeds = listOf("mood-run4"), tags = listOf("跑步", "晨间"), place = "亮马河",
    ),
    Seed(
        daysAgo = 5, time = LocalTime.of(22, 40),
        text = "把书桌收拾干净,给绿萝换了盆。安静地待了一晚上,什么也没做。",
        tags = listOf("家务"), place = "家",
    ),
    Seed(
        daysAgo = 6, time = LocalTime.of(12, 30),
        text = "下周的评审有点悬,方案还有两块没想清楚。中午在楼下走了两圈。",
        tags = listOf("工作"), place = "公司",
    ),
    Seed(
        daysAgo = 7, time = LocalTime.of(16, 10),
        text = "巷口新开的手冲店,耶加雪菲带一点柑橘,和咖啡师聊了很久。",
        photoSeeds = listOf("mood-coffee8"), tags = listOf("美食", "咖啡"), place = "蓝色港湾",
    ),
    Seed(
        daysAgo = 8, time = LocalTime.of(20, 15),
        text = "会开了一整天,晚上只想瘫着。点了碗面,追了两集剧。",
        tags = listOf("加班"), place = "家",
    ),
    Seed(
        daysAgo = 9, time = LocalTime.of(18, 45),
        text = "健身房换了新的训练计划,肩背练到发抖。回家路上顺手买了一束洋桔梗。",
        photoSeeds = listOf("mood-gym2"), tags = listOf("跑步"), place = "望京",
    ),
    Seed(
        daysAgo = 10, time = LocalTime.of(21, 0),
        text = "和爸妈视频了四十分钟,他们把院子里的柿子拍给我看。快熟了。",
        tags = listOf("家人"), place = "家",
    ),
    Seed(
        daysAgo = 11, time = LocalTime.of(15, 30),
        text = "雨天,在家做了壶手冲咖啡,听了一下午黑胶。",
        photoSeeds = listOf("mood-rain3"), tags = listOf("咖啡", "阅读"), place = "家",
    ),
    // daysAgo = 12 intentionally left blank — the streak above it is exactly 12 days.
    Seed(
        daysAgo = 13, time = LocalTime.of(9, 50),
        text = "把积压的邮件清了个干净,顺便重写了周报模板。清爽。",
        tags = listOf("工作"), place = "公司",
    ),
    Seed(
        daysAgo = 14, time = LocalTime.of(20, 30),
        text = "跟老同学吃了顿火锅,聊到十一点。有些人一年不见也不生分。",
        photoSeeds = listOf("mood-hotpot6"), tags = listOf("朋友", "美食"), place = "团结湖",
    ),
    Seed(
        daysAgo = 15, time = LocalTime.of(7, 40),
        text = "早起看了会儿书,《夜晚的潜水艇》开了个头。窗外一直在下雨。",
        tags = listOf("阅读", "晨间"), place = "奶奶家",
    ),
)

/** Tags offered in the editor even before they have been used. */
val SUGGESTED_TAGS = listOf("周末", "朋友", "美食", "阅读", "工作", "晨间", "跑步", "加班")

/** The author shown on 我的. A single-user app — there is no sign-in. */
const val OWNER_NAME = "念念"

fun seedEntries(today: LocalDate = LocalDate.now()): List<DiaryEntry> =
    SEEDS.map { seed ->
        val date = today.minusDays(seed.daysAgo)
        DiaryEntry(
            id = "seed-${seed.daysAgo}",
            createdAt = date.atTime(seed.time),
            text = seed.text,
            photos = seed.photoSeeds.map(::photo),
            tags = seed.tags,
            place = seed.place,
        )
    }
