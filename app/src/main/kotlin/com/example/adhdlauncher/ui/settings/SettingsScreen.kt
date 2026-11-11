package com.example.adhdlauncher.ui.settings

import android.content.Intent
import android.provider.Settings
import com.example.adhdlauncher.BuildConfig
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adhdlauncher.data.AiProviderType
import com.example.adhdlauncher.data.AppCategory
import com.example.adhdlauncher.data.AppInfo
import com.example.adhdlauncher.data.ClockPosition
import com.example.adhdlauncher.data.DiscoverMode
import com.example.adhdlauncher.data.DisplayMode
import com.example.adhdlauncher.data.DrawerSortOrder
import com.example.adhdlauncher.data.Handedness
import com.example.adhdlauncher.data.IconStyle
import com.example.adhdlauncher.data.LauncherPreferences
import com.example.adhdlauncher.data.SearchProvider
import com.example.adhdlauncher.ui.util.HexagonShape

enum class SettingsPage {
    ROOT,
    APPEARANCE,
    GESTURES,
    BACKUP,
    HOME_PAGE,
    DRAWER_PAGE,
    SEARCH_PAGE,
    FEED_DISCOVER,
    AI_INTEGRATION
}

private val AccentCyan = Color(0xFF90CAF9)
private val AccentSage = Color(0xFF94A88B)

@Composable
fun SettingsScreen(
    apps: List<AppInfo>,
    preferences: LauncherPreferences,
    initialPage: SettingsPage = SettingsPage.ROOT,
    onPickWidget: () -> Unit = {},
    onRemoveWidget: () -> Unit = {},
    onToggleAppHidden: (AppInfo, Boolean) -> Unit,
    onResetUsageHistory: () -> Unit,
    onUnhideAllApps: () -> Unit,
    onRefreshApps: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember(initialPage) { mutableStateOf(initialPage) }

    BackHandler {
        if (currentPage != SettingsPage.ROOT) {
            currentPage = SettingsPage.ROOT
        } else {
            onClose()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                fadeIn(animationSpec = tween(120)) togetherWith fadeOut(animationSpec = tween(90))
            },
            label = "SettingsNavigation"
        ) { page ->
            when (page) {
                SettingsPage.ROOT -> RootSettingsView(
                    onNavigate = { currentPage = it },
                    onClose = onClose
                )
                SettingsPage.DRAWER_PAGE -> DrawerSettingsView(
                    apps = apps,
                    preferences = preferences,
                    onToggleAppHidden = onToggleAppHidden,
                    onUnhideAllApps = onUnhideAllApps,
                    onRefreshApps = onRefreshApps,
                    onBack = { currentPage = SettingsPage.ROOT },
                    onOpenAppearance = { currentPage = SettingsPage.APPEARANCE }
                )
                SettingsPage.HOME_PAGE -> HomeSettingsView(
                    preferences = preferences,
                    onPickWidget = onPickWidget,
                    onRemoveWidget = onRemoveWidget,
                    onBack = { currentPage = SettingsPage.ROOT }
                )
                SettingsPage.APPEARANCE -> AppearanceSettingsView(
                    preferences = preferences,
                    onBack = { currentPage = SettingsPage.ROOT }
                )
                SettingsPage.GESTURES -> GestureSettingsView(
                    preferences = preferences,
                    onBack = { currentPage = SettingsPage.ROOT }
                )
                SettingsPage.SEARCH_PAGE -> SearchSettingsView(
                    preferences = preferences,
                    onBack = { currentPage = SettingsPage.ROOT },
                    onOpenAppearance = { currentPage = SettingsPage.APPEARANCE }
                )
                SettingsPage.FEED_DISCOVER -> FeedDiscoverSettingsView(
                    preferences = preferences,
                    onBack = { currentPage = SettingsPage.ROOT }
                )
                SettingsPage.AI_INTEGRATION -> AiIntegrationSettingsView(
                    apps = apps,
                    preferences = preferences,
                    onBack = { currentPage = SettingsPage.ROOT }
                )
                SettingsPage.BACKUP -> BackupSettingsView(
                    preferences = preferences,
                    onResetUsageHistory = onResetUsageHistory,
                    onUnhideAllApps = onUnhideAllApps,
                    onBack = { currentPage = SettingsPage.ROOT }
                )
            }
        }
    }
}

