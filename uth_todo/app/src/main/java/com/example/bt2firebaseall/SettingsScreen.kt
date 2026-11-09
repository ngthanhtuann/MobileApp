package com.example.bt2firebaseall

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.navigation.NavController

// Màn Cài đặt: 3 nhóm GIAO DIỆN, THÔNG BÁO, CHUNG như thiết kế.

@Composable
fun SettingsScreen(nav: NavController) = TabScaffold(nav, Routes.SETTINGS) { padding ->
    val userName = AuthRepository.user.displayName()

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Cài đặt", fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Avatar(userName, size = 44, onClick = { nav.navigate(Routes.PROFILE) })
        }
        AppearanceSection()
        NotificationSection()
        GeneralSection(nav, userName)
        Spacer(Modifier.height(20.dp))
    }
}

// ---------- GIAO DIỆN: chế độ sáng/tối, màu chủ đạo, cỡ chữ ----------

@Composable
private fun AppearanceSection() {
    val context = LocalContext.current

    SettingsGroup("GIAO DIỆN") {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ThemeMode.entries.forEach { mode ->
                ThemeTile(
                    mode = mode,
                    selected = AppColors.mode == mode,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        AppColors.mode = mode
                        AppColors.save(context)
                    },
                )
            }
        }
        HorizontalDivider(color = AppColors.Bg)

        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Màu chủ đạo", Modifier.weight(1f), fontSize = 16.sp)
            AppColors.Palette.forEachIndexed { index, color ->
                ColorDot(
                    color = color,
                    selected = AppColors.colorIndex == index,
                    onClick = {
                        AppColors.colorIndex = index
                        AppColors.save(context)
                    },
                )
            }
        }
        HorizontalDivider(color = AppColors.Bg)

        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Cỡ chữ", Modifier.weight(1f), fontSize = 16.sp)
            Text("A", fontSize = 12.sp)
            Slider(
                value = AppColors.fontScale,
                onValueChange = { AppColors.fontScale = it },
                onValueChangeFinished = { AppColors.save(context) },
                valueRange = 0.85f..1.25f,
                modifier = Modifier.width(120.dp).padding(horizontal = 6.dp),
            )
            Text("A", fontSize = 20.sp)
        }
    }
}

/** Ô xem trước giao diện: Sáng (nền sáng), Tối (nền đen), Tự động (nửa sáng nửa tối). */
@Composable
private fun ThemeTile(mode: ThemeMode, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val light = Color(0xFFF2F3F7)
    val dark = Color.Black
    val background = when (mode) {
        ThemeMode.LIGHT -> Brush.linearGradient(listOf(light, light))
        ThemeMode.DARK -> Brush.linearGradient(listOf(dark, dark))
        ThemeMode.SYSTEM -> Brush.horizontalGradient(0f to light, 0.5f to light, 0.5f to dark, 1f to dark)
    }
    val borderColor = if (selected) AppColors.Primary else AppColors.Gray.copy(alpha = 0.3f)
    val placeholderBar = Color(0x66888888)

    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.35f)
                .clip(RoundedCornerShape(12.dp))
                .background(background)
                .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.fillMaxWidth(0.45f).height(6.dp).clip(CircleShape).background(placeholderBar))
            repeat(2) {
                Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).background(placeholderBar))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            mode.label,
            color = if (selected) AppColors.Primary else AppColors.Gray,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Chấm màu chủ đạo; màu đang chọn có thêm vòng viền bên ngoài. */
@Composable
private fun ColorDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    val ring = if (selected) Modifier.border(2.dp, color, CircleShape).padding(3.dp) else Modifier
    Box(
        Modifier
            .padding(start = 10.dp)
            .size(32.dp)
            .then(ring)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
    )
}

// ---------- THÔNG BÁO ----------

// shortcut: 3 công tắc chỉ lưu lựa chọn, chưa hẹn giờ gửi nhắc thật; muốn nhắc thật thì dùng AlarmManager hoặc WorkManager
@Composable
private fun NotificationSection() {
    SettingsGroup("THÔNG BÁO") {
        SwitchRow("Nhắc việc", "Báo trước hạn chót 15 phút", prefKey = "notify_remind", defaultOn = true)
        HorizontalDivider(color = AppColors.Bg)
        SwitchRow("Âm thanh", "Phát âm khi có thông báo", prefKey = "notify_sound", defaultOn = true)
        HorizontalDivider(color = AppColors.Bg)
        SwitchRow("Tổng kết cuối ngày", "Gửi lúc 21:00 mỗi tối", prefKey = "notify_summary", defaultOn = false)
    }
}

/** Một dòng có công tắc; trạng thái lưu vào SharedPreferences theo `prefKey`. */
@Composable
private fun SwitchRow(title: String, subtitle: String, prefKey: String, defaultOn: Boolean) {
    val prefs = LocalContext.current.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var isOn by remember { mutableStateOf(prefs.getBoolean(prefKey, defaultOn)) }

    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = AppColors.Gray)
        }
        Switch(
            checked = isOn,
            onCheckedChange = { checked ->
                isOn = checked
                prefs.edit { putBoolean(prefKey, checked) }
            },
            colors = greenSwitchColors(),
        )
    }
}

// ---------- CHUNG ----------

@Composable
private fun GeneralSection(nav: NavController, userName: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    SettingsGroup("CHUNG") {
        LinkRow("Ngôn ngữ", value = "Tiếng Việt", onClick = null) // app chỉ có tiếng Việt
        HorizontalDivider(color = AppColors.Bg)
        LinkRow("Hồ sơ cá nhân", value = userName, onClick = { nav.navigate(Routes.PROFILE) })
        HorizontalDivider(color = AppColors.Bg)
        LinkRow("Giá crypto realtime", value = "Nâng cao 02", onClick = { nav.navigate(Routes.CRYPTO) })
        HorizontalDivider(color = AppColors.Bg)
        LinkRow("Đặt lại dữ liệu mẫu", value = "", onClick = {
            scope.call(context, onDone = { Toast.makeText(context, "Đã đặt lại dữ liệu mẫu", Toast.LENGTH_SHORT).show() }) {
                TaskRepository.resetSamples()
            }
        })
    }
}

/** Dòng có giá trị bên phải; có `onClick` thì hiện thêm mũi tên ">". */
@Composable
private fun LinkRow(title: String, value: String, onClick: (() -> Unit)?) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(Modifier.fillMaxWidth().then(clickable).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(value, color = AppColors.Gray)
        if (onClick != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

/** Tiêu đề nhóm (chữ in hoa, xám) + thẻ trắng chứa các dòng cài đặt. */
@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppColors.Gray,
        modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 8.dp),
    )
    Column(Modifier.fillMaxWidth().card(16), content = content)
}
