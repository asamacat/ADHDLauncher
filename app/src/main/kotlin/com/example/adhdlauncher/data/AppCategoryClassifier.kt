package com.example.adhdlauncher.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import org.json.JSONObject

object AppCategoryClassifier {

    @Volatile
    private var isLoaded = false
    private val packageRules = mutableMapOf<String, AppCategory>()
    private val categoryKeywords = mutableMapOf<AppCategory, List<String>>()
    private val searchAliases = mutableMapOf<String, String>()

    fun ensureLoaded(context: Context) {
        if (isLoaded) return
        synchronized(this) {
            if (isLoaded) return
            try {
                val jsonText = context.assets
                    .open("dictionaries/app_dictionary.json")
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
                val root = JSONObject(jsonText)

                // 1. package_rules の読み込み
                root.optJSONObject("package_rules")?.let { pkgObj ->
                    for (catKey in pkgObj.keys()) {
                        val category = runCatching { AppCategory.valueOf(catKey) }.getOrNull() ?: continue
                        val arr = pkgObj.optJSONArray(catKey) ?: continue
                        for (i in 0 until arr.length()) {
                            val pkg = arr.optString(i)
                            if (pkg.isNotBlank()) {
                                packageRules[pkg] = category
                            }
                        }
                    }
                }

                // 2. category_keywords の読み込み
                root.optJSONObject("category_keywords")?.let { kwObj ->
                    for (catKey in kwObj.keys()) {
                        val category = runCatching { AppCategory.valueOf(catKey) }.getOrNull() ?: continue
                        val arr = kwObj.optJSONArray(catKey) ?: continue
                        val list = mutableListOf<String>()
                        for (i in 0 until arr.length()) {
                            val kw = arr.optString(i).lowercase()
                            if (kw.isNotBlank()) {
                                list.add(kw)
                            }
                        }
                        categoryKeywords[category] = list
                    }
                }

                // 3. search_aliases の読み込み
                root.optJSONObject("search_aliases")?.let { aliasObj ->
                    for (key in aliasObj.keys()) {
                        val value = aliasObj.optString(key)
                        if (key.isNotBlank() && value.isNotBlank()) {
                            searchAliases[key] = value
                        }
                    }
                }

                isLoaded = true
            } catch (_: Exception) {
                isLoaded = true
            }
        }
    }

    fun getSearchAliases(): Map<String, String> = searchAliases

    fun classify(pkgName: String, label: String, appInfo: ApplicationInfo?): AppCategory {
        // 1. JSON辞書のパッケージ名完全一致
        packageRules[pkgName]?.let { return it }

        // 2. JSON辞書のキーワード部分一致（多言語・各国語拡張対応）
        val lowerPkg = pkgName.lowercase()
        val lowerLabel = label.lowercase()

        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { kw -> lowerPkg.contains(kw) || lowerLabel.contains(kw) }) {
                return category
            }
        }

        // 3. Android標準 ApplicationInfo.category (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appInfo != null) {
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.COMMUNICATION
                ApplicationInfo.CATEGORY_NEWS -> return AppCategory.INTERNET
                ApplicationInfo.CATEGORY_VIDEO,
                ApplicationInfo.CATEGORY_AUDIO,
                ApplicationInfo.CATEGORY_IMAGE -> return AppCategory.MEDIA
                ApplicationInfo.CATEGORY_GAME -> return AppCategory.GAMES
                ApplicationInfo.CATEGORY_PRODUCTIVITY,
                ApplicationInfo.CATEGORY_MAPS -> return AppCategory.TOOLS
            }
        }

        return AppCategory.TOOLS
    }
}
