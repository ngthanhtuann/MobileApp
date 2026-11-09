package com.example.bt2firebaseall

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit

// Nâng cao 01: lần đầu mở app, lần lượt xin quyền Vị trí → Thông báo → Camera.
// Mỗi bước là một hộp màu cam có 2 nút: "Bật/Cho phép" (hỏi quyền thật của Android) và "Để sau".

private val Orange = Color(0xFFE8673C)
private const val PREF_ASKED = "permissions_asked"

/** Một bước xin quyền. */
private class PermissionStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val allowButton: String,
    val permissions: Array<String>,
)

/** Các bước cần hỏi. Quyền Thông báo chỉ có từ Android 13 (API 33). */
private fun allSteps(): List<PermissionStep> = listOfNotNull(
    PermissionStep(
        icon = Icons.Filled.LocationOn,
        title = "Vị trí",
        description = "Cho phép UTH Todo truy cập vị trí của bạn khi đang dùng ứng dụng?",
        allowButton = "Cho phép",
        permissions = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
    ),
    if (Build.VERSION.SDK_INT >= 33) {
        PermissionStep(
            icon = Icons.Filled.NotificationsActive,
            title = "Thông báo",
            description = "Bật thông báo để nhận cập nhật và lời nhắc công việc.",
            allowButton = "Bật",
            permissions = arrayOf(Manifest.permission.POST_NOTIFICATIONS),
        )
    } else {
        null
    },
    PermissionStep(
        icon = Icons.Filled.PhotoCamera,
        title = "Camera",
        description = "Chúng tôi cần quyền camera để quét mã QR.",
        allowButton = "Bật",
        permissions = arrayOf(Manifest.permission.CAMERA),
    ),
)

private fun isGranted(context: Context, step: PermissionStep): Boolean =
    step.permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

@Composable
fun PermissionOnboarding() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    // Chỉ hỏi những quyền chưa được cấp
    val steps = remember { allSteps().filterNot { isGranted(context, it) } }
    val alreadyAsked = prefs.getBoolean(PREF_ASKED, false)
    var stepIndex by rememberSaveable { mutableIntStateOf(if (alreadyAsked) steps.size else 0) }

    fun goToNextStep() {
        stepIndex++
        if (stepIndex >= steps.size) prefs.edit { putBoolean(PREF_ASKED, true) } // hỏi xong: lần sau không hiện nữa
    }

    // Dù người dùng cho phép hay từ chối trong hộp thoại của Android, đều chuyển sang bước sau
    val requestPermissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        goToNextStep()
    }

    val step = steps.getOrNull(stepIndex) ?: return

    // Lớp nền mờ phủ cả màn hình, chặn bấm xuyên xuống màn bên dưới
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        PermissionCard(
            step = step,
            stepText = "${stepIndex + 1}/${steps.size}",
            onAllow = { requestPermissions.launch(step.permissions) },
            onSkip = ::goToNextStep,
        )
    }
}

@Composable
private fun PermissionCard(step: PermissionStep, stepText: String, onAllow: () -> Unit, onSkip: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth(0.85f)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(120.dp).clip(CircleShape).background(Color(0xFFE8F5EE)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(step.icon, contentDescription = null, tint = Orange, modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(step.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1E))
        Spacer(Modifier.height(10.dp))
        Text(step.description, textAlign = TextAlign.Center, color = Color(0xFF555555))
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onAllow,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Orange),
        ) { Text(step.allowButton, color = Color.White) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, Orange),
        ) { Text("Để sau", color = Orange) }
        Text(stepText, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 10.dp))
    }
}
