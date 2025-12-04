package de.nielstron.texttocalendar

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class TimezoneParsingTest {

    @Test
    fun testParseTimezoneAwareDateTime() {
        // Test the same logic as in MainActivity
        val timestampWithTimezone = "2025-09-12T19:30:00+09:00"

        val result = DateTimeParser.toZonedDateTime(timestampWithTimezone)
        val expected = ZonedDateTime.of(LocalDateTime.of(2025, 9, 12, 19, 30), ZoneOffset.ofHours(9))

        assertEquals(expected, result)
    }

    @Test
    fun testParseBasicDateTime() {
        // Test basic ISO format without timezone
        val basicTimestamp = "2025-09-12T19:30:00"
        val defaultZone = ZoneId.of("UTC")

        val result = DateTimeParser.toZonedDateTime(basicTimestamp) { defaultZone }
        val expected = ZonedDateTime.of(LocalDateTime.of(2025, 9, 12, 19, 30), defaultZone)

        assertEquals(expected, result)
    }

    @Test
    fun testParseZuluTime() {
        // Test UTC timezone format
        val zuluTimestamp = "2025-09-12T10:30:00Z"

        val result = DateTimeParser.toZonedDateTime(zuluTimestamp)
        val expected = ZonedDateTime.of(LocalDateTime.of(2025, 9, 12, 10, 30), ZoneOffset.UTC)

        assertEquals(expected, result)
    }
}
