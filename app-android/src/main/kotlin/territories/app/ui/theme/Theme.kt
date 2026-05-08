package territories.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PlayerAColor          = Color(0xFF1565C0)
val PlayerAColorTerritory = Color(0x401565C0)
val PlayerBColor          = Color(0xFFC62828)
val PlayerBColorTerritory = Color(0x40C62828)
val GridLineColor         = Color(0xFF9E9E9E)
val BoardBackground       = Color(0xFFFAF8F3)
val BoardBackgroundDark   = Color(0xFF1A1A1A)
val LastMoveColor         = Color(0xFFFFEB3B)

private val DarkColorScheme = darkColorScheme(
    primary   = PlayerAColor,
    secondary = PlayerBColor,
    surface   = Color(0xFF1E1E1E),
    background = Color(0xFF121212),
    onSurface = Color(0xFFE0E0E0),
    onBackground = Color(0xFFE0E0E0),
)

private val LightColorScheme = lightColorScheme(
    primary   = PlayerAColor,
    secondary = PlayerBColor,
    surface   = Color(0xFFF5F5F5),
    background = Color(0xFFFFFFFF),
)

@Composable
fun TerritoriesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content     = content
    )
}