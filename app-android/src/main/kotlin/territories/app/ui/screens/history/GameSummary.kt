package territories.app.ui.screens.history

data class GameSummary(
    val id: String,
    val winner: String,          // "A", "B", or "Draw"
    val finalScoreA: Int,
    val finalScoreB: Int,
    val totalMoves: Int,
    val finishedAt: Long?
)
