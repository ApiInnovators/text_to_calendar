package de.nielstron.texttocalendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

class CalendarLaunchDataTest {

    @Test
    fun buildCalendarLaunchDataUsesProvidedOffset() {
        val event = ProperEvent(
            title = "Standup",
            description = "Daily sync",
            startTime = ZonedDateTime.of(2025, 2, 1, 9, 0, 0, 0, ZoneOffset.UTC),
            endTime = ZonedDateTime.of(2025, 2, 1, 9, 15, 0, 0, ZoneOffset.UTC),
            location = "HQ"
        )

        val launchData = buildCalendarLaunchData(event)

        assertEquals(event.title, launchData.title)
        assertEquals(event.description, launchData.description)
        assertEquals(event.location, launchData.location)
        assertEquals(event.startTime.toInstant().toEpochMilli(), launchData.startTimeEpochMillis)
        assertEquals(event.endTime!!.toInstant().toEpochMilli(), launchData.endTimeEpochMillis)
    }

    @Test
    fun buildCalendarLaunchDataAllowsMissingEndTime() {
        val offset = ZoneOffset.ofHours(2)
        val event = ProperEvent(
            title = "One-off",
            description = "No end time",
            startTime = ZonedDateTime.of(2025, 3, 10, 12, 0, 0, 0, offset),
            endTime = null,
            location = null
        )

        val launchData = buildCalendarLaunchData(event)

        assertEquals(event.startTime.toInstant().toEpochMilli(), launchData.startTimeEpochMillis)
        assertNull(launchData.endTimeEpochMillis)
        assertNull(launchData.location)
    }
}
