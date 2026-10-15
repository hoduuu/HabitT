package com.habitdiary.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habitdiary.app.data.Habit
import com.habitdiary.app.style.Palette

sealed interface EditTarget {
    data object New : EditTarget
    data class Existing(val habit: Habit) : EditTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditSheet(
    target: EditTarget,
    onSave: (name: String, emoji: String, colorIndex: Int) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val existing = (target as? EditTarget.Existing)?.habit
    var name by remember(target) { mutableStateOf(existing?.name ?: "") }
    var emoji by remember(target) { mutableStateOf(existing?.emoji ?: "") }
    var colorIndex by remember(target) { mutableIntStateOf(existing?.colorIndex ?: 0) }
    var confirmDelete by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = PaperColor) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(if (existing == null) "새 습관" else "습관 수정", fontFamily = DiaryFont, fontSize = 26.sp, color = Ink)
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("이름") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = emoji, onValueChange = { emoji = lastGrapheme(it) },
                label = { Text("이모지 (비우면 ${MainViewModel.DEFAULT_EMOJI})") }, singleLine = true,
                modifier = Modifier.width(200.dp),
            )
            ColorPicker(colorIndex) { colorIndex = it }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (existing != null) {
                    OutlinedButton(onClick = { confirmDelete = true }) { Text("삭제") }
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = { onSave(name.trim(), emoji, colorIndex) }, enabled = name.isNotBlank()) { Text("저장") }
            }
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("습관 삭제") },
            text = { Text("‘${existing.name}’ 습관과 모든 기록을 지울까요?") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("삭제") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun ColorPicker(selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Palette.habitColors.forEachIndexed { i, c ->
            Box(
                Modifier
                    .size(34.dp)
                    .border(if (i == selected) 3.dp else 0.dp, if (i == selected) Ink else Color.Transparent, CircleShape)
                    .padding(4.dp)
                    .background(Color(c), CircleShape)
                    .clickable { onSelect(i) },
            )
        }
    }
}
