package com.example.adhdlauncher.data

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JapaneseSearchMatcherTest {

    private fun createApp(
        label: String,
        packageName: String = "com.example.test",
        category: AppCategory = AppCategory.TOOLS
    ): AppInfo {
        return AppInfo(
            packageName = packageName,
            activityName = "com.example.test.MainActivity",
            label = label,
            iconBitmap = null,
            category = category
        )
    }

    @Before
    fun setUp() {
        AppCategoryClassifier.resetForTesting()
    }

    @After
    fun tearDown() {
        AppCategoryClassifier.resetForTesting()
    }

    @Test
    fun matches_emptyOrBlankQuery_returnsTrue() {
        val app = createApp("Camera")
        assertTrue(JapaneseSearchMatcher.matches(app, ""))
        assertTrue(JapaneseSearchMatcher.matches(app, "   "))
    }

    @Test
    fun matches_directLabelMatch_caseInsensitive() {
        val app = createApp("YouTube", "com.google.android.youtube")
        assertTrue(JapaneseSearchMatcher.matches(app, "youtube"))
        assertTrue(JapaneseSearchMatcher.matches(app, "YOUTUBE"))
        assertTrue(JapaneseSearchMatcher.matches(app, "You"))
        assertTrue(JapaneseSearchMatcher.matches(app, "tube"))
        assertFalse(JapaneseSearchMatcher.matches(app, "discord"))
    }

    @Test
    fun matches_packageNameMatch() {
        val app = createApp("Twitter", "com.twitter.android")
        assertTrue(JapaneseSearchMatcher.matches(app, "twitter"))
        assertTrue(JapaneseSearchMatcher.matches(app, "android"))
        assertTrue(JapaneseSearchMatcher.matches(app, "com.tw"))
    }

    @Test
    fun matches_katakanaLabel_hiraganaQuery() {
        val app = createApp("カメラ")
        assertTrue(JapaneseSearchMatcher.matches(app, "かめら"))
        assertTrue(JapaneseSearchMatcher.matches(app, "かめ"))
        assertTrue(JapaneseSearchMatcher.matches(app, "カメラ"))
        assertFalse(JapaneseSearchMatcher.matches(app, "しゃしん"))
    }

    @Test
    fun matches_katakanaLabel_romajiQuery() {
        val cameraApp = createApp("カメラ")
        assertTrue(JapaneseSearchMatcher.matches(cameraApp, "kamera"))
        assertTrue(JapaneseSearchMatcher.matches(cameraApp, "kame"))

        val radioApp = createApp("ラジオ")
        assertTrue(JapaneseSearchMatcher.matches(radioApp, "rajio"))

        val memoApp = createApp("メモ帳")
        assertTrue(JapaneseSearchMatcher.matches(memoApp, "memo"))
    }

    @Test
    fun matches_sokuonRomajiConversion() {
        val chatApp = createApp("チャット")
        assertTrue(JapaneseSearchMatcher.matches(chatApp, "chatto"))

        val messageApp = createApp("メッセージ")
        assertTrue(JapaneseSearchMatcher.matches(messageApp, "messe-ji"))

        val stampApp = createApp("キッテ")
        assertTrue(JapaneseSearchMatcher.matches(stampApp, "kitte"))
    }

    @Test
    fun matches_youonRomajiConversion() {
        val newsApp = createApp("ニュース")
        assertTrue(JapaneseSearchMatcher.matches(newsApp, "nyu"))

        val photoApp = createApp("シャシン")
        assertTrue(JapaneseSearchMatcher.matches(photoApp, "shashin"))

        val travelApp = createApp("リョコウ")
        assertTrue(JapaneseSearchMatcher.matches(travelApp, "ryoko"))

        val trainApp = createApp("デンシャ")
        assertTrue(JapaneseSearchMatcher.matches(trainApp, "densha"))
    }

    @Test
    fun matches_hatsuonRomajiConversion() {
        val mangaApp = createApp("マンガ")
        assertTrue(JapaneseSearchMatcher.matches(mangaApp, "manga"))

        val guideApp = createApp("アンナイ")
        assertTrue(JapaneseSearchMatcher.matches(guideApp, "annai"))

        val japanApp = createApp("ニホン")
        assertTrue(JapaneseSearchMatcher.matches(japanApp, "nihon"))
    }

    @Test
    fun matches_searchAliasFromClassifier() {
        AppCategoryClassifier.loadFromJson(
            """
            {
              "package_rules": {},
              "category_keywords": {},
              "search_aliases": {
                "写真": "camera",
                "電卓": "calculator"
              }
            }
            """.trimIndent()
        )

        val photoApp = createApp("写真アルバム")
        // "写真" has alias "camera", so querying "camera" should match
        assertTrue(JapaneseSearchMatcher.matches(photoApp, "camera"))
        assertTrue(JapaneseSearchMatcher.matches(photoApp, "写真"))

        val calcApp = createApp("電卓プラス")
        assertTrue(JapaneseSearchMatcher.matches(calcApp, "calculator"))
        assertTrue(JapaneseSearchMatcher.matches(calcApp, "電卓"))
        assertFalse(JapaneseSearchMatcher.matches(calcApp, "camera"))
    }

    @Test
    fun matches_unrelatedQuery_returnsFalse() {
        val app = createApp("設定", "com.android.settings")
        assertFalse(JapaneseSearchMatcher.matches(app, "ゲーム"))
        assertFalse(JapaneseSearchMatcher.matches(app, "xyz123"))
    }
}
