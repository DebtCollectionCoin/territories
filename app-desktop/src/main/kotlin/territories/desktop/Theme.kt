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
    val PlayerC          = Color(P.playerC)
    val PlayerCHighlight = Color(P.playerCHighlight)
    val PlayerCTerritory = Color(P.playerCTerritory)
    val PlayerD          = Color(P.playerD)
    val PlayerDHighlight = Color(P.playerDHighlight)
    val PlayerDTerritory = Color(P.playerDTerritory)

    /** Color for a seat's stones. NONE returns a neutral on-surface color. */
    fun forPlayer(p: territories.engine.model.Player): Color = when (p) {
        territories.engine.model.Player.A -> PlayerA
        territories.engine.model.Player.B -> PlayerB
        territories.engine.model.Player.C -> PlayerC
        territories.engine.model.Player.D -> PlayerD
        territories.engine.model.Player.NONE -> OnSurface
    }

    fun highlightFor(p: territories.engine.model.Player): Color = when (p) {
        territories.engine.model.Player.A -> PlayerAHighlight
        territories.engine.model.Player.B -> PlayerBHighlight
        territories.engine.model.Player.C -> PlayerCHighlight
        territories.engine.model.Player.D -> PlayerDHighlight
        territories.engine.model.Player.NONE -> OnSurface
    }

    fun territoryFor(p: territories.engine.model.Player): Color = when (p) {
        territories.engine.model.Player.A -> PlayerATerritory
        territories.engine.model.Player.B -> PlayerBTerritory
        territories.engine.model.Player.C -> PlayerCTerritory
        territories.engine.model.Player.D -> PlayerDTerritory
        territories.engine.model.Player.NONE -> OnSurface
    }

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
