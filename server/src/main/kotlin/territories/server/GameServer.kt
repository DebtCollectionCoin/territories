package territories.server

import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import territories.protocol.ClientMessage
import territories.protocol.CreateLobby
import territories.protocol.GameEnded
import territories.protocol.GameStarted
import territories.protocol.Heartbeat
import territories.protocol.Hello
import territories.protocol.JoinLobby
import territories.protocol.LeaveLobby
import territories.protocol.MoveApplied
import territories.protocol.MoveRejected
import territories.protocol.Resign
import territories.protocol.Resume
import territories.protocol.ServerError
import territories.protocol.ServerMessage
import territories.protocol.SubmitMove
import territories.protocol.Welcome

/**
 * Phase D coordinator: holds the [GameRegistry], dispatches WebSocket
 * frames to lobby/game logic, and fans out server messages to all
 * connected sessions in a lobby or game.
 */
class GameServer(val registry: GameRegistry) {
    val json: Json = Json { ignoreUnknownKeys = true; classDiscriminator = "type" }

    private val sessions = ConcurrentHashMap<String, Session>() // userId → session
    private val lobbySubs = ConcurrentHashMap<String, MutableSet<String>>() // lobbyId → userIds
    private val gameSubs  = ConcurrentHashMap<String, MutableSet<String>>() // gameId  → userIds

    private val nextUser = AtomicLong(1)

    class Session(
        val user: ConnectedUser,
        val send: suspend (ServerMessage) -> Unit,
    )

    fun anonymousUser(): ConnectedUser {
        val n = nextUser.getAndIncrement()
        return ConnectedUser(userId = "u-$n", displayName = "Player $n")
    }

    fun register(session: Session) {
        sessions[session.user.userId] = session
    }

    fun unregister(userId: String) {
        sessions.remove(userId)
        lobbySubs.values.forEach { it.remove(userId) }
        gameSubs.values.forEach { it.remove(userId) }
    }

    suspend fun handleMessage(session: Session, msg: ClientMessage) {
        when (msg) {
            is Hello       -> session.send(Welcome(session.user.userId, session.user.displayName))
            is CreateLobby -> {
                val lobby = registry.createLobby(session.user, msg.config, msg.ranked)
                lobbySubs.getOrPut(lobby.id) { ConcurrentHashMap.newKeySet() } += session.user.userId
                broadcastLobby(lobby.id)
                maybeStartGame(lobby.id)
            }
            is JoinLobby   -> {
                val lobby = registry.joinLobby(session.user, msg.lobbyId)
                if (lobby == null) {
                    session.send(ServerError("LOBBY_FULL_OR_MISSING", "lobby ${msg.lobbyId} unavailable"))
                } else {
                    lobbySubs.getOrPut(lobby.id) { ConcurrentHashMap.newKeySet() } += session.user.userId
                    broadcastLobby(lobby.id)
                    maybeStartGame(lobby.id)
                }
            }
            is LeaveLobby  -> {
                registry.leaveLobby(session.user.userId, msg.lobbyId)
                lobbySubs[msg.lobbyId]?.remove(session.user.userId)
                broadcastLobby(msg.lobbyId)
            }
            is SubmitMove  -> handleSubmit(session, msg)
            is Resign      -> session.send(ServerError("NOT_IMPLEMENTED", "resign in Phase E"))
            is Resume      -> session.send(ServerError("NOT_IMPLEMENTED", "resume in Phase E"))
            Heartbeat      -> { /* keepalive */ }
        }
    }

    private suspend fun broadcastLobby(lobbyId: String) {
        val lobby = registry.getLobby(lobbyId) ?: return
        val state = lobby.snapshot()
        for (uid in lobbySubs[lobbyId].orEmpty()) {
            sessions[uid]?.send?.invoke(state)
        }
    }

    private suspend fun maybeStartGame(lobbyId: String) {
        val lobby = registry.getLobby(lobbyId) ?: return
        if (!registry.isLobbyReady(lobby)) return
        val game = registry.startGame(lobby)
        val started = GameStarted(game.gameId, game.seatOrder, game.state.value)
        val users = lobbySubs.remove(lobbyId).orEmpty().toList()
        gameSubs.getOrPut(game.gameId) { ConcurrentHashMap.newKeySet() } += users
        for (uid in users) sessions[uid]?.send?.invoke(started)
    }

    private suspend fun handleSubmit(session: Session, msg: SubmitMove) {
        val game = registry.getGame(msg.gameId)
            ?: return session.send(ServerError("GAME_NOT_FOUND", msg.gameId))
        val result = game.applyMove(session.user.userId, msg.coord, msg.expectedMoveCount)
        when (result) {
            is ServerGame.MoveResult.Applied -> {
                val applied = MoveApplied(game.gameId, msg.coord, result.newState)
                for (uid in gameSubs[game.gameId].orEmpty()) {
                    sessions[uid]?.send?.invoke(applied)
                }
                if (result.newState.isGameOver) {
                    val score = registry.finalScore(game)
                    val best = listOf(score.playerA, score.playerB, score.playerC, score.playerD).max()
                    val seats = listOf(territories.engine.model.Player.A,
                                       territories.engine.model.Player.B,
                                       territories.engine.model.Player.C,
                                       territories.engine.model.Player.D)
                        .take(game.config.playerCount)
                    val winners = seats.filter { score.forPlayer(it) == best }
                    val ended = GameEnded(game.gameId, score, winners)
                    for (uid in gameSubs[game.gameId].orEmpty()) {
                        sessions[uid]?.send?.invoke(ended)
                    }
                }
            }
            is ServerGame.MoveResult.Rejected ->
                session.send(MoveRejected(game.gameId, game.state.value.moveCount, result.reason))
        }
    }
}

/** Wires a single `/ws` WebSocket endpoint that consumes [GameServer]. */
fun Route.gameRoutes(server: GameServer) {
    webSocket("/ws") {
        val user = server.anonymousUser()
        val session = GameServer.Session(user) { msg ->
            send(Frame.Text(server.json.encodeToString(ServerMessage.serializer(), msg)))
        }
        server.register(session)
        try {
            session.send(Welcome(user.userId, user.displayName))
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val text = frame.readText()
                val msg = runCatching {
                    server.json.decodeFromString(ClientMessage.serializer(), text)
                }.getOrNull()
                if (msg == null) {
                    session.send(ServerError("BAD_FRAME", "could not decode: ${text.take(120)}"))
                    continue
                }
                server.handleMessage(session, msg)
            }
        } finally {
            server.unregister(user.userId)
        }
    }
}
