package territories.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title          = "Territories",
        state          = WindowState(width = 1100.dp, height = 750.dp),
        onKeyEvent     = event@{ e ->
            if (e.type == KeyEventType.KeyDown && e.isCtrlPressed && e.key == Key.Z) {
                // Undo is handled inside GameWindow via the ViewModel
                // We return false here and let the focusable composable handle it
                // Alternatively, we expose the viewModel here:
                false
            } else false
        }
    ) {
        val viewModel = remember { GameViewModel() }
        DisposableEffect(Unit) {
            onDispose { viewModel.dispose() }
        }
        GameWindow(viewModel = viewModel)
    }
}
