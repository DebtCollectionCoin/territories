package territories.engine.ai

import territories.engine.engine.ScoreCalculator
import territories.engine.model.*

/**
 * Evaluator that understands the goal of Territories/Židi: build a wall
 * between the opponent's dots and the board border so the opponent gets
 * captured.
 *
 * Core signal: BFS distance-from-border for every dot, computed twice:
 *   • Avoiding our dots → "how buried is the opponent in our region"
 *   • Avoiding opponent dots → "how buried are we in opponent's region"
 *
 * The deeper the opponent is buried, the closer to capture. This produces
 * a smooth, monotonic gradient as we build any wall, and naturally
 * discourages interior clustering (an interior dot doesn't change anyone's
 * BFS distance, so it adds zero pressure).
 */
class BoardEvaluator(private val scorer: ScoreCalculator) {

    data class Breakdown(
        val realScore: Int,
        val oppTrapped: Int,
        val ownTrapped: Int,
        val oppDepthSum: Int,
        val ownDepthSum: Int,
        val frontier: Int,
        val proximity: Int,
        val oppEscapeArea: Int,
        val ownEscapeArea: Int,
        val total: Int
    ) {
        fun summary(): String =
            "total=$total | scoreΔ=$realScore (×1000) " +
            "oTrap=$oppTrapped (×400) sTrap=$ownTrapped (×-380) " +
            "oDepth=$oppDepthSum (×6) sDepth=$ownDepthSum (×-6) " +
            "frontier=$frontier (×8) prox=$proximity (×8) " +
            "oEscape=$oppEscapeArea (×-4) sEscape=$ownEscapeArea (×1)"
    }

    fun evaluate(state: GameState, player: Player): Int =
        breakdown(state, player).total

    fun breakdown(state: GameState, player: Player): Breakdown {
        val opponent = if (player == Player.A) Player.B else Player.A
        val board = state.board
        val score = scorer.calculate(state)

        val realScore = if (player == Player.A) score.playerA - score.playerB
                        else score.playerB - score.playerA

        // BFS distance from border, treating the given player's dots/territory
        // as walls (impassable).
        val distOpp = bfsDistanceFromBorder(board, blocker = player)
        val distOwn = bfsDistanceFromBorder(board, blocker = opponent)

        // A dot only counts as "live" for the player whose colour it bears
        // AND whose territory it is NOT trapped inside. Captured dots are
        // absorbed and don't contribute to either side's calculations.
        val oppDots = board.cellsOf(opponent).filter {
            board.get(it).territory != player
        }
        val ownDots = board.cellsOf(player).filter {
            board.get(it).territory != opponent
        }

        // Trapped = unreachable from border (distance == -1).
        val oppTrapped = oppDots.count { (distOpp[it.col][it.row]) < 0 }
        val ownTrapped = ownDots.count { (distOwn[it.col][it.row]) < 0 }

        // Sum of BFS distances for opponent / own dots that are still alive.
        // Deeper = more buried = better for us when summing opponent depth.
        var oppDepthSum = 0
        for (c in oppDots) {
            val d = distOpp[c.col][c.row]
            if (d >= 0) oppDepthSum += d
        }
        var ownDepthSum = 0
        for (c in ownDots) {
            val d = distOwn[c.col][c.row]
            if (d >= 0) ownDepthSum += d
        }

        // Proximity: smooth, monotonic gradient that pulls placements toward
        // opponent dots. For every own dot, contribute max(0, R - cheb) where
        // cheb = Chebyshev distance to the nearest opponent dot. This is the
        // key signal that prevents the AI from drifting toward empty edges
        // when no wall is yet in contact with anything.
        //   • adjacent to opponent (cheb=1) → +(R-1)
        //   • cheb ≥ R → 0
        // R=8 means contributions taper out across roughly half the board.
        val proximityRadius = 8
        var proximity = 0
        if (oppDots.isNotEmpty()) {
            for (c in ownDots) {
                val nearest = oppDots.minOf { o ->
                    maxOf(kotlin.math.abs(o.col - c.col), kotlin.math.abs(o.row - c.row))
                }
                proximity += maxOf(0, proximityRadius - nearest)
            }
        }

        // Frontier: own dots that (a) sit adjacent to an empty cell in the
        // opponent's escape region AND (b) are within Chebyshev 4 of an
        // opponent dot. Counts dots that actively press on the opponent's
        // living space.
        val oppEscape = HashSet<Coord>()
        for (col in 0 until board.cols) for (row in 0 until board.rows) {
            if (distOpp[col][row] >= 0) oppEscape.add(Coord(col, row))
        }
        var frontier = 0
        if (oppDots.isNotEmpty()) {
            for (c in ownDots) {
                val nearest = oppDots.minOf { o ->
                    maxOf(kotlin.math.abs(o.col - c.col), kotlin.math.abs(o.row - c.row))
                }
                if (nearest > 4) continue
                val touchesLiveOpp = c.neighbors8().any { n ->
                    board.isOnBoard(n) &&
                    board.get(n).dot == Player.NONE &&
                    n in oppEscape
                }
                if (touchesLiveOpp) frontier++
            }
        }

        // Escape areas: number of border-reachable cells from each player's
        // perspective. Shrinking the *opponent's* escape area is the direct
        // signal of enclosure — a wall that orphans a chunk from the border
        // produces a large drop here. Without this term the heuristic only
        // rewards burial depth, which can be gamed by walking the border
        // forever without ever closing a region.
        var oppEscapeArea = 0
        var ownEscapeArea = 0
        for (col in 0 until board.cols) for (row in 0 until board.rows) {
            if (distOpp[col][row] >= 0) oppEscapeArea++
            if (distOwn[col][row] >= 0) ownEscapeArea++
        }

        val total = realScore     * 1000 +
                    oppTrapped    * 400 -
                    ownTrapped    * 380 +
                    oppDepthSum   * 6 -
                    ownDepthSum   * 6 +
                    frontier      * 8 +
                    proximity     * 8 -
                    oppEscapeArea * 4 +
                    ownEscapeArea * 1

        return Breakdown(
            realScore = realScore,
            oppTrapped = oppTrapped,
            ownTrapped = ownTrapped,
            oppDepthSum = oppDepthSum,
            ownDepthSum = ownDepthSum,
            frontier = frontier,
            proximity = proximity,
            oppEscapeArea = oppEscapeArea,
            ownEscapeArea = ownEscapeArea,
            total = total
        )
    }

