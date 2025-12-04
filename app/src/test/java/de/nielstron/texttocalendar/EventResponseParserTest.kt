package de.nielstron.texttocalendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class EventResponseParserTest {

    @Test
    fun missingStartTimeFallsBackToNow() {
        val response = """
            {
                "title": "Lunch",
                "summary": "Casual meetup",
                "location": "Downtown Cafe"
            }
        """.trimIndent()

        val now = LocalDateTime.of(2024, 11, 12, 9, 30)
        val event = parseEventResponse(
            response = response,
            originalText = "Lunch at 9:30 on Nov 12",
            descriptionFormatter = { summary, original -> "$summary :: $original" },
            nowProvider = { now }
        )

        assertEquals("Lunch", event.title)
        assertEquals(now, event.startTime)
        assertEquals("Casual meetup :: Lunch at 9:30 on Nov 12", event.description)
        assertNull(event.endTime)
    }

    @Test
    fun invalidEndTimeIsIgnored() {
        val response = """
            {
                "title": "Planning Session",
                "summary": "Discuss roadmap",
                "startTime": "2025-01-05T10:00:00",
                "endTime": "not-a-date"
            }
        """.trimIndent()

        val event = parseEventResponse(
            response = response,
            originalText = "Planning Session at 10",
            descriptionFormatter = { summary, original -> "$summary -> $original" },
            nowProvider = { LocalDateTime.of(2025, 1, 1, 0, 0) }
        )

        assertEquals(LocalDateTime.of(2025, 1, 5, 10, 0), event.startTime)
        assertNull(event.endTime)
    }
}
