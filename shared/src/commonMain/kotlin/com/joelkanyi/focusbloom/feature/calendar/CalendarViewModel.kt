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
@file:OptIn(ExperimentalTime::class)

package com.joelkanyi.focusbloom.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joelkanyi.focusbloom.core.domain.model.CalendarEvent
import com.joelkanyi.focusbloom.core.domain.model.Task
import com.joelkanyi.focusbloom.core.domain.repository.calendar.GoogleCalendarRepository
import com.joelkanyi.focusbloom.core.domain.repository.settings.SettingsRepository
import com.joelkanyi.focusbloom.core.domain.repository.tasks.TasksRepository
import com.joelkanyi.focusbloom.core.utils.plusDays
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

sealed class TimelineItem {
    data class TaskItem(val task: Task) : TimelineItem()
    data class EventItem(val event: CalendarEvent) : TimelineItem()
}

class CalendarViewModel(
    private val tasksRepository: TasksRepository,
    private val googleCalendarRepository: GoogleCalendarRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _selectedDay = MutableStateFlow(
        Clock.System.now().toLocalDateTime(
            TimeZone.currentSystemDefault(),
        ).date,
    )
    val selectedDay = _selectedDay.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Clock.System.now().toLocalDateTime(
            TimeZone.currentSystemDefault(),
        ).date,
    )

    fun setSelectedDay(date: kotlinx.datetime.LocalDate) {
        _selectedDay.value = date
    }

    val tasks = tasksRepository.getTasks()
        .map { tasks ->
            tasks.sortedByDescending {
                it.date
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList(),
        )

    // Calendar Events
    val calendarEvents = googleCalendarRepository.getCalendarEvents()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList(),
        )

    // Combined timeline of tasks and calendar events for the selected day
    val timelineItems = combine(
        tasks,
        calendarEvents,
        selectedDay,
    ) { taskList, eventList, day ->
        val dayTasks = taskList.filter { task ->
            task.date.date == day
        }.map { TimelineItem.TaskItem(it) }

        val dayEvents = eventList.filter { event ->
            event.startTime.date == day
        }.map { TimelineItem.EventItem(it) }

        // Combine and sort by start time
        (dayTasks + dayEvents).sortedBy { item ->
            when (item) {
                is TimelineItem.TaskItem -> item.task.start
                is TimelineItem.EventItem -> item.event.startTime
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = emptyList(),
    )

    val hourFormat = settingsRepository.getHourFormat()
        .map { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val sessionTime = settingsRepository.getSessionTime()
        .map {
            it
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )
    val shortBreakTime = settingsRepository.getShortBreakTime()
        .map { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )
    val longBreakTime = settingsRepository.getLongBreakTime()
        .map { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private val _selectedTask = MutableStateFlow<Task?>(null)
    val selectedTask = _selectedTask.asStateFlow()
    fun selectTask(task: Task?) {
        _selectedTask.value = task
    }

    private val _openBottomSheet = MutableStateFlow(false)
    val openBottomSheet = _openBottomSheet.asStateFlow()
    fun openBottomSheet(value: Boolean) {
        _openBottomSheet.value = value
    }

    fun pushToTomorrow(task: Task) {
        viewModelScope.launch {
            tasksRepository.updateTask(
                task.copy(
                    date = task.date.plusDays(1),
                    start = task.start.plusDays(1),
                ),
            )
        }
    }

    fun markAsCompleted(task: Task) {
        viewModelScope.launch {
            tasksRepository.updateTaskCompleted(
                id = task.id,
                completed = true,
            )
            tasksRepository.updateTaskActive(
                id = task.id,
                active = false,
            )
            tasksRepository.updateTaskInProgress(
                id = task.id,
                inProgressTask = false,
            )
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            tasksRepository.deleteTask(task.id)
        }
    }

    // Google Calendar integration
    val isGoogleCalendarConnected = googleCalendarRepository.isAuthenticated()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false,
        )
}
