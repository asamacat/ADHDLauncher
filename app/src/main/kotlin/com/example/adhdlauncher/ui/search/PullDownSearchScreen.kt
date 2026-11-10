package com.example.adhdlauncher.ui.search

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shop
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adhdlauncher.data.AppInfo
import com.example.adhdlauncher.data.Handedness
import com.example.adhdlauncher.data.JapaneseSearchMatcher
import com.example.adhdlauncher.data.LauncherPreferences
import com.example.adhdlauncher.ui.util.HexagonShape
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PullDownSearchScreen(
    apps: List<AppInfo>,
    preferences: LauncherPreferences,
    onAppClick: (AppInfo) -> Unit,
    onOpenFullDrawer: () -> Unit,
    onOpenSearchSettings: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onClose()
    }

    val context = LocalContext.current
    val searchProvider by preferences.searchProvider.collectAsState()
    val includeHidden by preferences.searchIncludeHiddenApps.collectAsState()
    val startAppsCount by preferences.searchStartAppsCount.collectAsState()
    val enableWebSuggest by preferences.searchWebSuggest.collectAsState()
    val enablePlayStore by preferences.searchPlayStore.collectAsState()
    val handedness by preferences.handedness.collectAsState()
    val aiQuickAnswerEnabled by preferences.aiQuickAnswerEnabled.collectAsState()
    val aiApiKey by preferences.aiApiKey.collectAsState()

    var query by remember { mutableStateOf("") }
    var showAllAppsInSearch by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // 開いた瞬間に検索バーへフォーカスしてキーボードを展開
    LaunchedEffect(Unit) {
        delay(60)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val candidateApps = remember(apps, includeHidden) {
        val base = if (includeHidden) apps else apps.filter { !it.isHidden }
        base.sortedWith(
            compareByDescending<AppInfo> { it.launchCount }
                .thenByDescending { it.lastLaunchTime }
                .thenByDescending { it.systemUsageScore }
                .thenBy { it.label.lowercase() }
        )
    }

    // 課題3修正: 右利き設定時は4つのうち最もよく使う1位が右端に来るよう行単位で反転
    val displayedApps = remember(candidateApps, query, startAppsCount, showAllAppsInSearch, handedness) {
        val raw = if (query.isBlank()) {
            if (showAllAppsInSearch) {
                candidateApps
            } else {
                candidateApps.take(startAppsCount)
            }
        } else {
            candidateApps.filter { JapaneseSearchMatcher.matches(it, query) }
        }

        if (handedness == Handedness.RIGHT) {
            raw.chunked(4).flatMap { it.reversed() }
        } else {
            raw
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0E131A).copy(alpha = 0.94f))
            .pointerInput(showAllAppsInSearch) {
                var upwardDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = { upwardDrag = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        upwardDrag += dragAmount
                        if (upwardDrag < -55f && !showAllAppsInSearch) {
                            upwardDrag = 0f
                            keyboardController?.hide()
                            onClose()
                        }
                    }
                )
            }
            .systemBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // 1. 上部空白タップで閉じる（下スワイプ時もアプリアイコンと検索バーを親指が届く画面下部へ寄せる）
            if (!showAllAppsInSearch) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                keyboardController?.hide()
                                onClose()
                            }
                        )
                )
            }

            // 2. 検索キーワード入力時の AIクイックアンサー枠 & Webサジェスト & Playストア検索アクション
            if (query.isNotBlank()) {
                if (aiQuickAnswerEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                keyboardController?.hide()
                                if (aiApiKey.isBlank()) {
                                    onOpenSearchSettings()
                                }
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF94A88B),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "AIに「$query」を即答させる",
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (aiApiKey.isBlank()) "設定からAPIキーを登録すると利用できます" else "API連携枠（準備完了）",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CallMade,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (enableWebSuggest) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                keyboardController?.hide()
                                val uri = Uri.parse(searchProvider.queryUrlPrefix + Uri.encode(query))
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF82B1FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "「$query」を ${searchProvider.label} で検索",
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CallMade,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (enablePlayStore) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                keyboardController?.hide()
                                val uri = Uri.parse("https://play.google.com/store/search?q=${Uri.encode(query)}&c=apps")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shop,
                                contentDescription = null,
                                tint = Color(0xFF81C784),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Playストアで「$query」を検索",
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CallMade,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 3. セクションヘッダー: 左「よく使うアプリ」 / 右「すべて表示」
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        query.isNotBlank() -> "一致するアプリ (${displayedApps.size})"
                        showAllAppsInSearch -> "すべてのアプリ (${displayedApps.size})"
                        else -> "よく使うアプリ"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.5f)
                )
                if (query.isBlank()) {
                    Text(
                        text = if (showAllAppsInSearch) "折りたたむ" else "すべて表示",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                showAllAppsInSearch = !showAllAppsInSearch
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. 下部親指ゾーン：よく使うアプリ4つ（または検索ヒット一覧）
            if (displayedApps.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    reverseLayout = showAllAppsInSearch,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = if (showAllAppsInSearch) {
                        Modifier.weight(1f)
                    } else {
                        Modifier.wrapContentHeight()
                    }
                ) {
                    items(
                        items = displayedApps,
                        key = { "${it.packageName}:${it.activityName}" }
                    ) { app ->
                        SearchAppTile(
                            app = app,
                            onClick = {
                                keyboardController?.hide()
                                onAppClick(app)
                            }
                        )
                    }
                }
            } else {
                Text(
                    text = "一致するアプリはありません",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. 最下部（キーボード直上）: ピル型検索バー ＋ 右端「⋮」メニューボタン
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White.copy(alpha = 0.14f),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "アプリを検索",
                                    fontSize = 15.sp,
                                    color = Color.White.copy(alpha = 0.55f)
                                )
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 15.sp
                                ),
                                cursorBrush = SolidColor(Color(0xFF82B1FF)),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        if (displayedApps.isNotEmpty()) {
                                            keyboardController?.hide()
                                            val target = if (handedness == Handedness.RIGHT) displayedApps.last() else displayedApps.first()
                                            onAppClick(target)
                                        } else if (query.isNotBlank()) {
                                            val uri = Uri.parse(searchProvider.queryUrlPrefix + Uri.encode(query))
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        }
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                        }
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { query = "" },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "クリア",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        keyboardController?.hide()
                        onOpenSearchSettings()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "検索ページ設定",
                        tint = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchAppTile(
    app: AppInfo,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        val bitmap = app.iconBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = app.label,
                modifier = Modifier
                    .size(56.dp)
                    .clip(HexagonShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(HexagonShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.label.take(1),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = app.label,
            fontSize = 12.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
