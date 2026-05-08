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

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val configHolder: GameConfigHolder,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    var boardSize   by mutableStateOf("medium")
    var scoring     by mutableStateOf("territory")
    var opponent    by mutableStateOf("medium")
    var firstPlayer by mutableStateOf("a")

    init {
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            boardSize   = prefs[KEY_BOARD_SIZE]   ?: "medium"
            scoring     = prefs[KEY_SCORING]      ?: "territory"
            opponent    = prefs[KEY_OPPONENT]     ?: "medium"
            firstPlayer = prefs[KEY_FIRST_PLAYER] ?: "a"
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

        val playerBType = when (opponent) {
            "easy"  -> PlayerType.AI_EASY
            "hard"  -> PlayerType.AI_HARD
            "human" -> PlayerType.HUMAN
            else    -> PlayerType.AI_MEDIUM
        }
        val fp = when (firstPlayer) {
            "b"      -> Player.B
            "random" -> if ((0..1).random() == 0) Player.A else Player.B
            else     -> Player.A
        }
        configHolder.current = GameConfig(
            cols          = cols,
            rows          = rows,
            scoringVariant = scoringVariant,
            firstPlayer   = fp,
            playerAType   = PlayerType.HUMAN,
            playerBType   = playerBType
        )

        // Persist choices (fire and forget)
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_BOARD_SIZE]   = boardSize
                prefs[KEY_SCORING]      = scoring
                prefs[KEY_OPPONENT]     = opponent
                prefs[KEY_FIRST_PLAYER] = firstPlayer
            }
        }
    }
}
