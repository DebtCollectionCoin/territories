package territories.ranking

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.math.abs

/**
 * Internal helpers: standard normal pdf, cdf, and inverse cdf, plus the
 * truncated-Gaussian correction terms (`v` and `w`) used by TrueSkill.
 *
 * These are pure-math helpers with no external dependencies so the ranking
 * module stays a vanilla KMP library.
 */
internal object Gaussian {

    /** Standard normal probability density. */
    fun pdf(x: Double): Double = exp(-0.5 * x * x) / sqrt(2.0 * PI)

    /**
     * Standard normal cumulative distribution.
     *
     * Uses Abramowitz & Stegun 26.2.17 (max error ~7.5e-8) — adequate for
     * skill ratings.
     */
    fun cdf(x: Double): Double {
        // erf-based formulation; we implement erf via A&S 7.1.26.
        val sign = if (x >= 0.0) 1.0 else -1.0
        val ax = abs(x) / sqrt(2.0)
        val t = 1.0 / (1.0 + 0.3275911 * ax)
        val y = 1.0 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t
                - 0.284496736) * t + 0.254829592) * t * exp(-ax * ax)
        val erf = sign * y
        return 0.5 * (1.0 + erf)
    }

    /**
     * Inverse standard normal cdf (probit). Beasley-Springer-Moro
     * approximation. Used for converting win probabilities into draw margins.
     */
    fun inverseCdf(p: Double): Double {
        require(p > 0.0 && p < 1.0) { "p must be in (0,1) (got $p)" }
        val a = doubleArrayOf(
            -3.969683028665376e+01, 2.209460984245205e+02,
            -2.759285104469687e+02, 1.383577518672690e+02,
            -3.066479806614716e+01, 2.506628277459239e+00
        )
        val b = doubleArrayOf(
            -5.447609879822406e+01, 1.615858368580409e+02,
            -1.556989798598866e+02, 6.680131188771972e+01,
            -1.328068155288572e+01
        )
        val c = doubleArrayOf(
            -7.784894002430293e-03, -3.223964580411365e-01,
            -2.400758277161838e+00, -2.549732539343734e+00,
            4.374664141464968e+00, 2.938163982698783e+00
        )
        val d = doubleArrayOf(
            7.784695709041462e-03, 3.224671290700398e-01,
            2.445134137142996e+00, 3.754408661907416e+00
        )
        val pLow = 0.02425
        val pHigh = 1.0 - pLow
        return when {
            p < pLow -> {
                val q = sqrt(-2.0 * ln(p))
                (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
                    ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1.0)
            }
            p <= pHigh -> {
                val q = p - 0.5
                val r = q * q
                (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q /
                    (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1.0)
            }
            else -> {
                val q = sqrt(-2.0 * ln(1.0 - p))
                -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
                    ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1.0)
            }
        }
    }

    /** TrueSkill v-function for win/loss outcomes. */
    fun vWin(t: Double, epsilon: Double): Double {
        val denom = cdf(t - epsilon)
        // Avoid catastrophic cancellation when t is very negative
        return if (denom < 1e-300) -(t - epsilon) else pdf(t - epsilon) / denom
    }

    /** TrueSkill w-function for win/loss outcomes. */
    fun wWin(t: Double, epsilon: Double): Double {
        val v = vWin(t, epsilon)
        return v * (v + (t - epsilon))
    }

    /** TrueSkill v-function for draw outcomes. */
    fun vDraw(t: Double, epsilon: Double): Double {
        val absT = abs(t)
        val denom = cdf(epsilon - absT) - cdf(-epsilon - absT)
        if (denom < 1e-300) return if (t < 0) -t - epsilon else -t + epsilon
        val num = pdf(-epsilon - absT) - pdf(epsilon - absT)
        val sign = if (t < 0) -1.0 else 1.0
        return sign * num / denom
    }

    /** TrueSkill w-function for draw outcomes. */
    fun wDraw(t: Double, epsilon: Double): Double {
        val absT = abs(t)
        val denom = cdf(epsilon - absT) - cdf(-epsilon - absT)
        if (denom < 1e-300) return 1.0
        val v = vDraw(t, epsilon)
        return v * v + ((epsilon - absT) * pdf(epsilon - absT) -
            (-epsilon - absT) * pdf(-epsilon - absT)) / denom
    }
}
