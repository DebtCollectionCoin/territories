package territories.engine.ai

import kotlinx.coroutines.withContext
import territories.engine.engine.GameEngine
import territories.engine.engine.LegalMoveChecker
import territories.engine.engine.ScoreCalculator
import territories.engine.model.*

class HardAiPlayer(
    private val engine: GameEngine,
    private val maxDepth: Int = 4,
    private val maxTimeMs: Long = 2000L
) : AiPlayer {
    private val checker = LegalMoveChecker()
    private val evaluator = BoardEvaluator(ScoreCalculator())

    override suspend fun selectMove(state: GameState): Coord {
        val legal = checker.allLegalMoves(state)
        if (legal.isEmpty()) error("No legal moves available")
        if (legal.size == 1) return legal.first()

        val player = state.currentPlayer
        val deadline = currentTimeMs() + maxTimeMs
        val startMs = currentTimeMs()

        val candidates = legal.filter { coord ->
            coord.neighbors8().any { n ->
                state.board.isOnBoard(n) && state.board.get(n).dot != Player.NONE
            }
        }.ifEmpty { legal }

        AiLog.log("─── Hard AI turn (player=$player) ───")
        AiLog.log("legal=${legal.size} candidates=${candidates.size} depth=$maxDepth budget=${maxTimeMs}ms")

        // Sort moves by 1-ply heuristic for better alpha-beta pruning
        val ordered = sortMoves(candidates, state, player)

        // Show top-5 1-ply previews so the user can sanity-check the heuristic.
        val topPreviews = ordered.take(5).mapNotNull { coord ->
            val s = engine.applyMove(state, coord).getOrNull() ?: return@mapNotNull null
            coord to evaluator.breakdown(s, player)
        }
        for ((coord, b) in topPreviews) {
            AiLog.log("  preview ${coord.col},${coord.row}  ${b.summary()}")
        }

        var bestMove = ordered.first()
        var bestScore = Int.MIN_VALUE
        var evaluated = 0

        for (coord in ordered) {
            if (currentTimeMs() > deadline) break
            val newState = engine.applyMove(state, coord).getOrNull() ?: continue
            val score = minimax(newState, maxDepth - 1, Int.MIN_VALUE, Int.MAX_VALUE, false, player, deadline)
            evaluated++
            if (score > bestScore) {
                bestScore = score
                bestMove = coord
            }
        }
        val elapsed = currentTimeMs() - startMs
        val chosenBreakdown = engine.applyMove(state, bestMove).getOrNull()
            ?.let { evaluator.breakdown(it, player) }
        AiLog.log("CHOSEN ${bestMove.col},${bestMove.row}  minimaxScore=$bestScore  evaluated=$evaluated  elapsed=${elapsed}ms")
        if (chosenBreakdown != null) AiLog.log("  → after-move ${chosenBreakdown.summary()}")
        return bestMove
    }

    private fun minimax(
        state: GameState,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean,
        rootPlayer: Player,
        deadline: Long
    ): Int {
        if (depth == 0 || state.isGameOver || currentTimeMs() > deadline) {
            return evaluator.evaluate(state, rootPlayer)
        }

        val legalAll = checker.allLegalMoves(state)
        if (legalAll.isEmpty()) return evaluator.evaluate(state, rootPlayer)

        // Same near-dots restriction inside the search tree.
        val legal = legalAll.filter { coord ->
            coord.neighbors8().any { n ->
                state.board.isOnBoard(n) && state.board.get(n).dot != Player.NONE
            }
        }.ifEmpty { legalAll }

        var a = alpha
        var b = beta
        var result = if (isMaximizing) Int.MIN_VALUE else Int.MAX_VALUE

        for (coord in legal) {
            if (currentTimeMs() > deadline) break
            val child = engine.applyMove(state, coord).getOrNull() ?: continue
            val score = minimax(child, depth - 1, a, b, !isMaximizing, rootPlayer, deadline)
            if (isMaximizing) {
                result = maxOf(result, score)
                a = maxOf(a, result)
            } else {
                result = minOf(result, score)
                b = minOf(b, result)
            }
            if (b <= a) break // Alpha-beta cutoff
        }
        return result
    }

    private fun sortMoves(moves: List<Coord>, state: GameState, player: Player): List<Coord> {
        return moves.sortedByDescending { coord ->
            val newState = engine.applyMove(state, coord).getOrNull() ?: return@sortedByDescending Int.MIN_VALUE
            evaluator.evaluate(newState, player)
        }
    }
}

expect fun currentTimeMs(): Long
