package territories.server

import io.ktor.client.plugins.websocket.ClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import territories.engine.model.Coord
import territories.engine.model.GameConfig
import territories.engine.model.ScoringVariant
import territories.protocol.ClientMessage
import territories.protocol.CreateLobby
import territories.protocol.GameEnded
import territories.protocol.GameStarted
import territories.protocol.JoinLobby
import territories.protocol.LobbyState
import territories.protocol.MoveApplied
import territories.protocol.Resign
import territories.protocol.ServerMessage
import territories.protocol.SubmitMove
import territories.protocol.Welcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end test: two clients connect, join a 2-player lobby on a tiny
 * board, play moves through `GameStarted`, then one resigns and the
 * server pushes `GameEnded` to both.
 */
class TwoPlayerFlowTest {

    private val json = Json { ignoreUnknownKeys = true; classDiscriminator = "type" }
    private fun encode(msg: ClientMessage) = json.encodeToString(ClientMessage.serializer(), msg)
    private fun decode(text: String) = json.decodeFromString(ServerMessage.serializer(), text)

    private suspend fun ClientWebSocketSession.next(): ServerMessage =
        decode((incoming.receive() as Frame.Text).readText())

    private suspend fun ClientWebSocketSession.nextOfType(predicate: (ServerMessage) -> Boolean): ServerMessage {
        repeat(20) {
            val m = next()
            if (predicate(m)) return m
        }
        error("expected message not received within 20 frames")
    }

    @Test
    fun `two clients play moves then host resigns and both see GameEnded`() = testApplication {
        application { module() }
        val client = createClient { install(ClientWebSockets) }

        val lobbyOpened = CompletableDeferred<String>()
        val gameStartedForA = CompletableDeferred<GameStarted>()

        coroutineScope {
            val a = async {
                client.webSocket("/ws") {
                    val welcome = next() as Welcome
                    assertEquals(true, welcome.userId.startsWith("u-"))
                    val cfg = GameConfig(
                        rows = 3, cols = 3, playerCount = 2,
                        scoringVariant = ScoringVariant.TERRITORY_AREA
                    )
                    send(Frame.Text(encode(CreateLobby(cfg))))
                    // first LobbyState (host alone)
                    val lobby = nextOfType { it is LobbyState } as LobbyState
                    lobbyOpened.complete(lobby.lobbyId)

                    // wait until GameStarted (after B joins)
                    val started = nextOfType { it is GameStarted } as GameStarted
                    gameStartedForA.complete(started)

                    // play A at (0,0)
                    send(Frame.Text(encode(SubmitMove(started.gameId, Coord(0, 0), 0))))
                    val a1 = nextOfType { it is MoveApplied } as MoveApplied
                    assertEquals(1, a1.newState.moveCount)

                    // wait for B's move (move 2)
                    val b1 = nextOfType { it is MoveApplied && it.newState.moveCount == 2 } as MoveApplied

                    // A resigns
                    send(Frame.Text(encode(Resign(started.gameId))))
                    val ended = withTimeout(5_000) {
                        nextOfType { it is GameEnded } as GameEnded
                    }
                    assertEquals(started.gameId, ended.gameId)
                }
            }

            val b = async {
                val lobbyId = lobbyOpened.await()
                client.webSocket("/ws") {
                    val welcome = next() as Welcome
                    assertEquals(true, welcome.userId.startsWith("u-"))
                    send(Frame.Text(encode(JoinLobby(lobbyId))))

                    val started = nextOfType { it is GameStarted } as GameStarted

                    // wait until A has moved (server forwards MoveApplied to all)
                    nextOfType { it is MoveApplied && it.newState.moveCount == 1 }

                    // B plays at (2,2)
                    send(Frame.Text(encode(SubmitMove(started.gameId, Coord(2, 2), 1))))
                    nextOfType { it is MoveApplied && it.newState.moveCount == 2 }

                    // wait for GameEnded triggered by A's resignation
                    val ended = withTimeout(5_000) {
                        nextOfType { it is GameEnded } as GameEnded
                    }
                    assertEquals(started.gameId, ended.gameId)
                }
            }

            a.await()
            b.await()
            assertEquals(2, gameStartedForA.await().seatOrder.size)
        }
    }
}
