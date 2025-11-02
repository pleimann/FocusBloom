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
package com.joelkanyi.focusbloom.feature.settings.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.joelkanyi.focusbloom.core.domain.model.CalendarInfo
import com.joelkanyi.focusbloom.core.domain.model.CalendarSyncSettings
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Composable
fun CalendarSyncSettingCard(
    isConnected: Boolean,
    email: String?,
    syncSettings: CalendarSyncSettings,
    availableCalendars: List<CalendarInfo>,
    lastSyncTime: Long?,
    isSyncing: Boolean,
    syncError: String?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onToggleCalendar: (String) -> Unit,
    onManualSync: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onUpdateSettings: (CalendarSyncSettings) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Google Calendar",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Google Calendar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (isConnected && !isSyncing) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Connected",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Connection Status
            if (!isConnected) {
                Text(
                    text = "Sync your Google Calendar to see events alongside tasks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Connect Google Calendar")
                }
            } else {
                // Connected UI
                email?.let {
                    Text(
                        text = "Connected as: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Auto Sync Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Auto Sync",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        checked = syncSettings.autoSyncEnabled,
                        onCheckedChange = onToggleAutoSync,
                    )
                }

                // Last Sync Time
                lastSyncTime?.let { timestamp ->
                    val timeAgo = getTimeAgo(timestamp)
                    Text(
                        text = "Last synced: $timeAgo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Manual Sync Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onManualSync,
                        enabled = !isSyncing,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Sync",
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        }
                        Text(if (isSyncing) "Syncing..." else "Sync Now")
                    }

                    TextButton(
                        onClick = onDisconnect,
                        enabled = !isSyncing,
                    ) {
                        Text("Disconnect")
                    }
                }

                // Sync Progress
                if (isSyncing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Error Display
                syncError?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        IconButton(onClick = onClearError) {
                            Text("×", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }

                // Calendar Selection
                AnimatedVisibility(visible = availableCalendars.isNotEmpty()) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Select Calendars to Sync",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        availableCalendars.forEach { calendar ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = calendar.isSelected,
                                    onCheckedChange = { onToggleCalendar(calendar.id) },
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(
                                        text = calendar.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    calendar.description?.let { desc ->
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun getTimeAgo(timestamp: Long): String {
    val now = Clock.System.now()
    val then = Instant.fromEpochMilliseconds(timestamp)
    val duration = now - then

    return when {
        duration < 1.milliseconds * 60 * 1000 -> "Just now"
        duration < 1.milliseconds * 60 * 60 * 1000 -> "${duration.inWholeMinutes}m ago"
        duration < 1.milliseconds * 24 * 60 * 60 * 1000 -> "${duration.inWholeHours}h ago"
        else -> "${duration.inWholeDays}d ago"
    }
}
