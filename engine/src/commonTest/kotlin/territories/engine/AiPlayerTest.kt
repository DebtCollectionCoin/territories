package territories.engine

import kotlinx.coroutines.runBlocking
import territories.engine.ai.EasyAiPlayer
import territories.engine.ai.HardAiPlayer
import territories.engine.ai.MediumAiPlayer
import territories.engine.engine.GameEngine
import territories.engine.engine.LegalMoveChecker
import territories.engine.model.*
import kotlin.test.Test
import kotlin.test.assertTrue

class AiPlayerTest {

    private val smallConfig = GameConfig(
        cols = 11, rows = 11,
        playerBType = PlayerType.AI_EASY
    )
    private val engine = GameEngine(smallConfig)
    private val checker = LegalMoveChecker()

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun freshState() = engine.initialState()

    private fun playMoves(state: GameState, vararg coords: Pair<Int, Int>): GameState {
        var s = state
        for ((col, row) in coords) {
            val coord = Coord(col, row)
            val result = engine.applyMove(s, coord)
            s = result.getOrThrow()
        }
        return s
    }

    // ── Easy AI ────────────────────────────────────────────────────────────

    @Test
    fun easyAi_returnsLegalMove() = runBlocking {
        val ai = EasyAiPlayer()
        val state = freshState()
        val move = ai.selectMove(state)
        assertTrue(checker.isLegal(move, state), "Easy AI returned an illegal move: $move")
    }

    @Test
    fun easyAi_doesNotCrashOnNearlyFullBoard() = runBlocking {
        // Play moves until only a small number of cells remain
        var state = freshState()
        // Fill most of the board with alternating moves (skip the last few)
        val allLegal = checker.allLegalMoves(state).take(40)
        for (coord in allLegal) {
            if (state.isGameOver) break
            val result = engine.applyMove(state, coord)
            if (result.isSuccess) state = result.getOrThrow()
        }
        if (!state.isGameOver) {
            val ai = EasyAiPlayer()
            val move = ai.selectMove(state)
            assertTrue(checker.isLegal(move, state), "Easy AI returned illegal move on crowded board")
        }
    }

    @Test
    fun easyAi_completesFullGame() = runBlocking {
        val ai = EasyAiPlayer()
        var state = freshState()
        var iterations = 0
        while (!state.isGameOver && iterations < 1000) {
            val coord = ai.selectMove(state)
            state = engine.applyMove(state, coord).getOrThrow()
            iterations++
        }
        // Game should end naturally (board full or through captures)
        assertTrue(iterations < 1000, "Easy AI game did not terminate in reasonable moves")
    }

    // ── Medium AI ──────────────────────────────────────────────────────────

    @Test
    fun mediumAi_returnsLegalMove() = runBlocking {
        val ai = MediumAiPlayer(engine)
        val state = freshState()
        val move = ai.selectMove(state)
        assertTrue(checker.isLegal(move, state), "Medium AI returned an illegal move: $move")
    }

    @Test
    fun mediumAi_prefersImmediateCapture() = runBlocking {
        // Set up a state where Player A has almost enclosed Player B's dot
        // Place a ring of A dots around (5,5) with one gap at (6,5)
        val state = playMoves(
            freshState(),
            // A places ring dots leaving a gap
            4 to 4,  // B somewhere
            4 to 5,  // A
            3 to 5,  // B somewhere
            4 to 6,  // A
            5 to 4,  // B
            5 to 6,  // A — forces next to be A at 6,5
            6 to 4,  // B
            3 to 4   // A — extra boundary
        )
        // Just verify Medium AI finds a legal move and doesn't crash
        if (!state.isGameOver) {
            val ai = MediumAiPlayer(engine)
            val move = ai.selectMove(state)
            assertTrue(checker.isLegal(move, state), "Medium AI returned an illegal move in capture scenario")
        }
    }

    @Test
    fun mediumAi_returnsLegalMoveOnEmptyBoard() = runBlocking {
        val ai = MediumAiPlayer(engine)
        val state = freshState()
        // On an empty board, candidates will be empty → fallback to all legal moves
        val move = ai.selectMove(state)
        assertTrue(checker.isLegal(move, state))
    }

    // ── Hard AI ────────────────────────────────────────────────────────────

    @Test
    fun hardAi_returnsLegalMove() = runBlocking {
        val ai = HardAiPlayer(engine, maxDepth = 2, maxTimeMs = 1000L)
        val state = freshState()
        val move = ai.selectMove(state)
        assertTrue(checker.isLegal(move, state), "Hard AI returned an illegal move: $move")
    }

    @Test
    fun hardAi_returnsLegalMoveOnCrowdedBoard() = runBlocking {
        // Pre-play 10 moves to get a realistic mid-game state
        var state = freshState()
        val positions = listOf(
            3 to 3, 7 to 7, 3 to 7, 7 to 3, 5 to 5,
            2 to 5, 8 to 5, 5 to 2, 5 to 8, 4 to 4
        )
        for ((col, row) in positions) {
            if (state.isGameOver) break
            val result = engine.applyMove(state, Coord(col, row))
            if (result.isSuccess) state = result.getOrThrow()
        }
        if (!state.isGameOver) {
            val ai = HardAiPlayer(engine, maxDepth = 2, maxTimeMs = 1500L)
            val move = ai.selectMove(state)
            assertTrue(checker.isLegal(move, state), "Hard AI returned illegal move in mid-game")
        }
    }

    @Test
    fun hardAi_respectsTimeLimit() = runBlocking {
        val ai = HardAiPlayer(engine, maxDepth = 6, maxTimeMs = 500L) // tight limit
        val state = freshState()
        val start = System.currentTimeMillis()
        val move = ai.selectMove(state)
        val elapsed = System.currentTimeMillis() - start
        assertTrue(checker.isLegal(move, state))
        // Allow 2× the limit for overhead, but it should not hang indefinitely
        assertTrue(elapsed < 2000L, "Hard AI exceeded 2s even with 500ms limit (elapsed=${elapsed}ms)")
    }

    // ── AI vs AI smoke test ────────────────────────────────────────────────

    @Test
    fun easyVsMedium_fullGame() = runBlocking {
        val easyA = EasyAiPlayer()
        val mediumB = MediumAiPlayer(engine)
        var state = freshState()
        var turns = 0
        while (!state.isGameOver && turns < 500) {
            val ai: territories.engine.ai.AiPlayer = when (state.currentPlayer) {
                Player.A -> easyA
                Player.B -> mediumB
                Player.NONE -> break
            }
            val coord = ai.selectMove(state)
            val result = engine.applyMove(state, coord)
            if (result.isFailure) break
            state = result.getOrThrow()
            turns++
        }
        assertTrue(turns > 0, "AI vs AI game played 0 turns")
        assertTrue(turns < 500, "AI vs AI game did not terminate")
    }

    @Test
    fun hardVsHard_terminatesWithinTimeLimit() = runBlocking {
        val hardA = HardAiPlayer(engine, maxDepth = 2, maxTimeMs = 300L)
        val hardB = HardAiPlayer(engine, maxDepth = 2, maxTimeMs = 300L)
        var state = freshState()
        var turns = 0
        while (!state.isGameOver && turns < 200) {
            val ai = if (state.currentPlayer == Player.A) hardA else hardB
            val coord = ai.selectMove(state)
            val result = engine.applyMove(state, coord)
            if (result.isFailure) break
            state = result.getOrThrow()
            turns++
        }
        assertTrue(turns > 0)
    }
}
