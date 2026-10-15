package com.habitdiary.app.render

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import com.habitdiary.app.calendar.MonthGrid
import com.habitdiary.app.style.CardStyle
import com.habitdiary.app.style.Palette
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CardRendererTest {
    private val renderer = CardRenderer(CardFonts(Typeface.DEFAULT, Typeface.DEFAULT_BOLD, Typeface.DEFAULT))
    private val today = LocalDate.of(2026, 10, 14)
    private val content = CardContent(
        habitId = 2, name = "1시30분전 수면", emoji = "🌙", colorIndex = 0,
        doneDays = setOf(2, 3, 5, 6, 7, 10, 12, 13), today = today,
    )

    private fun render(style: CardStyle): Pair<CardLayout, Bitmap> {
        val layout = CardLayout.compute(style, MonthGrid.of(YearMonth.of(2026, 10)))
        val bmp = renderer.renderBitmap(layout, content, (layout.width * 2).toInt(), (layout.height * 2).toInt())
        File("build/render-preview").mkdirs()
        File("build/render-preview/${style.name}.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return layout to bmp
    }

    private fun pixelAtDay(layout: CardLayout, bmp: Bitmap, day: Int): Int {
        val c = layout.cells[day - 1]
        return bmp.getPixel((c.centerX * 2).toInt(), (c.centerY * 2).toInt())
    }

    @Test
    fun indexTab_colorsCellsByState() {
        val (layout, bmp) = render(CardStyle.INDEX_TAB)
        assertEquals(Palette.habitColor(0), pixelAtDay(layout, bmp, 2))   // 완료
        assertEquals(Palette.EMPTY, pixelAtDay(layout, bmp, 1))            // 지난 미완료
        assertEquals(Palette.FUTURE, pixelAtDay(layout, bmp, 20))          // 미래
    }

    @Test
    fun maskingTape_colorsCellsByState() {
        val (layout, bmp) = render(CardStyle.MASKING_TAPE)
        // ±1° 기울임이 있어 중심 픽셀 색은 그대로지만 안티앨리어싱 허용 위해 RGB만 비교
        assertEquals(Color.rgb(0xFF, 0xB3, 0x8A) and 0xFFFFFF, pixelAtDay(layout, bmp, 2) and 0xFFFFFF)
        assertEquals(Palette.FUTURE and 0xFFFFFF, pixelAtDay(layout, bmp, 20) and 0xFFFFFF)
    }
}
