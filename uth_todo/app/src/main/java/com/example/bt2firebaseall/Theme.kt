package com.example.bt2firebaseall

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.edit

// Màu sắc và giao diện chung. Màn Cài đặt › Giao diện đổi các giá trị trong AppColors.

/** Chế độ giao diện, đúng thứ tự 3 ô trong màn Cài đặt. */
enum class ThemeMode(val label: String) { LIGHT("Sáng"), DARK("Tối"), SYSTEM("Tự động") }

/**
 * Màu của toàn app. Các biến dùng mutableStateOf nên khi đổi trong Cài đặt,
 * mọi màn đang hiển thị tự vẽ lại với màu mới.
 */
object AppColors {
    /** 4 màu chủ đạo trong Cài đặt: xanh dương, xanh lá, cam, tím. */
    val Palette = listOf(Color(0xFF2D5BE3), Color(0xFF14855F), Color(0xFFC2410C), Color(0xFF7C3AED))
    val Green = Color(0xFF2F9E44)
    val Red = Color(0xFFE03131)
    val SwitchOn = Color(0xFF34C759)

    var mode by mutableStateOf(ThemeMode.LIGHT)
    var systemDark by mutableStateOf(false) // máy đang bật giao diện tối? (dùng cho chế độ Tự động)
    var colorIndex by mutableIntStateOf(0)
    var fontScale by mutableFloatStateOf(1f)

    val dark: Boolean
        get() = when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> systemDark
        }

    val Primary get() = Palette[colorIndex]
    val Bg get() = if (dark) Color(0xFF000000) else Color(0xFFF2F3F7)
    val Card get() = if (dark) Color(0xFF1C1C1E) else Color.White
    val Text get() = if (dark) Color(0xFFECECF1) else Color(0xFF1C1C1E)
    val Gray get() = if (dark) Color(0xFF9A9BA3) else Color(0xFF6B6B6B)

    private fun prefs(context: Context) = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Đọc lựa chọn đã lưu khi mở app. */
    fun load(context: Context) {
        val prefs = prefs(context)
        mode = ThemeMode.entries.getOrElse(prefs.getInt("mode", 0)) { ThemeMode.LIGHT }
        colorIndex = prefs.getInt("color", 0).coerceIn(Palette.indices)
        fontScale = prefs.getFloat("font", 1f)
    }

    /** Lưu lựa chọn để lần sau mở app vẫn giữ nguyên. */
    fun save(context: Context) {
        prefs(context).edit {
            putInt("mode", mode.ordinal)
            putInt("color", colorIndex)
            putFloat("font", fontScale)
        }
    }
}

/** Nền của nhãn (chip). Giao diện tối dùng màu chữ pha mờ, vì nền sáng sẽ quá chói trên nền đen. */
fun Color.tinted(textColor: Color): Color = if (AppColors.dark) textColor.copy(alpha = 0.22f) else this

/** Thẻ bo góc, nền trắng (nền xám đậm khi bật giao diện tối). */
fun Modifier.card(radius: Int = 12): Modifier = this.clip(RoundedCornerShape(radius.dp)).background(AppColors.Card)

/** Công tắc màu xanh lá như trong thiết kế. */
@Composable
fun greenSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = AppColors.SwitchOn,
    checkedBorderColor = Color.Transparent,
)

/** Ô nhập không viền, không nền: dùng khi ô nhập nằm sẵn trong một thẻ. */
@Composable
fun plainTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
)
