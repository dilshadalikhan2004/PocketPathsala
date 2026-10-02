package com.dilshad.myapplication.domain.learning

import com.dilshad.myapplication.data.db.AppDatabase
import com.dilshad.myapplication.data.db.entities.AttemptEntity
import com.dilshad.myapplication.data.db.entities.MasteryEntity
import kotlin.math.exp

object MasteryEngine {

    suspend fun updateMastery(
        conceptId: String,
        conceptName: String,
        subject: String,
        isCorrect: Boolean,
        difficulty: String = "MEDIUM",
        database: AppDatabase
    ): MasteryEntity {
        val dao = database.dao()
        val existing = dao.getMasteryForConcept(conceptId) ?: MasteryEntity(
            conceptId = conceptId,
            conceptName = conceptName,
            subject = subject,
            masteryScore = 0.0f,
            attemptCount = 0,
            correctCount = 0
        )

        val newAttemptCount = existing.attemptCount + 1
        val newCorrectCount = existing.correctCount + (if (isCorrect) 1 else 0)
        val now = System.currentTimeMillis()

        // Explicit Mastery Algorithm (Section 17):
        // Accuracy weight = 0.5, Recency weight = 0.3, Difficulty weight = 0.2
        val accuracy = newCorrectCount.toFloat() / newAttemptCount.toFloat()

        // Recency decay (7-day half life)
        val daysElapsed = ((now - existing.lastAttemptTimestamp) / (1000.0 * 60.0 * 60.0 * 24.0)).coerceAtLeast(0.0)
        val recencyFactor = exp(-0.1 * daysElapsed).toFloat()

        val diffWeight = when (difficulty.uppercase()) {
            "ADVANCED" -> 1.0f
            "MEDIUM" -> 0.8f
            else -> 0.6f
        }

        val calculatedScore = (0.5f * accuracy) + (0.3f * recencyFactor * accuracy) + (0.2f * diffWeight * accuracy)
        val finalScore = calculatedScore.coerceIn(0.0f, 1.0f)

        val updated = existing.copy(
            masteryScore = finalScore,
            attemptCount = newAttemptCount,
            correctCount = newCorrectCount,
            lastAttemptTimestamp = now,
            difficulty = difficulty
        )

        dao.saveMastery(updated)
        return updated
    }

    suspend fun getWeakConcepts(database: AppDatabase): List<MasteryEntity> {
        val all = database.dao().getAllMastery()
        return all.filter { it.masteryScore < 0.60f }
    }
}
