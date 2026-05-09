package territories.ranking

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.math.abs

class TrueSkillUpdaterTest {

    private fun close(a: Double, b: Double, eps: Double = 1e-2): Boolean = abs(a - b) <= eps

    @Test
    fun defaults_haveExpectedValues() {
        val r = Rating.DEFAULT
        assertEquals(25.0, r.mu)
        assertEquals(25.0 / 3.0, r.sigma)
        assertEquals(0.0, r.conservative)
    }

    @Test
    fun oneVsOne_winnerGains_loserLoses() {
        val updater = TrueSkillUpdater()
        val (newW, newL) = updater.updateOneVsOne(Rating.DEFAULT, Rating.DEFAULT)
        assertTrue(newW.mu > Rating.DEFAULT.mu, "winner mu should rise")
        assertTrue(newL.mu < Rating.DEFAULT.mu, "loser mu should fall")
        assertTrue(newW.sigma < Rating.DEFAULT.sigma, "winner sigma should shrink")
        assertTrue(newL.sigma < Rating.DEFAULT.sigma, "loser sigma should shrink")
        // Symmetry: gain == loss for equal priors
        val gain = newW.mu - Rating.DEFAULT.mu
        val loss = Rating.DEFAULT.mu - newL.mu
        assertTrue(close(gain, loss, 1e-6), "symmetric update: gain=$gain loss=$loss")
    }

    @Test
    fun oneVsOne_drawnMatch_keepsMusUnchangedForEqualPriors() {
        val updater = TrueSkillUpdater()
        val (newW, newL) = updater.updateOneVsOne(
            Rating.DEFAULT, Rating.DEFAULT, drawn = true
        )
        assertTrue(close(newW.mu, Rating.DEFAULT.mu, 1e-6))
        assertTrue(close(newL.mu, Rating.DEFAULT.mu, 1e-6))
        assertTrue(newW.sigma < Rating.DEFAULT.sigma, "draw still shrinks sigma")
    }

    @Test
    fun oneVsOne_underdogBeatingFavorite_gainsMore() {
        val updater = TrueSkillUpdater()
        val favorite = Rating(mu = 30.0, sigma = 25.0 / 3.0)
        val underdog = Rating(mu = 20.0, sigma = 25.0 / 3.0)
        val (newU, newF) = updater.updateOneVsOne(winner = underdog, loser = favorite)
        val underdogGain = newU.mu - underdog.mu
        val favoriteLoss = favorite.mu - newF.mu
        // Underdog winning is "surprising", so update is large
        assertTrue(underdogGain > 4.0, "underdog gain should be large, got $underdogGain")
        assertTrue(favoriteLoss > 4.0, "favorite loss should be large, got $favoriteLoss")
    }

    @Test
    fun ffaFourPlayer_rankOrderingPreservedInDeltas() {
        val updater = TrueSkillUpdater()
        val r = Rating.DEFAULT
        val results = updater.update(listOf(
            TrueSkillUpdater.Entry(r, 1),
            TrueSkillUpdater.Entry(r, 2),
            TrueSkillUpdater.Entry(r, 3),
            TrueSkillUpdater.Entry(r, 4)
        ))
        assertEquals(4, results.size)
        // Finishing order 1 > 2 > 3 > 4 in mu
        assertTrue(results[0].mu > results[1].mu)
        assertTrue(results[1].mu > results[2].mu)
        assertTrue(results[2].mu > results[3].mu)
        // All sigmas shrink
        for (newR in results) assertTrue(newR.sigma < r.sigma)
        // First-place gains, last-place loses (sequential-pairwise FFA does
        // not strictly conserve sum-of-mus across the whole field, but
        // best/worst extremes always move in the expected direction).
        assertTrue(results[0].mu > r.mu, "winner mu should rise above prior")
        assertTrue(results[3].mu < r.mu, "last place mu should fall below prior")
    }

    @Test
    fun ffaThreePlayer_tieAtTop_bothBeatLoser() {
        val updater = TrueSkillUpdater()
        val r = Rating.DEFAULT
        // Two seats tie for first, one comes in last
        val results = updater.update(listOf(
            TrueSkillUpdater.Entry(r, 1),
            TrueSkillUpdater.Entry(r, 1),
            TrueSkillUpdater.Entry(r, 2)
        ))
        // Both tied winners must end above the loser (sequential-pairwise
        // FFA propagates updates left-to-right, so the two winners aren't
        // bit-identical, but both finish above the loser).
        assertTrue(results[0].mu > results[2].mu, "winner-1 above loser")
        assertTrue(results[1].mu > results[2].mu, "winner-2 above loser")
        assertTrue(results[2].mu < r.mu, "loser drops below prior")
    }

    @Test
    fun unequalPriors_inFfa_stillUpdate() {
        val updater = TrueSkillUpdater()
        val strong = Rating(mu = 35.0, sigma = 5.0)
        val weak1 = Rating(mu = 20.0, sigma = 8.0)
        val weak2 = Rating(mu = 22.0, sigma = 8.0)
        val results = updater.update(listOf(
            TrueSkillUpdater.Entry(weak1, 1),    // surprise winner
            TrueSkillUpdater.Entry(weak2, 2),
            TrueSkillUpdater.Entry(strong, 3)    // favorite came last
        ))
        assertTrue(results[0].mu > weak1.mu, "weak1 should gain after winning")
        assertTrue(results[2].mu < strong.mu, "favorite should lose after losing")
    }

    @Test
    fun rejectInvalid_singleEntry() {
        val updater = TrueSkillUpdater()
        try {
            updater.update(listOf(TrueSkillUpdater.Entry(Rating.DEFAULT, 1)))
            error("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }
}

class GaussianTest {
    private fun close(a: Double, b: Double, eps: Double = 1e-4): Boolean = abs(a - b) <= eps

    @Test
    fun cdfSpotChecks() {
        assertTrue(close(Gaussian.cdf(0.0), 0.5))
        assertTrue(close(Gaussian.cdf(1.0), 0.8413, 1e-3))
        assertTrue(close(Gaussian.cdf(-1.0), 0.1587, 1e-3))
        assertTrue(close(Gaussian.cdf(1.96), 0.9750, 1e-3))
    }

    @Test
    fun inverseCdf_isInverseOfCdf() {
        for (p in listOf(0.05, 0.25, 0.5, 0.75, 0.95)) {
            val x = Gaussian.inverseCdf(p)
            assertTrue(close(Gaussian.cdf(x), p, 1e-3),
                "cdf(invCdf($p)) should equal $p, got ${Gaussian.cdf(x)}")
        }
    }
}
