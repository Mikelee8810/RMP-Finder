package com.mike.rmpfinder.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {
    @Test
    fun newerReleaseWithApkIsAvailable() {
        val result = AppUpdateChecker.evaluateRelease(
            "1.2.7", "v1.2.8", "https://github.com/example/release",
            listOf(ReleaseAsset("RMP-Finder-1.2.8.apk", "https://github.com/example/app.apk")),
        )

        assertTrue(result is AppUpdateState.Available)
        result as AppUpdateState.Available
        assertEquals("1.2.8", result.version)
        assertEquals("https://github.com/example/app.apk", result.downloadUrl)
    }

    @Test
    fun newerReleaseWithoutApkFailsInsteadOfDownloadingHtml() {
        val result = AppUpdateChecker.evaluateRelease(
            "1.2.7", "v1.2.8", "https://github.com/example/release",
            listOf(ReleaseAsset("notes.txt", "https://github.com/example/notes.txt")),
        )

        assertEquals(AppUpdateState.Failed, result)
    }

    @Test
    fun currentReleaseDoesNotRequireAnApkAsset() {
        val result = AppUpdateChecker.evaluateRelease("1.2.7", "v1.2.7", "https://github.com/example/release", emptyList())

        assertEquals(AppUpdateState.Current("1.2.7"), result)
    }
}
