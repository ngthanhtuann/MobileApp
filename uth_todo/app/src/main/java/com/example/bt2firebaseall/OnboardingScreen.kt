package com.example.bt2firebaseall

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit

// Onboarding 3 bước, chỉ hiện ở lần mở app đầu tiên.

/** Ghi nhớ người dùng đã xem Onboarding chưa. */
object Intro {
    private const val KEY = "onboarded"

    private fun prefs(context: Context) = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun isDone(context: Context): Boolean = prefs(context).getBoolean(KEY, false)

    fun markDone(context: Context) = prefs(context).edit { putBoolean(KEY, true) }
}

private class IntroPage(val title: String, val description: String, val illustration: @Composable () -> Unit)

private val pages = listOf(
    IntroPage("Mọi việc, gọn trong một chỗ", "Ghi lại bài tập, việc nhà, lịch hẹn. Không còn quên việc nào.") { ChecklistIllustration() },
    IntroPage("Chia theo Học tập và Cuộc sống", "Mỗi việc có nhóm riêng, nhìn là biết hôm nay cần lo gì.") { CategoriesIllustration() },
    IntroPage("Nhắc đúng lúc", "Bật thông báo để UTH Todo nhắc bạn trước hạn chót.") { ReminderIllustration() },
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    var pageIndex by rememberSaveable { mutableIntStateOf(0) }
    val page = pages[pageIndex]
    val isLastPage = pageIndex == pages.lastIndex

    Column(Modifier.fillMaxSize().background(AppColors.Card).padding(24.dp)) {
        // Logo + "Bỏ qua"
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppLogo(size = 32)
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("  UTH ") }
                    withStyle(SpanStyle(color = AppColors.Primary)) { append("Todo") }
                },
                fontSize = 20.sp,
                modifier = Modifier.weight(1f),
            )
            Text("Bỏ qua", color = AppColors.Gray, modifier = Modifier.clickable(onClick = onFinished).padding(6.dp))
        }

        // Hình minh họa
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(230.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(AppColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) { page.illustration() }
        }

        Text(page.title, fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp)
        Spacer(Modifier.height(10.dp))
        Text(page.description, color = AppColors.Gray, fontSize = 16.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(28.dp))
        PageDots(count = pages.size, current = pageIndex)
        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { if (isLastPage) onFinished() else pageIndex++ },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
        ) {
            Text(
                if (isLastPage) "Bắt đầu ngay" else "Tiếp tục",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }
    }
}

/** Dấu chấm chỉ trang: trang hiện tại là một vạch dài. */
@Composable
private fun PageDots(count: Int, current: Int) {
    Row {
        repeat(count) { index ->
            val isCurrent = index == current
            Box(
                Modifier
                    .padding(end = 6.dp)
                    .height(6.dp)
                    .width(if (isCurrent) 24.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (isCurrent) AppColors.Primary else AppColors.Gray.copy(alpha = 0.35f)),
            )
        }
    }
}

// ---------- Hình minh họa của 3 trang ----------

@Composable
private fun ChecklistIllustration() {
    val barWidths = listOf(100, 80, 110)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        barWidths.forEachIndexed { index, barWidth ->
            val isChecked = index == 0
            WhiteCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isChecked) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isChecked) AppColors.Primary else Color.LightGray,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    GrayBar(barWidth)
                }
            }
        }
    }
}

@Composable
private fun CategoriesIllustration() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        WhiteCard {
            Chip(Category.STUDY.label, Category.STUDY.text, Category.STUDY.bg)
            Spacer(Modifier.height(10.dp))
            GrayBar(130)
        }
        WhiteCard {
            Chip(Category.LIFE.label, Category.LIFE.text, Category.LIFE.bg)
            Spacer(Modifier.height(10.dp))
            GrayBar(100)
        }
    }
}

@Composable
private fun ReminderIllustration() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(80.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Ôn bài Toán lúc 19:00",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1C1C1E),
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

/** Thanh xám tượng trưng cho một dòng chữ. */
@Composable
private fun GrayBar(width: Int) {
    Box(Modifier.width(width.dp).height(8.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.25f)))
}

@Composable
private fun WhiteCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .width(180.dp)
            .shadow(4.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(12.dp),
        content = content,
    )
}
