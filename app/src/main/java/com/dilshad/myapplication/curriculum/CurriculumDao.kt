package com.dilshad.myapplication.curriculum

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CurriculumDao {
    @Query("SELECT * FROM acquired_books WHERE bookId = :bookId LIMIT 1")
    suspend fun getAcquisition(bookId: String): AcquiredBookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAcquisition(book: AcquiredBookEntity): Long

    @Query("SELECT * FROM acquired_books ORDER BY updatedAt DESC, bookId")
    suspend fun listAcquisitions(): List<AcquiredBookEntity>

    @Query("DELETE FROM acquired_books WHERE bookId = :bookId")
    suspend fun deleteAcquisition(bookId: String): Int
}

