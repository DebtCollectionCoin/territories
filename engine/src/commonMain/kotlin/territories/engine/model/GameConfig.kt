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
    val allowSelfCapture: Boolean = false,
    /** Number of seats. 2 by default; 3 and 4 enable free-for-all play. */
    val playerCount: Int = 2,
    val playerCType: PlayerType = PlayerType.HUMAN,
    val playerDType: PlayerType = PlayerType.HUMAN
) {
    init {
        require(playerCount in 2..4) { "playerCount must be 2..4, was $playerCount" }
    }

    /** Seats actually playing this game, in seat order. */
    val seats: List<Player>
        get() = ALL_SEATS.take(playerCount)

    companion object {
        val SMALL = GameConfig(cols = 21, rows = 16)
        val MEDIUM = GameConfig(cols = 31, rows = 21)
        val LARGE = GameConfig(cols = 61, rows = 41)
    }
}
