package com.example.adhdlauncher.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adhdlauncher.data.ClockPosition
import com.example.adhdlauncher.data.DiscoverMode
import com.example.adhdlauncher.data.DisplayMode
import com.example.adhdlauncher.data.Handedness
import com.example.adhdlauncher.data.LauncherPreferences
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    preferences: LauncherPreferences,
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

    val backgroundColor = if (isTrueBlack) Color.Black else Color.Transparent

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .combinedClickable(
                onDoubleClick = {
                    if (gestureDoubleTapTrueBlack) {
                        val newMode = if (isTrueBlack) DisplayMode.WALLPAPER else DisplayMode.TRUE_BLACK
                        preferences.setDisplayMode(newMode)
                        isTrueBlack = !isTrueBlack
                    }
                },
                onLongClick = {
                    // ホーム長押しでも設定画面を即座に開ける
                    onOpenSettings()
                },
                onClick = {
                    if (isEditingAnchor) {
                        isEditingAnchor = false
                        focusManager.clearFocus()
                    }
                }
            )
            .pointerInput(gestureSwipeUpDrawer, gestureSwipeDownSearch) {
                var triggered = false
                detectVerticalDragGestures(
                    onDragStart = { triggered = false },
                    onVerticalDrag = { _, dragAmount ->
                        if (!triggered && dragAmount < -18f && gestureSwipeUpDrawer) {
                            triggered = true
                            onOpenDrawer()
                        } else if (!triggered && dragAmount > 18f && gestureSwipeDownSearch) {
                            triggered = true
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
                            onOpenFeed()
                        }
                    }
                )
            }
            .systemBarsPadding()
            .imePadding()
    ) {
        // 1A. 時計（画面上部に大きく表示：デフォルト）
        if (clockPosition == ClockPosition.TOP_LARGE) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 44.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentTime,
                    fontSize = 78.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White,
                    letterSpacing = (-2).sp
                )
                Text(
                    text = currentDate,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        // 下部エルゴノミクス領域（親指が届く範囲）
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
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
                            onClick = onOpenDrawer
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
                        .combinedClickable(onClick = onOpenSettings)
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
    }
}
