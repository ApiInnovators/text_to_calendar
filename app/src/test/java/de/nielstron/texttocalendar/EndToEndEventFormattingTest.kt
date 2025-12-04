package de.nielstron.texttocalendar

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class EndToEndEventFormattingTest {

    private lateinit var server: MockWebServer
    private lateinit var service: OpenAiService

    private val testModel = AppPreferencesConfig.DEFAULT_MODEL
    private val isoLocalDateRegex = Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")
    private val isoWithOffsetRegex =
        Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(Z|[+-]\\d{2}:\\d{2})")
    private val deterministicZone = ZoneId.of("UTC")

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        service = OpenAiService(server.url("/").toString(), "test-api-key")
    }

    @Test
    fun llmResponseWithCestTimesIsParsedCorrectly() = runBlocking {
        val rawEventJson = """
            {
              "title": "Berlin Product Meetup",
              "summary": "Monthly sync with EU team",
              "location": "Factory Berlin",
              "startTime": "2025-06-10T09:00:00+02:00",
              "endTime": "2025-06-10T11:00:00+02:00"
            }
        """.trimIndent()
        enqueueResponse(rawEventJson)

        val response = service.chatCompletion(
            model = testModel,
            prompt = "A Berlin event",
            reasoning_effort = AppPreferencesConfig.DEFAULT_REASONING_EFFORT
        )

        val parsedEvent = Json.decodeFromString<RawEvent>(response)

        assertTrue(parsedEvent.startTime!!.matches(isoWithOffsetRegex))
        assertTrue(parsedEvent.endTime!!.matches(isoWithOffsetRegex))
        assertEquals(
            ZonedDateTime.of(LocalDateTime.of(2025, 6, 10, 9, 0), ZoneOffset.ofHours(2)),
            DateTimeParser.toZonedDateTime(parsedEvent.startTime!!)
        )
        assertEquals(
            ZonedDateTime.of(LocalDateTime.of(2025, 6, 10, 11, 0), ZoneOffset.ofHours(2)),
            DateTimeParser.toZonedDateTime(parsedEvent.endTime!!)
        )
    }

    @Test
    fun llmResponseWithSingaporeTimeIsParsedCorrectly() = runBlocking {
        val rawEventJson = """
            {
              "title": "Singapore Night Ride",
              "summary": "Cycling with friends",
              "location": "Marina Bay Sands",
              "startTime": "2025-08-01T20:00:00+08:00",
              "endTime": "2025-08-01T23:00:00+08:00"
            }
        """.trimIndent()
        enqueueResponse(rawEventJson)

        val response = service.chatCompletion(
            model = testModel,
            prompt = "Singapore ride",
            reasoning_effort = AppPreferencesConfig.DEFAULT_REASONING_EFFORT
        )

        val parsedEvent = Json.decodeFromString<RawEvent>(response)

        assertTrue(parsedEvent.startTime!!.matches(isoWithOffsetRegex))
        assertTrue(parsedEvent.endTime!!.matches(isoWithOffsetRegex))
        assertEquals(
            ZonedDateTime.of(LocalDateTime.of(2025, 8, 1, 20, 0), ZoneOffset.ofHours(8)),
            DateTimeParser.toZonedDateTime(parsedEvent.startTime!!)
        )
        assertEquals(
            ZonedDateTime.of(LocalDateTime.of(2025, 8, 1, 23, 0), ZoneOffset.ofHours(8)),
            DateTimeParser.toZonedDateTime(parsedEvent.endTime!!)
        )
    }

    @Test
    fun llmResponseWithNewYorkTimeIsParsedCorrectly() = runBlocking {
        val rawEventJson = """
            {
              "title": "NYC Investor Dinner",
              "summary": "Dinner with east coast investors",
              "location": "Gramercy Tavern, NYC",
              "startTime": "2025-03-01T18:00:00-05:00",
              "endTime": "2025-03-01T21:00:00-05:00"
            }
        """.trimIndent()
        enqueueResponse(rawEventJson)

        val response = service.chatCompletion(
            model = testModel,
            prompt = "NYC dinner",
            reasoning_effort = AppPreferencesConfig.DEFAULT_REASONING_EFFORT
        )

        val parsedEvent = Json.decodeFromString<RawEvent>(response)

        assertTrue(parsedEvent.startTime!!.matches(isoWithOffsetRegex))
        assertTrue(parsedEvent.endTime!!.matches(isoWithOffsetRegex))
        assertEquals(
            ZonedDateTime.of(LocalDateTime.of(2025, 3, 1, 18, 0), ZoneOffset.ofHours(-5)),
            DateTimeParser.toZonedDateTime(parsedEvent.startTime!!)
        )
        assertEquals(
            ZonedDateTime.of(LocalDateTime.of(2025, 3, 1, 21, 0), ZoneOffset.ofHours(-5)),
            DateTimeParser.toZonedDateTime(parsedEvent.endTime!!)
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun llmResponseWithTimezoneIsParsedEndToEnd() = runBlocking {
        val rawEventJson = """
            {
              "title": "We are international friends",
              "summary": "Friday meetup with international friends",
              "location": "Mike's cabin, mapo sogyo-dong, 358-110, seoul",
              "startTime": "2025-09-12T19:30:00+09:00",
              "endTime": "2025-09-12T23:30:00+09:00"
            }
        """.trimIndent()
        enqueueResponse(rawEventJson)

        val response = service.chatCompletion(
            model = testModel,
            prompt = "text",
            reasoning_effort = AppPreferencesConfig.DEFAULT_REASONING_EFFORT
        )

        val parsedEvent = Json.decodeFromString<RawEvent>(response)

        assertTrue(parsedEvent.startTime!!.matches(isoWithOffsetRegex))
        assertTrue(parsedEvent.endTime!!.matches(isoWithOffsetRegex))

        val startZoned = DateTimeParser.toZonedDateTime(parsedEvent.startTime!!)
        val endZoned = DateTimeParser.toZonedDateTime(parsedEvent.endTime!!)

        assertEquals(
            ZonedDateTime.of(LocalDateTime.of(2025, 9, 12, 19, 30), ZoneOffset.ofHours(9)),
            startZoned
        )
        assertEquals(
            ZonedDateTime.of(LocalDateTime.of(2025, 9, 12, 23, 30), ZoneOffset.ofHours(9)),
            endZoned
        )

        assertEquals(Duration.ofHours(4), Duration.between(startZoned.toInstant(), endZoned.toInstant()))
    }

    @Test
    fun llmResponseWithLocalTimesIsFormattedCorrectly() = runBlocking {
        val rawEventJson = """
            {
              "title": "BBQ with Max",
              "summary": "Chill BBQ at Max place",
              "location": "Max place",
              "startTime": "2025-01-15T19:00:00",
              "endTime": "2025-01-15T22:00:00"
            }
        """.trimIndent()
        enqueueResponse(rawEventJson)

        val response = service.chatCompletion(
            model = testModel,
            prompt = "text",
            reasoning_effort = AppPreferencesConfig.DEFAULT_REASONING_EFFORT
        )

        val parsedEvent = Json.decodeFromString<RawEvent>(response)

        assertTrue(parsedEvent.startTime!!.matches(isoLocalDateRegex))
        assertTrue(parsedEvent.endTime!!.matches(isoLocalDateRegex))

        val startZoned = DateTimeParser.toZonedDateTime(parsedEvent.startTime!!) { deterministicZone }
        val endZoned = DateTimeParser.toZonedDateTime(parsedEvent.endTime!!) { deterministicZone }

        assertEquals(ZonedDateTime.of(LocalDateTime.of(2025, 1, 15, 19, 0), deterministicZone), startZoned)
        assertEquals(ZonedDateTime.of(LocalDateTime.of(2025, 1, 15, 22, 0), deterministicZone), endZoned)
        assertEquals(Duration.ofHours(3), Duration.between(startZoned, endZoned))
    }

    @Test
    fun llmResponseMatchesLumaInviteFormatting() = runBlocking {
        val rawEventJson = """
            {
              "title": "Day 1 - Synchronous Comms: Co-Working Hub",
              "summary": "Luma coworking day",
              "location": "Cafe Sevilla of San Diego, San Diego, California",
              "startTime": "2025-12-04T11:00:00-08:00",
              "endTime": "2025-12-04T17:00:00-08:00"
            }
        """.trimIndent()
        enqueueResponse(rawEventJson)

        val response = service.chatCompletion(
            model = testModel,
            prompt = """
                You’ve got a spot at
                Day 1 - Synchronous Comms: Co-Working Hub
                Thursday, December 4
                11:00 AM - 5:00 PM PST
                
                Cafe Sevilla of San Diego
                San Diego, California
            """.trimIndent(),
            reasoning_effort = AppPreferencesConfig.DEFAULT_REASONING_EFFORT
        )

        val parsedEvent = Json.decodeFromString<RawEvent>(response)

        assertEquals("Day 1 - Synchronous Comms: Co-Working Hub", parsedEvent.title)
        assertTrue(parsedEvent.startTime!!.matches(isoWithOffsetRegex))
        assertTrue(parsedEvent.endTime!!.matches(isoWithOffsetRegex))

        assertEquals(
            ZonedDateTime.of(LocalDateTime.of(2025, 12, 4, 11, 0), ZoneOffset.ofHours(-8)),
            DateTimeParser.toZonedDateTime(parsedEvent.startTime!!)
        )
        assertEquals(
            ZonedDateTime.of(LocalDateTime.of(2025, 12, 4, 17, 0), ZoneOffset.ofHours(-8)),
            DateTimeParser.toZonedDateTime(parsedEvent.endTime!!)
        )
    }

    private fun enqueueResponse(rawEventJson: String) {
        val openAiResponse = OpenAiResponse(
            choices = listOf(
                Choice(
                    message = Message(
                        role = "assistant",
                        content = rawEventJson
                    )
                )
            )
        )

        val responseBody = Json.encodeToString(OpenAiResponse.serializer(), openAiResponse)
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody)
        )
    }

}
