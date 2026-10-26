package com.example.bt1uthtodo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

// ===================== DỮ LIỆU (OOP) =====================

object AppInfo {
    const val NAME_PREFIX = "UTH"
    const val NAME_SUFFIX = "Todo"
    const val SLOGAN = "Việc học, việc nhà — gọn gàng mỗi ngày"
    const val VERSION = "Phiên bản 2.1.0"
    const val SPLASH_MS = 2000
}

object AppColors {
    val Primary = Color(0xFF3B5BDB)
    val Soft = Color(0xFFEEF1FB)
    val Gray = Color(0xFF6B6B6B)
}

enum class Category(val label: String, val text: Color, val bg: Color) {
    STUDY("Học tập", Color(0xFF3B5BDB), Color(0xFFE3E9FF)),
    LIFE("Cuộc sống", Color(0xFFD9822B), Color(0xFFFFEBD6)),
}

/** Hình minh họa của từng bước onboarding. */
sealed class Illustration {
    object Checklist : Illustration()
    class Categories(val items: List<Category>) : Illustration()
    class Reminder(val message: String) : Illustration()
}

class OnboardingPage(
    val title: String,
    val description: String,
    val illustration: Illustration,
    val buttonText: String,
)

object OnboardingRepository {
    val pages = listOf(
        OnboardingPage(
            "Mọi việc, gọn trong một chỗ",
            "Ghi lại bài tập, việc nhà, lịch hẹn. Không còn quên việc nào.",
            Illustration.Checklist, "Tiếp tục",
        ),
        OnboardingPage(
            "Chia theo Học tập và Cuộc sống",
            "Mỗi việc có nhóm riêng, nhìn là biết hôm nay cần lo gì.",
            Illustration.Categories(Category.entries), "Tiếp tục",
        ),
        OnboardingPage(
            "Nhắc đúng lúc",
            "Bật thông báo để UTH Todo nhắc bạn trước hạn chót.",
            Illustration.Reminder("Ôn bài Toán lúc 19:00"), "Bắt đầu ngay",
        ),
    )
}

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding/{step}"
    fun onboarding(step: Int) = "onboarding/$step"
}

// ===================== UI =====================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AppNav() } }
    }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen {
                nav.navigate(Routes.onboarding(0)) { popUpTo(Routes.SPLASH) { inclusive = true } }
            }
        }
        composable(Routes.ONBOARDING, arguments = listOf(navArgument("step") { type = NavType.IntType })) {
            val step = it.arguments?.getInt("step") ?: 0
            val pages = OnboardingRepository.pages
            OnboardingScreen(
                page = pages[step], step = step, total = pages.size,
                onSkip = { if (step < pages.lastIndex) nav.navigate(Routes.onboarding(pages.lastIndex)) },
                // shortcut: nút cuối chưa làm gì, nối sang màn Home khi làm các màn sau
                onNext = { if (step < pages.lastIndex) nav.navigate(Routes.onboarding(step + 1)) },
            )
        }
    }
}

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(AppInfo.SPLASH_MS, easing = LinearEasing))
        onFinish()
    }
    Box(Modifier.fillMaxSize().background(AppColors.Primary).padding(24.dp)) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(96.dp).clip(RoundedCornerShape(20.dp)).background(Color.White),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Checklist, null, tint = AppColors.Primary, modifier = Modifier.size(56.dp)) }
            Spacer(Modifier.height(16.dp))
            Text("${AppInfo.NAME_PREFIX} ${AppInfo.NAME_SUFFIX}", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(AppInfo.SLOGAN, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        }
        Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            LinearProgressIndicator(
                progress = { progress.value }, modifier = Modifier.width(100.dp),
                color = Color.White, trackColor = Color.White.copy(alpha = 0.3f),
            )
            Spacer(Modifier.height(8.dp))
            Text(AppInfo.VERSION, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
        }
    }
}

@Composable
fun AppBrand() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(AppColors.Primary),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Checklist, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(8.dp))
        Text(buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(AppInfo.NAME_PREFIX + " ") }
            withStyle(SpanStyle(color = AppColors.Primary)) { append(AppInfo.NAME_SUFFIX) }
        }, fontSize = 18.sp)
    }
}

@Composable
fun OnboardingScreen(page: OnboardingPage, step: Int, total: Int, onSkip: () -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.White).padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AppBrand()
            Spacer(Modifier.weight(1f))
            Text("Bỏ qua", color = AppColors.Gray, modifier = Modifier.clickable(onClick = onSkip).padding(8.dp))
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            IllustrationCard(page.illustration)
        }
        Text(page.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(page.description, color = AppColors.Gray)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { i ->
                Box(
                    Modifier.height(6.dp).width(if (i == step) 22.dp else 6.dp).clip(CircleShape)
                        .background(if (i == step) AppColors.Primary else Color.LightGray)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onNext, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
        ) { Text(page.buttonText, fontSize = 16.sp) }
    }
}

@Composable
fun IllustrationCard(illustration: Illustration) {
    Column(
        Modifier.size(220.dp).clip(RoundedCornerShape(24.dp)).background(AppColors.Soft).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (illustration) {
            is Illustration.Checklist -> listOf(true, false, false).forEach { checked ->
                WhiteRow {
                    Icon(
                        if (checked) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null,
                        tint = if (checked) AppColors.Primary else Color.LightGray, modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    GrayLine()
                }
            }
            is Illustration.Categories -> illustration.items.forEach { c ->
                WhiteRow(vertical = true) {
                    Text(
                        c.label, color = c.text, fontSize = 11.sp,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(c.bg).padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    GrayLine()
                }
            }
            is Illustration.Reminder -> {
                Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Notifications, null, tint = AppColors.Primary, modifier = Modifier.size(32.dp))
                }
                Text(
                    illustration.message, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White).padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun WhiteRow(vertical: Boolean = false, content: @Composable () -> Unit) {
    val m = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White).padding(12.dp)
    if (vertical) Column(m) { content() } else Row(m, verticalAlignment = Alignment.CenterVertically) { content() }
}

@Composable
private fun GrayLine() = Box(Modifier.fillMaxWidth(0.8f).height(5.dp).clip(CircleShape).background(Color(0xFFDDDDDD)))
