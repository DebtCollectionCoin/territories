package territories.engine.model

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max

/**
 * Per-seat score totals. [playerA] and [playerB] are populated for all
 * games; [playerC] and [playerD] are populated only for 3- or 4-player
 * games and remain at 0 otherwise. Use [forPlayer] for code that should
 * be player-count-agnostic.
 */
@Serializable
data class Score(
    val playerA: Int = 0,
    val playerB: Int = 0,
    val playerC: Int = 0,
    val playerD: Int = 0
) {
    /** Returns the score for [player]; [Player.NONE] returns 0. */
    fun forPlayer(player: Player): Int = when (player) {
        Player.A -> playerA
        Player.B -> playerB
        Player.C -> playerC
        Player.D -> playerD
        Player.NONE -> 0
    }

    /** Returns the seat with the highest score, or [Player.NONE] on ties. */
    val leader: Player
        get() {
            val pairs = listOf(
                Player.A to playerA,
                Player.B to playerB,
                Player.C to playerC,
                Player.D to playerD
            ).filter { it.second > 0 || it.first == Player.A || it.first == Player.B }
            val top = pairs.maxByOrNull { it.second } ?: return Player.NONE
            val tied = pairs.count { it.second == top.second }
            return if (tied > 1) Player.NONE else top.first
        }

    /** Margin between the top scorer and the runner-up. Always non-negative. */
    val difference: Int
        get() {
            val sorted = listOf(playerA, playerB, playerC, playerD).sortedDescending()
            return max(0, sorted[0] - sorted[1])
        }
}
