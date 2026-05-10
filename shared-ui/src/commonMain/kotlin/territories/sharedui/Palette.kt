package territories.sharedui

/**
 * Pure-data colour palette shared between the Android and Desktop UIs.
 *
 * Values are 32-bit ARGB longs (`0xAARRGGBB`). Each platform app converts
 * to its own Compose `Color` (Android uses `androidx.compose.ui.graphics.Color`
 * via the AndroidX Compose BOM; Desktop uses the same class via the JetBrains
 * Compose Multiplatform plugin — but the dependency graphs differ, so this
 * module avoids importing either).
 *
 * Two presets ship today:
 *  - [LIGHT] — cream/indigo, used by the Android app.
 *  - [DARK]  — charcoal/indigo, used by the Desktop app.
 *
 * Both share the same player-stone colours so a player who plays on both
 * surfaces sees a consistent visual identity.
 */
data class Palette(
    // Player stones
    val playerA: Long,
    val playerAHighlight: Long,
    val playerATerritory: Long,
    val playerB: Long,
    val playerBHighlight: Long,
    val playerBTerritory: Long,
    val playerC: Long,
    val playerCHighlight: Long,
    val playerCTerritory: Long,
    val playerD: Long,
    val playerDHighlight: Long,
    val playerDTerritory: Long,

    // Board surface
    val gridMinor: Long,
    val gridMajor: Long,
    val boardBgTop: Long,
    val boardBgBottom: Long,
    val boardBorder: Long,
    val dotShadow: Long,

    // Last-move ring
    val lastMoveRing: Long
) {
    companion object {
        /** Cream-and-indigo palette used by the Android app. */
        val LIGHT = Palette(
            playerA           = 0xFF3949AB,
            playerAHighlight  = 0xFF7986CB,
            playerATerritory  = 0x553949AB,
            playerB           = 0xFFD84A4A,
            playerBHighlight  = 0xFFE57373,
            playerBTerritory  = 0x55D84A4A,
            playerC           = 0xFF2E7D32,
            playerCHighlight  = 0xFF66BB6A,
            playerCTerritory  = 0x552E7D32,
            playerD           = 0xFFF9A825,
            playerDHighlight  = 0xFFFFD54F,
            playerDTerritory  = 0x55F9A825,
            gridMinor         = 0x1F000000,
            gridMajor         = 0x33000000,
            boardBgTop        = 0xFFFBF7EC,
            boardBgBottom     = 0xFFF1EAD8,
            boardBorder       = 0x33000000,
            dotShadow         = 0x55000000,
            lastMoveRing      = 0xFFFFB300
        )

        /** Charcoal-and-indigo palette used by the Desktop app. */
        val DARK = Palette(
            playerA           = 0xFF3949AB,
            playerAHighlight  = 0xFF7986CB,
            playerATerritory  = 0x553949AB,
            playerB           = 0xFFD84A4A,
            playerBHighlight  = 0xFFE57373,
            playerBTerritory  = 0x55D84A4A,
            playerC           = 0xFF43A047,
            playerCHighlight  = 0xFF81C784,
            playerCTerritory  = 0x5543A047,
            playerD           = 0xFFFFB300,
            playerDHighlight  = 0xFFFFD54F,
            playerDTerritory  = 0x55FFB300,
            gridMinor         = 0x1FFFFFFF,
            gridMajor         = 0x40FFFFFF,
            boardBgTop        = 0xFF26282F,
            boardBgBottom     = 0xFF181A20,
            boardBorder       = 0x66FFFFFF,
            dotShadow         = 0x66000000,
            lastMoveRing      = 0xFFFFB300
        )
    }
}
