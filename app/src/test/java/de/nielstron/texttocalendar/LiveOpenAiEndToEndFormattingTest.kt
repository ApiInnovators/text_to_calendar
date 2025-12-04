package de.nielstron.texttocalendar

import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.Assert.assertEquals
import java.time.LocalDateTime

class LiveOpenAiEndToEndFormattingTest {

    private fun ensureApiKey(): String {
        val apiKey = BuildConfig.DEFAULT_API_KEY
        assumeTrue("No OpenAI API key configured; skipping live test", !apiKey.isNullOrBlank())
        return apiKey!!
    }

    private fun promptSettings(apiKey: String) = PromptSettings(
        endpoint = AppPreferencesConfig.DEFAULT_ENDPOINT,
        apiKey = apiKey,
        model = AppPreferencesConfig.DEFAULT_MODEL,
        reasoningEffort = AppPreferencesConfig.DEFAULT_REASONING_EFFORT,
        forceJson = AppPreferencesConfig.DEFAULT_FORCE_JSON,
        keepLanguageFor = "en-US",
        autoTranslateTo = "English",
    )

    private val descriptionFormatter = { summary: String, original: String ->
        "$summary\n\nOriginal Text: \n $original"
    }

    @Test
    fun liveCestScenarioMatchesExpectedFormatting() = runBlocking {
        val apiKey = ensureApiKey()
        val text = """
            Join the Berlin Product Meetup on June 10, 2025 at 09:00 CEST.
            We will gather at Factory Berlin and finish around 11:00.
        """.trimIndent()

        val response = sendPromptToModel(text, promptSettings(apiKey))

        val parsedEvent = parseEventResponse(response, text, descriptionFormatter)
        assertEquals("Berlin Product Meetup", parsedEvent.title)
        assertEquals(LocalDateTime.of(2025, 6, 10, 9, 0), parsedEvent.startTime)
        assertEquals(LocalDateTime.of(2025, 6, 10, 11, 0), parsedEvent.endTime)
    }

    @Test
    fun liveSingaporeScenarioMatchesExpectedFormatting() = runBlocking {
        val apiKey = ensureApiKey()
        val text = """
            Singapore Night Ride happens on August 1st, 2025 from 8 PM to 11 PM SGT.
            We'll meet at Marina Bay Sands.
        """.trimIndent()

        val response = sendPromptToModel(text, promptSettings(apiKey))

        val parsedEvent = parseEventResponse(response, text, descriptionFormatter)
        assertEquals("Singapore Night Ride", parsedEvent.title)
        assertEquals(LocalDateTime.of(2025, 8, 1, 20, 0), parsedEvent.startTime)
        assertEquals(LocalDateTime.of(2025, 8, 1, 23, 0), parsedEvent.endTime)
    }

    @Test
    fun liveNewYorkScenarioMatchesExpectedFormatting() = runBlocking {
        val apiKey = ensureApiKey()
        val text = """
            NYC Investor Dinner is on March 1, 2025 from 6 PM to 9 PM EST at Gramercy Tavern in New York.
        """.trimIndent()

        val response = sendPromptToModel(text, promptSettings(apiKey))
        val parsedEvent = parseEventResponse(response, text, descriptionFormatter)
        assertEquals("NYC Investor Dinner", parsedEvent.title)
        assertEquals(LocalDateTime.of(2025, 3, 1, 18, 0), parsedEvent.startTime)
        assertEquals(LocalDateTime.of(2025, 3, 1, 21, 0), parsedEvent.endTime)
    }
}
