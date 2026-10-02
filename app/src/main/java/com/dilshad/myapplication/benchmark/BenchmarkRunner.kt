package com.dilshad.myapplication.benchmark

import android.os.Build
import com.dilshad.myapplication.data.db.AppDao
import com.dilshad.myapplication.data.db.entities.BenchmarkResultEntity
import com.google.gson.Gson
import java.util.UUID

enum class BenchmarkState { UNMEASURED, MEASURED }

data class BenchmarkResult(
    val workloadId: String,
    val state: BenchmarkState = BenchmarkState.UNMEASURED,
    val elapsedNanos: Long? = null,
    val timestamp: Long? = null,
    val provider: String = "local",
    val packId: String? = null
) {
    val elapsedMillis: Double? get() = elapsedNanos?.let { it / 1_000_000.0 }
}

/** Measures only work executed on this device; it never supplies estimates. */
class BenchmarkRunner(
    private val dao: AppDao? = null,
    private val deviceModel: String = Build.MODEL?.takeIf { it.isNotBlank() } ?: "unknown",
    private val androidVersion: String = Build.VERSION.RELEASE ?: "unknown"
) {
    suspend fun run(workloadId: String, provider: String = "local", packId: String? = null, block: suspend () -> Unit): BenchmarkResult {
        val start = System.nanoTime()
        block()
        val result = BenchmarkResult(workloadId, BenchmarkState.MEASURED, System.nanoTime() - start, System.currentTimeMillis(), provider, packId)
        dao?.saveBenchmarkResult(result.toEntity(deviceModel, androidVersion))
        return result
    }

    suspend fun results(): List<BenchmarkResult> = dao?.getBenchmarkResults()?.map { it.toResult() }.orEmpty()

    fun exportJson(results: List<BenchmarkResult>): ByteArray = Gson().toJson(results).toByteArray(Charsets.UTF_8)
    fun exportCsv(results: List<BenchmarkResult>): ByteArray =
        buildString {
            appendLine("workloadId,state,elapsedNanos,timestamp,provider,packId")
            results.forEach { appendLine(listOf(it.workloadId, it.state, it.elapsedNanos ?: "", it.timestamp ?: "", it.provider, it.packId ?: "").joinToString(",")) }
        }.toByteArray(Charsets.UTF_8)
}

private fun BenchmarkResult.toEntity(device: String, android: String) = BenchmarkResultEntity(
    UUID.randomUUID().toString(), workloadId, state.name, elapsedNanos, timestamp ?: 0L, device, android, provider, packId
)
private fun BenchmarkResultEntity.toResult() = BenchmarkResult(
    workloadId, runCatching { BenchmarkState.valueOf(state) }.getOrDefault(BenchmarkState.UNMEASURED),
    elapsedNanos, timestamp, provider, packId
)
