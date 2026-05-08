package territories.app.ui.screens.howtoplay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToPlayScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How to Play") },
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
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            RuleSection(title = "Objective") {
                RuleText(
                    "Surround your opponent's dots with a closed ring of your own dots " +
                    "to capture territory. The player with the most territory at the end wins."
                )
            }

            RuleSection(title = "Taking a Turn") {
                RuleText("Each turn, place one dot on any empty intersection of the grid.")
                RuleText("You cannot place a dot inside an already-captured territory.")
            }

            RuleSection(title = "Capturing Territory") {
                RuleText(
                    "When your dots form a closed loop around one or more of your opponent's " +
                    "dots, you capture all dots and empty spaces inside the loop."
                )
                RuleText("A region touching the board border can never be captured.")
                RuleText("You can re-capture territory your opponent has already captured.")
            }

            RuleSection(title = "The Staircase Rule") {
                RuleText(
                    "Dots are connected in 8 directions (including diagonals). " +
                    "A straight diagonal line has gaps — an opponent can slip between dots. " +
                    "Use a staircase pattern instead: each dot overlaps the previous one " +
                    "both horizontally and vertically, creating an unbreakable boundary."
                )
            }

            RuleSection(title = "Scoring") {
                RuleText(
                    "Territory Area — count every intersection inside your captured regions " +
                    "(dots + empty squares). Highest total wins."
                )
                RuleText(
                    "Captured Dots — count only your opponent's dots that you captured. " +
                    "Highest total wins."
                )
            }

            RuleSection(title = "End of Game") {
                RuleText("A player surrenders, OR no legal moves remain on the board.")
            }

            RuleSection(title = "Strategy Tips") {
                RuleText("• Connect your dot chains to the board border — they become uncapturable.")
                RuleText("• Complete encirclements quickly before your opponent can escape.")
                RuleText("• Don't close a territory too early if a better move exists elsewhere.")
                RuleText("• Watch for your opponent building a staircase around your dots.")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RuleSection(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(16.dp))
    Text(
        text       = title,
        style      = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color      = MaterialTheme.colorScheme.primary
    )
    Spacer(Modifier.height(6.dp))
    content()
    Spacer(Modifier.height(8.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun RuleText(text: String) {
    Text(
        text   = text,
        style  = MaterialTheme.typography.bodyMedium,
        color  = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
