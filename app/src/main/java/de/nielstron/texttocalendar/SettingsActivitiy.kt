package de.nielstron.texttocalendar

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
        val sharedPrefs = AppPreferencesConfig.getSharedPreferences(this)
        val defaultAutoTranslate = AppPreferencesConfig.defaultAutoTranslateTo(resources)
        val defaultKeepLanguageFor = AppPreferencesConfig.defaultKeepLanguageFor(resources)
        forceJsonSwitch.isChecked = sharedPrefs.getBoolean(AppPreferencesConfig.KEY_FORCE_JSON, AppPreferencesConfig.DEFAULT_FORCE_JSON)
        autoTranslateTo.setText(sharedPrefs.getString(AppPreferencesConfig.KEY_AUTO_TRANSLATE_TO, defaultAutoTranslate))
        keepLanguageFor.setText(sharedPrefs.getString(AppPreferencesConfig.KEY_KEEP_LANGUAGE_FOR, defaultKeepLanguageFor))
        modelEdit.setText(sharedPrefs.getString(AppPreferencesConfig.KEY_MODEL, AppPreferencesConfig.DEFAULT_MODEL))
        endpointEdit.setText(sharedPrefs.getString(AppPreferencesConfig.KEY_ENDPOINT, AppPreferencesConfig.DEFAULT_ENDPOINT))
        apiKeyEdit.setText(sharedPrefs.getString(AppPreferencesConfig.KEY_API_KEY, null))
    }

    private fun saveSettings() {
        val sharedPrefs = AppPreferencesConfig.getSharedPreferences(this)
        val apiKeyContent = apiKeyEdit.text.toString()
        with(sharedPrefs.edit()) {
            putBoolean(AppPreferencesConfig.KEY_FORCE_JSON, forceJsonSwitch.isChecked)
            putString(AppPreferencesConfig.KEY_AUTO_TRANSLATE_TO, autoTranslateTo.text.toString())
            putString(AppPreferencesConfig.KEY_KEEP_LANGUAGE_FOR, keepLanguageFor.text.toString())
            putString(AppPreferencesConfig.KEY_MODEL, modelEdit.text.toString())
            putString(AppPreferencesConfig.KEY_ENDPOINT, endpointEdit.text.toString())
            putString(AppPreferencesConfig.KEY_API_KEY, if (apiKeyContent.equals("null") || apiKeyContent.equals("")) null else apiKeyContent)
            apply()
        }
    }

    fun resetSettings() {
        val sharedPrefs = AppPreferencesConfig.getSharedPreferences(this)
        val defaultAutoTranslate = AppPreferencesConfig.defaultAutoTranslateTo(resources)
        val defaultKeepLanguageFor = AppPreferencesConfig.defaultKeepLanguageFor(resources)
        with(sharedPrefs.edit()) {
            putBoolean(AppPreferencesConfig.KEY_FORCE_JSON, AppPreferencesConfig.DEFAULT_FORCE_JSON)
            putString(AppPreferencesConfig.KEY_AUTO_TRANSLATE_TO, defaultAutoTranslate)
            putString(AppPreferencesConfig.KEY_KEEP_LANGUAGE_FOR, defaultKeepLanguageFor)
            putString(AppPreferencesConfig.KEY_ENDPOINT, AppPreferencesConfig.DEFAULT_ENDPOINT)
            putString(AppPreferencesConfig.KEY_MODEL, AppPreferencesConfig.DEFAULT_MODEL)
            putString(AppPreferencesConfig.KEY_API_KEY, null)
            apply()
        }
        loadSettings()
    }
}
