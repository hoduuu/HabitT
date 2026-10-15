package com.habitdiary.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomHabitStoreTest {
    private lateinit var db: HabitDatabase
    private lateinit var store: RoomHabitStore
    private var changes = 0
    private val oct = YearMonth.of(2026, 10)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HabitDatabase::class.java)
            .allowMainThreadQueries().build()
        store = RoomHabitStore(db.habitDao()) { changes++ }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun addHabit_appendsInOrder() = runTest {
        store.addHabit("수면", "🌙", 0)
        store.addHabit("밀크티", "🥤", 2)
        assertEquals(listOf("수면", "밀크티"), store.observeHabits().first().map { it.name })
        assertEquals(2, changes)
    }

    @Test
    fun toggle_addsThenRemoves_andScopesToMonth() = runTest {
        val id = store.addHabit("수면", "🌙", 0)
        store.toggle(id, LocalDate.of(2026, 10, 5))
        store.toggle(id, LocalDate.of(2026, 10, 6))
        store.toggle(id, LocalDate.of(2026, 9, 30))
        assertEquals(setOf(5, 6), store.doneDays(id, oct))
        assertEquals(mapOf(id to setOf(5, 6)), store.observeMonthChecks(oct).first())
        store.toggle(id, LocalDate.of(2026, 10, 5))
        assertEquals(setOf(6), store.doneDays(id, oct))
    }

    @Test
    fun deleteHabit_removesItsChecks() = runTest {
        val id = store.addHabit("수면", "🌙", 0)
        store.toggle(id, LocalDate.of(2026, 10, 5))
        store.deleteHabit(id)
        assertNull(store.getHabit(id))
        assertEquals(emptyMap<Long, Set<Int>>(), store.observeMonthChecks(oct).first())
    }

    @Test
    fun reorder_and_update() = runTest {
        val a = store.addHabit("A", "🅰", 0)
        val b = store.addHabit("B", "🅱", 1)
        store.reorder(listOf(b, a))
        assertEquals(listOf(b, a), store.observeHabits().first().map { it.id })
        store.updateHabit(store.getHabit(a)!!.copy(name = "A2"))
        assertEquals("A2", store.getHabit(a)!!.name)
    }
}
