package com.shardmail.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ShardMailApi {

    @GET("health")
    suspend fun getHealth(): Response<HealthResponse>

    @POST("api/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @GET("api/messages")
    suspend fun getMessages(
        @Query("search") search: String? = null,
        @Query("to") to: String? = null
    ): Response<MessagesResponse>

    @GET("api/messages/{id}")
    suspend fun getMessageById(@Path("id") id: String): Response<SingleMessageResponse>

    @DELETE("api/messages/{id}")
    suspend fun deleteMessage(@Path("id") id: String): Response<GenericResponse>

    @GET("api/aliases")
    suspend fun getAliases(): Response<AliasesResponse>

    @POST("api/aliases")
    suspend fun createAlias(@Body body: CreateAliasRequest): Response<CreateAliasResponse>

    @DELETE("api/aliases/{id}")
    suspend fun deleteAlias(@Path("id") id: String): Response<GenericResponse>

    @GET("api/settings")
    suspend fun getSettings(): Response<SettingsResponse>

    @POST("api/settings")
    suspend fun updateSettings(@Body body: Map<String, String>): Response<GenericResponse>

    @POST("api/test-inject")
    suspend fun testInject(@Body body: TestInjectRequest): Response<TestInjectResponse>
}
