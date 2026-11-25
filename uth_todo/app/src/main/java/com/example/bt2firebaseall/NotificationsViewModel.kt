package com.example.bt2firebaseall

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.time.LocalDate

/**
 * ViewModel cho màn hình Thông báo (Thực hành MVVM – Tuần 04).
 *
 * Luồng:
 *  1. Khởi tạo: sinh danh sách thông báo từ TaskRepository (task thật) + Inbox (FCM).
 *  2. Bấm vào thẻ → đánh dấu đã đọc, trả về taskId để Screen navigate đến chi tiết.
 *  3. Swipe / bấm X → xóa một thông báo khỏi danh sách.
 *  4. "Đánh dấu đã đọc" → đánh dấu tất cả.
 *  5. Khi tasks thay đổi (tick xong, thêm mới...) → gọi refresh() để cập nhật danh sách.
 */
class NotificationsViewModel : ViewModel() {

    var notifications by mutableStateOf<List<NotificationItem>>(emptyList())
        private set

    val isEmpty: Boolean get() = notifications.isEmpty()

    /** Flag nội bộ: nếu người dùng đã xóa tất cả thủ công, không tự refresh nữa. */
    private var manuallyCleared = false

    init {
        refresh()
    }

    /** Làm mới danh sách từ TaskRepository + Inbox. Gọi khi tasks thay đổi hoặc navigate back về tab. */
    fun refresh() {
        if (manuallyCleared) return  // Người dùng đã chủ động xóa hết
        val fromTasks = buildFromTasks(TaskRepository.tasks)
        val fromInbox = buildFromInbox(Inbox.messages)
        val all = (fromInbox + fromTasks).ifEmpty { sampleNotifications() }
        
        // Giữ lại trạng thái isRead của các thông báo cũ khi refresh
        notifications = all.map { new ->
            val existing = notifications.find { it.id == new.id }
            if (existing != null) new.copy(isRead = existing.isRead) else new
        }
    }

    /** Bấm vào thẻ: đánh dấu đã đọc rồi trả taskId (Screen dùng để navigate). */
    fun onTap(id: Int): String? {
        val item = notifications.find { it.id == id } ?: return null
        notifications = notifications.map { if (it.id == id) it.copy(isRead = true) else it }
        return item.taskId
    }

    /** Đánh dấu tất cả đã đọc. */
    fun markAllRead() {
        notifications = notifications.map { it.copy(isRead = true) }
    }

    /** Xóa một thông báo khỏi danh sách. */
    fun dismiss(id: Int) {
        notifications = notifications.filter { it.id != id }
        if (notifications.isEmpty()) manuallyCleared = true
    }

    /** Xóa tất cả thông báo (nút "Xóa tất cả" nếu muốn). */
    fun clearAll() {
        notifications = emptyList()
        manuallyCleared = true
    }

    // ── Xây danh sách từ task thật ──────────────────────────────────────────

    private fun buildFromTasks(tasks: List<Task>): List<NotificationItem> {
        val today = LocalDate.now()
        val result = mutableListOf<NotificationItem>()
        var id = 1000  // tránh trùng id với Inbox (bắt đầu từ 0)

        tasks.filter { !it.done }.forEach { task ->
            val taskDate = runCatching { LocalDate.parse(task.date) }.getOrNull() ?: return@forEach
            val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, taskDate)

            when {
                // Quá hạn
                daysLeft < 0 -> result.add(
                    NotificationItem(
                        id = id++,
                        taskId = task.id,
                        title = "Đã quá hạn",
                        body = "${task.title} – ${task.whenLabel()}",
                        time = task.time,
                        type = NotifType.OVERDUE,
                        isRead = false,
                    )
                )
                // Hôm nay hoặc ngày mai
                daysLeft in 0..1 -> result.add(
                    NotificationItem(
                        id = id++,
                        taskId = task.id,
                        title = "Sắp đến hạn",
                        body = "${task.title} – ${task.whenLabel()}",
                        time = task.time,
                        type = NotifType.UPCOMING,
                        isRead = false,
                    )
                )
                // Trong tuần + có nhắc
                daysLeft in 2..6 && task.remind -> result.add(
                    NotificationItem(
                        id = id++,
                        taskId = task.id,
                        title = "Nhắc việc",
                        body = "${task.title} – ${task.whenLabel()}",
                        time = task.time,
                        type = NotifType.REMINDER,
                        isRead = false,
                    )
                )
            }
        }

        // Thêm thông báo "Làm tốt lắm!" nếu hôm nay có việc xong
        val doneToday = tasks.count { it.done && it.isToday }
        if (doneToday > 0) {
            result.add(
                NotificationItem(
                    id = id,
                    taskId = null,
                    title = "Làm tốt lắm!",
                    body = "Bạn đã xong $doneToday việc${if (doneToday > 1) "" else ""} trong hôm nay.",
                    time = "Hôm nay",
                    type = NotifType.DONE,
                    isRead = true,
                )
            )
        }

        return result
    }

    // ── Xây danh sách từ FCM Inbox ──────────────────────────────────────────

    private fun buildFromInbox(messages: List<String>): List<NotificationItem> =
        messages.mapIndexed { index, msg ->
            val parts = msg.split(": ", limit = 2)
            NotificationItem(
                id = index,
                taskId = null,          // FCM không gắn taskId
                title = parts.getOrElse(0) { msg },
                body = parts.getOrElse(1) { "" },
                time = "Vừa xong",
                type = NotifType.REMINDER,
                isRead = false,
            )
        }

    // ── Dữ liệu mẫu (fallback khi chưa đăng nhập / chưa có task) ───────────

    private fun sampleNotifications() = listOf(
        NotificationItem(0, null, "Sắp đến hạn",  "Ôn chương 3 – Cấu trúc dữ liệu lúc 14:00.", "13:45", NotifType.UPCOMING,  false),
        NotificationItem(1, null, "Đã quá hạn",   "Bài đọc tham khảo môn Mạng máy tính chưa xong.", "10:10", NotifType.OVERDUE, false),
        NotificationItem(2, null, "Làm tốt lắm!", "Bạn đã xong 2 việc trong sáng nay.", "11:30", NotifType.DONE,     true),
        NotificationItem(3, null, "Nhắc việc",    "Họp nhóm đồ ăn trong tuần này.", "Hôm qua", NotifType.REMINDER, true),
    )
}

/** Một thông báo trong danh sách. `taskId != null` → có thể navigate đến chi tiết task đó. */
data class NotificationItem(
    val id     : Int,
    val taskId : String?,      // null = thông báo chung (FCM, "làm tốt lắm"...)
    val title  : String,
    val body   : String,
    val time   : String,
    val type   : NotifType,
    val isRead : Boolean,
)

/** Loại thông báo – xác định màu icon. */
enum class NotifType { UPCOMING, OVERDUE, DONE, REMINDER }
