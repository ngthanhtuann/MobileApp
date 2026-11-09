package com.example.bt2firebaseall

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.School
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.firebase.auth.FirebaseUser
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// Nhóm, mức ưu tiên và cách hiển thị ngày giờ / tên người dùng.

val Vietnamese: Locale = Locale.forLanguageTag("vi")

/** Nhóm công việc. `key` là giá trị trong JSON của API ("study" | "life"). */
enum class Category(val label: String, val text: Color, val bg: Color, val icon: ImageVector) {
    STUDY("Học tập", Color(0xFF3B5BDB), Color(0xFFE3E9FF), Icons.Outlined.School),
    LIFE("Cuộc sống", Color(0xFFD9822B), Color(0xFFFFEBD6), Icons.Outlined.Home);

    val key: String get() = name.lowercase()

    companion object {
        fun from(key: String): Category = entries.find { it.key == key.lowercase() } ?: STUDY
    }
}

/** Mức ưu tiên. `key` là giá trị trong JSON của API ("low" | "medium" | "high"). */
enum class Priority(val short: String) {
    LOW("Thấp"),
    MEDIUM("Vừa"),
    HIGH("Cao");

    val label: String get() = "Ưu tiên ${short.lowercase()}"
    val key: String get() = name.lowercase()

    companion object {
        fun from(key: String): Priority = entries.find { it.key == key.lowercase() } ?: MEDIUM
    }
}

val Task.cat: Category get() = Category.from(category)
val Task.isToday: Boolean get() = date == LocalDate.now().toString()

/** Hạn chót dạng dễ đọc: "Hôm nay, 14:00", "Ngày mai, 21:00", "Tuần này, 20:00", "Quá hạn 18/10, 09:00"... */
fun Task.whenLabel(): String {
    val day = runCatching { LocalDate.parse(date) }.getOrNull() ?: return time
    val shortDate = day.format(DateTimeFormatter.ofPattern("dd/MM"))
    val daysFromToday = ChronoUnit.DAYS.between(LocalDate.now(), day)
    val dayText = when {
        daysFromToday == 0L -> "Hôm nay"
        daysFromToday == 1L -> "Ngày mai"
        daysFromToday == -1L -> "Hôm qua"
        daysFromToday in 2L..6L -> "Tuần này"
        daysFromToday < 0 -> "Quá hạn $shortDate"
        else -> shortDate
    }
    return "$dayText, $time"
}

/** "2026-10-21" -> "Thứ Tư, 21/10" (dùng ở màn Việc mới). */
fun dateLabel(date: String): String = runCatching {
    LocalDate.parse(date)
        .format(DateTimeFormatter.ofPattern("EEEE, dd/MM", Vietnamese))
        .replaceFirstChar { it.uppercase() }
}.getOrDefault(date)

/** Tên hiển thị: tên Google, hoặc phần trước @ của email, hoặc "Khách" khi dùng thử. */
fun FirebaseUser?.displayName(): String = when {
    this == null -> ""
    isAnonymous -> "Khách"
    !displayName.isNullOrBlank() -> displayName!!
    else -> email?.substringBefore('@') ?: "Bạn"
}

/** Chữ viết tắt trên avatar: "Minh Anh" -> "MA", "Khách" -> "K". */
fun initials(name: String): String {
    val words = name.split(" ").filter { it.isNotBlank() }
    val first = words.firstOrNull()
    val last = if (words.size > 1) words.last() else null
    return listOfNotNull(first, last).joinToString("") { it.first().uppercase() }
}
