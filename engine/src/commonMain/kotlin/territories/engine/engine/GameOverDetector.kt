package territories.engine.engine

import territories.engine.model.GameState
import territories.engine.model.Player

class GameOverDetector(private val legalMoveChecker: LegalMoveChecker = LegalMoveChecker()) {

    fun isGameOver(state: GameState): Boolean =
        !legalMoveChecker.hasLegalMoves(state)

    fun determineWinner(state: GameState): Player {
        val score = state.score
        return when {
            score.playerA > score.playerB -> Player.A
            score.playerB > score.playerA -> Player.B
            else -> Player.NONE
        }
    }
}
