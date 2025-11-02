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
@file:OptIn(ExperimentalTime::class)

package com.joelkanyi.focusbloom.core.data.repository.calendar

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.joelkanyi.focusbloom.core.data.mapper.toCalendarEvent
import com.joelkanyi.focusbloom.core.domain.model.CalendarEvent
import com.joelkanyi.focusbloom.core.domain.model.CalendarInfo
import com.joelkanyi.focusbloom.core.domain.model.CalendarSyncSettings
import com.joelkanyi.focusbloom.core.domain.model.SyncStatus
import com.joelkanyi.focusbloom.core.domain.repository.calendar.GoogleCalendarRepository
import com.joelkanyi.focusbloom.core.domain.repository.settings.SettingsRepository
import com.joelkanyi.focusbloom.database.BloomDatabase
import com.joelkanyi.focusbloom.platform.GoogleAuthManager
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class GoogleCalendarRepositoryImpl(
    private val bloomDatabase: BloomDatabase,
    private val googleAuthManager: GoogleAuthManager,
    private val settingsRepository: SettingsRepository,
) : GoogleCalendarRepository {

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                },
            )
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Napier.d(message, tag = "GoogleCalendarAPI")
                }
            }
            level = LogLevel.INFO
        }
        install(Auth) {
            bearer {
                loadTokens {
                    googleAuthManager.getAccessToken()?.let {
                        BearerTokens(it, "")
                    }
                }
                refreshTokens {
                    googleAuthManager.refreshToken()?.let {
                        BearerTokens(it, "")
                    }
                }
            }
        }
    }

    private val _isAuthenticated = MutableStateFlow(false)
    private val _userEmail = MutableStateFlow<String?>(null)

    init {
        _isAuthenticated.value = googleAuthManager.isSignedIn()
        _userEmail.value = googleAuthManager.getUserEmail()
    }

    override fun isAuthenticated(): Flow<Boolean> = _isAuthenticated

    override fun getUserEmail(): Flow<String?> = _userEmail

    override suspend fun signIn(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val authResult = googleAuthManager.signIn()
            if (authResult.success && authResult.accessToken != null) {
                _isAuthenticated.value = true
                _userEmail.value = authResult.email
                // Save email to settings
                authResult.email?.let {
                    settingsRepository.saveGoogleCalendarEmail(it)
                }
                Result.success(authResult.accessToken)
            } else {
                Result.failure(Exception(authResult.error ?: "Authentication failed"))
            }
        } catch (e: Exception) {
            Napier.e("Sign in failed", e)
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Unit = withContext(Dispatchers.IO) {
        try {
            googleAuthManager.signOut()
            _isAuthenticated.value = false
            _userEmail.value = null
            clearCachedEvents()
        } catch (e: Exception) {
            Napier.e("Sign out failed", e)
        }
    }

    override suspend fun fetchAvailableCalendars(): Result<List<CalendarInfo>> =
        withContext(Dispatchers.IO) {
            try {
                val response: GoogleCalendarListResponse = httpClient.get(
                    "https://www.googleapis.com/calendar/v3/users/me/calendarList",
                ).body()

                val calendars = response.items.map { item ->
                    CalendarInfo(
                        id = item.id,
                        name = item.summary,
                        description = item.description,
                        colorHex = item.backgroundColor,
                        isPrimary = item.primary ?: false,
                        isSelected = false,
                    )
                }
                Result.success(calendars)
            } catch (e: Exception) {
                Napier.e("Failed to fetch calendars", e)
                Result.failure(e)
            }
        }

    @OptIn(ExperimentalTime::class)
    override suspend fun syncCalendarEvents(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val settings = getSyncSettings().map { it }.let {
                // Get current settings synchronously (for simplicity, you might want to refactor this)
                CalendarSyncSettings()
            }

            val timeZone = TimeZone.currentSystemDefault()
            val now = Clock.System.now()
            val timeMin = now.minus(
                kotlin.time.Duration.parse("${settings.syncDaysBehind}d"),
            )
            val timeMax = now.plus(kotlin.time.Duration.parse("${settings.syncDaysAhead}d"))

            for (calendarId in settings.selectedCalendarIds) {
                syncCalendarById(
                    calendarId,
                    timeMin.toString(),
                    timeMax.toString(),
                    settings,
                )
            }

            // Update last sync time
            settingsRepository.saveLastSyncTime(Clock.System.now().toEpochMilliseconds())

            // Clean up expired events
            cleanupExpiredEvents()

            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Calendar sync failed", e)
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun syncCalendarById(
        calendarId: String,
        timeMin: String,
        timeMax: String,
        settings: CalendarSyncSettings,
    ) {
        try {
            val response: GoogleEventsResponse = httpClient.get(
                "https://www.googleapis.com/calendar/v3/calendars/$calendarId/events",
            ) {
                parameter("timeMin", timeMin)
                parameter("timeMax", timeMax)
                parameter("singleEvents", "true")
                parameter("orderBy", "startTime")
            }.body()

            val currentTime = Clock.System.now().toEpochMilliseconds()

            response.items.forEach { event ->
                // Filter based on settings
                if (!settings.includeRecurringEvents && event.recurringEventId != null) return@forEach
                if (!settings.includeAllDayEvents && event.start.date != null) return@forEach

                val startDateTime = event.start.dateTime?.let { LocalDateTime.parse(it) }
                    ?: event.start.date?.let { LocalDate.parse(it).atStartOfDayIn(TimeZone.currentSystemDefault()).toLocalDateTime(TimeZone.currentSystemDefault()) }
                    ?: return@forEach

                val endDateTime = event.end.dateTime?.let { LocalDateTime.parse(it) }
                    ?: event.end.date?.let { LocalDate.parse(it).atStartOfDayIn(TimeZone.currentSystemDefault()).toLocalDateTime(TimeZone.currentSystemDefault()) }
                    ?: return@forEach

                bloomDatabase.calendarEventQueries.insertCalendarEvent(
                    googleEventId = event.id,
                    calendarId = calendarId,
                    summary = event.summary ?: "Untitled Event",
                    description = event.description,
                    startTime = startDateTime,
                    endTime = endDateTime,
                    color = event.colorId,
                    location = event.location,
                    isAllDay = event.start.date != null,
                    isRecurring = event.recurringEventId != null,
                    syncStatus = SyncStatus.SYNCED,
                    lastSyncedAt = currentTime,
                )
            }
        } catch (e: Exception) {
            Napier.e("Failed to sync calendar $calendarId", e)
        }
    }

    override fun getCalendarEvents(): Flow<List<CalendarEvent>> =
        bloomDatabase.calendarEventQueries
            .getAllCalendarEvents()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { events ->
                events.map { it.toCalendarEvent() }
            }

    override fun getEventsForDate(date: LocalDate): Flow<List<CalendarEvent>> =
        bloomDatabase.calendarEventQueries
            .getEventsForDay(date.toString())
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { events ->
                events.map { it.toCalendarEvent() }
            }

    override fun getEventsByDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<CalendarEvent>> {
        val startDateTime = startDate.atStartOfDayIn(TimeZone.currentSystemDefault())
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val endDateTime = endDate.atStartOfDayIn(TimeZone.currentSystemDefault())
            .toLocalDateTime(TimeZone.currentSystemDefault())

        return bloomDatabase.calendarEventQueries
            .getEventsByDateRange(
                startDateTime,
                endDateTime,
            )
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { events ->
                events.map { it.toCalendarEvent() }
            }
    }

    override fun getSyncSettings(): Flow<CalendarSyncSettings> =
        settingsRepository.getCalendarSyncSettings()

    override suspend fun updateSyncSettings(settings: CalendarSyncSettings) {
        settingsRepository.saveCalendarSyncSettings(settings)
    }

    override fun getLastSyncTime(): Flow<Long?> =
        settingsRepository.getLastSyncTime()

    override suspend fun clearCachedEvents(): Unit = withContext(Dispatchers.IO) {
        bloomDatabase.calendarEventQueries.deleteAllCalendarEvents()
    }

    override suspend fun cleanupExpiredEvents(): Unit = withContext(Dispatchers.IO) {
        val expirationTime = Clock.System.now()
            .minus(kotlin.time.Duration.parse("24h"))
            .toEpochMilliseconds()

        bloomDatabase.calendarEventQueries.markEventsAsExpired(expirationTime)
        bloomDatabase.calendarEventQueries.deleteExpiredEvents()
    }
}

// Google Calendar API Response Models
@Serializable
private data class GoogleCalendarListResponse(
    val items: List<GoogleCalendarItem>,
)

@Serializable
private data class GoogleCalendarItem(
    val id: String,
    val summary: String,
    val description: String? = null,
    val backgroundColor: String? = null,
    val primary: Boolean? = null,
)

@Serializable
private data class GoogleEventsResponse(
    val items: List<GoogleEventItem>,
)

@Serializable
private data class GoogleEventItem(
    val id: String,
    val summary: String? = null,
    val description: String? = null,
    val location: String? = null,
    val colorId: String? = null,
    val start: EventDateTime,
    val end: EventDateTime,
    val recurringEventId: String? = null,
)

@Serializable
private data class EventDateTime(
    @SerialName("dateTime") val dateTime: String? = null,
    @SerialName("date") val date: String? = null,
)
