package territories.engine.ai

import kotlinx.coroutines.runBlocking
import territories.engine.engine.GameEngine
import territories.engine.engine.ScoreCalculator
import territories.engine.model.*
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * AI vs AI benchmark harness.
 *
 * Runs N games per match-up between configurable opponents and reports
 * win-rate, avg captures, avg score, avg move count, and timing. The goal
 * is to make tuning the [BoardEvaluator] weights mechanical: change the
 * defaults, run the bench, see if A-versus-baseline win rate goes up.
 *
 * Run with:
 *   ./gradlew :engine:runBenchmark
 *   ./gradlew :engine:runBenchmark -Pbench.args="--games 50 --moves 80 --seed 42"
 *
 * Args:
 *   --games N       number of games per match-up (default 20)
 *   --moves N       max moves per game (default 80)
 *   --seed N        RNG seed for opening moves (default 1)
 *   --cols N        board columns (default 15)
 *   --rows N        board rows (default 11)
 *   --depth N       Hard AI search depth (default 3)
 *   --time-ms N     Hard AI per-move budget (default 600)
 *
 * Match-ups are hard-coded in [main] — edit that list to add new
 * weight sweeps.
 */

private data class BenchArgs(
    val games: Int = 20,
    val maxMoves: Int = 80,
    val seed: Long = 1L,
    val cols: Int = 15,
    val rows: Int = 11,
    val hardDepth: Int = 3,
    val hardTimeMs: Long = 600L
)

private fun parseArgs(argv: Array<String>): BenchArgs {
    var a = BenchArgs()
    var i = 0
    while (i < argv.size) {
        when (argv[i]) {
            "--games"   -> { a = a.copy(games = argv[++i].toInt()) }
            "--moves"   -> { a = a.copy(maxMoves = argv[++i].toInt()) }
            "--seed"    -> { a = a.copy(seed = argv[++i].toLong()) }
            "--cols"    -> { a = a.copy(cols = argv[++i].toInt()) }
            "--rows"    -> { a = a.copy(rows = argv[++i].toInt()) }
            "--depth"   -> { a = a.copy(hardDepth = argv[++i].toInt()) }
            "--time-ms" -> { a = a.copy(hardTimeMs = argv[++i].toLong()) }
            else -> error("unknown arg: ${argv[i]}")
        }
        i++
    }
    return a
}

private data class MatchResult(
    val name: String,
    val games: Int,
    val winsA: Int,
    val winsB: Int,
    val draws: Int,
    val avgCapsA: Double,
    val avgCapsB: Double,
    val avgScoreA: Double,
    val avgScoreB: Double,
    val avgMoves: Double,
    val totalMs: Long
) {
    fun pretty(): String = buildString {
        append("%-40s ".format(name))
        append("games=%-3d ".format(games))
        append("A=%2d B=%2d D=%2d  ".format(winsA, winsB, draws))
        append("caps=%4.1f/%4.1f  ".format(avgCapsA, avgCapsB))
        append("score=%5.1f/%5.1f  ".format(avgScoreA, avgScoreB))
        append("mv=%4.1f  ".format(avgMoves))
        append("%5dms".format(totalMs))
    }
}

private fun playOne(
    engine: GameEngine,
    cols: Int, rows: Int,
    aiAFactory: (GameEngine) -> AiPlayer,
    aiBFactory: (GameEngine) -> AiPlayer,
    maxMoves: Int,
    rng: Random,
    scorer: ScoreCalculator
): Triple<GameState, Int, Int> {
    val aiA = aiAFactory(engine)
    val aiB = aiBFactory(engine)
    var state = engine.initialState()
    // Random opening seed dot (legal & on-board) to break symmetry.
    val seedCol = 1 + rng.nextInt(cols - 2)
    val seedRow = 1 + rng.nextInt(rows - 2)
    state = engine.applyMove(state, Coord(seedCol, seedRow)).getOrThrow()

    var moves = 0
    while (moves < maxMoves && !state.isGameOver) {
        val ai = if (state.currentPlayer == Player.A) aiA else aiB
        val mv = runBlocking { ai.selectMove(state) }
        val r = engine.applyMove(state, mv)
        if (r.isFailure) break
        state = r.getOrThrow()
        moves++
    }
    val s = scorer.calculate(state)
    return Triple(state, s.playerA, s.playerB)
}

