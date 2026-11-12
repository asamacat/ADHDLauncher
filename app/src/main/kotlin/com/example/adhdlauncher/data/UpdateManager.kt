package com.example.adhdlauncher.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.adhdlauncher.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val assetName: String
    ) : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class Downloading(val progress: Float) : UpdateStatus()
    data class Downloaded(val apkFile: File) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

object VersionComparator {
    fun isNewer(remote: String, current: String): Boolean {
        val cleanRemote = cleanVersion(remote)
        val cleanCurrent = cleanVersion(current)

        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    fun cleanVersion(v: String): String {
        return v.trim()
            .removePrefix("v")
            .removePrefix("V")
            .split("-")[0]
            .trim()
    }
}

class UpdateManager(
    private val context: Context,
    private val preferences: LauncherPreferences
) {
    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    private val repoApiUrl = "https://api.github.com/repos/asamacat/ADHDLauncher/releases/latest"

    /**
     * 最新の GitHub Release を照会し、更新が存在するか確認する。
     * Zero-tracking invariant: 端末識別子、位置情報、利用履歴等のテレメトリは一切送信しません。
     *
     * @param force true の場合は 24 時間レートリミットガードをバイパスして即座に GitHub API を確認
     */
    suspend fun checkForUpdates(force: Boolean = false): UpdateStatus = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastCheck = preferences.getLastUpdateCheckTime()
        val twentyFourHours = 24L * 60 * 60 * 1000L

        if (!force && (now - lastCheck in 0 until twentyFourHours)) {
            if (_status.value == UpdateStatus.Idle) {
                _status.value = UpdateStatus.UpToDate
            }
            return@withContext _status.value
        }

        _status.value = UpdateStatus.Checking

        try {
            val connection = (URL(repoApiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "ADHDLauncher-App")
                connectTimeout = 12000
                readTimeout = 15000
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val err = "GitHub API エラー (HTTP $responseCode)"
                val errorStatus = UpdateStatus.Error(err)
                _status.value = errorStatus
                return@withContext errorStatus
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            preferences.setLastUpdateCheckTime(now)

            val json = JSONObject(responseText)
            val tagName = json.optString("tag_name", "")
            val releaseNotes = json.optString("body", "")
            val assets = json.optJSONArray("assets")

            val currentVersion = BuildConfig.VERSION_NAME

            var apkUrl: String? = null
            var apkName: String? = null

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        apkName = name
                        break
                    }
                }
            }

            val isNewer = VersionComparator.isNewer(tagName, currentVersion)
            val newStatus = if (isNewer && !apkUrl.isNullOrBlank()) {
                UpdateStatus.UpdateAvailable(
                    currentVersion = currentVersion,
                    latestVersion = tagName,
                    releaseNotes = releaseNotes,
                    downloadUrl = apkUrl,
                    assetName = apkName ?: "ADHDLauncher-latest.apk"
                )
            } else {
                UpdateStatus.UpToDate
            }

            _status.value = newStatus
            newStatus
        } catch (e: Exception) {
            val errStatus = UpdateStatus.Error("更新確認に失敗しました: ${e.localizedMessage ?: e.message}")
            _status.value = errStatus
            errStatus
        }
    }

    /**
     * GitHub Releases から APK をダウンロードし、context.cacheDir/updates/ADHDLauncher-latest.apk に保存する
     */
    suspend fun downloadApk(
        downloadUrl: String,
        onProgress: (Float) -> Unit = {}
    ): File? = withContext(Dispatchers.IO) {
        _status.value = UpdateStatus.Downloading(0f)
        var tempFile: File? = null
        try {
            val updateDir = File(context.cacheDir, "updates")
            if (!updateDir.exists()) {
                updateDir.mkdirs()
            }
            val apkFile = File(updateDir, "ADHDLauncher-latest.apk")
            tempFile = File(updateDir, "ADHDLauncher-latest.apk.tmp")
            if (tempFile.exists()) {
                tempFile.delete()
            }

            val connection = openConnectionFollowingRedirects(downloadUrl)
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw java.io.IOException("HTTP $code ${connection.responseMessage}")
            }
            val contentLength = connection.contentLengthLong

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastReported = -1

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (contentLength > 0L) {
                            val progress = (totalRead.toFloat() / contentLength).coerceIn(0f, 1f)
                            val percent = (progress * 100).toInt()
                            if (percent != lastReported) {
                                lastReported = percent
                                _status.value = UpdateStatus.Downloading(progress)
                                onProgress(progress)
                            }
                        }
                    }
                    output.flush()
                }
            }

            if (apkFile.exists()) {
                apkFile.delete()
            }
            if (!tempFile.renameTo(apkFile)) {
                tempFile.copyTo(apkFile, overwrite = true)
                tempFile.delete()
            }

            val result = UpdateStatus.Downloaded(apkFile)
            _status.value = result
            apkFile
        } catch (e: Exception) {
            tempFile?.delete()
            _status.value = UpdateStatus.Error("ダウンロードに失敗しました: ${e.localizedMessage ?: e.message}")
            null
        }
    }

    /**
     * ダウンロード完了した APK のインストール Intent を発行する
     */
    fun installApk(targetContext: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) {
                _status.value = UpdateStatus.Error("インストールファイルが見つかりません")
                return
            }

            // Android 8.0 (Oreo) 以降の「不明なアプリのインストール」権限チェック
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!targetContext.packageManager.canRequestPackageInstalls()) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${targetContext.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    targetContext.startActivity(intent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                targetContext,
                "${targetContext.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            targetContext.startActivity(installIntent)
        } catch (e: Exception) {
            _status.value = UpdateStatus.Error("インストーラの起動に失敗しました: ${e.localizedMessage ?: e.message}")
        }
    }

    private fun openConnectionFollowingRedirects(initialUrl: String): HttpURLConnection {
        var currentUrl = URL(initialUrl)
        var redirects = 0
        while (redirects < 6) {
            val conn = currentUrl.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "ADHDLauncher-App")
            conn.connectTimeout = 15000
            conn.readTimeout = 30000

            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_MOVED_TEMP ||
                code == HttpURLConnection.HTTP_MOVED_PERM ||
                code == 307 || code == 308
            ) {
                val newLocation = conn.getHeaderField("Location")
                conn.disconnect()
                if (newLocation != null) {
                    currentUrl = URL(currentUrl, newLocation)
                    redirects++
                    continue
                }
            }
            return conn
        }
        throw IllegalStateException("リダイレクトが多すぎます (5回超過)")
    }
}
