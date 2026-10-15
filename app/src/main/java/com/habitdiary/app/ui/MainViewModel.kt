package com.habitdiary.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitdiary.app.data.Habit
import com.habitdiary.app.data.HabitStore
import com.habitdiary.app.data.SettingsStore
import com.habitdiary.app.style.CardStyle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class HabitRow(val habit: Habit, val doneDays: Set<Int>)

data class MainUiState(
    val month: YearMonth,
    val today: LocalDate,
    val style: CardStyle,
    val rows: List<HabitRow>,
    val loaded: Boolean,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val store: HabitStore,
    private val settings: SettingsStore,
    private val clock: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.from(clock()))
    private val today = MutableStateFlow(clock())

    // 달과 그 달의 체크를 한 쌍으로 묶어, 달을 넘길 때 이전 달 체크가 잠깐 섞여 보이지 않게 한다.
    private val monthChecks = month.flatMapLatest { m -> store.observeMonthChecks(m).map { m to it } }

    val state: StateFlow<MainUiState> =
        combine(monthChecks, today, settings.cardStyle, store.observeHabits()) { (m, checks), t, style, habits ->
            MainUiState(m, t, style, habits.map { HabitRow(it, checks[it.id] ?: emptySet()) }, loaded = true)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            MainUiState(month.value, today.value, CardStyle.INDEX_TAB, emptyList(), loaded = false),
        )

    fun previousMonth() { month.value = month.value.minusMonths(1) }

    fun nextMonth() { month.value = month.value.plusMonths(1) }

    /** 앱이 다시 보일 때 호출: 자정이 지났으면 오늘을 갱신한다. */
    fun refreshToday() { today.value = clock() }

    fun onDayTapped(habitId: Long, day: Int) {
        val date = month.value.atDay(day)
        if (date.isAfter(clock())) return
        viewModelScope.launch { store.toggle(habitId, date) }
    }

    fun saveHabit(id: Long?, name: String, emoji: String, colorIndex: Int) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val e = emoji.ifBlank { DEFAULT_EMOJI }
        viewModelScope.launch {
            val existing = id?.let { store.getHabit(it) }
            if (existing == null) store.addHabit(trimmed, e, colorIndex)
            else store.updateHabit(existing.copy(name = trimmed, emoji = e, colorIndex = colorIndex))
        }
    }

    fun deleteHabit(id: Long) { viewModelScope.launch { store.deleteHabit(id) } }

    fun reorder(ids: List<Long>) { viewModelScope.launch { store.reorder(ids) } }

    fun setStyle(style: CardStyle) { viewModelScope.launch { settings.setCardStyle(style) } }

    companion object {
        const val DEFAULT_EMOJI = "📝"
    }
}
