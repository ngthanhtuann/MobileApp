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

// Trang chu: loi chao, tien do hom nay, thong ke nhom, danh sach viec HOM NAY.

@Composable
fun HomeScreen(nav: NavController) = TabScaffold(nav, Routes.HOME) { padding ->
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val allTasks    = TaskRepository.tasks
    val todayTasks  = allTasks.filter { it.isToday }
    val todayDone   = todayTasks.count { it.done }
    val todayTodo   = todayTasks.filter { !it.done }
    val todayDoneList = todayTasks.filter { it.done }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item { Greeting(nav) }

        // Tien do hom nay: chi tinh task hom nay
        item {
            ProgressCard(
                done    = todayDone,
                total   = todayTasks.size,
                message = RemoteConfigRepo.homeMessage.value,
            )
        }

        // The nhom: dem theo toan bo allTasks (giong ListScreen)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Category.entries.forEach { category ->
                    CategoryCard(
                        category  = category,
                        remaining = allTasks.count { it.cat == category && !it.done },
                        total     = allTasks.count { it.cat == category },
                        modifier  = Modifier.weight(1f),
                        onClick   = { nav.openTab(Routes.LIST) },
                    )
                }
            }
        }

        // Header danh sach hom nay
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Việc hôm nay",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
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
            item { Text("Hôm nay chưa có việc nào \uD83C\uDF89", color = AppColors.Gray) }
        }

        // Can lam hom nay
        items(todayTodo, key = { it.id }) { task ->
            TaskRow(
                task         = task,
                timeLabel    = task.time,
                onOpen       = { nav.navigate(Routes.detail(task.id)) },
                onToggleDone = { scope.call(context) { TaskRepository.toggleDone(task) } },
            )
        }

        // Da xong hom nay (compact, o cuoi)
        if (todayDoneList.isNotEmpty()) {
            item { SectionLabel("ĐÃ XONG HÔM NAY") }
            items(todayDoneList, key = { "done_${it.id}" }) { task ->
                TaskRow(
                    task         = task,
                    timeLabel    = task.time,
                    compact      = true,
                    onOpen       = { nav.navigate(Routes.detail(task.id)) },
                    onToggleDone = { scope.call(context) { TaskRepository.toggleDone(task) } },
                )
            }
        }
    }
}

@Composable
private fun Greeting(nav: NavController) {
    val userName  = AuthRepository.user.displayName()
    val todayText = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE, d 'tháng' M", Vietnamese))
        .replaceFirstChar { it.uppercase() }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
            onClick  = { nav.openTab(Routes.NOTIFICATIONS) },
            modifier = Modifier.clip(CircleShape).background(AppColors.Card),
        ) {
            BadgedBox(badge = { if (Inbox.messages.isNotEmpty()) Badge() }) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Thông báo")
            }
        }
    }
}

@Composable
private fun ProgressCard(done: Int, total: Int, message: String) {
    val progress = if (total == 0) 0f else done.toFloat() / total
    val percent  = (progress * 100).toInt()

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
            Text(message, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        }
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress    = { progress },
                modifier    = Modifier.size(60.dp),
                color       = Color.White,
                trackColor  = Color.White.copy(alpha = 0.3f),
                strokeWidth = 5.dp,
            )
            Text("$percent%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CategoryCard(
    category  : Category,
    remaining : Int,
    total     : Int,
    modifier  : Modifier,
    onClick   : () -> Unit,
) {
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
        Text("$remaining/$total việc còn lại", fontSize = 12.sp, color = AppColors.Gray)
    }
}
