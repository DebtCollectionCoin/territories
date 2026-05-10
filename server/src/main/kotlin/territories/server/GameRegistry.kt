package territories.server

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import territories.engine.engine.ScoreCalculator
import territories.engine.model.GameConfig
import territories.engine.model.Player
import territories.protocol.LobbyPlayer
import territories.protocol.LobbyState

/**
 * In-memory lobby + game registry. Phase D scope: single-process,
 * no persistence. Replace with Postgres + Redis in Phase E.
 */
class GameRegistry {
    private val lobbies = ConcurrentHashMap<String, Lobby>()
    private val games   = ConcurrentHashMap<String, ServerGame>()
    private val nextId  = AtomicLong(1)
    private val scorer  = ScoreCalculator()

    data class Lobby(
        val id: String,
        val hostUserId: String,
        val config: GameConfig,
        val ranked: Boolean,
        val players: MutableList<LobbyPlayer> = mutableListOf(),
    ) {
        fun snapshot(): LobbyState =
            LobbyState(id, players.toList(), config, ranked)
    }

    fun createLobby(host: ConnectedUser, config: GameConfig, ranked: Boolean): Lobby {
        val id = "lob-${nextId.getAndIncrement()}"
        val lobby = Lobby(id, host.userId, config, ranked)
        lobby.players += LobbyPlayer(host.userId, host.displayName, ready = false)
        lobbies[id] = lobby
        return lobby
    }

    fun joinLobby(user: ConnectedUser, lobbyId: String): Lobby? {
        val lobby = lobbies[lobbyId] ?: return null
        if (lobby.players.size >= lobby.config.playerCount) return null
        if (lobby.players.none { it.userId == user.userId }) {
            lobby.players += LobbyPlayer(user.userId, user.displayName, ready = false)
        }
        return lobby
    }

    fun leaveLobby(userId: String, lobbyId: String): Lobby? {
        val lobby = lobbies[lobbyId] ?: return null
        lobby.players.removeAll { it.userId == userId }
        if (lobby.players.isEmpty()) lobbies.remove(lobbyId)
        return lobby
    }

    fun isLobbyReady(lobby: Lobby): Boolean =
        lobby.players.size == lobby.config.playerCount

    /** Promote a full lobby into a live [ServerGame]. */
    fun startGame(lobby: Lobby): ServerGame {
        val seats = listOf(Player.A, Player.B, Player.C, Player.D)
            .take(lobby.config.playerCount)
        val seatOrder = seats.zip(lobby.players).associate { (seat, p) -> seat to p.userId }
        val gameId = "g-${nextId.getAndIncrement()}"
        val game = ServerGame(gameId, lobby.config, seatOrder)
        games[gameId] = game
        lobbies.remove(lobby.id)
        return game
    }

    fun getGame(id: String): ServerGame? = games[id]
    fun getLobby(id: String): Lobby? = lobbies[id]

    fun finalScore(game: ServerGame) = scorer.calculate(game.state.value)
}

data class ConnectedUser(
    val userId: String,
    val displayName: String,
)
