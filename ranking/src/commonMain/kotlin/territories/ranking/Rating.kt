package territories.ranking

/**
 * A player's skill estimate under the TrueSkill rating system.
 *
 * TrueSkill models skill as a Gaussian: mu is the believed mean strength,
 * sigma is the uncertainty. The conservative public rating shown to users is
 * usually `mu - 3 * sigma`, i.e. the lower bound of a 99% confidence interval.
 *
 * Reference: Herbrich, Minka, Graepel — "TrueSkill: A Bayesian Skill Rating
 * System" (NIPS 2006).
 */
data class Rating(
    val mu: Double,
    val sigma: Double
) {
    /** Conservative public rating: mean minus 3 standard deviations. */
    val conservative: Double get() = mu - 3.0 * sigma

    init {
        require(sigma > 0.0) { "sigma must be positive (got $sigma)" }
    }

    companion object {
        /** Default starting rating: mu = 25, sigma = 25/3. */
        val DEFAULT: Rating = Rating(mu = 25.0, sigma = 25.0 / 3.0)
    }
}
