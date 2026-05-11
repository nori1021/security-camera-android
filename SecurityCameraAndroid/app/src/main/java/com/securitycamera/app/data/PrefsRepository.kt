package com.securitycamera.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.securitycamera.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "security_camera_prefs")

class PrefsRepository(private val context: Context) {

    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val FUNCTION_KEY = stringPreferencesKey("function_key")
        val NOTIFY_EMAIL = stringPreferencesKey("notify_email")
        val ROI_LEFT = floatPreferencesKey("roi_left")
        val ROI_TOP = floatPreferencesKey("roi_top")
        val ROI_WIDTH = floatPreferencesKey("roi_width")
        val ROI_HEIGHT = floatPreferencesKey("roi_height")
        val SUBJECTS = stringSetPreferencesKey("subjects")
        val LAST_ANALYZE_MS = longPreferencesKey("last_analyze_ms")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            baseUrl = BuildConfig.FUNCTIONS_BASE_URL.ifBlank {
                p[Keys.BASE_URL].orEmpty()
            },
            functionKey = BuildConfig.FUNCTIONS_KEY.ifBlank {
                p[Keys.FUNCTION_KEY].orEmpty()
            },
            notifyEmail = p[Keys.NOTIFY_EMAIL].orEmpty(),
            roiLeft = p[Keys.ROI_LEFT] ?: 0f,
            roiTop = p[Keys.ROI_TOP] ?: 0f,
            roiWidth = p[Keys.ROI_WIDTH] ?: 1f,
            roiHeight = p[Keys.ROI_HEIGHT] ?: 1f,
            enrolledSubjects = p[Keys.SUBJECTS] ?: emptySet(),
        )
    }

    val lastAnalyzeTimeMs: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_ANALYZE_MS] ?: 0L }

    suspend fun updateConnection(baseUrl: String, functionKey: String) {
        context.dataStore.edit {
            it[Keys.BASE_URL] = baseUrl.trimEnd('/')
            it[Keys.FUNCTION_KEY] = functionKey.trim()
        }
    }

    suspend fun updateNotifyEmail(email: String) {
        context.dataStore.edit { it[Keys.NOTIFY_EMAIL] = email.trim() }
    }

    suspend fun updateRoi(left: Float, top: Float, width: Float, height: Float) {
        context.dataStore.edit {
            it[Keys.ROI_LEFT] = left.coerceIn(0f, 1f)
            it[Keys.ROI_TOP] = top.coerceIn(0f, 1f)
            it[Keys.ROI_WIDTH] = width.coerceIn(0.05f, 1f)
            it[Keys.ROI_HEIGHT] = height.coerceIn(0.05f, 1f)
        }
    }

    suspend fun addEnrolledSubject(subjectId: String) {
        context.dataStore.edit { prefs ->
            val cur = prefs[Keys.SUBJECTS]?.toMutableSet() ?: mutableSetOf()
            cur.add(subjectId)
            prefs[Keys.SUBJECTS] = cur
        }
    }

    suspend fun removeEnrolledSubject(subjectId: String) {
        context.dataStore.edit { prefs ->
            val cur = prefs[Keys.SUBJECTS]?.toMutableSet() ?: mutableSetOf()
            cur.remove(subjectId)
            prefs[Keys.SUBJECTS] = cur
        }
    }

    suspend fun markAnalyzeAttempt() {
        context.dataStore.edit { it[Keys.LAST_ANALYZE_MS] = System.currentTimeMillis() }
    }

    suspend fun getLastAnalyzeMs(): Long =
        context.dataStore.data.first()[Keys.LAST_ANALYZE_MS] ?: 0L
}
