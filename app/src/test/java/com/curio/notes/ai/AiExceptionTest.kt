package com.curio.notes.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AiExceptionTest {

    @Test
    fun `every subtype carries a distinct stable code`() {
        val codes = listOf(
            AiException.MissingApiKey().code,
            AiException.InvalidApiKey().code,
            AiException.Network(IOException("x")).code,
            AiException.TimedOut().code,
            AiException.RateLimited().code,
            AiException.InvalidResponse().code,
            AiException.ServiceError("x").code
        )
        assertTrue(codes.all { it.isNotBlank() })
        assertEquals(codes.size, codes.toSet().size)
    }
}
