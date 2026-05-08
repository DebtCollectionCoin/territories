package territories.engine.model

import kotlinx.serialization.Serializable

@Serializable
enum class Player {
    A, B, NONE
}

fun Player.opponent(): Player = when (this) {
    Player.A -> Player.B
    Player.B -> Player.A
    Player.NONE -> Player.NONE
}
