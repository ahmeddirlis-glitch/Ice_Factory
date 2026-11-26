package com.example.data.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "brohi_ice_prefs")

data class AppPreferences(
    val darkMode: Boolean = false,
    val language: String = "en",
    val rememberMe: Boolean = true,
    val ownerPreviewRole: UserRole? = null,
    val savedEmailHint: String = "",
    val pendingRegistrationJson: String = "",
    val aiAuditEnabled: Boolean = false,
    val aiAuditAutoWatch: Boolean = true,
    val aiAuditExplainLanguage: String = "ur",
    val aiAuditFindingsJson: String = "",
    val heyIceWakeEnabled: Boolean = false,
    val voiceWakeLang: String = "ur-PK"
)

class PreferencesManager(private val context: Context) {

    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        val OWNER_PREVIEW_ROLE = stringPreferencesKey("owner_preview_role")
        val SAVED_EMAIL_HINT = stringPreferencesKey("saved_email_hint")
        val PENDING_REG_JSON = stringPreferencesKey("pending_registration_json")
        val AI_AUDIT_ENABLED = booleanPreferencesKey("ai_audit_enabled")
        val AI_AUDIT_AUTO_WATCH = booleanPreferencesKey("ai_audit_auto_watch")
        val AI_AUDIT_EXPLAIN_LANG = stringPreferencesKey("ai_audit_explain_lang")
        val AI_AUDIT_FINDINGS_JSON = stringPreferencesKey("ai_audit_findings_json")
        val HEY_ICE_WAKE_ENABLED = booleanPreferencesKey("hey_ice_wake_enabled")
        val VOICE_WAKE_LANG = stringPreferencesKey("voice_wake_lang")
    }

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            darkMode = prefs[Keys.DARK_MODE] ?: false,
            language = prefs[Keys.LANGUAGE] ?: "en",
            rememberMe = prefs[Keys.REMEMBER_ME] ?: true,
            ownerPreviewRole = UserRole.fromStringOrNull(prefs[Keys.OWNER_PREVIEW_ROLE]),
            savedEmailHint = prefs[Keys.SAVED_EMAIL_HINT] ?: "",
            pendingRegistrationJson = prefs[Keys.PENDING_REG_JSON] ?: "",
            aiAuditEnabled = prefs[Keys.AI_AUDIT_ENABLED] ?: false,
            aiAuditAutoWatch = prefs[Keys.AI_AUDIT_AUTO_WATCH] ?: true,
            aiAuditExplainLanguage = prefs[Keys.AI_AUDIT_EXPLAIN_LANG] ?: "ur",
            aiAuditFindingsJson = prefs[Keys.AI_AUDIT_FINDINGS_JSON] ?: "",
            heyIceWakeEnabled = prefs[Keys.HEY_ICE_WAKE_ENABLED] ?: false,
            voiceWakeLang = prefs[Keys.VOICE_WAKE_LANG] ?: "ur-PK"
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = if (lang == "ur") "ur" else "en" }
    }

    suspend fun setRememberMe(remember: Boolean) {
        context.dataStore.edit { it[Keys.REMEMBER_ME] = remember }
    }

    suspend fun setOwnerPreviewRole(role: UserRole?) {
        context.dataStore.edit {
            if (role == null || role == UserRole.OWNER) {
                it.remove(Keys.OWNER_PREVIEW_ROLE)
            } else {
                it[Keys.OWNER_PREVIEW_ROLE] = role.label
            }
        }
    }

    suspend fun setSavedEmailHint(email: String) {
        context.dataStore.edit { it[Keys.SAVED_EMAIL_HINT] = email }
    }

    suspend fun setPendingRegistrationJson(json: String) {
        context.dataStore.edit { it[Keys.PENDING_REG_JSON] = json }
    }

    suspend fun setAiAuditEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AI_AUDIT_ENABLED] = enabled }
    }

    suspend fun setAiAuditAutoWatch(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AI_AUDIT_AUTO_WATCH] = enabled }
    }

    suspend fun setAiAuditExplainLanguage(lang: String) {
        context.dataStore.edit { it[Keys.AI_AUDIT_EXPLAIN_LANG] = if (lang == "en") "en" else "ur" }
    }

    suspend fun setAiAuditFindingsJson(json: String) {
        context.dataStore.edit { it[Keys.AI_AUDIT_FINDINGS_JSON] = json }
    }

    suspend fun setHeyIceWakeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HEY_ICE_WAKE_ENABLED] = enabled }
    }

    suspend fun setVoiceWakeLang(langTag: String) {
        context.dataStore.edit { it[Keys.VOICE_WAKE_LANG] = langTag }
    }
}
