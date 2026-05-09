package territories.engine.engine

import territories.engine.model.GameState
import territories.engine.model.Player

class GameOverDetector(private val legalMoveChecker: LegalMoveChecker = LegalMoveChecker()) {

    fun isGameOver(state: GameState): Boolean =
        !legalMoveChecker.hasLegalMoves(state)

    /**
     * Returns the seat with the highest score, or [Player.NONE] on a tie.
     * For 2-player games this is equivalent to the previous A-vs-B comparison.
     */
    fun determineWinner(state: GameState): Player = state.score.leader
}
