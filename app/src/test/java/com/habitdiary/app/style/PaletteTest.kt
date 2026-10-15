package com.habitdiary.app.style

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PaletteTest {
    @Test
    fun habitColor_wrapsIndex() {
        assertEquals(0xFFFFB38A.toInt(), Palette.habitColor(0))
        assertEquals(0xFFD9B99B.toInt(), Palette.habitColor(7))
        assertEquals(Palette.habitColor(0), Palette.habitColor(8))
    }

    @Test
    fun tapeColor_isDeterministic_andDiffersFromHabitColor() {
        for (id in 1L..40L) {
            for (c in 0 until 8) {
                val t = Palette.tapeColorIndex(id, c)
                assertNotEquals(c, t)
                assertEquals(t, Palette.tapeColorIndex(id, c))
            }
        }
    }
}
