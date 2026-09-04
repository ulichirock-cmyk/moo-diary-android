# 设计稿同步

这个目录是 Claude Design 画布项目《Moodiary 日记应用设计》的**源码快照**。
它不参与编译，唯一的用途是：设计稿更新时，先把新版拉下来覆盖这里，
`git diff` 就直接告诉你改了哪几行，不用整篇重读四万多字的 HTML。

仓库根目录的 `CLAUDE.md` 是入口——任何一份克隆里开 Claude Code 会话都会自动加载它，
里面第一条就指向这份文档。

## 项目坐标

| | |
|---|---|
| 项目 ID | `b270202f-0b14-42df-9cae-e15fa11b1aea` |
| 名称 | Moodiary 日记应用设计 |
| 画布链接 | https://claude.ai/design/p/b270202f-0b14-42df-9cae-e15fa11b1aea |
| 主文件 | `Moodiary 设计稿.dc.html` |

## 同步步骤

1. **授权**（首次或过期时）：在 Claude Code 里执行 `/design-login`。
2. **拉取**：用 `DesignSync` 的 `get_file` 读 `Moodiary 设计稿.dc.html`。
   这个文件大到会触发工具输出落盘，返回里会给一个
   `tool-results/*.txt` 路径——**用它，不要照着对话誊写**：

   ```python
   import json
   o = json.load(open(TOOL_RESULT_TXT, encoding="utf-8"))
   assert o.get("truncated") is not True
   open("design/Moodiary 设计稿.dc.html", "w", encoding="utf-8", newline="").write(o["content"])
   ```

   这样快照是逐字节的，diff 里出现的每一行都是真的设计改动。
3. **看差异**：`git diff design/` —— 只有真正改动的那几行会亮起来。
4. **改代码**：按下面的映射表定位。
5. **验证**：`./gradlew assembleDebug` + 真机装一遍，别只看编译过没过。
6. **提交**：设计快照和 Kotlin 改动放在同一个 commit 里，
   这样以后能回答「这次 UI 变更对应设计稿的哪一版」。

## 映射表

| 设计稿里改了什么 | 改这里 |
|---|---|
| 色值（`#RRGGBB` / `rgba(...)`） | `app/src/main/java/com/moodiary/app/ui/theme/Color.kt` |
| 字号 / 字重 / 行高 | `ui/theme/Type.kt` |
| 圆角、卡片、胶囊、虚线框 | `ui/components/Common.kt` |
| 底部栏、悬浮按钮 | `ui/components/BottomBar.kt` |
| 时间线卡片结构 | `ui/components/EntryCard.kt` |
| 照片排布规则 | `ui/components/Photo.kt` |
| 新图标（新的 `<svg>` path） | 新建 `res/drawable/ic_*.xml` |
| 界面文案 | `res/values/strings.xml` |
| 01 时间线 | `ui/screens/TimelineScreen.kt` |
| 02 发布 | `ui/screens/EditorScreen.kt` |
| 03 日历 | `ui/screens/CalendarScreen.kt` |
| 04 搜索 | `ui/screens/SearchScreen.kt` |
| 05 洞察 | `ui/screens/InsightsScreen.kt` |
| 06 我的 | `ui/screens/ProfileScreen.kt` |
| 示例日记内容 | `data/SeedData.kt` |

## 几件需要知道的事

**没有变更通知。** `get_project` 对这个画布项目只返回 name/type/canEdit，
不给 `updatedAt`；`list_projects` 只列设计系统类型的项目，画布不在其中。
所以没法自动探测「设计稿改没改」——要么你说一声，要么定期拉一次比对。

**设计侧关联的是另一个仓库。** `github.md` 里写的是
`ulichirock-cmyk/moodiary`（develop 分支），不是本仓库
`moo-diary-android`。如果哪天在 Claude Design 那边用「同步到仓库」，
它会去找错的 repo。

**`support.js` 没有快照。** 那是画布在浏览器里渲染 `.dc.html` 的运行时框架，
不含任何设计内容，跟 Android 实现无关。

**`ios-frame.jsx` 会自己变。** 文件头注明它是 omelette starter 的拷贝，
上游更新时会被整体覆盖。也就是说它的 diff 有可能跟设计改动无关——
看到它变了先别急着改 Android 代码，那是设备边框，本来就没移植
（我们走的是 edge-to-edge + 真实系统 inset）。

**快照怎么来的。** 首版是照着 MCP 返回誊写的，之后改用上面的落盘提取，逐字节可信。
如果某次 diff 出现某行「变了但看不出是什么设计改动」，那是早期誊写的残留，
以新拉的为准修掉即可。

## 变更记录

| 日期 | 设计侧变化 | 落到代码 |
|---|---|---|
| 2026-09-04 | 首版 6 屏 | 全量实现 |
| 2026-09-04 | 心情体系整体移除、改为地点；新增 07–12 六屏；卡片间距与图片高度调整；底栏改不透明 | 见 `feat : 同步设计稿改版` 系列提交 |
