/*
 * Copyright 2023 Joel Kanyi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.joelkanyi.focusbloom.feature.settings

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joelkanyi.focusbloom.core.domain.model.CalendarInfo
import com.joelkanyi.focusbloom.core.domain.model.CalendarSyncSettings
import com.joelkanyi.focusbloom.core.domain.repository.calendar.GoogleCalendarRepository
import com.joelkanyi.focusbloom.core.domain.repository.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val googleCalendarRepository: GoogleCalendarRepository,
) : ViewModel() {
    private val _selectedColorCardTitle = MutableStateFlow("")
    val selectedColorCardTitle = _selectedColorCardTitle.asStateFlow()
    fun setSelectedColorCardTitle(title: String) {
        _selectedColorCardTitle.value = title
    }

    val hourFormats: List<String> = listOf("12-hour", "24-hour")

    val optionsOpened = mutableStateListOf("")
    fun openOptions(option: String) {
        if (optionsOpened.contains(option)) {
            optionsOpened.remove(option)
        } else {
            optionsOpened.add(option)
        }
    }

    val appTheme: StateFlow<Int?> = settingsRepository.getAppTheme()
        .map { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null,
        )

    fun setAppTheme(appTheme: Int) {
        viewModelScope.launch {
            settingsRepository.saveAppTheme(appTheme)
        }
    }

    val sessionTime = settingsRepository.getSessionTime()
        .map {
            it
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun setSessionTime(sessionTime: Int) {
        viewModelScope.launch {
            settingsRepository.saveSessionTime(sessionTime)
        }
    }

    val shortBreakTime = settingsRepository.getShortBreakTime()
        .map { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun setShortBreakTime(shortBreakTime: Int) {
        viewModelScope.launch {
            settingsRepository.saveShortBreakTime(shortBreakTime)
        }
    }

    val longBreakTime = settingsRepository.getLongBreakTime()
        .map { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun setLongBreakTime(longBreakTime: Int) {
        viewModelScope.launch {
            settingsRepository.saveLongBreakTime(longBreakTime)
        }
    }

    val timeFormat = settingsRepository.getHourFormat()
        .map { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun setHourFormat(timeFormat: Int) {
        viewModelScope.launch {
            settingsRepository.saveHourFormat(timeFormat)
        }
    }

    fun setShortBreakColor(color: Long) {
        viewModelScope.launch {
            settingsRepository.saveShortBreakColor(color)
        }
    }

    fun setLongBreakColor(color: Long) {
        viewModelScope.launch {
            settingsRepository.saveLongBreakColor(color)
        }
    }

    fun setFocusColor(color: Long) {
        viewModelScope.launch {
            settingsRepository.saveFocusColor(color)
        }
    }

    private val _showColorDialog = MutableStateFlow(false)
    val showColorDialog = _showColorDialog.asStateFlow()
    fun setShowColorDialog(it: Boolean) {
        _showColorDialog.value = it
    }

    val shortBreakColor = settingsRepository.shortBreakColor()
        .map {
            it
        }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null,
        )

    val longBreakColor = settingsRepository.longBreakColor()
        .map { it }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null,
        )

    val focusColor = settingsRepository.focusColor()
        .map { it }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null,
        )

    val remindersOn = settingsRepository.remindersOn()
        .map { it }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false,
        )

    fun setReminders(value: Int) {
        viewModelScope.launch {
            settingsRepository.toggleReminder(value)
        }
    }

    // Google Calendar Sync
    val isGoogleCalendarConnected: StateFlow<Boolean> = googleCalendarRepository.isAuthenticated()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false,
        )

    val googleCalendarEmail: StateFlow<String?> = googleCalendarRepository.getUserEmail()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null,
        )

    val calendarSyncSettings: StateFlow<CalendarSyncSettings> =
        googleCalendarRepository.getSyncSettings()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = CalendarSyncSettings(),
            )

    val lastSyncTime: StateFlow<Long?> = googleCalendarRepository.getLastSyncTime()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null,
        )

    private val _availableCalendars = MutableStateFlow<List<CalendarInfo>>(emptyList())
    val availableCalendars = _availableCalendars.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError = _syncError.asStateFlow()

    fun connectGoogleCalendar() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            val result = googleCalendarRepository.signIn()
            result.onSuccess {
                // Fetch available calendars after successful sign-in
                fetchAvailableCalendars()
            }.onFailure { error ->
                _syncError.value = error.message ?: "Failed to connect to Google Calendar"
            }
            _isSyncing.value = false
        }
    }

    fun disconnectGoogleCalendar() {
        viewModelScope.launch {
            googleCalendarRepository.signOut()
            _availableCalendars.value = emptyList()
        }
    }

    fun fetchAvailableCalendars() {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = googleCalendarRepository.fetchAvailableCalendars()
            result.onSuccess { calendars ->
                // Update calendars with current selections
                val currentSelections = calendarSyncSettings.value.selectedCalendarIds
                _availableCalendars.value = calendars.map { calendar ->
                    calendar.copy(isSelected = currentSelections.contains(calendar.id))
                }
            }.onFailure { error ->
                _syncError.value = error.message ?: "Failed to fetch calendars"
            }
            _isSyncing.value = false
        }
    }

    fun toggleCalendarSelection(calendarId: String) {
        val updatedCalendars = _availableCalendars.value.map { calendar ->
            if (calendar.id == calendarId) {
                calendar.copy(isSelected = !calendar.isSelected)
            } else {
                calendar
            }
        }
        _availableCalendars.value = updatedCalendars

        // Update settings with selected calendar IDs
        val selectedIds = updatedCalendars.filter { it.isSelected }.map { it.id }
        updateCalendarSyncSettings(calendarSyncSettings.value.copy(selectedCalendarIds = selectedIds))
    }

    fun updateCalendarSyncSettings(settings: CalendarSyncSettings) {
        viewModelScope.launch {
            googleCalendarRepository.updateSyncSettings(settings)
        }
    }

    fun manualSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            val result = googleCalendarRepository.syncCalendarEvents()
            result.onFailure { error ->
                _syncError.value = error.message ?: "Sync failed"
            }
            _isSyncing.value = false
        }
    }

    fun clearSyncError() {
        _syncError.value = null
    }
}
