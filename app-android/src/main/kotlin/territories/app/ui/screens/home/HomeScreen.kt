package territories.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HomeScreen(
    onNewGame:   () -> Unit,
    onHowToPlay: () -> Unit,
    onHistory:   () -> Unit = {},
    onSettings:  () -> Unit = {},
    onResume:    () -> Unit = {},
    vm: HomeViewModel = hiltViewModel()
) {
    val resumableId by vm.resumableGameId.collectAsState()
    LaunchedEffect(Unit) { vm.refresh() }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier              = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            verticalArrangement   = Arrangement.Center,
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            Text(
                text      = "Territories",
                style     = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center
            )
            Text(
                text      = "A game of encirclement",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            Button(
                onClick  = {
                    vm.clearPendingResume()
                    onNewGame()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("New Game", style = MaterialTheme.typography.titleMedium)
            }

            if (resumableId != null) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick  = {
                        vm.prepareResume()
                        onResume()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Resume Game")
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick  = onHowToPlay,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("How to Play")
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick  = onHistory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Game History")
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick  = onSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Settings")
            }
        }
    }
}
