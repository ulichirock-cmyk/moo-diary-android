# moodiary — Android

Kotlin + Jetpack Compose 实现的私人图文日记 App。

## 最重要的一条

**这个 App 的 UI 不是自由发挥的，它是一份设计稿的实现。**

设计稿是 Claude Design 上的画布项目《Moodiary 日记应用设计》，
它的源码快照存在 `design/` 目录里。改任何视觉相关的东西之前——
色值、字号、间距、圆角、布局、图标、文案——**先去 `design/Moodiary 设计稿.dc.html`
里确认设计稿是怎么定的**，不要凭感觉改，也不要"顺手优化"成 Material 默认样式。

完整的同步流程、项目 ID、以及「设计稿改了什么 → 改哪个 Kotlin 文件」的映射表，
都在 **`design/SYNC.md`**。设计稿更新时按那份文档走。

`design/` 目录不参与编译，也**不要手改**——它是从设计侧拉下来的快照，
手改会让下一次 diff 失去意义。

## 构建

```bash
./gradlew assembleDebug          # → app/build/outputs/apk/debug/app-debug.apk
```

需要 JDK 17 和带 `platforms;android-35` + `build-tools;35.0.0` 的 Android SDK。
`local.properties` 是 gitignore 的，新克隆下来要自己建：

```
sdk.dir=/你的/android-sdk
DEEPSEEK_API_KEY=sk-...   # 可选,洞察页的每周回顾用它调 deepseek-v4-flash;不填就在 App 里「我的 → AI 洞察」填
```

AGP 8.7.2 / Kotlin 2.0.21 / Compose BOM 2024.10.01 / Room 2.6.1（KSP），minSdk 26、compileSdk 35。

**改完 UI 要装到跑着的模拟器上看一眼，不要只看编译过没过。** 这个项目的 bug 大多是布局层面的
（比如占位文字和输入框叠加错位），编译器不会报。

`adb` 不在 PATH 里，用全路径 `$HOME/android-sdk/platform-tools/adb`：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.moodiary.app/.MainActivity
adb exec-out screencap -p > /tmp/s.png      # 截图自己看
adb shell input tap <x> <y>                 # 屏幕 1080x2400
```

**滚动流畅度别在模拟器上下结论。** 实测（2026-09-06，时间线 42 篇、单向连续快甩）：

| | 模拟器 release | 真机 release | 真机 debug |
|---|---|---|---|
| 刷新率 | ~10fps 封顶 | 120Hz | 120Hz |
| App UI 线程/帧 中位 | 32ms | 0.42ms | 0.78ms |
| 整帧 中位 | 96ms | 3.8ms | 4.2ms |
| 超出 vsync 预算的帧 | 全部 | 0% | 0%（冷图缓存时 6.5%） |

模拟器的 `dequeueBuffer` 中位就有 20–40ms，`dumpsys gfxinfo` 里任何 App 都是 80% janky
（系统设置页同一手势也是 79%），跟 App 无关。真机上时间线是满帧的。

要看 App 自己花了多少，用 `dumpsys gfxinfo <pkg> framestats` 拆帧：
`HandleInputStart→SyncStart` 是 App 在 UI 线程的全部活，`AnimationStart→PerformTraversals`
是 Compose 组合那一段。注意表头列在 Android 16 上多了一列，按表头名字取值别按下标。
120Hz 上 framestats 只存最近 120 帧 ≈ 1 秒，要一次甩动一次 dump，别甩十次再读。

## 结构

```
data/          日记模型、Room 仓库、照片落盘、统计、DeepSeek 客户端 / 回顾生成 / 问答助手 / 标签与写作引导、MCP server
ui/theme/      Color.kt 和 Type.kt 是设计 token 的落点
ui/components/ 卡片、胶囊、照片块、底部栏
ui/screens/    十二屏，文件名对应设计稿编号（07/08/09 同在 DetailScreen.kt）；ReviewScreen 是洞察的子页
ui/map/        自绘的 OSM 瓦片地图：投影、相机、瓦片缓存
ui/nav/        MoodiaryApp.kt —— 四个 tab + 一个浮层back stack（最深三层：编辑→地点→地图）
```

`DiaryRepository` 是接口，当前实现是 Room（`RoomDiaryRepository`，库文件 `moodiary.db`，
首次建库灌入种子日记）；`InMemoryDiaryRepository` 留给预览和测试。
选中的照片会复制进 `filesDir/photos/`，库里存 `file://` 路径（`PhotoStore`）——
相册选择器给的 `content://` URI 重启后就失效，直接存进库等于没存。

