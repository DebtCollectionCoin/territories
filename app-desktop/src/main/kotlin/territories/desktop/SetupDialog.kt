package territories.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import territories.engine.model.GameConfig
import territories.engine.model.Player
import territories.engine.model.PlayerType
import territories.engine.model.ScoringVariant

@Composable
fun SetupDialog(
    showResume: Boolean = false,
    onStart: (GameConfig, PlayerType) -> Unit,
    onResume: () -> Unit = {}
) {
    var boardSize   by remember { mutableStateOf("medium") }
    var scoring     by remember { mutableStateOf("territory") }
    var opponent    by remember { mutableStateOf("medium") }
    var firstPlayer by remember { mutableStateOf("a") }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            Card(modifier = Modifier.width(400.dp)) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Territories",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    SetupSelect(
                        label = "Board Size",
                        selected = boardSize,
                        options = listOf(
                            "small"  to "Small (21 × 16)",
                            "medium" to "Medium (31 × 21)",
                            "large"  to "Large (61 × 41)"
                        )
                    ) { boardSize = it }

                    SetupSelect(
                        label = "Scoring",
                        selected = scoring,
                        options = listOf(
                            "territory" to "Territory Area",
                            "captured"  to "Captured Dots"
                        )
                    ) { scoring = it }

                    SetupSelect(
                        label = "Opponent",
                        selected = opponent,
                        options = listOf(
                            "human"  to "Human (pass-and-play)",
                            "easy"   to "AI – Easy",
                            "medium" to "AI – Medium",
                            "hard"   to "AI – Hard"
                        )
                    ) { opponent = it }

                    SetupSelect(
                        label = "First Player",
                        selected = firstPlayer,
                        options = listOf(
                            "a"      to "Player A (Blue)",
                            "b"      to "Player B (Red)",
                            "random" to "Random"
                        )
                    ) { firstPlayer = it }

                    Button(
                        onClick = {
                            onStart(
                                buildConfig(boardSize, scoring, firstPlayer),
                                parseOpponent(opponent)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Start Game") }

                    if (showResume) {
                        OutlinedButton(
                            onClick = onResume,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Resume Current Game") }
                    }

                    Text(
                        "Shortcuts: Ctrl+Z undo · Ctrl+N new game",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupSelect(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: selected

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (value, displayLabel) ->
                    DropdownMenuItem(
                        text = { Text(displayLabel) },
                        onClick = { onSelect(value); expanded = false }
                    )
                }
            }
        }
    }
}

private fun buildConfig(boardSize: String, scoring: String, firstPlayer: String): GameConfig {
    val preset = when (boardSize) {
        "small" -> GameConfig.SMALL
        "large" -> GameConfig.LARGE
        else    -> GameConfig.MEDIUM
    }
    val variant = if (scoring == "captured") ScoringVariant.CAPTURED_DOTS else ScoringVariant.TERRITORY_AREA
    val fp = when (firstPlayer) {
        "b"      -> Player.B
        "random" -> if ((0..1).random() == 0) Player.A else Player.B
        else     -> Player.A
    }
    return preset.copy(scoringVariant = variant, firstPlayer = fp)
}

private fun parseOpponent(value: String): PlayerType = when (value) {
    "easy"  -> PlayerType.AI_EASY
    "hard"  -> PlayerType.AI_HARD
    "human" -> PlayerType.HUMAN
    else    -> PlayerType.AI_MEDIUM
}
