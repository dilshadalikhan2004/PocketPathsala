package com.dilshad.myapplication

import com.dilshad.myapplication.curriculum.AcquisitionEvent
import com.dilshad.myapplication.curriculum.BookAcquisitionRepository
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

    @Test
    fun missingMimeUsesPdfOrTextExtension() {
        assertEquals("application/pdf", BookAcquisitionRepository.resolveMime("", "/tmp/book.pdf", null))
        assertEquals("text/plain", BookAcquisitionRepository.resolveMime("", "/tmp/book.txt", null))
    }

    @Test
    fun missingMimeUsesPdfSignatureAndUnsupportedMimeStaysRejected() {
        assertEquals("application/pdf", BookAcquisitionRepository.resolveMime("", "/tmp/book.bin", "%PDF-".toByteArray()))
        assertEquals("application/zip", BookAcquisitionRepository.resolveMime("application/zip", "/tmp/book.pdf", null))
        assertEquals("", BookAcquisitionRepository.resolveMime("", "/tmp/book.bin", "not a pdf".toByteArray()))
    }
}
