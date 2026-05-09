package territories.ranking

import territories.engine.model.GameState
import territories.engine.model.Player

/**
 * Helpers that bridge the engine's `GameState` to TrueSkill rating updates.
 *
 * Lives in `:ranking` (which depends on `:engine`) so a future server or
 * session module can call into it without each app reimplementing the
 * mapping.
 */
object MatchOutcome {

    /**
     * Compute a finishing rank for each seat in a finished game.
     *
     * Ranks follow standard competition order (1 = best): a seat with a
     * higher score gets a lower (better) rank. Ties produce equal ranks
     * with the next-rank gap honoring the number of tied players.
     *
     * Eliminated seats finish below all surviving non-zero seats.
     *
     * @return a map from seat to finishing rank (1-based).
     */
    fun rankings(state: GameState): Map<Player, Int> {
        require(state.isGameOver) { "rankings() requires a finished game" }
        val seats = state.players
        // For surrendered 2-player games (and any game where a winner is
        // explicitly set), seats other than the winner that are not the
        // winner are treated as having "lost", which we model by giving the
        // winner the maximum score and any non-eliminated non-winner the
        // minimum non-eliminated score. Eliminated seats are always last.
        val explicitWinner = state.winner.takeIf { it != Player.NONE && it in seats }
        val scored = seats.map { p ->
            val rawScore = state.score.forPlayer(p)
            val effective = when {
                p in state.eliminated -> Int.MIN_VALUE
                explicitWinner != null && p == explicitWinner -> Int.MAX_VALUE
                explicitWinner != null && p != explicitWinner -> Int.MIN_VALUE + 1
                else -> rawScore
            }
            p to effective
        }.sortedByDescending { it.second }

        val result = mutableMapOf<Player, Int>()
        var currentRank = 1
        var processed = 0
        var i = 0
        while (i < scored.size) {
            var j = i
            // gather all seats tied with scored[i]
            while (j < scored.size && scored[j].second == scored[i].second) j++
            // standard competition ranking: tied seats share the lowest rank
            for (k in i until j) result[scored[k].first] = currentRank
            processed += (j - i)
            currentRank = processed + 1
            i = j
        }
        return result
    }

    /**
     * Build a list of TrueSkill entries from a finished match.
     *
     * @param state finished game
     * @param priors prior rating for each seat. Seats not in the map use
     *   `Rating.DEFAULT`.
     * @return entries paired with the seat they belong to (same order as
     *   `state.players`), suitable for passing to `TrueSkillUpdater.update`.
     */
    fun entries(
        state: GameState,
        priors: Map<Player, Rating> = emptyMap()
    ): List<Pair<Player, TrueSkillUpdater.Entry>> {
        val ranks = rankings(state)
        return state.players.map { p ->
            val prior = priors[p] ?: Rating.DEFAULT
            p to TrueSkillUpdater.Entry(prior, ranks.getValue(p))
        }
    }
}
