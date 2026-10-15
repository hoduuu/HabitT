package com.habitdiary.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class HabitDao {
    @Query("SELECT * FROM habits ORDER BY sortOrder, id")
    abstract fun observeHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id")
    abstract suspend fun getHabit(id: Long): Habit?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM habits")
    abstract suspend fun maxSortOrder(): Int

    @Insert
    abstract suspend fun insert(habit: Habit): Long

    @Update
    abstract suspend fun update(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :id")
    abstract suspend fun deleteHabitRow(id: Long)

    @Query("DELETE FROM checks WHERE habitId = :id")
    abstract suspend fun deleteChecksOf(id: Long)

    @Transaction
    open suspend fun deleteHabitWithChecks(id: Long) {
        deleteChecksOf(id)
        deleteHabitRow(id)
    }

    @Query("UPDATE habits SET sortOrder = :order WHERE id = :id")
    abstract suspend fun setSortOrder(id: Long, order: Int)

    @Transaction
    open suspend fun reorder(ids: List<Long>) {
        ids.forEachIndexed { index, id -> setSortOrder(id, index) }
    }

    @Query("SELECT * FROM checks WHERE epochDay BETWEEN :from AND :to")
    abstract fun observeChecks(from: Long, to: Long): Flow<List<Check>>

    @Query("SELECT * FROM checks WHERE habitId = :habitId AND epochDay BETWEEN :from AND :to")
    abstract suspend fun getChecks(habitId: Long, from: Long, to: Long): List<Check>

    @Query("SELECT COUNT(*) FROM checks WHERE habitId = :habitId AND epochDay = :day")
    abstract suspend fun countCheck(habitId: Long, day: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertCheck(check: Check)

    @Query("DELETE FROM checks WHERE habitId = :habitId AND epochDay = :day")
    abstract suspend fun deleteCheck(habitId: Long, day: Long)
}
