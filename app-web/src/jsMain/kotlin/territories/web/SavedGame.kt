package territories.web

import kotlinx.browser.window
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import territories.engine.model.GameConfig
import territories.engine.model.Move
import territories.engine.model.Player
import territories.engine.model.PlayerType

/**
 * Persists in-progress game data into localStorage so a player can resume
 * after closing the browser/tab.
 *
 * Strategy: store the [GameConfig] + the list of [Move]s applied so far.
 * To restore, we replay the moves on a fresh engine.
 */
@Serializable
data class SavedGame(
    val config: GameConfig,
    val playerAType: PlayerType,
    val playerBType: PlayerType,
    val moves: List<Move>
)

object SavedGameStore {
    private const val KEY = "territories.saved-game.v1"
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): SavedGame? {
        val raw = window.localStorage.getItem(KEY) ?: return null
        return try {
            json.decodeFromString(SavedGame.serializer(), raw)
        } catch (_: Throwable) {
            window.localStorage.removeItem(KEY)
            null
        }
    }

    fun save(game: SavedGame) {
        try {
            window.localStorage.setItem(KEY, json.encodeToString(game))
        } catch (_: Throwable) { /* quota — ignore */ }
    }

    fun clear() {
        window.localStorage.removeItem(KEY)
    }

    fun hasSaved(): Boolean = window.localStorage.getItem(KEY) != null

    /** Encode a SavedGame as a URL-safe base64 string suitable for `#g=` fragment. */
    fun encodeForShare(game: SavedGame): String {
        val raw = json.encodeToString(SavedGame.serializer(), game)
        return urlSafeBase64Encode(raw)
    }

    /** Decode a SavedGame from the `#g=` fragment, or null on any failure. */
    fun decodeFromShare(token: String): SavedGame? = try {
        val raw = urlSafeBase64Decode(token)
        json.decodeFromString(SavedGame.serializer(), raw)
    } catch (_: Throwable) {
        null
    }

    /** Build a shareable URL pointing at the current page with the given saved game. */
    fun buildShareUrl(game: SavedGame): String {
        val loc = window.location
        val origin = loc.origin
        val path = loc.pathname
        return "$origin$path#g=${encodeForShare(game)}"
    }

    /** Read a SavedGame from `window.location.hash` if present (and clear the fragment). */
    fun consumeFragment(): SavedGame? {
        val hash = window.location.hash
        val prefix = "#g="
        if (!hash.startsWith(prefix)) return null
        val token = hash.substring(prefix.length)
        val saved = decodeFromShare(token) ?: return null
        // Strip fragment so a refresh doesn't replay the share again
        try {
            window.history.replaceState(null, "", window.location.pathname + window.location.search)
        } catch (_: Throwable) { /* ignore */ }
        return saved
    }
}

private fun urlSafeBase64Encode(s: String): String {
    // Encode UTF-8 → btoa expects binary string; use encodeURIComponent trick.
    val utf8 = js("unescape(encodeURIComponent(s))").unsafeCast<String>()
    val b64 = js("btoa(utf8)").unsafeCast<String>()
    return b64.replace('+', '-').replace('/', '_').trimEnd('=')
}

private fun urlSafeBase64Decode(s: String): String {
    var b64 = s.replace('-', '+').replace('_', '/')
    while (b64.length % 4 != 0) b64 += "="
    val raw = js("atob(b64)").unsafeCast<String>()
    return js("decodeURIComponent(escape(raw))").unsafeCast<String>()
}

/** Generic settings persisted in localStorage. */
object Settings {
    private const val K_SOUND = "territories.settings.sound"
    private const val K_PULSE = "territories.settings.pulse"

    var soundEnabled: Boolean
        get() = window.localStorage.getItem(K_SOUND) != "false"
        set(v) { window.localStorage.setItem(K_SOUND, v.toString()) }

    var pulseEnabled: Boolean
        get() = window.localStorage.getItem(K_PULSE) != "false"
        set(v) { window.localStorage.setItem(K_PULSE, v.toString()) }
}
