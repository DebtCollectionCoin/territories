package territories.engine.ai

import kotlinx.coroutines.runBlocking
import territories.engine.ai.AiLog
import territories.engine.ai.BoardEvaluator
import territories.engine.ai.HardAiPlayer
import territories.engine.ai.MediumAiPlayer
import territories.engine.engine.GameEngine
import territories.engine.engine.ScoreCalculator
import territories.engine.model.*

/**
 * Standalone JVM simulation — not a normal test, just run directly to
 * see move-by-move AI decisions.
 *
 * Run with:
 *   ./gradlew :engine:runSimulation
 *
 * Or from IntelliJ: right-click → Run 'main'
 */
fun main() {
    AiLog.enabled = false // disable per-move logs; we print our own richer output

    val cols = 15; val rows = 11
    val config = GameConfig(
        cols = cols, rows = rows,
        playerAType = PlayerType.AI_MEDIUM,
        playerBType = PlayerType.AI_MEDIUM
    )
    val engine = GameEngine(config)
    val evaluator = BoardEvaluator(ScoreCalculator())

    var state = engine.initialState()

    // First human move to seed the board so AI has something to react to
    state = engine.applyMove(state, Coord(cols / 2, rows / 2)).getOrThrow()
    println("Seeded: Human placed at (${cols/2}, ${rows/2})")

    val aiA = MediumAiPlayer(engine)
    val aiB = MediumAiPlayer(engine)

    repeat(40) { turn ->
        if (state.isGameOver) return@repeat
        val player = state.currentPlayer
        val ai = if (player == Player.A) aiA else aiB

        val move = runBlocking { ai.selectMove(state) }
        val newState = engine.applyMove(state, move).getOrThrow()

        // Detailed breakdown
        val b = evaluator.breakdown(newState, player)
        val opponent = if (player == Player.A) Player.B else Player.A
        val bOpp = evaluator.breakdown(newState, opponent)

        println(
            "Turn ${turn + 1} | $player → (${move.col},${move.row}) | " +
            "score ${newState.score.playerA}A-${newState.score.playerB}B | " +
            "ME total=${b.total} oTrap=${b.oppTrapped} oDepth=${b.oppDepthSum} " +
            "sTrap=${b.ownTrapped} sDepth=${b.ownDepthSum} fr=${b.frontier} | " +
            "OPP total=${bOpp.total} oTrap=${bOpp.oppTrapped} oDepth=${bOpp.oppDepthSum}"
        )

        printBoard(newState, cols, rows)
        state = newState
    }

    println("\n=== FINAL SCORE: A=${state.score.playerA} B=${state.score.playerB} ===")
}

private fun printBoard(state: GameState, cols: Int, rows: Int) {
    val sb = StringBuilder()
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val cell = state.board.get(Coord(col, row))
            sb.append(when {
                cell.dot == Player.A && cell.territory == Player.A -> 'A'
                cell.dot == Player.B && cell.territory == Player.B -> 'B'
                cell.dot == Player.A -> 'a'
                cell.dot == Player.B -> 'b'
                cell.territory == Player.A -> '░'
                cell.territory == Player.B -> '▓'
                else -> '.'
            })
        }
        sb.append('\n')
    }
    print(sb)
}
