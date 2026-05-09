package territories.engine.engine

import territories.engine.model.GameState
import territories.engine.model.Player
import territories.engine.model.Score
import territories.engine.model.ScoringVariant

class ScoreCalculator {

    fun calculate(state: GameState): Score {
        return when (state.config.scoringVariant) {
            ScoringVariant.TERRITORY_AREA -> calculateArea(state)
            ScoringVariant.CAPTURED_DOTS -> calculateCapturedDots(state)
        }
    }

    private fun calculateArea(state: GameState): Score {
        val totals = mutableMapOf(
            Player.A to 0, Player.B to 0, Player.C to 0, Player.D to 0
        )
        for (coord in state.board.allCoords()) {
            val owner = state.board.get(coord).territory
            if (owner != Player.NONE) {
                totals[owner] = (totals[owner] ?: 0) + 1
            }
        }
        return Score(
            playerA = totals[Player.A] ?: 0,
            playerB = totals[Player.B] ?: 0,
            playerC = totals[Player.C] ?: 0,
            playerD = totals[Player.D] ?: 0
        )
    }

    private fun calculateCapturedDots(state: GameState): Score {
        val totals = mutableMapOf(
            Player.A to 0, Player.B to 0, Player.C to 0, Player.D to 0
        )
        for (coord in state.board.allCoords()) {
            val cell = state.board.get(coord)
            // A captured dot is a stone of one colour sitting in another
            // colour's territory. Credit goes to the territory owner.
            if (cell.territory != Player.NONE &&
                cell.dot != Player.NONE &&
                cell.dot != cell.territory
            ) {
                totals[cell.territory] = (totals[cell.territory] ?: 0) + 1
            }
        }
        return Score(
            playerA = totals[Player.A] ?: 0,
            playerB = totals[Player.B] ?: 0,
            playerC = totals[Player.C] ?: 0,
            playerD = totals[Player.D] ?: 0
        )
    }
}
