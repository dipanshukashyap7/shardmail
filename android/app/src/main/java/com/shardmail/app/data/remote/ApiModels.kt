package com.shardmail.app.data.remote

data class HealthResponse(
    val ok: Boolean,
    val messages: Int = 0,
    val aliases: Int = 0,
    val domain: String? = null,
    val resendConfigured: Boolean = false,
    val time: String? = null,
    val error: String? = null
)

data class LoginRequest(
    val password: String
)

data class LoginResponse(
    val ok: Boolean,
    val error: String? = null
)

data class MessageDto(
    val id: String,
    val from_addr: String,
    val to_addr: String,
    val subject: String,
    val text: String? = null,
    val code: String? = null,
    val date: String? = null,
    val created_at: String? = null,
    val read: Int = 0
)

data class FullMessageDto(
    val id: String,
    val from_addr: String,
    val to_addr: String,
    val subject: String,
    val text: String? = null,
    val html: String? = null,
    val code: String? = null,
    val date: String? = null,
    val created_at: String? = null,
    val read: Int = 0
)

data class MessagesResponse(
    val ok: Boolean,
    val messages: List<MessageDto>? = null,
    val error: String? = null
)

data class SingleMessageResponse(
    val ok: Boolean,
    val message: FullMessageDto? = null,
    val error: String? = null
)

data class AliasDto(
    val id: String,
    val prefix: String,
    val address: String,
    val created_at: String
)

data class AliasesResponse(
    val ok: Boolean,
    val aliases: List<AliasDto>? = null,
    val error: String? = null
)

data class CreateAliasRequest(
    val prefix: String
)

data class CreateAliasResponse(
    val ok: Boolean,
    val alias: AliasDto? = null,
    val error: String? = null
)

data class TestInjectRequest(
    val to: String
)

data class TestInjectResponse(
    val ok: Boolean,
    val id: String? = null,
    val message: MessageDto? = null,
    val error: String? = null
)

data class SettingsResponse(
    val ok: Boolean,
    val settings: Map<String, String>? = null,
    val error: String? = null
)

data class GenericResponse(
    val ok: Boolean,
    val error: String? = null
)
