package com.nodimobilebanking.data.api

import android.content.Context
import android.content.Intent
import com.nodimobilebanking.utils.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.*

/**
 * SECURITY-FIRST Authenticator.
 *
 * When the server returns 401 (token expired or invalid) we:
 *   1. Clear stored tokens immediately.
 *   2. Broadcast ACTION_FORCE_LOGOUT so MainActivity kicks back to LoginActivity.
 *   3. Return null (abort the request — user MUST re-authenticate).
 *
 * We intentionally do NOT silently refresh. A banking app must not let an
 * expired session continue operating; the user must prove identity again
 * (PIN or OTP) after any token expiry.
 */
class TokenAuthenticator(
    private val context: Context,
    private val tokenManager: TokenManager
) : Authenticator {

    companion object {
        const val ACTION_FORCE_LOGOUT = "com.nodimobilebanking.FORCE_LOGOUT"
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath

        // Auth endpoints handle their own errors — never retry them here
        if (path.contains("auth/login") || path.contains("auth/onboard")) return null

        // Any 401 on a protected endpoint → force logout
        triggerLogout()
        return null
    }

    private fun triggerLogout() {
        runBlocking { tokenManager.clearTokens() }
        context.sendBroadcast(Intent(ACTION_FORCE_LOGOUT).apply {
            setPackage(context.packageName)
        })
    }
}
