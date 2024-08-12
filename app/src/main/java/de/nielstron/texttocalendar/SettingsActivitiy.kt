package de.nielstron.texttocalendar

import android.content.res.Resources
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class SettingsActivitiy : AppCompatActivity() {
    private lateinit var forceJsonSwitch: Switch
    private lateinit var autoTranslateTo: EditText
    private lateinit var keepLanguageFor: EditText
    private lateinit var endpointEdit: EditText
    private lateinit var modelEdit: EditText
    private lateinit var apiKeyEdit: EditText
    private lateinit var resetButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)


        forceJsonSwitch = findViewById(R.id.forceJsonSwitch)
        autoTranslateTo = findViewById(R.id.autoTranslateTo)
        keepLanguageFor = findViewById(R.id.keepLanguageFor)
        endpointEdit = findViewById(R.id.apiEndpoint)
        modelEdit = findViewById(R.id.modelEdit)
        apiKeyEdit = findViewById(R.id.userApiKey)
        resetButton = findViewById(R.id.resetButton)

        // Load current settings
        loadSettings()

        fun triggerSave() {
            saveSettings()
        }

        for (view in listOf(forceJsonSwitch, autoTranslateTo, keepLanguageFor, endpointEdit, apiKeyEdit, modelEdit)) {
            view.setOnFocusChangeListener { _, _ -> triggerSave() }
        }
        resetButton.setOnClickListener {
            resetSettings()
        }
    }

    override fun onDestroy() {
        saveSettings()
        super.onDestroy()
    }

    private fun loadSettings() {
        val userConfig = Resources.getSystem().configuration
        val userLangs = userConfig.locales.toLanguageTags()
        val userLang = userConfig.locales[0].displayLanguage
        val sharedPrefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        forceJsonSwitch.isChecked = sharedPrefs.getBoolean("forceJson", true)
        autoTranslateTo.setText(sharedPrefs.getString("autoTranslateTo", userLang))
        keepLanguageFor.setText(sharedPrefs.getString("keepLanguageFor", userLangs))
        modelEdit.setText(sharedPrefs.getString("model", "gpt-4o-mini"))
        endpointEdit.setText(sharedPrefs.getString("endpoint", "https://api.openai.com/v1"))
        apiKeyEdit.setText(sharedPrefs.getString("apiKey", null))
    }

    private fun saveSettings() {
        val sharedPrefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val apiKeyContent = apiKeyEdit.text.toString()
        with(sharedPrefs.edit()) {
            putBoolean("forceJson", forceJsonSwitch.isChecked)
            putString("autoTranslateTo", autoTranslateTo.text.toString())
            putString("keepLanguageFor", keepLanguageFor.text.toString())
            putString("model", modelEdit.text.toString())
            putString("endpoint", endpointEdit.text.toString())
            putString("apiKey", if (apiKeyContent.equals("null") || apiKeyContent.equals("")) null else apiKeyContent)
            apply()
        }
    }

    fun resetSettings() {
        val sharedPrefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val userConfig = Resources.getSystem().configuration
        val userLangs = userConfig.locales.toLanguageTags()
        val userLang = userConfig.locales[0].displayLanguage
        with(sharedPrefs.edit()) {
            putBoolean("forceJson", true)
            putString("autoTranslateTo", userLang)
            putString("keepLanguageFor", userLangs)
            putString("endpoint", "https://api.openai.com/v1")
            putString("model", "gpt-4o-mini")
            putString("apiKey", null)
            apply()
        }
        loadSettings()
    }
}