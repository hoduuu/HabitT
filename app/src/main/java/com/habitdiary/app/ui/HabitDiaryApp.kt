package com.habitdiary.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habitdiary.app.render.CardRenderer

@Composable
fun HabitDiaryApp(vm: MainViewModel, renderer: CardRenderer) {
    val state by vm.state.collectAsStateWithLifecycle()
    MainScreen(
        state = state,
        renderer = renderer,
        onPrev = vm::previousMonth,
        onNext = vm::nextMonth,
        onDayTap = vm::onDayTapped,
        onAdd = {},
        onSettings = {},
    )
}
