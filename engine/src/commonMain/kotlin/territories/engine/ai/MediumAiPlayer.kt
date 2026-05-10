package territories.engine.ai

import territories.engine.engine.GameEngine
import territories.engine.engine.LegalMoveChecker
import territories.engine.engine.ScoreCalculator
import territories.engine.model.Coord
import territories.engine.model.GameState
import territories.engine.model.Player

class MediumAiPlayer(
    private val engine: GameEngine,
    weights: BoardEvaluator.Weights = BoardEvaluator.Weights.DEFAULT
) : AiPlayer {
    private val checker = LegalMoveChecker()
    private val evaluator = BoardEvaluator(ScoreCalculator(), weights)

    override suspend fun selectMove(state: GameState): Coord {
        val legal = checker.allLegalMoves(state)
        if (legal.isEmpty()) error("No legal moves available")
        if (legal.size == 1) return legal.first()

        val player = state.currentPlayer
        var bestCoord = legal.first()
        var bestScore = Int.MIN_VALUE
        var bestBreakdown: BoardEvaluator.Breakdown? = null

        val candidates = legal.filter { coord ->
            coord.neighbors8().any { n ->
                state.board.isOnBoard(n) && state.board.get(n).dot != Player.NONE
            }
        }.ifEmpty { legal }

        AiLog.log("─── Medium AI turn (player=$player) ───")
        AiLog.log("legal=${legal.size} candidates=${candidates.size}")

        val previews = mutableListOf<Pair<Coord, BoardEvaluator.Breakdown>>()
        for (coord in candidates) {
            val newState = engine.applyMove(state, coord).getOrNull() ?: continue
            val b = evaluator.breakdown(newState, player)
            previews += coord to b
            if (b.total > bestScore) {
                bestScore = b.total
                bestCoord = coord
                bestBreakdown = b
            }
        }

        previews.sortByDescending { it.second.total }
        for ((coord, b) in previews.take(5)) {
            AiLog.log("  preview ${coord.col},${coord.row}  ${b.summary()}")
        }
        AiLog.log("CHOSEN ${bestCoord.col},${bestCoord.row}  score=$bestScore")
        if (bestBreakdown != null) AiLog.log("  → after-move ${bestBreakdown.summary()}")
        return bestCoord
    }
}
