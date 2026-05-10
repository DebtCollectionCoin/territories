package territories.app.ui.screens.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onBack:  () -> Unit,
    onStart: () -> Unit,
    vm:      SetupViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title         = { Text("New Game") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            SettingSection(label = "Board Size", description = "Larger boards take longer to fill") {
                ChoiceGrid(
                    options  = listOf(
                        Choice("Small",  "21×16",  "small"),
                        Choice("Medium", "31×21",  "medium"),
                        Choice("Large",  "61×41",  "large"),
                    ),
                    selected = vm.boardSize,
                    onSelect = { vm.boardSize = it }
                )
            }

            SettingSection(label = "Scoring", description = "Choose how the winner is decided") {
                ChoiceGrid(
                    options  = listOf(
                        Choice("Territory Area", "Largest enclosed region wins", "territory"),
                        Choice("Captured Dots",  "Most opponent dots captured wins", "captured"),
                    ),
                    selected = vm.scoring,
                    onSelect = { vm.scoring = it }
                )
            }

            SettingSection(label = "Players") {
                ChoiceGrid(
                    options = listOf(
                        Choice("2", "Head-to-head", "2"),
                        Choice("3", "Free-for-all", "3"),
                        Choice("4", "Free-for-all", "4"),
                    ),
                    selected = vm.players,
                    onSelect = { vm.players = it }
                )
            }

            if (vm.players == "2") {
                SettingSection(label = "Opponent") {
                    ChoiceGrid(
                        options = listOf(
                            Choice("Human",   "Pass and play",         "human"),
                            Choice("AI Easy", "Random legal moves",    "easy"),
                            Choice("AI Med",  "Looks 1 move ahead",    "medium"),
                            Choice("AI Hard", "Looks several ahead",   "hard"),
                        ),
                        selected = vm.opponent,
                        onSelect = { vm.opponent = it }
                    )
                }
            } else {
                SettingSection(
                    label = "Seats",
                    description = "Medium / Hard AI for free-for-all is in progress"
                ) {
                    SeatRow("Blue",   vm.seatA) { vm.seatA = it }
                    Spacer(Modifier.height(8.dp))
                    SeatRow("Red",    vm.seatB) { vm.seatB = it }
                    Spacer(Modifier.height(8.dp))
                    SeatRow("Green",  vm.seatC) { vm.seatC = it }
                    if (vm.players == "4") {
                        Spacer(Modifier.height(8.dp))
                        SeatRow("Yellow", vm.seatD) { vm.seatD = it }
                    }
                }
            }

            SettingSection(label = "First Player") {
                val firstOptions = mutableListOf(
                    Choice("Blue",   "Starts (A)",    "a"),
                    Choice("Red",    "Starts (B)",    "b"),
                )
                val count = vm.players.toIntOrNull() ?: 2
                if (count >= 3) firstOptions.add(Choice("Green",  "Starts (C)",    "c"))
                if (count >= 4) firstOptions.add(Choice("Yellow", "Starts (D)",    "d"))
                firstOptions.add(Choice("Random", "Pick a seat",   "random"))
                ChoiceGrid(
                    options  = firstOptions,
                    selected = vm.firstPlayer,
                    onSelect = { vm.firstPlayer = it }
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = { vm.applyConfig(); onStart() },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Start Game", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingSection(
    label:       String,
    description: String? = null,
    content:     @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text  = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        if (description != null) {
            Text(
                text  = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        content()
    }
}

private data class Choice(val title: String, val subtitle: String, val value: String)

@Composable
private fun ChoiceGrid(
    options:  List<Choice>,
    selected: String,
    onSelect: (String) -> Unit
) {
    // 2-column grid via Rows of 2 (avoids LazyVerticalGrid + scroll conflict)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { rowItems ->
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { choice ->
                    Box(modifier = Modifier.weight(1f)) {
                        ChoiceCard(
                            choice    = choice,
                            isSelected = choice.value == selected,
                            onClick   = { onSelect(choice.value) }
                        )
                    }
                }
                if (rowItems.size == 1) {
                    // Spacer to keep grid aligned
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ChoiceCard(
    choice:     Choice,
    isSelected: Boolean,
    onClick:    () -> Unit
) {
    val border = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val bg = if (isSelected)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    else
        Color.Transparent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = border,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text       = choice.title,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color      = if (isSelected) MaterialTheme.colorScheme.primary
                         else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text  = choice.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun SeatRow(
    seatLabel: String,
    selected:  String,
    onSelect:  (String) -> Unit
) {
    Row(
        modifier            = Modifier.fillMaxWidth(),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text       = seatLabel.uppercase(),
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.primary,
            modifier   = Modifier.padding(end = 4.dp)
        )
        Box(modifier = Modifier.weight(1f)) {
            ChoiceCard(
                choice     = Choice("Human", "Pass and play", "human"),
                isSelected = selected == "human",
                onClick    = { onSelect("human") }
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            ChoiceCard(
                choice     = Choice("AI Easy", "Random legal", "easy"),
                isSelected = selected == "easy",
                onClick    = { onSelect("easy") }
            )
        }
    }
}
