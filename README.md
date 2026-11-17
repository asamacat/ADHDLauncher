# ADHD Launcher

**Anti-Dopamine Human-factor Drawer Launcher for Android** *(Backronym customizable in Settings)*

[![Android CI](https://img.shields.io/badge/Android_CI-Gradle_9_%2F_JDK_17-3DDC84?logo=android)](.github/workflows/android-ci.yml)
[![Min SDK](https://img.shields.io/badge/API-26%2B-blue)](app/build.gradle.kts)
[![Target SDK](https://img.shields.io/badge/Target_SDK-36-brightgreen)](app/build.gradle.kts)
[![Version](https://img.shields.io/badge/Version-v0.3.7-94A88B)](https://github.com/asamacat/ADHDLauncher/releases)
[![License](https://img.shields.io/badge/License-Apache_2.0-orange.svg)](LICENSE)

> **日本語概要**: ADHD傾向やスマホ依存（「無意識にSNSやショート動画を開いてしまうドーパミンループ」）を防ぐために設計された、完全無料・完全ローカルプライバシー・OLED省電力のミニマルAndroidホームアプリです。
> 詳細な開発計画は [`ROADMAP.md`](ROADMAP.md) を参照してください。

---

## Core Philosophy / 基本思想

1. **Zero Home Clutter (視覚トリガーの排除)**: ホーム画面は時計、先まわりコンテキスト情報（At a Glance もどき）、そして 1行の **目的アンカー（今スマホを開いた目的メモ / ON-OFF切替可）** のみ。通知バッジや原色アイコンによる衝動タップを防ぎます。
2. **Ergonomic Bottom-Priority Drawer (`Swipe Up`)**: 親指の届く画面右下（左利き時は左下）から利用頻度順にアプリを自動配列。
3. **Instant Phonetic & Romaji Pull-Down Search (`Swipe Down`)**: ひらがな・カタカナ・ローマ字（促音/撥音/拗音/長音符対応）で即座に絞り込み検索。
4. **OLED True Black Power Saving (`#000000`) & Tunable Controls**: ダブルタップ切替やユーザー可変の時間帯スケジュール・無操作フェード（`idleDimDelaySec` / `idleDimAlphaPercent`）で視覚刺激と消費電力を抑制。
5. **100% On-Device Privacy (Zero-Tracking)**: 行動履歴や設定は端末外に一切送信しません（クラウドバックアップおよび端末自動移行対象からも除外済み）。
6. **In-App Auto-Update & Envelope v2 Local Migration**: 固定署名 APK による競合のないワンタップ更新と、設定＋学習ログをまるごと端末移動できる SAF/共有/クリップボードバックアップに対応。

---

## On-Device Usage Learning Logic & Specifications / 利用状況学習ロジックと仕様

ADHD Launcher does not use cloud analytics. All behavioral learning runs strictly on-device inside private preferences (`adhd_app_usage.xml`, protected by `backup_rules.xml` and `data_extraction_rules.xml` so raw usage logs never leak to cloud backups). Users can explicitly include or exclude these learned habits when creating a local Envelope v2 JSON backup for device migration.

利用状況の学習はすべて **端末内のみ（`adhd_app_usage.xml`）** で完結し、自動クラウドバックアップからは厳格に除外されています（機種変更時はバックアップ画面のチェックボックスで明示的に移行可能）。

### 1. 12 Time-Context Buckets / 12個の時間帯・曜日バケット構造
アプリが起動されるたびに、端末の現在曜日と時刻から以下のバケットキーへ起動カウントを加算します（キー形式: `ctx_<dayType>_h<bucket>_<packageName>`）：

| 次元 | キー値 | 範囲・仕様 |
|---|---|---|
| **曜日種別 (`dayType`)** | `wkdy` / `wknd` | `wkdy` = 平日（月〜金）、`wknd` = 週末（土・日） |
| **時間帯 (`hourBucket`)** | `h0` 〜 `h5` | 24時間を 4 時間刻みで 6 分割：<br>`h0`: 00–03時 / `h1`: 04–07時 / `h2`: 08–11時 / `h3`: 12–15時 / `h4`: 16–19時 / `h5`: 20–23時 |

> *(v0.3.2 にて、新規バケット初回起動時の初期値は必ず `0` からカウントするよう補正し、データの指数増殖バグを根治しています)*

### 2. 3-Bucket Temporal Smoothing Formula / 時間帯境界の 3バケット平滑化式
「7:55 から 8:05 になった瞬間に朝の定番アプリが候補から消える」現象を防ぐため、現在のバケットに前後 1 バケットを加重加算して `contextScore` を算出します：

$$\text{contextScore} = 3 \times \text{count}(h_{\text{now}}) + 1 \times \text{count}(h_{\text{prev}}) + 1 \times \text{count}(h_{\text{next}})$$

### 3. Hybrid Ranking Priority / ランキング優先順位の仕様
OS 標準の `UsageStatsManager`（直近 30 日間のフォアグラウンド利用統計: `systemUsageScore`）も組み合わせ、以下のルールで優先度を決定します：

---

## Key Smart Features Specification / 主要機能の仕様詳細

### ① ホーム画面タップ：扇形ハニカム展開（Tap Fan-Arc Honeycomb）
ホーム画面の空白領域をタップすると、**指が触れた座標 `(tapX, tapY)` を要（ピボット）として六角形アイコンが扇形（Fan Arc）に展開** されます。

- **ハイブリッド固定スロット（筋肉記憶 × 動的推薦）**:
  - **固定アプリ枠（最優先・最大 4 個）**: 設定画面またはドロワーの長押しメニューで固定したアプリ（カメラ・メモなど）は、使用回数に関係なく常に **内側アークの 1〜4 番目** に配置されます（「いつ開いても同じ場所にある」安心感を確保）。
  - **動的推薦枠（残りのスロット〜最大 12 個、推奨 7 個）**: 非表示アプリを除外したうえで、以下の優先順で自動選抜されます：
    1. `contextScore * 2 + launchCount`（降順：今の時間帯の習慣を 2 倍重視 ＋ 通算起動数）
    2. `contextScore`（降順）
    3. `lastLaunchTime`（降順：直近開いたアプリ）
    4. `systemUsageScore`（降順：OS 利用統計）
    5. `label.lowercase()`（昇順：名前の辞書順）
- **エルゴノミクス自動展開**:
  - タップ座標が画面の右・中央・左・上下のどこにあるかに応じて、アイコンが画面外へはみ出ない方向（内側）へ自動的に扇角度を向き直します。
  - 右利きモード時は 1 位が親指に最も近い端へ配置されます。
  - 内側アーク半径（`80〜140dp` / 標準 `108dp`）および外側アーク半径（`140〜230dp` / 標準 `184dp`）は設定画面から調整可能です。

### ② At a Glance もどき：先まわりコンテキスト表示（Proactive At-a-Glance Bar）
Google Pixel の At a Glance に着想を得て、**外部通信やカレンダー等の高リスク権限を一切要求せず、OS 標準情報と上記の端末内 12 バケット学習のみで動作する低刺激ヘッダ** を時計の下に表示します（`設定 → ホーム画面` で ON/OFF 可能）。

- **表示されるコンテキスト情報（1行テキスト）**:
  - **時間帯コンテキスト**: 現在時刻に応じた静かな時間帯ラベル（`深夜` / `早朝・朝` / `午前` / `午後` / `夕方` / `夜`）
  - **次回アラーム時刻**: `AlarmManager.nextAlarmClock` から取得し、設定されている次のアラーム時刻（`⏰ HH:mm`）を静かに表示（時間感覚が飛びやすい ADHD 特性を支援）
  - **バッテリー注意・充電状態**: `ACTION_BATTERY_CHANGED` から読み取り、充電中（`⚡ 充電中 XX%`）または残量 25% 以下の低下時（`🔋 残量 XX%`）のみ控えめに通知
- **先まわりアプリ提案チップ（`この時間帯の定番: <App>`）**:
  - 12 バケット学習（＋固定枠）から算出された「今この曜日のこの時間帯に一番使われるアプリ」1 件のみを低刺激なピルで提示し、タップ 1 つでドロワーや扇を開かずに即起動できます。

---

## Gesture Map & Customization / ジェスチャーとカスタマイズ一覧

| 操作 / 箇所 | 動作・仕様 | 設定可能項目（すべてユーザー可変） |
|---|---|---|
| **Home Tap** | 指の座標中心に扇形ハニカム展開 | ON/OFF、個数 (`3–12`)、内外半径 (`dp`)、固定アプリ（最大4個・カメラ等） |
| **Swipe Up** | 片手親指優先ドロワーを開く | 有効/無効、スワイプ閾値 (`16–72dp`)、列数、下段優先、カテゴリタブ管理 |
| **Swipe Down** | 日本語/ローマ字対応クイック検索 | 有効/無効、候補アプリ数、検索エンジン、Playストア連携 |
| **Double Tap** | 壁紙 ⇔ True Black (`#000000`) 切替 | 有効/無効、夜間スケジュール開始/終了時刻（各 `0–23時`） |
| **Swipe Right** | フィード（Googleアプリ等）を開く | Google アプリ連携 / オフ |
| **Widget Host** | ホーム画面ウィジェット（KWGT/時計等） | 配置（上/中/下）、横揃え（左/中/右）、幅%・高さdp・上下オフセット・背景透過・角丸・余白・内部スクロール優先 |
| **Icon Style** | 一括 Material You 変換 | 最上段「オリジナル（標準・カラー）」＋ 6 色トーン（モノクロ/セージ/シアン/ラベンダー/アンバー/ローズ） |

---

## Contributing Localized App Dictionaries (No Kotlin Required!)

We welcome Pull Requests from users around the world! To add regional apps or localized search keywords:

1. Edit [`app/src/main/assets/dictionaries/app_dictionary.json`](app/src/main/assets/dictionaries/app_dictionary.json).
2. Add package names to `package_rules`, localized category substrings to `category_keywords`, or phonetic/romanized readings to `search_aliases`.
3. Open a Pull Request — GitHub Actions will automatically validate the JSON schema, run unit tests (`./gradlew testDebugUnitTest`), build the APK, and run Lint (`lintDebug`).

See [`CONTRIBUTING.md`](CONTRIBUTING.md) and [`app/src/main/assets/dictionaries/README.md`](app/src/main/assets/dictionaries/README.md) for details.

---

## Building from Source

### Prerequisites
- **JDK 17** or newer
- **Android SDK Platform 36**

### Command Line Build & Test
```bash
./gradlew testDebugUnitTest assembleRelease lintDebug
```
The signed release APK will be generated at `app/build/outputs/apk/release/app-release.apk`.

---

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
