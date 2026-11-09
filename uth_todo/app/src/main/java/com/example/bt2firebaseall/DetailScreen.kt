package com.example.bt2firebaseall

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException

// Chi tiết công việc: GET /tasks/{id} khi mở, PATCH khi sửa, DELETE khi xóa.

@Composable
fun DetailScreen(id: String, backLabel: String, onBack: () -> Unit, onEdit: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var task by remember { mutableStateOf<Task?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadCount by remember { mutableIntStateOf(0) } // tăng lên 1 => GET lại
    var askDelete by remember { mutableStateOf(false) }

    // GET /tasks/{id}: lúc mở màn hình và sau mỗi lần sửa
    LaunchedEffect(reloadCount) {
        loadError = null
        try {
            task = TaskRepository.get(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            loadError = e.toMessage()
        }
    }

    /** PATCH /tasks/{id} với các trường thay đổi, xong thì tải lại. */
    fun patch(changes: Map<String, Any>) {
        scope.call(context, onDone = { reloadCount++ }) { TaskRepository.update(id, changes) }
    }

    fun delete() {
        scope.call(context, onDone = {
            Toast.makeText(context, "Đã xóa công việc", Toast.LENGTH_SHORT).show()
            onBack()
        }) { TaskRepository.delete(id) }
    }

    val currentTask = task
    if (askDelete && currentTask != null) {
        ConfirmDeleteDialog(
            title = currentTask.title,
            onConfirm = {
                askDelete = false
                delete()
            },
            onDismiss = { askDelete = false },
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        TopBar(
            leftText = backLabel,
            onLeftClick = onBack,
            actionText = if (currentTask != null) "Sửa" else null,
            onActionClick = onEdit,
        )
        Spacer(Modifier.height(4.dp))

        when {
            currentTask != null -> TaskDetail(
                task = currentTask,
                onPatch = ::patch,
                onDeleteClick = { askDelete = true },
            )

            loadError != null -> Column(Modifier.fillMaxWidth().card().padding(14.dp)) {
                Text("Không tải được công việc", fontWeight = FontWeight.SemiBold, color = AppColors.Red)
                Text(loadError.orEmpty(), fontSize = 12.sp, color = AppColors.Gray)
                TextButton(onClick = { reloadCount++ }) { Text("Thử lại") }
            }

            else -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

/** Nội dung khi đã tải xong: nhãn, tiêu đề, hạn chót, ghi chú, việc nhỏ, các nút. */
@Composable
private fun ColumnScope.TaskDetail(task: Task, onPatch: (Map<String, Any>) -> Unit, onDeleteClick: () -> Unit) {
    val priority = Priority.from(task.priority)

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Chip(task.cat.label, task.cat.text, task.cat.bg)
        if (priority == Priority.HIGH) {
            Chip(priority.label, AppColors.Red, Color(0xFFFFE3E3))
        } else {
            Chip(priority.label, Color(0xFF6B6B6B), Color(0xFFEDEDED))
        }
        if (task.done) Chip("Đã xong", AppColors.Green, Color(0xFFE6F6EA))
    }
    Spacer(Modifier.height(6.dp))
    Text(task.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
    Spacer(Modifier.height(14.dp))

    Column(Modifier.fillMaxWidth().card()) {
        InfoRow(Icons.Outlined.CalendarMonth, "Hạn chót", task.whenLabel())
        HorizontalDivider(color = AppColors.Bg)
        InfoRow(
            Icons.Outlined.Notifications,
            "Nhắc nhở",
            value = if (task.remind) "Trước 15 phút" else "Tắt",
            onClick = { onPatch(mapOf("remind" to !task.remind)) },
        )
        HorizontalDivider(color = AppColors.Bg)
        InfoRow(Icons.Outlined.Repeat, "Lặp lại", "Không") // API không có trường "lặp lại"
    }
    Spacer(Modifier.height(14.dp))

    Text("Ghi chú", fontSize = 12.sp, color = AppColors.Gray)
    Spacer(Modifier.height(6.dp))
    Text(
        task.note.ifBlank { "Chưa có ghi chú" },
        color = if (task.note.isBlank()) AppColors.Gray else AppColors.Text,
        modifier = Modifier.fillMaxWidth().card().padding(14.dp),
    )
    Spacer(Modifier.height(14.dp))

    SubtaskList(task.subtasks, onChange = { newSubtasks -> onPatch(mapOf("subtasks" to newSubtasks)) })
    Spacer(Modifier.height(24.dp))

    Button(
        onClick = { onPatch(mapOf("done" to !task.done)) },
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
    ) {
        Text(if (task.done) "Đánh dấu chưa xong" else "Đánh dấu hoàn thành", color = Color.White)
    }
    TextButton(onClick = onDeleteClick, modifier = Modifier.align(Alignment.CenterHorizontally)) {
        Text("Xóa công việc", color = AppColors.Red)
    }
}

/**
 * Danh sách việc nhỏ: tick, sửa, xóa, thêm.
 * Mọi thay đổi tạo ra danh sách mới rồi gửi cả danh sách qua `onChange` (PATCH "subtasks").
 */
@Composable
private fun SubtaskList(subtasks: List<Subtask>, onChange: (List<Subtask>) -> Unit) {
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var newTitle by remember { mutableStateOf("") }
    val doneCount = subtasks.count { it.done }

    editingIndex?.let { index ->
        TextInputDialog(
            title = "Sửa việc nhỏ",
            initialText = subtasks[index].title,
            onDismiss = { editingIndex = null },
            onSave = { text ->
                editingIndex = null
                onChange(subtasks.replaceAt(index) { it.copy(title = text) })
            },
        )
    }

    Row {
        Text("Việc nhỏ", fontSize = 12.sp, color = AppColors.Gray, modifier = Modifier.weight(1f))
        Text("$doneCount/${subtasks.size}", fontSize = 12.sp, color = AppColors.Gray)
    }
    Spacer(Modifier.height(6.dp))
    LinearProgressIndicator(
        progress = { if (subtasks.isEmpty()) 0f else doneCount.toFloat() / subtasks.size },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))

    Column(Modifier.fillMaxWidth().card()) {
        subtasks.forEachIndexed { index, subtask ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = subtask.done,
                    onCheckedChange = { checked -> onChange(subtasks.replaceAt(index) { it.copy(done = checked) }) },
                )
                Text(
                    subtask.title,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (subtask.done) AppColors.Gray else AppColors.Text,
                    textDecoration = if (subtask.done) TextDecoration.LineThrough else null,
                )
                IconButton(onClick = { editingIndex = index }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Sửa", tint = AppColors.Gray, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onChange(subtasks.filterIndexed { i, _ -> i != index }) }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Xóa", tint = AppColors.Red, modifier = Modifier.size(18.dp))
                }
            }
            HorizontalDivider(color = AppColors.Bg)
        }

        // Dòng "+ Thêm việc nhỏ"
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.padding(8.dp))
            TextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                placeholder = { Text("Thêm việc nhỏ") },
                singleLine = true,
                colors = plainTextFieldColors(),
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = {
                    onChange(subtasks + Subtask(newTitle.trim()))
                    newTitle = ""
                },
                enabled = newTitle.isNotBlank(),
            ) { Text("Thêm") }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xóa công việc?") },
        text = { Text("\"$title\" sẽ bị xóa.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Xóa", color = AppColors.Red) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

/** Trả về danh sách mới, trong đó phần tử thứ `index` được thay bằng `transform(phần tử cũ)`. */
private fun <T> List<T>.replaceAt(index: Int, transform: (T) -> T): List<T> =
    mapIndexed { i, item -> if (i == index) transform(item) else item }
