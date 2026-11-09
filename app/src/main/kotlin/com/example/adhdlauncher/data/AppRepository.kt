package com.example.adhdlauncher.data

import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.adhdlauncher.ui.util.toImageBitmapOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val usagePrefs = context.getSharedPreferences("adhd_app_usage", Context.MODE_PRIVATE)
    private var cachedApps: List<AppInfo>? = null

    private val usageComparator = compareByDescending<AppInfo> { it.launchCount }
        .thenByDescending { it.lastLaunchTime }
        .thenByDescending { it.systemUsageScore }
        .thenBy { it.label.lowercase() }

    private fun querySystemUsageScores(): Map<String, Long> {
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyMap()
            val now = System.currentTimeMillis()
            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_MONTHLY,
                now - 30L * 24 * 60 * 60 * 1000L,
                now
            )
            stats?.associate { it.packageName to (it.totalTimeInForeground + (it.lastTimeUsed / 100_000L)) }
                ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun getInstalledApps(forceRefresh: Boolean = false): List<AppInfo> = withContext(Dispatchers.Default) {
        if (!forceRefresh && cachedApps != null) {
            return@withContext cachedApps!!
        }

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        AppCategoryClassifier.ensureLoaded(context)

        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
        val myPackageName = context.packageName
        val systemUsageMap = querySystemUsageScores()

        val apps = resolveInfos
            .filter { it.activityInfo.packageName != myPackageName }
            .map { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo
                val pkg = activityInfo.packageName
                val label = resolveInfo.loadLabel(packageManager).toString()
                val iconBitmap = resolveInfo.loadIcon(packageManager).toImageBitmapOrNull(144)

                // 手動カテゴリ設定があれば優先、なければ自動分類
                val customCatName = usagePrefs.getString("cat_$pkg", null)
                val category = if (customCatName != null) {
                    runCatching { AppCategory.valueOf(customCatName) }.getOrNull()
                        ?: AppCategoryClassifier.classify(pkg, label, activityInfo.applicationInfo)
                } else {
                    AppCategoryClassifier.classify(pkg, label, activityInfo.applicationInfo)
                }

                val isHidden = usagePrefs.getBoolean("hidden_$pkg", false)
                val count = usagePrefs.getInt("count_$pkg", usagePrefs.getInt(pkg, 0))
                val lastTime = usagePrefs.getLong("time_$pkg", 0L)
                val sysScore = systemUsageMap[pkg] ?: 0L

                AppInfo(
                    packageName = pkg,
                    activityName = activityInfo.name,
                    label = label,
                    iconBitmap = iconBitmap,
                    category = category,
                    launchCount = count,
                    lastLaunchTime = lastTime,
                    systemUsageScore = sysScore,
                    isHidden = isHidden
                )
            }
            .sortedWith(usageComparator)

        cachedApps = apps
        apps
    }

    fun launchApp(app: AppInfo): List<AppInfo> {
        val newCount = app.launchCount + 1
        val now = System.currentTimeMillis()
        usagePrefs.edit()
            .putInt("count_${app.packageName}", newCount)
            .putInt(app.packageName, newCount)
            .putLong("time_${app.packageName}", now)
            .apply()

        val updatedList = cachedApps?.map {
            if (it.packageName == app.packageName && it.activityName == app.activityName) {
                it.copy(launchCount = newCount, lastLaunchTime = now)
            } else {
                it
            }
        }?.sortedWith(usageComparator) ?: emptyList()

        cachedApps = updatedList

        val intent = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName(app.packageName, app.activityName)
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // アンインストール直後等で起動できない場合はキャッシュから除外してクラッシュを防ぐ
            val filtered = updatedList.filterNot { it.packageName == app.packageName && it.activityName == app.activityName }
            cachedApps = filtered
            return filtered
        }
        return updatedList
    }

    fun setAppCategory(packageName: String, category: AppCategory): List<AppInfo> {
        usagePrefs.edit().putString("cat_$packageName", category.name).apply()
        val updated = cachedApps?.map {
            if (it.packageName == packageName) {
                it.copy(category = category)
            } else {
                it
            }
        } ?: emptyList()
        cachedApps = updated
        return updated
    }

    fun setAppHidden(packageName: String, hidden: Boolean): List<AppInfo> {
        usagePrefs.edit().putBoolean("hidden_$packageName", hidden).apply()
        val updated = cachedApps?.map {
            if (it.packageName == packageName) {
                it.copy(isHidden = hidden)
            } else {
                it
            }
        } ?: emptyList()
        cachedApps = updated
        return updated
    }

    fun unhideAllApps(): List<AppInfo> {
        val editor = usagePrefs.edit()
        cachedApps?.forEach { app ->
            if (app.isHidden) {
                editor.remove("hidden_${app.packageName}")
            }
        }
        editor.apply()
        val updated = cachedApps?.map { it.copy(isHidden = false) } ?: emptyList()
        cachedApps = updated
        return updated
    }

    fun resetUsageHistory(): List<AppInfo> {
        usagePrefs.edit().clear().apply()
        val updated = cachedApps?.map {
            it.copy(launchCount = 0, lastLaunchTime = 0L, isHidden = false)
        }?.sortedWith(usageComparator) ?: emptyList()
        cachedApps = updated
        return updated
    }
}
