package territories.engine.ai

import territories.engine.engine.LegalMoveChecker
import territories.engine.model.Coord
import territories.engine.model.GameState
import territories.engine.model.Player

class EasyAiPlayer : AiPlayer {
    private val checker = LegalMoveChecker()

    override suspend fun selectMove(state: GameState): Coord {
        val legal = checker.allLegalMoves(state)
        // 75% chance: prefer positions adjacent to any existing dot
        // (builds loose clusters, making captures more likely)
        val adjacent = legal.filter { coord ->
            coord.neighbors8().any { n ->
                state.board.isOnBoard(n) && state.board.get(n).dot != Player.NONE
            }
        }
        return if (adjacent.isNotEmpty() && (0..3).random() > 0) adjacent.random()
               else legal.random()
    }
}
