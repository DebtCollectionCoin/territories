# AI Opponent Design

> **Agent Instructions:** The AI lives in the `:engine` module (no Android dependency).
> It receives a `GameState` and returns a `Coord` to play.
> It must be async-friendly (runs on background thread/coroutine).

---

## Interface

```kotlin
// engine/ai/AiPlayer.kt
interface AiPlayer {
    suspend fun selectMove(state: GameState): Coord
}
```

All difficulty levels implement this interface.

---

## Difficulty Levels

| Level | Algorithm | Depth | Think Time (max) |
|-------|-----------|-------|-----------------|
| Easy | Random Legal Move | — | instant |
| Medium | Greedy Heuristic | 1-ply | ~50ms |
| Hard | Minimax + Alpha-Beta | 3–5 ply | ~2s |
| (Future) Expert | MCTS | N rollouts | configurable |

---

## Level 1 — Easy (Random)

```kotlin
class EasyAiPlayer : AiPlayer {
    override suspend fun selectMove(state: GameState): Coord {
        val legal = LegalMoveChecker().allLegalMoves(state)
        return legal.random()
    }
}
```

Simple, fast, unpredictable. Good for children / first-time players.

---

## Level 2 — Medium (Greedy Heuristic)

Evaluate each legal move and pick the one with the highest immediate score gain.

```kotlin
class MediumAiPlayer(
    private val engine: GameEngine,
    private val scorer: ScoreCalculator
) : AiPlayer {
    override suspend fun selectMove(state: GameState): Coord {
        val legal = LegalMoveChecker().allLegalMoves(state)
        return legal.maxByOrNull { coord ->
            val newState = engine.applyMove(state, coord).getOrThrow()
            heuristic(newState, state.currentPlayer)
        } ?: legal.random()
    }

    private fun heuristic(state: GameState, player: Player): Int {
        val score = scorer.calculate(state)
        return if (player == Player.A) score.playerA - score.playerB
               else score.playerB - score.playerA
    }
}
```

Enhancement: also consider blocking opponent's imminent captures.

---

## Level 3 — Hard (Minimax with Alpha-Beta Pruning)

```kotlin
class HardAiPlayer(
    private val engine: GameEngine,
    private val evaluator: BoardEvaluator,
    private val maxDepth: Int = 4,
    private val maxTimeMs: Long = 2000L
) : AiPlayer {

    override suspend fun selectMove(state: GameState): Coord =
        withContext(Dispatchers.Default) {
            val deadline = System.currentTimeMillis() + maxTimeMs
            val legal = LegalMoveChecker().allLegalMoves(state)

            var bestMove = legal.first()
            var bestScore = Int.MIN_VALUE

            for (coord in legal) {
                if (System.currentTimeMillis() > deadline) break
                val newState = engine.applyMove(state, coord).getOrThrow()
                val score = minimax(newState, maxDepth - 1, Int.MIN_VALUE, Int.MAX_VALUE, false, deadline)
                if (score > bestScore) {
                    bestScore = score
                    bestMove = coord
                }
            }
            bestMove
        }

    private fun minimax(
        state: GameState,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean,
        deadline: Long
    ): Int {
        if (depth == 0 || state.isGameOver || System.currentTimeMillis() > deadline) {
            return evaluator.evaluate(state)
        }

        val legal = LegalMoveChecker().allLegalMoves(state)
        var result = if (isMaximizing) Int.MIN_VALUE else Int.MAX_VALUE
        var a = alpha; var b = beta

        for (coord in legal) {
            val child = engine.applyMove(state, coord).getOrThrow()
            val score = minimax(child, depth - 1, a, b, !isMaximizing, deadline)
            if (isMaximizing) {
                result = maxOf(result, score)
                a = maxOf(a, result)
            } else {
                result = minOf(result, score)
                b = minOf(b, result)
            }
            if (b <= a) break  // Alpha-beta cutoff
        }
        return result
    }
}
```

---

