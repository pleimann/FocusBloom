/*
 * Copyright 2024 Joel Kanyi.
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
package com.joelkanyi.focusbloom.core.domain.repository.calendar

import com.joelkanyi.focusbloom.core.domain.model.CalendarEvent
import com.joelkanyi.focusbloom.core.domain.model.CalendarInfo
import com.joelkanyi.focusbloom.core.domain.model.CalendarSyncSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface GoogleCalendarRepository {
    /**
     * Get authentication status
     */
    fun isAuthenticated(): Flow<Boolean>

    /**
     * Get authenticated user's email
     */
    fun getUserEmail(): Flow<String?>

    /**
     * Authenticate with Google and get access token
     */
    suspend fun signIn(): Result<String>

    /**
     * Sign out and revoke access
     */
    suspend fun signOut()

    /**
     * Get list of available calendars from Google
     */
    suspend fun fetchAvailableCalendars(): Result<List<CalendarInfo>>

    /**
     * Sync calendar events from Google to local database
     */
    suspend fun syncCalendarEvents(): Result<Unit>

    /**
     * Get cached calendar events from local database
     */
    fun getCalendarEvents(): Flow<List<CalendarEvent>>

    /**
     * Get calendar events for a specific date
     */
    fun getEventsForDate(date: LocalDate): Flow<List<CalendarEvent>>

    /**
     * Get calendar events for a date range
     */
    fun getEventsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<CalendarEvent>>

    /**
     * Get sync settings
     */
    fun getSyncSettings(): Flow<CalendarSyncSettings>

    /**
     * Update sync settings
     */
    suspend fun updateSyncSettings(settings: CalendarSyncSettings)

    /**
     * Get last sync timestamp
     */
    fun getLastSyncTime(): Flow<Long?>

    /**
     * Clear all cached calendar events
     */
    suspend fun clearCachedEvents()

    /**
     * Clean up expired events from cache
     */
    suspend fun cleanupExpiredEvents()
}
