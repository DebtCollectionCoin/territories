package territories.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import territories.app.data.AppPreferencesRepository
import territories.app.data.ThemeMode
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferencesRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = prefs.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ThemeMode.SYSTEM
    )

    fun onThemeChanged(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }
}