private fun runMatch(
    name: String,
    args: BenchArgs,
    aiAFactory: (GameEngine) -> AiPlayer,
    aiBFactory: (GameEngine) -> AiPlayer
): MatchResult {
    AiLog.enabled = false
    val cfg = GameConfig(
        cols = args.cols, rows = args.rows,
        playerAType = PlayerType.AI_HARD,
        playerBType = PlayerType.AI_HARD
    )
    val engine = GameEngine(cfg)
    val scorer = ScoreCalculator()
    var winsA = 0; var winsB = 0; var draws = 0
    var capsA = 0; var capsB = 0
    var scoreA = 0; var scoreB = 0
    var moves = 0
    val ms = measureTimeMillis {
        val rng = Random(args.seed)
        repeat(args.games) {
            val (state, sA, sB) = playOne(
                engine, args.cols, args.rows,
                aiAFactory, aiBFactory, args.maxMoves, rng, scorer
            )
            scoreA += sA; scoreB += sB
            // capture count = sum of opponent dots inside our territories
            val board = state.board
            var cA = 0; var cB = 0
            for (c in board.cellsOf(Player.B)) if (board.get(c).territory == Player.A) cA++
            for (c in board.cellsOf(Player.A)) if (board.get(c).territory == Player.B) cB++
            capsA += cA; capsB += cB
            moves += state.moveCount
            when {
                sA > sB -> winsA++
                sB > sA -> winsB++
                else    -> draws++
            }
        }
    }
    val n = args.games.toDouble()
    return MatchResult(
        name = name,
        games = args.games,
        winsA = winsA, winsB = winsB, draws = draws,
        avgCapsA = capsA / n, avgCapsB = capsB / n,
        avgScoreA = scoreA / n, avgScoreB = scoreB / n,
        avgMoves = moves / n,
        totalMs = ms
    )
}

private fun mediumWith(weights: BoardEvaluator.Weights): (GameEngine) -> AiPlayer =
    { engine -> MediumAiPlayer(engine, weights) }

private fun hardWith(
    weights: BoardEvaluator.Weights,
    depth: Int, timeMs: Long
): (GameEngine) -> AiPlayer =
    { engine -> HardAiPlayer(engine, depth, timeMs, weights) }

fun main(argv: Array<String>) {
    val args = parseArgs(argv)
    println("Bench: games=${args.games} moves=${args.maxMoves} board=${args.cols}x${args.rows} " +
            "depth=${args.hardDepth} timeMs=${args.hardTimeMs} seed=${args.seed}")

    val baseline = BoardEvaluator.Weights()  // current defaults
    // Sanity check: head-to-head vs the original "old" heuristic (high
    // proximity & frontier). If that still loses, the new defaults are
    // a real improvement.
    val oldHeuristic = baseline.copy(frontier = 8, proximity = 8, componentPressure = 0)
    val variants = listOf(
        "old_heuristic" to oldHeuristic,
        "fr5"           to baseline.copy(frontier = 5),
        "cP10"          to baseline.copy(componentPressure = 10),
        "cP12_K20"      to baseline.copy(componentPressure = 12, pressureK = 20),
        "depth10_cP8"   to baseline.copy(oppDepthSum = 10, ownDepthSum = 10, componentPressure = 8),
        "esc8_cP10"     to baseline.copy(oppEscapeArea = 8, componentPressure = 10),
        "esc0_cP12"     to baseline.copy(oppEscapeArea = 0, componentPressure = 12),
        "no_pressure"   to baseline.copy(componentPressure = 0, pressureGain = 0)
    )

    val results = mutableListOf<MatchResult>()

    // Each variant plays as A against the baseline as B (Medium-vs-Medium
    // for speed), then as B against baseline as A. The two halves let us
    // see whether the variant is symmetrically better.
    for ((name, w) in variants) {
        if (name == "baseline") continue
        val resA = runMatch("$name(A) vs baseline(B)", args,
            aiAFactory = mediumWith(w),
            aiBFactory = mediumWith(baseline))
        val resB = runMatch("baseline(A) vs $name(B)", args,
            aiAFactory = mediumWith(baseline),
            aiBFactory = mediumWith(w))
        results += resA
        results += resB
    }

    // Self-play baseline for sanity.
    results += runMatch("baseline(A) vs baseline(B)", args,
        aiAFactory = mediumWith(baseline),
        aiBFactory = mediumWith(baseline))

    println()
    println("─── Results ───")
    for (r in results) println(r.pretty())

    // Aggregate per-variant: cumulative captures-FOR minus captures-AGAINST
    // across both halves.
    println()
    println("─── Variant net captures vs baseline ───")
    for ((name, _) in variants) {
        if (name == "baseline") continue
        val asA = results.first { it.name.startsWith("$name(A)") }
        val asB = results.first { it.name.endsWith("$name(B)") }
        val capsFor = asA.avgCapsA + asB.avgCapsB
        val capsAgainst = asA.avgCapsB + asB.avgCapsA
        val winsFor = asA.winsA + asB.winsB
        val winsAgainst = asA.winsB + asB.winsA
        println("%-30s  capsFor=%4.1f capsAgainst=%4.1f  net=%+5.1f  wins=%d/%d".format(
            name, capsFor, capsAgainst, capsFor - capsAgainst, winsFor, winsAgainst
        ))
    }
}
