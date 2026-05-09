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
    val viewModel = remember { GameViewModel() }
    DisposableEffect(Unit) {
        onDispose { viewModel.dispose() }
    }
    Window(
        onCloseRequest = ::exitApplication,
        title          = "Territories",
        state          = WindowState(width = 1100.dp, height = 750.dp),
        onKeyEvent     = event@{ e ->
            if (e.type != KeyEventType.KeyDown) return@event false
            if (e.isCtrlPressed && e.key == Key.Z) {
                if (viewModel.canUndo()) {
                    viewModel.undo()
                    return@event true
                }
            }
            false
        }
    ) {
        GameWindow(viewModel = viewModel)
    }
}
