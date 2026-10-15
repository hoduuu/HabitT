package com.habitdiary.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habitdiary.app.render.CardRenderer

@Composable
fun HabitDiaryApp(vm: MainViewModel, renderer: CardRenderer) {
    val state by vm.state.collectAsStateWithLifecycle()
    var editTarget by remember { mutableStateOf<EditTarget?>(null) }

    MainScreen(
        state = state,
        renderer = renderer,
        onPrev = vm::previousMonth,
        onNext = vm::nextMonth,
        onDayTap = vm::onDayTapped,
        onAdd = { editTarget = EditTarget.New },
        onSettings = {},
        onEdit = { editTarget = EditTarget.Existing(it) },
        onReorder = vm::reorder,
    )

    editTarget?.let { target ->
        val existingId = (target as? EditTarget.Existing)?.habit?.id
        HabitEditSheet(
            target = target,
            onSave = { name, emoji, color -> vm.saveHabit(existingId, name, emoji, color); editTarget = null },
            onDelete = { existingId?.let(vm::deleteHabit); editTarget = null },
            onDismiss = { editTarget = null },
        )
    }
}
