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

    // ── Earlier in the year ──────────────────────────────────────────────────
    // Sparser, two to four a month, so 每月回顾 and 年度回顾 have an arc to find:
    // running starts mid-year and sticks, 小七 keeps showing up, the parents' persimmon
    // tree and 《夜晚的潜水艇》 recur, and the workload has a visible peak.
    Seed(
        daysAgo = 19, time = LocalTime.of(22, 10),
        text = "台风外围的雨下了一整天。在家把上半年的照片整理成了相册,删掉三百多张重复的。",
        photoSeeds = listOf("mood-album1"), tags = listOf("家务"), place = "家",
    ),
    Seed(
        daysAgo = 24, time = LocalTime.of(8, 0),
        text = "跑步满一百天。今天没跑,只是走了走。膝盖有点不舒服,得换双鞋了。",
        tags = listOf("跑步", "晨间"), place = "亮马河",
    ),
    Seed(
        daysAgo = 31, time = LocalTime.of(19, 40),
        text = "小七的婚礼。她在台上念誓词的时候我哭得比新郎还厉害。回来的路上一直在想,认识她十一年了。",
        photoSeeds = listOf("mood-wedding1", "mood-wedding2", "mood-wedding3"), tags = listOf("朋友"), place = "顺义",
    ),
    Seed(
        daysAgo = 38, time = LocalTime.of(23, 30),
        text = "连续第三周加班到十一点。今天在电梯里差点睡着。方案改到第七版,客户说还是想要第一版的感觉。",
        tags = listOf("加班", "工作"), place = "公司",
    ),
    Seed(
        daysAgo = 45, time = LocalTime.of(6, 50),
        text = "热得跑不动,五公里跑成了三公里。但回来冲完澡那一刻,觉得这一天已经赚到了。",
        photoSeeds = listOf("mood-summer-run"), tags = listOf("跑步", "晨间"), place = "亮马河",
    ),
    Seed(
        daysAgo = 52, time = LocalTime.of(17, 20),
        text = "去看了蔡国强的展。有一幅火药画站了十分钟没走。展厅冷气太足,出来时外面像蒸笼。",
        photoSeeds = listOf("mood-exhibit1", "mood-exhibit2"), tags = listOf("周末"), place = "798",
    ),
    Seed(
        daysAgo = 60, time = LocalTime.of(21, 15),
        text = "爸妈来北京住了一周,今天送他们上高铁。妈妈把冰箱塞满了,爸爸修好了那盏一直接触不良的台灯。",
        photoSeeds = listOf("mood-parents1"), tags = listOf("家人"), place = "北京南站",
    ),
    Seed(
        daysAgo = 67, time = LocalTime.of(12, 5),
        text = "项目上线了。团队一起吃了顿午饭,下午没人干活。三个月的东西,终于放出去了。",
        photoSeeds = listOf("mood-launch1"), tags = listOf("工作", "美食"), place = "望京",
    ),
    Seed(
        daysAgo = 75, time = LocalTime.of(7, 30),
        text = "跑步第三十天。从两公里喘到现在五公里不停,原来真的可以。今天第一次有人在河边跟我点头。",
        tags = listOf("跑步", "晨间"), place = "亮马河",
    ),
    Seed(
        daysAgo = 83, time = LocalTime.of(20, 50),
        text = "梅雨似的一周,衣服都晾不干。晚上重读《小王子》,还是会在狐狸那一章停下来。",
        tags = listOf("阅读"), place = "家",
    ),
    Seed(
        daysAgo = 90, time = LocalTime.of(22, 0),
        text = "和小七吃饭,她说要结婚了。我愣了三秒才反应过来要祝福。回家路上买了瓶酒,一个人喝了两杯。",
        photoSeeds = listOf("mood-dinner4"), tags = listOf("朋友", "美食"), place = "三里屯",
    ),
    Seed(
        daysAgo = 98, time = LocalTime.of(6, 45),
        text = "今天开始跑步。跑了两公里就喘得不行,但是六点的亮马河真好看。先坚持一个月看看。",
        photoSeeds = listOf("mood-first-run"), tags = listOf("跑步", "晨间"), place = "亮马河",
    ),
    Seed(
        daysAgo = 105, time = LocalTime.of(23, 40),
        text = "体检报告出来了,血脂偏高。医生说要动起来。三十岁的身体开始跟我算账了。",
        tags = listOf("加班"), place = "家",
    ),
    Seed(
        daysAgo = 115, time = LocalTime.of(19, 0),
        text = "五一去了趟青岛。海边风大得睁不开眼,但啤酒是真的好喝。第一次一个人旅行,比想象中自在。",
        photoSeeds = listOf("mood-qingdao1", "mood-qingdao2", "mood-qingdao3", "mood-qingdao4"), tags = listOf("周末"), place = "青岛",
    ),
    Seed(
        daysAgo = 124, time = LocalTime.of(21, 20),
        text = "新项目启动,客户是家做户外装备的。第一次开会就吵了一架,但吵完反而清楚了。",
        tags = listOf("工作"), place = "公司",
    ),
    Seed(
        daysAgo = 133, time = LocalTime.of(15, 45),
        text = "玉渊潭的樱花开了。人太多,拍了两张就走了。倒是回来路上一棵没人看的海棠开得特别好。",
        photoSeeds = listOf("mood-sakura1", "mood-haitang"), tags = listOf("周末"), place = "玉渊潭",
    ),
    Seed(
        daysAgo = 142, time = LocalTime.of(22, 30),
        text = "买了盆绿萝放在书桌上。据说很好养,希望这次别再养死了。",
        photoSeeds = listOf("mood-plant1"), tags = listOf("家务"), place = "家",
    ),
    Seed(
        daysAgo = 150, time = LocalTime.of(20, 10),
        text = "和小七去看了场话剧,《恋爱的犀牛》。散场后在剧场门口聊到十一点,她最近好像有什么事没说。",
        tags = listOf("朋友"), place = "东城",
    ),
    Seed(
        daysAgo = 160, time = LocalTime.of(9, 30),
        text = "北京的春天来得很突然,昨天还穿羽绒服,今天就能穿衬衫了。楼下的柳树一夜之间绿了。",
        photoSeeds = listOf("mood-spring1"), tags = listOf("晨间"), place = "家",
    ),
    Seed(
        daysAgo = 172, time = LocalTime.of(23, 55),
        text = "年后第一周,邮件堆了三百多封。加班到十二点,打车回家的时候司机在听相声,莫名被治愈了。",
        tags = listOf("加班", "工作"), place = "公司",
    ),
    Seed(
        daysAgo = 185, time = LocalTime.of(14, 0),
        text = "初三,和爸爸去后山看了那棵柿子树。他说今年要给它剪枝。妈妈在家包饺子,视频里全是面粉。",
        photoSeeds = listOf("mood-cny1", "mood-cny2"), tags = listOf("家人"), place = "老家",
    ),
    Seed(
        daysAgo = 189, time = LocalTime.of(22, 0),
        text = "除夕。一家人看春晚,爸爸照例睡着。零点的时候给小七发了消息,她秒回。",
        photoSeeds = listOf("mood-cny3"), tags = listOf("家人"), place = "老家",
    ),
    Seed(
        daysAgo = 200, time = LocalTime.of(18, 30),
        text = "回家的高铁上。带了三本书,一本都没看,一直在看窗外。北方的冬天是土黄色的。",
        tags = listOf("家人", "阅读"), place = "高铁上",
    ),
    Seed(
        daysAgo = 214, time = LocalTime.of(21, 45),
        text = "年终总结写完了。回头看这一年,做的事不少,记得住的没几件。明年想认真记点什么。",
        tags = listOf("工作"), place = "家",
    ),
    Seed(
        daysAgo = 228, time = LocalTime.of(8, 20),
        text = "下雪了。今年第一场。上班路上走得很慢,鞋踩在雪上的声音很好听。",
        photoSeeds = listOf("mood-snow1"), tags = listOf("晨间"), place = "望京",
    ),
    Seed(
        daysAgo = 240, time = LocalTime.of(23, 10),
        text = "新年第一天,给自己定了三件事:跑步、读书、每天写几句。不知道能坚持多久,先写下来。",
        tags = listOf("晨间"), place = "家",
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