@Composable
private fun RootSettingsView(
    onNavigate: (SettingsPage) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        // 独自ミニマルヘッダー（The Centering Arc アイコンバッジ採用）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AccentSage,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3A4138))
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "設定",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "ADHD Launcher v${BuildConfig.VERSION_NAME}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.45f)
                    )
                }
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "閉じる",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // 上部カード（利用頻度アクセス権限・システム同期）
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF14161B),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                    try {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    } catch (_: Exception) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "アプリの利用頻度をOSと同期",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "よく使うアプリを右下・下スワイプ検索へ自動反映",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.55f)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "基本設定",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AccentCyan.copy(alpha = 0.8f),
            modifier = Modifier.padding(vertical = 6.dp)
        )

        RootMenuItem(
            icon = Icons.Outlined.Image,
            title = "壁紙",
            subtitle = "システムの壁紙を変更",
            onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER))
                } catch (_: Exception) {
                    onNavigate(SettingsPage.APPEARANCE)
                }
            }
        )
        RootMenuItem(
            icon = Icons.Outlined.Palette,
            title = "全体的な外観・省電力",
            subtitle = "Material You アイコン変換 / 時間帯スケジュール / True Black",
            onClick = { onNavigate(SettingsPage.APPEARANCE) }
        )
        RootMenuItem(
            icon = Icons.Outlined.TouchApp,
            title = "ジェスチャー・利き手",
            subtitle = "各スワイプ動作のON/OFF・右利き/左利き配置",
            onClick = { onNavigate(SettingsPage.GESTURES) }
        )
        RootMenuItem(
            icon = Icons.Outlined.Restore,
            title = "バックアップ・初期化",
            subtitle = "非表示アプリの一括復元・利用頻度リセット",
            onClick = { onNavigate(SettingsPage.BACKUP) }
        )

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.padding(vertical = 14.dp)
        )

        Text(
            text = "画面・連携設定",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AccentCyan.copy(alpha = 0.8f),
            modifier = Modifier.padding(vertical = 6.dp)
        )

        RootMenuItem(
            icon = Icons.Outlined.Home,
            title = "ホーム画面",
            subtitle = "タップ展開ハニカム・ウィジェット配置・無操作フェード・時計",
            onClick = { onNavigate(SettingsPage.HOME_PAGE) }
        )
        RootMenuItem(
            icon = Icons.Outlined.Apps,
            title = "ドロワー設定",
            subtitle = "下積み配置・カテゴリバー・非表示アプリの個別管理",
            onClick = { onNavigate(SettingsPage.DRAWER_PAGE) }
        )
        RootMenuItem(
            icon = Icons.Outlined.Search,
            title = "検索ページ（下スワイプ検索）",
            subtitle = "よく使うアプリ表示数・検索プロバイダ",
            onClick = { onNavigate(SettingsPage.SEARCH_PAGE) }
        )
        RootMenuItem(
            icon = Icons.Outlined.DynamicFeed,
            title = "フィード・Google Discover",
            subtitle = "ホーム右スワイプ時のGoogle Discover連携設定",
            onClick = { onNavigate(SettingsPage.FEED_DISCOVER) }
        )
        RootMenuItem(
            icon = Icons.Outlined.AutoAwesome,
            title = "AI API 連携",
            subtitle = "Gemini / OpenAI互換API設定・未知アプリの自動分類",
            onClick = { onNavigate(SettingsPage.AI_INTEGRATION) }
        )

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.padding(vertical = 14.dp)
        )

        Text(
            text = "システム",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AccentCyan.copy(alpha = 0.8f),
            modifier = Modifier.padding(vertical = 6.dp)
        )

        RootMenuItem(
            icon = Icons.Default.Smartphone,
            title = "デフォルトのホームアプリ設定",
            subtitle = "Androidの既定ホームアプリを切り替える",
            onClick = {
                try {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                } catch (_: Exception) {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun RootMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(23.dp)
            )
            Spacer(modifier = Modifier.width(18.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * ドロワー設定サブ画面（非表示アプリの個別チェック管理ダイアログ付き）
 */
@Composable
private fun DrawerSettingsView(
    apps: List<AppInfo>,
    preferences: LauncherPreferences,
    onToggleAppHidden: (AppInfo, Boolean) -> Unit,
    onUnhideAllApps: () -> Unit,
    onRefreshApps: () -> Unit,
    onBack: () -> Unit,
    onOpenAppearance: () -> Unit
) {
    val showCategoryBar by preferences.showCategoryBar.collectAsState()
    val moveIconsDown by preferences.moveIconsDown.collectAsState()
    val stackFromBottom by preferences.stackFromBottom.collectAsState()
    val closeDrawerOnLaunch by preferences.closeDrawerOnLaunch.collectAsState()
    val autoOpenKeyboard by preferences.autoOpenKeyboard.collectAsState()
    val sortOrder by preferences.drawerSortOrder.collectAsState()
    val columns by preferences.drawerColumns.collectAsState()
    val handedness by preferences.handedness.collectAsState()
    val categoryCustomNames by preferences.categoryCustomNames.collectAsState()
    val hiddenCategories by preferences.hiddenCategories.collectAsState()
    val categoryOrder by preferences.categoryOrder.collectAsState()

    var showGridPropertiesDialog by remember { mutableStateOf(false) }
    var showHiddenAppsDialog by remember { mutableStateOf(false) }
    var showCategoryManageDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<AppCategory?>(null) }
    var editingCategoryName by remember { mutableStateOf("") }
    var advancedExpanded by remember { mutableStateOf(true) }

    val hiddenAppsCount = remember(apps) { apps.count { it.isHidden } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        SubPageHeader(
            title = "ドロワー設定",
            onBack = onBack,
            onOpenAppearance = onOpenAppearance
        )

        SubPageNavRow(
            icon = Icons.Default.Palette,
            title = "アイコンの外観・配置方向（${handedness.label} / Material You変換）",
            onClick = onOpenAppearance
        )

        SubPageNavRow(
            icon = Icons.Default.GridView,
            title = "グリッドのプロパティ（${sortOrder.label} / ${columns}列）",
            onClick = { showGridPropertiesDialog = true }
        )

        SubPageNavRow(
            icon = Icons.Default.Visibility,
            title = "非表示アプリの管理（現在 ${hiddenAppsCount}個）",
            onClick = { showHiddenAppsDialog = true }
        )

        SubPageNavRow(
            icon = Icons.Default.Category,
            title = "カテゴリーの管理（名称変更・並び替え・非表示）",
            onClick = { showCategoryManageDialog = true }
        )

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.1f),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { preferences.setShowCategoryBar(!showCategoryBar) }
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = "カテゴリーバー",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(Color.White.copy(alpha = 0.2f))
                )
                Spacer(modifier = Modifier.width(14.dp))
                Switch(
                    checked = showCategoryBar,
                    onCheckedChange = { preferences.setShowCategoryBar(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF2979FF)
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    text = "アニメーション・操作",
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = "超高速 (110ms) / 下スワイプで閉じる / 長押しでカテゴリ変更",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
        }

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.1f),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { advancedExpanded = !advancedExpanded }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "詳細設定",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.55f)
            )
            Icon(
                imageVector = if (advancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.55f)
            )
        }

        if (advancedExpanded) {
            CleanSwitchRow(
                title = "ドロワー展開時に自動でキーボードを開く",
                subtitle = "ドロワーを開いた瞬間に文字入力でアプリを即座に絞り込みます",
                checked = autoOpenKeyboard,
                onCheckedChange = { preferences.setAutoOpenKeyboard(it) }
            )

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.padding(vertical = 6.dp)
            )

            CleanSwitchRow(
                title = "アイコンを下に移動",
                subtitle = "親指が届きやすいよう、よく使うアプリを画面下部の利き手側（右下または左下）から順に配置します",
                checked = moveIconsDown,
                onCheckedChange = { preferences.setMoveIconsDown(it) }
            )

            CleanSwitchRow(
                title = "アイコンを下に寄せる",
                subtitle = "アプリ数が少ないカテゴリでも、アイコンを画面下部（カテゴリーバーの直上）へ揃えて配置します",
                checked = stackFromBottom,
                onCheckedChange = { preferences.setStackFromBottom(it) }
            )

            CleanSwitchRow(
                title = "アプリ起動後にドロワーを閉じる",
                subtitle = null,
                checked = closeDrawerOnLaunch,
                onCheckedChange = { preferences.setCloseDrawerOnLaunch(it) }
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
    }

    if (showGridPropertiesDialog) {
        AlertDialog(
            onDismissRequest = { showGridPropertiesDialog = false },
            containerColor = Color(0xFF1A1B20),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("グリッドのプロパティ", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "並び順",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    DrawerSortOrder.values().forEach { order ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    preferences.setDrawerSortOrder(order)
                                    onRefreshApps()
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (sortOrder == order),
                                onClick = {
                                    preferences.setDrawerSortOrder(order)
                                    onRefreshApps()
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = order.label, fontSize = 14.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "グリッドの列数",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(4, 5).forEach { col ->
                            FilterChip(
                                selected = (columns == col),
                                onClick = { preferences.setDrawerColumns(col) },
                                label = { Text("${col}列") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGridPropertiesDialog = false }) {
                    Text("閉じる", color = AccentCyan)
                }
            }
        )
    }

    // 課題5: 非表示アプリの個別チェック管理ダイアログ
    if (showHiddenAppsDialog) {
        val sortedAllApps = remember(apps) {
            apps.sortedWith(compareByDescending<AppInfo> { it.isHidden }.thenBy { it.label.lowercase() })
        }
        AlertDialog(
            onDismissRequest = { showHiddenAppsDialog = false },
            containerColor = Color(0xFF1A1B20),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("非表示アプリの管理", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (hiddenAppsCount > 0) {
                        TextButton(onClick = onUnhideAllApps) {
                            Text("すべて解除", fontSize = 12.sp, color = AccentCyan)
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "チェックを入れたアプリはドロワーから非表示になります",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    sortedAllApps.forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleAppHidden(app, !app.isHidden) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (app.iconBitmap != null) {
                                    Image(
                                        bitmap = app.iconBitmap,
                                        contentDescription = app.label,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = app.label,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                            Checkbox(
                                checked = app.isHidden,
                                onCheckedChange = { onToggleAppHidden(app, it) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHiddenAppsDialog = false }) {
                    Text("完了", color = AccentCyan)
                }
            }
        )
    }

    // カテゴリー管理ダイアログ（名称変更・並び替え・タブ表示ON/OFF）
    if (showCategoryManageDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryManageDialog = false },
            containerColor = Color(0xFF1A1B20),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("カテゴリーの管理", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { preferences.resetCategoriesToDefault() }) {
                        Text("初期化", fontSize = 12.sp, color = AccentCyan)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "鉛筆アイコンで名称変更、上下矢印で並び替え、チェックでタブの表示/非表示を切り替えられます。",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    val nonAllCategories = categoryOrder.filter { it != AppCategory.ALL }
                    categoryOrder.forEach { cat ->
                        val label = categoryCustomNames[cat] ?: cat.displayName
                        val isAll = (cat == AppCategory.ALL)
                        val isVisible = cat !in hiddenCategories
                        val idxInNonAll = nonAllCategories.indexOf(cat)

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = isVisible,
                                        enabled = !isAll,
                                        onCheckedChange = { checked ->
                                            preferences.setCategoryHidden(cat, !checked)
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = label,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                        if (label != cat.displayName) {
                                            Text(
                                                text = "標準: ${cat.displayName}",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.45f)
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!isAll) {
                                        IconButton(
                                            onClick = { preferences.moveCategoryOrder(cat, moveUp = true) },
                                            enabled = idxInNonAll > 0,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                contentDescription = "上へ",
                                                tint = if (idxInNonAll > 0) Color.White else Color.White.copy(alpha = 0.2f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { preferences.moveCategoryOrder(cat, moveUp = false) },
                                            enabled = idxInNonAll >= 0 && idxInNonAll < nonAllCategories.lastIndex,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "下へ",
                                                tint = if (idxInNonAll < nonAllCategories.lastIndex) Color.White else Color.White.copy(alpha = 0.2f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            editingCategory = cat
                                            editingCategoryName = label
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "名称変更",
                                            tint = AccentCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryManageDialog = false }) {
                    Text("完了", color = AccentCyan)
                }
            }
        )
    }

    // 個別カテゴリー名称変更ダイアログ
    editingCategory?.let { targetCat ->
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            containerColor = Color(0xFF1A1B20),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("カテゴリー名の変更", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "標準名: ${targetCat.displayName}（空欄で保存すると標準名に戻ります）",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = editingCategoryName,
                        onValueChange = { editingCategoryName = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text("キャンセル", color = Color.White.copy(alpha = 0.6f))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        preferences.setCategoryCustomName(targetCat, editingCategoryName)
                        editingCategory = null
                    }
                ) {
                    Text("保存", color = AccentCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

/**
 * フィード・Google Discover 連携設定枠（サブ画面）
 */
@Composable
private fun FeedDiscoverSettingsView(
    preferences: LauncherPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val discoverMode by preferences.discoverMode.collectAsState()

    val isGoogleAppInstalled = remember {
        try {
            context.packageManager.getPackageInfo("com.google.android.googlequicksearchbox", 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        SubPageHeader(title = "フィード・Google Discover", onBack = onBack)

        // ステータスカード
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF14161B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DynamicFeed,
                        contentDescription = null,
                        tint = AccentSage,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isGoogleAppInstalled) "Googleアプリ検出済み（連携可能）" else "Googleアプリ未検出（ブラウザ起動にフォールバック）",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ホーム画面で左から右へスワイプした際に表示するフィード画面を選択できます。",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "右スワイプ時のフィード動作",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        DiscoverMode.values().forEach { mode ->
            CleanRadioRow(
                title = mode.label,
                subtitle = mode.description,
                selected = (discoverMode == mode),
                onClick = { preferences.setDiscoverMode(mode) }
            )
        }
    }
}

/**
 * AI API 連携設定枠（サブ画面）
 */
@Composable
private fun AiIntegrationSettingsView(
    apps: List<AppInfo>,
    preferences: LauncherPreferences,
    onBack: () -> Unit
) {
    val providerType by preferences.aiProviderType.collectAsState()
    val apiKey by preferences.aiApiKey.collectAsState()
    val baseUrl by preferences.aiBaseUrl.collectAsState()
    val modelName by preferences.aiModelName.collectAsState()
    val autoCategorize by preferences.aiAutoCategorizeEnabled.collectAsState()
    val quickAnswer by preferences.aiQuickAnswerEnabled.collectAsState()

    var showKey by remember { mutableStateOf(false) }
    var testStatus by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        SubPageHeader(title = "AI API 連携", onBack = onBack)

        Text(
            text = "AIプロバイダ選択",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        AiProviderType.values().forEach { type ->
            CleanRadioRow(
                title = type.label,
                subtitle = "標準モデル: ${type.defaultModel}",
                selected = (providerType == type),
                onClick = { preferences.setAiProviderType(type) }
            )
        }

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.1f),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Text(
            text = "API 接続設定 (BYOK)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = { preferences.setAiApiKey(it) },
            label = { Text("API Key") },
            placeholder = { Text("AIza... または sk-...") },
            singleLine = true,
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showKey = !showKey }) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "表示切替",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                focusedLabelColor = AccentCyan,
                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        )

        OutlinedTextField(
            value = baseUrl,
            onValueChange = { preferences.setAiBaseUrl(it) },
            label = { Text("エンドポイント URL") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                focusedLabelColor = AccentCyan,
                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        )

        OutlinedTextField(
            value = modelName,
            onValueChange = { preferences.setAiModelName(it) },
            label = { Text("モデル名") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                focusedLabelColor = AccentCyan,
                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        )

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.1f),
            modifier = Modifier.padding(vertical = 14.dp)
        )

        Text(
            text = "AI 連携機能",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        CleanSwitchRow(
            title = "未知アプリのAI一括カテゴライズ",
            subtitle = "辞書にないアプリをインストールした際、AIで6カテゴリへ自動仕分けします",
            checked = autoCategorize,
            onCheckedChange = { preferences.setAiAutoCategorizeEnabled(it) }
        )

        CleanSwitchRow(
            title = "下スワイプ検索でのAIクイックアンサー",
            subtitle = "ブラウザを開かずに検索画面上でAIの簡潔な回答を表示します",
            checked = quickAnswer,
            onCheckedChange = { preferences.setAiQuickAnswerEnabled(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                testStatus = if (apiKey.isBlank()) {
                    "APIキーが未入力です（現在 ${apps.size} 個のアプリがローカルJSON辞書で分類済み）"
                } else {
                    "設定保存済み（${providerType.label} / モデル: $modelName）"
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentSage,
                contentColor = Color(0xFF14161B)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("設定を確認・未知アプリをスキャン", fontWeight = FontWeight.Bold)
        }

        testStatus?.let { msg ->
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = msg,
                fontSize = 12.sp,
                color = AccentCyan
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

/**
 * 添付画像2枚目（検索ページ設定）に準拠したサブ画面
 */
@Composable
private fun SearchSettingsView(
    preferences: LauncherPreferences,
    onBack: () -> Unit,
    onOpenAppearance: () -> Unit
) {
    val searchProvider by preferences.searchProvider.collectAsState()
    val includeHidden by preferences.searchIncludeHiddenApps.collectAsState()
    val startAppsCount by preferences.searchStartAppsCount.collectAsState()
    val webSuggest by preferences.searchWebSuggest.collectAsState()
    val playStore by preferences.searchPlayStore.collectAsState()

    var showProviderDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        SubPageHeader(
            title = "検索ページ",
            onBack = onBack,
            onOpenAppearance = onOpenAppearance
        )

        // 1. 検索プロバイダ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { showProviderDialog = true }
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        text = "検索プロバイダ",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        text = searchProvider.label,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f)
            )
        }

        // 2. 検索結果に非表示アプリを含める
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { preferences.setSearchIncludeHiddenApps(!includeHidden) }
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = "検索結果に非表示アプリを含める",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
            Switch(
                checked = includeHidden,
                onCheckedChange = { preferences.setSearchIncludeHiddenApps(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF2979FF)
                )
            )
        }

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.1f),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // 3. START SCREEN セクション（よく使うアプリの表示数 - / 4 / +）
        Text(
            text = "START SCREEN（下スワイプ初期表示）",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = "よく使うアプリの表示数",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1C1D22),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { preferences.setSearchStartAppsCount(startAppsCount - 1) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "減らす",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = startAppsCount.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1C1D22),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { preferences.setSearchStartAppsCount(startAppsCount + 1) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "増やす",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.1f),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // 4. 検索結果セクション
        Text(
            text = "検索結果",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        IconSwitchRow(
            icon = Icons.AutoMirrored.Filled.CallMade,
            title = "Webサジェストを使用する",
            subtitle = "検索キーワードに関するWeb検索ショートカットを表示",
            checked = webSuggest,
            onCheckedChange = { preferences.setSearchWebSuggest(it) }
        )

        IconSwitchRow(
            icon = Icons.Default.Shop,
            title = "Playストアでアプリを検索",
            subtitle = "未インストールのアプリをPlayストアで直接探す",
            checked = playStore,
            onCheckedChange = { preferences.setSearchPlayStore(it) }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showProviderDialog) {
        AlertDialog(
            onDismissRequest = { showProviderDialog = false },
            containerColor = Color(0xFF1A1B20),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("検索プロバイダ", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    SearchProvider.values().forEach { provider ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    preferences.setSearchProvider(provider)
                                    showProviderDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (searchProvider == provider),
                                onClick = {
                                    preferences.setSearchProvider(provider)
                                    showProviderDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = provider.label, fontSize = 15.sp, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProviderDialog = false }) {
                    Text("閉じる", color = AccentCyan)
                }
            }
        )
    }
}

@Composable
private fun HomeSettingsView(
    preferences: LauncherPreferences,
    onPickWidget: () -> Unit,
    onRemoveWidget: () -> Unit,
    onBack: () -> Unit
) {
    val clockPosition by preferences.clockPosition.collectAsState()
    val honeycombEnabled by preferences.honeycombEnabled.collectAsState()
    val honeycombAppCount by preferences.honeycombAppCount.collectAsState()
    val homeWidgetId by preferences.homeWidgetId.collectAsState()
    val homeWidgetHeightDp by preferences.homeWidgetHeightDp.collectAsState()
    val idleDimEnabled by preferences.idleDimEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        SubPageHeader(title = "ホーム画面", onBack = onBack)

        // 1. 画面タップ時の動的ハニカムショートカット
        Text(
            text = "動的ハニカムショートカット（扇形展開）",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AccentCyan,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        CleanSwitchRow(
            title = "タップした指の座標から扇形にハニカム展開",
            subtitle = "空白領域をタップした位置を起点（扇の要）として、現在の曜日・時間帯によく使うアプリを六角形（ハニカム）の扇形アーク状に展開します",
            checked = honeycombEnabled,
            onCheckedChange = { preferences.setHoneycombEnabled(it) }
        )

        if (honeycombEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ハニカム扇形の展開アプリ数",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        text = "内側アーク最大5個 ＋ 外側アーク（最大12個、推奨7個）",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1C1D22),
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { preferences.setHoneycombAppCount(honeycombAppCount - 1) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "減らす",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = honeycombAppCount.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1C1D22),
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { preferences.setHoneycombAppCount(honeycombAppCount + 1) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "増やす",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.1f),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // 2. ウィジェット配置（KWGT / 時計 / カレンダー等）
        Text(
            text = "ホーム画面ウィジェット（KWGT・時計・予定等）",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AccentCyan,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Text(
            text = if (homeWidgetId != -1) {
                "現在ウィジェットが配置されています（ID: $homeWidgetId）"
            } else {
                "KWGT や Google At a Glance、カレンダー等のウィジェットを1つホーム画面に配置できます。"
            },
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.65f),
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPickWidget,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentSage,
                    contentColor = Color(0xFF14161B)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (homeWidgetId == -1) "ウィジェットを選択" else "ウィジェットを変更",
                    fontWeight = FontWeight.Bold
                )
            }

            if (homeWidgetId != -1) {
                OutlinedButton(
                    onClick = onRemoveWidget,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(0.7f)
                ) {
                    Text("削除", color = Color(0xFFEF9A9A))
                }
            }
        }

        if (homeWidgetId != -1) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ウィジェットの高さ (${homeWidgetHeightDp}dp)",
                    fontSize = 14.sp,
                    color = Color.White
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1C1D22),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { preferences.setHomeWidgetHeightDp(homeWidgetHeightDp - 20) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Remove, contentDescription = "低くする", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1C1D22),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { preferences.setHomeWidgetHeightDp(homeWidgetHeightDp + 20) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "高くする", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.1f),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // 3. 無操作時の半透明化（Idle Dimming）
        Text(
            text = "視覚ノイズ低減・省電力",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AccentCyan,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        CleanSwitchRow(
            title = "無操作時にウィジェットや時計を半透明にする",
            subtitle = "ホーム画面で5秒間操作がない場合、UIとウィジェットを半透明（24%）にフェードさせ、タッチで即座に戻します",
            checked = idleDimEnabled,
            onCheckedChange = { preferences.setIdleDimEnabled(it) }
        )

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.1f),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // 4. 時計の表示位置
        Text(
            text = "内蔵時計の表示位置",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AccentCyan,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        ClockPosition.values().forEach { pos ->
            CleanRadioRow(
                title = pos.label,
                subtitle = pos.description,
                selected = (clockPosition == pos),
                onClick = { preferences.setClockPosition(pos) }
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun AppearanceSettingsView(
    preferences: LauncherPreferences,
    onBack: () -> Unit
) {
    val displayMode by preferences.displayMode.collectAsState()
    val iconStyle by preferences.iconStyle.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        SubPageHeader(title = "全体的な外観・省電力", onBack = onBack)

        Text(
            text = "アプリアイコンのカラー変換（Material You・標準モノクロ）",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AccentCyan,
            modifier = Modifier.padding(vertical = 10.dp)
        )

        // 六角形カラースウォッチバー（ワンタップで色を切り替え可能）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconStyle.values().forEach { style ->
                val isSelected = (iconStyle == style)
                val bgColor = if (style == IconStyle.ORIGINAL) Color(0xFF2B2D35) else Color((0xFF000000L) or style.bgHex.toLong())
                val fgColor = if (style == IconStyle.ORIGINAL) AccentCyan else Color((0xFF000000L) or style.fgHex.toLong())
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(HexagonShape)
                        .background(bgColor)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) Color.White else fgColor.copy(alpha = 0.45f),
                            shape = HexagonShape
                        )
                        .clickable { preferences.setIconStyle(style) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(HexagonShape)
                            .background(fgColor)
                    )
                }
            }
        }

        IconStyle.values().forEach { style ->
            CleanRadioRow(
                title = style.label,
                subtitle = style.description,
                selected = (iconStyle == style),
                onClick = { preferences.setIconStyle(style) }
            )
        }

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.1f),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Text(
            text = "背景・OLED省電力モード",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AccentCyan,
            modifier = Modifier.padding(vertical = 10.dp)
        )

        DisplayMode.values().forEach { mode ->
            CleanRadioRow(
                title = mode.label,
                subtitle = mode.description,
                selected = (displayMode == mode),
                onClick = { preferences.setDisplayMode(mode) }
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun GestureSettingsView(
    preferences: LauncherPreferences,
    onBack: () -> Unit
) {
    val handedness by preferences.handedness.collectAsState()
    val swipeUpDrawer by preferences.gestureSwipeUpDrawer.collectAsState()
    val swipeDownSearch by preferences.gestureSwipeDownSearch.collectAsState()
    val swipeDownCloseDrawer by preferences.gestureSwipeDownCloseDrawer.collectAsState()
    val doubleTapTrueBlack by preferences.gestureDoubleTapTrueBlack.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        SubPageHeader(title = "ジェスチャー・利き手", onBack = onBack)

        Text(
            text = "ジェスチャー動作の有効 / 無効",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        CleanSwitchRow(
            title = "ホーム画面で上スワイプ",
            subtitle = "下から上へのスワイプでアプリドロワーを展開します",
            checked = swipeUpDrawer,
            onCheckedChange = { preferences.setGestureSwipeUpDrawer(it) }
        )
        CleanSwitchRow(
            title = "ホーム画面で下スワイプ",
            subtitle = "上から下へのスワイプでクイック検索画面を展開します",
            checked = swipeDownSearch,
            onCheckedChange = { preferences.setGestureSwipeDownSearch(it) }
        )
        CleanSwitchRow(
            title = "ドロワー画面で下スワイプ",
            subtitle = "ドロワー初期位置から下へスワイプしてホーム画面へ戻ります",
            checked = swipeDownCloseDrawer,
            onCheckedChange = { preferences.setGestureSwipeDownCloseDrawer(it) }
        )
        CleanSwitchRow(
            title = "ホーム画面のダブルタップ",
            subtitle = "空白領域のダブルタップで壁紙表示と True Black 省電力モードを切り替えます",
            checked = doubleTapTrueBlack,
            onCheckedChange = { preferences.setGestureDoubleTapTrueBlack(it) }
        )

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.1f),
            modifier = Modifier.padding(vertical = 14.dp)
        )

        Text(
            text = "利き手優先配置",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Handedness.values().forEach { hand ->
            CleanRadioRow(
                title = hand.label,
                subtitle = hand.description,
                selected = (handedness == hand),
                onClick = { preferences.setHandedness(hand) }
            )
        }
    }
}

@Composable
private fun BackupSettingsView(
    preferences: LauncherPreferences,
    onResetUsageHistory: () -> Unit,
    onUnhideAllApps: () -> Unit,
    onBack: () -> Unit
) {
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        SubPageHeader(title = "バックアップ・初期化", onBack = onBack)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    onUnhideAllApps()
                    statusMessage = "非表示にしたすべてのアプリを再表示しました"
                }
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "非表示アプリをすべて元に戻す",
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = "ドロワーの長押しメニューで非表示にしたアプリをすべて復元します",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    onResetUsageHistory()
                    statusMessage = "アプリ起動回数履歴とカスタム分類をリセットしました"
                }
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "アプリ利用頻度カウント・分類をリセット",
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = "ランチャー内で学習した起動回数と手動カテゴリ変更を初期化します",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    preferences.resetAllSettings()
                    statusMessage = "すべての設定を初期値に戻しました"
                }
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ランチャー設定を初期値に戻す",
                    fontSize = 16.sp,
                    color = Color(0xFFFF8A80)
                )
                Text(
                    text = "時計位置・ドロワー設定・検索ページ設定をすべてデフォルトへ戻します",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
        }

        statusMessage?.let { msg ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = msg,
                fontSize = 13.sp,
                color = Color(0xFF81C784)
            )
        }
    }
}

@Composable
private fun SubPageHeader(
    title: String,
    onBack: () -> Unit,
    onOpenAppearance: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "戻る",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        if (onOpenAppearance != null) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF1C1D22),
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenAppearance)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "外観",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SubPageNavRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(20.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                color = Color.White
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun IconSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).padding(end = 16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = Color.White
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.55f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2979FF)
            )
        )
    }
}

@Composable
private fun CleanSwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = Color.White
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    lineHeight = 18.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2979FF)
            )
        )
    }
}

@Composable
private fun CleanRadioRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF2979FF),
                unselectedColor = Color.White.copy(alpha = 0.5f)
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.55f)
            )
        }
    }
}
