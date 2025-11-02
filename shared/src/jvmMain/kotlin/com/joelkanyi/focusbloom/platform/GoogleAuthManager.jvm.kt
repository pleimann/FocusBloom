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

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import io.github.aakira.napier.Napier
import java.awt.Desktop
import java.net.URI

actual class GoogleAuthManager(
    private val settings: Settings,
) {
    private companion object {
        const val KEY_ACCESS_TOKEN = "google_access_token"
        const val KEY_REFRESH_TOKEN = "google_refresh_token"
        const val KEY_USER_EMAIL = "google_user_email"
        const val CLIENT_ID = "" // TODO: Add your Google OAuth 2.0 Client ID here
        const val REDIRECT_URI = "http://localhost:8080/oauth2callback"
    }

    actual suspend fun signIn(): AuthResult {
        return try {
            // For desktop, we'll open a browser for OAuth
            // This is a simplified implementation - a full implementation would:
            // 1. Start a local server to receive the OAuth callback
            // 2. Open the browser to Google's OAuth page
            // 3. Receive the authorization code
            // 4. Exchange it for access tokens

            val authUrl = buildAuthUrl()

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(authUrl))
            }

            // TODO: Implement callback server and token exchange
            // For now, return a placeholder
            AuthResult(
                success = false,
                error = "Desktop OAuth not fully implemented - please use Android or iOS",
            )
        } catch (e: Exception) {
            Napier.e("Desktop sign in failed", e)
            AuthResult(
                success = false,
                error = e.message ?: "Unknown error",
            )
        }
    }

    private fun buildAuthUrl(): String {
        val scope = "https://www.googleapis.com/auth/calendar.readonly"
        return "https://accounts.google.com/o/oauth2/v2/auth?" +
            "client_id=$CLIENT_ID&" +
            "redirect_uri=$REDIRECT_URI&" +
            "response_type=code&" +
            "scope=$scope&" +
            "access_type=offline"
    }

    actual suspend fun signOut() {
        settings.remove(KEY_ACCESS_TOKEN)
        settings.remove(KEY_REFRESH_TOKEN)
        settings.remove(KEY_USER_EMAIL)
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
        // TODO: Implement token refresh for desktop
        return getAccessToken()
    }
}
