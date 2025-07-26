package se.premex.gmai.plugin.utils

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import se.premex.gmai.plugin.utils.OSUtils.platformName

class OSUtilsTest {

    @Test
    fun `should detect operating system correctly`() {
        val os = OSUtils.getOperatingSystem()
        assertNotNull(os)
        assertTrue(os in listOf(OSUtils.OperatingSystem.MACOS, OSUtils.OperatingSystem.LINUX, OSUtils.OperatingSystem.WINDOWS))
    }

    @Test
    fun `should return correct platform name for each OS`() {
        assertEquals("darwin", OSUtils.OperatingSystem.MACOS.platformName)
        assertEquals("linux", OSUtils.OperatingSystem.LINUX.platformName)
        assertEquals("windows", OSUtils.OperatingSystem.WINDOWS.platformName)
    }

    @Test
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun `should find executable in path on Unix systems`() {
        // Test with 'ls' which should be available on all Unix systems
        val result = OSUtils.findExecutableInPath("ls")
        assertNotNull(result, "Should find 'ls' executable on Unix systems")
        assertTrue(result.isNotBlank(), "Result should not be blank")
        assertTrue(result.contains("ls"), "Result should contain 'ls'")
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    fun `should find executable in path on Windows`() {
        // Test with 'cmd' which should be available on Windows
        val result = OSUtils.findExecutableInPath("cmd")
        assertNotNull(result, "Should find 'cmd' executable on Windows")
        assertTrue(result.isNotBlank(), "Result should not be blank")
        assertTrue(result.contains("cmd"), "Result should contain 'cmd'")
    }

    @Test
    fun `should return null for non-existent executable`() {
        val result = OSUtils.findExecutableInPath("this-executable-definitely-does-not-exist-123456")
        assertEquals(null, result, "Should return null for non-existent executable")
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    fun `should detect Linux correctly`() {
        val os = OSUtils.getOperatingSystem()
        assertEquals(OSUtils.OperatingSystem.LINUX, os)
    }

    @Test
    @EnabledOnOs(OS.MAC)
    fun `should detect macOS correctly`() {
        val os = OSUtils.getOperatingSystem()
        assertEquals(OSUtils.OperatingSystem.MACOS, os)
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    fun `should detect Windows correctly`() {
        val os = OSUtils.getOperatingSystem()
        assertEquals(OSUtils.OperatingSystem.WINDOWS, os)
    }
}