package com.example.bt2firebaseall

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

// Màn Việc mới (POST /tasks) và Sửa công việc (PATCH /tasks/{id}) dùng chung một giao diện.

/** `taskId = null` -> thêm việc mới; có `taskId` -> sửa việc đó. */
@Composable
fun TaskEditScreen(taskId: String?, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isNew = taskId == null

    // Dữ liệu trong form (rememberSaveable: xoay màn hình không bị mất)
    var title by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(Category.STUDY) }
    var priority by rememberSaveable { mutableStateOf(Priority.MEDIUM) }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var time by rememberSaveable { mutableStateOf("19:00") }
    var remind by rememberSaveable { mutableStateOf(true) }

    var isLoaded by rememberSaveable { mutableStateOf(isNew) } // việc mới thì không cần tải
    var isSaving by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Sửa: GET /tasks/{id} rồi điền sẵn vào form
    if (taskId != null && !isLoaded) {
        LaunchedEffect(taskId) {
            try {
                val task = TaskRepository.get(taskId)
                title = task.title
                note = task.note
                category = task.cat
                priority = Priority.from(task.priority)
                date = task.date
                time = task.time
                remind = task.remind
                isLoaded = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(context, e.toMessage(), Toast.LENGTH_LONG).show()
                onClose()
            }
        }
    }

    val canSave = isLoaded && title.isNotBlank() && !isSaving

    fun save() {
        if (!canSave) return
        isSaving = true
        val task = Task(
            title = title.trim(),
            note = note.trim(),
            category = category.key,
            date = date,
            time = time,
            priority = priority.key,
            remind = remind,
        )
        val successMessage = if (isNew) "Đã thêm công việc" else "Đã lưu thay đổi"
        val job = scope.call(context, onDone = {
            Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
            onClose()
        }) {
            if (taskId == null) {
                TaskRepository.add(task)
                Tracker.log(context, "add_task", "category" to task.category)
            } else {
                TaskRepository.update(taskId, task.toChangeMap())
            }
        }
        job.invokeOnCompletion { isSaving = false }
    }

    if (showDatePicker) {
        PickDateDialog(
            initialDate = date,
            onPicked = { date = it },
            onDismiss = { showDatePicker = false },
        )
    }
    if (showTimePicker) {
        PickTimeDialog(
            initialTime = time,
            onPicked = { time = it },
            onDismiss = { showTimePicker = false },
        )
    }

    Column(Modifier.fillMaxSize().background(AppColors.Bg).padding(16.dp)) {
        TopBar(
            leftText = "Hủy",
            showBackArrow = false,
            onLeftClick = onClose,
            title = if (isNew) "Việc mới" else "Sửa công việc",
            actionText = "Lưu",
            actionEnabled = canSave,
            onActionClick = ::save,
        )
        if (!isLoaded) LinearProgressIndicator(Modifier.fillMaxWidth())

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp)) {
            // Tên + ghi chú
            Column(Modifier.fillMaxWidth().card(16)) {
                FieldCaption("Tên công việc")
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Ví dụ: Ôn chương 4 – Giải thuật") },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium),
                    colors = plainTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalDivider(color = AppColors.Bg)
                FieldCaption("Ghi chú")
                TextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Thêm ghi chú (không bắt buộc)") },
                    colors = plainTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            FormLabel("Nhóm")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Category.entries.forEach { option ->
                    CategoryOption(
                        category = option,
                        selected = category == option,
                        modifier = Modifier.weight(1f),
                        onClick = { category = option },
                    )
                }
            }

            // Ngày, giờ, nhắc nhở
            Spacer(Modifier.height(14.dp))
            Column(Modifier.fillMaxWidth().card(16)) {
                PickerRow(Icons.Outlined.CalendarMonth, "Ngày", value = dateLabel(date), onClick = { showDatePicker = true })
                HorizontalDivider(color = AppColors.Bg)
                PickerRow(Icons.Outlined.Schedule, "Giờ", value = time, onClick = { showTimePicker = true })
                HorizontalDivider(color = AppColors.Bg)
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = AppColors.Gray, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("Nhắc tôi", fontSize = 16.sp)
                        Text("Trước 15 phút", fontSize = 12.sp, color = AppColors.Gray)
                    }
                    Switch(checked = remind, onCheckedChange = { remind = it }, colors = greenSwitchColors())
                }
            }

            FormLabel("Mức ưu tiên")
            PrioritySelector(selected = priority, onSelect = { priority = it })
        }

        Button(
            onClick = ::save,
            enabled = canSave,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
        ) {
            if (isSaving) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Lưu công việc", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}

/** Các trường gửi lên khi sửa (PATCH chỉ cần những trường có trong form). */
private fun Task.toChangeMap(): Map<String, Any> = mapOf(
    "title" to title,
    "note" to note,
    "category" to category,
    "date" to date,
    "time" to time,
    "priority" to priority,
    "remind" to remind,
)

// ---------- Các phần nhỏ của form ----------

@Composable
private fun FieldCaption(text: String) {
    Text(text, fontSize = 12.sp, color = AppColors.Gray, modifier = Modifier.padding(start = 16.dp, top = 12.dp))
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppColors.Gray,
        modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp),
    )
}

