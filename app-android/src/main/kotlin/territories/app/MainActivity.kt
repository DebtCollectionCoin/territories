package territories.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import territories.app.data.ThemeMode
import territories.app.ui.AppViewModel
import territories.app.ui.navigation.AppNavGraph
import territories.app.ui.theme.TerritoriesTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by appViewModel.themeMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val useDark = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT  -> false
                ThemeMode.DARK   -> true
            }
            TerritoriesTheme(darkTheme = useDark) {
                AppNavGraph()
            }
        }
    }
}
