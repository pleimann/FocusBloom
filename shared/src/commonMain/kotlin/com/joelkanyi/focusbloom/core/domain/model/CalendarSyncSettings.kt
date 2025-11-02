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
package com.joelkanyi.focusbloom.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CalendarSyncSettings(
    val isEnabled: Boolean = false,
    val autoSyncEnabled: Boolean = true,
    val selectedCalendarIds: List<String> = emptyList(),
    val syncDaysAhead: Int = 30,
    val syncDaysBehind: Int = 7,
    val includeAllDayEvents: Boolean = true,
    val includeRecurringEvents: Boolean = true,
    val syncIntervalMinutes: Int = 60,
)
