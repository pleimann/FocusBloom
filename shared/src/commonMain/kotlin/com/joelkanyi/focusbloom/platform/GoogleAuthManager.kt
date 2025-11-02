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

data class AuthResult(
    val success: Boolean,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val email: String? = null,
    val error: String? = null,
)

expect class GoogleAuthManager {
    suspend fun signIn(): AuthResult
    suspend fun signOut()
    fun getAccessToken(): String?
    fun getUserEmail(): String?
    fun isSignedIn(): Boolean
    suspend fun refreshToken(): String?
}
