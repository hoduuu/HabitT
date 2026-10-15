package com.habitdiary.app.calendar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthGridTest {
    @Test
    fun october2026_startsThursday_fiveRows() {
        val g = MonthGrid.of(YearMonth.of(2026, 10))
        assertEquals(4, g.leadingBlanks)
        assertEquals(31, g.daysInMonth)
        assertEquals(5, g.rows)
    }

    @Test
    fun february2026_startsSunday_fourRows() {
        val g = MonthGrid.of(YearMonth.of(2026, 2))
        assertEquals(0, g.leadingBlanks)
        assertEquals(28, g.daysInMonth)
        assertEquals(4, g.rows)
    }

    @Test
    fun august2026_startsSaturday_sixRows() {
        val g = MonthGrid.of(YearMonth.of(2026, 8))
        assertEquals(6, g.leadingBlanks)
        assertEquals(6, g.rows)
    }

    @Test
    fun february2027_monday_and_leapFebruary2028() {
        val feb27 = MonthGrid.of(YearMonth.of(2027, 2))
        assertEquals(1, feb27.leadingBlanks)
        assertEquals(28, feb27.daysInMonth)
        assertEquals(29, MonthGrid.of(YearMonth.of(2028, 2)).daysInMonth)
    }

    @Test
    fun kindOf_comparesWithToday() {
        val g = MonthGrid.of(YearMonth.of(2026, 10))
        val today = LocalDate.of(2026, 10, 14)
        assertEquals(DayKind.PAST, g.kindOf(13, today))
        assertEquals(DayKind.TODAY, g.kindOf(14, today))
        assertEquals(DayKind.FUTURE, g.kindOf(15, today))
        assertEquals(DayKind.PAST, MonthGrid.of(YearMonth.of(2026, 9)).kindOf(30, today))
        assertEquals(DayKind.FUTURE, MonthGrid.of(YearMonth.of(2026, 11)).kindOf(1, today))
    }
}
