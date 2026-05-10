package territories.protocol

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import territories.engine.model.Coord
import territories.engine.model.GameConfig
import territories.engine.model.GameState
import territories.engine.model.Player
import territories.engine.model.Score

/**
 * Wire schema for the multiplayer server (see MULTIPLAYER_DESIGN.md §4.2).
 *
 * Both directions use a sealed hierarchy so the JSON tag drives Kotlinx
 * serialization's polymorphism. Schema is intentionally narrow at this
 * stage — only the messages required for Phase D (lobby + live game).
 */

@Serializable
sealed interface ClientMessage

@Serializable
sealed interface ServerMessage

// ── Handshake ───────────────────────────────────────────────────────────

@Serializable
@SerialName("Hello")
data class Hello(
    val authToken: String? = null,
    val clientVersion: String,
) : ClientMessage

@Serializable
@SerialName("Welcome")
data class Welcome(
    val userId: String,
    val displayName: String,
    val ratingMu: Double = 25.0,
    val ratingSigma: Double = 25.0 / 3.0,
) : ServerMessage

// ── Lobby ───────────────────────────────────────────────────────────────

@Serializable
@SerialName("CreateLobby")
data class CreateLobby(
    val config: GameConfig,
    val ranked: Boolean = false,
) : ClientMessage

@Serializable
@SerialName("JoinLobby")
data class JoinLobby(val lobbyId: String) : ClientMessage

@Serializable
@SerialName("LeaveLobby")
data class LeaveLobby(val lobbyId: String) : ClientMessage

@Serializable
@SerialName("LobbyState")
data class LobbyState(
    val lobbyId: String,
    val players: List<LobbyPlayer>,
    val config: GameConfig,
    val ranked: Boolean,
) : ServerMessage

@Serializable
data class LobbyPlayer(
    val userId: String,
    val displayName: String,
    val ready: Boolean,
)

// ── Game ────────────────────────────────────────────────────────────────

@Serializable
@SerialName("GameStarted")
data class GameStarted(
    val gameId: String,
    val seatOrder: Map<Player, String>,   // seat → userId
    val initialState: GameState,
) : ServerMessage

@Serializable
@SerialName("SubmitMove")
data class SubmitMove(
    val gameId: String,
    val coord: Coord,
    val expectedMoveCount: Int,
) : ClientMessage

@Serializable
@SerialName("MoveApplied")
data class MoveApplied(
    val gameId: String,
    val coord: Coord,
    val newState: GameState,
    val deadlineEpochMs: Long? = null,
) : ServerMessage

@Serializable
@SerialName("MoveRejected")
data class MoveRejected(
    val gameId: String,
    val expectedMoveCount: Int,
    val reason: String,
) : ServerMessage

@Serializable
@SerialName("GameEnded")
data class GameEnded(
    val gameId: String,
    val finalScore: Score,
    val winners: List<Player>,
    val ratingDeltas: Map<String, Double> = emptyMap(),
) : ServerMessage

@Serializable
@SerialName("Resign")
data class Resign(val gameId: String) : ClientMessage

@Serializable
@SerialName("Resume")
data class Resume(
    val gameId: String,
    val lastKnownMoveCount: Int,
) : ClientMessage

@Serializable
@SerialName("Heartbeat")
object Heartbeat : ClientMessage

// ── Errors ──────────────────────────────────────────────────────────────

@Serializable
@SerialName("Error")
data class ServerError(val code: String, val message: String) : ServerMessage
