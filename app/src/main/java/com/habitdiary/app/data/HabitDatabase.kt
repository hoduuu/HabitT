package com.habitdiary.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Habit::class, Check::class], version = 1, exportSchema = false)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        fun build(context: Context): HabitDatabase =
            Room.databaseBuilder(context, HabitDatabase::class.java, "habits.db").build()
    }
}
