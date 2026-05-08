package territories.engine

import territories.engine.engine.LegalMoveChecker
import territories.engine.model.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LegalMoveCheckerTest {

    private val checker = LegalMoveChecker()
    private val config = GameConfig.SMALL
    private val emptyBoard = Board.empty(config.cols, config.rows)

    private fun stateWith(board: Board) = GameState(
        board = board,
        currentPlayer = Player.A,
        phase = GamePhase.IN_PROGRESS,
        territories = emptyList(),
        moveCount = 0,
        lastMove = null,
        winner = Player.NONE,
        config = config,
        score = Score()
    )

    @Test
    fun emptyBoardAllLegal() {
        val state = stateWith(emptyBoard)
        for (coord in emptyBoard.allCoords()) {
            assertTrue(checker.isLegal(coord, state), "Expected $coord to be legal on empty board")
        }
    }

    @Test
    fun occupiedCellIsNotLegal() {
        val coord = Coord(5, 5)
        val board = emptyBoard.withDot(coord, Player.A)
        val state = stateWith(board)
        assertFalse(checker.isLegal(coord, state), "Cell with dot should not be legal")
    }

    @Test
    fun cellInsideTerritoryIsNotLegal() {
        val coord = Coord(5, 5)
        val board = emptyBoard.withTerritory(setOf(coord), Player.A)
        val state = stateWith(board)
        assertFalse(checker.isLegal(coord, state), "Cell inside territory should not be legal")
    }

    @Test
    fun borderCellIsLegal() {
        val state = stateWith(emptyBoard)
        val borderCoord = Coord(0, 0)
        assertTrue(checker.isLegal(borderCoord, state), "Border cell should be legal")
    }

    @Test
    fun outOfBoundsIsNotLegal() {
        val state = stateWith(emptyBoard)
        assertFalse(checker.isLegal(Coord(-1, 0), state))
        assertFalse(checker.isLegal(Coord(0, -1), state))
        assertFalse(checker.isLegal(Coord(config.cols, 0), state))
    }

    @Test
    fun hasLegalMovesOnEmptyBoard() {
        val state = stateWith(emptyBoard)
        assertTrue(checker.hasLegalMoves(state))
    }
}
