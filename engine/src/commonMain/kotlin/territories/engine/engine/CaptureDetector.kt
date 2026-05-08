package territories.engine.engine

import territories.engine.model.*

class CaptureDetector {

    /**
     * Returns all regions captured by [player] after placing a dot at [coord].
     *
     * Algorithm:
     * 1. Seed from each 8-directional neighbor of [coord] that is NOT player's own dot/territory.
     * 2. Merge touching seeds into connected groups via BFS.
     * 3. For each group: flood-fill (4-directional) blocked by player's dots/territory.
     * 4. If flood fill does NOT reach the board border → captured region.
     */
    fun detectCaptures(coord: Coord, player: Player, board: Board): List<CapturedRegion> {
        // Collect 8-directional neighbor seeds that are not the active player's material
        val seeds = coord.neighbors8()
            .filter { board.isOnBoard(it) && !isPlayerMaterial(board, it, player) }

        if (seeds.isEmpty()) return emptyList()

        // Merge seeds into connected components (so each component gets one flood-fill)
        val groups = mergeConnectedSeeds(seeds, board, player)

        val results = mutableListOf<CapturedRegion>()
        for (group in groups) {
            val filled = floodFill(group.first(), board, player)
            if (!touchesBorder(filled, board)) {
                val capturedDots = filled.filter { board.get(it).dot == player.opponent() }.toSet()
                // Only capture if there is at least one opponent dot inside (rule §4.1)
                if (capturedDots.isNotEmpty()) {
                    results.add(buildCapturedRegion(filled, board, emptyList()))
                }
            }
        }
        return results
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun isPlayerMaterial(board: Board, coord: Coord, player: Player): Boolean {
        val cell = board.get(coord)
        return cell.dot == player || cell.territory == player
    }

    /**
     * 4-directional BFS from [start], blocked by [player]'s dots and territory cells.
     * Returns all reachable coords (the interior region).
     */
    private fun floodFill(start: Coord, board: Board, player: Player): Set<Coord> {
        val visited = mutableSetOf<Coord>()
        val queue = ArrayDeque<Coord>()
        queue.add(start)
        visited.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (neighbor in current.neighbors4()) {
                if (neighbor in visited) continue
                if (!board.isOnBoard(neighbor)) continue
                if (isPlayerMaterial(board, neighbor, player)) continue
                visited.add(neighbor)
                queue.add(neighbor)
            }
        }
        return visited
    }

    private fun touchesBorder(region: Set<Coord>, board: Board): Boolean =
        region.any { board.isBorder(it) }

    /**
     * Merge seed coords into connected groups using 8-directional adjacency.
     * Each group can be passed a single flood-fill seed.
     */
    private fun mergeConnectedSeeds(
        seeds: List<Coord>,
        board: Board,
        player: Player
    ): List<Set<Coord>> {
        // Use union-find-style merging via BFS over seeds
        val seedSet = seeds.toMutableSet()
        val groups = mutableListOf<Set<Coord>>()

        while (seedSet.isNotEmpty()) {
            val group = mutableSetOf<Coord>()
            val queue = ArrayDeque<Coord>()
            val first = seedSet.first()
            queue.add(first)
            group.add(first)
            seedSet.remove(first)

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                // Expand using flood-fill from this seed — add all reachable non-player cells
                val region = floodFill(current, board, player)
                // Any other seeds reachable in the same flood-fill belong to the same group
                val absorbed = seedSet.filter { it in region }
                for (s in absorbed) {
                    group.add(s)
                    seedSet.remove(s)
                    queue.add(s)
                }
            }
            groups.add(group)
        }
        return groups
    }

    private fun buildCapturedRegion(
        cells: Set<Coord>,
        board: Board,
        existingTerritories: List<territories.engine.model.Territory>
    ): CapturedRegion {
        val capturedDots = cells.filter { board.get(it).dot != Player.NONE }.toSet()
        val absorbed = existingTerritories.filter { t -> t.cells.any { it in cells } }
        return CapturedRegion(cells, capturedDots, absorbed)
    }
}
