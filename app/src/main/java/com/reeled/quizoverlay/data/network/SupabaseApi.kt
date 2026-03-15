package com.reeled.quizoverlay.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApi {
    @GET("rest/v1/quiz_questions")
    @Headers("Prefer: count=exact")
    suspend fun fetchActiveQuestions(
        @Query("active") active: String = "eq.true",
        @Query("limit") limit: Int = 100,
        @Query("order") order: String = "difficulty.asc"
    ): List<QuizQuestionDto>

    @POST("rest/v1/quiz_attempts")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun batchInsertAttempts(@Body attempts: List<QuizAttemptDto>)

    @POST("rest/v1/event_logs")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun batchInsertEvents(@Body events: List<EventLogDto>)
}
