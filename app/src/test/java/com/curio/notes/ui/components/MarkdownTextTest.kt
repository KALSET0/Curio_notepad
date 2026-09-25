package com.curio.notes.ui.components

import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTextTest {

    @Test
    fun `bold markers become bold spans`() {
        val annotated = "Hello **world**!".toBoldAnnotated()

        assertEquals("Hello world!", annotated.text)
        val bold = annotated.spanStyles.single()
        assertEquals(6, bold.start)
        assertEquals(11, bold.end)
        assertEquals(FontWeight.Bold, bold.item.fontWeight)
    }

    @Test
    fun `multiple bolds parse independently`() {
        val annotated = "**a** and **b**".toBoldAnnotated()

        assertEquals("a and b", annotated.text)
        assertEquals(2, annotated.spanStyles.size)
    }

    @Test
    fun `unclosed markers stay literal`() {
        assertEquals("**oops", "**oops".toBoldAnnotated().text)
        assertTrue("**oops".toBoldAnnotated().spanStyles.isEmpty())
        assertEquals("****", "****".toBoldAnnotated().text)
    }

    @Test
    fun `plain text passes through`() {
        assertEquals("plain", "plain".toBoldAnnotated().text)
        assertEquals("", "".toBoldAnnotated().text)
    }
}
