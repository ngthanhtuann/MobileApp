package com.example.bt2firebaseall

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

// Tab Thông báo – Thực hành MVVM (Tuần 04)
// Kiến trúc: NotificationsScreen (View) ← NotificationsViewModel (ViewModel) ← TaskRepository / Inbox (Model)
//
// Tương tác:
//  • Bấm vào thẻ → đánh dấu đã đọc + navigate đến chi tiết task (nếu có taskId)
//  • Swipe sang trái → xóa thông báo đó
//  • "Đánh dấu đã đọc" → đánh dấu tất cả
//  • Sau 10 giây toàn bộ danh sách biến mất → hiển thị emptyView

@Composable
fun NotificationsScreen(
    nav: NavController,
    vm: NotificationsViewModel = viewModel(
        // Scope ViewModel vào NavBackStackEntry của route NOTIFICATIONS
        // → ViewModel tồn tại suốt vòng đời của tab, không bị tạo lại khi navigate back
        viewModelStoreOwner = nav.getBackStackEntry(Routes.NOTIFICATIONS),
    ),
) = TabScaffold(nav, Routes.NOTIFICATIONS) { padding ->

    // Refresh khi người dùng navigate back về tab này (currentBackStackEntry thay đổi thành NOTIFICATIONS)
    val currentEntry = nav.currentBackStackEntryAsState().value
    LaunchedEffect(currentEntry) {
        if (currentEntry?.destination?.route == Routes.NOTIFICATIONS) {
            vm.refresh()
        }
    }

    if (vm.isEmpty) {
        NotificationsEmptyView(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            onAddTask = { nav.navigate(Routes.ADD) },
            onGoSettings = { nav.openTab(Routes.SETTINGS) },
        )
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Thông báo",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { vm.markAllRead() }) {
                    Text("Đánh dấu đã đọc", color = AppColors.Primary, fontSize = 14.sp)
                }
            }

            // Gợi ý swipe nếu có thông báo chưa đọc
            val unread = vm.notifications.count { !it.isRead }
            if (unread > 0) {
                Text(
                    "Vuốt sang trái để xóa",
                    fontSize = 11.sp,
                    color = AppColors.Gray,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            // ── Danh sách ────────────────────────────────────────────────────
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(vm.notifications, key = { it.id }) { item ->
                    SwipeToDeleteWrapper(
                        onDelete = { vm.dismiss(item.id) },
                    ) {
                        NotificationCard(
                            item = item,
                            onClick = {
                                val taskId = vm.onTap(item.id)
                                if (taskId != null) nav.navigate(Routes.detail(taskId))
                            },
                        )
                    }
                }
            }
        }
    }
}

// ── Swipe-to-delete wrapper ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteWrapper(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { it == SwipeToDismissBoxValue.EndToStart },
    )

    // Khi swipe hoàn tất → xóa
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) onDelete()
    }

    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            // Nền đỏ hiện ra khi vuốt trái
            val color by animateColorAsState(
                targetValue = if (state.targetValue == SwipeToDismissBoxValue.EndToStart)
                    AppColors.Red else Color(0xFFFFE3E3),
                label = "swipe_bg",
            )
            val scale by animateFloatAsState(
                targetValue = if (state.targetValue == SwipeToDismissBoxValue.EndToStart) 1f else 0.75f,
                label = "swipe_icon_scale",
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Xóa",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp).scale(scale),
                )
            }
        },
        enableDismissFromStartToEnd = false,
    ) {
        content()
    }
}

// ── Thẻ thông báo ────────────────────────────────────────────────────────────

@Composable
private fun NotificationCard(item: NotificationItem, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .card()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon loại thông báo
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(notifIconBg(item.type)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when (item.type) {
                    NotifType.OVERDUE  -> Icons.Outlined.Warning
                    NotifType.DONE     -> Icons.Filled.CheckCircle
                    NotifType.UPCOMING -> Icons.Outlined.AccessTime
                    NotifType.REMINDER -> Icons.Outlined.NotificationsActive
                },
                contentDescription = null,
                tint = notifIconColor(item.type),
                modifier = Modifier.size(20.dp),
            )
        }

        // Tiêu đề + mô tả
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                item.title,
                fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.SemiBold,
                fontSize = 15.sp,
                color = AppColors.Text,
            )
            Text(
                item.body,
                fontSize = 12.sp,
                color = AppColors.Gray,
                lineHeight = 16.sp,
            )
        }

        // Thời gian + chấm chưa đọc + mũi tên (nếu có task đích)
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.time, fontSize = 11.sp, color = AppColors.Gray)
            if (!item.isRead) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AppColors.Primary),
                )
            }
        }
        if (item.taskId != null) {
            Icon(
                Icons.Outlined.ArrowForwardIos,
                contentDescription = "Xem chi tiết",
                tint = AppColors.Gray,
                modifier = Modifier.padding(start = 6.dp).size(14.dp),
            )
        }
    }
}

// ── EmptyView ────────────────────────────────────────────────────────────────

@Composable
private fun NotificationsEmptyView(
    modifier: Modifier = Modifier,
    onAddTask: () -> Unit,
    onGoSettings: () -> Unit,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(AppColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.NotificationsNone,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(50.dp),
                )
            }
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AppColors.Card),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = AppColors.Green,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Chưa có thông báo nào",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.Text,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Khi có việc sắp đến hạn, quá hạn hoặc\nlời nhắc, bạn sẽ thấy chúng ở đây.",
            fontSize = 14.sp,
            color = AppColors.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onAddTask,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(
                "  Thêm việc cần làm",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onGoSettings) {
            Text("Cài đặt nhắc việc", color = AppColors.Primary, fontSize = 14.sp)
        }
    }
}

// ── Màu icon theo loại ───────────────────────────────────────────────────────

private fun notifIconBg(type: NotifType): Color = when (type) {
    NotifType.OVERDUE  -> Color(0xFFFFE3E3)
    NotifType.DONE     -> Color(0xFFD3F9D8)
    NotifType.UPCOMING -> Color(0xFFE3E9FF)
    NotifType.REMINDER -> Color(0xFFFFF3BF)
}

private fun notifIconColor(type: NotifType): Color = when (type) {
    NotifType.OVERDUE  -> Color(0xFFE03131)
    NotifType.DONE     -> Color(0xFF2F9E44)
    NotifType.UPCOMING -> Color(0xFF3B5BDB)
    NotifType.REMINDER -> Color(0xFFF59F00)
}
