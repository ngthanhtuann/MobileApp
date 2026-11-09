package com.example.bt2firebaseall

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate

// Màn Hồ sơ: ảnh đại diện (Firebase Storage), tên, thống kê, biểu đồ tuần, đồng bộ, đăng xuất.

@Composable
fun ProfileScreen(backLabel: String, onBack: () -> Unit, onLoggedOut: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = AuthRepository.user ?: return
    val stats = calculateStats(TaskRepository.tasks)

    var userName by remember { mutableStateOf(user.displayName()) }
    var photo by remember { mutableStateOf<ImageBitmap?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf("Chạm để đồng bộ") }

    LaunchedEffect(user.uid) { photo = AvatarRepository.load(user.uid) }

    // Chọn ảnh trong máy -> tải lên Firebase Storage -> hiện ảnh mới
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isUploading = true
        scope.launch {
            try {
                AvatarRepository.upload(user.uid, uri)
                photo = AvatarRepository.load(user.uid)
                Toast.makeText(context, "Đã tải ảnh lên Firebase Storage", Toast.LENGTH_SHORT).show()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(context, "Tải ảnh thất bại: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isUploading = false
            }
        }
    }

    if (isEditingName) {
        TextInputDialog(
            title = "Sửa tên hiển thị",
            initialText = userName,
            onDismiss = { isEditingName = false },
            onSave = { newName ->
                isEditingName = false
                scope.call(context, onDone = { userName = newName }) { AuthRepository.updateName(newName) }
            },
        )
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopBar(
            leftText = backLabel,
            onLeftClick = onBack,
            title = "Hồ sơ",
            actionText = "Sửa",
            onActionClick = { isEditingName = true },
        )
        Spacer(Modifier.height(24.dp))

        AvatarWithCameraBadge(
            name = userName,
            photo = photo,
            isUploading = isUploading,
            onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        )
        Spacer(Modifier.height(12.dp))
        Text(userName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        user.email?.let { Text(it, color = AppColors.Gray) }
        Spacer(Modifier.height(8.dp))
        AccountTypeBadge(user)
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("${stats.doneCount}", "Việc đã xong", Modifier.weight(1f))
            StatCard("${stats.streakDays} ngày", "Chuỗi liên tiếp", Modifier.weight(1f))
            StatCard("${stats.onTimePercent}%", "Đúng hạn", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        WeekChart(stats.week)
        Spacer(Modifier.height(12.dp))

        Column(Modifier.fillMaxWidth().card(16)) {
            ProfileRow(Icons.Outlined.Sync, "Đồng bộ dữ liệu", value = syncStatus, onClick = {
                syncStatus = "Đang đồng bộ…"
                scope.launch {
                    TaskRepository.load() // GET /tasks
                    val error = TaskRepository.loadError.value
                    syncStatus = if (error == null) "Vừa xong" else "Lỗi: $error"
                }
            })
            HorizontalDivider(color = AppColors.Bg)
            ProfileRow(Icons.Outlined.Lock, "Quyền riêng tư", value = "", onClick = { openAppPermissionSettings(context) })
        }
        Spacer(Modifier.height(24.dp))

        // Bài Tuần 5 – BT02: nút thử Crashlytics
        TextButton(onClick = { throw RuntimeException("Test crash từ UTH Todo (Crashlytics)") }) {
            Text("Gửi crash thử (Crashlytics)", color = AppColors.Gray, fontSize = 12.sp)
        }
        LogoutButton(onClick = {
            scope.launch {
                AuthRepository.signOut(context)
                Toast.makeText(context, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
                onLoggedOut()
            }
        })
    }
}

/** "Quyền riêng tư": mở trang quyền của app trong Cài đặt máy (Vị trí, Thông báo, Camera của Nâng cao 01). */
private fun openAppPermissionSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    context.startActivity(intent)
}

@Composable
private fun AvatarWithCameraBadge(name: String, photo: ImageBitmap?, isUploading: Boolean, onClick: () -> Unit) {
    Box {
        Avatar(name, size = 100, photo = photo, onClick = onClick)
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(32.dp)
                .clip(CircleShape)
                .background(AppColors.Primary)
                .border(2.dp, AppColors.Bg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.PhotoCamera, contentDescription = "Đổi ảnh", tint = Color.White, modifier = Modifier.size(16.dp))
        }
        if (isUploading) CircularProgressIndicator(Modifier.size(100.dp))
    }
}

/** Nhãn xanh cho biết cách đăng nhập: Google / Email / Khách. */
@Composable
private fun AccountTypeBadge(user: FirebaseUser) {
    val isGoogle = user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
    val text = when {
        isGoogle -> "✓ Đã liên kết tài khoản Google"
        user.isAnonymous -> "Tài khoản khách (ẩn danh)"
        else -> "✓ Đăng nhập bằng Email"
    }
    Text(
        text,
        color = AppColors.Green,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFE6F6EA).tinted(AppColors.Green))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier) {
    Column(modifier.card(16).padding(14.dp)) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, fontSize = 12.sp, color = AppColors.Gray)
    }
}

/**
 * Biểu đồ cột "Tuần này": mỗi cột là một ngày, cao theo số việc của ngày đó.
 * Hôm nay: màu chủ đạo đậm. Ngày có việc xong: màu chủ đạo nhạt. Còn lại: xám.
 */
@Composable
private fun WeekChart(week: List<DayCount>) {
    val dayNames = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
    val todayIndex = LocalDate.now().dayOfWeek.value - 1 // Thứ Hai = 0
    val maxTotal = week.maxOf { it.total }.coerceAtLeast(1)

    Column(Modifier.fillMaxWidth().card(16).padding(16.dp)) {
        Row {
            Text("Tuần này", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${week.sumOf { it.done }}/${week.sumOf { it.total }} việc", color = AppColors.Gray)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().height(110.dp), verticalAlignment = Alignment.Bottom) {
            week.forEachIndexed { index, day ->
                val isToday = index == todayIndex
                val barColor = when {
                    isToday -> AppColors.Primary
                    day.done > 0 -> AppColors.Primary.copy(alpha = 0.35f)
                    else -> AppColors.Gray.copy(alpha = 0.2f)
                }
                val barHeight = 14 + 70f * day.total / maxTotal // tối thiểu 14dp để ngày trống vẫn thấy cột

                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.width(28.dp).height(barHeight.dp).clip(RoundedCornerShape(8.dp)).background(barColor))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        dayNames[index],
                        fontSize = 11.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) AppColors.Text else AppColors.Gray,
                    )
                }
            }
        }
    }
}

/** Dòng trong thẻ cuối màn: biểu tượng + tên, bên phải là trạng thái (hoặc mũi tên ">" khi không có). */
@Composable
private fun ProfileRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = AppColors.Gray, modifier = Modifier.size(20.dp))
        Text(title, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
        if (value.isEmpty()) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
        } else {
            Text(value, color = AppColors.Gray, maxLines = 1)
        }
    }
}

@Composable
private fun LogoutButton(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().card(16).clickable(onClick = onClick).padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = AppColors.Red, modifier = Modifier.size(18.dp))
        Text("  Đăng xuất", color = AppColors.Red, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}
