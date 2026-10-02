package com.dilshad.myapplication

import com.dilshad.myapplication.benchmark.BenchmarkRunner
import com.dilshad.myapplication.benchmark.BenchmarkState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BenchmarkRunnerTest {
    @Test fun newRunnerHasNoInventedMeasurement() {
        val result = com.dilshad.myapplication.benchmark.BenchmarkResult("decode")
        assertEquals(BenchmarkState.UNMEASURED, result.state)
        assertNull(result.elapsedNanos)
    }

    @Test fun runRecordsMeasuredLocalDuration() = runBlocking {
        val result = BenchmarkRunner().run("small-work") { repeat(1000) { hashCode() } }
        assertEquals(BenchmarkState.MEASURED, result.state)
        assertTrue(result.elapsedNanos != null)
        result.elapsedNanos?.let { assertTrue(it >= 0) }
        Unit
    }
}
