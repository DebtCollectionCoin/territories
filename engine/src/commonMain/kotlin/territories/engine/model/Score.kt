package territories.engine.model

import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class Score(
    val playerA: Int = 0,
    val playerB: Int = 0
) {
    val leader: Player get() = when {
        playerA > playerB -> Player.A
        playerB > playerA -> Player.B
        else -> Player.NONE
    }
    val difference: Int get() = abs(playerA - playerB)
}
