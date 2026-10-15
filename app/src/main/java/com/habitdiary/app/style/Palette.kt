package com.habitdiary.app.style

object Palette {
    val PAGE = 0xFFFFF8F0.toInt()
    val PAPER = 0xFFFFFDF9.toInt()
    val INK = 0xFF5B4636.toInt()
    val WEEKDAY = 0xFFC4AB97.toInt()
    val EMPTY = 0xFFF3EBE3.toInt()
    val FUTURE = 0xFFFAF5EF.toInt()
    val GRID_DOT = 0xFFEAD9C9.toInt()
    val PAPER_SHADOW = 0x1F966E50

    val habitColors = intArrayOf(
        0xFFFFB38A.toInt(), 0xFFFFD27A.toInt(), 0xFF9CCFA8.toInt(), 0xFF8FD3D0.toInt(),
        0xFFA9C4F5.toInt(), 0xFFC7B2F0.toInt(), 0xFFF5A9C8.toInt(), 0xFFD9B99B.toInt(),
    )

    fun habitColor(index: Int): Int = habitColors[index.mod(habitColors.size)]

    /** 마스킹테이프 색: 습관 id로 정해지되 습관 색과는 겹치지 않는 팔레트 인덱스. */
    fun tapeColorIndex(habitId: Long, habitColorIndex: Int): Int {
        val n = habitColors.size
        val i = habitId.mod(n.toLong()).toInt()
        return if (i == habitColorIndex.mod(n)) (i + 1) % n else i
    }
}
