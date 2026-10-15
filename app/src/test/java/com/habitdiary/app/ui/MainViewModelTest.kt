package com.habitdiary.app.ui

import com.habitdiary.app.data.Habit
import com.habitdiary.app.data.HabitStore
import com.habitdiary.app.data.SettingsStore
import com.habitdiary.app.style.CardStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

private class FakeStore : HabitStore {
    val habits = MutableStateFlow<List<Habit>>(emptyList())
    val checks = MutableStateFlow<Set<Pair<Long, LocalDate>>>(emptySet())
    private var nextId = 1L

    override fun observeHabits(): Flow<List<Habit>> = habits
    override fun observeMonthChecks(month: YearMonth) = checks.map { all ->
        all.filter { YearMonth.from(it.second) == month }
            .groupBy({ it.first }, { it.second.dayOfMonth }).mapValues { it.value.toSet() }
    }
    override suspend fun getHabit(id: Long) = habits.value.firstOrNull { it.id == id }
    override suspend fun doneDays(habitId: Long, month: YearMonth) =
        checks.value.filter { it.first == habitId && YearMonth.from(it.second) == month }.map { it.second.dayOfMonth }.toSet()
    override suspend fun toggle(habitId: Long, date: LocalDate) {
        val key = habitId to date
        checks.value = if (key in checks.value) checks.value - key else checks.value + key
    }
    override suspend fun addHabit(name: String, emoji: String, colorIndex: Int): Long {
        val id = nextId++
        habits.value = habits.value + Habit(id, name, emoji, colorIndex, habits.value.size)
        return id
    }
    override suspend fun updateHabit(habit: Habit) {
        habits.value = habits.value.map { if (it.id == habit.id) habit else it }
    }
    override suspend fun deleteHabit(id: Long) {
        habits.value = habits.value.filterNot { it.id == id }
    }
    override suspend fun reorder(idsInOrder: List<Long>) {
        habits.value = idsInOrder.mapIndexed { i, id -> habits.value.first { it.id == id }.copy(sortOrder = i) }
    }
}

private class FakeSettings : SettingsStore {
    override val cardStyle = MutableStateFlow(CardStyle.INDEX_TAB)
    override suspend fun setCardStyle(style: CardStyle) { cardStyle.value = style }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val today = LocalDate.of(2026, 10, 14)
    private lateinit var store: FakeStore
    private lateinit var settings: FakeSettings
    private lateinit var vm: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        store = FakeStore()
        settings = FakeSettings()
        vm = MainViewModel(store, settings) { today }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsOnCurrentMonth_withRows() {
        vm.saveHabit(null, "수면", "🌙", 0)
        val s = vm.state.value
        assertEquals(YearMonth.of(2026, 10), s.month)
        assertEquals(today, s.today)
        assertEquals(listOf("수면"), s.rows.map { it.habit.name })
        assertEquals(true, s.loaded)
    }

    @Test
    fun tapPastAndToday_toggles_futureIgnored() {
        vm.saveHabit(null, "수면", "🌙", 0)
        val id = vm.state.value.rows.single().habit.id
        vm.onDayTapped(id, 13)
        vm.onDayTapped(id, 14)
        vm.onDayTapped(id, 15)
        assertEquals(setOf(13, 14), vm.state.value.rows.single().doneDays)
        vm.onDayTapped(id, 13)
        assertEquals(setOf(14), vm.state.value.rows.single().doneDays)
    }

    @Test
    fun monthNavigation_showsThatMonthsChecks() {
        vm.saveHabit(null, "수면", "🌙", 0)
        val id = vm.state.value.rows.single().habit.id
        vm.previousMonth()
        assertEquals(YearMonth.of(2026, 9), vm.state.value.month)
        vm.onDayTapped(id, 30)
        assertEquals(setOf(30), vm.state.value.rows.single().doneDays)
        vm.nextMonth()
        assertEquals(emptySet<Int>(), vm.state.value.rows.single().doneDays)
    }

    @Test
    fun saveHabit_trimsName_defaultsEmoji_ignoresBlank_updatesExisting() {
        vm.saveHabit(null, "   ", "🌙", 0)
        assertEquals(0, vm.state.value.rows.size)
        vm.saveHabit(null, "  수면  ", "", 3)
        val h = vm.state.value.rows.single().habit
        assertEquals("수면", h.name)
        assertEquals(MainViewModel.DEFAULT_EMOJI, h.emoji)
        vm.saveHabit(h.id, "잠", "🌙", 5)
        val updated = vm.state.value.rows.single().habit
        assertEquals("잠", updated.name)
        assertEquals("🌙", updated.emoji)
        assertEquals(5, updated.colorIndex)
    }

    @Test
    fun deleteReorderAndStyle() {
        vm.saveHabit(null, "A", "🅰", 0)
        vm.saveHabit(null, "B", "🅱", 1)
        val (a, b) = vm.state.value.rows.map { it.habit.id }
        vm.reorder(listOf(b, a))
        assertEquals(listOf("B", "A"), vm.state.value.rows.map { it.habit.name })
        vm.deleteHabit(b)
        assertEquals(listOf("A"), vm.state.value.rows.map { it.habit.name })
        vm.setStyle(CardStyle.MASKING_TAPE)
        assertEquals(CardStyle.MASKING_TAPE, vm.state.value.style)
    }
}
