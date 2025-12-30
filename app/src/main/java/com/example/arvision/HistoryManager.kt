package com.example.arvision

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


enum class HistoryEventType {
    OBJECT_DESCRIBED,
    TEXT_SELECTED,
    TEXT_COPIED,
    TEXT_TRANSLATED,
    TEXT_SHARED,
    TEXT_SEARCHED,
    DISTANCE_MEASURED
}

// Note: This class needs a no-argument constructor for Firestore deserialization.
// Providing default values for all properties achieves this.
data class HistoryItem(
    val eventType: HistoryEventType = HistoryEventType.OBJECT_DESCRIBED,
    val data: String = "", // e.g., The object name, the copied text, etc.
    val timestamp: Long = System.currentTimeMillis()
)

object HistoryManager {

    private const val PREFS_NAME = "history_prefs"
    private const val KEY_HISTORY_LIST = "history_list"
    private val gson = Gson()

    private fun getPreferences(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getHistory(context: Context): MutableList<HistoryItem> {
        val prefs = getPreferences(context)
        val json = prefs.getString(KEY_HISTORY_LIST, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<HistoryItem>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    fun addHistoryItem(context: Context, item: HistoryItem) {
        val history = getHistory(context)
        history.add(0, item) // Add new items to the top
        saveHistory(context, history)
        DatabaseManager.saveHistoryItem(item) // Also save to Firestore
    }

    fun saveHistory(context: Context, history: List<HistoryItem>) {
        val editor = getPreferences(context).edit()
        val json = gson.toJson(history)
        editor.putString(KEY_HISTORY_LIST, json)
        editor.apply()
    }

    fun clearHistory(context: Context) {
        val editor = getPreferences(context).edit()
        editor.remove(KEY_HISTORY_LIST)
        editor.apply()
    }
}
