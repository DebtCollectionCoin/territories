package territories.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import territories.app.data.AppPreferencesRepository
import territories.app.data.ThemeMode
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    prefs: AppPreferencesRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = prefs.themeMode.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.Eagerly,
        initialValue = ThemeMode.SYSTEM
    )

    val colorBlindMode: StateFlow<Boolean> = prefs.colorBlindMode.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.Eagerly,
        initialValue = false
    )
}
