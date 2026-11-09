package com.example.adhdlauncher.ui.home

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.adhdlauncher.data.AppInfo
import com.example.adhdlauncher.data.ClockPosition
import com.example.adhdlauncher.data.DiscoverMode
import com.example.adhdlauncher.data.DisplayMode
import com.example.adhdlauncher.data.Handedness
import com.example.adhdlauncher.data.LauncherPreferences
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    preferences: LauncherPreferences,
    honeycombApps: List<AppInfo>,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    onAppClick: (AppInfo) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenQuickSearch: () -> Unit,
    onOpenFeed: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val anchorText by preferences.anchorText.collectAsState()
    val displayMode by preferences.displayMode.collectAsState()
    val handedness by preferences.handedness.collectAsState()
    val clockPosition by preferences.clockPosition.collectAsState()
    val gestureSwipeUpDrawer by preferences.gestureSwipeUpDrawer.collectAsState()
    val gestureSwipeDownSearch by preferences.gestureSwipeDownSearch.collectAsState()
    val gestureDoubleTapTrueBlack by preferences.gestureDoubleTapTrueBlack.collectAsState()
    val discoverMode by preferences.discoverMode.collectAsState()
    val honeycombEnabled by preferences.honeycombEnabled.collectAsState()
    val honeycombAppCount by preferences.honeycombAppCount.collectAsState()
    val homeWidgetId by preferences.homeWidgetId.collectAsState()
    val homeWidgetHeightDp by preferences.homeWidgetHeightDp.collectAsState()
    val idleDimEnabled by preferences.idleDimEnabled.collectAsState()

    var isTrueBlack by remember(displayMode) { mutableStateOf(preferences.isTrueBlackActive()) }

    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    // 分単位で効率的に更新（毎秒ループを廃止して省電力＆もっさり解消）
    LaunchedEffect(displayMode) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("M月d日 EEEE", Locale.JAPAN)
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now)
            if (displayMode == DisplayMode.AUTO_SCHEDULE) {
                isTrueBlack = preferences.isTrueBlackActive()
            }
            val cal = Calendar.getInstance()
            val delayMs = (60 - cal.get(Calendar.SECOND)) * 1000L - cal.get(Calendar.MILLISECOND)
            delay(delayMs.coerceAtLeast(1000L))
        }
    }

    var isEditingAnchor by remember { mutableStateOf(false) }
    var anchorInputText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current

    // 画面タップ座標を中心とした動的ハニカム展開状態
    var honeycombCenter by remember { mutableStateOf<Offset?>(null) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    // 無操作時の半透明化（Idle Dimming）制御
    var lastInteractionTick by remember { mutableIntStateOf(0) }
    var isIdleDimmed by remember { mutableStateOf(false) }

    LaunchedEffect(idleDimEnabled, lastInteractionTick, isEditingAnchor, honeycombCenter) {
        if (!idleDimEnabled || isEditingAnchor || honeycombCenter != null) {
            isIdleDimmed = false
        } else {
            isIdleDimmed = false
            delay(5000L) // 5秒間無操作なら半透明フェード
            isIdleDimmed = true
        }
    }

    val uiAlpha by animateFloatAsState(
        targetValue = if (isIdleDimmed) 0.24f else 1.0f,
        animationSpec = tween(durationMillis = if (isIdleDimmed) 650 else 150),
        label = "idleDimAlpha"
    )

    val backgroundColor = if (isTrueBlack) Color.Black else Color.Transparent

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it }
            .background(backgroundColor)
            // 画面内のあらゆるタッチを検知して半透明状態を即解除
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.pressed }) {
                            lastInteractionTick++
                        }
                    }
                }
            }
            // タップ座標取得・ダブルタップ・長押しジェスチャー
            .pointerInput(
                gestureDoubleTapTrueBlack,
                honeycombEnabled,
                isEditingAnchor,
                isTrueBlack
            ) {
                detectTapGestures(
                    onDoubleTap = if (gestureDoubleTapTrueBlack) {
                        {
                            honeycombCenter = null
                            val newMode = if (isTrueBlack) DisplayMode.WALLPAPER else DisplayMode.TRUE_BLACK
                            preferences.setDisplayMode(newMode)
                            isTrueBlack = !isTrueBlack
                        }
                    } else null,
                    onLongPress = {
                        honeycombCenter = null
                        onOpenSettings()
                    },
                    onTap = { tapOffset ->
                        if (isEditingAnchor) {
                            isEditingAnchor = false
                            focusManager.clearFocus()
                        } else if (honeycombCenter != null) {
                            // 展開中に背景をタップしたら閉じる
                            honeycombCenter = null
                        } else if (honeycombEnabled && honeycombApps.isNotEmpty()) {
                            // タップした指の座標を中心にハニカムショートカットを展開
                            honeycombCenter = tapOffset
                        }
                    }
                )
            }
            .pointerInput(gestureSwipeUpDrawer, gestureSwipeDownSearch) {
                var triggered = false
                detectVerticalDragGestures(
                    onDragStart = { triggered = false },
                    onVerticalDrag = { _, dragAmount ->
                        if (!triggered && dragAmount < -18f && gestureSwipeUpDrawer) {
                            triggered = true
                            honeycombCenter = null
                            onOpenDrawer()
                        } else if (!triggered && dragAmount > 18f && gestureSwipeDownSearch) {
                            triggered = true
                            honeycombCenter = null
                            onOpenQuickSearch()
                        }
                    }
                )
            }
            .pointerInput(discoverMode) {
                var hTriggered = false
                detectHorizontalDragGestures(
                    onDragStart = { hTriggered = false },
                    onHorizontalDrag = { _, dragAmount ->
                        if (!hTriggered && dragAmount > 28f && discoverMode != DiscoverMode.OFF) {
                            hTriggered = true
                            honeycombCenter = null
                            onOpenFeed()
                        }
                    }
                )
            }
            .systemBarsPadding()
            .imePadding()
    ) {
        // 1A. 時計＋上部ウィジェット領域（画面上部）
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 36.dp, start = 20.dp, end = 20.dp)
                .alpha(uiAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (clockPosition == ClockPosition.TOP_LARGE) {
                Text(
                    text = currentTime,
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White,
                    letterSpacing = (-2).sp
                )
                Text(
                    text = currentDate,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }

            // ホーム画面ウィジェット（KWGT / 時計 / At a Glance 等）
            if (homeWidgetId != -1) {
                HomeWidgetContainer(
                    appWidgetId = homeWidgetId,
                    widgetHeightDp = homeWidgetHeightDp,
                    appWidgetHost = appWidgetHost,
                    appWidgetManager = appWidgetManager,
                    onInvalidWidget = { preferences.setHomeWidgetId(-1) }
                )
            }
        }

        // 下部エルゴノミクス領域（親指が届く範囲）
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .alpha(uiAlpha),
            horizontalAlignment = if (handedness == Handedness.RIGHT) Alignment.End else Alignment.Start
        ) {
            // 1B. 時計（画面下部に寄せて表示：設定切替時）
            if (clockPosition == ClockPosition.BOTTOM_COMPACT) {
                Column(
                    horizontalAlignment = if (handedness == Handedness.RIGHT) Alignment.End else Alignment.Start
                ) {
                    Text(
                        text = currentTime,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = currentDate,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                }
            }

            // 2. 1行アンカー（Anti-Dopamine Anchor: 今やることメモ）
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.62f),
                contentColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditingAnchor) {
                        BasicTextField(
                            value = anchorInputText,
                            onValueChange = { anchorInputText = it },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(Color.White),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                preferences.updateAnchorText(anchorInputText)
                                isEditingAnchor = false
                                focusManager.clearFocus()
                            }),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                        )
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                        IconButton(
                            onClick = {
                                preferences.updateAnchorText(anchorInputText)
                                isEditingAnchor = false
                                focusManager.clearFocus()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "確定",
                                tint = Color(0xFF81C784)
                            )
                        }
                    } else {
                        val hasAnchor = anchorText.isNotBlank()
                        Text(
                            text = if (hasAnchor) anchorText else "＋ 今スマホを開いた目的をメモ...",
                            fontSize = 15.sp,
                            fontWeight = if (hasAnchor) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (hasAnchor) Color.White else Color.White.copy(alpha = 0.65f),
                            modifier = Modifier
                                .weight(1f)
                                .combinedClickable(
                                    onClick = {
                                        honeycombCenter = null
                                        anchorInputText = anchorText
                                        isEditingAnchor = true
                                    }
                                )
                        )
                        if (hasAnchor) {
                            IconButton(
                                onClick = { preferences.clearAnchorText() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "完了・消去",
                                    tint = Color.White.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }
                }
            }

            // 3. 下部検索バー兼ドロワー引き手 ＋ 設定ボタン
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    contentColor = Color.White,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .combinedClickable(
                            onClick = {
                                honeycombCenter = null
                                onOpenDrawer()
                            }
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "検索",
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "アプリを検索 / ドロワー",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.85f))
                        )
                    }
                }

                // 設定ボタン（親指でワンタップ）
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.72f),
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = {
                                honeycombCenter = null
                                onOpenSettings()
                            }
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "設定を開く",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // 4. タップ座標を中心とした動的ハニカムショートカットポップアップ
        val activeCenter = honeycombCenter
        AnimatedVisibility(
            visible = activeCenter != null && honeycombApps.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(110)) + scaleIn(
                initialScale = 0.78f,
                animationSpec = tween(130, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(90)) + scaleOut(
                targetScale = 0.85f,
                animationSpec = tween(90)
            )
        ) {
            if (activeCenter != null) {
                val displayApps = honeycombApps.take(honeycombAppCount.coerceIn(3, 12))
                val hasOuterRing = displayApps.size > 7
                val safeMarginPx = with(density) { (if (hasOuterRing) 158.dp else 108.dp).toPx() }
                val clampedX = if (boxSize.width > safeMarginPx * 2) {
                    activeCenter.x.coerceIn(safeMarginPx, boxSize.width - safeMarginPx)
                } else {
                    (boxSize.width / 2f).coerceAtLeast(activeCenter.x)
                }
                val clampedY = if (boxSize.height > safeMarginPx * 2) {
                    activeCenter.y.coerceIn(safeMarginPx, boxSize.height - safeMarginPx)
                } else {
                    (boxSize.height / 2f).coerceAtLeast(activeCenter.y)
                }

                HoneycombPopupOverlay(
                    centerX = clampedX,
                    centerY = clampedY,
                    apps = displayApps,
                    onSelectApp = { selected ->
                        honeycombCenter = null
                        onAppClick(selected)
                    },
                    onDismiss = { honeycombCenter = null }
                )
            }
        }
    }
}

