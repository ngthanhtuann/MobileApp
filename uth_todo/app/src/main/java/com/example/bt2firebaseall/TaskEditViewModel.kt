package com.example.bt2firebaseall

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel cho màn hình Thêm / Sửa công việc (MVVM – Tuần 04).
 * Screen chỉ đọc state từ đây và gọi các hàm để thay đổi; không chứa logic nào.
 */
class TaskEditViewModel : ViewModel() {

    // ── Form state ──────────────────────────────────────────────────────────
    var title    by mutableStateOf("")
    var note     by mutableStateOf("")
    var category by mutableStateOf(Category.STUDY)
    var priority by mutableStateOf(Priority.MEDIUM)
    var date     by mutableStateOf(LocalDate.now().toString())
    var time     by mutableStateOf("19:00")
    var remind   by mutableStateOf(true)

    // ── UI state ─────────────────────────────────────────────────────────────
    var isLoaded  by mutableStateOf(false)   // false khi đang tải dữ liệu (chế độ sửa)
    var isSaving  by mutableStateOf(false)
    var errorMsg  by mutableStateOf<String?>(null)

    val canSave: Boolean get() = isLoaded && title.isNotBlank() && !isSaving

    // ── Khởi tạo cho chế độ Thêm mới (không cần tải) ───────────────────────
    fun initNew() {
        isLoaded = true
    }

    // ── Tải dữ liệu (chế độ Sửa): GET /tasks/{id} ───────────────────────────
    fun load(taskId: String) {
        viewModelScope.launch {
            try {
                val task = TaskRepository.get(taskId)
                title    = task.title
                note     = task.note
                category = task.cat
                priority = Priority.from(task.priority)
                date     = task.date
                time     = task.time
                remind   = task.remind
                isLoaded = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMsg = e.toMessage()
            }
        }
    }

    // ── Lưu: POST (mới) hoặc PATCH (sửa) ───────────────────────────────────
    fun save(context: Context, taskId: String?, onDone: () -> Unit) {
        if (!canSave) return
        isSaving = true
        val task = Task(
            title    = title.trim(),
            note     = note.trim(),
            category = category.key,
            date     = date,
            time     = time,
            priority = priority.key,
            remind   = remind,
        )
        viewModelScope.launch {
            try {
                if (taskId == null) {
                    TaskRepository.add(task)
                    Tracker.log(context, "add_task", "category" to task.category)
                } else {
                    TaskRepository.update(taskId, task.toChangeMap())
                }
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMsg = e.toMessage()
            } finally {
                isSaving = false
            }
        }
    }

    fun clearError() { errorMsg = null }
}

/** Các trường cần gửi lên khi PATCH. */
private fun Task.toChangeMap(): Map<String, Any> = mapOf(
    "title"    to title,
    "note"     to note,
    "category" to category,
    "date"     to date,
    "time"     to time,
    "priority" to priority,
    "remind"   to remind,
)
