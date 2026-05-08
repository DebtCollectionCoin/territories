package territories.engine.model

import kotlinx.serialization.Serializable

@Serializable
data class Coord(val col: Int, val row: Int) {

    /** Orthogonal (4-directional) neighbors — used for flood-fill interior expansion. */
    fun neighbors4(): List<Coord> = listOf(
        Coord(col - 1, row),
        Coord(col + 1, row),
        Coord(col, row - 1),
        Coord(col, row + 1)
    )

    /** Moore (8-directional) neighbors — used for ring-closure detection. */
    fun neighbors8(): List<Coord> = listOf(
        Coord(col - 1, row - 1), Coord(col, row - 1), Coord(col + 1, row - 1),
        Coord(col - 1, row),                           Coord(col + 1, row),
        Coord(col - 1, row + 1), Coord(col, row + 1), Coord(col + 1, row + 1)
    )
}
