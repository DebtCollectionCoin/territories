package territories.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import territories.engine.model.GameState
import territories.engine.model.Player

@Composable
fun GameWindow(viewModel: GameViewModel) {
    val gameState    by viewModel.gameState.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    val showSetup    by viewModel.showSetup.collectAsState()

    var showResult by remember { mutableStateOf(false) }

    LaunchedEffect(gameState?.isGameOver) {
        if (gameState?.isGameOver == true) showResult = true
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {

            Column(Modifier.fillMaxSize()) {
                // ── HUD bar ──────────────────────────────────────────
                HudBar(
                    gameState    = gameState,
                    isAiThinking = isAiThinking,
                    canUndo      = viewModel.canUndo(),
                    onUndo       = { viewModel.undo() },
                    onSurrender  = { viewModel.surrender() },
                    onMenu       = { viewModel.openSetup() }
                )

                // ── Board area ────────────────────────────────────────
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    gameState?.let { state ->
                        BoardCanvas(
                            state        = state,
                            onCoordClick = { coord -> viewModel.humanMove(coord) },
                            modifier     = Modifier.fillMaxSize()
                        )
                    }
                    if (isAiThinking) {
                        Text(
                            "AI thinking…",
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                            color    = AppColors.OnSurfaceDim,
                            style    = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // ── Overlays ─────────────────────────────────────────────
            if (showSetup) {
                SetupDialog(
                    showResume = viewModel.hasSavedGame() ||
                                 (gameState != null && gameState?.isGameOver == false),
                    onStart    = { config, types ->
                        viewModel.closeSetup()
                        showResult = false
                        viewModel.startGame(config, types)
                    },
                    onResume   = {
                        // If a fresh launch with a save on disk, load it.
                        // Otherwise the existing in-memory game continues.
                        if (gameState == null && viewModel.hasSavedGame()) {
                            viewModel.resumeSavedGame()
                        }
                        viewModel.closeSetup()
                    }
                )
            }

            if (showResult && !showSetup) {
                ResultOverlay(
                    gameState   = gameState,
                    onPlayAgain = { showResult = false; viewModel.openSetup() },
                    onNewGame   = { showResult = false; viewModel.openSetup() }
                )
            }
        }
    }
}

// ── HUD bar ──────────────────────────────────────────────────────────────────

@Composable
fun HudBar(
    gameState:    GameState?,
    isAiThinking: Boolean,
    canUndo:      Boolean,
    onUndo:       () -> Unit,
    onSurrender:  () -> Unit,
    onMenu:       () -> Unit
) {
    Row(
        modifier             = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left score panels: Players A and (in 4-player) C
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ScorePanel(
                label  = "Blue",
                score  = gameState?.score?.playerA ?: 0,
                color  = AppColors.PlayerA,
                active = gameState?.let { it.currentPlayer == Player.A && !it.isGameOver } ?: false
            )
            if ((gameState?.players?.size ?: 0) >= 3) {
                ScorePanel(
                    label  = "Green",
                    score  = gameState?.score?.playerC ?: 0,
                    color  = AppColors.PlayerC,
                    active = gameState?.let { it.currentPlayer == Player.C && !it.isGameOver } ?: false
                )
            }
        }

        // Centre: menu / turn label / undo / surrender
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenu) {
                Text("☰", color = AppColors.OnSurface, style = MaterialTheme.typography.titleMedium)
            }

            val seatLabel = mapOf(
                Player.A to "Blue", Player.B to "Red",
                Player.C to "Green", Player.D to "Yellow"
            )
            val turnText = when {
                gameState == null           -> "Start a game"
                gameState.isGameOver        -> "Game Over"
                isAiThinking                -> "AI thinking…"
                else -> "${seatLabel[gameState.currentPlayer] ?: ""}'s turn"
            }
            val turnColor = when {
                gameState == null || gameState.isGameOver -> AppColors.OnSurfaceDim
                isAiThinking                              -> AppColors.OnSurfaceDim
                else -> AppColors.forPlayer(gameState.currentPlayer)
            }
            Text(
                text     = turnText,
                color    = turnColor,
                style    = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            IconButton(onClick = onUndo, enabled = canUndo) {
                Text("↩", color = if (canUndo) AppColors.OnSurface else AppColors.OnSurfaceDim,
                     style = MaterialTheme.typography.titleMedium)
            }
            IconButton(
                onClick  = onSurrender,
                enabled  = gameState != null && !gameState.isGameOver
            ) {
                Text("✖", color = AppColors.OnSurface, style = MaterialTheme.typography.titleMedium)
            }
        }

        // Right score panels: Players B and (in 4-player) D
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ScorePanel(
                label  = "Red",
                score  = gameState?.score?.playerB ?: 0,
                color  = AppColors.PlayerB,
                active = gameState?.let { it.currentPlayer == Player.B && !it.isGameOver } ?: false
            )
            if ((gameState?.players?.size ?: 0) >= 4) {
                ScorePanel(
                    label  = "Yellow",
                    score  = gameState?.score?.playerD ?: 0,
                    color  = AppColors.PlayerD,
                    active = gameState?.let { it.currentPlayer == Player.D && !it.isGameOver } ?: false
                )
            }
        }
    }
}

@Composable
private fun ScorePanel(label: String, score: Int, color: Color, active: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text  = label.uppercase(),
            color = color.copy(alpha = if (active) 1f else 0.7f),
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text  = score.toString(),
            color = if (active) color else AppColors.OnSurface,
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

// ── Result overlay ────────────────────────────────────────────────────────────

@Composable
fun ResultOverlay(
    gameState:   GameState?,
    onPlayAgain: () -> Unit,
    onNewGame:   () -> Unit
) {
    Box(
        modifier        = Modifier.fillMaxSize().background(Color(0xAA000000)),
        contentAlignment = Alignment.Center
    ) {
        Card(modifier = Modifier.width(340.dp)) {
            Column(
                modifier              = Modifier.padding(32.dp),
                horizontalAlignment   = Alignment.CenterHorizontally,
                verticalArrangement   = Arrangement.spacedBy(16.dp)
            ) {
                Text("Game Over", style = MaterialTheme.typography.headlineMedium)

                val resultText = gameState?.let { s ->
                    val seatLabel = mapOf(
                        Player.A to "🔵 Blue", Player.B to "🔴 Red",
                        Player.C to "🟢 Green", Player.D to "🟡 Yellow"
                    )
                    val scores = s.players.joinToString(" – ") { p ->
                        s.score.forPlayer(p).toString()
                    }
                    val winnerLabel = seatLabel[s.winner]
                    if (winnerLabel != null) "$winnerLabel wins!\n$scores"
                    else "Draw!\n$scores"
                } ?: ""
                Text(resultText, style = MaterialTheme.typography.bodyLarge)

                Button(
                    onClick  = onPlayAgain,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Play Again") }

                OutlinedButton(
                    onClick  = onNewGame,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("New Game") }
            }
        }
    }
}
