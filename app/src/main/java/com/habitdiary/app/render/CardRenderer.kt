package com.habitdiary.app.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import com.habitdiary.app.calendar.DayKind
import com.habitdiary.app.style.CardStyle
import com.habitdiary.app.style.Palette
import java.time.LocalDate
import kotlin.math.min

data class CardContent(
    val habitId: Long,
    val name: String,
    val emoji: String,
    val colorIndex: Int,
    val doneDays: Set<Int>,
    val today: LocalDate,
)

class CardRenderer(private val fonts: CardFonts) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val text = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val dash = DashPathEffect(floatArrayOf(5f, 4f), 0f)

    /** layout의 디자인 단위 좌표 그대로 그린다. 호출자가 canvas.scale로 크기를 맞춘다. */
    fun draw(canvas: Canvas, layout: CardLayout, content: CardContent) {
        when (layout.style) {
            CardStyle.INDEX_TAB -> drawIndexTab(canvas, layout, content)
            CardStyle.MASKING_TAPE -> drawMaskingTape(canvas, layout, content)
        }
    }

    fun renderBitmap(layout: CardLayout, content: CardContent, widthPx: Int, heightPx: Int): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.scale(widthPx / layout.width, heightPx / layout.height)
        draw(canvas, layout, content)
        return bmp
    }

    private fun drawIndexTab(canvas: Canvas, layout: CardLayout, content: CardContent) {
        val color = Palette.habitColor(content.colorIndex)
        val tab = layout.tab!!
        val p = layout.paper

        setText(fonts.gamja, 19f, Color.WHITE, Paint.Align.LEFT)
        val title = ellipsize(content.name, layout.titleMaxWidth)
        val tabRight = tab.left + min(text.measureText(title) + 32f, tab.width)
        fill.color = color
        canvas.drawPath(roundPath(tab.left, tab.top, tabRight, tab.bottom, floatArrayOf(12f, 12f, 12f, 12f, 0f, 0f, 0f, 0f)), fill)
        canvas.drawText(title, layout.titleX, layout.titleBaseline, text)

        val paperPath = roundPath(p.left, p.top, p.right, p.bottom, floatArrayOf(4f, 4f, 18f, 18f, 18f, 18f, 18f, 18f))
        fill.color = Palette.PAPER
        canvas.drawPath(paperPath, fill)
        canvas.save()
        canvas.clipPath(paperPath)
        fill.color = Palette.GRID_DOT
        var y = p.top + 7f
        while (y < p.bottom) {
            var x = p.left + 7f
            while (x < p.right) {
                canvas.drawCircle(x, y, 1f, fill)
                x += 14f
            }
            y += 14f
        }
        canvas.restore()
        stroke.color = color
        stroke.strokeWidth = 2f
        stroke.pathEffect = null
        canvas.drawPath(paperPath, stroke)

        stroke.pathEffect = dash
        canvas.drawCircle(layout.emojiCenterX, layout.emojiCenterY, 31f, stroke)
        stroke.pathEffect = null
        drawEmoji(canvas, content.emoji, layout.emojiCenterX, layout.emojiCenterY, layout.emojiSize, -10f)

        drawGrid(canvas, layout, content, fonts.gamja)
    }

    private fun drawMaskingTape(canvas: Canvas, layout: CardLayout, content: CardContent) {
        val p = layout.paper
        val tape = layout.tape!!
        canvas.save()
        canvas.rotate(if (content.habitId % 2L == 0L) -1f else 1f, p.centerX, p.centerY)

        fill.color = Palette.PAPER
        fill.setShadowLayer(10f, 0f, 3f, Palette.PAPER_SHADOW)
        canvas.drawRoundRect(p.left, p.top, p.right, p.bottom, 6f, 6f, fill)
        fill.clearShadowLayer()

        canvas.save()
        canvas.rotate(-4f, tape.centerX, tape.centerY)
        fill.color = Palette.habitColor(Palette.tapeColorIndex(content.habitId, content.colorIndex))
        fill.alpha = 191
        canvas.drawRoundRect(tape.left, tape.top, tape.right, tape.bottom, 2f, 2f, fill)
        fill.alpha = 255
        canvas.restore()

        drawEmoji(canvas, content.emoji, layout.emojiCenterX, layout.emojiCenterY, layout.emojiSize, -8f)
        setText(fonts.gaeguBold, 24f, Palette.INK, Paint.Align.LEFT)
        canvas.drawText(ellipsize(content.name, layout.titleMaxWidth), layout.titleX, layout.titleBaseline, text)

        drawGrid(canvas, layout, content, fonts.gaegu)
        canvas.restore()
    }

    private fun drawGrid(canvas: Canvas, layout: CardLayout, content: CardContent, headerFont: Typeface) {
        setText(headerFont, 13f, Palette.WEEKDAY, Paint.Align.CENTER)
        WEEKDAYS.forEachIndexed { i, w ->
            val a = layout.weekdayAnchors[i]
            canvas.drawText(w, a.x, a.y, text)
        }
        val color = Palette.habitColor(content.colorIndex)
        layout.cells.forEachIndexed { i, r ->
            val day = i + 1
            val kind = layout.grid.kindOf(day, content.today)
            fill.color = when {
                day in content.doneDays -> color
                kind == DayKind.FUTURE -> Palette.FUTURE
                else -> Palette.EMPTY
            }
            canvas.drawCircle(r.centerX, r.centerY, r.width / 2f, fill)
            if (kind == DayKind.TODAY) {
                stroke.color = color
                stroke.strokeWidth = 2f
                stroke.pathEffect = null
                canvas.drawCircle(r.centerX, r.centerY, r.width / 2f + 1f, stroke)
            }
        }
    }

    private fun drawEmoji(canvas: Canvas, emoji: String, cx: Float, cy: Float, size: Float, degrees: Float) {
        setText(Typeface.DEFAULT, size, Color.BLACK, Paint.Align.CENTER)
        canvas.save()
        canvas.rotate(degrees, cx, cy)
        canvas.drawText(emoji, cx, cy - (text.ascent() + text.descent()) / 2f, text)
        canvas.restore()
    }

    private fun setText(typeface: Typeface, size: Float, color: Int, align: Paint.Align) {
        text.typeface = typeface
        text.textSize = size
        text.color = color
        text.textAlign = align
    }

    private fun ellipsize(s: String, maxWidth: Float): String =
        TextUtils.ellipsize(s, text, maxWidth, TextUtils.TruncateAt.END).toString()

    private fun roundPath(l: Float, t: Float, r: Float, b: Float, radii: FloatArray) =
        Path().apply { addRoundRect(RectF(l, t, r, b), radii, Path.Direction.CW) }

    private companion object {
        val WEEKDAYS = listOf("일", "월", "화", "수", "목", "금", "토")
    }
}
