package territories.engine

import territories.engine.engine.GameEngine
import territories.engine.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameEngineIntegrationTest {

    private val config = GameConfig.SMALL
    private val engine = GameEngine(config)

    @Test
    fun initialStateIsValid() {
        val state = engine.initialState()
        assertEquals(Player.A, state.currentPlayer)
        assertEquals(GamePhase.IN_PROGRESS, state.phase)
        assertEquals(0, state.moveCount)
        assertEquals(0, state.score.playerA)
        assertEquals(0, state.score.playerB)
        assertEquals(Player.NONE, state.winner)
    }

    @Test
    fun legalMoveTogglesPlayer() {
        val state = engine.initialState()
        val result = engine.applyMove(state, Coord(5, 5))
        assertTrue(result.isSuccess)
        val newState = result.getOrThrow()
        assertEquals(Player.B, newState.currentPlayer)
        assertEquals(1, newState.moveCount)
        assertEquals(Coord(5, 5), newState.lastMove)
    }

    @Test
    fun illegalMoveReturnsFailure() {
        val state = engine.initialState()
        // Place a dot, then try to place again at the same spot
        val state2 = engine.applyMove(state, Coord(5, 5)).getOrThrow()
        val result = engine.applyMove(state2, Coord(5, 5))
        assertTrue(result.isFailure)
    }

    @Test
    fun originalStateUnchangedAfterApplyMove() {
        val original = engine.initialState()
        engine.applyMove(original, Coord(5, 5))
        // original should still have Player.A's turn and empty board
        assertEquals(Player.A, original.currentPlayer)
        assertEquals(Player.NONE, original.board.get(Coord(5, 5)).dot)
    }

    @Test
    fun surrenderSetsCorrectWinner() {
        val state = engine.initialState()
        val surrendered = engine.surrender(state, Player.A)
        assertEquals(GamePhase.SURRENDERED, surrendered.phase)
        assertEquals(Player.B, surrendered.winner)
    }

    @Test
    fun captureUpdatesScore() {
        // A builds an 8-cell ring on a 7×7 board; B plays inside then outside.
        // Exact alternating sequence (A goes first):
        //   A:(2,2) B:(3,3) A:(3,2) B:(0,0) A:(4,2) B:(0,1)
        //   A:(2,3) B:(0,2) A:(4,3) B:(6,0) A:(2,4) B:(6,1)
        //   A:(3,4) B:(6,2) A:(4,4) ← ring closes, B at (3,3) captured
        val cfg = GameConfig(cols = 7, rows = 7, scoringVariant = ScoringVariant.TERRITORY_AREA)
        val eng = GameEngine(cfg)
        var state = eng.initialState()

        val sequence = listOf(
            Coord(2, 2), Coord(3, 3),  // A builds ring top-left; B plays inside ring
            Coord(3, 2), Coord(0, 0),  // A builds ring top-mid; B outside
            Coord(4, 2), Coord(0, 1),  // A builds ring top-right; B outside
            Coord(2, 3), Coord(0, 2),  // A builds ring mid-left; B outside
            Coord(4, 3), Coord(6, 0),  // A builds ring mid-right; B outside
            Coord(2, 4), Coord(6, 1),  // A builds ring bot-left; B outside
            Coord(3, 4), Coord(6, 2),  // A builds ring bot-mid; B outside
            Coord(4, 4)                // A closes ring bot-right → captures B at (3,3)
        )

        for (coord in sequence) {
            state = eng.applyMove(state, coord).getOrThrow()
        }

        assertTrue(state.score.playerA > 0, "A should have captured territory after ring closure")
    }
}
