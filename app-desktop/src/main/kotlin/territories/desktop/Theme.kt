package territories.desktop

import androidx.compose.ui.graphics.Color
import territories.sharedui.Palette

private val P = Palette.DARK

object AppColors {
    // Player stones
    val PlayerA          = Color(P.playerA)
    val PlayerAHighlight = Color(P.playerAHighlight)
    val PlayerATerritory = Color(P.playerATerritory)
    val PlayerB          = Color(P.playerB)
    val PlayerBHighlight = Color(P.playerBHighlight)
    val PlayerBTerritory = Color(P.playerBTerritory)

    // Board surface (dark theme for desktop)
    val GridMinor        = Color(P.gridMinor)
    val GridMajor        = Color(P.gridMajor)
    val BoardBgTop       = Color(P.boardBgTop)
    val BoardBgBottom    = Color(P.boardBgBottom)
    val BoardBorder      = Color(P.boardBorder)
    val DotShadow        = Color(P.dotShadow)

    // App chrome (Desktop-specific, not yet shared)
    val Background       = Color(0xFF121316)
    val Surface          = Color(0xFF22242B)
    val OnSurface        = Color(0xFFEDEEF3)
    val OnSurfaceDim     = Color(0xFFA0A4B0)
    val LastMoveRing     = Color(P.lastMoveRing)
}
