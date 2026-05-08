package territories.engine.model

import kotlinx.serialization.Serializable

@Serializable
enum class ScoringVariant {
    TERRITORY_AREA,
    CAPTURED_DOTS
}
