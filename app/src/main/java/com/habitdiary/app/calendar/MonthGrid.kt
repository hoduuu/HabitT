package com.habitdiary.app.calendar

import java.time.LocalDate
import java.time.YearMonth

enum class DayKind { PAST, TODAY, FUTURE }

/** 일요일 시작 7열 달력 배치. leadingBlanks = 1일 앞 빈 칸 수. */
data class MonthGrid(
    val month: YearMonth,
    val leadingBlanks: Int,
    val daysInMonth: Int,
    val rows: Int,
) {
    fun kindOf(day: Int, today: LocalDate): DayKind {
        val date = month.atDay(day)
        return when {
            date.isBefore(today) -> DayKind.PAST
            date == today -> DayKind.TODAY
            else -> DayKind.FUTURE
        }
    }

    companion object {
        fun of(month: YearMonth): MonthGrid {
            // DayOfWeek.value: 월=1 … 일=7 → 일=0
            val leading = month.atDay(1).dayOfWeek.value % 7
            val days = month.lengthOfMonth()
            return MonthGrid(month, leading, days, (leading + days + 6) / 7)
        }
    }
}
