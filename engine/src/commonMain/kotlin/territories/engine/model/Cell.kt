package territories.engine.model

import kotlinx.serialization.Serializable

@Serializable
data class Cell(
    val dot: Player = Player.NONE,
    val territory: Player = Player.NONE
) {
    val isEmpty: Boolean get() = dot == Player.NONE && territory == Player.NONE
    val isLegal: Boolean get() = isEmpty
}
