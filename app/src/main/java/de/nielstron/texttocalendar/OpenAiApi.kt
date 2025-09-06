package de.nielstron.texttocalendar

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiRequest(
    val model: String,
    val messages: List<Message>,
    val response_format: ResponseFormat? = null
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class ResponseFormat(
    val type: String
)

@Serializable
data class OpenAiResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: Message
)

interface OpenAiApi {
    @POST
    suspend fun chatCompletion(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: OpenAiRequest
    ): Response<OpenAiResponse>
}