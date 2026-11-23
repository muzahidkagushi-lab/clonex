package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CloneDatabase
import com.example.data.repository.CloneRepository
import com.example.data.repository.InstalledAppsRepository
import com.example.data.repository.NameValidationResult
import com.example.domain.model.BadgeStyle
import com.example.domain.model.LanguageOption
import com.example.engine.MockCloneEngine
import com.example.util.Formatters
import com.example.util.LocaleHelper
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `bilingual string resources resolve for English and Bengali`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val enCtx = LocaleHelper.wrapContext(context, LanguageOption.ENGLISH)
        val bnCtx = LocaleHelper.wrapContext(context, LanguageOption.BENGALI)

        assertEquals("CloneX", enCtx.getString(R.string.app_name))
        assertEquals("CloneX – App Cloner", enCtx.getString(R.string.app_full_title))
        assertEquals("CloneX – অ্যাপ ক্লোনার", bnCtx.getString(R.string.app_full_title))
    }

    @Test
    fun `mock clone engine provisions and cleans up isolated sandbox directories`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = MockCloneEngine(context)

        val clone = engine.createClone(
            cloneId = "test-uuid-1234",
            originalPackageName = "com.example.chat",
            originalAppName = "ChatApp",
            cloneName = "ChatApp 2",
            badgeLabel = "2",
            badgeColorHex = 0xFFF59E0B,
            badgeStyle = BadgeStyle.CORNER_PILL,
            notificationsEnabled = true,
            isHiddenFromLauncher = false,
            isLocked = false
        )

        val root = File(clone.dataDirPath)
        assertTrue(root.exists())
        assertTrue(clone.storageBytes > 0L)
        assertTrue(clone.cacheBytes > 0L)

        val freed = engine.clearCache(clone.id)
        assertTrue(freed > 0L)
        assertEquals(0L, engine.getStorageUsage(clone.id).cacheBytes)

        engine.deleteClone(clone.id)
        assertFalse(root.exists())
    }

    @Test
    fun `clone name validation enforces uniqueness and 1 to 30 char length`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = CloneDatabase.getInstance(context)
        val repo = CloneRepository(
            context = context,
            cloneDao = db.cloneDao(),
            cloneEngine = MockCloneEngine(context),
            appsRepository = InstalledAppsRepository(context)
        )

        val existing = listOf(
            com.example.domain.model.CloneItem(
                id = "1",
                originalPackageName = "com.test",
                originalAppName = "Test",
                cloneName = "Test 2",
                badgeLabel = "2",
                badgeColorHex = 0xFF4F46E5,
                dataDirPath = "/tmp/1",
                createdAt = 1L,
                lastUsedAt = null
            )
        )

        assertEquals(NameValidationResult.Empty, repo.validateCloneName("   ", existing))
        assertEquals(
            NameValidationResult.TooLong,
            repo.validateCloneName("A".repeat(31), existing)
        )
        assertEquals(
            NameValidationResult.Duplicate,
            repo.validateCloneName("test 2", existing)
        )
        assertEquals(
            NameValidationResult.Valid,
            repo.validateCloneName("Test 3", existing)
        )

        val (generated, badge) = Formatters.generateUniqueCloneName("{name} {n}", "Test", existing)
        assertEquals("Test 3", generated)
        assertEquals("3", badge)
        db.close()
    }
}
