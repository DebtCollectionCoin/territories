package territories.session

import kotlinx.serialization.Serializable
import territories.engine.model.*

@Serializable
sealed class GameEvent {
    @Serializable
    data class GameStarted(val sessionId: String, val config: GameConfig) : GameEvent()

    @Serializable
    data class MovePlayed(val move: Move, val resultingStateHash: Int) : GameEvent()

    @Serializable
    data class UndoRequested(val byPlayer: Player) : GameEvent()

    @Serializable
    object UndoApproved : GameEvent()

    @Serializable
    object UndoDenied : GameEvent()

    @Serializable
    data class PlayerSurrendered(val player: Player) : GameEvent()

    @Serializable
    data class GameOver(val winner: Player, val finalScore: Score) : GameEvent()

    @Serializable
    object Ping : GameEvent()

    @Serializable
    object Pong : GameEvent()
}
