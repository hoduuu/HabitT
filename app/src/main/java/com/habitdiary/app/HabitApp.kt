package com.habitdiary.app

import android.app.Application
import android.content.Context
import com.habitdiary.app.data.DataStoreSettings
import com.habitdiary.app.data.HabitDatabase
import com.habitdiary.app.data.HabitStore
import com.habitdiary.app.data.RoomHabitStore
import com.habitdiary.app.data.SettingsStore
import com.habitdiary.app.data.WidgetBindings
import com.habitdiary.app.data.settingsDataStore
import com.habitdiary.app.render.CardFonts
import com.habitdiary.app.render.CardRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    private val app = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val db = HabitDatabase.build(app)
    val store: HabitStore = RoomHabitStore(db.habitDao(), ::changed)
    val settings: SettingsStore = DataStoreSettings(app.settingsDataStore, ::changed)
    val bindings = WidgetBindings(app.settingsDataStore)
    val renderer: CardRenderer by lazy { CardRenderer(CardFonts.load(app)) }

    /** 데이터·설정이 바뀔 때 호출된다. 위젯 갱신은 Task 10에서 연결한다. */
    private fun changed() {}
}

class HabitApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as HabitApp).container
