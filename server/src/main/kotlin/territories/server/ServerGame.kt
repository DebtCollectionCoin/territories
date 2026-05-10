package territories.server

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import territories.engine.engine.GameEngine
import territories.engine.engine.LegalMoveChecker
import territories.engine.model.Coord
import territories.engine.model.GameConfig
import territories.engine.model.GameState
import territories.engine.model.Player
import kotlin.concurrent.Volatile

/**
 * Authoritative server-side state for a single live game.
 *
 * The server owns a [GameEngine] instance and applies moves on behalf
 * of clients. The state flow lets connected sessions subscribe and
 * push changes back to their respective WebSockets.
 */
class ServerGame(
    val gameId: String,
    val config: GameConfig,
    val seatOrder: Map<Player, String>, // seat → userId
) {
    private val engine = GameEngine(config)
    private val checker = LegalMoveChecker()
    private val _state = MutableStateFlow(engine.initialState())

    val state = _state.asStateFlow()

    @Volatile
    var ended: Boolean = false
        private set

    /** True if [userId] currently sits at [seat]. */
    fun isUserAtSeat(userId: String, seat: Player): Boolean =
        seatOrder[seat] == userId

    /** Returns the seat for [userId], or null if not seated. */
    fun seatOf(userId: String): Player? =
        seatOrder.entries.firstOrNull { it.value == userId }?.key

    /**
     * Apply a move on behalf of [userId]. Returns the new state on
     * success, or a [MoveError] describing why it was rejected.
     */
    sealed interface MoveResult {
        data class Applied(val newState: GameState) : MoveResult
        data class Rejected(val reason: String) : MoveResult
    }

    fun applyMove(userId: String, coord: Coord, expectedMoveCount: Int): MoveResult {
        if (ended) return MoveResult.Rejected("game ended")
        val current = _state.value
        if (current.moveCount != expectedMoveCount) {
            return MoveResult.Rejected(
                "stale move (server=${current.moveCount}, client=$expectedMoveCount)"
            )
        }
        val seat = seatOf(userId) ?: return MoveResult.Rejected("not seated")
        if (current.currentPlayer != seat) {
            return MoveResult.Rejected("not your turn (current=${current.currentPlayer}, you=$seat)")
        }
        if (!checker.isLegal(coord, current)) {
            return MoveResult.Rejected("illegal move at ${coord.col},${coord.row}")
        }
        val applied = engine.applyMove(current, coord).getOrElse {
            return MoveResult.Rejected(it.message ?: "engine rejected move")
        }
        _state.value = applied
        if (applied.isGameOver) ended = true
        return MoveResult.Applied(applied)
    }

    /**
     * Resign on behalf of [userId]. Delegates to the engine's
     * [GameEngine.surrender] which handles both 2-player (immediate
     * end) and N-player (eliminate seat, continue if >1 left) cases.
     */
    fun resign(userId: String): MoveResult {
        if (ended) return MoveResult.Rejected("game ended")
        val seat = seatOf(userId) ?: return MoveResult.Rejected("not seated")
        val applied = engine.surrender(_state.value, seat)
        _state.value = applied
        if (applied.isGameOver) ended = true
        return MoveResult.Applied(applied)
    }
}
