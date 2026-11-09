package com.example.adhdlauncher

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.adhdlauncher.data.AppInfo
import com.example.adhdlauncher.data.AppRepository
import com.example.adhdlauncher.data.DiscoverMode
import com.example.adhdlauncher.data.LauncherPreferences
import com.example.adhdlauncher.ui.drawer.DrawerScreen
import com.example.adhdlauncher.ui.home.HomeScreen
import com.example.adhdlauncher.ui.search.PullDownSearchScreen
import com.example.adhdlauncher.ui.settings.SettingsPage
import com.example.adhdlauncher.ui.settings.SettingsScreen
import com.example.adhdlauncher.ui.theme.ADHDLauncherTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var appRepository: AppRepository
    private lateinit var preferences: LauncherPreferences
    private lateinit var appWidgetHost: AppWidgetHost
    private lateinit var appWidgetManager: AppWidgetManager

    private var homePressTrigger by mutableIntStateOf(0)
    private var packageChangeTrigger by mutableIntStateOf(0)
    private var pendingWidgetId: Int = -1

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            packageChangeTrigger++
        }
    }

    // ウィジェット設定画面（KWGT等の初期設定Activity）からの戻りハンドラ
    private val widgetConfigureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val widgetId = pendingWidgetId
        if (result.resultCode == Activity.RESULT_OK && widgetId != -1) {
            finalizeWidgetPlacement(widgetId)
        } else if (widgetId != -1) {
            appWidgetHost.deleteAppWidgetId(widgetId)
            pendingWidgetId = -1
        }
    }

    // システムのウィジェットピッカーからの戻りハンドラ
    private val widgetPickLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val widgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId) ?: pendingWidgetId
        if (result.resultCode == Activity.RESULT_OK && widgetId != -1) {
            val info = appWidgetManager.getAppWidgetInfo(widgetId)
            if (info?.configure != null) {
                val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = info.configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                }
                try {
                    widgetConfigureLauncher.launch(configIntent)
                } catch (_: Exception) {
                    finalizeWidgetPlacement(widgetId)
                }
            } else {
                finalizeWidgetPlacement(widgetId)
            }
        } else if (widgetId != -1) {
            appWidgetHost.deleteAppWidgetId(widgetId)
            pendingWidgetId = -1
        }
    }

    private fun finalizeWidgetPlacement(widgetId: Int) {
        val oldId = preferences.homeWidgetId.value
        if (oldId != -1 && oldId != widgetId) {
            try {
                appWidgetHost.deleteAppWidgetId(oldId)
            } catch (_: Exception) {}
        }
        preferences.setHomeWidgetId(widgetId)
        pendingWidgetId = -1
    }

    private fun launchWidgetPicker() {
        try {
            val allocatedId = appWidgetHost.allocateAppWidgetId()
            pendingWidgetId = allocatedId
            val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, allocatedId)
            }
            widgetPickLauncher.launch(pickIntent)
        } catch (_: Exception) {}
    }

    private fun removeHomeWidget() {
        val currentId = preferences.homeWidgetId.value
        if (currentId != -1) {
            try {
                appWidgetHost.deleteAppWidgetId(currentId)
            } catch (_: Exception) {}
            preferences.setHomeWidgetId(-1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appRepository = AppRepository(this)
        preferences = LauncherPreferences(this)
        appWidgetHost = AppWidgetHost(applicationContext, 1024)
        appWidgetManager = AppWidgetManager.getInstance(applicationContext)

        // アプリのインストール・アンインストール・更新を自動検知してドロワーへ即時反映
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)

        enableEdgeToEdge()

        setContent {
            ADHDLauncherTheme {
                LauncherMain(
                    appRepository = appRepository,
                    preferences = preferences,
                    appWidgetHost = appWidgetHost,
                    appWidgetManager = appWidgetManager,
                    homePressTrigger = homePressTrigger,
                    packageChangeTrigger = packageChangeTrigger,
                    onPickWidget = ::launchWidgetPicker,
                    onRemoveWidget = ::removeHomeWidget
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            appWidgetHost.startListening()
        } catch (_: Exception) {}
    }

    override fun onStop() {
        super.onStop()
        try {
            appWidgetHost.stopListening()
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(packageReceiver)
        } catch (_: Exception) {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            homePressTrigger++
        }
    }
}

@Composable
fun LauncherMain(
    appRepository: AppRepository,
    preferences: LauncherPreferences,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    homePressTrigger: Int,
    packageChangeTrigger: Int,
    onPickWidget: () -> Unit,
    onRemoveWidget: () -> Unit
) {
    val context = LocalContext.current
    var isDrawerOpen by remember { mutableStateOf(false) }
    var isQuickSearchOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var settingsInitialPage by remember { mutableStateOf(SettingsPage.ROOT) }
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    val closeDrawerOnLaunch by preferences.closeDrawerOnLaunch.collectAsState()
    val discoverMode by preferences.discoverMode.collectAsState()
    val iconStyle by preferences.iconStyle.collectAsState()
    val honeycombAppCount by preferences.honeycombAppCount.collectAsState()
    val scope = rememberCoroutineScope()

    // 初回起動時・パッケージ変更時・アイコン変換スタイル変更時に一覧を更新
    LaunchedEffect(packageChangeTrigger, iconStyle) {
        apps = appRepository.getInstalledApps(
            forceRefresh = packageChangeTrigger > 0,
            iconStyle = iconStyle
        )
    }

    // 現在の曜日・時間帯の学習スコア + 利用頻度に基づくハニカム候補アプリ
    val honeycombApps = remember(apps, honeycombAppCount) {
        appRepository.getContextualTopApps(apps, honeycombAppCount)
    }

    // ホームボタン・ホームジェスチャー押下時にドロワーや設定画面を閉じてホームへ戻る
    LaunchedEffect(homePressTrigger) {
        if (homePressTrigger > 0) {
            isDrawerOpen = false
            isQuickSearchOpen = false
            isSettingsOpen = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. ホーム画面（指の座標を中心にしたハニカム展開、ウィジェット、無操作半透明フェード対応）
        if (!isDrawerOpen && !isQuickSearchOpen && !isSettingsOpen) {
            HomeScreen(
                preferences = preferences,
                honeycombApps = honeycombApps,
                appWidgetHost = appWidgetHost,
                appWidgetManager = appWidgetManager,
                onAppClick = { app ->
                    apps = appRepository.launchApp(app)
                },
                onOpenDrawer = {
                    isQuickSearchOpen = false
                    isDrawerOpen = true
                },
                onOpenQuickSearch = {
                    isDrawerOpen = false
                    isQuickSearchOpen = true
                },
                onOpenFeed = {
                    when (discoverMode) {
                        DiscoverMode.GOOGLE_APP -> {
                            try {
                                val googleIntent = context.packageManager
                                    .getLaunchIntentForPackage("com.google.android.googlequicksearchbox")
                                if (googleIntent != null) {
                                    googleIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(googleIntent)
                                } else {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            } catch (_: Exception) {}
                        }
                        DiscoverMode.AI_MINIMAL_FEED -> {
                            settingsInitialPage = SettingsPage.FEED_DISCOVER
                            isSettingsOpen = true
                        }
                        DiscoverMode.OFF -> {}
                    }
                },
                onOpenSettings = {
                    settingsInitialPage = SettingsPage.ROOT
                    isSettingsOpen = true
                }
            )
        }

        // 2. 下から上へスワイプ：アプリドロワー画面（reverseLayoutによる自然な下積み＆上スクロール）
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 90)),
            exit = slideOutVertically(
                targetOffsetY = { it / 4 },
                animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 70))
        ) {
            DrawerScreen(
                apps = apps,
                preferences = preferences,
                onAppClick = { app ->
                    if (closeDrawerOnLaunch) {
                        isDrawerOpen = false
                    }
                    apps = appRepository.launchApp(app)
                },
                onChangeAppCategory = { app, newCategory ->
                    apps = appRepository.setAppCategory(app.packageName, newCategory)
                },
                onHideApp = { app ->
                    apps = appRepository.setAppHidden(app.packageName, true)
                },
                onOpenSettings = {
                    isDrawerOpen = false
                    settingsInitialPage = SettingsPage.ROOT
                    isSettingsOpen = true
                },
                onClose = { isDrawerOpen = false }
            )
        }

        // 3. 上から下へスワイプ：クイック検索画面（下部検索バー＋よく使うアプリ4つ）
        AnimatedVisibility(
            visible = isQuickSearchOpen,
            enter = slideInVertically(
                initialOffsetY = { -it / 4 },
                animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 90)),
            exit = slideOutVertically(
                targetOffsetY = { -it / 4 },
                animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 70))
        ) {
            PullDownSearchScreen(
                apps = apps,
                preferences = preferences,
                onAppClick = { app ->
                    isQuickSearchOpen = false
                    apps = appRepository.launchApp(app)
                },
                onOpenFullDrawer = {
                    isQuickSearchOpen = false
                    isDrawerOpen = true
                },
                onOpenSearchSettings = {
                    isQuickSearchOpen = false
                    settingsInitialPage = SettingsPage.SEARCH_PAGE
                    isSettingsOpen = true
                },
                onClose = { isQuickSearchOpen = false }
            )
        }

        // 4. 設定画面（110msの高速フェード）
        AnimatedVisibility(
            visible = isSettingsOpen,
            enter = fadeIn(animationSpec = tween(durationMillis = 110)),
            exit = fadeOut(animationSpec = tween(durationMillis = 90))
        ) {
            SettingsScreen(
                apps = apps,
                preferences = preferences,
                initialPage = settingsInitialPage,
                onPickWidget = onPickWidget,
                onRemoveWidget = onRemoveWidget,
                onToggleAppHidden = { app, hidden ->
                    apps = appRepository.setAppHidden(app.packageName, hidden)
                },
                onResetUsageHistory = {
                    apps = appRepository.resetUsageHistory()
                },
                onUnhideAllApps = {
                    apps = appRepository.unhideAllApps()
                },
                onRefreshApps = {
                    scope.launch {
                        apps = appRepository.getInstalledApps(
                            forceRefresh = true,
                            iconStyle = iconStyle
                        )
                    }
                },
                onClose = { isSettingsOpen = false }
            )
        }
    }
}
