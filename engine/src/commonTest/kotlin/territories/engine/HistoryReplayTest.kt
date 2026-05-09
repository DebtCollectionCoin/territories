package territories.engine

import territories.engine.engine.GameEngine
import territories.engine.model.*
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end check for the move-replay pipeline used by Android (Room → GameRepository
 * → GameViewModel.resumeGame) and the web app (localStorage SavedGame → GameController.resumeGame).
 *
 * Both paths reduce to: record [Move.PlaceDot] entries during play, then replay them on a fresh
 * [GameEngine] and assert the final [GameState] is identical to the original.
 *
 * If this test ever fails, saved games will silently desync on resume.
 */
class HistoryReplayTest {

    @Test
    fun replayingRecordedMovesYieldsIdenticalFinalState() {
        for (seed in 1..5) {
            verifyOneGame(GameConfig.SMALL, seed.toLong())
        }
    }

    @Test
    fun replayingPartialHistoryYieldsCorrectIntermediateState() {
        val (engine, fullHistory, finalState) = playRandomGame(GameConfig.SMALL, seed = 42L)

        // Replay every prefix length and ensure each matches the corresponding state from
        // the original play-through.
        val originalStates = mutableListOf(engine.initialState())
        for (move in fullHistory) {
            val s = engine.applyMove(originalStates.last(), move.coord).getOrThrow()
            originalStates.add(s)
        }
        // Sanity: last replay matches the captured final state.
        assertStatesEqual(finalState, originalStates.last())

        for (prefixLen in 0..fullHistory.size) {
            var s = engine.initialState()
            for (i in 0 until prefixLen) {
                s = engine.applyMove(s, fullHistory[i].coord).getOrThrow()
            }
            assertStatesEqual(
                originalStates[prefixLen], s,
                "Prefix length $prefixLen diverged on replay"
            )
        }
    }

    @Test
    fun replayPreservesScoreAndWinner() {
        val (_, _, finalState) = playRandomGame(GameConfig.SMALL, seed = 7L, maxMoves = 200)
        // Winner / score are derived from board contents — replaying the same moves
        // must give the same answer.
        val replay = playRandomGame(GameConfig.SMALL, seed = 7L, maxMoves = 200).third
        assertEquals(finalState.score, replay.score)
        assertEquals(finalState.winner, replay.winner)
        assertEquals(finalState.phase, replay.phase)
    }

    // ── Helpers ─────────────────────────────────────────

    private fun verifyOneGame(config: GameConfig, seed: Long) {
        val (engine, history, original) = playRandomGame(config, seed)
        assertTrue(history.isNotEmpty(), "Game seed=$seed produced no moves")

        var replay = engine.initialState()
        for (move in history) {
            val res = engine.applyMove(replay, move.coord)
            assertTrue(res.isSuccess, "Replay failed at move ${move.moveNumber} (seed=$seed)")
            replay = res.getOrThrow()
        }
        assertStatesEqual(original, replay, "Replay diverged for seed=$seed")
    }

    /**
     * Drives a randomised game using only legal moves, returning the engine, the recorded
     * move history, and the final state.
     */
    private fun playRandomGame(
        config: GameConfig,
        seed: Long,
        maxMoves: Int = 120
    ): Triple<GameEngine, List<Move.PlaceDot>, GameState> {
        val rng = Random(seed)
        val engine = GameEngine(config)
        var state = engine.initialState()
        val history = mutableListOf<Move.PlaceDot>()

        repeat(maxMoves) {
            if (state.isGameOver) return@repeat
            val candidates = state.board.allCoords().filter { coord ->
                val cell = state.board.get(coord)
                cell.dot == Player.NONE && cell.territory == Player.NONE
            }.toList()
            if (candidates.isEmpty()) return@repeat
            val coord = candidates[rng.nextInt(candidates.size)]
            val before = state
            val result = engine.applyMove(state, coord)
            if (result.isSuccess) {
                history.add(Move.PlaceDot(coord, before.currentPlayer, before.moveCount + 1))
                state = result.getOrThrow()
            }
        }
        return Triple(engine, history, state)
    }

    private fun assertStatesEqual(expected: GameState, actual: GameState, hint: String = "") {
        val msg = if (hint.isEmpty()) "" else " ($hint)"
        assertEquals(expected.currentPlayer, actual.currentPlayer, "currentPlayer$msg")
        assertEquals(expected.phase, actual.phase, "phase$msg")
        assertEquals(expected.moveCount, actual.moveCount, "moveCount$msg")
        assertEquals(expected.lastMove, actual.lastMove, "lastMove$msg")
        assertEquals(expected.winner, actual.winner, "winner$msg")
        assertEquals(expected.score, actual.score, "score$msg")
        assertEquals(expected.config, actual.config, "config$msg")
        assertEquals(expected.territories, actual.territories, "territories$msg")
        // Board: no equals(), compare cell-by-cell.
        val a = expected.board
        val b = actual.board
        assertEquals(a.cols, b.cols, "board.cols$msg")
        assertEquals(a.rows, b.rows, "board.rows$msg")
        for (coord in a.allCoords()) {
            assertEquals(a.get(coord), b.get(coord), "cell at $coord$msg")
        }
    }
}
