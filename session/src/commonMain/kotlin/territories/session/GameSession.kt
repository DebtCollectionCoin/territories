package territories.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import territories.engine.model.GameState
import territories.engine.model.Move
import territories.engine.model.Player

interface GameSession {
    val stateFlow: StateFlow<GameState>
    val sessionId: String

    suspend fun submitMove(move: Move): Result<GameState>
    suspend fun surrender(player: Player): GameState
    fun observeOpponentMoves(): Flow<Move>
    suspend fun requestUndo(): Boolean
    fun getHistory(): List<GameState>
    fun close()
}
