package territories.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import territories.engine.ai.AiPlayer
import territories.engine.ai.EasyAiPlayer
import territories.engine.ai.HardAiPlayer
import territories.engine.ai.MediumAiPlayer
import territories.engine.engine.GameEngine
import territories.engine.model.Coord
import territories.engine.model.GameConfig
import territories.engine.model.GameState
import territories.engine.model.Move
import territories.engine.model.Player
import territories.engine.model.PlayerType
import territories.session.GameSessionFactory
import territories.session.LocalGameSession

class GameViewModel {
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private var session: LocalGameSession? = null
    private var aiPlayer: AiPlayer? = null
    private var playerBType: PlayerType = PlayerType.HUMAN
    private var lastConfig: GameConfig? = null
    private var lastBType: PlayerType = PlayerType.HUMAN

    /** Replayed moves of the live game — kept in sync to persist after each turn. */
    private val appliedMoves = mutableListOf<Move>()

    fun hasSavedGame(): Boolean = SavedGameStore.hasSaved()

    fun startGame(config: GameConfig, pBType: PlayerType) {
        session?.close()
        lastConfig = config
        lastBType = pBType
        playerBType = pBType
        session = GameSessionFactory.createLocal(config)
        aiPlayer = buildAi(pBType, config)
        appliedMoves.clear()
        SavedGameStore.clear()
        _gameState.value = session!!.stateFlow.value
        _isAiThinking.value = false
        if (isAiTurn(config.firstPlayer)) scheduleAiTurn()
    }

    /**
     * Restore an in-progress game from disk by replaying its move list on
     * a fresh engine. Returns false if there's no save or it's corrupt.
     */
    fun resumeSavedGame(): Boolean {
        val saved = SavedGameStore.load() ?: return false
        return try {
            session?.close()
            lastConfig = saved.config
            lastBType = saved.playerBType
            playerBType = saved.playerBType
            session = GameSessionFactory.createLocal(saved.config)
            aiPlayer = buildAi(saved.playerBType, saved.config)
            appliedMoves.clear()

            scope.launch {
                val s = session ?: return@launch
                for (move in saved.moves) {
                    val result = s.submitMove(move)
                    if (result.isSuccess) appliedMoves.add(move) else break
                }
                _gameState.value = s.stateFlow.value
                val state = s.stateFlow.value
                if (!state.isGameOver && isAiTurn(state.currentPlayer)) {
                    scheduleAiTurn()
                }
            }
            true
        } catch (_: Throwable) {
            SavedGameStore.clear()
            false
        }
    }

    fun humanMove(coord: Coord) {
        val s = session ?: return
        val state = s.stateFlow.value
        if (state.isGameOver) return
        if (isAiTurn(state.currentPlayer)) return
        scope.launch {
            val move = Move.PlaceDot(coord, state.currentPlayer, state.moveCount + 1)
            val result = s.submitMove(move)
            if (result.isSuccess) {
                appliedMoves.add(move)
                _gameState.value = s.stateFlow.value
                val newState = result.getOrThrow()
                persist(newState)
                if (!newState.isGameOver && isAiTurn(newState.currentPlayer)) {
                    scheduleAiTurn()
                }
            }
        }
    }

    fun undo() {
        scope.launch {
            val didUndo = session?.requestUndo() ?: false
            if (didUndo && appliedMoves.isNotEmpty()) {
                appliedMoves.removeAt(appliedMoves.size - 1)
            }
            val newState = session?.stateFlow?.value
            _gameState.value = newState
            if (newState != null) persist(newState)
        }
    }

    fun surrender() {
        val s = session ?: return
        val player = s.stateFlow.value.currentPlayer
        scope.launch {
            s.surrender(player)
            _gameState.value = s.stateFlow.value
            // Game's over — clear save so we don't auto-resume a finished game.
            SavedGameStore.clear()
        }
    }

    fun canUndo(): Boolean = session?.canUndo() ?: false

    private fun isAiTurn(player: Player): Boolean =
        player == Player.B && playerBType != PlayerType.HUMAN

    private fun scheduleAiTurn() {
        val ai = aiPlayer ?: return
        val s = session ?: return
        scope.launch {
            _isAiThinking.value = true
            val state = s.stateFlow.value
            val coord = ai.selectMove(state)
            val move = Move.PlaceDot(coord, state.currentPlayer, state.moveCount + 1)
            val result = s.submitMove(move)
            if (result.isSuccess) appliedMoves.add(move)
            _isAiThinking.value = false
            _gameState.value = s.stateFlow.value
            val newState = s.stateFlow.value
            persist(newState)
            if (!newState.isGameOver && isAiTurn(newState.currentPlayer)) {
                scheduleAiTurn()
            }
        }
    }

    private fun persist(state: GameState) {
        val cfg = lastConfig ?: return
        if (state.isGameOver) {
            SavedGameStore.clear()
            return
        }
        SavedGameStore.save(
            SavedGame(
                config = cfg,
                playerAType = PlayerType.HUMAN,
                playerBType = lastBType,
                moves = appliedMoves.toList()
            )
        )
    }

    private fun buildAi(type: PlayerType, config: GameConfig): AiPlayer? {
        val engine = GameEngine(config)
        return when (type) {
            PlayerType.AI_EASY   -> EasyAiPlayer()
            PlayerType.AI_MEDIUM -> MediumAiPlayer(engine)
            PlayerType.AI_HARD   -> HardAiPlayer(engine)
            PlayerType.HUMAN     -> null
        }
    }

    fun dispose() {
        scope.cancel()
        session?.close()
    }
}
