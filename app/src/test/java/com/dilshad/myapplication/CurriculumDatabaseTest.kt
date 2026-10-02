package com.dilshad.myapplication

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dilshad.myapplication.content.ContentChunkEntity
import com.dilshad.myapplication.content.ContentPackEntity
import com.dilshad.myapplication.curriculum.AcquiredBookEntity
import com.dilshad.myapplication.curriculum.AcquisitionState
import com.dilshad.myapplication.data.db.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@Ignore("Room in-memory tests require an Android/SQLite runtime; exercised by instrumentation")
class CurriculumDatabaseTest {
    private lateinit var database: AppDatabase

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After fun tearDown() = database.close()

    @Test fun acquisitionStatesAndCatalogDeletionPreserveDemoPack() = runBlocking {
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
}



