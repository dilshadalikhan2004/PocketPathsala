package com.dilshad.myapplication.curriculum

import androidx.room.Entity
import androidx.room.Index

/** Lifecycle states for a catalog book's local acquisition and indexing. */
enum class AcquisitionState { NOT_ACQUIRED, DOWNLOADING, ACQUIRED, INDEXING, READY, FAILED }

@Entity(
    tableName = "acquired_books",
    indices = [Index(value = ["state"]), Index(value = ["catalogVersion"]), Index(value = ["updatedAt"])]
)
data class AcquiredBookEntity(
    @androidx.room.PrimaryKey val bookId: String,
    val catalogVersion: Int,
    val state: String,
    val localPath: String?,
    val sourceUri: String?,
    val bytesDownloaded: Long,
    val totalBytes: Long?,
    val errorMessage: String?,
    val updatedAt: Long
)
