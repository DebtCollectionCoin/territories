package territories.web

import kotlinx.coroutines.*
import territories.engine.ai.*
import territories.engine.model.*
import territories.session.GameSessionFactory
import territories.session.LocalGameSession

/**
 * Drives the game logic. Tracks applied moves to support save/resume.
 */
class GameController {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var session: LocalGameSession? = null
    private var aiBySeat: Map<Player, AiPlayer?> = emptyMap()
    private var typeBySeat: Map<Player, PlayerType> = emptyMap()
    private var config: GameConfig? = null
    private val appliedMoves = mutableListOf<Move>()

    var onStateChanged: ((GameState) -> Unit)? = null
    var onAiThinking: ((Boolean) -> Unit)? = null
    var onMovePlaced: ((capturedTerritory: Boolean) -> Unit)? = null

    val currentState: GameState? get() = session?.stateFlow?.value
    val isGameActive: Boolean get() = session != null && currentState?.isGameOver == false

    /** Snapshot of the current in-progress game suitable for sharing or storage. */
    fun currentSavedGame(): SavedGame? {
        val cfg = config ?: return null
        if (appliedMoves.isEmpty()) return null
        return SavedGame(
            config = cfg,
            playerAType = typeBySeat[Player.A] ?: PlayerType.HUMAN,
            playerBType = typeBySeat[Player.B] ?: PlayerType.HUMAN,
            playerCType = typeBySeat[Player.C] ?: PlayerType.HUMAN,
            playerDType = typeBySeat[Player.D] ?: PlayerType.HUMAN,
            moves = appliedMoves.toList()
        )
    }

    fun startGame(config: GameConfig, types: Map<Player, PlayerType>) {
        session?.close()
        session = GameSessionFactory.createLocal(config)
        this.config = config
        typeBySeat = config.seats.associateWith { (types[it] ?: PlayerType.HUMAN) }
        aiBySeat = typeBySeat.mapValues { (_, t) -> buildAi(t, config) }
        appliedMoves.clear()
        SavedGameStore.clear()
        notifyState()
        if (currentState?.currentPlayer?.let { isAiTurn(it) } == true) {
            scheduleAiTurn()
        }
    }

    fun resumeGame(saved: SavedGame): Boolean {
        return try {
            session?.close()
            session = GameSessionFactory.createLocal(saved.config)
            config = saved.config
            typeBySeat = mapOf(
                Player.A to saved.playerAType,
                Player.B to saved.playerBType,
                Player.C to saved.playerCType,
                Player.D to saved.playerDType
            ).filterKeys { it in saved.config.seats }
            aiBySeat = typeBySeat.mapValues { (_, t) -> buildAi(t, saved.config) }
            appliedMoves.clear()

            scope.launch(Dispatchers.Main) {
                for (move in saved.moves) {
                    val s = session ?: break
                    val res = s.submitMove(move)
                    if (res.isSuccess) appliedMoves.add(move) else break
                }
                notifyState()
                val state = currentState
                if (state != null && !state.isGameOver && isAiTurn(state.currentPlayer)) {
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

        scope.launch(Dispatchers.Main) {
            val before = state
            val move = Move.PlaceDot(coord, state.currentPlayer, state.moveCount + 1)
            val result = s.submitMove(move)
            if (result.isSuccess) {
                appliedMoves.add(move)
                val newState = result.getOrThrow()
                emitMovePlaced(before, newState)
                persist()
                notifyState()
                if (!newState.isGameOver && isAiTurn(newState.currentPlayer)) {
                    scheduleAiTurn()
                }
            }
        }
    }

    fun undo() {
        scope.launch(Dispatchers.Main) {
            val s = session ?: return@launch
            if (s.requestUndo()) {
                if (appliedMoves.isNotEmpty()) appliedMoves.removeAt(appliedMoves.size - 1)
                persist()
                notifyState()
            }
        }
    }

    fun surrender() {
        val s = session ?: return
        val player = s.stateFlow.value.currentPlayer
        scope.launch(Dispatchers.Main) {
            val state = s.stateFlow.value
            val move = Move.Surrender(player, state.moveCount + 1)
            s.surrender(player)
            appliedMoves.add(move)
            persist()
            notifyState()
        }
    }

    fun canUndo(): Boolean = session?.canUndo() ?: false

    fun quit() {
        session?.close()
        session = null
        appliedMoves.clear()
        SavedGameStore.clear()
    }

    private fun scheduleAiTurn() {
        val s = session ?: return
        val state = s.stateFlow.value
        val ai = aiBySeat[state.currentPlayer] ?: return
        scope.launch {
            onAiThinking?.invoke(true)
            val coord = ai.selectMove(state)
            withContext(Dispatchers.Main) {
                val before = state
                val move = Move.PlaceDot(coord, state.currentPlayer, state.moveCount + 1)
                val r = s.submitMove(move)
                if (r.isSuccess) {
                    appliedMoves.add(move)
                    persist()
                    emitMovePlaced(before, s.stateFlow.value)
                }
                onAiThinking?.invoke(false)
                notifyState()
                val newState = s.stateFlow.value
                if (!newState.isGameOver && isAiTurn(newState.currentPlayer)) {
                    scheduleAiTurn()
                }
            }
        }
    }

    private fun emitMovePlaced(before: GameState, after: GameState) {
        val captured = countTerritory(after) > countTerritory(before)
        onMovePlaced?.invoke(captured)
    }

    private fun countTerritory(state: GameState): Int {
        var n = 0
        for (coord in state.board.allCoords()) {
            if (state.board.get(coord).territory != Player.NONE) n++
        }
        return n
    }

    private fun persist() {
        val cfg = config ?: return
        val state = currentState ?: return
        if (state.isGameOver || appliedMoves.isEmpty()) {
            SavedGameStore.clear()
            return
        }
        SavedGameStore.save(
            SavedGame(
                config      = cfg,
                playerAType = typeBySeat[Player.A] ?: PlayerType.HUMAN,
                playerBType = typeBySeat[Player.B] ?: PlayerType.HUMAN,
                playerCType = typeBySeat[Player.C] ?: PlayerType.HUMAN,
                playerDType = typeBySeat[Player.D] ?: PlayerType.HUMAN,
                moves       = appliedMoves.toList()
            )
        )
    }

    private fun isAiTurn(player: Player): Boolean =
        (typeBySeat[player] ?: PlayerType.HUMAN) != PlayerType.HUMAN

    private fun buildAi(type: PlayerType, config: GameConfig): AiPlayer? {
        val engine = territories.engine.engine.GameEngine(config)
        return when (type) {
            PlayerType.AI_EASY -> EasyAiPlayer()
            PlayerType.AI_MEDIUM -> MediumAiPlayer(engine)
            PlayerType.AI_HARD -> HardAiPlayer(engine)
            PlayerType.HUMAN -> null
        }
    }

    private fun notifyState() {
        session?.stateFlow?.value?.let { onStateChanged?.invoke(it) }
    }
}
