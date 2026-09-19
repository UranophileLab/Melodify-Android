package dev.melodify.uranophilelab.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManagerTest {

    @Test
    fun testIsNewerVersion() {
        // Equal versions
        assertFalse(UpdateManager.isNewerVersion("2.0.0", "2.0.0"))
        assertFalse(UpdateManager.isNewerVersion("v2.0.0", "2.0.0"))

        // Newer patch version
        assertTrue(UpdateManager.isNewerVersion("2.0.0", "2.0.1"))
        assertTrue(UpdateManager.isNewerVersion("2.0.0", "v2.0.1"))

        // Newer minor version
        assertTrue(UpdateManager.isNewerVersion("2.0.0", "2.1.0"))

        // Newer major version
        assertTrue(UpdateManager.isNewerVersion("2.0.0", "3.0.0"))

        // Older version
        assertFalse(UpdateManager.isNewerVersion("2.1.0", "2.0.0"))
        assertFalse(UpdateManager.isNewerVersion("3.0.0", "2.1.5"))

        // Different number of parts
        assertTrue(UpdateManager.isNewerVersion("2.0", "2.0.1"))
        assertFalse(UpdateManager.isNewerVersion("2.0.1", "2.0"))

        // Pre-release suffixes
        assertTrue(UpdateManager.isNewerVersion("2.0.0", "2.0.1-beta"))
        assertFalse(UpdateManager.isNewerVersion("2.0.0", "2.0.0-beta"))

        // Blank latest version
        assertFalse(UpdateManager.isNewerVersion("2.0.0", ""))
    }
}