## 已知的、有意为之的偏离

下面这些**不是 bug，不要"修正"回去**，理由在 README 的
"Deliberate departures from the static design" 一节：

- 丢掉了 iOS 设备边框（假状态栏、灵动岛、home indicator），改用 edge-to-edge
  和真实系统 inset
- 时间线头部加了搜索图标（设计稿没给搜索入口，不加的话第 4 屏进不去）
- 日历加了翻月按钮，默认当前月
- 「我的」多了一行「AI 洞察」用来填 DeepSeek Key（洞察页的回顾是 AI 生成的，Key 总得有地方填）
- 洞察页改成索引：三行「每周 / 每月 / 年度回顾」，点进 `ReviewScreen` 才看设计稿那张回顾卡、才调 DeepSeek（`data/ReviewPeriod.kt`）
- 洞察页顶部有「问问日记」入口，进 `ChatScreen` 和 AI 对话找日记、问问题。模型只拿到
  search / get 两个工具，日记内容不进提示词；回答里的 `[[id]]` 引用渲染成日期胶囊，点进详情。
  对话只读、进程内保留、不落库（`data/DiaryAssistant.kt`）
- 「我的」有「自动标签」开关（默认开）：发布后用 DeepSeek 按正文补标签，优先从已有标签库选，
  只增不删，每篇最多 4 个（`data/TagSuggester.kt`）
- 「我的」有「写作引导」开关（默认开）：新日记正文的占位文字不再是「今天发生了什么?」，而是 DeepSeek
  按最近几篇的日期/标签/地点（不发正文）写的一句问题，每天一句、存在 prefs 里；没 Key 或请求失败时用
  内置问题顶上。编辑旧日记时不用（`data/WritingPromptSuggester.kt`）
- 「导出 Markdown」（我的页那行和更多操作里的那项）已删掉，改成「我的」里的「Claude Code 连接」开关：
  开着时手机自己当 MCP server（Streamable HTTP，`/mcp`，端口 8765，Bearer token），Claude Code 用
  `claude mcp add --transport http …` 直连，工具就是问问日记那套 search / get 再加一个 overview
  （`data/DiaryTools.kt` 是两边共用的实现，`data/DiaryMcpServer.kt` 手写 HTTP 不引库，
  `DiaryMcpService` 是 specialUse 前台服务）。局域网明文，默认关，用完关
- 文中图：日记正文是「文字 / 照片」块的有序列表（`DiaryEntry.blocks`，Room 里 `blocks` JSON 列，
  版本 2 迁移加的，老行读成「先文字后照片」）。编辑器里正文是一列段落输入框和照片，点「添加照片」
  把照片插在光标处、把段落一分为二；点或长按照片弹「照片标注」，可写一行小字（存在
  `Block.Photo.caption`），删除照片也在这个弹窗里；详情页按块顺序渲染，连续照片紧挨着堆、标注在图下。设计稿的详情页是
  「照片组在上、正文在下」，这是有意偏离。时间线卡片不变，仍按派生的 `text` / `photos` 画
