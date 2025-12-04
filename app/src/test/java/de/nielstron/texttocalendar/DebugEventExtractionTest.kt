package de.nielstron.texttocalendar

import kotlinx.coroutines.runBlocking
import org.junit.Test

class DebugEventExtractionTest {

    @Test
    fun testSpecificEventText() = runBlocking {
        val apiKey = de.nielstron.texttocalendar.BuildConfig.DEFAULT_API_KEY
        val baseUrl = "https://api.openai.com/v1/"
        
        if (apiKey.isNullOrEmpty()) {
            println("No API key available, skipping test")
            return@runBlocking
        }

        val service = OpenAiService(baseUrl, apiKey)
        
        val testText = """Photo of Daria goos
Hosted By
Daria g. and 4 others
♥️🔥❤️‍🔥We are international friends❤️‍🔥🔥♥️
Sep 12 @ 7:30 PM KST
Details

Hello! Nice to meet you guys! Welcome to MNT language exchange and after parties!

We are really great and wonderful community and it will be really great time for you too definitely!

We are language exchange meetup and pub crawl for foreigners and Korean who want"""

        val today = java.time.LocalDateTime.now().toString()
        val tomorrow = java.time.LocalDateTime.now().plusDays(1).toString()

        val prompt = """
            You are an expert at extracting calendar event details from a text.
            Provide the result in JSON format with fields: title, summary, location, startTime, endTime (both in ISO 8601 format).
            If a field is not present, omit it from the response. Do not include fields that can not be derived from the text.
            Keep the summary very short and concise, the full original text will also be provided.
            The following languages can be kept for the description: en-US.
            Otherwise translate to English.
            If there is no event in the text, return an empty JSON object.
            Today is $today
            
            Text:
            ```
            $testText
            ```
        """.trimIndent()

        try {
            println("Making API call with model: ${AppPreferencesConfig.DEFAULT_MODEL}")
            val response = service.chatCompletion(
                model = AppPreferencesConfig.DEFAULT_MODEL, // Use the shared default model
                prompt = prompt,
                reasoning_effort = AppPreferencesConfig.DEFAULT_REASONING_EFFORT,
                forceJson = AppPreferencesConfig.DEFAULT_FORCE_JSON
            )
            
            println("API Response: $response")
            
            if (response.trim() == "{}") {
                println("Empty JSON response - no event detected")
            } else {
                try {
                    val parsedEvent = kotlinx.serialization.json.Json.decodeFromString<RawEvent>(response)
                    println("Parsed event: $parsedEvent")
                } catch (e: Exception) {
                    println("JSON parsing failed: ${e.message}")
                    println("Raw response: $response")
                }
            }
            
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
        }
    }
}
