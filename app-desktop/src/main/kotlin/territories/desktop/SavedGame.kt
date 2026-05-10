package territories.desktop

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import territories.engine.model.GameConfig
import territories.engine.model.Move
import territories.engine.model.PlayerType
import java.io.File

/**
 * Persists in-progress game data to a JSON file under the user-data dir
 * so the desktop app can resume after closing the window. Mirrors the
 * web app's [SavedGame] schema (see app-web/.../SavedGame.kt) so the
 * two persistence layers can converge later if we add cross-device sync.
 */
@Serializable
data class SavedGame(
    val config: GameConfig,
    val playerAType: PlayerType,
    val playerBType: PlayerType,
    val moves: List<Move>,
    val playerCType: PlayerType = PlayerType.HUMAN,
    val playerDType: PlayerType = PlayerType.HUMAN
)

object SavedGameStore {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    /**
     * Resolve the OS-appropriate user-data directory for our app and
     * return the path to the save file inside it. Creates the directory
     * if it does not exist.
     */
    private fun saveFile(): File {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        val home = System.getProperty("user.home").orEmpty()
        val dir = when {
            os.contains("win") -> {
                val appData = System.getenv("APPDATA") ?: "$home/AppData/Roaming"
                File(appData, "Territories")
            }
            os.contains("mac") -> File(home, "Library/Application Support/Territories")
            else -> {
                val xdg = System.getenv("XDG_DATA_HOME")
                if (!xdg.isNullOrBlank()) File(xdg, "territories")
                else File(home, ".local/share/territories")
            }
        }
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "saved-game.v1.json")
    }

    fun load(): SavedGame? {
        val f = saveFile()
        if (!f.exists()) return null
        return try {
            json.decodeFromString(SavedGame.serializer(), f.readText())
        } catch (_: Throwable) {
            // Corrupt or schema-mismatched save — discard and start fresh.
            try { f.delete() } catch (_: Throwable) { /* ignore */ }
            null
        }
    }

    fun save(game: SavedGame) {
        try {
            saveFile().writeText(json.encodeToString(game))
        } catch (_: Throwable) {
            // Disk full / permissions — silently fail; user can keep playing.
        }
    }

    fun clear() {
        try { saveFile().delete() } catch (_: Throwable) { /* ignore */ }
    }

    fun hasSaved(): Boolean = saveFile().exists()
}
