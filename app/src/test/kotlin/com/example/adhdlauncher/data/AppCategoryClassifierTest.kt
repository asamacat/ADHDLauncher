package com.example.adhdlauncher.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class AppCategoryClassifierTest {

    @Before
    fun setUp() {
        AppCategoryClassifier.resetForTesting()
    }

    @After
    fun tearDown() {
        AppCategoryClassifier.resetForTesting()
    }

    @Test
    fun classify_packageRuleExactMatch() {
        AppCategoryClassifier.loadFromJson(
            """
            {
              "package_rules": {
                "COMMUNICATION": ["jp.naver.line.android", "com.discord"],
                "FINANCE": ["jp.ne.paypay.android.app"],
                "INTERNET": ["com.android.chrome"],
                "MEDIA": ["com.google.android.youtube"]
              },
              "category_keywords": {},
              "search_aliases": {}
            }
            """.trimIndent()
        )

        assertEquals(
            AppCategory.COMMUNICATION,
            AppCategoryClassifier.classify("jp.naver.line.android", "LINE", null)
        )
        assertEquals(
            AppCategory.COMMUNICATION,
            AppCategoryClassifier.classify("com.discord", "Discord", null)
        )
        assertEquals(
            AppCategory.FINANCE,
            AppCategoryClassifier.classify("jp.ne.paypay.android.app", "PayPay", null)
        )
        assertEquals(
            AppCategory.INTERNET,
            AppCategoryClassifier.classify("com.android.chrome", "Chrome", null)
        )
        assertEquals(
            AppCategory.MEDIA,
            AppCategoryClassifier.classify("com.google.android.youtube", "YouTube", null)
        )
    }

    @Test
    fun classify_keywordMatchInLabelOrPackage() {
        AppCategoryClassifier.loadFromJson(
            """
            {
              "package_rules": {},
              "category_keywords": {
                "FINANCE": ["bank", "wallet", "pay"],
                "COMMUNICATION": ["chat", "talk"],
                "GAMES": ["puzzle", "rpg"]
              },
              "search_aliases": {}
            }
            """.trimIndent()
        )

        // Matches by keyword in label
        assertEquals(
            AppCategory.FINANCE,
            AppCategoryClassifier.classify("com.random.app", "My Bank App", null)
        )
        // Matches by keyword in package
        assertEquals(
            AppCategory.COMMUNICATION,
            AppCategoryClassifier.classify("org.custom.secretchat", "Secret", null)
        )
        // Matches game keyword
        assertEquals(
            AppCategory.GAMES,
            AppCategoryClassifier.classify("com.fun.dragons", "Dragon RPG", null)
        )
    }

    @Test
    fun classify_packageRuleTakesPrecedenceOverKeyword() {
        AppCategoryClassifier.loadFromJson(
            """
            {
              "package_rules": {
                "TOOLS": ["com.bank.calculator"]
              },
              "category_keywords": {
                "FINANCE": ["bank"]
              },
              "search_aliases": {}
            }
            """.trimIndent()
        )

        // Package is in TOOLS even though it contains keyword "bank"
        assertEquals(
            AppCategory.TOOLS,
            AppCategoryClassifier.classify("com.bank.calculator", "Bank Calculator", null)
        )
    }

    @Test
    fun classify_unrecognizedApp_defaultsToTools() {
        assertEquals(
            AppCategory.TOOLS,
            AppCategoryClassifier.classify("com.unknown.foo", "Foo Bar", null)
        )
    }

    @Test
    fun loadFromJson_withInvalidCategory_ignoresInvalidGracefully() {
        AppCategoryClassifier.loadFromJson(
            """
            {
              "package_rules": {
                "NON_EXISTENT_CATEGORY": ["com.bad.cat"],
                "COMMUNICATION": ["com.good.cat"]
              },
              "category_keywords": {
                "ANOTHER_BAD_CATEGORY": ["keyword"]
              },
              "search_aliases": {
                "test": "alias"
              }
            }
            """.trimIndent()
        )

        assertEquals(
            AppCategory.COMMUNICATION,
            AppCategoryClassifier.classify("com.good.cat", "Good", null)
        )
        assertEquals(
            AppCategory.TOOLS,
            AppCategoryClassifier.classify("com.bad.cat", "Bad", null)
        )
        assertEquals("alias", AppCategoryClassifier.getSearchAliases()["test"])
    }

    @Test
    fun resetForTesting_clearsAllRules() {
        AppCategoryClassifier.loadFromJson(
            """
            {
              "package_rules": {
                "COMMUNICATION": ["com.test.app"]
              },
              "category_keywords": {
                "MEDIA": ["video"]
              },
              "search_aliases": {
                "foo": "bar"
              }
            }
            """.trimIndent()
        )

        assertEquals(
            AppCategory.COMMUNICATION,
            AppCategoryClassifier.classify("com.test.app", "Test", null)
        )
        assertEquals("bar", AppCategoryClassifier.getSearchAliases()["foo"])

        AppCategoryClassifier.resetForTesting()

        assertEquals(
            AppCategory.TOOLS,
            AppCategoryClassifier.classify("com.test.app", "Test", null)
        )
        assertTrue(AppCategoryClassifier.getSearchAliases().isEmpty())
    }

    @Test
    fun realAppDictionary_loadsAndClassifiesCanonicalApps() {
        // Read the bundled dictionary from the project directory
        val dictFile = File("src/main/assets/dictionaries/app_dictionary.json")
            .takeIf { it.exists() }
            ?: File("app/src/main/assets/dictionaries/app_dictionary.json")

        assertTrue("app_dictionary.json should exist", dictFile.exists())
        val jsonText = dictFile.readText(Charsets.UTF_8)
        AppCategoryClassifier.loadFromJson(jsonText)

        // Verify known apps from the real dictionary
        assertEquals(
            AppCategory.COMMUNICATION,
            AppCategoryClassifier.classify("jp.naver.line.android", "LINE", null)
        )
        assertEquals(
            AppCategory.FINANCE,
            AppCategoryClassifier.classify("jp.ne.paypay.android.app", "PayPay", null)
        )
        assertEquals(
            AppCategory.INTERNET,
            AppCategoryClassifier.classify("com.android.chrome", "Chrome", null)
        )
        assertEquals(
            AppCategory.MEDIA,
            AppCategoryClassifier.classify("com.google.android.youtube", "YouTube", null)
        )
        assertEquals(
            AppCategory.COMMUNICATION,
            AppCategoryClassifier.classify("com.discord", "Discord", null)
        )

        // Verify search aliases loaded
        val aliases = AppCategoryClassifier.getSearchAliases()
        assertTrue("Aliases should be loaded", aliases.isNotEmpty())
    }
}
