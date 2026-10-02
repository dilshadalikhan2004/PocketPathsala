package com.dilshad.myapplication

import com.dilshad.myapplication.host.joinUrl
import com.dilshad.myapplication.host.makeJoinCode
import org.junit.Assert.*
import org.junit.Test

class JoinCodeTest {
    @Test fun joinUrlIsLocalAndStable() {
        assertEquals("http://192.168.1.4:8080/", joinUrl("192.168.1.4", 8080))
        assertEquals("http://192.168.1.4:8080/", joinUrl("http://192.168.1.4/", 8080))
    }

    @Test fun qrContainsJoinPayload() {
        val code = makeJoinCode(joinUrl("10.0.0.2", 4321))
        assertEquals("http://10.0.0.2:4321/", code.url)
        assertTrue(code.matrix.width > 0)
        assertTrue(code.matrix.height > 0)
    }
}
