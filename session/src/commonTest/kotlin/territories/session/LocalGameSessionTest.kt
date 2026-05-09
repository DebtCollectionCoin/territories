package territories.session

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import territories.engine.engine.GameEngine
import territories.engine.model.Coord
import territories.engine.model.GameConfig
import territories.engine.model.Move
import territories.engine.model.Player
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LocalGameSessionTest {

    private val config = GameConfig(cols = 11, rows = 11, undoLimit = 5)
    private val engine = GameEngine(config)
    private val session = LocalGameSession(engine, sessionId = "TEST01")

    @AfterTest
    fun tearDown() {
        session.close()
    }

    @Test
    fun initialStateMatchesEngineInitialState() = runTest {
        val state = session.stateFlow.value
        assertEquals(0, state.moveCount)
        assertEquals(Player.A, state.currentPlayer)
        assertFalse(state.isGameOver)
    }

    @Test
    fun submitMovePlacesDotAndAdvancesTurn() = runTest {
        val coord = Coord(5, 5)
        val result = session.submitMove(Move.PlaceDot(coord, Player.A, moveNumber = 1))
        assertTrue(result.isSuccess, "first move should succeed")

        val state = session.stateFlow.value
        assertEquals(Player.A, state.board.get(coord).dot)
        assertEquals(Player.B, state.currentPlayer)
        assertEquals(1, state.moveCount)
    }

    @Test
    fun stateFlowEmitsAfterEachMove() = runTest {
        val states = mutableListOf<Int>()
        states += session.stateFlow.value.moveCount

        session.submitMove(Move.PlaceDot(Coord(1, 1), Player.A, 1))
        states += session.stateFlow.value.moveCount

        session.submitMove(Move.PlaceDot(Coord(2, 2), Player.B, 2))
        states += session.stateFlow.value.moveCount

        assertEquals(listOf(0, 1, 2), states)
    }

    @Test
    fun illegalMoveReturnsFailureAndDoesNotMutateState() = runTest {
        // Place A at (5,5).
        session.submitMove(Move.PlaceDot(Coord(5, 5), Player.A, 1))
        val before = session.stateFlow.value

        // Same coord again — illegal (occupied).
        val bad = session.submitMove(Move.PlaceDot(Coord(5, 5), Player.B, 2))
        assertTrue(bad.isFailure, "second move on same cell must fail")

        val after = session.stateFlow.value
        assertEquals(before.moveCount, after.moveCount)
        assertEquals(before.currentPlayer, after.currentPlayer)
    }

    @Test
    fun undoRestoresPreviousState() = runTest {
        session.submitMove(Move.PlaceDot(Coord(3, 3), Player.A, 1))
        session.submitMove(Move.PlaceDot(Coord(4, 4), Player.B, 2))
        assertEquals(2, session.stateFlow.value.moveCount)

        val undone = session.requestUndo()
        assertTrue(undone, "undo should report success")
        assertEquals(1, session.stateFlow.value.moveCount)
        // After undo, currentPlayer should be B again (the one who just moved at (4,4)).
        assertEquals(Player.B, session.stateFlow.value.currentPlayer)
    }

    @Test
    fun undoOnEmptyHistoryReturnsFalse() = runTest {
        assertFalse(session.canUndo(), "no history yet → cannot undo")
        val undone = session.requestUndo()
        assertFalse(undone)
    }

    @Test
    fun surrenderEndsTheGame() = runTest {
        val finalState = session.surrender(Player.A)
        assertTrue(finalState.isGameOver)
        assertEquals(Player.B, finalState.winner)
    }

    @Test
    fun surrenderViaSubmitMoveAlsoEndsGame() = runTest {
        val result = session.submitMove(Move.Surrender(Player.A, moveNumber = 1))
        assertTrue(result.isSuccess)
        assertTrue(session.stateFlow.value.isGameOver)
    }

    @Test
    fun sessionIdIsExposed() {
        assertEquals("TEST01", session.sessionId)
    }

    @Test
    fun observeOpponentMovesIsEmptyForLocalSession() = runTest {
        // Local session has no remote opponent — should never emit.
        // We just check the flow is consumable; emptyFlow() completes immediately.
        var count = 0
        session.observeOpponentMoves().collect { count++ }
        assertEquals(0, count)
    }

    @Test
    fun firstStateEmittedHasZeroMoves() = runTest {
        val first = session.stateFlow.first()
        assertEquals(0, first.moveCount)
        assertEquals(Player.A, first.currentPlayer)
    }
}
