package de.nielstron.texttocalendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class CalendarLaunchDataTest {

    @Test
    fun buildCalendarLaunchDataUsesProvidedOffset() {
        val event = ProperEvent(
            title = "Standup",
            description = "Daily sync",
            startTime = LocalDateTime.of(2025, 2, 1, 9, 0),
            endTime = LocalDateTime.of(2025, 2, 1, 9, 15),
            location = "HQ"
        )

        val launchData = buildCalendarLaunchData(event) { ZoneOffset.UTC }

        assertEquals(event.title, launchData.title)
        assertEquals(event.description, launchData.description)
        assertEquals(event.location, launchData.location)
        assertEquals(event.startTime.toEpochSecond(ZoneOffset.UTC) * 1000, launchData.startTimeEpochMillis)
        assertEquals(event.endTime!!.toEpochSecond(ZoneOffset.UTC) * 1000, launchData.endTimeEpochMillis)
    }

    @Test
    fun buildCalendarLaunchDataAllowsMissingEndTime() {
        val event = ProperEvent(
            title = "One-off",
            description = "No end time",
            startTime = LocalDateTime.of(2025, 3, 10, 12, 0),
            endTime = null,
            location = null
        )

        val launchData = buildCalendarLaunchData(event) { ZoneOffset.ofHours(2) }

        assertEquals(event.startTime.toEpochSecond(ZoneOffset.ofHours(2)) * 1000, launchData.startTimeEpochMillis)
        assertNull(launchData.endTimeEpochMillis)
        assertNull(launchData.location)
    }
}
