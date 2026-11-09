package com.example.bt2firebaseall

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

// Các thành phần giao diện dùng lại ở nhiều màn.

/** Logo app: ô vuông bo góc màu chủ đạo, bên trong là biểu tượng danh sách việc. */
@Composable
fun AppLogo(size: Int) {
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.28).dp))
            .background(AppColors.Primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Checklist, contentDescription = null, tint = Color.White, modifier = Modifier.size((size * 0.6).dp))
    }
}

/**
 * Thanh trên cùng kiểu iOS như thiết kế:  [< Trang chủ]   Tiêu đề   [Sửa]
 * - `showBackArrow = false` cho màn Việc mới (bên trái chỉ là chữ "Hủy").
 * - `actionText = null` thì không có nút bên phải.
 */
@Composable
fun TopBar(
    leftText: String,
    onLeftClick: () -> Unit,
    title: String = "",
    showBackArrow: Boolean = true,
    actionText: String? = null,
    actionEnabled: Boolean = true,
    onActionClick: () -> Unit = {},
) {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            Modifier.align(Alignment.CenterStart).clickable(onClick = onLeftClick).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBackArrow) {
                Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(16.dp))
            }
            Text(leftText, color = AppColors.Primary, fontSize = 17.sp)
        }
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
        if (actionText != null) {
            Text(
                actionText,
                color = if (actionEnabled) AppColors.Primary else AppColors.Gray,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable(enabled = actionEnabled, onClick = onActionClick)
                    .padding(4.dp),
            )
        }
    }
}

// ---------- Khung chung cho 4 tab: nội dung + thanh điều hướng dưới có nút + ở giữa ----------

@Composable
fun TabScaffold(nav: NavController, currentRoute: String, content: @Composable (PaddingValues) -> Unit) {
    val context = LocalContext.current
    val user = AuthRepository.user ?: return

    // Vào app (vừa đăng nhập xong): GET /tasks một lần cho người dùng này
    LaunchedEffect(user.uid) {
        TaskRepository.start(user.uid)
        Tracker.setUser(context, user.uid)
        TaskRepository.load()
    }

    Scaffold(
        containerColor = AppColors.Bg,
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().background(AppColors.Card).padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TabItem(Icons.Outlined.Home, "Trang chủ", Routes.HOME, currentRoute, nav)
                TabItem(Icons.AutoMirrored.Outlined.List, "Danh sách", Routes.LIST, currentRoute, nav)
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    FloatingActionButton(
                        onClick = { nav.navigate(Routes.ADD) },
                        shape = CircleShape,
                        containerColor = AppColors.Primary,
                        modifier = Modifier.size(50.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Thêm việc", tint = Color.White)
                    }
                }
                TabItem(Icons.Outlined.Notifications, "Thông báo", Routes.NOTIFICATIONS, currentRoute, nav)
                TabItem(Icons.Outlined.Tune, "Cài đặt", Routes.SETTINGS, currentRoute, nav)
            }
        },
        content = content,
    )
}

@Composable
private fun RowScope.TabItem(icon: ImageVector, label: String, route: String, currentRoute: String, nav: NavController) {
    val isSelected = route == currentRoute
    val color = if (isSelected) AppColors.Primary else AppColors.Gray
    Column(
        Modifier
            .weight(1f)
            .clickable { if (!isSelected) nav.openTab(route) }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        Text(label, fontSize = 10.sp, color = color)
    }
}

/** Trạng thái tải danh sách việc: thanh tiến trình khi đang tải, hoặc thẻ báo lỗi + nút Thử lại. */
@Composable
fun LoadState() {
    val scope = rememberCoroutineScope()
    val error = TaskRepository.loadError.value

    if (error != null) {
        Column(Modifier.fillMaxWidth().card().padding(14.dp)) {
            Text("Không tải được công việc", fontWeight = FontWeight.SemiBold, color = AppColors.Red)
            Text(error, fontSize = 12.sp, color = AppColors.Gray)
            TextButton(onClick = { scope.launch { TaskRepository.load() } }) { Text("Thử lại") }
        }
    } else if (TaskRepository.loading.value) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

/** Ảnh đại diện tròn: có ảnh thì hiện ảnh, chưa có thì hiện chữ viết tắt của tên. */
@Composable
fun Avatar(name: String, size: Int, photo: ImageBitmap? = null, onClick: (() -> Unit)? = null) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color(0xFFE3E9FF))
            .then(clickable),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(photo, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(initials(name), color = AppColors.Primary, fontWeight = FontWeight.Bold, fontSize = (size / 2.6).sp)
        }
    }
}

/** Nhãn nhỏ có nền màu, ví dụ "Học tập", "Gấp". */
@Composable
fun Chip(text: String, textColor: Color, background: Color) {
    Text(
        text,
        color = textColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(background.tinted(textColor))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/**
 * Một dòng công việc: bấm vòng tròn = tick xong / bỏ tick, bấm vào dòng = mở Chi tiết.
 * `compact = true` (việc đã xong ở màn Danh sách) thì chỉ hiện tên, ẩn nhóm và giờ.
 */
@Composable
fun TaskRow(task: Task, timeLabel: String, onOpen: () -> Unit, onToggleDone: () -> Unit, compact: Boolean = false) {
    val isUrgent = Priority.from(task.priority) == Priority.HIGH && !task.done

    Row(
        Modifier.fillMaxWidth().card().clickable(onClick = onOpen).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (task.done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = if (task.done) "Bỏ đánh dấu" else "Đánh dấu xong",
            tint = if (task.done) AppColors.Primary else Color.LightGray,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onToggleDone),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                task.title,
                fontWeight = FontWeight.Medium,
                color = if (task.done) AppColors.Gray else AppColors.Text,
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
            )
            if (!compact) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Chip(task.cat.label, task.cat.text, task.cat.bg)
                    Spacer(Modifier.width(8.dp))
                    Text(timeLabel, fontSize = 12.sp, color = AppColors.Gray)
                }
            }
        }
        if (isUrgent) {
            Chip("Gấp", AppColors.Red, Color(0xFFFFE3E3))
        } else if (!compact) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

/** Tiêu đề nhỏ in hoa màu xám, ví dụ "CẦN LÀM", "ĐÃ XONG". */
@Composable
fun SectionLabel(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.Gray, modifier = Modifier.padding(top = 6.dp))
}

/** Dòng thông tin: biểu tượng + nhãn bên trái, giá trị in đậm bên phải. */
@Composable
fun InfoRow(icon: ImageVector, label: String, value: String, onClick: (() -> Unit)? = null) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(Modifier.fillMaxWidth().then(clickable).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = AppColors.Gray, modifier = Modifier.size(18.dp))
        Text("  $label", color = AppColors.Gray, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

/** Hộp thoại nhập một dòng chữ (sửa tên, sửa việc nhỏ...). */
@Composable
fun TextInputDialog(title: String, initialText: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true) },
        confirmButton = {
            TextButton(onClick = { onSave(text.trim()) }, enabled = text.isNotBlank()) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}
