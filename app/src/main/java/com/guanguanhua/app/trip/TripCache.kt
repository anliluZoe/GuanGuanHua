package com.guanguanhua.app.trip

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.guanguanhua.app.data.TripState
import java.time.LocalDate

object TripCache {
    private const val PREFS = "trips"
    private const val KEY_STATE = "state"
    private const val KEY_REMINDED = "endReminded"

    private val gson = Gson()

    fun read(context: Context): TripState? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_STATE, null) ?: return null
        return runCatching { gson.fromJson(raw, TripState::class.java) }.getOrNull()?.copy(loaded = true)
    }

    fun write(context: Context, state: TripState) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_STATE, gson.toJson(state.copy(loaded = true)))
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { clear() }
    }

    fun lastReminded(context: Context): LocalDate? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_REMINDED, null)
        return raw?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    }

    fun setLastReminded(context: Context, date: LocalDate?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            if (date == null) remove(KEY_REMINDED) else putString(KEY_REMINDED, date.toString())
        }
    }
}
