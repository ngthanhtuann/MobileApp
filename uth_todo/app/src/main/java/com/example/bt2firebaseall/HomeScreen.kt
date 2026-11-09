package com.example.bt2firebaseall

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Trang chủ: lời chào, tiến độ hôm nay, thống kê theo nhóm, danh sách việc hôm nay.

@Composable
fun HomeScreen(nav: NavController) = TabScaffold(nav, Routes.HOME) { padding ->
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allTasks = TaskRepository.tasks
    val todayTasks = allTasks.filter { it.isToday }
    val todayDoneCount = todayTasks.count { it.done }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item { Greeting(nav) }
        item { ProgressCard(done = todayDoneCount, total = todayTasks.size, message = RemoteConfigRepo.homeMessage.value) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Category.entries.forEach { category ->
                    CategoryCard(
                        category = category,
                        remaining = allTasks.count { it.cat == category && !it.done },
                        modifier = Modifier.weight(1f),
                        onClick = { nav.openTab(Routes.LIST) },
                    )
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Việc hôm nay", fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    "Xem tất cả",
                    color = AppColors.Primary,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { nav.openTab(Routes.LIST) },
                )
            }
        }
        item { LoadState() }
        if (todayTasks.isEmpty() && !TaskRepository.loading.value) {
            item { Text("Hôm nay chưa có việc nào 🎉", color = AppColors.Gray) }
        }
        items(todayTasks, key = { it.id }) { task ->
            TaskRow(
                task = task,
                timeLabel = task.time,
                onOpen = { nav.navigate(Routes.detail(task.id)) },
                onToggleDone = { scope.call(context) { TaskRepository.toggleDone(task) } },
            )
        }
    }
}

/** Avatar + ngày + "Chào <tên>!" bên trái, nút chuông Thông báo bên phải. */
@Composable
private fun Greeting(nav: NavController) {
    val userName = AuthRepository.user.displayName()
    val todayText = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE, d 'tháng' M", Vietnamese))
        .replaceFirstChar { it.uppercase() }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // Nhấn vào avatar hoặc tên để mở Hồ sơ
        Row(
            Modifier.weight(1f).clickable { nav.navigate(Routes.PROFILE) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(userName, size = 44)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(todayText, fontSize = 12.sp, color = AppColors.Gray)
                Text("Chào $userName!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        IconButton(
            onClick = { nav.openTab(Routes.NOTIFICATIONS) },
            modifier = Modifier.clip(CircleShape).background(AppColors.Card),
        ) {
            // Chấm đỏ khi có thông báo mới (FCM)
            BadgedBox(badge = { if (Inbox.messages.isNotEmpty()) Badge() }) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Thông báo")
            }
        }
    }
}

/** Thẻ màu chủ đạo: "3/7 việc" và vòng tròn phần trăm. */
@Composable
private fun ProgressCard(done: Int, total: Int, message: String) {
    val progress = if (total == 0) 0f else done.toFloat() / total
    val percent = (progress * 100).toInt()

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.Primary)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Tiến độ hôm nay", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            Text("$done/$total việc", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(message, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp) // câu này lấy từ Remote Config
        }
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(60.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
                strokeWidth = 5.dp,
            )
            Text("$percent%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

/** Thẻ "Học tập / Cuộc sống — N việc còn lại". */
@Composable
private fun CategoryCard(category: Category, remaining: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.card(14).clickable(onClick = onClick).padding(14.dp)) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(category.bg.tinted(category.text)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(category.icon, contentDescription = null, tint = category.text, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(category.label, fontWeight = FontWeight.SemiBold)
        Text("$remaining việc còn lại", fontSize = 12.sp, color = AppColors.Gray)
    }
}
