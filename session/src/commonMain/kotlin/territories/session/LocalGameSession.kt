package territories.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import territories.engine.engine.GameEngine
import territories.engine.history.UndoManager
import territories.engine.model.*

class LocalGameSession(
    private val engine: GameEngine,
    override val sessionId: String = generateSessionId()
) : GameSession {

    private val _stateFlow = MutableStateFlow(engine.initialState())
    override val stateFlow: StateFlow<GameState> = _stateFlow

    private val undoManager = UndoManager(engine.config.undoLimit)

    override suspend fun submitMove(move: Move): Result<GameState> {
        return when (move) {
            is Move.PlaceDot -> {
                val current = _stateFlow.value
                undoManager.recordMove(current)
                val result = engine.applyMove(current, move.coord)
                result.onSuccess { newState -> _stateFlow.value = newState }
                result.onFailure { undoManager.undo() } // rollback history on failure
                result
            }
            is Move.Surrender -> {
                val current = _stateFlow.value
                val newState = engine.surrender(current, move.player)
                _stateFlow.value = newState
                Result.success(newState)
            }
        }
    }

    override suspend fun surrender(player: Player): GameState {
        val newState = engine.surrender(_stateFlow.value, player)
        _stateFlow.value = newState
        return newState
    }

    override fun observeOpponentMoves(): Flow<Move> = emptyFlow()

    override suspend fun requestUndo(): Boolean {
        val previous = undoManager.undo() ?: return false
        _stateFlow.value = previous
        return true
    }

    fun canUndo(): Boolean = undoManager.canUndo()

    override fun getHistory(): List<GameState> = emptyList() // Full history not exposed locally

    override fun close() {
        undoManager.clear()
    }
}

private fun generateSessionId(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return (1..6).map { chars.random() }.joinToString("")
}
