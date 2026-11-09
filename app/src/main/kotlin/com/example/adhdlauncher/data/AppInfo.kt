package com.example.adhdlauncher.data

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

enum class AppCategory(val displayName: String) {
    ALL("すべて"),
    COMMUNICATION("連絡・SNS"),
    FINANCE("決済・生活"),
    INTERNET("ネット・情報"),
    MEDIA("メディア"),
    GAMES("ゲーム"),
    TOOLS("ツール・設定")
}

@Immutable
data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val iconBitmap: ImageBitmap?,
    val category: AppCategory,
    val launchCount: Int = 0,
    val lastLaunchTime: Long = 0L,
    val systemUsageScore: Long = 0L,
    val isHidden: Boolean = false
)
