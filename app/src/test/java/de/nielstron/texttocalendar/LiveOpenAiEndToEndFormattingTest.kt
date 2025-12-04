package de.nielstron.texttocalendar

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.time.LocalDateTime

class LiveOpenAiEndToEndFormattingTest {

    private fun ensureApiKey(): String {
        val apiKey = BuildConfig.DEFAULT_API_KEY
        assumeTrue("No OpenAI API key configured; skipping live test", !apiKey.isNullOrBlank())
        return apiKey!!
    }

    @Test
    fun liveCestScenarioMatchesExpectedFormatting() = runBlocking {
        val apiKey = ensureApiKey()
        val service = OpenAiService(AppPreferencesConfig.DEFAULT_ENDPOINT, apiKey)
        val text = """
            Join the Berlin Product Meetup on June 10, 2025 at 09:00 CEST.
            We will gather at Factory Berlin and finish around 11:00.
        """.trimIndent()

        val response = service.chatCompletion(
            model = AppPreferencesConfig.DEFAULT_MODEL,
            prompt = buildPrompt(text),
            reasoning_effort = AppPreferencesConfig.DEFAULT_REASONING_EFFORT,
            forceJson = AppPreferencesConfig.DEFAULT_FORCE_JSON
        )

        val parsedEvent = Json.decodeFromString<RawEvent>(response)
        assertEquals("Berlin Product Meetup", parsedEvent.title)
        assertTrue(parsedEvent.startTime!!.contains("+02:00"))
        assertEquals(LocalDateTime.of(2025, 6, 10, 9, 0), DateTimeParser.toLocalDateTime(parsedEvent.startTime!!))
        assertEquals(LocalDateTime.of(2025, 6, 10, 11, 0), DateTimeParser.toLocalDateTime(parsedEvent.endTime!!))
    }

    @Test
    fun liveSingaporeScenarioMatchesExpectedFormatting() = runBlocking {
        val apiKey = ensureApiKey()
        val service = OpenAiService(AppPreferencesConfig.DEFAULT_ENDPOINT, apiKey)
        val text = """
            Singapore Night Ride happens on August 1st, 2025 from 8 PM to 11 PM SGT.
            We'll meet at Marina Bay Sands.
        """.trimIndent()

        val response = service.chatCompletion(
            model = AppPreferencesConfig.DEFAULT_MODEL,
            prompt = buildPrompt(text),
            reasoning_effort = AppPreferencesConfig.DEFAULT_REASONING_EFFORT,
            forceJson = AppPreferencesConfig.DEFAULT_FORCE_JSON
        )

        val parsedEvent = Json.decodeFromString<RawEvent>(response)
        assertEquals("Singapore Night Ride", parsedEvent.title)
        assertTrue(parsedEvent.startTime!!.contains("+08:00"))
        assertEquals(LocalDateTime.of(2025, 8, 1, 20, 0), DateTimeParser.toLocalDateTime(parsedEvent.startTime!!))
        assertEquals(LocalDateTime.of(2025, 8, 1, 23, 0), DateTimeParser.toLocalDateTime(parsedEvent.endTime!!))
    }

    @Test
    fun liveNewYorkScenarioMatchesExpectedFormatting() = runBlocking {
        val apiKey = ensureApiKey()
        val service = OpenAiService(AppPreferencesConfig.DEFAULT_ENDPOINT, apiKey)
        val text = """
            NYC Investor Dinner is on March 1, 2025 from 6 PM to 9 PM EST at Gramercy Tavern in New York.
        """.trimIndent()

        val response = service.chatCompletion(
            model = AppPreferencesConfig.DEFAULT_MODEL,
            prompt = buildPrompt(text),
            reasoning_effort = AppPreferencesConfig.DEFAULT_REASONING_EFFORT,
            forceJson = AppPreferencesConfig.DEFAULT_FORCE_JSON
        )

        val parsedEvent = Json.decodeFromString<RawEvent>(response)
        assertEquals("NYC Investor Dinner", parsedEvent.title)
        assertTrue(parsedEvent.startTime!!.contains("-05:00"))
        assertEquals(LocalDateTime.of(2025, 3, 1, 18, 0), DateTimeParser.toLocalDateTime(parsedEvent.startTime!!))
        assertEquals(LocalDateTime.of(2025, 3, 1, 21, 0), DateTimeParser.toLocalDateTime(parsedEvent.endTime!!))
    }

    private fun buildPrompt(text: String): String {
        val now = LocalDateTime.now()
        val tomorrow = now.plusDays(1)
        return """
            You are an expert at extracting calendar event details from a text.
            Provide the result in JSON format with fields: title, summary, location, startTime, endTime (both in ISO 8601 format).
            If a field is not present, omit it from the response. Do not include fields that can not be derived from the text.
            Keep the summary very short and concise, the full original text will also be provided.
            The following languages can be kept for the description: en-US.
            Otherwise translate to English.
            If there is no event in the text, return an empty JSON object.
            Today is $now
            Here are some examples:

            Text:
            ```
            Hey everyone, I would like to invite you for a chill BBQ tomorrow, around 7 at my place? Until 10?
            Best Max
            ```
            Extracted JSON:
            {
            "title": "BBQ with Max",
            "location": "Max place",
            "startTime": "${tomorrow.toLocalDate()}T19:00:00",
            "endTime": "${tomorrow.toLocalDate()}T22:00:00"
            }

            Text:
            ```
            $text
            ```
        """.trimIndent()
    }
}
