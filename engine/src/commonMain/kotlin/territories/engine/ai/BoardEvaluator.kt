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
class BoardEvaluator(
    private val scorer: ScoreCalculator,
    private val weights: Weights = Weights.DEFAULT
) {

    /**
     * Tunable weight set. All heuristic terms multiply through here so
     * benchmarks can sweep weights without touching the evaluator body.
     */
    data class Weights(
        val realScore: Int        = 1000,
        val oppTrapped: Int       = 400,
        val ownTrapped: Int       = 380,
        val oppDepthSum: Int      = 6,
        val ownDepthSum: Int      = 6,
        val frontier: Int         = 0,
        val proximity: Int        = 0,
        val oppEscapeArea: Int    = 4,
        val ownEscapeArea: Int    = 1,
        /** Nonlinear "the noose tightens" term: max(0, K-area)^2 multiplied
         *  by this weight, taken on opponent escape minus own escape.
         *  Stays at 0 in the wide-open midgame and only kicks in once a
         *  region is genuinely close to capture. */
        val pressureGain: Int     = 1,
        val pressureK: Int        = 30,
        /** Per-component pressure: encourages tightening any small
         *  opponent component instead of building a single mega-wall. */
        val componentPressure: Int = 6
    ) {
        companion object {
            val DEFAULT = Weights()
        }
    }

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
        val nonlinearPressure: Int,
        val componentPressure: Int,
        val total: Int
    ) {
        fun summary(): String =
            "total=$total | scoreΔ=$realScore " +
            "oTrap=$oppTrapped sTrap=$ownTrapped " +
            "oDepth=$oppDepthSum sDepth=$ownDepthSum " +
            "frontier=$frontier prox=$proximity " +
            "oEscape=$oppEscapeArea sEscape=$ownEscapeArea " +
            "nlP=$nonlinearPressure cP=$componentPressure"
    }

    fun evaluate(state: GameState, player: Player): Int =
        breakdown(state, player).total

    fun breakdown(state: GameState, player: Player): Breakdown {
        // Paranoid generalisation: treat every other seat as a single
        // virtual opponent. In 2-player games this collapses back to the
        // original behaviour exactly.
        val others: Set<Player> = state.players.toSet() - player
        val board = state.board
        val score = scorer.calculate(state)

        // Score delta = own score minus the worst-case opponent score.
        // In 2-player games this is identical to the old playerA-playerB term.
        val ownScore = score.forPlayer(player)
        val maxOtherScore = others.maxOfOrNull { score.forPlayer(it) } ?: 0
        val realScore = ownScore - maxOtherScore

        // BFS distance from border. Two perspectives:
        //   • distOpp — rootPlayer's stones are walls, every other seat is
        //     passable. Used to bury / trap *any* opponent.
        //   • distOwn — every non-root seat is a wall. Used to ensure we
        //     are not the one being buried by the coalition.
        val distOpp = bfsDistanceFromBorder(board, blockers = setOf(player))
        val distOwn = bfsDistanceFromBorder(board, blockers = others)

        // Live opponent dots = any non-root seat's stones not absorbed into
        // root's territory.
        val oppDots = others.flatMap { board.cellsOf(it) }.filter {
            board.get(it).territory != player
        }
        // Live own dots = root's stones not absorbed by anyone else.
        val ownDots = board.cellsOf(player).filter {
            val t = board.get(it).territory
            t == Player.NONE || t == player
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

        // Nonlinear "noose" term — only fires once a region is close to
        // capture. (K - area)^2 grows quadratically, so the AI prioritises
        // closing a half-shut region over starting a new one elsewhere.
        val K = weights.pressureK
        val nonlinearPressure =
            maxOf(0, K - oppEscapeArea) * maxOf(0, K - oppEscapeArea) -
            maxOf(0, K - ownEscapeArea) * maxOf(0, K - ownEscapeArea)

        // Per-component pressure: identify each connected opponent group
        // (8-conn, only counting *live* opponent dots — those still reachable
        // from the border via empty cells / their own colour) and reward
        // tightening the noose around the smallest. This is the user's
        // "surround opponent dots as they come" rule made explicit: we
        // care about the most-threatened group, not the global wall length.
        val componentPressure = computeComponentPressure(board, player, oppDots)

        val total = realScore         * weights.realScore +
                    oppTrapped        * weights.oppTrapped -
                    ownTrapped        * weights.ownTrapped +
                    oppDepthSum       * weights.oppDepthSum -
                    ownDepthSum       * weights.ownDepthSum +
                    frontier          * weights.frontier +
                    proximity         * weights.proximity -
                    oppEscapeArea     * weights.oppEscapeArea +
                    ownEscapeArea     * weights.ownEscapeArea +
                    nonlinearPressure * weights.pressureGain +
                    componentPressure * weights.componentPressure

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
            nonlinearPressure = nonlinearPressure,
            componentPressure = componentPressure,
            total = total
        )
    }

    /**
     * For each connected component of opponent dots (8-connected, live
     * dots only), compute its "escape budget" — the number of empty
     * cells in its border-reachable region — and turn it into a bonus
     * that grows quadratically as the budget shrinks.
     *
     * `distOpp` was produced by [bfsDistanceFromBorder] with our own
     * dots/territory acting as walls, so reachable cells share the same
     * connected region in that obstacle field. Two opponent groups that
     * sit in the same region naturally share their escape budget; that
     * is correct, since either group escaping leaves both alive.
     */
    private fun computeComponentPressure(
        board: Board,
        ourPlayer: Player,
        oppDots: List<Coord>
    ): Int {
        if (oppDots.isEmpty()) return 0

        // A cell is passable for the opponent iff our wall does not block it.
        fun passable(c: Coord): Boolean {
            val cell = board.get(c)
            // Once a cell is inside someone's territory, only that owner's
            // material lives there. Our territory is a hard wall for opp.
            return if (cell.territory != Player.NONE) cell.territory != ourPlayer
            else cell.dot != ourPlayer
        }

        // Flood-fill regions of cells passable for opp; record the
        // empty-cell count (escape budget) and which region every cell
        // belongs to.
        val regionId = Array(board.cols) { IntArray(board.rows) { -1 } }
        val regionEscape = mutableListOf<Int>()
        var nextId = 0
        for (sc in 0 until board.cols) for (sr in 0 until board.rows) {
            val start = Coord(sc, sr)
            if (regionId[sc][sr] != -1) continue
            if (!passable(start)) continue
            val queue = ArrayDeque<Coord>()
            queue.addLast(start)
            regionId[sc][sr] = nextId
            var escape = 0
            while (queue.isNotEmpty()) {
                val cur = queue.removeFirst()
                val curCell = board.get(cur)
                if (curCell.dot == Player.NONE && curCell.territory == Player.NONE) {
                    escape++
                }
                for (n in cur.neighbors4()) {
                    if (!board.isOnBoard(n)) continue
                    if (regionId[n.col][n.row] != -1) continue
                    if (!passable(n)) continue
                    regionId[n.col][n.row] = nextId
                    queue.addLast(n)
                }
            }
            regionEscape.add(escape)
            nextId++
        }

        // Tally: for each region, count opp dots in it; reward shrinkage.
        val groupSize = IntArray(nextId)
        for (d in oppDots) {
            val id = regionId[d.col][d.row]
            if (id >= 0) groupSize[id]++
        }

        var pressure = 0
        val K = weights.pressureK
        for (i in 0 until nextId) {
            val gSize = groupSize[i]
            if (gSize == 0) continue
            val deficit = maxOf(0, K - regionEscape[i])
            // Multiply by group size — tightening a region with 5 opp dots
            // matters more than tightening one with 1 dot.
            pressure += deficit * deficit * gSize
        }
        return pressure
    }

    /**
     * Multi-source BFS from every passable border cell. Returns a 2D array
     * where cells reachable from the border have their (4-connected)
     * geodesic distance, and unreachable cells have -1. A cell is
     * impassable iff its dot OR its territory belongs to any seat in
     * [blockers].
     */
    private fun bfsDistanceFromBorder(board: Board, blockers: Set<Player>): Array<IntArray> {
        val dist = Array(board.cols) { IntArray(board.rows) { -1 } }
        val queue = ArrayDeque<Coord>()

        fun isImpassable(c: Coord): Boolean {
            val cell = board.get(c)
            return if (cell.territory != Player.NONE) cell.territory in blockers
            else cell.dot in blockers
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
