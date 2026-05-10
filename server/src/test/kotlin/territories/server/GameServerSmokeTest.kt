package territories.server

import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import territories.protocol.ServerMessage
import territories.protocol.Welcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameServerSmokeTest {

    private val json = Json { ignoreUnknownKeys = true; classDiscriminator = "type" }

    @Test
    fun `health endpoint returns ok`() = testApplication {
        application { module() }
        val resp = client.get("/healthz")
        assertEquals("ok", resp.bodyAsText())
    }

    @Test
    fun `websocket sends a Welcome on connect`() = testApplication {
        application { module() }
        val client = createClient { install(ClientWebSockets) }
        client.webSocket("/ws") {
            val first = (incoming.receive() as Frame.Text).readText()
            val msg = json.decodeFromString(ServerMessage.serializer(), first)
            assertTrue(msg is Welcome, "expected Welcome, got $msg")
            assertTrue(msg.userId.startsWith("u-"))
        }
    }
}
