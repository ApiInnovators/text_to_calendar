package de.nielstron.texttocalendar

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime
import java.time.ZonedDateTime

class TimezoneParsingTest {

    @Test
    fun testParseTimezoneAwareDateTime() {
        // Test the same logic as in MainActivity
        val timestampWithTimezone = "2025-09-12T19:30:00+09:00"
        
        val result = if (timestampWithTimezone.contains("+") || timestampWithTimezone.contains("Z")) {
            // Parse timezone-aware format and convert to local time
            ZonedDateTime.parse(timestampWithTimezone).toLocalDateTime()
        } else {
            // Parse basic ISO format
            LocalDateTime.parse(timestampWithTimezone)
        }
        
        // Verify it parses successfully
        assertNotNull(result)
        assertEquals(2025, result.year)
        assertEquals(9, result.monthValue)
        assertEquals(12, result.dayOfMonth)
        assertEquals(19, result.hour)
        assertEquals(30, result.minute)
    }

    @Test
    fun testParseBasicDateTime() {
        // Test basic ISO format without timezone
        val basicTimestamp = "2025-09-12T19:30:00"
        
        val result = if (basicTimestamp.contains("+") || basicTimestamp.contains("Z")) {
            // Parse timezone-aware format and convert to local time
            ZonedDateTime.parse(basicTimestamp).toLocalDateTime()
        } else {
            // Parse basic ISO format
            LocalDateTime.parse(basicTimestamp)
        }
        
        // Verify it parses successfully
        assertNotNull(result)
        assertEquals(2025, result.year)
        assertEquals(9, result.monthValue)
        assertEquals(12, result.dayOfMonth)
        assertEquals(19, result.hour)
        assertEquals(30, result.minute)
    }

    @Test
    fun testParseZuluTime() {
        // Test UTC timezone format
        val zuluTimestamp = "2025-09-12T10:30:00Z"
        
        val result = if (zuluTimestamp.contains("+") || zuluTimestamp.contains("Z")) {
            // Parse timezone-aware format and convert to local time
            ZonedDateTime.parse(zuluTimestamp).toLocalDateTime()
        } else {
            // Parse basic ISO format
            LocalDateTime.parse(zuluTimestamp)
        }
        
        // Verify it parses successfully
        assertNotNull(result)
        assertEquals(2025, result.year)
        assertEquals(9, result.monthValue)
        assertEquals(12, result.dayOfMonth)
        assertEquals(10, result.hour)
        assertEquals(30, result.minute)
    }
}