/** Nút chọn nhóm "Học tập" / "Cuộc sống"; nút đang chọn có viền và nền màu chủ đạo. */
@Composable
private fun CategoryOption(category: Category, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .height(48.dp)
            .clip(shape)
            .background(if (selected) AppColors.Primary.copy(alpha = 0.12f) else AppColors.Card)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) AppColors.Primary else AppColors.Gray.copy(alpha = 0.3f),
                shape = shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(category.label, fontWeight = FontWeight.SemiBold, color = if (selected) AppColors.Primary else AppColors.Text)
    }
}

/** Ô chọn 3 nấc Thấp / Vừa / Cao (kiểu segmented control). */
@Composable
private fun PrioritySelector(selected: Priority, onSelect: (Priority) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Gray.copy(alpha = 0.15f))
            .padding(3.dp),
    ) {
        Priority.entries.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) AppColors.Card else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option.short,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) AppColors.Text else AppColors.Gray,
                )
            }
        }
    }
}

/** Dòng "Ngày  [Thứ Tư, 21/10]": bấm để mở hộp chọn. */
@Composable
private fun PickerRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = AppColors.Gray, modifier = Modifier.size(20.dp))
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
        Text(
            value,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.Gray.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

/** Hộp chọn ngày. Ngày truyền vào/ra dạng "yyyy-MM-dd" giống JSON của API. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickDateDialog(initialDate: String, onPicked: (String) -> Unit, onDismiss: () -> Unit) {
    val start = runCatching { LocalDate.parse(initialDate) }.getOrDefault(LocalDate.now())
    // DatePicker làm việc với mili-giây theo giờ UTC
    val state = rememberDatePickerState(initialSelectedDateMillis = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) {
                    onPicked(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString())
                }
                onDismiss()
            }) { Text("Chọn") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    ) {
        DatePicker(state)
    }
}

/** Hộp chọn giờ (24 giờ). Giờ truyền vào/ra dạng "HH:mm". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickTimeDialog(initialTime: String, onPicked: (String) -> Unit, onDismiss: () -> Unit) {
    val hourAndMinute = initialTime.split(":").map { it.toIntOrNull() ?: 0 }
    val state = rememberTimePickerState(
        initialHour = hourAndMinute.getOrElse(0) { 19 },
        initialMinute = hourAndMinute.getOrElse(1) { 0 },
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onPicked(String.format(Locale.US, "%02d:%02d", state.hour, state.minute))
                onDismiss()
            }) { Text("Chọn") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
        text = { TimePicker(state) },
    )
}
