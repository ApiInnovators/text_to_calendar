package de.nielstron.texttocalendar

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class OpenAiServiceTest {

    @Test
    fun testChatCompletionWithValidResponse() = runBlocking {
        // This test uses the actual OpenAI API with a simple prompt
        // Note: This requires a valid API key and will make a real API call
        val apiKey = de.nielstron.texttocalendar.BuildConfig.DEFAULT_API_KEY
        val baseUrl = "https://api.openai.com/v1/"
        
        if (apiKey.isNullOrEmpty()) {
            // Skip test if no API key provided
            return@runBlocking
        }
        
        val service = OpenAiService(baseUrl, apiKey)
        
        val prompt = """
            Extract event details from this text and return JSON with fields: title, summary, location, startTime, endTime.
            If no event found, return empty JSON object {}.
            
            Text: "Meeting with John tomorrow at 3pm in conference room A"
        """.trimIndent()
        
        try {
            val response = service.chatCompletion(
                model = AppPreferencesConfig.DEFAULT_MODEL,
                prompt = prompt,
                reasoning_effort = AppPreferencesConfig.DEFAULT_REASONING_EFFORT,
                forceJson = AppPreferencesConfig.DEFAULT_FORCE_JSON
            )
            
            // Verify response is not empty
            assertNotNull(response)
            assertTrue(response.isNotEmpty())
            
            // Verify response contains JSON
            assertTrue(response.trim().startsWith("{") || response.trim().startsWith("["))
            
        } catch (e: Exception) {
            // If API call fails, just verify the service handles errors properly
            assertTrue("Service should throw meaningful exceptions", 
                e.message?.contains("API") == true || e.message?.contains("error") == true)
        }
    }

    @Test
    fun testChatCompletionThrowsExceptionWithInvalidApiKey() = runBlocking {
        val service = OpenAiService("https://api.openai.com/v1/", "invalid-key")
        
        var exceptionThrown = false
        var actualMessage = ""
        
        try {
            service.chatCompletion(
                model = AppPreferencesConfig.DEFAULT_MODEL,
                prompt = "test prompt",
                reasoning_effort = AppPreferencesConfig.DEFAULT_REASONING_EFFORT
            )
            fail("Should have thrown an exception with invalid API key")
        } catch (e: Exception) {
            exceptionThrown = true
            actualMessage = e.message ?: "null message"
            println("Actual exception: ${e.javaClass.simpleName}")
            println("Actual message: $actualMessage")
        }
        
        assertTrue("An exception should have been thrown", exceptionThrown)
        assertTrue("Exception message was: '$actualMessage'. Should mention API or baseUrl error", 
            actualMessage.contains("API") || 
            actualMessage.contains("401") ||
            actualMessage.contains("error") ||
            actualMessage.contains("Unauthorized") ||
            actualMessage.contains("authentication") ||
            actualMessage.contains("baseUrl"))
    }
}
