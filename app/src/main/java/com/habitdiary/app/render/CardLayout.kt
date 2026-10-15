package com.habitdiary.app.render

import com.habitdiary.app.calendar.MonthGrid
import com.habitdiary.app.style.CardStyle
import kotlin.math.max

data class RectU(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom
}

data class PointU(val x: Float, val y: Float)

/** 카드 한 장의 좌표 (디자인 단위). cells[i] = (i+1)일 칸. */
data class CardLayout(
    val style: CardStyle,
    val grid: MonthGrid,
    val width: Float,
    val height: Float,
    val paper: RectU,
    val tab: RectU?,
    val tape: RectU?,
    val emojiCenterX: Float,
    val emojiCenterY: Float,
    val emojiSize: Float,
    val titleX: Float,
    val titleBaseline: Float,
    val titleMaxWidth: Float,
    val weekdayAnchors: List<PointU>,
    val cells: List<RectU>,
) {
    fun dayAt(x: Float, y: Float): Int? {
        val i = cells.indexOfFirst { it.contains(x, y) }
        return if (i < 0) null else i + 1
    }

    companion object {
        const val CELL = 22f
        const val GAP = 6f
        const val PITCH = CELL + GAP
        const val HEADER = 19f            // 요일 글자 13 + 간격 6
        const val MARGIN = 8f
        const val PAPER_W = 304f
        const val GRID_W = 7 * CELL + 6 * GAP   // 190

        fun gridHeight(rows: Int): Float = HEADER + rows * CELL + (rows - 1) * GAP

        fun compute(style: CardStyle, grid: MonthGrid): CardLayout = when (style) {
            CardStyle.INDEX_TAB -> indexTab(grid)
            CardStyle.MASKING_TAPE -> maskingTape(grid)
        }

        private fun indexTab(grid: MonthGrid): CardLayout {
            val gridH = gridHeight(grid.rows)
            val contentH = max(64f, gridH)
            val paperTop = MARGIN + 26f
            val paper = RectU(MARGIN, paperTop, MARGIN + PAPER_W, paperTop + 16f + contentH + 16f)
            val tab = RectU(MARGIN + 16f, MARGIN, paper.right - 16f, paperTop)
            val gridLeft = MARGIN + 16f + 64f + 18f
            val gridTop = paperTop + 16f + (contentH - gridH) / 2f
            val (anchors, cells) = gridParts(grid, gridLeft, gridTop)
            return CardLayout(
                style = CardStyle.INDEX_TAB, grid = grid,
                width = MARGIN * 2 + PAPER_W, height = paper.bottom + MARGIN,
                paper = paper, tab = tab, tape = null,
                emojiCenterX = MARGIN + 16f + 32f, emojiCenterY = paperTop + 16f + contentH / 2f, emojiSize = 32f,
                titleX = tab.left + 16f, titleBaseline = MARGIN + 19f, titleMaxWidth = tab.width - 32f,
                weekdayAnchors = anchors, cells = cells,
            )
        }

        private fun maskingTape(grid: MonthGrid): CardLayout {
            val gridH = gridHeight(grid.rows)
            val paperTop = MARGIN + 10f
            val gridTop = paperTop + 22f + 40f + 12f
            val paper = RectU(MARGIN, paperTop, MARGIN + PAPER_W, gridTop + gridH + 18f)
            val tape = RectU(paper.centerX - 35f, MARGIN, paper.centerX + 35f, MARGIN + 20f)
            val gridLeft = MARGIN + (PAPER_W - GRID_W) / 2f
            val titleX = MARGIN + 18f + 40f + 10f
            val (anchors, cells) = gridParts(grid, gridLeft, gridTop)
            return CardLayout(
                style = CardStyle.MASKING_TAPE, grid = grid,
                width = MARGIN * 2 + PAPER_W, height = paper.bottom + MARGIN,
                paper = paper, tab = null, tape = tape,
                emojiCenterX = MARGIN + 18f + 20f, emojiCenterY = paperTop + 22f + 20f, emojiSize = 34f,
                titleX = titleX, titleBaseline = paperTop + 22f + 28f, titleMaxWidth = paper.right - 18f - titleX,
                weekdayAnchors = anchors, cells = cells,
            )
        }

        private fun gridParts(grid: MonthGrid, left: Float, top: Float): Pair<List<PointU>, List<RectU>> {
            val anchors = (0 until 7).map { PointU(left + it * PITCH + CELL / 2f, top + 11f) }
            val cells = (1..grid.daysInMonth).map { day ->
                val idx = grid.leadingBlanks + day - 1
                val l = left + (idx % 7) * PITCH
                val t = top + HEADER + (idx / 7) * PITCH
                RectU(l, t, l + CELL, t + CELL)
            }
            return anchors to cells
        }
    }
}
