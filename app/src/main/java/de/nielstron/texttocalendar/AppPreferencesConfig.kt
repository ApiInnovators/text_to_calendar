package de.nielstron.texttocalendar

import android.content.Context
import android.content.res.Resources

/**
 * Centralized configuration for SharedPreferences keys and defaults.
 */
object AppPreferencesConfig {
    const val PREF_FILE = "MyAppPrefs"

    const val KEY_FORCE_JSON = "forceJson"
    const val KEY_AUTO_TRANSLATE_TO = "autoTranslateTo"
    const val KEY_KEEP_LANGUAGE_FOR = "keepLanguageFor"
    const val KEY_ENDPOINT = "endpoint"
    const val KEY_MODEL = "model"
    const val KEY_API_KEY = "apiKey"
    const val KEY_REASONING_EFFORT = "reasoning_effort"

    const val DEFAULT_ENDPOINT = "https://api.openai.com/v1/"
    const val DEFAULT_MODEL = "gpt-5-nano"
    const val DEFAULT_FORCE_JSON = true
    const val DEFAULT_REASONING_EFFORT = "low"

    fun defaultKeepLanguageFor(resources: Resources = Resources.getSystem()): String {
        val locales = resources.configuration.locales
        return locales.toLanguageTags()
    }

    fun defaultAutoTranslateTo(resources: Resources = Resources.getSystem()): String {
        val locales = resources.configuration.locales
        return if (!locales.isEmpty) {
            locales[0].displayLanguage
        } else {
            "English"
        }
    }

    fun getSharedPreferences(context: Context) =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
}
