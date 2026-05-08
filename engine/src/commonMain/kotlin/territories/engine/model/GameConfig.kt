package territories.engine.model

import kotlinx.serialization.Serializable

@Serializable
data class GameConfig(
    val cols: Int = 61,
    val rows: Int = 41,
    val scoringVariant: ScoringVariant = ScoringVariant.TERRITORY_AREA,
    val firstPlayer: Player = Player.A,
    val playerAType: PlayerType = PlayerType.HUMAN,
    val playerBType: PlayerType = PlayerType.HUMAN,
    val undoLimit: Int = 50,
    val allowSelfCapture: Boolean = false
) {
    companion object {
        val SMALL = GameConfig(cols = 21, rows = 16)
        val MEDIUM = GameConfig(cols = 31, rows = 21)
        val LARGE = GameConfig(cols = 61, rows = 41)
    }
}
