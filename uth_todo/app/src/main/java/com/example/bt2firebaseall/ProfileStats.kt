package com.example.bt2firebaseall

import java.time.DayOfWeek
import java.time.LocalDate

// Tính số liệu cho màn Hồ sơ từ danh sách công việc (không có giao diện, có unit test: StatsTest).

/** Số việc trong một ngày của biểu đồ "Tuần này". */
data class DayCount(val done: Int, val total: Int)

data class ProfileStats(
    val doneCount: Int,     // "Việc đã xong"
    val streakDays: Int,    // "Chuỗi liên tiếp"
    val onTimePercent: Int, // "Đúng hạn"
    val week: List<DayCount>, // 7 phần tử: Thứ 2 -> Chủ nhật của tuần này
)

fun calculateStats(tasks: List<Task>, today: LocalDate = LocalDate.now()): ProfileStats {
    val tasksByDate: Map<LocalDate?, List<Task>> = tasks.groupBy { runCatching { LocalDate.parse(it.date) }.getOrNull() }

    fun hasDoneTask(day: LocalDate): Boolean = tasksByDate[day].orEmpty().any { it.done }

    // Chuỗi liên tiếp: đếm lùi từng ngày, ngày nào có ít nhất 1 việc xong thì +1, gặp ngày không có thì dừng.
    // Hôm nay chưa xong việc nào thì chưa tính là đứt chuỗi -> bắt đầu đếm từ hôm qua.
    var day = if (hasDoneTask(today)) today else today.minusDays(1)
    var streakDays = 0
    while (hasDoneTask(day)) {
        streakDays++
        day = day.minusDays(1)
    }

    // Đúng hạn: trong các việc đã tới hạn (ngày <= hôm nay), bao nhiêu phần trăm đã xong.
    val dueTasks = tasks.filter { task ->
        val date = runCatching { LocalDate.parse(task.date) }.getOrNull()
        date != null && !date.isAfter(today)
    }
    val onTimePercent = if (dueTasks.isEmpty()) 0 else dueTasks.count { it.done } * 100 / dueTasks.size

    // Tuần này: Thứ 2 -> Chủ nhật
    val monday = today.with(DayOfWeek.MONDAY)
    val week = (0L..6L).map { offset ->
        val tasksOfDay = tasksByDate[monday.plusDays(offset)].orEmpty()
        DayCount(done = tasksOfDay.count { it.done }, total = tasksOfDay.size)
    }

    return ProfileStats(
        doneCount = tasks.count { it.done },
        streakDays = streakDays,
        onTimePercent = onTimePercent,
        week = week,
    )
}
