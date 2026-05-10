package territories.server

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

/**
 * Ktor entry point. Phase D scope: single-process server with
 * in-memory lobby/game registry. Run via `./gradlew :server:run`.
 */
fun main() {
    embeddedServer(Netty, port = 9000, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    install(CallLogging)
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(WebSockets) {
        pingPeriod = 20.seconds
        timeout    = 30.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    val registry = GameRegistry()
    val gameServer = GameServer(registry)

    routing {
        get("/") {
            call.respondText("territories server up")
        }
        get("/healthz") {
            call.respondText("ok")
        }
        gameRoutes(gameServer)
    }
}
