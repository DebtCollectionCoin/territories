package territories.engine

import territories.engine.engine.GameEngine
import territories.engine.model.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * Tests for the N-player (3- or 4-player) free-for-all extensions added in
 * Phase A of the multiplayer roadmap. The 2-player path is exercised by the
 * existing test suite and remains the default.
 */
class FfaEngineTest {

    private fun config(playerCount: Int): GameConfig = GameConfig(
        cols = 9, rows = 9,
        playerCount = playerCount
    )

    @Test
    fun threePlayerInitialState_hasThreeSeats() {
        val engine = GameEngine(config(3))
        val state = engine.initialState()
        assertEquals(listOf(Player.A, Player.B, Player.C), state.players)
        assertEquals(emptySet(), state.eliminated)
        assertEquals(Player.A, state.currentPlayer)
    }

    @Test
    fun fourPlayerInitialState_hasFourSeats() {
        val engine = GameEngine(config(4))
        val state = engine.initialState()
        assertEquals(listOf(Player.A, Player.B, Player.C, Player.D), state.players)
    }

    @Test
    fun turnRotation_threePlayers_cyclesAcdAcdAcd() = runBlocking {
        val engine = GameEngine(config(3))
        var state = engine.initialState()
        val rotation = mutableListOf<Player>()
        rotation.add(state.currentPlayer)
        repeat(6) {
            // Place at a different cell each time
            val coord = Coord(it % 9, it / 9)
            val result = engine.applyMove(state, coord)
            state = result.getOrThrow()
            rotation.add(state.currentPlayer)
        }
        // 3-player rotation A→B→C→A→B→C→A
        assertEquals(
            listOf(Player.A, Player.B, Player.C, Player.A, Player.B, Player.C, Player.A),
            rotation
        )
    }

    @Test
    fun turnRotation_fourPlayers_cyclesAbcdAbcd() = runBlocking {
        val engine = GameEngine(config(4))
        var state = engine.initialState()
        val rotation = mutableListOf<Player>()
        rotation.add(state.currentPlayer)
        repeat(8) {
            val coord = Coord(it % 9, it / 9)
            state = engine.applyMove(state, coord).getOrThrow()
            rotation.add(state.currentPlayer)
        }
        assertEquals(
            listOf(Player.A, Player.B, Player.C, Player.D,
                   Player.A, Player.B, Player.C, Player.D, Player.A),
            rotation
        )
    }

