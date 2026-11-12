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
    private lateinit var preferences: LauncherPreferences

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        preferences = LauncherPreferences(fakePrefs)
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
}
