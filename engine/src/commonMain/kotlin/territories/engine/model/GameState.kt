package territories.engine.model

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val board: Board,
    val currentPlayer: Player,
    val phase: GamePhase,
    val territories: List<Territory>,
    val moveCount: Int,
    val lastMove: Coord?,
    val winner: Player,
    val config: GameConfig,
    val score: Score
) {
    val isGameOver: Boolean get() = phase != GamePhase.IN_PROGRESS
}
