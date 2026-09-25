package com.curio.notes.domain

import com.curio.notes.ai.AiLanguage
import com.curio.notes.domain.model.AppLanguage
import com.curio.notes.ui.util.toLocale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class AppLanguageTest {

    @Test
    fun `explicit choices map directly`() {
        withDefaultLocale(Locale.ENGLISH) {
            assertEquals(AiLanguage.ENGLISH, AppLanguage.ENGLISH.toAiLanguage())
            assertEquals(AiLanguage.SPANISH, AppLanguage.SPANISH.toAiLanguage())
        }
    }

    @Test
    fun `system follows device locale`() {
        withDefaultLocale(Locale.forLanguageTag("es")) {
            assertEquals(AiLanguage.SPANISH, AppLanguage.SYSTEM.toAiLanguage())
        }
        withDefaultLocale(Locale.ENGLISH) {
            assertEquals(AiLanguage.ENGLISH, AppLanguage.SYSTEM.toAiLanguage())
        }
    }

    @Test
    fun `locales resolve`() {
        assertEquals("en", AppLanguage.ENGLISH.toLocale()?.language)
        assertEquals("es", AppLanguage.SPANISH.toLocale()?.language)
        assertNull(AppLanguage.SYSTEM.toLocale())
    }

    private fun withDefaultLocale(locale: Locale, block: () -> Unit) {
        val previous = Locale.getDefault()
        Locale.setDefault(locale)
        try {
            block()
        } finally {
            Locale.setDefault(previous)
        }
    }
}
