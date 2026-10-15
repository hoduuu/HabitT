package com.habitdiary.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

interface HabitStore {
    fun observeHabits(): Flow<List<Habit>>
    /** habitId → 그 달에 완료한 날(dayOfMonth) 집합. 완료가 없는 습관은 키가 없다. */
    fun observeMonthChecks(month: YearMonth): Flow<Map<Long, Set<Int>>>
    suspend fun getHabit(id: Long): Habit?
    suspend fun doneDays(habitId: Long, month: YearMonth): Set<Int>
    suspend fun toggle(habitId: Long, date: LocalDate)
    suspend fun addHabit(name: String, emoji: String, colorIndex: Int): Long
    suspend fun updateHabit(habit: Habit)
    suspend fun deleteHabit(id: Long)
    suspend fun reorder(idsInOrder: List<Long>)
}

class RoomHabitStore(
    private val dao: HabitDao,
    private val onChanged: () -> Unit = {},
) : HabitStore {
    override fun observeHabits() = dao.observeHabits()

    override fun observeMonthChecks(month: YearMonth): Flow<Map<Long, Set<Int>>> =
        dao.observeChecks(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay()).map { checks ->
            checks.groupBy({ it.habitId }, { LocalDate.ofEpochDay(it.epochDay).dayOfMonth })
                .mapValues { it.value.toSet() }
        }

    override suspend fun getHabit(id: Long) = dao.getHabit(id)

    override suspend fun doneDays(habitId: Long, month: YearMonth): Set<Int> =
        dao.getChecks(habitId, month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay())
            .map { LocalDate.ofEpochDay(it.epochDay).dayOfMonth }.toSet()

    override suspend fun toggle(habitId: Long, date: LocalDate) {
        val day = date.toEpochDay()
        if (dao.countCheck(habitId, day) > 0) dao.deleteCheck(habitId, day)
        else dao.insertCheck(Check(habitId, day))
        onChanged()
    }

    override suspend fun addHabit(name: String, emoji: String, colorIndex: Int): Long {
        val id = dao.insert(Habit(name = name, emoji = emoji, colorIndex = colorIndex, sortOrder = dao.maxSortOrder() + 1))
        onChanged()
        return id
    }

    override suspend fun updateHabit(habit: Habit) {
        dao.update(habit)
        onChanged()
    }

    override suspend fun deleteHabit(id: Long) {
        dao.deleteHabitWithChecks(id)
        onChanged()
    }

    override suspend fun reorder(idsInOrder: List<Long>) {
        dao.reorder(idsInOrder)
        onChanged()
    }
}
