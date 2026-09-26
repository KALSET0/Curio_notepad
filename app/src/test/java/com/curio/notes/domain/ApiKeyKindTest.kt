package com.curio.notes.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveApiKeyTest {
    @Test
    fun `user key wins over build key`() {
        assertEquals("user", resolveApiKey("user", "build"))
    }

    @Test
    fun `blank user key falls back to build key`() {
        assertEquals("build", resolveApiKey("", "build"))
        assertEquals("build", resolveApiKey("   ", "build"))
    }

    @Test
    fun `both blank stays blank`() {
        assertEquals("", resolveApiKey("", ""))
    }
}
