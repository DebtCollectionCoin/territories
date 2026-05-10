package territories.net

import io.ktor.client.engine.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import territories.engine.model.Coord
import territories.engine.model.GameConfig
import territories.engine.model.ScoringVariant
import territories.protocol.CreateLobby
import territories.protocol.GameStarted
import territories.protocol.JoinLobby
import territories.protocol.LobbyState
import territories.protocol.MoveApplied
import territories.protocol.SubmitMove
import territories.protocol.Welcome
import territories.server.module
import java.net.ServerSocket
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end: real Netty server + real CIO client over TCP. Validates
 * the JSON wire protocol and the Ktor plumbing on both sides.
 */
class GameClientRoundTripTest {

    private lateinit var server: io.ktor.server.engine.EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>
    private var port: Int = 0

    @BeforeTest
    fun start() {
        port = freePort()
        server = embeddedServer(Netty, port = port, host = "127.0.0.1") { module() }
        server.start(wait = false)
    }

    @AfterTest
    fun stop() {
        server.stop(500, 1_000)
    }

    private fun freePort(): Int = ServerSocket(0).use { it.localPort }

    @Test
    fun `two clients create-join lobby and exchange a move over real TCP`() = runBlocking {
        val http = buildHttpClient(CIO)
        val a = GameClient(http, host = "127.0.0.1", port = port)
        val b = GameClient(http, host = "127.0.0.1", port = port)
        try {
            a.connect()
            b.connect()

            assertTrue(withTimeout(3_000) { a.incoming.first { true } } is Welcome)
            assertTrue(withTimeout(3_000) { b.incoming.first { true } } is Welcome)

            val cfg = GameConfig(
                rows = 3, cols = 3, playerCount = 2,
                scoringVariant = ScoringVariant.TERRITORY_AREA
            )
            a.send(CreateLobby(cfg))
            val lobby = withTimeout(3_000) { a.incoming.first { it is LobbyState } } as LobbyState

            b.send(JoinLobby(lobby.lobbyId))

            val startedA = withTimeout(3_000) { a.incoming.first { it is GameStarted } } as GameStarted
            val startedB = withTimeout(3_000) { b.incoming.first { it is GameStarted } } as GameStarted
            assertEquals(startedA.gameId, startedB.gameId)

            a.send(SubmitMove(startedA.gameId, Coord(0, 0), 0))
            val appliedA = withTimeout(3_000) { a.incoming.first { it is MoveApplied } } as MoveApplied
            val appliedB = withTimeout(3_000) { b.incoming.first { it is MoveApplied } } as MoveApplied
            assertEquals(1, appliedA.newState.moveCount)
            assertEquals(1, appliedB.newState.moveCount)
        } finally {
            a.close(); b.close(); http.close()
        }
    }
}
