package territories.sharedui

import kotlin.test.Test
import kotlin.test.assertEquals

class PaletteTest {

    @Test
    fun bothPresetsShareSameStoneColors() {
        // Visual-identity contract: a player should see the same indigo/coral
        // for stones whether they're on Android or Desktop.
        assertEquals(Palette.LIGHT.playerA, Palette.DARK.playerA)
        assertEquals(Palette.LIGHT.playerB, Palette.DARK.playerB)
        assertEquals(Palette.LIGHT.playerAHighlight, Palette.DARK.playerAHighlight)
        assertEquals(Palette.LIGHT.playerBHighlight, Palette.DARK.playerBHighlight)
        assertEquals(Palette.LIGHT.lastMoveRing, Palette.DARK.lastMoveRing)
    }

    @Test
    fun lightAndDarkBackgroundsActuallyDiffer() {
        // Sanity: someone refactoring shouldn't accidentally identify the two presets.
        assert(Palette.LIGHT.boardBgTop != Palette.DARK.boardBgTop)
        assert(Palette.LIGHT.gridMinor != Palette.DARK.gridMinor)
    }

    @Test
    fun argbValuesAreInRange() {
        for (palette in listOf(Palette.LIGHT, Palette.DARK)) {
            for (color in listOf(
                palette.playerA, palette.playerB, palette.boardBgTop, palette.boardBgBottom,
                palette.gridMinor, palette.gridMajor, palette.lastMoveRing
            )) {
                assert(color in 0L..0xFFFFFFFFL) { "Out-of-range ARGB: 0x${color.toString(16)}" }
            }
        }
    }
}
