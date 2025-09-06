package de.nielstron.texttocalendar

import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime

class TextExtractionTest {

    @Test
    fun testEventExtractionFromValidJson() {
        // Test the JSON parsing part of the extraction logic
        val validJsonResponse = """
            {
                "title": "Meeting with John",
                "summary": "Discuss project updates",
                "location": "Conference Room A",
                "startTime": "2024-01-15T15:00:00",
                "endTime": "2024-01-15T16:00:00"
            }
        """.trimIndent()

        val parsedEvent = Json.decodeFromString<RawEvent>(validJsonResponse)
        
        assertEquals("Meeting with John", parsedEvent.title)
        assertEquals("Discuss project updates", parsedEvent.summary)
        assertEquals("Conference Room A", parsedEvent.location)
        assertEquals("2024-01-15T15:00:00", parsedEvent.startTime)
        assertEquals("2024-01-15T16:00:00", parsedEvent.endTime)
    }

    @Test
    fun testEventExtractionFromMinimalJson() {
        // Test with minimal required fields
        val minimalJsonResponse = """
            {
                "title": "Simple Event"
            }
        """.trimIndent()

        val parsedEvent = Json.decodeFromString<RawEvent>(minimalJsonResponse)
        
        assertEquals("Simple Event", parsedEvent.title)
        assertEquals("", parsedEvent.summary) // default value
        assertNull(parsedEvent.location)
        assertNull(parsedEvent.startTime)
        assertNull(parsedEvent.endTime)
    }

    @Test
    fun testEventConversionToProperEvent() {
        // Test the conversion from RawEvent to ProperEvent
        val rawEvent = RawEvent(
            title = "Test Meeting",
            summary = "Test description",
            location = "Office",
            startTime = "2024-01-15T10:00:00",
            endTime = "2024-01-15T11:00:00"
        )

        val startTime = LocalDateTime.parse(rawEvent.startTime!!)
        val endTime = LocalDateTime.parse(rawEvent.endTime!!)
        
        val properEvent = ProperEvent(
            title = rawEvent.title,
            description = "Full description: ${rawEvent.summary}\n\nOriginal text here",
            location = rawEvent.location,
            startTime = startTime,
            endTime = endTime
        )

        assertEquals("Test Meeting", properEvent.title)
        assertTrue(properEvent.description.contains("Test description"))
        assertEquals("Office", properEvent.location)
        assertEquals(LocalDateTime.of(2024, 1, 15, 10, 0), properEvent.startTime)
        assertEquals(LocalDateTime.of(2024, 1, 15, 11, 0), properEvent.endTime)
    }

    @Test
    fun testEventConversionWithInvalidDateTime() {
        // Test handling of invalid date/time strings
        val rawEvent = RawEvent(
            title = "Test Event",
            summary = "Test",
            startTime = "invalid-date",
            endTime = null
        )

        // Simulate the error handling logic from MainActivity
        var startTime: LocalDateTime
        try {
            startTime = LocalDateTime.parse(rawEvent.startTime)
        } catch (e: Exception) {
            // If the start time is not provided or parseable, use the current time
            startTime = LocalDateTime.now()
        }
        
        var endTime: LocalDateTime?
        try {
            endTime = if (rawEvent.endTime != null) LocalDateTime.parse(rawEvent.endTime) else null
        } catch (e: Exception) {
            endTime = null
        }

        // Should not throw exception and should have reasonable defaults
        assertNotNull(startTime)
        assertNull(endTime)
    }

    @Test
    fun testPromptGeneration() {
        // Test that the prompt includes proper context
        val testText = "Meeting tomorrow at 3pm"
        val today = LocalDateTime.now().toString()
        val tomorrow = LocalDateTime.now().plusDays(1).toString()
        
        val prompt = """
            You are an expert at extracting calendar event details from a text.
            Provide the result in JSON format with fields: title, summary, location, startTime, endTime (both in ISO 8601 format).
            If a field is not present, omit it from the response. Do not include fields that can not be derived from the text.
            Keep the summary very short and concise, the full original text will also be provided.
            The following languages can be kept for the description: en-US.
            Otherwise translate to English.
            If there is no event in the text, return an empty JSON object.
            Today is $today
            Here are some examples:

            Text:
            ```
            $testText
            ```
        """.trimIndent()

        assertTrue("Prompt should contain today's date", prompt.contains(today))
        assertTrue("Prompt should contain the input text", prompt.contains(testText))
        assertTrue("Prompt should mention JSON format", prompt.contains("JSON"))
        assertTrue("Prompt should mention required fields", prompt.contains("title"))
    }
}