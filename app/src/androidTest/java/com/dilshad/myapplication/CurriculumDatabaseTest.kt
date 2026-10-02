package com.dilshad.myapplication

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dilshad.myapplication.content.ContentChunkEntity
import com.dilshad.myapplication.content.ContentPackEntity
import com.dilshad.myapplication.curriculum.AcquiredBookEntity
import com.dilshad.myapplication.curriculum.AcquisitionState
import com.dilshad.myapplication.data.db.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CurriculumDatabaseTest {
    private val databases = mutableListOf<AppDatabase>()

    @After
    fun closeDatabases() {
        databases.forEach(AppDatabase::close)
    }

    @Test
    fun acquisitionStatesAndCatalogDeletionPreserveDemoPack() = runBlocking {
        val database = newDatabase()
        val catalogBookId = "ncert-class-6-science-en"
        database.curriculumDao().upsertAcquisition(
            AcquiredBookEntity(catalogBookId, 1, AcquisitionState.NOT_ACQUIRED.name, null, "https://example.test/book", 0, 100, null, 1)
        )
        AcquisitionState.values().forEach { state ->
            database.curriculumDao().upsertAcquisition(
                AcquiredBookEntity(catalogBookId, 1, state.name, if (state == AcquisitionState.READY) "/books/6" else null, null, 10, 100, if (state == AcquisitionState.FAILED) "failed" else null, state.ordinal.toLong() + 2)
            )
            assertEquals(state.name, database.curriculumDao().getAcquisition(catalogBookId)?.state)
        }
        database.contentDao().insertPack(ContentPackEntity("catalog-pack", 1, "Science", "NCERT", "6", "Science", "Metadata only", catalogBookId = catalogBookId))
        database.contentDao().insertChunks(listOf(ContentChunkEntity("catalog-chunk", "catalog-pack", 1, "1", "Matter", 1, "text", "p.1")))
        database.contentDao().insertPack(ContentPackEntity("demo-pack", 1, "Demo", "Demo", "6", "Science", "Demo"))

        assertEquals(1, database.deleteCatalogBook(catalogBookId))
        assertNull(database.curriculumDao().getAcquisition(catalogBookId))
        assertNull(database.contentDao().getPack("catalog-pack"))
        assertTrue(database.contentDao().getPack("demo-pack") != null)
    }

    @Test
    fun migration6To7CreatesAcquisitionAndCatalogOwnershipSchemaPreservingData() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "curriculum-migration-${System.nanoTime()}.db"
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(version6FixtureCallback())
                .build()
        )
        try {
            val database = helper.writableDatabase
            AppDatabase.MIGRATION_6_7.migrate(database)

            assertTrue(tableExists(database, "acquired_books"))
            assertTrue(indexExists(database, "index_acquired_books_state"))
            assertTrue(indexExists(database, "index_content_packs_catalogBookId"))
            assertTrue(columnExists(database, "content_packs", "catalogBookId"))
            assertEquals(0, database.query("SELECT bookId FROM acquired_books").count)
            assertEquals(1, database.query("SELECT id FROM content_packs WHERE id = 'existing-pack' AND bookTitle = 'Existing'").count)
        } finally {
            helper.close()
            context.deleteDatabase(name)
        }
    }

    private fun newDatabase(): AppDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(), AppDatabase::class.java
    ).allowMainThreadQueries().build().also(databases::add)

    private fun version6FixtureCallback() = object : SupportSQLiteOpenHelper.Callback(6) {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE content_packs (id TEXT NOT NULL PRIMARY KEY, version INTEGER NOT NULL, bookTitle TEXT NOT NULL, board TEXT NOT NULL, classLevel TEXT NOT NULL, subject TEXT NOT NULL, licensingNote TEXT NOT NULL, isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
            db.execSQL("INSERT INTO content_packs VALUES ('existing-pack', 2, 'Existing', 'NCERT', '6', 'Science', 'License', 0, 123)")
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }

    private fun tableExists(database: SupportSQLiteDatabase, table: String): Boolean =
        database.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = '$table'").use { it.moveToFirst() }

    private fun indexExists(database: SupportSQLiteDatabase, index: String): Boolean =
        database.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name = '$index'").use { it.moveToFirst() }

    private fun columnExists(database: SupportSQLiteDatabase, table: String, column: String): Boolean =
        database.query("PRAGMA table_info($table)").use {
            val nameColumn = it.getColumnIndexOrThrow("name")
            generateSequence { if (it.moveToNext()) it.getString(nameColumn) else null }.any { it == column }
        }
}
