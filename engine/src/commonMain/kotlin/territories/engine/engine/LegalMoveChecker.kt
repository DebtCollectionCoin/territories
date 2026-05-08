package territories.engine.engine

import territories.engine.model.Coord
import territories.engine.model.GameState

class LegalMoveChecker {

    fun isLegal(coord: Coord, state: GameState): Boolean {
        if (!state.board.isOnBoard(coord)) return false
        val cell = state.board.get(coord)
        return cell.dot == territories.engine.model.Player.NONE &&
               cell.territory == territories.engine.model.Player.NONE
    }

    fun allLegalMoves(state: GameState): List<Coord> =
        state.board.allCoords().filter { isLegal(it, state) }.toList()

    fun hasLegalMoves(state: GameState): Boolean =
        state.board.allCoords().any { isLegal(it, state) }
}
