package com.example.bt2firebaseall

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatsTest {
    private val today = LocalDate.of(2026, 11, 11) // Thứ Tư

    private fun task(daysAgo: Long, done: Boolean) = Task(date = today.minusDays(daysAgo).toString(), done = done)

    @Test
    fun streakOnTimeAndWeek() {
        val tasks = listOf(
            task(daysAgo = 0, done = false),  // hôm nay (T4) chưa xong -> chuỗi tính từ hôm qua
            task(daysAgo = 1, done = true),   // T3
            task(daysAgo = 2, done = true),   // T2
            task(daysAgo = 4, done = true),   // T7 tuần trước; ngày 3 trống nên chuỗi dừng ở 2
            task(daysAgo = -1, done = false), // ngày mai (T5): chưa tới hạn
        )

        val stats = calculateStats(tasks, today)

        assertEquals(3, stats.doneCount)
        assertEquals(2, stats.streakDays)
        assertEquals(75, stats.onTimePercent) // 3 việc xong / 4 việc đã tới hạn
        assertEquals(
            listOf(
                DayCount(done = 1, total = 1), // T2
                DayCount(done = 1, total = 1), // T3
                DayCount(done = 0, total = 1), // T4
                DayCount(done = 0, total = 1), // T5
                DayCount(done = 0, total = 0), // T6
                DayCount(done = 0, total = 0), // T7
                DayCount(done = 0, total = 0), // CN
            ),
            stats.week,
        )
    }
}
