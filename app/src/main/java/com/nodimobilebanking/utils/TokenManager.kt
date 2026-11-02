package com.nodimobilebanking.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import android.provider.Settings
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sacco_auth")

class TokenManager(private val context: Context) {

    companion object {
        private val ACCESS_TOKEN_KEY  = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val SAVED_USERNAME = stringPreferencesKey("saved_username")
        private val SAVED_MEMBER_NO = stringPreferencesKey("saved_member_no")
        private val DEVICE_ID = stringPreferencesKey("device_id")
        private val COMPANY_NAME = stringPreferencesKey("company_name")
        private val COMPANY_LOGO_URL = stringPreferencesKey("company_logo_url")
        private val COMPANY_FOOTER = stringPreferencesKey("company_footer")
        private val COMPANY_ADDRESS = stringPreferencesKey("company_address")
        private val COMPANY_CONTACT = stringPreferencesKey("company_contact")
        private val COMPANY_LOCATION = stringPreferencesKey("company_location")
        private val BALANCES_HIDDEN = stringPreferencesKey("balances_hidden")
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY]  = accessToken
            prefs[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    suspend fun getAccessToken(): String? =
        context.dataStore.data.map { it[ACCESS_TOKEN_KEY] }.first()

    suspend fun getRefreshToken(): String? =
        context.dataStore.data.map { it[REFRESH_TOKEN_KEY] }.first()

    suspend fun clearTokens() {
        context.dataStore.edit {
            it.remove(ACCESS_TOKEN_KEY)
            it.remove(REFRESH_TOKEN_KEY)
            // Keep SAVED_USERNAME, SAVED_MEMBER_NO and DEVICE_ID — they persist across logouts
        }
    }

    suspend fun isLoggedIn(): Boolean = !getAccessToken().isNullOrEmpty()

    // ── Username persistence (remember me) ─────────────────────────
    suspend fun saveUsername(username: String) {
        context.dataStore.edit { it[SAVED_USERNAME] = username }
    }

    suspend fun getSavedUsername(): String? =
        context.dataStore.data.first()[SAVED_USERNAME]

    // ── Member number persistence ────────────────────────────────────
    suspend fun saveMemberNo(memberNo: String) {
        context.dataStore.edit { it[SAVED_MEMBER_NO] = memberNo }
    }

    suspend fun getSavedMemberNo(): String? =
        context.dataStore.data.first()[SAVED_MEMBER_NO]

    suspend fun clearSavedMemberNo() {
        context.dataStore.edit { it.remove(SAVED_MEMBER_NO) }
    }

    // ── Device locking ─────────────────────────────────────────────
    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    suspend fun saveDeviceId(deviceId: String) {
        context.dataStore.edit { it[DEVICE_ID] = deviceId }
    }

    suspend fun getSavedDeviceId(): String? =
        context.dataStore.data.first()[DEVICE_ID]

    // ── Company branding cache ────────────────────────────────────
    suspend fun saveCompanyInfo(
        name: String,
        logoUrl: String,
        footer: String,
        address: String = "",
        contact: String = "",
        location: String = "",
    ) {
        context.dataStore.edit {
            it[COMPANY_NAME] = name
            it[COMPANY_LOGO_URL] = logoUrl
            it[COMPANY_FOOTER] = footer
            it[COMPANY_ADDRESS] = address
            it[COMPANY_CONTACT] = contact
            it[COMPANY_LOCATION] = location
        }
    }

    suspend fun getCompanyName(): String =
        context.dataStore.data.first()[COMPANY_NAME] ?: "SACCO MIS"

    suspend fun getCompanyLogoUrl(): String =
        context.dataStore.data.first()[COMPANY_LOGO_URL] ?: ""

    suspend fun getCompanyFooter(): String =
        context.dataStore.data.first()[COMPANY_FOOTER] ?: "Company MIS"

    suspend fun getCompanyAddress(): String =
        context.dataStore.data.first()[COMPANY_ADDRESS] ?: ""

    suspend fun getCompanyContact(): String =
        context.dataStore.data.first()[COMPANY_CONTACT] ?: ""

    suspend fun getCompanyLocation(): String =
        context.dataStore.data.first()[COMPANY_LOCATION] ?: ""

    // ── Balance visibility (persist hide-eye toggle across navigation) ──
    suspend fun saveBalancesHidden(hidden: Boolean) {
        context.dataStore.edit { it[BALANCES_HIDDEN] = if (hidden) "1" else "0" }
    }

    suspend fun getBalancesHidden(): Boolean =
        (context.dataStore.data.first()[BALANCES_HIDDEN] ?: "0") == "1"
}
