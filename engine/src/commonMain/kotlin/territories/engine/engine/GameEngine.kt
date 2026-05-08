package territories.engine.engine

import territories.engine.model.*

class GameEngine(val config: GameConfig) {

    private val legalMoveChecker = LegalMoveChecker()
    private val captureDetector = CaptureDetector()
    private val scoreCalculator = ScoreCalculator()
    private val gameOverDetector = GameOverDetector(legalMoveChecker)

    fun initialState(): GameState = GameState(
        board = Board.empty(config.cols, config.rows),
        currentPlayer = config.firstPlayer,
        phase = GamePhase.IN_PROGRESS,
        territories = emptyList(),
        moveCount = 0,
        lastMove = null,
        winner = Player.NONE,
        config = config,
        score = Score()
    )

    /**
     * Applies [coord] placement for the current player.
     * Returns the new [GameState] on success, or failure with a descriptive message.
     */
    fun applyMove(state: GameState, coord: Coord): Result<GameState> {
        if (state.isGameOver) return Result.failure(IllegalStateException("Game is already over."))
        if (!legalMoveChecker.isLegal(coord, state)) {
            return Result.failure(IllegalArgumentException("Illegal move at $coord"))
        }

        val player = state.currentPlayer

        // Step 1: Place dot
        var newBoard = state.board.withDot(coord, player)

        // Step 2: Detect captures
        val captured = captureDetector.detectCaptures(coord, player, newBoard)

        // Step 3: Apply territory claims
        val absorbedIds = mutableListOf<String>()
        for (region in captured) {
            newBoard = newBoard.withTerritory(region.cells, player)
            region.capturedTerritories.forEach { absorbedIds.add(it.id) }
        }

        // Step 4: Rebuild territory list
        val newTerritories = buildTerritories(
            existing = state.territories,
            captured = captured,
            player = player,
            moveNumber = state.moveCount + 1
        )

        // Step 5: Compute score
        val intermediateState = state.copy(
            board = newBoard,
            territories = newTerritories
        )
        val newScore = scoreCalculator.calculate(intermediateState)

        // Step 6: Build new state with toggled player
        val nextPlayer = player.opponent()
        var newState = state.copy(
            board = newBoard,
            currentPlayer = nextPlayer,
            territories = newTerritories,
            moveCount = state.moveCount + 1,
            lastMove = coord,
            score = newScore,
            phase = GamePhase.IN_PROGRESS,
            winner = Player.NONE
        )

        // Step 7: Check game over
        if (gameOverDetector.isGameOver(newState)) {
            newState = newState.copy(
                phase = GamePhase.GAME_OVER,
                winner = gameOverDetector.determineWinner(newState)
            )
        }

        return Result.success(newState)
    }

    fun surrender(state: GameState, player: Player): GameState = state.copy(
        phase = GamePhase.SURRENDERED,
        winner = player.opponent(),
        currentPlayer = player
    )

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun buildTerritories(
        existing: List<Territory>,
        captured: List<CapturedRegion>,
        player: Player,
        moveNumber: Int
    ): List<Territory> {
        if (captured.isEmpty()) return existing

        val absorbedIds = captured.flatMap { r -> r.capturedTerritories.map { it.id } }.toSet()
        val remaining = existing.filter { it.id !in absorbedIds }

        val newTerritories = captured.map { region ->
            Territory(
                id = generateId(),
                owner = player,
                cells = region.cells,
                capturedDots = region.capturedDots,
                capturedAt = moveNumber,
                absorbedTerritoryIds = region.capturedTerritories.map { it.id }
            )
        }

        return remaining + newTerritories
    }

    private var idCounter = 0
    private fun generateId(): String = "t${++idCounter}_${randomHex()}"

    private fun randomHex(): String {
        val chars = "0123456789abcdef"
        return (1..8).map { chars.random() }.joinToString("")
    }
}
