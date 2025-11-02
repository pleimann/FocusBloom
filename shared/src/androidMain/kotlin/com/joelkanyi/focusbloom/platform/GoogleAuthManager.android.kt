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
package com.joelkanyi.focusbloom.platform

import android.content.Context
import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier

/**
 * Android implementation of GoogleAuthManager
 *
 * TODO: Implement proper OAuth flow using KMPAuth or Google Sign-In SDK
 * Current implementation is a stub to allow compilation.
 * Follow these steps to enable:
 * 1. Add KMPAuth dependencies if not already added
 * 2. Configure OAuth 2.0 credentials in Google Cloud Console
 * 3. Add the Client ID to the GoogleAuthCredentials
 * 4. Implement the actual sign-in flow using googleAuthProvider.signIn()
 */
actual class GoogleAuthManager(
    private val context: Context,
    private val settings: Settings,
) {
    private companion object {
        const val KEY_ACCESS_TOKEN = "google_access_token"
        const val KEY_REFRESH_TOKEN = "google_refresh_token"
        const val KEY_USER_EMAIL = "google_user_email"
    }

    actual suspend fun signIn(): AuthResult {
        return AuthResult(
            success = false,
            error = "Google Calendar OAuth not yet configured. Please add OAuth Client ID and implement sign-in flow.",
        )
    }

    actual suspend fun signOut() {
        try {
            settings.remove(KEY_ACCESS_TOKEN)
            settings.remove(KEY_REFRESH_TOKEN)
            settings.remove(KEY_USER_EMAIL)
        } catch (e: Exception) {
            Napier.e("Android sign out failed", e)
        }
    }

    actual fun getAccessToken(): String? {
        return settings.getStringOrNull(KEY_ACCESS_TOKEN)
    }

    actual fun getUserEmail(): String? {
        return settings.getStringOrNull(KEY_USER_EMAIL)
    }

    actual fun isSignedIn(): Boolean {
        return getAccessToken() != null
    }

    actual suspend fun refreshToken(): String? {
        // TODO: Implement token refresh using OAuth refresh token
        return getAccessToken()
    }
}
