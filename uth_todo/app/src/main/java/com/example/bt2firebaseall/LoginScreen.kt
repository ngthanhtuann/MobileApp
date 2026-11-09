package com.example.bt2firebaseall

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// Màn Đăng nhập: Email/Mật khẩu, Google, hoặc Dùng thử (Firebase Authentication).
// Thành công -> Toast + vào Trang chủ. Thất bại -> hộp thoại báo lỗi. Mỗi lần đều ghi sự kiện Analytics.

@Composable
fun LoginScreen(onBack: () -> Unit, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current!! // Google Sign-In cần Activity để hiện bảng chọn tài khoản
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    /** Chạy một cách đăng nhập; `method` là tên ghi vào Analytics ("email", "google"...). */
    fun login(method: String, signIn: suspend () -> Unit) {
        if (isLoading) return
        isLoading = true
        scope.launch {
            try {
                signIn()
                Tracker.log(context, FirebaseAnalytics.Event.LOGIN, FirebaseAnalytics.Param.METHOD to method)
                val name = AuthRepository.user.displayName()
                Toast.makeText(context, "Đăng nhập thành công! Xin chào $name", Toast.LENGTH_SHORT).show()
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Tracker.log(context, "login_error", FirebaseAnalytics.Param.METHOD to method)
                errorMessage = e.toMessage()
            } finally {
                isLoading = false
            }
        }
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Đăng nhập thất bại") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("Đã hiểu") } },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.Card)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Quay lại", modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(16.dp))
        AppLogo(size = 72)
        Spacer(Modifier.height(20.dp))
        Text("Chào mừng đến với\nUTH Todo", fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp)
        Spacer(Modifier.height(8.dp))
        Text("Đăng nhập để lưu công việc và đồng bộ trên mọi thiết bị của bạn.", color = AppColors.Gray)
        Spacer(Modifier.height(16.dp))
        FeatureLine(Icons.Outlined.Sync, "Tự động sao lưu, không lo mất dữ liệu")
        FeatureLine(Icons.Outlined.PhoneAndroid, "Dùng trên điện thoại và máy tính bảng")
        Spacer(Modifier.height(24.dp))

        // Email + mật khẩu (bài Tuần 5 – BT01)
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { login("email") { AuthRepository.signInEmail(email, password) } },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
        ) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Đăng nhập bằng Email", color = Color.White)
            }
        }
        TextButton(
            onClick = { login("email_signup") { AuthRepository.signUpEmail(email, password) } },
            enabled = !isLoading,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) { Text("Chưa có tài khoản? Đăng ký bằng email này") }

        // Google
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { login("google") { AuthRepository.signInGoogle(activity) } },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text("G", fontWeight = FontWeight.Bold, color = AppColors.Red)
            Spacer(Modifier.width(10.dp))
            Text("Tiếp tục với Google", color = AppColors.Text)
        }

        // Dùng thử (đăng nhập ẩn danh)
        OrDivider()
        TextButton(
            onClick = { login("anonymous") { AuthRepository.signInGuest() } },
            enabled = !isLoading,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) { Text("Dùng thử không cần tài khoản", color = AppColors.Primary, fontWeight = FontWeight.SemiBold) }

        Spacer(Modifier.height(24.dp))
        Text(
            "Khi tiếp tục, bạn đồng ý với Điều khoản sử dụng và Chính sách quyền riêng tư của UTH Todo.",
            fontSize = 11.sp,
            color = AppColors.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Một dòng giới thiệu: biểu tượng + chữ. */
@Composable
private fun FeatureLine(icon: ImageVector, text: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(18.dp))
        Text("  $text", fontSize = 13.sp)
    }
}

/** Đường kẻ ngang có chữ "hoặc" ở giữa. */
@Composable
private fun OrDivider() {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f))
        Text("hoặc", color = AppColors.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
        HorizontalDivider(Modifier.weight(1f))
    }
}
