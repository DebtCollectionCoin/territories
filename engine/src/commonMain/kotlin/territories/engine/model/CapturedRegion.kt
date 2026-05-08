package territories.engine.model

/** Transient result of capture detection — not persisted, not serialized. */
data class CapturedRegion(
    val cells: Set<Coord>,
    val capturedDots: Set<Coord>,
    val capturedTerritories: List<Territory>
)
