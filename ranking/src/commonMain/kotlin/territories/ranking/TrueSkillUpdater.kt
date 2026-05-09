package territories.ranking

import kotlin.math.sqrt

/**
 * TrueSkill rating updater for free-for-all matches.
 *
 * For an N-player FFA we treat each seat as a single-player team and apply
 * the standard TrueSkill update for each adjacent pair in the finishing
 * order (Herbrich et al. 2006, section 3). This is the canonical FFA
 * formulation used by Microsoft's TrueSkill ladder for Halo.
 *
 * Constants follow the original paper defaults:
 * - beta  = sigma0 / 2          (per-game performance noise)
 * - tau   = sigma0 / 100        (dynamics: skill drift between games)
 * - drawProbability = 10%       (rough estimate; territory rarely draws)
 *
 * Construct once and reuse — the updater is stateless.
 */
class TrueSkillUpdater(
    val beta: Double = Rating.DEFAULT.sigma / 2.0,
    val tau: Double = Rating.DEFAULT.sigma / 100.0,
    val drawProbability: Double = 0.10
) {
    init {
        require(beta > 0.0) { "beta must be positive" }
        require(tau >= 0.0) { "tau must be non-negative" }
        require(drawProbability in 0.0..1.0) { "drawProbability must be in [0,1]" }
    }

    /**
     * Update ratings given a finishing order.
     *
     * @param entries one per seat. `rank` is the finishing position
     *   (1 = winner, 2 = runner-up, …); equal ranks indicate a tie.
     * @return the new ratings in the same order as `entries`.
     */
    fun update(entries: List<Entry>): List<Rating> {
        require(entries.size >= 2) { "Need at least 2 players (got ${entries.size})" }

        // Step 1: add dynamics noise to each player's prior.
        val priors = entries.map { e ->
            val s = sqrt(e.rating.sigma * e.rating.sigma + tau * tau)
            Rating(mu = e.rating.mu, sigma = s)
        }

        // Step 2: sort indices by rank (ascending — best first), but keep
        // original positions so we can scatter results back.
        val sortedIdx = entries.indices.sortedBy { entries[it].rank }

        // Step 3: walk adjacent pairs in finishing order and apply pairwise
        // TrueSkill updates accumulating into running rating arrays.
        val mu = priors.map { it.mu }.toDoubleArray()
        val sigma = priors.map { it.sigma }.toDoubleArray()

        for (k in 0 until sortedIdx.size - 1) {
            val winnerIdx = sortedIdx[k]
            val loserIdx = sortedIdx[k + 1]
            val winnerRank = entries[winnerIdx].rank
            val loserRank = entries[loserIdx].rank
            val drew = winnerRank == loserRank

            val muW = mu[winnerIdx]; val sW = sigma[winnerIdx]
            val muL = mu[loserIdx];  val sL = sigma[loserIdx]

            val c2 = 2.0 * beta * beta + sW * sW + sL * sL
            val c = sqrt(c2)

            // Draw margin epsilon from drawProbability
            val epsilon = Gaussian.inverseCdf(0.5 * (drawProbability + 1.0)) *
                sqrt(2.0) * beta

            val t = (muW - muL) / c
            val eps = epsilon / c

            val v: Double
            val w: Double
            if (drew) {
                v = Gaussian.vDraw(t, eps)
                w = Gaussian.wDraw(t, eps)
            } else {
                v = Gaussian.vWin(t, eps)
                w = Gaussian.wWin(t, eps)
            }

            // Apply update to winner (mu increases)
            val muWNew = muW + (sW * sW / c) * v
            val sWNew = sW * sqrt((1.0 - (sW * sW / c2) * w).coerceAtLeast(1e-9))
            // Apply update to loser (mu decreases)
            val muLNew = muL - (sL * sL / c) * v
            val sLNew = sL * sqrt((1.0 - (sL * sL / c2) * w).coerceAtLeast(1e-9))

            mu[winnerIdx] = muWNew
            sigma[winnerIdx] = sWNew
            mu[loserIdx] = muLNew
            sigma[loserIdx] = sLNew
        }

        return entries.indices.map { Rating(mu[it], sigma[it]) }
    }

    /**
     * One match entry: a player's prior rating plus their finishing rank.
     * Lower rank = better finish (1 wins). Equal ranks = draw.
     */
    data class Entry(val rating: Rating, val rank: Int) {
        init { require(rank >= 1) { "rank must be >= 1 (got $rank)" } }
    }

    /**
     * Convenience for 1v1 matches.
     *
     * @param winner prior rating of the winner
     * @param loser prior rating of the loser
     * @param drawn whether the match was drawn (default: no)
     * @return a pair of (newWinnerRating, newLoserRating)
     */
    fun updateOneVsOne(
        winner: Rating,
        loser: Rating,
        drawn: Boolean = false
    ): Pair<Rating, Rating> {
        val winnerRank = 1
        val loserRank = if (drawn) 1 else 2
        val results = update(listOf(
            Entry(winner, winnerRank),
            Entry(loser, loserRank)
        ))
        return results[0] to results[1]
    }
}
