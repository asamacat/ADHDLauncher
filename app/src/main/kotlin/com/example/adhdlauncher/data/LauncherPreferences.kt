package com.example.adhdlauncher.data

import android.content.Context
import android.content.SharedPreferences
import com.example.adhdlauncher.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class BackupRestoreResult(
    val success: Boolean,
    val prefsCount: Int,
    val usageCount: Int,
    val hasLearningData: Boolean
)

enum class DisplayMode(val label: String, val description: String) {
    AUTO_SCHEDULE("時間帯スケジュール", "22:00〜7:00 は有機ELの画素を消灯する完全黒モードへ自動切替"),
    WALLPAPER("常時壁紙を表示", "設定した壁紙を常に透過表示"),
    TRUE_BLACK("常時 True Black", "常時完全黒（#000000）で省電力・視覚刺激を最小化")
}

enum class Handedness(val label: String, val description: String) {
    RIGHT("右利き", "親指の届きやすい画面右下から優先してアプリを配置"),
    LEFT("左利き", "親指の届きやすい画面左下から優先してアプリを配置")
}

enum class ClockPosition(val label: String, val description: String) {
    TOP_LARGE("画面上部に大きく表示（標準）", "視認性に優れた大型ミニマル時計"),
    BOTTOM_COMPACT("画面下部に寄せて表示", "目線や親指の近くに時間と日付を配置"),
    HIDDEN("非表示（ウィジェット優先）", "内蔵時計を隠し、KWGT等の外部ウィジェットのみを表示")
}

enum class DrawerSortOrder(val label: String) {
    USAGE_FREQUENCY("利用頻度順（よく使うアプリ優先）"),
    ALPHABETICAL("名前順（50音・ABC順）")
}

enum class SearchProvider(val label: String, val queryUrlPrefix: String) {
    GOOGLE("Google", "https://www.google.com/search?q="),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q="),
    YAHOO_JP("Yahoo! JAPAN", "https://search.yahoo.co.jp/search?p=")
}

enum class AiProviderType(val label: String, val defaultEndpoint: String, val defaultModel: String) {
    GEMINI("Google Gemini API", "https://generativelanguage.googleapis.com/v1beta", "gemini-2.5-flash"),
    OPENAI_COMPATIBLE("OpenAI互換 / Jev API", "https://api.openai.com/v1", "gpt-4o-mini")
}

enum class DiscoverMode(val label: String, val description: String) {
    GOOGLE_APP("Google Discover（Googleアプリ連携）", "ホーム画面の右スワイプでGoogleアプリのフィードを起動"),
    AI_MINIMAL_FEED("ADHD低刺激フィード（準備中）", "広告やゴシップを排除した黒背景の要約フィード"),
    OFF("オフ", "ホーム画面の横スワイプフィードを無効化")
}

enum class IconStyle(
    val label: String,
    val description: String,
    val bgHex: Int,
    val fgHex: Int
) {
    MONOCHROME(
        "Material You（モノクローム・標準）",
        "全アプリを完全な白黒・グレー単色に統一。鮮烈な色彩による衝動的タップやドーパミン過剰刺激を最小化します",
        0x1E1F22,
        0xE2E4E8
    ),
    MATERIAL_SAGE(
        "Material You（セージグリーン）",
        "穏やかなセージグリーン単色アイコンに統一変換。視覚的な緊張をほぐし、落ち着いた集中状態を保ちます",
        0x232B22,
        0xB8CCAE
    ),
    MATERIAL_CYAN(
        "Material You（シアンブルー）",
        "知的なシアンブルー単色アイコンに統一変換。頭を冷静にクールダウンし、衝動的な情報過多を防ぎます",
        0x182430,
        0x90CAF9
    ),
    MATERIAL_LAVENDER(
        "Material You（ラベンダー）",
        "やわらかなラベンダーパープル単色アイコンに統一変換。不安感やタスク切り替え時の焦りを和らげます",
        0x252030,
        0xCEB8F5
    ),
    MATERIAL_AMBER(
        "Material You（ウォームアンバー）",
        "夕暮れや夜間でも目に優しい暖色アンバー単色。ブルーライトと視覚刺激を抑えて休息を促します",
        0x2E2418,
        0xF5CBA7
    ),
    MATERIAL_ROSE(
        "Material You（ダスティローズ）",
        "彩度をグッと抑えたダスティローズ単色。過度な刺激を排した温かみのある画面を作ります",
        0x2E1E22,
        0xF2B8C6
    ),
    ORIGINAL(
        "オリジナル（無変換）",
        "各アプリ本来のカラフルなアイコンをそのまま表示（注意散漫になりやすい方は単色スタイルを推奨）",
        0x000000,
        0xFFFFFF
    )
}

