package de.nielstron.texttocalendar

import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

/**
 * Utility for converting ISO timestamps (with or without timezone offsets) into LocalDateTime.
 */
object DateTimeParser {
    fun toLocalDateTime(timestamp: String): LocalDateTime {
        return try {
            ZonedDateTime.parse(timestamp).toLocalDateTime()
        } catch (e: DateTimeParseException) {
            LocalDateTime.parse(timestamp)
        }
    }
}
