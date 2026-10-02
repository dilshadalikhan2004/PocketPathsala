package com.dilshad.myapplication

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dilshad.myapplication.content.CachedQuizEntity
import com.dilshad.myapplication.content.CachedQuizQuestionEntity
import com.dilshad.myapplication.content.ContentChunkEntity
import com.dilshad.myapplication.content.ContentPackEntity
import com.dilshad.myapplication.content.ContentPackRepository
import com.dilshad.myapplication.data.db.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContentPackRepositoryInstrumentedTest {
    private val databases = mutableListOf<AppDatabase>()

    @After
    fun closeDatabases() {
        databases.forEach(AppDatabase::close)
    }

    @Test
    fun exportPackUsesPackVersionAndExcludesStaleChildren() = runBlocking {
        val database = newDatabase()
        val dao = database.contentDao()
        dao.insertPack(pack("pack", 2))
        dao.insertChunks(listOf(
            chunk("current-chunk", "pack", 2, "current text"),
            chunk("stale-chunk", "pack", 1, "stale text")
        ))
        dao.insertQuizzes(listOf(
            CachedQuizEntity("current-quiz", "pack", 2, "Chapter", "Current"),
            CachedQuizEntity("stale-quiz", "pack", 1, "Chapter", "Stale")
        ))
        dao.insertQuizQuestions(listOf(
            question("current-question", "current-quiz", "pack", 2, "Current question"),
            question("stale-question", "stale-quiz", "pack", 1, "Stale question")
        ))

        val export = String(ContentPackRepository(database).exportPack("pack"), Charsets.UTF_8)
        assertTrue(export.contains("current text"))
        assertTrue(export.contains("Current question"))
        assertFalse(export.contains("stale text"))
        assertFalse(export.contains("Stale question"))
    }

    @Test
    fun missingPackFailsExport() = runBlocking {
        val repository = ContentPackRepository(newDatabase())
        try {
            repository.exportPack("missing")
            assertTrue("Expected missing pack failure", false)
        } catch (error: IllegalStateException) {
            assertTrue(error.message.orEmpty().contains("Content pack not found"))
        }
    }

    @Test
    fun activatePackLeavesOnlyRequestedPackActive() = runBlocking {
        val database = newDatabase()
        val dao = database.contentDao()
        dao.insertPack(pack("first", 1, active = true))
        dao.insertPack(pack("second", 1))

        ContentPackRepository(database).activatePack("second")

        assertEquals("second", dao.getActivePack()?.id)
        assertFalse(dao.getPack("first")!!.isActive)
    }

    @Test
    fun repeatImportReplacesSamePackVersionChildrenAtomically() = runBlocking {
        val database = newDatabase()
        val dao = database.contentDao()
        val repository = ContentPackRepository(database)
        dao.insertPack(pack("pack", 1))
        dao.insertChunks(listOf(chunk("old", "pack", 1, "old text")))
        dao.insertQuizzes(listOf(CachedQuizEntity("old-quiz", "pack", 1, "Chapter", "Old")))
        dao.insertQuizzes(listOf(CachedQuizEntity("other-version", "pack", 2, "Chapter", "Keep")))
        dao.insertQuizQuestions(listOf(question("old-question", "old-quiz", "pack", 1, "old question")))

        repository.saveImportedPack(pack("pack", 1), listOf(chunk("new", "pack", 1, "new text")))

        assertEquals(listOf("new text"), dao.getChunks("pack", 1).map(ContentChunkEntity::sourceText))
        assertTrue(dao.getQuizzes("pack", 1).isEmpty())
        assertTrue(dao.getQuizQuestions("pack", 1).isEmpty())
        assertEquals(1, dao.getQuizzes("pack", 2).size)
    }
    @Test
    fun migrationPreservesExistingRowsAndCreatesContentTables() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-${System.nanoTime()}.db"
        val oldConfig = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE student_profile (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, preferredLanguage TEXT NOT NULL, classLevel TEXT NOT NULL, board TEXT NOT NULL, subjectsJson TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                    db.execSQL("INSERT INTO student_profile VALUES ('local_profile', 'Existing', 'English', 'Class 10', 'CBSE', '[]', 1)")
                }
                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build()
        val oldHelper = FrameworkSQLiteOpenHelperFactory().create(oldConfig)
        oldHelper.writableDatabase.close()
        oldHelper.close()

        val newConfig = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) = Unit
                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    AppDatabase.MIGRATION_2_3.migrate(db)
                }
            }).build()
        val newHelper = FrameworkSQLiteOpenHelperFactory().create(newConfig)
        val db = newHelper.writableDatabase
        val nameCursor = db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'content_packs'")
        assertTrue(nameCursor.moveToFirst())
        nameCursor.close()
        val rowCursor = db.query("SELECT name FROM student_profile WHERE id = 'local_profile'")
        assertTrue(rowCursor.moveToFirst())
        assertEquals("Existing", rowCursor.getString(0))
        rowCursor.close()
        newHelper.close()
        context.deleteDatabase(name)
    }

    private fun newDatabase(): AppDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(), AppDatabase::class.java
    ).allowMainThreadQueries().build().also(databases::add)

    private fun pack(id: String, version: Int, active: Boolean = false) = ContentPackEntity(
        id, version, "Book", "Board", "10", "Science", "License", active
    )

    private fun chunk(id: String, packId: String, version: Int, text: String) = ContentChunkEntity(
        id, packId, version, "Chapter", "Section", 1, text, "Citation"
    )

    private fun question(id: String, quizId: String, packId: String, version: Int, text: String) = CachedQuizQuestionEntity(
        id, quizId, packId, version, text, "[]", "A", "Explanation", "Citation"
    )
}