    /**
     * Multi-source BFS from every passable border cell. Returns a 2D array
     * where cells reachable from the border have their (4-connected)
     * geodesic distance, and unreachable cells have -1. A cell is
     * impassable iff its dot OR its territory belongs to [blocker].
     */
    private fun bfsDistanceFromBorder(board: Board, blocker: Player): Array<IntArray> {
        val dist = Array(board.cols) { IntArray(board.rows) { -1 } }
        val queue = ArrayDeque<Coord>()

        fun isImpassable(c: Coord): Boolean {
            val cell = board.get(c)
            // Match CaptureDetector: once a cell is inside someone's territory,
            // only the territory owner's material lives there. A captured
            // opponent dot does not block its original owner anymore.
            return if (cell.territory != Player.NONE) cell.territory == blocker
            else cell.dot == blocker
        }

        // Seed with passable border cells at distance 0.
        for (col in 0 until board.cols) {
            seed(Coord(col, 0), board, dist, queue, ::isImpassable)
            seed(Coord(col, board.rows - 1), board, dist, queue, ::isImpassable)
        }
        for (row in 0 until board.rows) {
            seed(Coord(0, row), board, dist, queue, ::isImpassable)
            seed(Coord(board.cols - 1, row), board, dist, queue, ::isImpassable)
        }

        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            val nd = dist[cur.col][cur.row] + 1
            for (n in cur.neighbors4()) {
                if (!board.isOnBoard(n)) continue
                if (dist[n.col][n.row] != -1) continue
                if (isImpassable(n)) continue
                dist[n.col][n.row] = nd
                queue.addLast(n)
            }
        }
        return dist
    }

    private fun seed(
        c: Coord,
        board: Board,
        dist: Array<IntArray>,
        queue: ArrayDeque<Coord>,
        impassable: (Coord) -> Boolean
    ) {
        if (!board.isOnBoard(c)) return
        if (dist[c.col][c.row] != -1) return
        if (impassable(c)) return
        dist[c.col][c.row] = 0
        queue.addLast(c)
    }
}
