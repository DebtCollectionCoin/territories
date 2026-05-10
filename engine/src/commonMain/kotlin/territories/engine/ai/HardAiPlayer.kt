package territories.engine.ai

import territories.engine.engine.GameEngine
import territories.engine.engine.LegalMoveChecker
import territories.engine.engine.ScoreCalculator
import territories.engine.model.*

class HardAiPlayer(
    private val engine: GameEngine,
    private val maxDepth: Int = 4,
    private val maxTimeMs: Long = 2000L,
    weights: BoardEvaluator.Weights = BoardEvaluator.Weights.DEFAULT
) : AiPlayer {
    private val checker = LegalMoveChecker()
    private val evaluator = BoardEvaluator(ScoreCalculator(), weights)

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
        AiLog.log("legal=${legal.size} candidates=${candidates.size} maxDepth=$maxDepth budget=${maxTimeMs}ms")

        // Sort moves by 1-ply heuristic for better alpha-beta pruning
        var ordered = sortMoves(candidates, state, player)

        // Show top-5 1-ply previews so the user can sanity-check the heuristic.
        val topPreviews = ordered.take(5).mapNotNull { coord ->
            val s = engine.applyMove(state, coord).getOrNull() ?: return@mapNotNull null
            coord to evaluator.breakdown(s, player)
        }
        for ((coord, b) in topPreviews) {
            AiLog.log("  preview ${coord.col},${coord.row}  ${b.summary()}")
        }

        // Iterative deepening: search depths 1..maxDepth in turn. Each completed
        // iteration overwrites bestMove and re-orders moves for the next pass
        // (PV-first heuristic). If the deadline hits mid-iteration we keep the
        // last fully-completed depth's bestMove — guaranteed to be at least as
        // good as a 1-ply choice and often much better.
        var bestMove = ordered.first()
        var bestScore = Int.MIN_VALUE
        var lastCompletedDepth = 0
        var totalEvaluated = 0

        for (depth in 1..maxDepth) {
            if (currentTimeMs() > deadline) break

            var iterBest = ordered.first()
            var iterScore = Int.MIN_VALUE
            var iterEvaluated = 0
            var completed = true

            for (coord in ordered) {
                if (currentTimeMs() > deadline) {
                    completed = false
                    break
                }
                val newState = engine.applyMove(state, coord).getOrNull() ?: continue
                val score = minimax(
                    newState, depth - 1,
                    Int.MIN_VALUE, Int.MAX_VALUE,
                    rootPlayer = player,
                    deadline = deadline
                )
                iterEvaluated++
                if (score > iterScore) {
                    iterScore = score
                    iterBest = coord
                }
            }

            totalEvaluated += iterEvaluated
            if (completed) {
                bestMove = iterBest
                bestScore = iterScore
                lastCompletedDepth = depth
                AiLog.log("  depth=$depth ✓ best=(${iterBest.col},${iterBest.row}) score=$iterScore eval=$iterEvaluated")
                // Re-order: put the best move first so alpha-beta prunes deeper next pass.
                ordered = listOf(iterBest) + ordered.filter { it != iterBest }
            } else {
                AiLog.log("  depth=$depth ⏱ aborted at eval=$iterEvaluated, keeping depth=$lastCompletedDepth result")
                break
            }
        }

        val elapsed = currentTimeMs() - startMs
        val chosenBreakdown = engine.applyMove(state, bestMove).getOrNull()
            ?.let { evaluator.breakdown(it, player) }
        AiLog.log(
            "CHOSEN ${bestMove.col},${bestMove.row}  score=$bestScore  " +
            "depth=$lastCompletedDepth  evaluated=$totalEvaluated  elapsed=${elapsed}ms"
        )
        if (chosenBreakdown != null) AiLog.log("  → after-move ${chosenBreakdown.summary()}")
        return bestMove
    }

    private fun minimax(
        state: GameState,
        depth: Int,
        alpha: Int,
        beta: Int,
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

        // Paranoid: root maximises on its own turn, every other seat
        // minimises on theirs. Generalises the 2-player isMaximizing flag
        // to N players via state.currentPlayer.
        val isMaximizing = state.currentPlayer == rootPlayer
        var a = alpha
        var b = beta
        var result = if (isMaximizing) Int.MIN_VALUE else Int.MAX_VALUE

        for (coord in legal) {
            if (currentTimeMs() > deadline) break
            val child = engine.applyMove(state, coord).getOrNull() ?: continue
            val score = minimax(child, depth - 1, a, b, rootPlayer, deadline)
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
