package territories.net

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import territories.protocol.ClientMessage
import territories.protocol.ServerMessage

/**
 * Thin WebSocket client for the territories game server.
 *
 * Pure KMP — uses [HttpClient] with the platform-appropriate engine
 * (CIO on JVM, JS on web). Callers receive [ServerMessage]s via the
 * [incoming] flow and push [ClientMessage]s via [send].
 *
 * Construction does not connect; call [connect] then [send] / collect.
 */
class GameClient(
    private val httpClient: HttpClient,
    private val host: String,
    private val port: Int,
    private val secure: Boolean = false,
    private val path: String = "/ws",
) {
    private val json: Json = Json { ignoreUnknownKeys = true; classDiscriminator = "type" }

    private val channel = Channel<ServerMessage>(capacity = Channel.UNLIMITED)

    /**
     * Stream of server messages. Backed by an unlimited [Channel]
     * so messages received before a collector subscribes are not
     * lost. The flow can be collected multiple times (callers may
     * use `incoming.first { ... }` repeatedly to drain a single
     * sequence of frames).
     */
    val incoming: Flow<ServerMessage> = channel.receiveAsFlow()

    private var session: io.ktor.client.plugins.websocket.DefaultClientWebSocketSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var readerJob: Job? = null

    suspend fun connect() {
        val s = httpClient.webSocketSession {
            url {
                protocol = if (secure) URLProtocol.WSS else URLProtocol.WS
                this.host = this@GameClient.host
                this.port = this@GameClient.port
                this.path(this@GameClient.path)
            }
        }
        session = s
        readerJob = scope.launch {
            for (frame in s.incoming) {
                if (frame !is Frame.Text) continue
                val text = frame.readText()
                val msg = runCatching {
                    json.decodeFromString(ServerMessage.serializer(), text)
                }.getOrNull() ?: continue
                channel.send(msg)
            }
        }
    }

    suspend fun send(msg: ClientMessage) {
        val s = session ?: error("not connected")
        s.send(Frame.Text(json.encodeToString(ClientMessage.serializer(), msg)))
    }

    suspend fun close() {
        readerJob?.cancel()
        session?.close()
        session = null
        channel.close()
        scope.cancel()
    }
}

/** Convenience: construct a JVM/Android-friendly [HttpClient] with the WebSockets plugin installed. */
fun <T : HttpClientEngineFactory<*>> buildHttpClient(
    engine: T,
    block: HttpClientConfig<*>.() -> Unit = {},
): HttpClient = HttpClient(engine) {
    install(WebSockets)
    block()
}
