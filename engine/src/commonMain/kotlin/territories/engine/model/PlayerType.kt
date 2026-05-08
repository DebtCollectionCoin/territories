package territories.engine.model

import kotlinx.serialization.Serializable

@Serializable
enum class PlayerType {
    HUMAN,
    AI_EASY,
    AI_MEDIUM,
    AI_HARD
}
