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
        var a = 0; var b = 0
        for (coord in state.board.allCoords()) {
            when (state.board.get(coord).territory) {
                Player.A -> a++
                Player.B -> b++
                Player.NONE -> Unit
            }
        }
        return Score(a, b)
    }

    private fun calculateCapturedDots(state: GameState): Score {
        var a = 0; var b = 0
        for (coord in state.board.allCoords()) {
            val cell = state.board.get(coord)
            if (cell.territory == Player.A && cell.dot == Player.B) a++
            if (cell.territory == Player.B && cell.dot == Player.A) b++
        }
        return Score(a, b)
    }
}
