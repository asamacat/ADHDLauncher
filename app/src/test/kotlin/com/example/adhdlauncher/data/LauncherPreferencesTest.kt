package com.example.adhdlauncher.data

import android.content.SharedPreferences
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FakeSharedPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any>()

    override fun getAll(): Map<String, *> = HashMap(data)

    override fun getString(key: String, defValue: String?): String? =
        data[key] as? String ?: defValue

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        @Suppress("UNCHECKED_CAST")
        return data[key] as? Set<String> ?: defValues
    }

    override fun getInt(key: String, defValue: Int): Int =
        data[key] as? Int ?: defValue

    override fun getLong(key: String, defValue: Long): Long =
        data[key] as? Long ?: defValue

    override fun getFloat(key: String, defValue: Float): Float =
        data[key] as? Float ?: defValue

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        data[key] as? Boolean ?: defValue

    override fun contains(key: String): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    inner class FakeEditor : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        private var clear = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            temp[key] = value
            return this
        }

        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
            temp[key] = values
            return this
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            temp[key] = value
            return this
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            temp[key] = value
            return this
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            temp[key] = value
            return this
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            temp[key] = value
            return this
        }

        override fun remove(key: String): SharedPreferences.Editor {
            temp[key] = null
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clear = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clear) data.clear()
            for ((k, v) in temp) {
                if (v == null) {
                    data.remove(k)
                } else {
                    data[k] = v
                }
            }
            temp.clear()
            clear = false
        }
    }
}

class LauncherPreferencesTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var fakeUsagePrefs: FakeSharedPreferences
    private lateinit var preferences: LauncherPreferences
    private lateinit var appRepository: AppRepository

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        fakeUsagePrefs = FakeSharedPreferences()
        preferences = LauncherPreferences(fakePrefs)
        appRepository = AppRepository(context = null, usagePrefs = fakeUsagePrefs)
    }

    @Test
    fun testConsolidation_stackFromBottomUpdatesMoveIconsDown() {
        preferences.setStackFromBottom(false)
        assertFalse(preferences.stackFromBottom.value)
        assertFalse(preferences.moveIconsDown.value)

        preferences.setStackFromBottom(true)
        assertTrue(preferences.stackFromBottom.value)
        assertTrue(preferences.moveIconsDown.value)

        preferences.setMoveIconsDown(false)
        assertFalse(preferences.stackFromBottom.value)
        assertFalse(preferences.moveIconsDown.value)
    }

    @Test
    fun testExportToJson_exportsAllPreferences() {
        preferences.setHandedness(Handedness.LEFT)
        preferences.setDrawerColumns(5)
        preferences.setHoneycombAppCount(9)
        preferences.setIconStyle(IconStyle.MATERIAL_SAGE)

        val jsonStr = preferences.exportToJson()
        assertNotNull(jsonStr)

        val json = JSONObject(jsonStr)
        assertEquals("LEFT", json.getString("key_handedness"))
        assertEquals(5, json.getInt("key_drawer_columns"))
        assertEquals(9, json.getInt("key_honeycomb_app_count"))
        assertEquals("MATERIAL_SAGE", json.getString("key_icon_style"))
    }

    @Test
    fun testImportFromJson_restoresAllPreferences() {
        val testJson = """
            {
                "key_handedness": "LEFT",
                "key_drawer_columns": 5,
                "key_honeycomb_app_count": 10,
                "key_stack_from_bottom": false,
                "key_icon_style": "MATERIAL_CYAN"
            }
        """.trimIndent()

        val success = preferences.importFromJson(testJson)
        assertTrue(success)

        assertEquals(Handedness.LEFT, preferences.handedness.value)
        assertEquals(5, preferences.drawerColumns.value)
        assertEquals(10, preferences.honeycombAppCount.value)
        assertFalse(preferences.stackFromBottom.value)
        assertEquals(IconStyle.MATERIAL_CYAN, preferences.iconStyle.value)
    }

    @Test
    fun testImportFromJson_rejectsCorruptOrNonLauncherJson() {
        assertFalse(preferences.importFromJson(""))
        assertFalse(preferences.importFromJson("not json at all"))
        assertFalse(preferences.importFromJson("""{"unrelated_key": "some_value"}"""))
    }

    @Test
    fun testRateLimitGuard_timestampStorage() {
        assertEquals(0L, preferences.getLastUpdateCheckTime())
        val now = 1731400000000L
        preferences.setLastUpdateCheckTime(now)
        assertEquals(now, preferences.getLastUpdateCheckTime())
    }

    @Test
    fun testExportToJson_doesNotExportRuntimeUpdateTimestamp() {
        preferences.setLastUpdateCheckTime(1731400000000L)
        val jsonStr = preferences.exportToJson()
        val json = JSONObject(jsonStr)
        assertFalse("Runtime cache timestamp should not be in exported JSON", json.has("key_last_update_check_time"))
    }

    @Test
    fun testImportFromJson_ignoresUnrelatedKeysEvenWithValidKeys() {
        val testJson = """
            {
                "malicious_key": "injected_value",
                "key_drawer_columns": 3
            }
        """.trimIndent()

        val success = preferences.importFromJson(testJson)
        assertTrue(success)
        assertEquals(3, preferences.drawerColumns.value)
        assertFalse(fakePrefs.contains("malicious_key"))
    }

    @Test
    fun testV2Envelope_exportAndImportWithLearningData() {
        preferences.setHandedness(Handedness.LEFT)
        preferences.setDrawerColumns(5)
        preferences.setCategoryCustomName(AppCategory.TOOLS, "仕事・集中")

        fakeUsagePrefs.edit()
            .putInt("count_com.slack", 42)
            .putInt("com.slack", 42)
            .putLong("time_com.slack", 1731405600000L)
            .putString("cat_com.slack", AppCategory.TOOLS.name)
            .putBoolean("hidden_com.android.calculator2", true)
            .putInt("ctx_wkdy_h2_com.slack", 15)
            .apply()

        val exportedJson = preferences.exportBackupJson(
            includeLearningData = true,
            appRepository = appRepository
        )

        val root = JSONObject(exportedJson)
        assertEquals(2, root.getInt("version"))
        assertTrue(root.getBoolean("has_learning_data"))
        assertTrue(root.has("preferences"))
        assertTrue(root.has("usage_data"))

        // 別インスタンスへ完全復元（既存の不要データが消去されることも検証）
        val targetPrefs = FakeSharedPreferences().apply {
            edit().putString("key_anchor_text", "古いアンカーテキスト").apply()
        }
        val targetUsagePrefs = FakeSharedPreferences().apply {
            edit().putInt("count_com.old.app", 99).apply()
        }
        val targetLauncherPrefs = LauncherPreferences(targetPrefs)
        val targetRepo = AppRepository(context = null, usagePrefs = targetUsagePrefs)

        val result = targetLauncherPrefs.importBackupJson(exportedJson, targetRepo)
        assertTrue(result.success)
        assertTrue(result.hasLearningData)
        assertEquals(3, result.prefsCount)
        assertEquals(6, result.usageCount)

        assertEquals(Handedness.LEFT, targetLauncherPrefs.handedness.value)
        assertEquals(5, targetLauncherPrefs.drawerColumns.value)
        assertEquals("仕事・集中", targetLauncherPrefs.getCategoryLabel(AppCategory.TOOLS))
        assertEquals("", targetLauncherPrefs.anchorText.value) // 完全置換で初期値へ戻る

        assertFalse("Old usage entry should be cleared on full replacement", targetUsagePrefs.contains("count_com.old.app"))
        assertEquals(42, targetUsagePrefs.getInt("count_com.slack", 0))
        assertEquals(42, targetUsagePrefs.getInt("com.slack", 0))
        assertEquals(1731405600000L, targetUsagePrefs.getLong("time_com.slack", 0L))
        assertEquals(AppCategory.TOOLS.name, targetUsagePrefs.getString("cat_com.slack", null))
        assertTrue(targetUsagePrefs.getBoolean("hidden_com.android.calculator2", false))
        assertEquals(15, targetUsagePrefs.getInt("ctx_wkdy_h2_com.slack", 0))
    }

    @Test
    fun testV2Envelope_exportWithoutLearningData_doesNotOverwriteExistingUsage() {
        preferences.setClockPosition(ClockPosition.BOTTOM_COMPACT)
        fakeUsagePrefs.edit().putInt("count_com.slack", 10).apply()

        val exportedJson = preferences.exportBackupJson(
            includeLearningData = false,
            appRepository = appRepository
        )

        val root = JSONObject(exportedJson)
        assertEquals(2, root.getInt("version"))
        assertFalse(root.getBoolean("has_learning_data"))
        assertEquals(0, root.getJSONObject("usage_data").length())

        val targetPrefs = FakeSharedPreferences()
        val targetUsagePrefs = FakeSharedPreferences().apply {
            edit().putInt("count_com.existing.app", 77).apply()
        }
        val targetLauncherPrefs = LauncherPreferences(targetPrefs)
        val targetRepo = AppRepository(context = null, usagePrefs = targetUsagePrefs)

        val result = targetLauncherPrefs.importBackupJson(exportedJson, targetRepo)
        assertTrue(result.success)
        assertFalse(result.hasLearningData)
        assertEquals(1, result.prefsCount)
        assertEquals(0, result.usageCount)
        assertEquals(ClockPosition.BOTTOM_COMPACT, targetLauncherPrefs.clockPosition.value)
        assertEquals(77, targetUsagePrefs.getInt("count_com.existing.app", 0))
    }

    @Test
    fun testV1FlatJson_backwardCompatibilityWithImportBackupJson() {
        val v1Json = """
            {
                "key_handedness": "LEFT",
                "key_honeycomb_app_count": 11,
                "cat_label_COMMUNICATION": "SNS・連絡"
            }
        """.trimIndent()

        fakeUsagePrefs.edit().putInt("count_com.keep.me", 5).apply()

        val result = preferences.importBackupJson(v1Json, appRepository)
        assertTrue(result.success)
        assertFalse(result.hasLearningData)
        assertEquals(3, result.prefsCount)
        assertEquals(0, result.usageCount)
        assertEquals(Handedness.LEFT, preferences.handedness.value)
        assertEquals(11, preferences.honeycombAppCount.value)
        assertEquals("SNS・連絡", preferences.getCategoryLabel(AppCategory.COMMUNICATION))
        assertEquals(5, fakeUsagePrefs.getInt("count_com.keep.me", 0))
    }

    @Test
    fun testUsageDataImport_ignoresInvalidKeysAndWrongTypes() {
        val rawUsageJson = JSONObject().apply {
            put("count_com.valid.app", 12)
            put("time_com.valid.app", 1731405600123L)
            put("cat_com.valid.app", "TOOLS")
            put("cat_com.invalid.cat", "NON_EXISTENT_CATEGORY")
            put("cat_com.all.cat", "ALL")
            put("hidden_com.valid.app", true)
            put("ctx_wknd_h5_com.valid.app", 4)
            put("malicious_script_key", "bad")
            put("key_handedness", "LEFT")
            put("count_wrong_type", "not_a_number")
        }

        val restored = appRepository.importUsageData(rawUsageJson)
        assertEquals(5, restored)
        assertEquals(12, fakeUsagePrefs.getInt("count_com.valid.app", 0))
        assertEquals(1731405600123L, fakeUsagePrefs.getLong("time_com.valid.app", 0L))
        assertEquals("TOOLS", fakeUsagePrefs.getString("cat_com.valid.app", null))
        assertFalse(fakeUsagePrefs.contains("cat_com.invalid.cat"))
        assertFalse(fakeUsagePrefs.contains("cat_com.all.cat"))
        assertTrue(fakeUsagePrefs.getBoolean("hidden_com.valid.app", false))
        assertEquals(4, fakeUsagePrefs.getInt("ctx_wknd_h5_com.valid.app", 0))
        assertFalse(fakeUsagePrefs.contains("malicious_script_key"))
        assertFalse(fakeUsagePrefs.contains("key_handedness"))
        assertFalse(fakeUsagePrefs.contains("count_wrong_type"))
    }

    @Test
    fun testV2Envelope_emptyPreferencesWithValidLearningData_succeeds() {
        val emptyPrefsV2 = """
            {
                "version": 2,
                "app_version": "0.3.4",
                "created_at": 1731405600000,
                "has_learning_data": true,
                "preferences": {},
                "usage_data": {
                    "count_com.slack": 25,
                    "cat_com.slack": "COMMUNICATION"
                }
            }
        """.trimIndent()

        fakePrefs.edit().putString("key_handedness", "LEFT").apply()
        preferences.reloadFromPreferences()
        assertEquals(Handedness.LEFT, preferences.handedness.value)

        val result = preferences.importBackupJson(emptyPrefsV2, appRepository)
        assertTrue(result.success)
        assertEquals(0, result.prefsCount)
        assertEquals(2, result.usageCount)
        assertTrue(result.hasLearningData)
        assertEquals(Handedness.RIGHT, preferences.handedness.value) // Reset to default on full replacement
        assertEquals(25, fakeUsagePrefs.getInt("count_com.slack", 0))
        assertEquals("COMMUNICATION", fakeUsagePrefs.getString("cat_com.slack", null))
    }

    @Test
    fun testV2Envelope_rejectsMalformedUsageDataWhenHasLearningIsTrue() {
        val badV2 = """
            {
                "version": 2,
                "has_learning_data": true,
                "preferences": {},
                "usage_data": "not_an_object"
            }
        """.trimIndent()

        val result = preferences.importBackupJson(badV2, appRepository)
        assertFalse(result.success)
    }
}
