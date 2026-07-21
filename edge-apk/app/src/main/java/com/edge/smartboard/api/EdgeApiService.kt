package com.edge.smartboard.api

import com.edge.smartboard.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface EdgeApiService {

    @POST("login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @Multipart
    @POST("capture/upload")
    suspend fun uploadSession(
        @Header("Authorization") token: String,
        @Part zip: MultipartBody.Part,
        @Part("session_id") sessionId: RequestBody,
        @Part("metadata") metadata: RequestBody
    ): Response<UploadResponse>

    @GET("session")
    suspend fun getSessions(
        @Header("Authorization") token: String
    ): Response<List<Session>>

    @GET("session/{id}")
    suspend fun getSession(
        @Header("Authorization") token: String,
        @Path("id") sessionId: String
    ): Response<Session>

    @GET("reports")
    suspend fun getReports(
        @Header("Authorization") token: String
    ): Response<List<Report>>

    @GET("reports/{sessionId}")
    suspend fun getReport(
        @Header("Authorization") token: String,
        @Path("sessionId") sessionId: String
    ): Response<Report>

    @GET("status")
    suspend fun getStatus(
        @Header("Authorization") token: String
    ): Response<SystemMetrics>

    @GET("dashboard")
    suspend fun getDashboard(
        @Header("Authorization") token: String
    ): Response<AdminDashboard>

    @GET("history")
    suspend fun getHistory(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 20
    ): Response<List<HistoryItem>>

    @GET("live/{sessionId}")
    suspend fun getLiveFrame(
        @Header("Authorization") token: String,
        @Path("sessionId") sessionId: String
    ): Response<LiveFrame>

    // Teacher Analysis Upload — video + frames + audio in one multipart request
    @Multipart
    @POST("teacher/upload")
    suspend fun uploadTeacherSession(
        @Header("Authorization")        token: String,
        @Part                           video: MultipartBody.Part,
        @Part                           frames: List<MultipartBody.Part>,
        @Part                           audio: MultipartBody.Part?,
        @Part("teacher_id")             teacherId: RequestBody,
        @Part("teacher_name")           teacherName: RequestBody,
        @Part("subject")                subject: RequestBody,
        @Part("duration_ms")            durationMs: RequestBody,
        @Part("frame_count")            frameCount: RequestBody,
        @Part("file_size_bytes")        fileSizeBytes: RequestBody
    ): Response<TeacherUploadResponse>
}
