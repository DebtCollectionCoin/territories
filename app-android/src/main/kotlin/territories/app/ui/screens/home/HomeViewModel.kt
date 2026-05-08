package territories.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import territories.app.data.GameConfigHolder
import territories.app.data.GameRepository
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: GameRepository,
    private val configHolder: GameConfigHolder
) : ViewModel() {

    private val _resumableGameId = MutableStateFlow<String?>(null)
    val resumableGameId: StateFlow<String?> = _resumableGameId.asStateFlow()

    init { refresh() }

    /** Re-check whether an in-progress game exists. Call when returning to Home. */
    fun refresh() {
        viewModelScope.launch {
            val game = repository.getInProgressGame()
            // Only consider it resumable if it has at least one move recorded
            _resumableGameId.value = if (game != null) {
                val moves = repository.loadMoves(game.id)
                if (moves.isNotEmpty()) game.id else null
            } else null
        }
    }

    /** Mark the resumable game as the next-to-load and clear nothing else. */
    fun prepareResume() {
        configHolder.resumeGameId = _resumableGameId.value
    }

    /** Discard any pending resume id (e.g. when starting a brand new game). */
    fun clearPendingResume() {
        configHolder.resumeGameId = null
    }
}
