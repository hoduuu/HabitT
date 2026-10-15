package com.habitdiary.app.render

import com.habitdiary.app.calendar.MonthGrid
import com.habitdiary.app.style.CardStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class CardLayoutTest {
    private val oct2026 = MonthGrid.of(YearMonth.of(2026, 10))   // 앞 빈칸 4, 5행
    private val feb2026 = MonthGrid.of(YearMonth.of(2026, 2))    // 앞 빈칸 0, 4행

    @Test
    fun indexTab_size() {
        val l = CardLayout.compute(CardStyle.INDEX_TAB, oct2026)
        assertEquals(320f, l.width, 0.01f)
        assertEquals(227f, l.height, 0.01f)          // 34 + 16 + 153 + 16 + 8
        assertEquals(31, l.cells.size)
        assertEquals(7, l.weekdayAnchors.size)
    }

    @Test
    fun indexTab_fourRowMonth_isShorter() {
        val l = CardLayout.compute(CardStyle.INDEX_TAB, feb2026)
        assertEquals(199f, l.height, 0.01f)          // 34 + 16 + 125 + 16 + 8
    }

    @Test
    fun indexTab_cellPositions() {
        val l = CardLayout.compute(CardStyle.INDEX_TAB, oct2026)
        assertEquals(RectU(218f, 69f, 240f, 91f), l.cells[0])      // 1일: 4열 0행
        assertEquals(RectU(274f, 181f, 296f, 203f), l.cells[30])   // 31일: 6열 4행
    }

    @Test
    fun maskingTape_sizeAndCells() {
        val l = CardLayout.compute(CardStyle.MASKING_TAPE, oct2026)
        assertEquals(320f, l.width, 0.01f)
        assertEquals(271f, l.height, 0.01f)          // 92 + 153 + 18 + 8
        assertEquals(RectU(177f, 111f, 199f, 133f), l.cells[0])
    }

    @Test
    fun dayAt_hitsCells_andMissesBlanks() {
        val l = CardLayout.compute(CardStyle.INDEX_TAB, oct2026)
        assertEquals(1, l.dayAt(229f, 80f))
        assertEquals(31, l.dayAt(285f, 192f))
        assertNull(l.dayAt(107f, 70f))    // 앞 빈 칸 자리
        assertNull(l.dayAt(5f, 5f))       // 카드 밖
    }
}
