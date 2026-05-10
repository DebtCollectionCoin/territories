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
    onStart: (GameConfig, Map<Player, PlayerType>) -> Unit,
    onResume: () -> Unit = {}
) {
    var boardSize   by remember { mutableStateOf("medium") }
    var scoring     by remember { mutableStateOf("territory") }
    var playerCount by remember { mutableStateOf(2) }
    var typeA       by remember { mutableStateOf("human") }
    var typeB       by remember { mutableStateOf("medium") }
    var typeC       by remember { mutableStateOf("human") }
    var typeD       by remember { mutableStateOf("human") }
    var firstPlayer by remember { mutableStateOf("a") }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            Card(modifier = Modifier.width(420.dp)) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        label = "Players",
                        selected = playerCount.toString(),
                        options = listOf(
                            "2" to "2 — head-to-head",
                            "3" to "3 — free-for-all",
                            "4" to "4 — free-for-all"
                        )
                    ) { playerCount = it.toInt() }

                    // For 2-player keep the original "Opponent" label and let
                    // Player A default to Human; for FFA expose all seats.
                    if (playerCount == 2) {
                        SetupSelect(
                            label = "Opponent (Red)",
                            selected = typeB,
                            options = opponentOptions(includeAi = true)
                        ) { typeB = it }
                    } else {
                        SeatRow("Player A (Blue)", typeA, includeAi = false) { typeA = it }
                        SeatRow("Player B (Red)", typeB, includeAi = false) { typeB = it }
                        SeatRow("Player C (Green)", typeC, includeAi = false) { typeC = it }
                        if (playerCount == 4) {
                            SeatRow("Player D (Yellow)", typeD, includeAi = false) { typeD = it }
                        }
                        Text(
                            "FFA mode currently supports Human or Easy-AI seats. " +
                                "Medium / Hard AI for free-for-all is in progress.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    SetupSelect(
                        label = "First Player",
                        selected = firstPlayer,
                        options = firstPlayerOptions(playerCount)
                    ) { firstPlayer = it }

                    Button(
                        onClick = {
                            val cfg = buildConfig(
                                boardSize, scoring, firstPlayer, playerCount,
                                typeA, typeB, typeC, typeD
                            )
                            val types = buildTypes(playerCount, typeA, typeB, typeC, typeD)
                            onStart(cfg, types)
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

@Composable
private fun SeatRow(
    label: String,
    selected: String,
    includeAi: Boolean,
    onSelect: (String) -> Unit
) {
    SetupSelect(
        label = label,
        selected = selected,
        options = opponentOptions(includeAi = includeAi),
        onSelect = onSelect
    )
}

private fun opponentOptions(includeAi: Boolean): List<Pair<String, String>> =
    if (includeAi) {
        listOf(
            "human"  to "Human (pass-and-play)",
            "easy"   to "AI – Easy",
            "medium" to "AI – Medium",
            "hard"   to "AI – Hard"
        )
    } else {
        // FFA mode: Medium and Hard are not yet supported beyond 2 seats.
        listOf(
            "human" to "Human (pass-and-play)",
            "easy"  to "AI – Easy"
        )
    }

private fun firstPlayerOptions(playerCount: Int): List<Pair<String, String>> {
    val base = mutableListOf(
        "a" to "Player A (Blue)",
        "b" to "Player B (Red)"
    )
    if (playerCount >= 3) base += "c" to "Player C (Green)"
    if (playerCount >= 4) base += "d" to "Player D (Yellow)"
    base += "random" to "Random"
    return base
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

private fun buildConfig(
    boardSize: String,
    scoring: String,
    firstPlayer: String,
    playerCount: Int,
    typeA: String,
    typeB: String,
    typeC: String,
    typeD: String
): GameConfig {
    val preset = when (boardSize) {
        "small" -> GameConfig.SMALL
        "large" -> GameConfig.LARGE
        else    -> GameConfig.MEDIUM
    }
    val variant = if (scoring == "captured") ScoringVariant.CAPTURED_DOTS else ScoringVariant.TERRITORY_AREA
    val seats = listOf(Player.A, Player.B, Player.C, Player.D).take(playerCount)
    val fp = when (firstPlayer) {
        "b"      -> Player.B
        "c"      -> if (Player.C in seats) Player.C else Player.A
        "d"      -> if (Player.D in seats) Player.D else Player.A
        "random" -> seats.random()
        else     -> Player.A
    }
    return preset.copy(
        scoringVariant = variant,
        firstPlayer = fp,
        playerCount = playerCount,
        playerAType = parseOpponent(typeA),
        playerBType = parseOpponent(typeB),
        playerCType = parseOpponent(typeC),
        playerDType = parseOpponent(typeD)
    )
}

private fun buildTypes(
    playerCount: Int,
    typeA: String, typeB: String, typeC: String, typeD: String
): Map<Player, PlayerType> {
    val m = mutableMapOf<Player, PlayerType>(
        Player.A to parseOpponent(typeA),
        Player.B to parseOpponent(typeB)
    )
    if (playerCount >= 3) m[Player.C] = parseOpponent(typeC)
    if (playerCount >= 4) m[Player.D] = parseOpponent(typeD)
    return m
}

private fun parseOpponent(value: String): PlayerType = when (value) {
    "easy"  -> PlayerType.AI_EASY
    "hard"  -> PlayerType.AI_HARD
    "human" -> PlayerType.HUMAN
    "medium"-> PlayerType.AI_MEDIUM
    else    -> PlayerType.HUMAN
}
