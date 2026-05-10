package territories.app.ui.screens.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import territories.engine.model.Player

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    onGameOver:     (winner: String, scoreA: Int, scoreB: Int, scoreC: Int, scoreD: Int, moves: Int) -> Unit,
    onNavigateHome: () -> Unit,
    vm: GameViewModel = hiltViewModel()
) {
    val stateOrNull  by vm.gameState.collectAsState()
    val isAiThinking by vm.isAiThinking.collectAsState()
    val colorBlindMode by vm.colorBlindMode.collectAsState()

    // Show nothing until first state is available
    val state = stateOrNull ?: return

    var showSurrenderDialog by remember { mutableStateOf(false) }
    var showQuitDialog      by remember { mutableStateOf(false) }

    // Intercept hardware back → ask if user wants to quit
    BackHandler { showQuitDialog = true }

    // Navigate to result when game is over (use LaunchedEffect to avoid calling during recomposition)
    LaunchedEffect(state.isGameOver) {
        if (state.isGameOver) {
            val winner = when (state.winner) {
                Player.A -> "Blue"
                Player.B -> "Red"
                Player.C -> "Green"
                Player.D -> "Yellow"
                else     -> "Draw"
            }
            onGameOver(winner, state.score.playerA, state.score.playerB, state.score.playerC, state.score.playerD, state.moveCount)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ─── Top HUD ─────────────────────────────────────────────
        TopAppBar(
            title = {
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment   = Alignment.CenterVertically
                ) {
                    PlayerScore(
                        label  = "Blue",
                        score  = state.score.playerA,
                        color  = PlayerAColor,
                        active = state.currentPlayer == Player.A && !state.isGameOver
                    )
                    if (state.players.size >= 3) {
                        PlayerScore(
                            label  = "Green",
                            score  = state.score.playerC,
                            color  = PlayerCColor,
                            active = state.currentPlayer == Player.C && !state.isGameOver
                        )
                    }
                    TurnIndicator(
                        currentPlayer = state.currentPlayer,
                        isAiThinking  = isAiThinking,
                        isGameOver    = state.isGameOver
                    )
                    if (state.players.size >= 4) {
                        PlayerScore(
                            label  = "Yellow",
                            score  = state.score.playerD,
                            color  = PlayerDColor,
                            active = state.currentPlayer == Player.D && !state.isGameOver
                        )
                    }
                    PlayerScore(
                        label  = "Red",
                        score  = state.score.playerB,
                        color  = PlayerBColor,
                        active = state.currentPlayer == Player.B && !state.isGameOver
                    )
                }
            },
            actions = {
                IconButton(onClick = { showQuitDialog = true }) {
                    Icon(Icons.Default.Home, contentDescription = "Home")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        // ─── Board ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            BoardCanvas(
                state        = state,
                onCoordClick = { coord ->
                    if (!state.isGameOver && !isAiThinking) vm.onCellTapped(coord)
                },
                modifier = Modifier.fillMaxSize(),
                colorBlindMode = colorBlindMode
            )

            if (isAiThinking) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                }
            }
        }

        // ─── Bottom controls ─────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick  = { vm.onUndo() },
                enabled  = vm.canUndo() && !isAiThinking && !state.isGameOver,
                modifier = Modifier.weight(1f)
            ) { Text("↩ Undo") }

            Button(
                onClick  = { showSurrenderDialog = true },
                enabled  = !state.isGameOver,
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.weight(1f)
            ) { Text("✖ Surrender") }
        }
    }

    // ─── Surrender confirmation ───────────────────────────────────
    if (showSurrenderDialog) {
        AlertDialog(
            onDismissRequest = { showSurrenderDialog = false },
            title            = { Text("Surrender?") },
            text             = { Text("Are you sure you want to concede this game?") },
            confirmButton    = {
                Button(
                    onClick = {
                        showSurrenderDialog = false
                        vm.onSurrender()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Surrender") }
            },
            dismissButton    = {
                TextButton(onClick = { showSurrenderDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ─── Quit confirmation ────────────────────────────────────────
    if (showQuitDialog) {
        AlertDialog(
            onDismissRequest = { showQuitDialog = false },
            title            = { Text("Leave game?") },
            text             = { Text("Your current game will be lost.") },
            confirmButton    = {
                Button(onClick = {
                    showQuitDialog = false
                    onNavigateHome()
                }) { Text("Leave") }
            },
            dismissButton    = {
                TextButton(onClick = { showQuitDialog = false }) { Text("Stay") }
            }
        )
    }
}

@Composable
private fun PlayerScore(label: String, score: Int, color: Color, active: Boolean) {
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 400),
        label = "score-$label"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label score $score${if (active) ", current turn" else ""}"
        }
    ) {
        Text(
            text  = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = androidx.compose.ui.unit.TextUnit(0.08f, androidx.compose.ui.unit.TextUnitType.Em)
            ),
            color = color.copy(alpha = if (active) 1f else 0.75f)
        )
        Text(
            text       = animatedScore.toString(),
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color      = if (active) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun TurnIndicator(currentPlayer: Player, isAiThinking: Boolean, isGameOver: Boolean) {
    val text = when {
        isGameOver    -> "Game Over"
        isAiThinking  -> "AI thinking…"
        currentPlayer == Player.A -> "Blue's turn"
        currentPlayer == Player.B -> "Red's turn"
        currentPlayer == Player.C -> "Green's turn"
        currentPlayer == Player.D -> "Yellow's turn"
        else -> "—"
    }
    val color = when {
        isGameOver -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        currentPlayer == Player.A && !isAiThinking -> PlayerAColor
        currentPlayer == Player.B && !isAiThinking -> PlayerBColor
        currentPlayer == Player.C && !isAiThinking -> PlayerCColor
        currentPlayer == Player.D && !isAiThinking -> PlayerDColor
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
    }
    Text(
        text  = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium,
        color = color,
        modifier = Modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = text
        }
    )
}
