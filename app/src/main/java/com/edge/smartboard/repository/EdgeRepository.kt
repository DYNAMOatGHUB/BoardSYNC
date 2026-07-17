package com.edge.smartboard.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.edge.smartboard.api.EdgeApiService
import com.edge.smartboard.database.SessionDao
import com.edge.smartboard.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EdgeRepository @Inject constructor(
    private val api: EdgeApiService,
    private val sessionDao: SessionDao,
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        val KEY_TOKEN  = stringPreferencesKey("auth_token")
        val KEY_EMAIL  = stringPreferencesKey("email")
        val KEY_USER   = stringPreferencesKey("user_json")
        val KEY_SERVER = stringPreferencesKey("server_url")
    }

    // ── Auth ──────────────────────────────────────────────
    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                saveToken(body.access_token)
                Result.success(body)
            } else {
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveToken(token: String) {
        dataStore.edit { it[KEY_TOKEN] = token }
    }

    suspend fun getToken(): String? {
        return dataStore.data.map { it[KEY_TOKEN] }.firstOrNull()
    }

    suspend fun logout() {
        dataStore.edit {
            it.remove(KEY_TOKEN)
            it.remove(KEY_EMAIL)
            it.remove(KEY_USER)
        }
    }

    fun isLoggedIn(): Flow<Boolean> = dataStore.data.map { it[KEY_TOKEN] != null }

    // ── Server URL ────────────────────────────────────────
    suspend fun saveServerUrl(url: String) {
        dataStore.edit { it[KEY_SERVER] = url.trimEnd('/') + "/" }
    }

    suspend fun getServerUrl(): String =
        dataStore.data.map { it[KEY_SERVER] ?: "" }.firstOrNull() ?: ""

    fun getServerUrlFlow(): Flow<String> =
        dataStore.data.map { it[KEY_SERVER] ?: "" }

    // ── Sessions ──────────────────────────────────────────
    fun getAllSessions(): Flow<List<Session>> = sessionDao.getAllSessions()

    suspend fun saveSession(session: Session) = sessionDao.insertSession(session)

    suspend fun updateSession(session: Session) = sessionDao.updateSession(session)

    suspend fun deleteSession(session: Session) = sessionDao.deleteSession(session)

    // ── Upload ────────────────────────────────────────────
    suspend fun uploadSession(zipFile: File, sessionId: String, metadataJson: String): Result<UploadResponse> {
        return try {
            val token = "Bearer ${getToken()}"
            val requestFile = zipFile.asRequestBody("application/zip".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", zipFile.name, requestFile)
            val sidPart = sessionId.toRequestBody("text/plain".toMediaTypeOrNull())
            val metaPart = metadataJson.toRequestBody("application/json".toMediaTypeOrNull())
            val response = api.uploadSession(token, filePart, sidPart, metaPart)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Upload failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Reports ───────────────────────────────────────────
    suspend fun getReport(sessionId: String): Result<Report> {
        return try {
            val token = "Bearer ${getToken()}"
            val response = api.getReport(token, sessionId)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception("Report fetch failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReports(): Result<List<Report>> {
        return try {
            val token = "Bearer ${getToken()}"
            val response = api.getReports(token)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception("Reports fetch failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── System Status ─────────────────────────────────────
    suspend fun getSystemStatus(): Result<SystemMetrics> {
        return try {
            val token = "Bearer ${getToken()}"
            val response = api.getStatus(token)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception("Status fetch failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Admin Dashboard ───────────────────────────────────
    suspend fun getAdminDashboard(): Result<AdminDashboard> {
        return try {
            val token = "Bearer ${getToken()}"
            val response = api.getDashboard(token)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception("Dashboard fetch failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── History ───────────────────────────────────────────
    suspend fun getHistory(page: Int = 0): Result<List<HistoryItem>> {
        return try {
            val token = "Bearer ${getToken()}"
            val response = api.getHistory(token, page)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception("History fetch failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Storage utils ─────────────────────────────────────
    suspend fun getTotalSessions() = sessionDao.getTotalCount()
    suspend fun getTotalStorage() = sessionDao.getTotalStorageBytes() ?: 0L
}
