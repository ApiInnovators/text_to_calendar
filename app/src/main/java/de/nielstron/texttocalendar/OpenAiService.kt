package de.nielstron.texttocalendar

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OpenAiService(private val baseUrl: String, private val apiKey: String) {
    
    private val api: OpenAiApi by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAiApi::class.java)
    }

    suspend fun chatCompletion(
        model: String,
        prompt: String,
        forceJson: Boolean = true
    ): String {
        val messages = listOf(
            Message(role = "user", content = prompt)
        )
        
        val responseFormat = if (forceJson) {
            ResponseFormat(type = "json_object")
        } else {
            null
        }
        
        val request = OpenAiRequest(
            model = model,
            messages = messages,
            response_format = responseFormat
        )
        
        val chatUrl = if (baseUrl.endsWith("/")) {
            "${baseUrl}chat/completions"
        } else {
            "$baseUrl/chat/completions"
        }
        
        val response = api.chatCompletion(
            url = chatUrl,
            authorization = "Bearer $apiKey",
            request = request
        )
        
        if (response.isSuccessful) {
            val responseBody = response.body()
            if (responseBody == null) {
                throw Exception("Null response body from OpenAI API")
            }
            if (responseBody.choices.isEmpty()) {
                throw Exception("No choices in OpenAI API response")
            }
            val content = responseBody.choices.firstOrNull()?.message?.content
            if (content.isNullOrBlank()) {
                throw Exception("Empty content in OpenAI API response. Response body: $responseBody")
            }
            return content
        } else {
            val errorBody = response.errorBody()?.string() ?: "Unknown error"
            throw Exception("OpenAI API error: ${response.code()} ${response.message()}. Error body: $errorBody")
        }
    }
}