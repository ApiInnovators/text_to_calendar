package de.nielstron.texttocalendar

import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
// import dev.langchain4j.data.message.UserMessage
// import dev.langchain4j.model.openai.OpenAiChatModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

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

        // initialize ad
        CoroutineScope(Dispatchers.IO).launch {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(this@MainActivity) {}
        }
        val adView = findViewById<com.google.android.gms.ads.AdView>(R.id.adView)
        // Start loading the ad in the background.
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)


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
            Log.e("MainActivity", "Exception in event extraction: ${exception.message}", exception)
            MainScope().launch {
                val errorMessage = exception.message ?: "Unknown error"
                Log.e("MainActivity", "Displaying error to user: $errorMessage")
                errorText.setText(getString(R.string.error_trying_to_create_event, errorMessage))
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
        Log.d("MainActivity", "Starting event extraction for text: ${text.take(100)}...")
        if (text.isEmpty()) {
            throw Exception(getString(R.string.no_text_entered))
        }
        val sharedPrefs = AppPreferencesConfig.getSharedPreferences(this)
        val endpoint = sharedPrefs.getString(AppPreferencesConfig.KEY_ENDPOINT, AppPreferencesConfig.DEFAULT_ENDPOINT)
        val defaultKey = if (endpoint == AppPreferencesConfig.DEFAULT_ENDPOINT) BuildConfig.DEFAULT_API_KEY else null
        val apiKey = sharedPrefs.getString(AppPreferencesConfig.KEY_API_KEY, null) ?: defaultKey
        Log.d("MainActivity", "Using endpoint: $endpoint")
        Log.d("MainActivity", "API key available: ${!apiKey.isNullOrEmpty()}")

        if (apiKey.isNullOrEmpty()) {
            throw Exception("No API key available. Please set up your OpenAI API key.")
        }

        val keepLangsDefault = AppPreferencesConfig.defaultKeepLanguageFor(resources)
        val autoTranslateToDefault = AppPreferencesConfig.defaultAutoTranslateTo(resources)
        val keepLangs = sharedPrefs.getString(AppPreferencesConfig.KEY_KEEP_LANGUAGE_FOR, keepLangsDefault) ?: keepLangsDefault
        val autoTranslateTo = sharedPrefs.getString(AppPreferencesConfig.KEY_AUTO_TRANSLATE_TO, autoTranslateToDefault) ?: autoTranslateToDefault
        val model = sharedPrefs.getString(AppPreferencesConfig.KEY_MODEL, AppPreferencesConfig.DEFAULT_MODEL)!!
        val reasoningEffort = sharedPrefs.getString(AppPreferencesConfig.KEY_REASONING_EFFORT, AppPreferencesConfig.DEFAULT_REASONING_EFFORT)
            ?: AppPreferencesConfig.DEFAULT_REASONING_EFFORT
        val forceJson = sharedPrefs.getBoolean(AppPreferencesConfig.KEY_FORCE_JSON, AppPreferencesConfig.DEFAULT_FORCE_JSON)

        val promptSettings = PromptSettings(
            endpoint = endpoint ?: AppPreferencesConfig.DEFAULT_ENDPOINT,
            apiKey = apiKey,
            model = model,
            reasoningEffort = reasoningEffort,
            forceJson = forceJson,
            keepLanguageFor = keepLangs,
            autoTranslateTo = autoTranslateTo,
        )

        val response = try {
            Log.d("MainActivity", "Making API call with model: $model and reasoning effort: $reasoningEffort")
            sendPromptToModel(text, promptSettings)
        } catch (e: Exception) {
            Log.e("MainActivity", "API call failed: ${e.message}", e)
            throw Exception("OpenAI API call failed: ${e.message}")
        }

        val properEvent = try {
            Log.d("MainActivity", "Parsing API response")
            parseEventResponse(
                response = response,
                originalText = text,
                descriptionFormatter = { summary, original ->
                    getString(R.string.full_description, summary, original)
                }
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to parse API response: ${e.message}", e)
            Log.e("MainActivity", "Raw response was: $response")
            throw e
        }
        Log.d("MainActivity", "ProperEvent created successfully: $properEvent")

        val launchData = buildCalendarLaunchData(properEvent)

        try {
            Log.d("MainActivity", "Opening calendar with event...")
            openCalendarAddEvent(launchData)
            Log.d("MainActivity", "Calendar opened successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to open calendar: ${e.message}", e)
            throw Exception("Failed to open calendar: ${e.message}")
        }
    }

    private fun openCalendarAddEvent(launchData: CalendarLaunchData) {
        Log.d("MainActivity", "Creating calendar intent for event: ${launchData.title}")

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, launchData.title)
            putExtra(CalendarContract.Events.DESCRIPTION, launchData.description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, launchData.startTimeEpochMillis)
            if (launchData.endTimeEpochMillis != null) {
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, launchData.endTimeEpochMillis)
            }
            if (launchData.location != null) {
                putExtra(CalendarContract.Events.EVENT_LOCATION, launchData.location)
            }
        }
        
        Log.d("MainActivity", "Intent extras: ${intent.extras}")
        Log.d("MainActivity", "Starting calendar activity...")
        
        try {
            startActivity(intent)
            Log.d("MainActivity", "Calendar activity started successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start calendar activity: ${e.message}", e)
            throw Exception("Cannot open calendar app: ${e.message}")
        }
    }
}
