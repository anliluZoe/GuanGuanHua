package com.guanguanhua.app.cycle

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.guanguanhua.app.data.CycleState
import java.time.LocalDate

/** 上次同步成功的周期，按成员 id 存。换身份或退出时整段清掉。 */
object CycleCache {
    private const val PREFS = "cycle"
    private const val KEY_STATE = "state"
    private const val KEY_NOTIFIED = "notifiedStart"

    private val gson = Gson()

    fun read(context: Context): CycleState? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_STATE, null) ?: return null
        val state = runCatching { gson.fromJson(raw, CycleState::class.java) }.getOrNull() ?: return null
        if (state.memberId == 0L) return null
        return state.copy(loaded = true)
    }

    fun write(context: Context, state: CycleState) {
        if (state.memberId == 0L) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_STATE, gson.toJson(state))
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { clear() }
    }

    fun notifiedStart(context: Context): LocalDate? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_NOTIFIED, null)
        return CycleMath.parse(raw)
    }

    fun setNotifiedStart(context: Context, start: LocalDate?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            if (start == null) remove(KEY_NOTIFIED) else putString(KEY_NOTIFIED, start.toString())
        }
    }
}
