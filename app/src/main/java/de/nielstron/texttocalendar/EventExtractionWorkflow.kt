package de.nielstron.texttocalendar

import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.ZoneOffset

data class PromptSettings(
    val endpoint: String,
    val apiKey: String,
    val model: String,
    val reasoningEffort: String,
    val forceJson: Boolean,
    val keepLanguageFor: String,
    val autoTranslateTo: String,
)

data class CalendarLaunchData(
    val title: String,
    val description: String,
    val location: String?,
    val startTimeEpochMillis: Long,
    val endTimeEpochMillis: Long?,
)

typealias OpenAiServiceFactory = (String, String) -> OpenAiService

suspend fun sendPromptToModel(
    text: String,
    settings: PromptSettings,
    serviceFactory: OpenAiServiceFactory = { endpoint, apiKey -> OpenAiService(endpoint, apiKey) },
    nowProvider: () -> LocalDateTime = { LocalDateTime.now() },
): String {
    val now = nowProvider()
    val tomorrow = now.plusDays(1)
    val service = serviceFactory(settings.endpoint, settings.apiKey)
    val prompt = """
            You are an expert at extracting calendar event details from a text.
            Provide the result in JSON format with fields: title, summary, location, startTime, endTime (both in ISO 8601 format).
            If a field is not present, omit it from the response. Do not include fields that can not be derived from the text.
            Keep the summary very short and concise, the full original text will also be provided.
            The following languages can be kept for the description: ${settings.keepLanguageFor}.
            Otherwise translate to ${settings.autoTranslateTo}.
            If there is no event in the text, return an empty JSON object.
            Today is $now
            Here are some examples:

            Text:
            ```
            Kosten

            Nettomiete:
            CHF 2’369.– 
            Nebenkosten:
            CHF 160.– 
            Miete:
            CHF 2’529.– 

            Beschreibung:

            An bester Lage im Kreis 3 - Wunderschöne 3.5-Zimmer-Wohnung bietet Wohnkomfort und Eigentumswohnungsstandard in einem:
            Weitere 2.5 - Zimmer-Wohnungen ab 42 m2 im gleichen Haus, Miete ab: 1'890.00 pro Monat
            Wo? - Idastrasse 23, 8003 Zürich
            Ab Wann? - 01.10.2024
            Interessiert an einer Besichtigung?
            Besichtigung: Donnerstag, 15.08.2024 um 16:00 Uhr
            Merkmale:
            Top Lage, nahe pulsierendem Idaplatz
            Beschtigung: Donnerstag, 15.08.2024 um 16:00 Uhr. Wir bitten Sie um eine Voranmeldung per Mail an: malag.ag@bluewin.ch.
            Haustiere wie Hund und Katze sind leider nicht erlaubt.
            Für eine sichere Bewerbung, bitten wir Sie um Auszug aus dem Betreibungsregister nicht älter als 3 Monate.
            ```
            Extracted JSON:
            {
            "title": "Besichtigung Idastrasse",
            "summary": "3.5 Wohnung in Kreis 3. Miete: CHF 2529.",
            "startTime": "2024-08-15T16:00:10",
            "location": "Idastrasse 23, 8003 Zürich"
            }

            Text:
            ```
            Hey everyone, I would like to invite you for a chill BBQ tomorrow, around 7 at my place? Until 10?
            Best Max
            ```
            Extracted JSON:
            {
            "title": "BBQ with Max",
            "location": "Max place",
            "startTime": "${tomorrow}T19:00:00"
            "endTime": "${tomorrow}T22:00:00"
            }
            
            Text:
            ```
            Hey dawg whats up
            ```
            Extracted JSON:
            {}
            
            Text:
            ```
            ♥️🔥❤️‍🔥We are international friends❤️‍🔥🔥♥️
            Photo of ♥️🔥❤️‍🔥We are international friends❤️‍🔥🔥♥️ group
            4.7
            32 ratings
            Friday, September 12, 2025
            7:30 PM to 11:30 PM KST

            Every week on Friday until September 18, 2025
            Mike's cabin
            mapo sogyo-dong, 358-110 · seoul
            ```
            Extracted JSON:
            {
              "title": "We are international friends",
              "summary": "Friday meetup with international friends",
              "location": "Mike's cabin, mapo sogyo-dong, 358-110, seoul",
              "startTime": "2025-09-12T19:30:00+09:00",
              "endTime": "2025-09-12T23:30:00+09:00"
            }

            Text:
            ```
            $text
            ```
        """.trimIndent()

    return service.chatCompletion(
        model = settings.model,
        prompt = prompt,
        reasoning_effort = settings.reasoningEffort,
        forceJson = settings.forceJson
    )
}

fun parseEventResponse(
    response: String,
    originalText: String,
    descriptionFormatter: (String, String) -> String,
    nowProvider: () -> LocalDateTime = { LocalDateTime.now() },
): ProperEvent {
    val trimmed = response.trim()
    if (trimmed.isEmpty()) {
        throw IllegalStateException("Model response is empty.")
    }
    if (trimmed == "{}") {
        throw IllegalStateException("Model did not find any event in the text.")
    }

    val rawEvent = try {
        Json.decodeFromString<RawEvent>(response)
    } catch (e: Exception) {
        throw IllegalArgumentException("Failed to parse API response: ${e.message}", e)
    }

    val fullSummary = descriptionFormatter(rawEvent.summary, originalText)
    val now = nowProvider()

    val startTime = try {
        rawEvent.startTime?.let { DateTimeParser.toLocalDateTime(it) } ?: now
    } catch (e: Exception) {
        now
    }

    val endTime = try {
        rawEvent.endTime?.let { DateTimeParser.toLocalDateTime(it) }
    } catch (e: Exception) {
        null
    }

    return ProperEvent(
        title = rawEvent.title,
        description = fullSummary,
        location = rawEvent.location,
        startTime = startTime,
        endTime = endTime,
    )
}

fun buildCalendarLaunchData(
    event: ProperEvent,
    zoneOffsetProvider: () -> ZoneOffset = {
        ZoneOffset.systemDefault().rules.getOffset(LocalDateTime.now())
    },
): CalendarLaunchData {
    val offset = zoneOffsetProvider()
    val startMillis = event.startTime.toEpochSecond(offset) * 1000
    val endMillis = event.endTime?.toEpochSecond(offset)?.times(1000)

    return CalendarLaunchData(
        title = event.title,
        description = event.description,
        location = event.location,
        startTimeEpochMillis = startMillis,
        endTimeEpochMillis = endMillis,
    )
}
