package com.guanguanhua.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeColorsTest {

    @Test
    fun lightCanvasAndPaperStayCoolQ() {
        assertEquals(Color(0xFFF7F8FB), LightQColors.canvas)
        assertEquals(Color(0xFFFFFFFF), LightQColors.paper)
        assertEquals(Color(0xFFF07A5C), LightQColors.coral)
        assertFalse(LightQColors.isDark)
    }

    @Test
    fun darkCanvasAndPaperMatchPrototype() {
        assertEquals(Color(0xFF12151C), DarkQColors.canvas)
        assertEquals(Color(0xFF1C212B), DarkQColors.paper)
        assertEquals(Color(0xFF161A22), DarkQColors.wash)
        assertEquals(Color(0xFF1A1F29), DarkQColors.sandDeep)
        assertTrue(DarkQColors.isDark)
    }

    @Test
    fun darkPrimaryButtonIsSkyApproveIsMintCoralStaysForAmounts() {
        assertEquals(Color(0xFFF07858), DarkQColors.coral)
        assertEquals(Color(0xFFFF8F70), DarkQColors.coralBright)
        assertEquals(DarkQColors.sky, DarkQColors.primaryButton)
        assertEquals(DarkQColors.mint, DarkQColors.approveButton)
        assertEquals(DarkQColors.sky, DarkQColors.secondaryStroke)
        assertEquals(LightQColors.coral, LightQColors.primaryButton)
        assertEquals(LightQColors.lineStrong, LightQColors.secondaryStroke)
        assertNotEquals(LightQColors.coral, DarkQColors.coral)
    }

    @Test
    fun darkAccentsMatchApprovedTokens() {
        assertEquals(Color(0xFF7EB8D8), DarkQColors.sky)
        assertEquals(Color(0xFF5ECFB8), DarkQColors.mint)
        assertEquals(Color(0xFFB89AD8), DarkQColors.lavender)
        assertEquals(Color(0xFFE8ECF2), DarkQColors.ink)
        assertEquals(Color(0xFFB8C0CE), DarkQColors.inkSoft)
        assertEquals(Color(0xFF8B95A8), DarkQColors.muted)
        assertEquals(Color(0xFF6A7488), DarkQColors.muted2)
        assertEquals(Color(0xFFEFB86A), DarkQColors.pendingInk)
        assertEquals(Color(0xFFF0909C), DarkQColors.rose)
        assertEquals(Color.White.copy(alpha = 0.06f), DarkQColors.line)
        assertEquals(Color.White.copy(alpha = 0.10f), DarkQColors.lineStrong)
        assertEquals(Color(0xFFB89AD8).copy(alpha = 0.16f), DarkQColors.lavenderSoft)
        assertEquals(DarkQColors.inkSoft, DarkQColors.secondary)
        assertEquals(LightQColors.muted, LightQColors.secondary)
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
