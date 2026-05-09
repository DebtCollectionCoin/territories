package territories.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import territories.app.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val themeMode by vm.themeMode.collectAsState()
    val colorBlindMode by vm.colorBlindMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader("Appearance")

            ThemeSelector(
                current = themeMode,
                onSelect = { vm.onThemeChanged(it) }
            )

            HorizontalDivider()

            SectionHeader("Accessibility")

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Color-blind mode", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Renders Red player as squares so colors aren't the only cue",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = colorBlindMode,
                    onCheckedChange = { vm.onColorBlindModeChanged(it) }
                )
            }

            HorizontalDivider()

            SectionHeader("About")
            Text("Territories", style = MaterialTheme.typography.titleMedium)
            Text("Version 1.0.0", style = MaterialTheme.typography.bodyMedium)
            Text(
                "A digital adaptation of the Czech pen-and-paper game Židi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ThemeSelector(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Theme", style = MaterialTheme.typography.labelLarge)
        ThemeMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = mode == current,
                        onClick  = { onSelect(mode) },
                        role     = Role.RadioButton
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = mode == current,
                    onClick = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when (mode) {
                        ThemeMode.SYSTEM -> "System default"
                        ThemeMode.LIGHT  -> "Light"
                        ThemeMode.DARK   -> "Dark"
                    }
                )
            }
        }
    }
}
