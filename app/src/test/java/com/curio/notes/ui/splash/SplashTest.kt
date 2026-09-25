package com.curio.notes.ui.splash

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashTest {

    @Test
    fun `advances after min time with data ready`() {
        assertTrue(shouldAdvanceSplash(SPLASH_MIN_MILLIS, true))
        assertTrue(shouldAdvanceSplash(SPLASH_MIN_MILLIS + 1_000, true))
    }

    @Test
    fun `waits for the minimum time`() {
        assertFalse(shouldAdvanceSplash(0, true))
        assertFalse(shouldAdvanceSplash(SPLASH_MIN_MILLIS - 1, true))
    }

    @Test
    fun `waits for inbox data`() {
        assertFalse(shouldAdvanceSplash(SPLASH_MIN_MILLIS, false))
        assertFalse(shouldAdvanceSplash(SPLASH_MAX_WAIT_MILLIS + 1_000, false))
    }
}
