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
