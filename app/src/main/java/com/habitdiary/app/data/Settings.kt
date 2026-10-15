package com.habitdiary.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.habitdiary.app.style.CardStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

interface SettingsStore {
    val cardStyle: Flow<CardStyle>
    suspend fun setCardStyle(style: CardStyle)
}

class DataStoreSettings(
    private val ds: DataStore<Preferences>,
    private val onChanged: () -> Unit = {},
) : SettingsStore {
    private val styleKey = stringPreferencesKey("card_style")

    override val cardStyle: Flow<CardStyle> = ds.data.map { prefs ->
        prefs[styleKey]?.let { name -> CardStyle.entries.firstOrNull { it.name == name } } ?: CardStyle.INDEX_TAB
    }

    override suspend fun setCardStyle(style: CardStyle) {
        ds.edit { it[styleKey] = style.name }
        onChanged()
    }
}

/** 홈 화면 위젯 id → 습관 id 연결. */
class WidgetBindings(private val ds: DataStore<Preferences>) {
    private fun key(appWidgetId: Int) = longPreferencesKey("widget_$appWidgetId")

    suspend fun bind(appWidgetId: Int, habitId: Long) {
        ds.edit { it[key(appWidgetId)] = habitId }
    }

    suspend fun habitIdFor(appWidgetId: Int): Long? = ds.data.first()[key(appWidgetId)]

    suspend fun unbind(appWidgetId: Int) {
        ds.edit { it.remove(key(appWidgetId)) }
    }
}
