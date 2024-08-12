package de.nielstron.texttocalendar

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.provider.CalendarContract
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.openai.OpenAiChatModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.ZoneOffset

@Serializable
data class RawEvent(
    val title: String = "Event",
    val summary: String = "",
    val location: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
)

data class ProperEvent(
    val title: String,
    val description: String,
    val startTime: LocalDateTime,
    val location: String? = null,
    val endTime: LocalDateTime? = null,
)


class MainActivity : AppCompatActivity() {

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivitiy::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val backbut = findViewById<Button>(R.id.createEventButton)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val enteredText = findViewById<EditText>(R.id.editTextText)
        val errorText = findViewById<TextView>(R.id.errorText)
        backbut.setOnClickListener {
            extractEventAndOpenCalendar(enteredText.text.toString(), backbut, progressBar, errorText)
        }
        // Check if the app was launched from a share intent
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                enteredText.setText(sharedText)
                extractEventAndOpenCalendar(sharedText, backbut, progressBar, errorText)
            } else {
                errorText.setText(getString(R.string.error_no_shared_text))
            }
        }
        else if (intent?.action == Intent.ACTION_PROCESS_TEXT) {
            val sharedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT).toString()
            enteredText.setText(sharedText)
            extractEventAndOpenCalendar(sharedText, backbut, progressBar, errorText)
        }
    }


    private fun extractEventAndOpenCalendar(text: String, button: Button, progressBar: ProgressBar, errorText: TextView) {
        fun resetButton() {
            button.isClickable = true
            button.isActivated = false
            progressBar.visibility = ProgressBar.INVISIBLE
        }
        val handler = CoroutineExceptionHandler { _, exception ->
            MainScope().launch {
                errorText.setText(getString(R.string.error_trying_to_create_event, exception.message))
                resetButton()
            }
        }
        errorText.setText("")
        button.isClickable = false
        button.isActivated = true
        progressBar.visibility = ProgressBar.VISIBLE
        lifecycleScope.launch(handler + Dispatchers.IO) {
            _extractEventAndOpenCalendar(text)
            MainScope().launch {
                resetButton()
            }
        }
    }

    private suspend fun _extractEventAndOpenCalendar(text: String) {
        if (text.isEmpty()) {
            throw Exception(getString(R.string.no_text_entered))
        }
        val sharedPrefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val defaultEndpoint = "https://api.openai.com/v1"
        val endpoint = sharedPrefs.getString("endpoint", defaultEndpoint)
        val defaultKey = (if (endpoint.equals(defaultEndpoint)) BuildConfig.DEFAULT_API_KEY else null)
        val apiKey = sharedPrefs.getString("apiKey", null) ?: defaultKey
        var modelBuilder = OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName(sharedPrefs.getString("model", "gpt-4o-mini"))
            .baseUrl(endpoint)
        if(sharedPrefs.getBoolean("forceJson", true)){
            modelBuilder = modelBuilder.responseFormat("json_object")
        }
        val model = modelBuilder.build()

        // Get the current date in ISO 8601 format
        val date = LocalDateTime.now()
        val today = date.toString()
        val tomorrow = date.plusDays(1).toString()

        // get langs settings
        val userConfig = Resources.getSystem().configuration
        val userLangs = userConfig.locales.toLanguageTags()
        val userLang = userConfig.locales[0].displayLanguage
        val keepLangs = sharedPrefs.getString("keepLanguageFor", userLangs)
        val autoTranslateTo = sharedPrefs.getString("autoTranslateTo", userLang)

        val prompt = """
            You are an expert at extracting calendar event details from a text.
            Provide the result in JSON format with fields: title, summary, location, startTime, endTime (both in ISO 8601 format).
            If a field is not present, omit it from the response. Do not include fields that can not be derived from the text.
            Keep the summary very short and concise, the full original text will also be provided.
            The following languages can be kept for the description: ${keepLangs}.
            Otherwise translate to ${autoTranslateTo}.
            If there is no event in the text, return an empty JSON object.
            Today is $today
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
            $text
            ```
        """.trimIndent()

        val response: String = model.generate(UserMessage(prompt)).content().text()
        if (response.trim().equals("{}")) {
            throw Exception(getString(R.string.no_value_found))
        }
        val parsedEvent = Json.decodeFromString<RawEvent>(response)

        // post-process the event
        val fullSummary = getString(R.string.full_description, parsedEvent.summary, text)
        var startTime: LocalDateTime;
        try {
            startTime = LocalDateTime.parse(parsedEvent.startTime);
        } catch (e: Exception) {
            // If the start time is not provided or parseable, use the current time
            startTime = LocalDateTime.now();
        }
        var endTime: LocalDateTime?;
        try {
            endTime = LocalDateTime.parse(parsedEvent.endTime);
        } catch (e: Exception) {
            // If the end time is not provided or parseable, use the start time
            endTime = null;
        }

        val properEvent = ProperEvent(
            title = parsedEvent.title,
            description = fullSummary,
            location = parsedEvent.location,
            startTime = startTime,
            endTime = endTime,
        )

        // Open the calendar app with the event details
        openCalendarAddEvent(properEvent)
    }

    private fun openCalendarAddEvent(parsedEvent: ProperEvent) {
        val localTimeOffset = ZoneOffset.systemDefault().rules.getOffset(LocalDateTime.now())
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, parsedEvent.title)
            putExtra(CalendarContract.Events.DESCRIPTION, parsedEvent.description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, parsedEvent.startTime.toEpochSecond(localTimeOffset) * 1000)
            if (parsedEvent.endTime != null) {
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, parsedEvent.endTime.toEpochSecond(localTimeOffset) * 1000)
            }
            if (parsedEvent.location != null) {
                putExtra(CalendarContract.Events.EVENT_LOCATION, parsedEvent.location)
            }
        }
        startActivity(intent)
    }
}
