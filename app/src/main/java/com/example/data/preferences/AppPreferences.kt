package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("au_notes_prefs", Context.MODE_PRIVATE)

    companion object {
        val DEFAULT_API_KEY: String
            get() = com.example.BuildConfig.GEMINI_API_KEY.takeUnless {
                it.isBlank() || it == "MY_GEMINI_API_KEY" || it == "YOUR_API_KEY_HERE"
            } ?: ""
        // Empty means first-time security setup is required. Never ship a universal PIN.
        const val DEFAULT_PIN = ""
        const val DEFAULT_MODEL = "gemini-2.0-flash"
        const val DEFAULT_SECURITY_QUESTION = ""
    }

    private val _blurApis = MutableStateFlow(prefs.getBoolean("blur_apis", true))
    val blurApis: StateFlow<Boolean> = _blurApis.asStateFlow()

    private val _syntaxHighlight = MutableStateFlow(prefs.getBoolean("syntax_highlight", true))
    val syntaxHighlight: StateFlow<Boolean> = _syntaxHighlight.asStateFlow()

    private val _lockPin = MutableStateFlow(prefs.getString("lock_pin", DEFAULT_PIN) ?: DEFAULT_PIN)
    val lockPin: StateFlow<String> = _lockPin.asStateFlow()

    private val _hasCustomPin = MutableStateFlow(prefs.getBoolean("has_custom_pin", false))
    val hasCustomPin: StateFlow<Boolean> = _hasCustomPin.asStateFlow()

    private val _securityQuestion = MutableStateFlow(prefs.getString("sec_question", DEFAULT_SECURITY_QUESTION) ?: DEFAULT_SECURITY_QUESTION)
    val securityQuestion: StateFlow<String> = _securityQuestion.asStateFlow()

    private val _securityAnswer = MutableStateFlow(prefs.getString("sec_answer", "") ?: "")
    val securityAnswer: StateFlow<String> = _securityAnswer.asStateFlow()

    private val _securityAutoLock = MutableStateFlow(prefs.getString("security_auto_lock", "leaving") ?: "leaving")
    val securityAutoLock: StateFlow<String> = _securityAutoLock.asStateFlow()

    private val _blinkAlarmIndicator = MutableStateFlow(prefs.getBoolean("blink_alarm_indicator", true))
    val blinkAlarmIndicator: StateFlow<Boolean> = _blinkAlarmIndicator.asStateFlow()
    private val _activeAlarm = MutableStateFlow(prefs.getBoolean("active_alarm", false))
    val activeAlarm: StateFlow<Boolean> = _activeAlarm.asStateFlow()
    private val _alarmTitle = MutableStateFlow(prefs.getString("alarm_title", "") ?: "")
    val alarmTitle: StateFlow<String> = _alarmTitle.asStateFlow()
    private val _alarmTime = MutableStateFlow(prefs.getLong("alarm_time", 0L))
    val alarmTime: StateFlow<Long> = _alarmTime.asStateFlow()
    private val _alarmRingtone = MutableStateFlow(prefs.getString("alarm_ringtone", "") ?: "")
    val alarmRingtone: StateFlow<String> = _alarmRingtone.asStateFlow()

    private val _lockedFolders = MutableStateFlow(prefs.getStringSet("locked_folders", emptySet()) ?: emptySet())
    val lockedFolders: StateFlow<Set<String>> = _lockedFolders.asStateFlow()

    private val _hiddenFolders = MutableStateFlow(prefs.getStringSet("hidden_folders", emptySet()) ?: emptySet())
    val hiddenFolders: StateFlow<Set<String>> = _hiddenFolders.asStateFlow()

    private val _customFolders = MutableStateFlow(prefs.getStringSet("custom_folders", emptySet()) ?: emptySet())
    val customFolders: StateFlow<Set<String>> = _customFolders.asStateFlow()

    private val defaultFolders = listOf("All Notes", "Favorites", "APIs Keys", "Code", "Media", "Personal")
    private val _folderOrder = MutableStateFlow(
        prefs.getString("folder_order", null)?.split(",")?.filter { it.isNotBlank() } ?: defaultFolders
    )
    val folderOrder: StateFlow<List<String>> = _folderOrder.asStateFlow()

    private val _useInbuiltApi = MutableStateFlow(prefs.getBoolean("use_inbuilt_api", true))
    val useInbuiltApi: StateFlow<Boolean> = _useInbuiltApi.asStateFlow()

    private val _userApiKey = MutableStateFlow(prefs.getString("user_api_key", "") ?: "")
    val userApiKey: StateFlow<String> = _userApiKey.asStateFlow()

    private val _selectedModel = MutableStateFlow(prefs.getString("selected_model", DEFAULT_MODEL) ?: DEFAULT_MODEL)
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun setBlurApis(value: Boolean) {
        prefs.edit().putBoolean("blur_apis", value).apply()
        _blurApis.value = value
    }

    fun setSyntaxHighlight(value: Boolean) {
        prefs.edit().putBoolean("syntax_highlight", value).apply()
        _syntaxHighlight.value = value
    }

    fun setLockPin(pin: String) {
        prefs.edit().putString("lock_pin", pin).putBoolean("has_custom_pin", true).apply()
        _lockPin.value = pin
        _hasCustomPin.value = true
    }

    fun hasSecuritySetup(): Boolean = _hasCustomPin.value && _lockPin.value.length == 4 && _securityAnswer.value.isNotBlank() && _securityQuestion.value.isNotBlank()

    fun setSecurityAutoLock(value: String) {
        prefs.edit().putString("security_auto_lock", value).apply()
        _securityAutoLock.value = value
    }

    fun setBlinkAlarmIndicator(value: Boolean) {
        prefs.edit().putBoolean("blink_alarm_indicator", value).apply()
        _blinkAlarmIndicator.value = value
    }

    fun setActiveAlarm(title: String, triggerTime: Long, ringtone: String) {
        prefs.edit().putBoolean("active_alarm", true).putString("alarm_title", title).putLong("alarm_time", triggerTime).putString("alarm_ringtone", ringtone).apply()
        _activeAlarm.value = true; _alarmTitle.value = title; _alarmTime.value = triggerTime; _alarmRingtone.value = ringtone
    }

    fun clearActiveAlarm() {
        prefs.edit().putBoolean("active_alarm", false).remove("alarm_title").remove("alarm_time").remove("alarm_ringtone").apply()
        _activeAlarm.value = false; _alarmTitle.value = ""; _alarmTime.value = 0L; _alarmRingtone.value = ""
    }

    fun setSecurityDetails(pin: String, question: String, answer: String) {
        prefs.edit()
            .putString("lock_pin", pin)
            .putBoolean("has_custom_pin", true)
            .putString("sec_question", question)
            .putString("sec_answer", answer.trim().lowercase())
            .apply()
        _lockPin.value = pin
        _hasCustomPin.value = true
        _securityQuestion.value = question
        _securityAnswer.value = answer.trim().lowercase()
    }

    fun verifySecurityAnswer(enteredAnswer: String): Boolean {
        val stored = _securityAnswer.value.trim().lowercase()
        return stored.isNotEmpty() && stored == enteredAnswer.trim().lowercase()
    }

    fun toggleFolderLock(folderName: String) {
        val current = _lockedFolders.value.toMutableSet()
        if (current.contains(folderName)) {
            current.remove(folderName)
        } else {
            current.add(folderName)
        }
        prefs.edit().putStringSet("locked_folders", current).apply()
        _lockedFolders.value = current
    }

    fun isFolderLocked(folderName: String): Boolean {
        return _lockedFolders.value.contains(folderName)
    }

    fun setFolderLocked(folderName: String, locked: Boolean) {
        val current = _lockedFolders.value.toMutableSet()
        if (locked) current.add(folderName) else current.remove(folderName)
        prefs.edit().putStringSet("locked_folders", current).apply()
        _lockedFolders.value = current
    }

    fun setFolderHidden(folderName: String, hidden: Boolean) {
        val current = _hiddenFolders.value.toMutableSet()
        if (hidden) current.add(folderName) else current.remove(folderName)
        prefs.edit().putStringSet("hidden_folders", current).apply()
        _hiddenFolders.value = current
    }

    fun isFolderHidden(folderName: String): Boolean = _hiddenFolders.value.contains(folderName)

    fun addCustomFolder(folderName: String): Boolean {
        val name = folderName.trim()
        if (name.isBlank() || name in defaultFolders || name in _customFolders.value) return false
        val current = _customFolders.value.toMutableSet().apply { add(name) }
        prefs.edit().putStringSet("custom_folders", current).apply()
        _customFolders.value = current
        if (name !in _folderOrder.value) setFolderOrder(_folderOrder.value + name)
        return true
    }

    fun removeCustomFolder(folderName: String) {
        val current = _customFolders.value.toMutableSet().apply { remove(folderName) }
        prefs.edit().putStringSet("custom_folders", current).apply()
        _customFolders.value = current
        setFolderOrder(_folderOrder.value.filterNot { it == folderName })
    }

    fun setFolderOrder(newOrder: List<String>) {
        val joined = newOrder.joinToString(",")
        prefs.edit().putString("folder_order", joined).apply()
        _folderOrder.value = newOrder
    }

    fun removeFolder(folderName: String) {
        val current = _folderOrder.value.toMutableList()
        current.remove(folderName)
        setFolderOrder(current)
    }

    fun setUseInbuiltApi(value: Boolean) {
        prefs.edit().putBoolean("use_inbuilt_api", value).apply()
        _useInbuiltApi.value = value
    }

    fun setUserApiKey(key: String) {
        prefs.edit().putString("user_api_key", key).apply()
        _userApiKey.value = key
    }

    fun setSelectedModel(model: String) {
        prefs.edit().putString("selected_model", model).apply()
        _selectedModel.value = model
    }

    fun setDarkMode(value: Boolean) {
        prefs.edit().putBoolean("is_dark_mode", value).apply()
        _isDarkMode.value = value
    }

    fun getEffectiveApiKey(): String {
        // API Room is the authoritative user-key store. Keep legacy settings as fallback
        // so existing installations continue to work after upgrading.
        val secureKey = try { SecureApiStore(context).get("key") } catch (_: Exception) { "" }
        return secureKey.ifBlank {
            if (_useInbuiltApi.value) DEFAULT_API_KEY else _userApiKey.value.ifBlank { DEFAULT_API_KEY }
        }
    }

    fun getAiProvider(): String = try {
        SecureApiStore(context).get("provider").ifBlank { "Gemini" }
    } catch (_: Exception) { "Gemini" }

    fun getAiBaseUrl(): String = try {
        SecureApiStore(context).get("baseUrl")
    } catch (_: Exception) { "" }

    fun getAiModel(): String = try {
        SecureApiStore(context).get("model").ifBlank { _selectedModel.value }
    } catch (_: Exception) { _selectedModel.value }
}
