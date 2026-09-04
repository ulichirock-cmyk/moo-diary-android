# moodiary — Android

Kotlin + Jetpack Compose implementation of **《Moodiary 设计稿》**
(Claude Design project `b270202f-0b14-42df-9cae-e15fa11b1aea`).

A private photo-and-text diary: no follows, no likes, no comments. Six screens,
one warm-paper palette, and a Markdown export that is the same shape Claude reads
over MCP.

## Screens

| # | Screen | File |
|---|---|---|
| 01 | 时间线 — the daily feed | `ui/screens/TimelineScreen.kt` |
| 02 | 发布 / 编辑 — write, attach, place, tag | `ui/screens/EditorScreen.kt` |
| 03 | 日历 — month grid marking days with entries | `ui/screens/CalendarScreen.kt` |
| 04 | 搜索 — full text + tags | `ui/screens/SearchScreen.kt` |
| 05 | 洞察 — Claude's weekly review | `ui/screens/InsightsScreen.kt` |
| 06 | 我的 — stats, export, reminder, lock, update | `ui/screens/ProfileScreen.kt` |
| 07 | 日记详情 — one entry in full | `ui/screens/DetailScreen.kt` |
| 08 | 更多操作 — edit / export / delete sheet | `ui/screens/DetailScreen.kt` |
| 09 | 删除确认 | `ui/screens/DetailScreen.kt` |
| 10 | 地点选择 — nearby, frequent, custom | `ui/screens/PlacePickerScreen.kt` |
| 11 | 地图选点 | `ui/screens/MapPickerScreen.kt` |
| 12 | 版本更新 | `ui/screens/UpdateScreen.kt` |

Moods were part of the first revision of the design and were removed wholesale in
the second, in favour of places. There is no `Mood` type left in the codebase.

## Build

```
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17 and an Android SDK with `platforms;android-35` +
`build-tools;35.0.0`. Point `local.properties` at your SDK:

```
sdk.dir=/path/to/android-sdk
```

`minSdk 26`, `compileSdk 35`, AGP 8.7.2, Kotlin 2.0.21, Compose BOM 2024.10.01.

## Where the design lives in the code

- **Colours** — `ui/theme/Color.kt`, one constant per hex in the design canvas.
- **Type scale** — `ui/theme/Type.kt`. The design pairs Inter Tight with
  Source Serif 4 / Noto Serif SC; we map those onto `FontFamily.SansSerif` and
  `FontFamily.Serif` so the app ships with no font download and keeps CJK
  coverage. Drop real `.ttf` files into `res/font/` and change the two values in
  `MoodiaryFonts` to match the design exactly.
- **Icons** — every stroke icon in the design is a hand-written vector drawable in
  `res/drawable/`; `ic_claude_mark.xml` is a port of `assets/claude-mark.svg`.
- **Shapes** — card radius 14, row radius 12, image radius 10, pills fully
  rounded (`ui/components/Common.kt`).

## Working on this repo

Read `CLAUDE.md` first. The short version: **the UI is an implementation of a
design, not a free hand.** Check `design/` before changing anything visual, and
follow `design/SYNC.md` when the design updates.

## Design source snapshot

`design/` holds a snapshot of the Claude Design canvas project this app was built
from, plus `design/SYNC.md` — the project id, the sync steps, and a design-region →
Kotlin-file mapping table. It does not participate in the build. When the design
changes, overwrite the snapshot with the new pull and `git diff design/` shows
exactly what moved.

## Data

`InMemoryDiaryRepository` seeds 15 entries reproducing the sample diary in the
design, anchored to *today* rather than to September 2026 so the timeline,
calendar and charts stay alive whenever the app is opened. Entries added in the
app live for the process lifetime.

`DiaryRepository` is an interface for exactly this reason — swapping in Room (or
a Markdown-file-backed store) touches one class and nothing else.

## Deliberate departures from the static design

- **iOS chrome dropped.** The design wraps every screen in an iPhone frame
  (`ios-frame.jsx`): fake status bar, dynamic island, home indicator. Those are
  the mockup's device, not the app — the Android build goes edge-to-edge and uses
  real system insets.
- **Search needs a way in.** Screen 04 has no entry point in the design, so the
  timeline header carries a search icon.
- **Calendar can move.** The design shows one fixed month; the app adds ‹ ›
  stepping and defaults to the current month.
- **The editor opens empty.** The design shows a draft mid-composition; a real
  editor starts blank. The draft is held in the view model, so leaving and coming
  back restores it — which is what "草稿已自动保存" promises.
- **Counters are computed.** 216 / 12 / 483 in the design are illustrative; the
  app counts real entries. The seed data is shaped so the streak really is 12.
- **"Face ID" → "生物识别".** Both that row and 每日提醒 are display-only for now.
- **Text-only entries still render.** The design's subtitle now says "单图与多图两种"
  and it cut the text-only sample card, but a diary cannot require a photo — the card
  handles an empty photo list and `canPublish` accepts text alone.
- **The map is hand-drawn, not an SDK.** The design labels screen 11's map area
  "系统地图接管渲染"; `ui/map/` renders OpenStreetMap raster tiles directly in a
  Compose `Canvas` — Web Mercator, pan, pinch and double-tap zoom, a three-level
  parent-tile fallback while a zoom loads, and a small disk cache. No API key and no
  map dependency. 高德 / 百度 tiles were the alternative and were not taken: they are
  GCJ-02, so pairing them with the platform's WGS-84 fixes would put the pin a few
  hundred metres off in China. Tiles carry the required attribution and a real
  User-Agent — OSM answers a generic one with a "please identify yourself" image.
- **Location is optional.** Screen 11 opens on the device position when the
  permission is already there, asks only when 回到当前位置 is tapped, and otherwise
  opens on a default centre with the hint line saying so. Built on the platform
  `LocationManager`, deliberately not on `play-services-location`, which would fail
  exactly on the phones this app targets.
- **Two things are still stubbed behind interfaces**, because each needs a key or an
  endpoint this project does not have. All the UI around them is real:
  - `PlaceSource.nearby()` — the 附近 list is POI search with distances, which needs
    a 高德 / 百度 account. `StubPlaceSource` returns the design's list.
    `PlaceSource.atPin()` is *not* a stub: it reverse-geocodes through the platform
    `Geocoder`, and falls back to the coordinates when a device has no geocoder.
  - `UpdateChecker` — there is no release feed yet; `StubUpdateChecker` returns the
    design's sample release.
