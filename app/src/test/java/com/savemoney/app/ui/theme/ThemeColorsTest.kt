package com.savemoney.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeColorsTest {

    @Test
    fun lightCanvasAndPaperStayCoolQ() {
        assertEquals(Color(0xFFF7F8FB), LightQColors.canvas)
        assertEquals(Color(0xFFFFFFFF), LightQColors.paper)
        assertFalse(LightQColors.isDark)
    }

    @Test
    fun darkCanvasAndPaperMatchPrototype() {
        assertEquals(Color(0xFF12151C), DarkQColors.canvas)
        assertEquals(Color(0xFF1C212B), DarkQColors.paper)
        assertTrue(DarkQColors.isDark)
    }

    @Test
    fun coralIsSharedAcrossThemes() {
        assertEquals(Color(0xFFF07A5C), LightQColors.coral)
        assertEquals(LightQColors.coral, DarkQColors.coral)
    }

    @Test
    fun darkAccentsMatchApprovedTokens() {
        assertEquals(Color(0xFF85BAD8), DarkQColors.sky)
        assertEquals(Color(0xFF63CDB8), DarkQColors.mint)
        assertEquals(Color(0xFFBBA0D8), DarkQColors.lavender)
    }

    @Test
    fun appearancePrefRoundTrips() {
        assertEquals(Appearance.System, Appearance.fromPref(null))
        assertEquals(Appearance.Dark, Appearance.fromPref("dark"))
        assertTrue(Appearance.Dark.isDark(systemDark = false))
        assertFalse(Appearance.Light.isDark(systemDark = true))
        assertTrue(Appearance.System.isDark(systemDark = true))
    }
}
