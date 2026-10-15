package com.habitdiary.app.data

import androidx.room.Entity
import androidx.room.Index

/** habitId 습관을 epochDay(LocalDate.toEpochDay) 날짜에 완료함. */
@Entity(tableName = "checks", primaryKeys = ["habitId", "epochDay"], indices = [Index("habitId")])
data class Check(val habitId: Long, val epochDay: Long)
