package de.nielstron.texttocalendar

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

/**
 * Utility for converting ISO timestamps (with or without timezone offsets) into LocalDateTime.
 */
object DateTimeParser {
    fun toZonedDateTime(
        timestamp: String,
        defaultZoneProvider: () -> ZoneId = { ZoneId.systemDefault() },
    ): ZonedDateTime {
        return try {
            ZonedDateTime.parse(timestamp)
        } catch (e: DateTimeParseException) {
            LocalDateTime.parse(timestamp).atZone(defaultZoneProvider())
        }
    }
}