@Composable
private fun HoneycombPopupOverlay(
    centerX: Float,
    centerY: Float,
    apps: List<AppInfo>,
    onSelectApp: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val nodeSizeDp = 64.dp
    val nodeHalfPx = with(density) { (nodeSizeDp / 2).toPx() }
    val ring1RadiusPx = with(density) { 74.dp.toPx() }
    val ring2RadiusPx = with(density) { 134.dp.toPx() }
    val backdropRadiusDp = if (apps.size > 7) 168.dp else 112.dp
    val backdropRadiusPx = with(density) { backdropRadiusDp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        // 半透明の円形バックドロップ（指の座標を中心に展開）
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (centerX - backdropRadiusPx).roundToInt(),
                        y = (centerY - backdropRadiusPx).roundToInt()
                    )
                }
                .size(backdropRadiusDp * 2)
                .clip(CircleShape)
                .background(Color(0xD9101411))
                .border(1.dp, Color(0xFF94A88B).copy(alpha = 0.35f), CircleShape)
        )

        apps.forEachIndexed { index, app ->
            val (dx, dy) = when {
                index == 0 -> 0f to 0f
                index in 1..6 -> {
                    val countInRing1 = (apps.size - 1).coerceAtMost(6)
                    val stepDeg = 360.0 / countInRing1.coerceAtLeast(1)
                    val angleRad = Math.toRadians((index - 1) * stepDeg - 90.0)
                    (ring1RadiusPx * cos(angleRad)).toFloat() to (ring1RadiusPx * sin(angleRad)).toFloat()
                }
                else -> {
                    val outerCount = (apps.size - 7).coerceAtLeast(1)
                    val outerIdx = index - 7
                    val stepDeg = 360.0 / outerCount
                    // 内側リングの間（+30度オフセット）にハニカム配置
                    val angleRad = Math.toRadians(outerIdx * stepDeg - 60.0)
                    (ring2RadiusPx * cos(angleRad)).toFloat() to (ring2RadiusPx * sin(angleRad)).toFloat()
                }
            }

            val isCenterNode = index == 0
            Column(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (centerX + dx - nodeHalfPx).roundToInt(),
                            y = (centerY + dy - nodeHalfPx).roundToInt()
                        )
                    }
                    .size(nodeSizeDp)
                    .clip(CircleShape)
                    .background(
                        if (isCenterNode) Color(0xFF28322A) else Color(0xCC1B201C)
                    )
                    .border(
                        width = if (isCenterNode) 1.5.dp else 1.dp,
                        color = if (isCenterNode) Color(0xFF94A88B) else Color.White.copy(alpha = 0.18f),
                        shape = CircleShape
                    )
                    .clickable { onSelectApp(app) }
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (app.iconBitmap != null) {
                    Image(
                        bitmap = app.iconBitmap,
                        contentDescription = app.label,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3A4138)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = app.label.take(1),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = app.label,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun HomeWidgetContainer(
    appWidgetId: Int,
    widgetHeightDp: Int,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    onInvalidWidget: () -> Unit
) {
    val context = LocalContext.current
    val providerInfo = remember(appWidgetId) {
        try {
            appWidgetManager.getAppWidgetInfo(appWidgetId)
        } catch (_: Exception) {
            null
        }
    }

    if (providerInfo == null) {
        LaunchedEffect(appWidgetId) {
            onInvalidWidget()
        }
        return
    }

    AndroidView(
        factory = { ctx ->
            appWidgetHost.createView(ctx.applicationContext, appWidgetId, providerInfo).apply {
                setAppWidget(appWidgetId, providerInfo)
                val options = Bundle().apply {
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 400)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, widgetHeightDp)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, widgetHeightDp)
                }
                try {
                    updateAppWidgetOptions(options)
                } catch (_: Exception) {}
            }
        },
        update = { hostView ->
            val options = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 400)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, widgetHeightDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, widgetHeightDp)
            }
            try {
                hostView.updateAppWidgetOptions(options)
            } catch (_: Exception) {}
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(widgetHeightDp.dp)
            .clip(RoundedCornerShape(16.dp))
    )
}