    @Test
    fun threePlayerCapture_aSurroundsB_butCdotIsAlsoCaptured() = runBlocking {
        // 7×7 board. A places a square ring around (3,3); inside are dots from B and C.
        // The rule says any non-A non-empty dot inside is captured. Test that the
        // capture still triggers (since at least one foreign dot is present) and
        // that the captured-dots count includes both colours.
        val engine = GameEngine(config(3).copy(cols = 7, rows = 7,
            scoringVariant = ScoringVariant.CAPTURED_DOTS))
        var state = engine.initialState()

        // Manually drive plays to set up the board:
        // A: ring at row 2,4 and col 2,4 around (3,3)
        // B: stone at (3,3)
        // C: stone at (3,3)? No — only one dot per cell. Use different inner cells:
        //   B at (3,3), and then C at...wait, ring of A around 1 cell only contains 1 inside cell.
        // Use a 5×5 ring around (3,3) with two interior cells: (3,3) and another.
        // For a single-interior-cell ring (3x3 ring), only one dot fits inside.
        // To test multi-colour capture, use a 4x4 interior region.
        //
        // Plan: A builds a thick ring around a 2×1 interior region.
        //   row=3: A at (1..6,3) except (3,3), (4,3) interior
        //   row=2: A at (2..5,2)
        //   row=4: A at (2..5,4)
        //   col=2,5 verticals at row 2..4
        // Actually, let's just construct the Board directly using withDot — there
        // is no rule that interior cells must have alternating-turn dots.
        //
        // Use the engine's applyMove path properly: A and B and C alternate.
        // We'll set up the position via direct withDot calls and a synthetic state
        // since exhaustive turn-by-turn construction is tedious.
        val emptyState = state
        var b = state.board
        // Outer A ring (5×5 box around (3,3))
        for (col in 1..5) {
            b = b.withDot(Coord(col, 1), Player.A)
            b = b.withDot(Coord(col, 5), Player.A)
        }
        for (row in 2..4) {
            b = b.withDot(Coord(1, row), Player.A)
            b = b.withDot(Coord(5, row), Player.A)
        }
        // Interior: B at (3,3), C at (3,2). Wait (3,2) is on the ring. Use (2,3)?
        // Ring is cols 1..5 rows 1..5; interior is cols 2..4, rows 2..4 (3×3 box).
        // Place foreign dots inside:
        b = b.withDot(Coord(2, 2), Player.B)
        b = b.withDot(Coord(4, 4), Player.C)
        // Empty cells in interior remain empty.

        state = state.copy(board = b)

        // Now place the closing A move. The ring is already complete, so any A
        // placement adjacent to the region triggers detection. Place A at (5,5)
        // which is already A — pick (3,1) instead which is already A too. The
        // ring is fully closed; we need a move that announces the closure to
        // the detector. Place a fresh A at (0,0) (corner, not adjacent) — this
        // won't trigger detection.
        // Better: directly test the detector class.
        val detector = territories.engine.engine.CaptureDetector()
        // Trigger detection from any ring cell.
        val captured = detector.detectCaptures(Coord(5, 5), Player.A, b)
        assertEquals(1, captured.size, "Should capture exactly 1 region")
        val region = captured[0]
        // Both the B dot (2,2) and the C dot (4,4) should be in capturedDots.
        assertTrue(Coord(2, 2) in region.capturedDots, "B dot at (2,2) should be captured")
        assertTrue(Coord(4, 4) in region.capturedDots, "C dot at (4,4) should be captured")
    }

    @Test
    fun surrender_threePlayer_keepsGameAliveBetweenSurvivors() = runBlocking {
        val engine = GameEngine(config(3))
        val state = engine.initialState()
        val afterSurrenderA = engine.surrender(state, Player.A)
        assertFalse(afterSurrenderA.isGameOver, "Game with B and C left must continue")
        assertEquals(setOf(Player.A), afterSurrenderA.eliminated)
        assertEquals(Player.B, afterSurrenderA.currentPlayer)

        // B surrenders too — only C left, game ends
        val afterSurrenderB = engine.surrender(afterSurrenderA, Player.B)
        assertTrue(afterSurrenderB.isGameOver, "Game with only C left must end")
        assertEquals(Player.C, afterSurrenderB.winner)
    }

    @Test
    fun surrender_twoPlayer_immediateGameOver_unchanged() = runBlocking {
        // 2-player back-compat: surrender path is preserved.
        val engine = GameEngine(GameConfig(cols = 9, rows = 9))
        val state = engine.initialState()
        val after = engine.surrender(state, Player.A)
        assertTrue(after.isGameOver)
        assertEquals(Player.B, after.winner)
    }

    @Test
    fun score_forPlayer_returnsPerSeatTotals() {
        val s = Score(playerA = 3, playerB = 5, playerC = 1, playerD = 7)
        assertEquals(3, s.forPlayer(Player.A))
        assertEquals(5, s.forPlayer(Player.B))
        assertEquals(1, s.forPlayer(Player.C))
        assertEquals(7, s.forPlayer(Player.D))
        assertEquals(0, s.forPlayer(Player.NONE))
    }

    @Test
    fun score_leader_returnsTopScorer_orNoneOnTie() {
        assertEquals(Player.D, Score(3, 5, 1, 7).leader)
        assertEquals(Player.B, Score(3, 5).leader)
        assertEquals(Player.NONE, Score(3, 3, 0, 0).leader)
        assertEquals(Player.NONE, Score(0, 0).leader)  // start of game
    }
}