- 编辑器开空白而不是预填草稿；草稿存在 ViewModel 里
- 统计数字实算，不用设计稿里的 216 / 12 / 483
- "Face ID" 改成"生物识别"
- 种子日记按「距今天数」锚定，不写死 2026 年 9 月
- 纯文字日记仍然能写能显示（设计稿第二版把纯文字样例卡删了，但日记不能强制配图）
- 地图是真的，但没有地图 SDK：`ui/map/` 自己画 OpenStreetMap 栅格瓦片，无 key、
  无依赖。**别换成高德/百度瓦片而不改坐标**——那两家是 GCJ-02，配系统定位给的
  WGS-84 会让北京的针偏出几百米。详见 `ui/map/TileMap.kt` 的注释
- 定位是可选的：没授权就开在默认中心（朝阳公园），提示行换成「没有定位权限」，
  屏还是能用。用的是系统 `LocationManager`，**不要引 `play-services-location`**，
  这批机器不一定有 GMS
- `PlaceSource.nearby()` 还是桩：「附近」那种带距离的 POI 搜索要高德/百度的 key，
  这个工程没有。`atPin()` 不是桩，走系统 `Geocoder` 反查地名（机器上没有 geocoder
  后端时退回坐标文本）
- 「我的」最下面单独一张卡是「恢复出厂设置」：日记、照片、AI Key、所有开关、回顾缓存、
  地图瓦片和图片缓存一起清掉，日记本变成**空的**——示例日记不会回来。判断「要不要灌种子」
  靠的是 prefs 里的 `seeded` 标记而不是「表是空的」，否则重置完重启示例又回来了
  （`RoomDiaryRepository`）。它单独一张卡、红色，是为了不挨着「检查更新」被误点。设计稿没有这行
- 版本更新是真的：`GitHubUpdateChecker` 读本仓库最新的 GitHub Release，「立即更新」
  下载那个 APK 再交给系统安装器（Android 不给第三方 App 静默安装，最后一下必须用户点）。
  流程见下面的「发布」

## 发布

打个 tag 就出版本：

```bash
git tag v0.2.0 && git push origin v0.2.0
```

`.github/workflows/release.yml` 构建签名 release APK 并发到 GitHub Releases。
版本号来自 tag（`-PversionName`），`versionCode` 用 Actions 的 run number。
release 正文是上一个 tag 以来的提交标题，每行一条——App 的「版本更新」页就照着这些行画，
所以提交标题写人话。也可以在 Actions 页手动跑，填个版本号。

**签名 key 不能换。** 换了手机就装不上更新，只能卸载重装（日记全丢）。key 在
`~/keys/moodiary-release.jks`（密码在旁边的 `.password` 文件），仓库里没有也不该有。
CI 用这几个 secret：

| secret | 内容 |
|---|---|
| `MOODIARY_KEYSTORE_BASE64` | `base64 -w0 ~/keys/moodiary-release.jks` 的输出 |
| `MOODIARY_KEYSTORE_PASSWORD` | keystore 密码 |
| `DEEPSEEK_API_KEY` | 可选，不配就让用户自己在「我的 → AI 洞察」填 |

本地也能出签名包：

```bash
MOODIARY_KEYSTORE=$HOME/keys/moodiary-release.jks \
MOODIARY_KEYSTORE_PASSWORD=$(cat ~/keys/moodiary-release.password) \
./gradlew assembleRelease -PversionName=0.1.1 -PversionCode=2
```

不给这些变量，release 出来的就是不签名的包（和以前一样），装不上。

## 心情体系已经删了

设计稿第二版把心情整套移除了，改成地点。代码里**没有 `Mood` 类型**，
日历上的点是「这天有记录」的意思（单一 accent 色），不是心情色。
看到旧截图或旧文档提到心情，那是第一版的东西，不要照着加回来。

## 主题

light-only，固定色板，**不跟随系统深色模式，也不用动态取色**。
设计稿是一套暖纸色调，套上 Material You 会整个垮掉。深色模式在设计稿里
列为"下一步"，等设计出了再做。

衬线字体现在用 `FontFamily.Serif` 系统回退（免下载、覆盖中文）。
要换成设计稿指定的 Source Serif 4，把字体文件丢进 `res/font/`，
改 `ui/theme/Type.kt` 里 `MoodiaryFonts` 的两个值，别的地方不用动。
