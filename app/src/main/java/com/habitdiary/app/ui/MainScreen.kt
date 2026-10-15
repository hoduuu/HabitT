package com.habitdiary.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habitdiary.app.render.CardRenderer
import java.time.YearMonth

@Composable
fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit, onAdd: () -> Unit, onSettings: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)) {
        Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
            HeaderButton("‹", onPrev)
            Text(
                "${month.year % 100}년 ${month.monthValue}월",
                fontFamily = DiaryFont, fontSize = 30.sp, color = Ink,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            HeaderButton("›", onNext)
        }
        Row(Modifier.align(Alignment.CenterEnd)) {
            HeaderButton("＋", onAdd)
            HeaderButton("⚙", onSettings)
        }
    }
}

@Composable
private fun HeaderButton(label: String, onClick: () -> Unit) {
    Text(label, fontSize = 26.sp, color = Ink, modifier = Modifier.clickable(onClick = onClick).padding(8.dp))
}

@Composable
fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("＋를 눌러 첫 습관을 만들어요", fontFamily = DiaryFont, fontSize = 22.sp, color = Ink, textAlign = TextAlign.Center)
    }
}

@Composable
fun MainScreen(
    state: MainUiState,
    renderer: CardRenderer,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDayTap: (habitId: Long, day: Int) -> Unit,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(PageColor).safeDrawingPadding()) {
        MonthHeader(state.month, onPrev, onNext, onAdd, onSettings)
        if (state.loaded && state.rows.isEmpty()) {
            EmptyState()
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.rows, key = { it.habit.id }) { row ->
                HabitCard(row, state.month, state.today, state.style, renderer, onDayTap = { d -> onDayTap(row.habit.id, d) })
            }
        }
    }
}
