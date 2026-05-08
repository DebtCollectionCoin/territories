package territories.app.ui.screens.history

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import territories.app.data.GameRepository
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    val finishedGames: Flow<List<GameSummary>> = repository.observeFinishedGames().map { games ->
        games.map { entity ->
            GameSummary(
                id          = entity.id,
                winner      = when (entity.winner) {
                    "A"    -> "A"
                    "B"    -> "B"
                    else   -> "Draw"
                },
                finalScoreA = entity.finalScoreA,
                finalScoreB = entity.finalScoreB,
                totalMoves  = entity.totalMoves,
                finishedAt  = entity.finishedAt
            )
        }
    }
}
