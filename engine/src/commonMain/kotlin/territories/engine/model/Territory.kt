package territories.engine.model

import kotlinx.serialization.Serializable

@Serializable
data class Territory(
    val id: String,
    val owner: Player,
    val cells: Set<Coord>,
    val capturedDots: Set<Coord>,
    val capturedAt: Int,
    val absorbedTerritoryIds: List<String> = emptyList()
) {
    val area: Int get() = cells.size
    val capturedDotCount: Int get() = capturedDots.size
}
