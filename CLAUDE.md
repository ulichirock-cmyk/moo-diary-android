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
```

AGP 8.7.2 / Kotlin 2.0.21 / Compose BOM 2024.10.01，minSdk 26、compileSdk 35。

**改完 UI 要真机验证，不要只看编译过没过。** 这个项目的 bug 大多是布局层面的
（比如占位文字和输入框叠加错位），编译器不会报。

## 结构

```
data/          日记模型、内存仓库、统计、Markdown 导出
ui/theme/      Color.kt 和 Type.kt 是设计 token 的落点
ui/components/ 卡片、胶囊、照片块、底部栏
ui/screens/    十二屏，文件名对应设计稿编号（07/08/09 同在 DetailScreen.kt）
ui/nav/        MoodiaryApp.kt —— 四个 tab + 一个浮层back stack（最深三层：编辑→地点→地图）
```

`DiaryRepository` 是接口，当前实现是进程内存储（`InMemoryDiaryRepository`），
换 Room 或文件存储只需要动这一个类。

## 已知的、有意为之的偏离

下面这些**不是 bug，不要"修正"回去**，理由在 README 的
"Deliberate departures from the static design" 一节：

- 丢掉了 iOS 设备边框（假状态栏、灵动岛、home indicator），改用 edge-to-edge
  和真实系统 inset
- 时间线头部加了搜索图标（设计稿没给搜索入口，不加的话第 4 屏进不去）
- 日历加了翻月按钮，默认当前月
- 编辑器开空白而不是预填草稿；草稿存在 ViewModel 里
- 统计数字实算，不用设计稿里的 216 / 12 / 483
- "Face ID" 改成"生物识别"
- 种子日记按「距今天数」锚定，不写死 2026 年 9 月
- 纯文字日记仍然能写能显示（设计稿第二版把纯文字样例卡删了，但日记不能强制配图）
- `PlaceSource` / `MapSurface` / `UpdateChecker` 三处是桩：附近地点要定位权限加
  POI 服务商 key，地图要地图 SDK，更新要发布源，这个工程都没有。桩外面的 UI 是
  真的，换实现只动这三处

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
