package de.nielstron.texttocalendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

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

        val now = ZonedDateTime.of(2024, 11, 12, 9, 30, 0, 0, ZoneOffset.UTC)
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
                "startTime": "2025-01-05T10:00:00Z",
                "endTime": "not-a-date"
            }
        """.trimIndent()

        val event = parseEventResponse(
            response = response,
            originalText = "Planning Session at 10",
            descriptionFormatter = { summary, original -> "$summary -> $original" },
            nowProvider = { ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC) }
        )

        assertEquals(ZonedDateTime.parse("2025-01-05T10:00:00Z"), event.startTime)
        assertNull(event.endTime)
    }
}
