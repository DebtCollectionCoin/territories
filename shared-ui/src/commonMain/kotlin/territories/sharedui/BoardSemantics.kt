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
    val turn = when {
        state.isGameOver -> "game over"
        state.currentPlayer == Player.A -> "Blue's turn"
        state.currentPlayer == Player.B -> "Red's turn"
        state.currentPlayer == Player.C -> "Green's turn"
        state.currentPlayer == Player.D -> "Yellow's turn"
        else -> "no active player"
    }
    val labels = mapOf(
        Player.A to "Blue", Player.B to "Red",
        Player.C to "Green", Player.D to "Yellow"
    )
    val scoreParts = state.players.joinToString(", ") { p ->
        "${labels[p] ?: p.name} ${state.score.forPlayer(p)}"
    }
    val last = state.lastMove?.let { coord ->
        val who = labels[board.get(coord).dot] ?: "Last"
        ", $who placed at column ${coord.col + 1} row ${coord.row + 1}"
    } ?: ""
    return "${board.cols} by ${board.rows} game board. $scoreParts. $turn$last"
}
