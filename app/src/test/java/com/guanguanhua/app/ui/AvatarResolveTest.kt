package com.guanguanhua.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarResolveTest {

    @Test
    fun presetsStartWithMascotsThenNineThemeIds() {
        assertEquals("mascot_cat", AvatarIds.ALL.first())
        assertEquals("mascot_dog", AvatarIds.ALL[1])
        assertEquals(9, AvatarIds.THEME.size)
        assertEquals(11, AvatarIds.ALL.size)
        assertTrue(AvatarIds.known("penguin"))
        assertTrue(!AvatarIds.known("dragon"))
    }

    @Test
    fun uploadedPhotoWinsOverPreset() {
        assertEquals(
            AvatarView.Photo("https://example/a.png"),
            resolveAvatar("https://example/a.png", "kitten", AvatarIds.CAT),
        )
    }

    @Test
    fun knownPresetIsUsedWhenNoPhoto() {
        assertEquals(
            AvatarView.Preset("corgi"),
            resolveAvatar(null, "corgi", AvatarIds.CAT),
        )
    }

    @Test
    fun unknownOrBlankPresetFallsBack() {
        assertEquals(AvatarView.Preset(AvatarIds.DOG), resolveAvatar(null, "dragon", AvatarIds.DOG))
        assertEquals(AvatarView.Preset(AvatarIds.CAT), resolveAvatar("  ", "  ", AvatarIds.CAT))
    }
}
