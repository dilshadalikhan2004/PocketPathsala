package com.dilshad.myapplication

import com.dilshad.myapplication.curriculum.AcquisitionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookAcquisitionRepositoryTest {
    @Test
    fun progressRetainsReceivedAndOptionalTotal() {
        val progress = AcquisitionEvent.Progress(128L, 512L)
        assertEquals(128L, progress.receivedBytes)
        assertEquals(512L, progress.totalBytes)
    }

    @Test
    fun failureExposesActionableMessage() {
        val failure = AcquisitionEvent.Failed("Downloaded file is not a valid PDF")
        assertTrue(failure.message.contains("PDF"))
    }
}
