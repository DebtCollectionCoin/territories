package territories.sharedui

import territories.engine.model.GameState
import territories.engine.model.Player

/**
 * Builds a screen-reader description of the current board state.
 *
 * Lives in shared-ui (commonMain) so both Android (TalkBack live region)
 * and Desktop (future accessibility parity) can use the same wording.
 */
fun buildBoardDescription(state: GameState): String {
    val board = state.board
    val a = state.score.playerA
    val b = state.score.playerB
    val turn = when {
        state.isGameOver -> "game over"
        state.currentPlayer == Player.A -> "Blue's turn"
        else -> "Red's turn"
    }
    val last = state.lastMove?.let { coord ->
        val who = when (board.get(coord).dot) {
            Player.A -> "Blue"
            Player.B -> "Red"
            else -> "Last"
        }
        ", $who placed at column ${coord.col + 1} row ${coord.row + 1}"
    } ?: ""
    return "${board.cols} by ${board.rows} game board. Blue $a, Red $b. $turn$last"
}
