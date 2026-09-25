package com.curio.notes.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowUpsTest {

    @Test
    fun `valid list parses`() {
        assertEquals(
            listOf("A?", "B?"),
            parseFollowUpList("""{"questions":["A?","B?"]}""")
        )
    }

    @Test
    fun `malformed output means keep old`() {
        assertTrue(parseFollowUpList("not json").isEmpty())
        assertTrue(parseFollowUpList(null).isEmpty())
        assertTrue(parseFollowUpList("{}").isEmpty())
        assertTrue(parseFollowUpList("""{"questions":"nope"}""").isEmpty())
    }
}
