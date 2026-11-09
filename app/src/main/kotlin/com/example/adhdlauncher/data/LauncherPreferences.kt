package com.example.adhdlauncher.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

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
    BOTTOM_COMPACT("画面下部に寄せて表示", "目線や親指の近くに時間と日付を配置")
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

class LauncherPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("adhd_launcher_prefs", Context.MODE_PRIVATE)

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
        prefs.edit().putBoolean(KEY_MOVE_ICONS_DOWN, enabled).apply()
        _moveIconsDown.value = enabled
    }

    fun setStackFromBottom(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STACK_FROM_BOTTOM, enabled).apply()
        _stackFromBottom.value = enabled
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
    }
}
