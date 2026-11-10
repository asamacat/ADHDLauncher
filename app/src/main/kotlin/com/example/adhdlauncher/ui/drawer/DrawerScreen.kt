package com.example.adhdlauncher.ui.drawer

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adhdlauncher.data.AppCategory
import com.example.adhdlauncher.data.AppInfo
import com.example.adhdlauncher.data.DrawerSortOrder
import com.example.adhdlauncher.data.Handedness
import com.example.adhdlauncher.data.JapaneseSearchMatcher
import com.example.adhdlauncher.data.LauncherPreferences
import com.example.adhdlauncher.ui.util.HexagonShape
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerScreen(
    apps: List<AppInfo>,
    preferences: LauncherPreferences,
    onAppClick: (AppInfo) -> Unit,
    onChangeAppCategory: (AppInfo, AppCategory) -> Unit,
    onHideApp: (AppInfo) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onClose()
    }

    val context = LocalContext.current
    val autoOpenKeyboard by preferences.autoOpenKeyboard.collectAsState()
    val showCategoryBar by preferences.showCategoryBar.collectAsState()
    val moveIconsDown by preferences.moveIconsDown.collectAsState()
    val stackFromBottom by preferences.stackFromBottom.collectAsState()
    val handedness by preferences.handedness.collectAsState()
    val sortOrder by preferences.drawerSortOrder.collectAsState()
    val columns by preferences.drawerColumns.collectAsState()
    val gestureSwipeDownCloseDrawer by preferences.gestureSwipeDownCloseDrawer.collectAsState()
    val categoryCustomNames by preferences.categoryCustomNames.collectAsState()
    val hiddenCategories by preferences.hiddenCategories.collectAsState()
    val categoryOrder by preferences.categoryOrder.collectAsState()

    val visibleCategories = remember(categoryOrder, hiddenCategories) {
        categoryOrder.filter { it == AppCategory.ALL || it !in hiddenCategories }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(AppCategory.ALL) }
    var longPressedApp by remember { mutableStateOf<AppInfo?>(null) }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val gridState = rememberLazyGridState()

    // 設定がONの場合、ドロワーを開いた瞬間にキーボードを自動展開
    LaunchedEffect(autoOpenKeyboard) {
        if (autoOpenKeyboard) {
            delay(50)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // 選択中カテゴリが非表示化された場合は「すべて」へ戻す
    LaunchedEffect(visibleCategories) {
        if (selectedCategory !in visibleCategories) {
            selectedCategory = AppCategory.ALL
        }
    }
    LaunchedEffect(selectedCategory, searchQuery) {
        gridState.scrollToItem(0)
    }

    // 下スワイプでドロワーを閉じる NestedScrollConnection
    // reverseLayout = true の場合も、index 0（最下段）が表示されており、かつそれ以上スクロールできない状態で下へ引っ張ると available.y > 0f が渡される
    val nestedScrollConnection = remember(gridState, gestureSwipeDownCloseDrawer) {
        object : NestedScrollConnection {
            private var downwardDragAccumulated = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f) {
                    downwardDragAccumulated = 0f
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (gestureSwipeDownCloseDrawer && source == NestedScrollSource.UserInput && available.y > 0f) {
                    downwardDragAccumulated += available.y
                    if (downwardDragAccumulated > 52f) {
                        downwardDragAccumulated = 0f
                        keyboardController?.hide()
                        onClose()
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                downwardDragAccumulated = 0f
                val isAtInitialPosition = gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
                if (gestureSwipeDownCloseDrawer && isAtInitialPosition && available.y > 650f) {
                    keyboardController?.hide()
                    onClose()
                }
                return Velocity.Zero
            }
        }
    }

    // ベースとなるフィルタ＆ソート済みリスト（非表示アプリを除外、日本語ローマ字・かな・漢字検索対応）
    val sortedApps = remember(apps, searchQuery, selectedCategory, sortOrder, showCategoryBar) {
        val visibleApps = apps.filter { !it.isHidden }

        val categoryFiltered = if (selectedCategory == AppCategory.ALL || !showCategoryBar) {
            visibleApps
        } else {
            visibleApps.filter { it.category == selectedCategory }
        }

        val searchFiltered = if (searchQuery.isBlank()) {
            categoryFiltered
        } else {
            categoryFiltered.filter { app ->
                JapaneseSearchMatcher.matches(app, searchQuery)
            }
        }

        when (sortOrder) {
            DrawerSortOrder.USAGE_FREQUENCY -> {
                searchFiltered.sortedWith(
                    compareByDescending<AppInfo> { it.launchCount }
                        .thenByDescending { it.lastLaunchTime }
                        .thenByDescending { it.systemUsageScore }
                        .thenBy { it.label.lowercase() }
                )
            }
            DrawerSortOrder.ALPHABETICAL -> {
                searchFiltered.sortedBy { it.label.lowercase() }
            }
        }
    }

    // 下積み（reverseLayout）有効判定
    val useBottomStack = moveIconsDown || stackFromBottom

    // 行単位（columns個ずつ）で利き手方向に並べ、reverseLayout = true で最下段から上へ自然に積む
    val arrangedSlots: List<AppInfo?> = remember(
        sortedApps,
        columns,
        useBottomStack,
        handedness
    ) {
        if (sortedApps.isEmpty()) return@remember emptyList()

        sortedApps.chunked(columns).flatMap { chunk ->
            val padded: List<AppInfo?> = if (chunk.size < columns) {
                chunk + List(columns - chunk.size) { null }
            } else {
                chunk
            }
            if (useBottomStack && handedness == Handedness.RIGHT) {
                padded.reversed()
            } else {
                padded
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
            .systemBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // 1. メインアプリグリッド（reverseLayout による自然な下積み＆上スクロール対応）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .nestedScroll(nestedScrollConnection),
                contentAlignment = if (useBottomStack) Alignment.BottomCenter else Alignment.TopCenter
            ) {
                if (arrangedSlots.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "一致するアプリがありません",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(columns),
                        reverseLayout = useBottomStack,
                        verticalArrangement = Arrangement.spacedBy(
                            space = 12.dp,
                            alignment = if (useBottomStack) Alignment.Bottom else Alignment.Top
                        ),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = 4.dp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(
                            items = arrangedSlots,
                            key = { index, app ->
                                if (app != null) {
                                    "${app.packageName}:${app.activityName}"
                                } else {
                                    "empty_slot_$index"
                                }
                            }
                        ) { _, app ->
                            if (app != null) {
                                AppGridItem(
                                    app = app,
                                    onClick = { onAppClick(app) },
                                    onLongClick = { longPressedApp = app }
                                )
                            } else {
                                Spacer(modifier = Modifier.height(82.dp))
                            }
                        }
                    }
                }
            }

            // 2. 下部親指ゾーン: カテゴリタブ（設定でON/OFF・名称変更・順序変更可能）
            if (showCategoryBar) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    items(visibleCategories) { category ->
                        val isSelected = category == selectedCategory
                        val label = categoryCustomNames[category] ?: category.displayName
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.85f)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.12f)
                            ),
                            border = null,
                            shape = RoundedCornerShape(18.dp)
                        )
                    }
                }
            }

            // 3. 最下部: 検索バー ＋ 設定アイコン（くどい説明文を排除してシンプル化）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "アプリを検索",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "検索",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "クリア",
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    keyboardController?.hide()
                                    onClose()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "閉じる",
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.06f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                )

                IconButton(
                    onClick = {
                        keyboardController?.hide()
                        onOpenSettings()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "設定",
                        tint = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }

    // A案: アプリアイコン長押しメニュー（カテゴリ手動変更・非表示・アプリ情報）
    longPressedApp?.let { targetApp ->
        AlertDialog(
            onDismissRequest = { longPressedApp = null },
            containerColor = Color(0xFF16181E),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = {
                Text(
                    text = targetApp.label,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "所属カテゴリを変更",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF90CAF9)
                    )
                    categoryOrder.filter { it != AppCategory.ALL }.forEach { cat ->
                        val isCurrent = (targetApp.category == cat)
                        val catLabel = categoryCustomNames[cat] ?: cat.displayName
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrent) Color(0xFF2979FF).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .combinedClickable(
                                    onClick = {
                                        onChangeAppCategory(targetApp, cat)
                                        longPressedApp = null
                                    }
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = catLabel,
                                    fontSize = 14.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = Color.White
                                )
                                if (isCurrent) {
                                    Text(
                                        text = "現在",
                                        fontSize = 11.sp,
                                        color = Color(0xFF90CAF9)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .combinedClickable(
                                onClick = {
                                    onHideApp(targetApp)
                                    longPressedApp = null
                                }
                            )
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "ドロワーから非表示にする",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .combinedClickable(
                                onClick = {
                                    longPressedApp = null
                                    try {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", targetApp.packageName, null)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            )
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "アプリ情報・アンインストール",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { longPressedApp = null }) {
                    Text("閉じる", color = Color(0xFF90CAF9))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridItem(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(82.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        val iconBitmap = app.iconBitmap
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = app.label,
                modifier = Modifier
                    .size(50.dp)
                    .clip(HexagonShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(HexagonShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.label.take(1),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = app.label,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
