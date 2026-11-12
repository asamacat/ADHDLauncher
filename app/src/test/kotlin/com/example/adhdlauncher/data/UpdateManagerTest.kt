package com.example.adhdlauncher.data

import android.content.Context
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class UpdateManagerTest {

    @Test
    fun testVersionComparator_newerVersions() {
        assertTrue(VersionComparator.isNewer("v0.3.3", "0.3.2"))
        assertTrue(VersionComparator.isNewer("0.3.3", "0.3.2"))
        assertTrue(VersionComparator.isNewer("v0.4.0", "0.3.3"))
        assertTrue(VersionComparator.isNewer("v1.0.0", "0.3.3"))
        assertTrue(VersionComparator.isNewer("v0.3.10", "0.3.3"))
        assertTrue(VersionComparator.isNewer("0.3.3.1", "0.3.3"))
    }

    @Test
    fun testVersionComparator_olderOrEqualVersions() {
        assertFalse(VersionComparator.isNewer("v0.3.2", "0.3.3"))
        assertFalse(VersionComparator.isNewer("0.3.3", "0.3.3"))
        assertFalse(VersionComparator.isNewer("v0.3.3", "0.3.3"))
        assertFalse(VersionComparator.isNewer("v0.3.3-beta", "0.3.3"))
        assertFalse(VersionComparator.isNewer("v0.2.9", "0.3.3"))
    }

    @Test
    fun testVersionComparator_cleanVersion() {
        assertEquals("0.3.3", VersionComparator.cleanVersion("v0.3.3"))
        assertEquals("0.3.3", VersionComparator.cleanVersion("V0.3.3"))
        assertEquals("0.3.3", VersionComparator.cleanVersion("0.3.3"))
        assertEquals("0.3.3", VersionComparator.cleanVersion("v0.3.3-beta.1"))
    }

    @Test
    fun testRateLimitGuard_doesNotHitNetworkWithin24Hours() = runTest {
        val fakePrefs = FakeSharedPreferences()
        val prefs = LauncherPreferences(fakePrefs)
        val now = System.currentTimeMillis()
        prefs.setLastUpdateCheckTime(now - 3600_000L) // 1 hour ago

        val dummyContext = object : android.content.ContextWrapper(null) {}

        val manager = UpdateManager(dummyContext, prefs)
        assertEquals(UpdateStatus.Idle, manager.status.value)

        val status = manager.checkForUpdates(force = false)
        assertEquals(UpdateStatus.UpToDate, status)
        assertEquals(UpdateStatus.UpToDate, manager.status.value)
    }
}
