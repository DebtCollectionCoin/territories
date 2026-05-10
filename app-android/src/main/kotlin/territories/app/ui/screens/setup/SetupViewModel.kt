package territories.app.ui.screens.setup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import territories.app.data.GameConfigHolder
import territories.engine.model.GameConfig
import territories.engine.model.Player
import territories.engine.model.PlayerType
import territories.engine.model.ScoringVariant
import javax.inject.Inject

private val KEY_BOARD_SIZE   = stringPreferencesKey("board_size")
private val KEY_SCORING      = stringPreferencesKey("scoring")
private val KEY_OPPONENT     = stringPreferencesKey("opponent")
private val KEY_FIRST_PLAYER = stringPreferencesKey("first_player")
private val KEY_PLAYERS      = stringPreferencesKey("players")
private val KEY_SEAT_A       = stringPreferencesKey("seat_a")
private val KEY_SEAT_B       = stringPreferencesKey("seat_b")
private val KEY_SEAT_C       = stringPreferencesKey("seat_c")
private val KEY_SEAT_D       = stringPreferencesKey("seat_d")

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val configHolder: GameConfigHolder,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    var boardSize   by mutableStateOf("medium")
    var scoring     by mutableStateOf("territory")
    var opponent    by mutableStateOf("medium")
    var firstPlayer by mutableStateOf("a")
    var players     by mutableStateOf("2")
    var seatA       by mutableStateOf("human")
    var seatB       by mutableStateOf("human")
    var seatC       by mutableStateOf("human")
    var seatD       by mutableStateOf("human")

    init {
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            boardSize   = prefs[KEY_BOARD_SIZE]   ?: "medium"
            scoring     = prefs[KEY_SCORING]      ?: "territory"
            opponent    = prefs[KEY_OPPONENT]     ?: "medium"
            firstPlayer = prefs[KEY_FIRST_PLAYER] ?: "a"
            players     = prefs[KEY_PLAYERS]      ?: "2"
            seatA       = prefs[KEY_SEAT_A]       ?: "human"
            seatB       = prefs[KEY_SEAT_B]       ?: "human"
            seatC       = prefs[KEY_SEAT_C]       ?: "human"
            seatD       = prefs[KEY_SEAT_D]       ?: "human"
        }
    }

    fun applyConfig() {
        val (cols, rows) = when (boardSize) {
            "small" -> 21 to 16
            "large" -> 61 to 41
            else    -> 31 to 21
        }
        val scoringVariant = if (scoring == "territory")
            ScoringVariant.TERRITORY_AREA else ScoringVariant.CAPTURED_DOTS

        val playerCount = players.toIntOrNull()?.coerceIn(2, 4) ?: 2
        val seats = listOf(Player.A, Player.B, Player.C, Player.D).take(playerCount)

        val (aType, bType, cType, dType) = if (playerCount == 2) {
            val bt = when (opponent) {
                "easy"  -> PlayerType.AI_EASY
                "hard"  -> PlayerType.AI_HARD
                "human" -> PlayerType.HUMAN
                else    -> PlayerType.AI_MEDIUM
            }
            arrayOf(PlayerType.HUMAN, bt, PlayerType.HUMAN, PlayerType.HUMAN)
        } else {
            arrayOf(parseSeat(seatA), parseSeat(seatB), parseSeat(seatC), parseSeat(seatD))
        }

        val fp = when (firstPlayer) {
            "b"      -> Player.B
            "c"      -> if (Player.C in seats) Player.C else Player.A
            "d"      -> if (Player.D in seats) Player.D else Player.A
            "random" -> seats.random()
            else     -> Player.A
        }
        configHolder.current = GameConfig(
            cols          = cols,
            rows          = rows,
            scoringVariant = scoringVariant,
            firstPlayer   = fp,
            playerCount   = playerCount,
            playerAType   = aType,
            playerBType   = bType,
            playerCType   = cType,
            playerDType   = dType
        )

        // Persist choices (fire and forget)
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_BOARD_SIZE]   = boardSize
                prefs[KEY_SCORING]      = scoring
                prefs[KEY_OPPONENT]     = opponent
                prefs[KEY_FIRST_PLAYER] = firstPlayer
                prefs[KEY_PLAYERS]      = players
                prefs[KEY_SEAT_A]       = seatA
                prefs[KEY_SEAT_B]       = seatB
                prefs[KEY_SEAT_C]       = seatC
                prefs[KEY_SEAT_D]       = seatD
            }
        }
    }

    private fun parseSeat(value: String): PlayerType = when (value) {
        "easy"   -> PlayerType.AI_EASY
        "medium" -> PlayerType.AI_MEDIUM
        "hard"   -> PlayerType.AI_HARD
        else     -> PlayerType.HUMAN
    }
}
