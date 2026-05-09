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
    val score: Score,
    /** Seats in turn order. Defaults to [config.seats] for back-compat. */
    val players: List<Player> = config.seats,
    /** Seats that have been eliminated (no legal moves remain) and are skipped. */
    val eliminated: Set<Player> = emptySet(),
    /** Number of consecutive passes by active seats since the last placement. */
    val passes: Int = 0
) {
    val isGameOver: Boolean get() = phase != GamePhase.IN_PROGRESS

    /** Active (non-eliminated) seats in turn order. */
    val activePlayers: List<Player>
        get() = players.filter { it !in eliminated }

    /**
     * Returns the next seat after [from] that is still active. For 2-player
     * games this is equivalent to [Player.opponent]. For N-player games it
     * walks the seat list, skipping eliminated seats. Returns [Player.NONE]
     * if no active seats remain.
     */
    fun nextSeat(from: Player): Player {
        val active = activePlayers
        if (active.isEmpty()) return Player.NONE
        val idx = active.indexOf(from)
        if (idx == -1) return active.first()
        return active[(idx + 1) % active.size]
    }
}
