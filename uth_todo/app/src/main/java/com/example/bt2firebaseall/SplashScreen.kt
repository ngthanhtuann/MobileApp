package com.example.bt2firebaseall

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Màn Khởi động: logo + tên app, thanh tiến trình chạy hết thì chuyển màn.

private const val SPLASH_DURATION_MS = 1300

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val progress = remember { Animatable(0f) }
    val versionName = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(SPLASH_DURATION_MS))
        onFinished()
    }

    Box(Modifier.fillMaxSize().background(AppColors.Primary)) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(110.dp)
                    .shadow(16.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Checklist, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(60.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(appNameText(), color = Color.White, fontSize = 38.sp)
            Spacer(Modifier.height(6.dp))
            Text("Việc học, việc nhà — gọn gàng mỗi ngày", color = Color.White.copy(alpha = 0.9f))
        }

        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier.width(120.dp).clip(RoundedCornerShape(50)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.35f),
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Spacer(Modifier.height(14.dp))
            Text("Phiên bản $versionName", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        }
    }
}

/** "UTH Todo" với chữ "UTH" in đậm, giống logo trong thiết kế. */
private fun appNameText() = buildAnnotatedString {
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("UTH ") }
    append("Todo")
}
