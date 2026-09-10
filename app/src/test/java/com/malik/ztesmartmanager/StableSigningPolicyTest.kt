package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StableSigningPolicyTest {
    @Test
    fun productionBuild_usesStableSigningEnvironment() {
        val gradle = File("build.gradle.kts").readText()
        assertTrue(gradle.contains("ZTE_SIGNING_STORE_FILE"))
        assertTrue(gradle.contains("ZTE_SIGNING_STORE_PASSWORD"))
        assertTrue(gradle.contains("ZTE_SIGNING_KEY_ALIAS"))
        assertTrue(gradle.contains("stableRelease"))
        assertTrue(gradle.contains("applicationId = \"com.malik.ztesmartmanager\""))
    }

    @Test
    fun workflow_neverUploadsTemporaryDebugApk() {
        val workflow = File("../.github/workflows/android-build.yml").readText()
        assertTrue(workflow.contains("Require stable signing key on main"))
        assertTrue(workflow.contains("zte-smart-hai-signed-release"))
        assertTrue(workflow.contains("apksigner"))
        assertTrue(workflow.contains("69FF3E0474964C733322E2AF1F36D840C03E22284B27731FF6B0D4B607D83009"))
        assertFalse(workflow.contains("name: zte-smart-manager-debug"))
    }
}
