package territories.engine.model

import kotlinx.serialization.Serializable

@Serializable
enum class GamePhase {
    IN_PROGRESS,
    GAME_OVER,
    SURRENDERED
}
