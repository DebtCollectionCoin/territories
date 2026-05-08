package territories.engine.model

import kotlinx.serialization.Serializable

@Serializable
sealed class Move {
    @Serializable
    data class PlaceDot(
        val coord: Coord,
        val player: Player,
        val moveNumber: Int
    ) : Move()

    @Serializable
    data class Surrender(
        val player: Player,
        val moveNumber: Int
    ) : Move()
}
