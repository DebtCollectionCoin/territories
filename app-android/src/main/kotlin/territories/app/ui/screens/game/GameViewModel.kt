package territories.app.ui.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import territories.app.data.AppPreferencesRepository
import territories.app.data.GameConfigHolder
import territories.app.data.GameRepository
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
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val configHolder: GameConfigHolder,
    private val repository: GameRepository,
    prefs: AppPreferencesRepository
) : ViewModel() {

    private lateinit var session: LocalGameSession
    private var aiPlayer: AiPlayer? = null
    private var currentGameId: String? = null

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    // Mirror the session's StateFlow in our own so we control when it updates
    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    val colorBlindMode: StateFlow<Boolean> = prefs.colorBlindMode.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.Eagerly,
        initialValue = false
    )

    init {
        newGame()
    }

    /** Start a fresh game with the current config from the holder. */
    fun newGame() {
        val resumeId = configHolder.resumeGameId
        if (resumeId != null) {
            configHolder.resumeGameId = null
            resumeGame(resumeId)
        } else {
            startFreshGame()
        }
    }

    private fun startFreshGame() {
        val config = configHolder.current
        if (::session.isInitialized) session.close()
        session = GameSessionFactory.createLocal(config)
        aiPlayer = buildAiPlayer(config)
        _isAiThinking.value = false
        _gameState.value = session.stateFlow.value

        // Wipe any leftover unfinished game and create a fresh Room record
        viewModelScope.launch {
            repository.deleteAllInProgress()
            currentGameId = repository.createGame(config)
        }

        if (isAiTurn(config.firstPlayer)) scheduleAiTurn()
    }

    private fun resumeGame(gameId: String) {
        viewModelScope.launch {
            val entity = repository.getInProgressGame() ?: run {
                startFreshGame()
                return@launch
            }
            val config = repository.decodeConfig(entity)
            configHolder.current = config

            if (::session.isInitialized) session.close()
            session = GameSessionFactory.createLocal(config)
            aiPlayer = buildAiPlayer(config)
            currentGameId = gameId

            // Replay all stored moves
            val moves = repository.loadMoves(gameId)
            for (move in moves) {
                session.submitMove(move)
            }
            _gameState.value = session.stateFlow.value
            _isAiThinking.value = false

            // If it's an AI's turn after replay, kick off its move
            val state = session.stateFlow.value
            if (!state.isGameOver && isAiTurn(state.currentPlayer)) {
                scheduleAiTurn()
            }
        }
    }

    fun onCellTapped(coord: Coord) {
        val state = session.stateFlow.value
        if (state.isGameOver) return
        if (isAiTurn(state.currentPlayer)) return
        viewModelScope.launch {
            val move = Move.PlaceDot(coord, state.currentPlayer, state.moveCount + 1)
            val result = session.submitMove(move)
            if (result.isSuccess) {
                val newState = result.getOrThrow()
                _gameState.value = newState
                currentGameId?.let { id ->
                    repository.recordMove(id, move)
                    if (newState.isGameOver) repository.finalizeGame(id, newState)
                }
                if (!newState.isGameOver && isAiTurn(newState.currentPlayer)) {
                    scheduleAiTurn()
                }
            }
        }
    }

    fun onUndo() {
        viewModelScope.launch {
            if (session.requestUndo()) {
                _gameState.value = session.stateFlow.value
            }
        }
    }

    fun onSurrender() {
        viewModelScope.launch {
            val player = session.stateFlow.value.currentPlayer
            val newState = session.surrender(player)
            _gameState.value = newState
            currentGameId?.let { id ->
                repository.recordMove(id, Move.Surrender(player, newState.moveCount))
                repository.finalizeGame(id, newState)
            }
        }
    }

    fun canUndo(): Boolean = session.canUndo()

    private fun isAiTurn(player: Player): Boolean {
        val config = configHolder.current
        return (player == Player.A && config.playerAType != PlayerType.HUMAN) ||
               (player == Player.B && config.playerBType != PlayerType.HUMAN)
    }

    private fun scheduleAiTurn() {
        val ai = aiPlayer ?: return
        viewModelScope.launch(Dispatchers.Default) {
            _isAiThinking.value = true
            val state = session.stateFlow.value
            if (!state.isGameOver) {
                val coord = ai.selectMove(state)
                val move  = Move.PlaceDot(coord, state.currentPlayer, state.moveCount + 1)
                session.submitMove(move)
                val newState = session.stateFlow.value
                _isAiThinking.value = false
                _gameState.value = newState
                currentGameId?.let { id ->
                    repository.recordMove(id, move)
                    if (newState.isGameOver) repository.finalizeGame(id, newState)
                }
                if (!newState.isGameOver && isAiTurn(newState.currentPlayer)) {
                    scheduleAiTurn()
                }
            } else {
                _isAiThinking.value = false
            }
        }
    }

    private fun buildAiPlayer(config: GameConfig): AiPlayer? {
        val engine = GameEngine(config)
        return when (config.playerBType) {
            PlayerType.AI_EASY   -> EasyAiPlayer()
            PlayerType.AI_MEDIUM -> MediumAiPlayer(engine)
            PlayerType.AI_HARD   -> HardAiPlayer(engine)
            PlayerType.HUMAN     -> null
        }
    }

    override fun onCleared() {
        session.close()
    }
}

