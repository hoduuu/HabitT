package com.habitdiary.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.habitdiary.app.calendar.MonthGrid
import com.habitdiary.app.render.CardContent
import com.habitdiary.app.render.CardLayout
import com.habitdiary.app.render.CardRenderer
import com.habitdiary.app.style.CardStyle
import java.time.LocalDate
import java.time.YearMonth

/** CardRenderer로 그린 카드. 탭 위치를 CardLayout.dayAt으로 날짜에 매핑한다. */
@Composable
fun HabitCard(
    row: HabitRow,
    month: YearMonth,
    today: LocalDate,
    style: CardStyle,
    renderer: CardRenderer,
    onDayTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layout = remember(style, month) { CardLayout.compute(style, MonthGrid.of(month)) }
    val content = CardContent(row.habit.id, row.habit.name, row.habit.emoji, row.habit.colorIndex, row.doneDays, today)
    val tap by rememberUpdatedState(onDayTap)
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(layout.width / layout.height)
            .pointerInput(layout) {
                detectTapGestures { pos ->
                    val s = size.width / layout.width
                    layout.dayAt(pos.x / s, pos.y / s)?.let { tap(it) }
                }
            },
    ) {
        val s = size.width / layout.width
        drawIntoCanvas { c ->
            val n = c.nativeCanvas
            n.save()
            n.scale(s, s)
            renderer.draw(n, layout, content)
            n.restore()
        }
    }
}
