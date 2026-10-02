package com.dilshad.myapplication.demo

import android.content.Context
import com.dilshad.myapplication.data.db.AppDatabase
import com.dilshad.myapplication.domain.rag.CurriculumCorpus

data class DiagnosticReport(
    val timestamp: Long = System.currentTimeMillis(),
    val offlineStatus: String = "OPERATIONAL (No Internet Required)",
    val corpusChunksCount: Int,
    val totalAttemptsLogged: Int,
    val masteredConceptsCount: Int,
    val weakConceptsCount: Int
)

object DiagnosticsManager {

    suspend fun generateReport(context: Context): DiagnosticReport {
        val db = AppDatabase.getInstance(context)
        val dao = db.dao()

        val allMastery = dao.getAllMastery()
        val allAttempts = dao.getAllAttempts()

        val mastered = allMastery.count { it.masteryScore >= 0.60f }
        val weak = allMastery.count { it.masteryScore < 0.60f }

        return DiagnosticReport(
            corpusChunksCount = CurriculumCorpus.chunks.size,
            totalAttemptsLogged = allAttempts.size,
            masteredConceptsCount = mastered,
            weakConceptsCount = weak
        )
    }
}