class LauncherPreferences(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(
        context.getSharedPreferences("adhd_launcher_prefs", Context.MODE_PRIVATE)
    )

    private val _anchorText = MutableStateFlow(prefs.getString(KEY_ANCHOR_TEXT, "") ?: "")
    val anchorText: StateFlow<String> = _anchorText.asStateFlow()

    private val _displayMode = MutableStateFlow(
        runCatching {
            DisplayMode.valueOf(prefs.getString(KEY_DISPLAY_MODE, DisplayMode.AUTO_SCHEDULE.name) ?: DisplayMode.AUTO_SCHEDULE.name)
        }.getOrDefault(DisplayMode.AUTO_SCHEDULE)
    )
    val displayMode: StateFlow<DisplayMode> = _displayMode.asStateFlow()

    private val _handedness = MutableStateFlow(
        runCatching {
            Handedness.valueOf(prefs.getString(KEY_HANDEDNESS, Handedness.RIGHT.name) ?: Handedness.RIGHT.name)
        }.getOrDefault(Handedness.RIGHT)
    )
    val handedness: StateFlow<Handedness> = _handedness.asStateFlow()

    private val _clockPosition = MutableStateFlow(
        runCatching {
            ClockPosition.valueOf(prefs.getString(KEY_CLOCK_POSITION, ClockPosition.TOP_LARGE.name) ?: ClockPosition.TOP_LARGE.name)
        }.getOrDefault(ClockPosition.TOP_LARGE)
    )
    val clockPosition: StateFlow<ClockPosition> = _clockPosition.asStateFlow()

    private val _autoOpenKeyboard = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_OPEN_KEYBOARD, false)
    )
    val autoOpenKeyboard: StateFlow<Boolean> = _autoOpenKeyboard.asStateFlow()

    private val _showCategoryBar = MutableStateFlow(
        prefs.getBoolean(KEY_SHOW_CATEGORY_BAR, true)
    )
    val showCategoryBar: StateFlow<Boolean> = _showCategoryBar.asStateFlow()

    private val _moveIconsDown = MutableStateFlow(
        prefs.getBoolean(KEY_MOVE_ICONS_DOWN, true)
    )
    val moveIconsDown: StateFlow<Boolean> = _moveIconsDown.asStateFlow()

    private val _stackFromBottom = MutableStateFlow(
        prefs.getBoolean(KEY_STACK_FROM_BOTTOM, true)
    )
    val stackFromBottom: StateFlow<Boolean> = _stackFromBottom.asStateFlow()

    private val _closeDrawerOnLaunch = MutableStateFlow(
        prefs.getBoolean(KEY_CLOSE_DRAWER_ON_LAUNCH, true)
    )
    val closeDrawerOnLaunch: StateFlow<Boolean> = _closeDrawerOnLaunch.asStateFlow()

    private val _drawerSortOrder = MutableStateFlow(
        runCatching {
            DrawerSortOrder.valueOf(
                prefs.getString(KEY_DRAWER_SORT_ORDER, DrawerSortOrder.USAGE_FREQUENCY.name)
                    ?: DrawerSortOrder.USAGE_FREQUENCY.name
            )
        }.getOrDefault(DrawerSortOrder.USAGE_FREQUENCY)
    )
    val drawerSortOrder: StateFlow<DrawerSortOrder> = _drawerSortOrder.asStateFlow()

    private val _drawerColumns = MutableStateFlow(
        prefs.getInt(KEY_DRAWER_COLUMNS, 4)
    )
    val drawerColumns: StateFlow<Int> = _drawerColumns.asStateFlow()

    // --- 検索ページ設定（下スワイプ検索画面） ---
    private val _searchProvider = MutableStateFlow(
        runCatching {
            SearchProvider.valueOf(
                prefs.getString(KEY_SEARCH_PROVIDER, SearchProvider.GOOGLE.name)
                    ?: SearchProvider.GOOGLE.name
            )
        }.getOrDefault(SearchProvider.GOOGLE)
    )
    val searchProvider: StateFlow<SearchProvider> = _searchProvider.asStateFlow()

    private val _searchIncludeHiddenApps = MutableStateFlow(
        prefs.getBoolean(KEY_SEARCH_INCLUDE_HIDDEN, false)
    )
    val searchIncludeHiddenApps: StateFlow<Boolean> = _searchIncludeHiddenApps.asStateFlow()

    private val _searchStartAppsCount = MutableStateFlow(
        prefs.getInt(KEY_SEARCH_START_APPS_COUNT, 4)
    )
    val searchStartAppsCount: StateFlow<Int> = _searchStartAppsCount.asStateFlow()

    private val _searchWebSuggest = MutableStateFlow(
        prefs.getBoolean(KEY_SEARCH_WEB_SUGGEST, true)
    )
    val searchWebSuggest: StateFlow<Boolean> = _searchWebSuggest.asStateFlow()

    private val _searchPlayStore = MutableStateFlow(
        prefs.getBoolean(KEY_SEARCH_PLAY_STORE, true)
    )
    val searchPlayStore: StateFlow<Boolean> = _searchPlayStore.asStateFlow()

    // --- ジェスチャー個別 ON/OFF 設定 ---
    private val _gestureSwipeUpDrawer = MutableStateFlow(
        prefs.getBoolean(KEY_GESTURE_SWIPE_UP_DRAWER, true)
    )
    val gestureSwipeUpDrawer: StateFlow<Boolean> = _gestureSwipeUpDrawer.asStateFlow()

    private val _gestureSwipeDownSearch = MutableStateFlow(
        prefs.getBoolean(KEY_GESTURE_SWIPE_DOWN_SEARCH, true)
    )
    val gestureSwipeDownSearch: StateFlow<Boolean> = _gestureSwipeDownSearch.asStateFlow()

    private val _gestureSwipeDownCloseDrawer = MutableStateFlow(
        prefs.getBoolean(KEY_GESTURE_SWIPE_DOWN_CLOSE_DRAWER, true)
    )
    val gestureSwipeDownCloseDrawer: StateFlow<Boolean> = _gestureSwipeDownCloseDrawer.asStateFlow()

    private val _gestureDoubleTapTrueBlack = MutableStateFlow(
        prefs.getBoolean(KEY_GESTURE_DOUBLE_TAP_TRUE_BLACK, true)
    )
    val gestureDoubleTapTrueBlack: StateFlow<Boolean> = _gestureDoubleTapTrueBlack.asStateFlow()

    // --- Google Discover & AI API 連携（設定枠） ---
    private val _discoverMode = MutableStateFlow(
        runCatching {
            DiscoverMode.valueOf(
                prefs.getString(KEY_DISCOVER_MODE, DiscoverMode.GOOGLE_APP.name)
                    ?: DiscoverMode.GOOGLE_APP.name
            )
        }.getOrDefault(DiscoverMode.GOOGLE_APP)
    )
    val discoverMode: StateFlow<DiscoverMode> = _discoverMode.asStateFlow()

    private val _aiProviderType = MutableStateFlow(
        runCatching {
            AiProviderType.valueOf(
                prefs.getString(KEY_AI_PROVIDER_TYPE, AiProviderType.GEMINI.name)
                    ?: AiProviderType.GEMINI.name
            )
        }.getOrDefault(AiProviderType.GEMINI)
    )
    val aiProviderType: StateFlow<AiProviderType> = _aiProviderType.asStateFlow()

    private val _aiApiKey = MutableStateFlow(prefs.getString(KEY_AI_API_KEY, "") ?: "")
    val aiApiKey: StateFlow<String> = _aiApiKey.asStateFlow()

    private val _aiBaseUrl = MutableStateFlow(
        prefs.getString(KEY_AI_BASE_URL, AiProviderType.GEMINI.defaultEndpoint)
            ?: AiProviderType.GEMINI.defaultEndpoint
    )
    val aiBaseUrl: StateFlow<String> = _aiBaseUrl.asStateFlow()

    private val _aiModelName = MutableStateFlow(
        prefs.getString(KEY_AI_MODEL_NAME, AiProviderType.GEMINI.defaultModel)
            ?: AiProviderType.GEMINI.defaultModel
    )
    val aiModelName: StateFlow<String> = _aiModelName.asStateFlow()

    private val _aiAutoCategorizeEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AI_AUTO_CATEGORIZE, true)
    )
    val aiAutoCategorizeEnabled: StateFlow<Boolean> = _aiAutoCategorizeEnabled.asStateFlow()

    private val _aiQuickAnswerEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AI_QUICK_ANSWER, true)
    )
    val aiQuickAnswerEnabled: StateFlow<Boolean> = _aiQuickAnswerEnabled.asStateFlow()

    // --- カテゴリ管理（カスタム名称・表示ON/OFF・カスタム順序） ---
    private val _categoryCustomNames = MutableStateFlow(loadCategoryCustomNames())
    val categoryCustomNames: StateFlow<Map<AppCategory, String>> = _categoryCustomNames.asStateFlow()

    private val _hiddenCategories = MutableStateFlow(loadHiddenCategories())
    val hiddenCategories: StateFlow<Set<AppCategory>> = _hiddenCategories.asStateFlow()

    private val _categoryOrder = MutableStateFlow(loadCategoryOrder())
    val categoryOrder: StateFlow<List<AppCategory>> = _categoryOrder.asStateFlow()

    // --- 動的ハニカム（画面タップ展開）& Material You アイコン変換 & ウィジェット・自動ディム ---
    private val _honeycombEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_HONEYCOMB_ENABLED, true)
    )
    val honeycombEnabled: StateFlow<Boolean> = _honeycombEnabled.asStateFlow()

    private val _honeycombAppCount = MutableStateFlow(
        prefs.getInt(KEY_HONEYCOMB_APP_COUNT, 7).coerceIn(3, 12)
    )
    val honeycombAppCount: StateFlow<Int> = _honeycombAppCount.asStateFlow()

    private val _iconStyle = MutableStateFlow(
        runCatching {
            IconStyle.valueOf(
                prefs.getString(KEY_ICON_STYLE, IconStyle.MONOCHROME.name)
                    ?: IconStyle.MONOCHROME.name
            )
        }.getOrDefault(IconStyle.MONOCHROME)
    )
    val iconStyle: StateFlow<IconStyle> = _iconStyle.asStateFlow()

    private val _homeWidgetId = MutableStateFlow(
        prefs.getInt(KEY_HOME_WIDGET_ID, -1)
    )
    val homeWidgetId: StateFlow<Int> = _homeWidgetId.asStateFlow()

    private val _homeWidgetHeightDp = MutableStateFlow(
        prefs.getInt(KEY_HOME_WIDGET_HEIGHT_DP, 160).coerceIn(80, 420)
    )
    val homeWidgetHeightDp: StateFlow<Int> = _homeWidgetHeightDp.asStateFlow()

    private val _idleDimEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_IDLE_DIM_ENABLED, true)
    )
    val idleDimEnabled: StateFlow<Boolean> = _idleDimEnabled.asStateFlow()

    private fun loadCategoryCustomNames(): Map<AppCategory, String> {
        val map = mutableMapOf<AppCategory, String>()
        for (cat in AppCategory.values()) {
            val saved = prefs.getString("cat_label_${cat.name}", null)
            if (!saved.isNullOrBlank()) {
                map[cat] = saved
            }
        }
        return map
    }

    private fun loadHiddenCategories(): Set<AppCategory> {
        val raw = prefs.getStringSet(KEY_HIDDEN_CATEGORIES, emptySet()) ?: emptySet()
        return raw.mapNotNull { name ->
            runCatching { AppCategory.valueOf(name) }.getOrNull()
        }.filter { it != AppCategory.ALL }.toSet()
    }

    private fun loadCategoryOrder(): List<AppCategory> {
        val raw = prefs.getString(KEY_CATEGORY_ORDER, null)
        if (raw.isNullOrBlank()) return AppCategory.values().toList()
        val parsed = raw.split(",")
            .mapNotNull { runCatching { AppCategory.valueOf(it.trim()) }.getOrNull() }
            .distinct()
        val missing = AppCategory.values().filter { it !in parsed }
        val full = parsed + missing
        // ALL は常に先頭に固定
        return listOf(AppCategory.ALL) + full.filter { it != AppCategory.ALL }
    }

    fun getCategoryLabel(category: AppCategory): String {
        return _categoryCustomNames.value[category] ?: category.displayName
    }

    fun setCategoryCustomName(category: AppCategory, customName: String) {
        val trimmed = customName.trim()
        if (trimmed.isEmpty() || trimmed == category.displayName) {
            prefs.edit().remove("cat_label_${category.name}").apply()
        } else {
            prefs.edit().putString("cat_label_${category.name}", trimmed).apply()
        }
        _categoryCustomNames.value = loadCategoryCustomNames()
    }

    fun setCategoryHidden(category: AppCategory, hidden: Boolean) {
        if (category == AppCategory.ALL) return
        val current = _hiddenCategories.value.toMutableSet()
        if (hidden) current.add(category) else current.remove(category)
        prefs.edit().putStringSet(KEY_HIDDEN_CATEGORIES, current.map { it.name }.toSet()).apply()
        _hiddenCategories.value = current
    }

    fun moveCategoryOrder(category: AppCategory, moveUp: Boolean) {
        if (category == AppCategory.ALL) return
        val nonAll = _categoryOrder.value.filter { it != AppCategory.ALL }.toMutableList()
        val idx = nonAll.indexOf(category)
        if (idx == -1) return
        val targetIdx = if (moveUp) idx - 1 else idx + 1
        if (targetIdx !in nonAll.indices) return
        val tmp = nonAll[idx]
        nonAll[idx] = nonAll[targetIdx]
        nonAll[targetIdx] = tmp
        val updated = listOf(AppCategory.ALL) + nonAll
        prefs.edit().putString(KEY_CATEGORY_ORDER, updated.joinToString(",") { it.name }).apply()
        _categoryOrder.value = updated
    }

    fun resetCategoriesToDefault() {
        val editor = prefs.edit()
        for (cat in AppCategory.values()) {
            editor.remove("cat_label_${cat.name}")
        }
        editor.remove(KEY_HIDDEN_CATEGORIES)
        editor.remove(KEY_CATEGORY_ORDER)
        editor.apply()
        _categoryCustomNames.value = emptyMap()
        _hiddenCategories.value = emptySet()
        _categoryOrder.value = AppCategory.values().toList()
    }

    fun setHoneycombEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HONEYCOMB_ENABLED, enabled).apply()
        _honeycombEnabled.value = enabled
    }

    fun setHoneycombAppCount(count: Int) {
        val coerced = count.coerceIn(3, 12)
        prefs.edit().putInt(KEY_HONEYCOMB_APP_COUNT, coerced).apply()
        _honeycombAppCount.value = coerced
    }

    fun setIconStyle(style: IconStyle) {
        prefs.edit().putString(KEY_ICON_STYLE, style.name).apply()
        _iconStyle.value = style
    }

    fun setHomeWidgetId(appWidgetId: Int) {
        prefs.edit().putInt(KEY_HOME_WIDGET_ID, appWidgetId).apply()
        _homeWidgetId.value = appWidgetId
    }

    fun setHomeWidgetHeightDp(heightDp: Int) {
        val coerced = heightDp.coerceIn(80, 420)
        prefs.edit().putInt(KEY_HOME_WIDGET_HEIGHT_DP, coerced).apply()
        _homeWidgetHeightDp.value = coerced
    }

    fun setIdleDimEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IDLE_DIM_ENABLED, enabled).apply()
        _idleDimEnabled.value = enabled
    }

    fun updateAnchorText(text: String) {
        prefs.edit().putString(KEY_ANCHOR_TEXT, text).apply()
        _anchorText.value = text
    }

    fun clearAnchorText() {
        updateAnchorText("")
    }

    fun setDisplayMode(mode: DisplayMode) {
        prefs.edit().putString(KEY_DISPLAY_MODE, mode.name).apply()
        _displayMode.value = mode
    }

    fun setHandedness(handedness: Handedness) {
        prefs.edit().putString(KEY_HANDEDNESS, handedness.name).apply()
        _handedness.value = handedness
    }

    fun setClockPosition(position: ClockPosition) {
        prefs.edit().putString(KEY_CLOCK_POSITION, position.name).apply()
        _clockPosition.value = position
    }

    fun setAutoOpenKeyboard(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_OPEN_KEYBOARD, enabled).apply()
        _autoOpenKeyboard.value = enabled
    }

    fun setShowCategoryBar(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_CATEGORY_BAR, enabled).apply()
        _showCategoryBar.value = enabled
    }

    fun setMoveIconsDown(enabled: Boolean) {
        setStackFromBottom(enabled)
    }

    fun setStackFromBottom(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_STACK_FROM_BOTTOM, enabled)
            .putBoolean(KEY_MOVE_ICONS_DOWN, enabled)
            .apply()
        _stackFromBottom.value = enabled
        _moveIconsDown.value = enabled
    }

    fun setCloseDrawerOnLaunch(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLOSE_DRAWER_ON_LAUNCH, enabled).apply()
        _closeDrawerOnLaunch.value = enabled
    }

    fun setDrawerSortOrder(order: DrawerSortOrder) {
        prefs.edit().putString(KEY_DRAWER_SORT_ORDER, order.name).apply()
        _drawerSortOrder.value = order
    }

    fun setDrawerColumns(columns: Int) {
        prefs.edit().putInt(KEY_DRAWER_COLUMNS, columns).apply()
        _drawerColumns.value = columns
    }

    fun setSearchProvider(provider: SearchProvider) {
        prefs.edit().putString(KEY_SEARCH_PROVIDER, provider.name).apply()
        _searchProvider.value = provider
    }

    fun setSearchIncludeHiddenApps(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SEARCH_INCLUDE_HIDDEN, enabled).apply()
        _searchIncludeHiddenApps.value = enabled
    }

    fun setSearchStartAppsCount(count: Int) {
        val coerced = count.coerceIn(0, 8)
        prefs.edit().putInt(KEY_SEARCH_START_APPS_COUNT, coerced).apply()
        _searchStartAppsCount.value = coerced
    }

    fun setSearchWebSuggest(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SEARCH_WEB_SUGGEST, enabled).apply()
        _searchWebSuggest.value = enabled
    }

    fun setSearchPlayStore(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SEARCH_PLAY_STORE, enabled).apply()
        _searchPlayStore.value = enabled
    }

    fun setGestureSwipeUpDrawer(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GESTURE_SWIPE_UP_DRAWER, enabled).apply()
        _gestureSwipeUpDrawer.value = enabled
    }

    fun setGestureSwipeDownSearch(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GESTURE_SWIPE_DOWN_SEARCH, enabled).apply()
        _gestureSwipeDownSearch.value = enabled
    }

    fun setGestureSwipeDownCloseDrawer(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GESTURE_SWIPE_DOWN_CLOSE_DRAWER, enabled).apply()
        _gestureSwipeDownCloseDrawer.value = enabled
    }

    fun setGestureDoubleTapTrueBlack(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GESTURE_DOUBLE_TAP_TRUE_BLACK, enabled).apply()
        _gestureDoubleTapTrueBlack.value = enabled
    }

    fun setDiscoverMode(mode: DiscoverMode) {
        prefs.edit().putString(KEY_DISCOVER_MODE, mode.name).apply()
        _discoverMode.value = mode
    }

    fun setAiProviderType(type: AiProviderType) {
        prefs.edit()
            .putString(KEY_AI_PROVIDER_TYPE, type.name)
            .putString(KEY_AI_BASE_URL, type.defaultEndpoint)
            .putString(KEY_AI_MODEL_NAME, type.defaultModel)
            .apply()
        _aiProviderType.value = type
        _aiBaseUrl.value = type.defaultEndpoint
        _aiModelName.value = type.defaultModel
    }

    fun setAiApiKey(key: String) {
        prefs.edit().putString(KEY_AI_API_KEY, key.trim()).apply()
        _aiApiKey.value = key.trim()
    }

    fun setAiBaseUrl(url: String) {
        prefs.edit().putString(KEY_AI_BASE_URL, url.trim()).apply()
        _aiBaseUrl.value = url.trim()
    }

    fun setAiModelName(model: String) {
        prefs.edit().putString(KEY_AI_MODEL_NAME, model.trim()).apply()
        _aiModelName.value = model.trim()
    }

    fun setAiAutoCategorizeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AI_AUTO_CATEGORIZE, enabled).apply()
        _aiAutoCategorizeEnabled.value = enabled
    }

    fun setAiQuickAnswerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AI_QUICK_ANSWER, enabled).apply()
        _aiQuickAnswerEnabled.value = enabled
    }

    fun resetAllSettings() {
        prefs.edit().clear().apply()
        _anchorText.value = ""
        _displayMode.value = DisplayMode.AUTO_SCHEDULE
        _handedness.value = Handedness.RIGHT
        _clockPosition.value = ClockPosition.TOP_LARGE
        _autoOpenKeyboard.value = false
        _showCategoryBar.value = true
        _moveIconsDown.value = true
        _stackFromBottom.value = true
        _closeDrawerOnLaunch.value = true
        _drawerSortOrder.value = DrawerSortOrder.USAGE_FREQUENCY
        _drawerColumns.value = 4
        _searchProvider.value = SearchProvider.GOOGLE
        _searchIncludeHiddenApps.value = false
        _searchStartAppsCount.value = 4
        _searchWebSuggest.value = true
        _searchPlayStore.value = true
        _gestureSwipeUpDrawer.value = true
        _gestureSwipeDownSearch.value = true
        _gestureSwipeDownCloseDrawer.value = true
        _gestureDoubleTapTrueBlack.value = true
        _discoverMode.value = DiscoverMode.GOOGLE_APP
        _aiProviderType.value = AiProviderType.GEMINI
        _aiApiKey.value = ""
        _aiBaseUrl.value = AiProviderType.GEMINI.defaultEndpoint
        _aiModelName.value = AiProviderType.GEMINI.defaultModel
        _aiAutoCategorizeEnabled.value = true
        _aiQuickAnswerEnabled.value = true
        _categoryCustomNames.value = emptyMap()
        _hiddenCategories.value = emptySet()
        _categoryOrder.value = AppCategory.values().toList()
        _honeycombEnabled.value = true
        _honeycombAppCount.value = 7
        _iconStyle.value = IconStyle.MONOCHROME
        _homeWidgetId.value = -1
        _homeWidgetHeightDp.value = 160
        _idleDimEnabled.value = true
    }

    fun isTrueBlackActive(): Boolean {
        return when (_displayMode.value) {
            DisplayMode.TRUE_BLACK -> true
            DisplayMode.WALLPAPER -> false
            DisplayMode.AUTO_SCHEDULE -> {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                hour >= 22 || hour < 7
            }
        }
    }

    fun reloadFromPreferences() {
        _anchorText.value = prefs.getString(KEY_ANCHOR_TEXT, "") ?: ""
        _displayMode.value = runCatching {
            DisplayMode.valueOf(prefs.getString(KEY_DISPLAY_MODE, DisplayMode.AUTO_SCHEDULE.name) ?: DisplayMode.AUTO_SCHEDULE.name)
        }.getOrDefault(DisplayMode.AUTO_SCHEDULE)
        _handedness.value = runCatching {
            Handedness.valueOf(prefs.getString(KEY_HANDEDNESS, Handedness.RIGHT.name) ?: Handedness.RIGHT.name)
        }.getOrDefault(Handedness.RIGHT)
        _clockPosition.value = runCatching {
            ClockPosition.valueOf(prefs.getString(KEY_CLOCK_POSITION, ClockPosition.TOP_LARGE.name) ?: ClockPosition.TOP_LARGE.name)
        }.getOrDefault(ClockPosition.TOP_LARGE)
        _autoOpenKeyboard.value = prefs.getBoolean(KEY_AUTO_OPEN_KEYBOARD, false)
        _showCategoryBar.value = prefs.getBoolean(KEY_SHOW_CATEGORY_BAR, true)
        val stackBottom = prefs.getBoolean(KEY_STACK_FROM_BOTTOM, prefs.getBoolean(KEY_MOVE_ICONS_DOWN, true))
        _stackFromBottom.value = stackBottom
        _moveIconsDown.value = stackBottom
        _closeDrawerOnLaunch.value = prefs.getBoolean(KEY_CLOSE_DRAWER_ON_LAUNCH, true)
        _drawerSortOrder.value = runCatching {
            DrawerSortOrder.valueOf(
                prefs.getString(KEY_DRAWER_SORT_ORDER, DrawerSortOrder.USAGE_FREQUENCY.name)
                    ?: DrawerSortOrder.USAGE_FREQUENCY.name
            )
        }.getOrDefault(DrawerSortOrder.USAGE_FREQUENCY)
        _drawerColumns.value = prefs.getInt(KEY_DRAWER_COLUMNS, 4)
        _searchProvider.value = runCatching {
            SearchProvider.valueOf(
                prefs.getString(KEY_SEARCH_PROVIDER, SearchProvider.GOOGLE.name)
                    ?: SearchProvider.GOOGLE.name
            )
        }.getOrDefault(SearchProvider.GOOGLE)
        _searchIncludeHiddenApps.value = prefs.getBoolean(KEY_SEARCH_INCLUDE_HIDDEN, false)
        _searchStartAppsCount.value = prefs.getInt(KEY_SEARCH_START_APPS_COUNT, 4)
        _searchWebSuggest.value = prefs.getBoolean(KEY_SEARCH_WEB_SUGGEST, true)
        _searchPlayStore.value = prefs.getBoolean(KEY_SEARCH_PLAY_STORE, true)
        _gestureSwipeUpDrawer.value = prefs.getBoolean(KEY_GESTURE_SWIPE_UP_DRAWER, true)
        _gestureSwipeDownSearch.value = prefs.getBoolean(KEY_GESTURE_SWIPE_DOWN_SEARCH, true)
        _gestureSwipeDownCloseDrawer.value = prefs.getBoolean(KEY_GESTURE_SWIPE_DOWN_CLOSE_DRAWER, true)
        _gestureDoubleTapTrueBlack.value = prefs.getBoolean(KEY_GESTURE_DOUBLE_TAP_TRUE_BLACK, true)
        _discoverMode.value = runCatching {
            DiscoverMode.valueOf(
                prefs.getString(KEY_DISCOVER_MODE, DiscoverMode.GOOGLE_APP.name)
                    ?: DiscoverMode.GOOGLE_APP.name
            )
        }.getOrDefault(DiscoverMode.GOOGLE_APP)
        _aiProviderType.value = runCatching {
            AiProviderType.valueOf(
                prefs.getString(KEY_AI_PROVIDER_TYPE, AiProviderType.GEMINI.name)
                    ?: AiProviderType.GEMINI.name
            )
        }.getOrDefault(AiProviderType.GEMINI)
        _aiApiKey.value = prefs.getString(KEY_AI_API_KEY, "") ?: ""
        _aiBaseUrl.value = prefs.getString(KEY_AI_BASE_URL, AiProviderType.GEMINI.defaultEndpoint)
            ?: AiProviderType.GEMINI.defaultEndpoint
        _aiModelName.value = prefs.getString(KEY_AI_MODEL_NAME, AiProviderType.GEMINI.defaultModel)
            ?: AiProviderType.GEMINI.defaultModel
        _aiAutoCategorizeEnabled.value = prefs.getBoolean(KEY_AI_AUTO_CATEGORIZE, true)
        _aiQuickAnswerEnabled.value = prefs.getBoolean(KEY_AI_QUICK_ANSWER, true)
        _categoryCustomNames.value = loadCategoryCustomNames()
        _hiddenCategories.value = loadHiddenCategories()
        _categoryOrder.value = loadCategoryOrder()
        _honeycombEnabled.value = prefs.getBoolean(KEY_HONEYCOMB_ENABLED, true)
        _honeycombAppCount.value = prefs.getInt(KEY_HONEYCOMB_APP_COUNT, 7).coerceIn(3, 12)
        _iconStyle.value = runCatching {
            IconStyle.valueOf(
                prefs.getString(KEY_ICON_STYLE, IconStyle.MONOCHROME.name)
                    ?: IconStyle.MONOCHROME.name
            )
        }.getOrDefault(IconStyle.MONOCHROME)
        _homeWidgetId.value = prefs.getInt(KEY_HOME_WIDGET_ID, -1)
        _homeWidgetHeightDp.value = prefs.getInt(KEY_HOME_WIDGET_HEIGHT_DP, 160).coerceIn(80, 420)
        _idleDimEnabled.value = prefs.getBoolean(KEY_IDLE_DIM_ENABLED, true)
    }

    private fun exportPreferencesObject(): JSONObject {
        val json = JSONObject()
        val all = prefs.all
        for ((key, value) in all) {
            if (key == KEY_LAST_UPDATE_CHECK_TIME) continue
            when (value) {
                is Boolean -> json.put(key, value)
                is Int -> json.put(key, value)
                is Long -> json.put(key, value)
                is Float -> json.put(key, value.toDouble())
                is String -> json.put(key, value)
                is Set<*> -> {
                    val array = JSONArray()
                    value.forEach { array.put(it.toString()) }
                    json.put(key, array)
                }
            }
        }
        return json
    }

    fun exportToJson(): String {
        return exportPreferencesObject().toString()
    }

    /**
     * Envelope v2 形式で設定データ（および任意で学習データ）をバックアップJSONとして出力する
     */
    fun exportBackupJson(
        includeLearningData: Boolean = true,
        appRepository: AppRepository? = null
    ): String {
        val envelope = JSONObject()
        val includeUsage = includeLearningData && appRepository != null
        envelope.put("version", 2)
        envelope.put("app_version", BuildConfig.VERSION_NAME)
        envelope.put("created_at", System.currentTimeMillis())
        envelope.put("has_learning_data", includeUsage)
        envelope.put("preferences", exportPreferencesObject())
        if (includeUsage) {
            envelope.put("usage_data", appRepository.exportUsageData())
        } else {
            envelope.put("usage_data", JSONObject())
        }
        return envelope.toString(2)
    }

    /**
     * Envelope v2 形式および従来の v1 フラット JSON 形式の両方をサポートして設定・学習データを復元する
     */
    fun importBackupJson(
        jsonString: String,
        appRepository: AppRepository? = null
    ): BackupRestoreResult {
        if (jsonString.isBlank()) {
            return BackupRestoreResult(false, 0, 0, false)
        }
        return try {
            val root = JSONObject(jsonString)
            val isV2Envelope = root.optInt("version", 1) >= 2 && root.has("preferences")

            val prefsJson = if (isV2Envelope) {
                root.optJSONObject("preferences") ?: return BackupRestoreResult(false, 0, 0, false)
            } else {
                root
            }

            val validPrefEntries = extractValidPreferenceEntries(prefsJson)

            if (isV2Envelope) {
                val hasLearning = root.optBoolean("has_learning_data", false) && root.has("usage_data")
                val usageJson = root.optJSONObject("usage_data")

                // v2 でも preferences と usage_data の両方が空で設定キーも存在しない不正JSONは拒否する
                if (validPrefEntries.isEmpty() && !hasLearning && prefsJson.length() > 0) {
                    return BackupRestoreResult(false, 0, 0, false)
                }
                if (hasLearning && usageJson == null) {
                    return BackupRestoreResult(false, 0, 0, false)
                }
                if (validPrefEntries.isEmpty() && prefsJson.length() > 0 && hasLearning && (usageJson == null || usageJson.length() == 0)) {
                    return BackupRestoreResult(false, 0, 0, false)
                }

                val lastCheckBackup = getLastUpdateCheckTime()
                val editor = prefs.edit()
                editor.clear()
                if (lastCheckBackup > 0L) {
                    editor.putLong(KEY_LAST_UPDATE_CHECK_TIME, lastCheckBackup)
                }
                val prefsRestored = writePreferenceEntries(editor, validPrefEntries)
                editor.apply()
                reloadFromPreferences()

                val usageRestored = if (hasLearning && usageJson != null && appRepository != null) {
                    appRepository.importUsageData(usageJson)
                } else {
                    0
                }

                BackupRestoreResult(
                    success = true,
                    prefsCount = prefsRestored,
                    usageCount = usageRestored,
                    hasLearningData = hasLearning && appRepository != null
                )
            } else {
                // v1 Flat JSON 後方互換ルート
                if (validPrefEntries.isEmpty()) {
                    return BackupRestoreResult(false, 0, 0, false)
                }

                val lastCheckBackup = getLastUpdateCheckTime()
                val editor = prefs.edit()
                editor.clear()
                if (lastCheckBackup > 0L) {
                    editor.putLong(KEY_LAST_UPDATE_CHECK_TIME, lastCheckBackup)
                }
                val prefsRestored = writePreferenceEntries(editor, validPrefEntries)
                editor.apply()
                reloadFromPreferences()

                BackupRestoreResult(
                    success = true,
                    prefsCount = prefsRestored,
                    usageCount = 0,
                    hasLearningData = false
                )
            }
        } catch (_: Exception) {
            BackupRestoreResult(false, 0, 0, false)
        }
    }

    fun importFromJson(jsonString: String): Boolean {
        return importBackupJson(jsonString, appRepository = null).success
    }

    private fun extractValidPreferenceEntries(prefsJson: JSONObject): List<Pair<String, Any>> {
        val keys = prefsJson.keys()
        val validEntries = mutableListOf<Pair<String, Any>>()
        while (keys.hasNext()) {
            val key = keys.next()
            if (!key.startsWith("key_") && !key.startsWith("cat_label_")) {
                continue
            }
            if (key == KEY_LAST_UPDATE_CHECK_TIME) {
                continue
            }
            val value = prefsJson.opt(key) ?: continue
            if (value == JSONObject.NULL) continue
            validEntries.add(key to value)
        }
        return validEntries
    }

    private fun writePreferenceEntries(
        editor: SharedPreferences.Editor,
        entries: List<Pair<String, Any>>
    ): Int {
        var count = 0
        for ((key, value) in entries) {
            when (value) {
                is Boolean -> {
                    editor.putBoolean(key, value)
                    count++
                }
                is Number -> {
                    editor.putInt(key, value.toInt())
                    count++
                }
                is String -> {
                    editor.putString(key, value)
                    count++
                }
                is JSONArray -> {
                    val set = mutableSetOf<String>()
                    for (i in 0 until value.length()) {
                        val item = value.optString(i, "")
                        if (item.isNotBlank()) {
                            set.add(item)
                        }
                    }
                    editor.putStringSet(key, set)
                    count++
                }
            }
        }
        return count
    }

    fun getLastUpdateCheckTime(): Long {
        return prefs.getLong(KEY_LAST_UPDATE_CHECK_TIME, 0L)
    }

    fun setLastUpdateCheckTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_UPDATE_CHECK_TIME, time).apply()
    }

    companion object {
        private const val KEY_ANCHOR_TEXT = "key_anchor_text"
        private const val KEY_DISPLAY_MODE = "key_display_mode"
        private const val KEY_HANDEDNESS = "key_handedness"
        private const val KEY_CLOCK_POSITION = "key_clock_position"
        private const val KEY_AUTO_OPEN_KEYBOARD = "key_auto_open_keyboard"
        private const val KEY_SHOW_CATEGORY_BAR = "key_show_category_bar"
        private const val KEY_MOVE_ICONS_DOWN = "key_move_icons_down"
        private const val KEY_STACK_FROM_BOTTOM = "key_stack_from_bottom"
        private const val KEY_CLOSE_DRAWER_ON_LAUNCH = "key_close_drawer_on_launch"
        private const val KEY_DRAWER_SORT_ORDER = "key_drawer_sort_order"
        private const val KEY_DRAWER_COLUMNS = "key_drawer_columns"
        private const val KEY_SEARCH_PROVIDER = "key_search_provider"
        private const val KEY_SEARCH_INCLUDE_HIDDEN = "key_search_include_hidden"
        private const val KEY_SEARCH_START_APPS_COUNT = "key_search_start_apps_count"
        private const val KEY_SEARCH_WEB_SUGGEST = "key_search_web_suggest"
        private const val KEY_SEARCH_PLAY_STORE = "key_search_play_store"
        private const val KEY_GESTURE_SWIPE_UP_DRAWER = "key_gesture_swipe_up_drawer"
        private const val KEY_GESTURE_SWIPE_DOWN_SEARCH = "key_gesture_swipe_down_search"
        private const val KEY_GESTURE_SWIPE_DOWN_CLOSE_DRAWER = "key_gesture_swipe_down_close_drawer"
        private const val KEY_GESTURE_DOUBLE_TAP_TRUE_BLACK = "key_gesture_double_tap_true_black"
        private const val KEY_DISCOVER_MODE = "key_discover_mode"
        private const val KEY_AI_PROVIDER_TYPE = "key_ai_provider_type"
        private const val KEY_AI_API_KEY = "key_ai_api_key"
        private const val KEY_AI_BASE_URL = "key_ai_base_url"
        private const val KEY_AI_MODEL_NAME = "key_ai_model_name"
        private const val KEY_AI_AUTO_CATEGORIZE = "key_ai_auto_categorize"
        private const val KEY_AI_QUICK_ANSWER = "key_ai_quick_answer"
        private const val KEY_HIDDEN_CATEGORIES = "key_hidden_categories"
        private const val KEY_CATEGORY_ORDER = "key_category_order"
        private const val KEY_HONEYCOMB_ENABLED = "key_honeycomb_enabled"
        private const val KEY_HONEYCOMB_APP_COUNT = "key_honeycomb_app_count"
        private const val KEY_ICON_STYLE = "key_icon_style"
        private const val KEY_HOME_WIDGET_ID = "key_home_widget_id"
        private const val KEY_HOME_WIDGET_HEIGHT_DP = "key_home_widget_height_dp"
        private const val KEY_IDLE_DIM_ENABLED = "key_idle_dim_enabled"
        private const val KEY_LAST_UPDATE_CHECK_TIME = "key_last_update_check_time"
    }
}