## Board Evaluator (Heuristic Function)

```kotlin
// engine/ai/BoardEvaluator.kt
class BoardEvaluator(private val scorer: ScoreCalculator) {

    fun evaluate(state: GameState): Int {
        val aiPlayer = state.config.playerBType.toPlayer()  // or whichever is AI
        val score = scorer.calculate(state)

        return scoreWeight(score, aiPlayer) +
               territoryPotential(state, aiPlayer) +
               borderControl(state, aiPlayer) +
               captureImminence(state, aiPlayer)
    }

    // Base score differential
    private fun scoreWeight(score: Score, player: Player): Int {
        return if (player == Player.A) (score.playerA - score.playerB) * 100
               else (score.playerB - score.playerA) * 100
    }

    // Reward dots that are close to forming encirclements
    private fun territoryPotential(state: GameState, player: Player): Int {
        // Count "almost closed" regions: loops missing only 1–2 dots
        // Heuristic: for each opponent dot cluster, measure the perimeter
        // covered by AI dots — higher coverage = higher score
        TODO("Implement in TASK AI-04")
    }

    // Reward chains connected to the board border (untouchable)
    private fun borderControl(state: GameState, player: Player): Int {
        val borderDots = state.board.cellsOf(player).count { state.board.isBorder(it) }
        return borderDots * 5
    }

    // Reward if AI is about to capture (opponent group almost enclosed)
    private fun captureImminence(state: GameState, player: Player): Int {
        TODO("Implement in TASK AI-05")
    }
}
```

---

## Implementation Tasks

### TASK AI-01 — Easy AI
- [ ] Implement `EasyAiPlayer`
- [ ] Unit test: always returns a legal move
- [ ] Verify works with a full game playthrough

### TASK AI-02 — Medium AI
- [ ] Implement `MediumAiPlayer` with 1-ply greedy heuristic
- [ ] Benchmark: must return move in <50ms on mid-range device
- [ ] Add: prefer moves that block opponent's imminent captures

### TASK AI-03 — Hard AI Shell
- [ ] Implement `HardAiPlayer` with minimax + alpha-beta
- [ ] Add deadline/timeout: if >2s elapsed, return best move found so far
- [ ] Move ordering: try high-value moves first (improves pruning efficiency)
- [ ] Benchmark: 3-ply on 30×20 board must complete within 2 seconds

### TASK AI-04 — Territory Potential Heuristic
- [ ] Implement `territoryPotential()` in `BoardEvaluator`
- [ ] Detect "almost closed" loops (perimeter coverage metric)
- [ ] Reward AI for being close to a capture

### TASK AI-05 — Capture Imminence Heuristic
- [ ] Implement `captureImminence()` in `BoardEvaluator`
- [ ] Detect opponent groups that AI can capture in ≤2 moves
- [ ] Reward closing those captures soon

### TASK AI-06 — Move Ordering
- [ ] Before minimax, sort moves by immediate heuristic value (best first)
- [ ] This dramatically improves alpha-beta pruning efficiency
- [ ] Benchmark improvement vs. random ordering

### TASK AI-07 — Iterative Deepening (Optional Enhancement)
- [ ] Implement iterative deepening: run depth=1, 2, 3... until deadline
- [ ] Always have a best move to return when time runs out
- [ ] This makes Hard AI feel more consistent

---

## AI in Pass-and-Play

When both players are human (pass-and-play mode), no AI is involved.
When Player B is AI, the AI triggers automatically after Player A's move.
When both players are AI (watch mode), moves fire automatically with a configurable delay.

---

## Performance Notes

- Minimax tree is expensive on large boards: a 60×40 board has ~2400 legal moves → prune aggressively
- For the Hard AI, limit search to a **sub-region**: moves near existing dots, not anywhere on the board
- On Android, AI runs on `Dispatchers.Default` (background thread pool)
- Show a thinking indicator in the UI during AI computation
- Consider caching: if the same state is evaluated twice, return cached result (transposition table)
