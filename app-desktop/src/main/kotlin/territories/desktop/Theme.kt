package territories.desktop

import androidx.compose.ui.graphics.Color

object AppColors {
    // Player stones
    val PlayerA          = Color(0xFF3949AB)
    val PlayerAHighlight = Color(0xFF7986CB)
    val PlayerATerritory = Color(0x553949AB)
    val PlayerB          = Color(0xFFD84A4A)
    val PlayerBHighlight = Color(0xFFE57373)
    val PlayerBTerritory = Color(0x55D84A4A)

    // Board surface (dark theme for desktop)
    val GridMinor        = Color(0x1FFFFFFF)
    val GridMajor        = Color(0x40FFFFFF)
    val BoardBgTop       = Color(0xFF26282F)
    val BoardBgBottom    = Color(0xFF181A20)
    val BoardBorder      = Color(0x66FFFFFF)
    val DotShadow        = Color(0x66000000)

    // App chrome
    val Background       = Color(0xFF121316)
    val Surface          = Color(0xFF22242B)
    val OnSurface        = Color(0xFFEDEEF3)
    val OnSurfaceDim     = Color(0xFFA0A4B0)
    val LastMoveRing     = Color(0xFFFFB300)
}
