package com.reeled.quizoverlay.data.remote

import com.reeled.quizoverlay.data.remote.dto.EventLogDto
import com.reeled.quizoverlay.data.remote.dto.OverlaySessionDto
import com.reeled.quizoverlay.data.remote.dto.QuizAttemptDto
import com.reeled.quizoverlay.data.remote.dto.QuizQuestionDto
import com.reeled.quizoverlay.data.remote.dto.TesterDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApi {
    @GET("rest/v1/quiz_questions")
    suspend fun getActiveQuestions(
        @Query("active", encoded = true) active: String = "eq.true",
        @Query("limit") limit: Int = 100,
        @Header("Range") range: String = "0-99"
    ): List<QuizQuestionDto>

    @POST("rest/v1/testers")
    suspend fun postTester(
        @Body tester: TesterDto,
        @Header("Prefer") prefer: String = "return=representation"
    )

    @POST("rest/v1/quiz_attempts")
    suspend fun postAttempts(
        @Body attempts: List<QuizAttemptDto>,
        @Header("Prefer") prefer: String = "return=representation"
    )

    @POST("rest/v1/overlay_sessions")
    suspend fun postSessions(
        @Body sessions: List<OverlaySessionDto>,
        @Header("Prefer") prefer: String = "return=representation"
    )

    @POST("rest/v1/event_logs")
    suspend fun postEvents(
        @Body events: List<EventLogDto>,
        @Header("Prefer") prefer: String = "return=representation"
    )
}
