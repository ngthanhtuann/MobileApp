package com.example.bt1firebaseauth

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ===================== DỮ LIỆU =====================

object AppColors {
    val Primary = Color(0xFF3B5BDB)
    val Bg = Color(0xFFF5F6FA)
    val Gray = Color(0xFF6B6B6B)
    val Green = Color(0xFF2F9E44)
    val Red = Color(0xFFE03131)
}

enum class Category(val label: String, val text: Color, val bg: Color, val icon: ImageVector) {
    STUDY("Học tập", Color(0xFF3B5BDB), Color(0xFFE3E9FF), Icons.Outlined.School),
    LIFE("Cuộc sống", Color(0xFFD9822B), Color(0xFFFFEBD6), Icons.Outlined.Home),
}

data class Task(val title: String, val category: Category, val time: String, val done: Boolean)

object TaskRepository {
    val tasks = mutableStateListOf(
        Task("Tập thể dục 30 phút", Category.LIFE, "06:30", true),
        Task("Nộp bài tập Lập trình di động", Category.STUDY, "09:00", true),
        Task("Ôn chương 3 – Cấu trúc dữ liệu", Category.STUDY, "14:00", false),
        Task("Đi chợ mua đồ cho tuần", Category.LIFE, "17:30", false),
    )

    fun toggle(i: Int) {
        tasks[i] = tasks[i].copy(done = !tasks[i].done)
    }
}

/** Mọi thao tác với Firebase Authentication. */
object AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    val user: FirebaseUser? get() = auth.currentUser

    suspend fun signInEmail(email: String, password: String) {
        require(email.isNotBlank() && password.isNotBlank()) { "Vui lòng nhập email và mật khẩu" }
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun signUpEmail(email: String, password: String) {
        require(email.isNotBlank() && password.isNotBlank()) { "Vui lòng nhập email và mật khẩu" }
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun signInGuest() {
        auth.signInAnonymously().await()
    }

    suspend fun signInGoogle(activity: Activity) {
        // default_web_client_id do plugin google-services sinh ra từ google-services.json
        val resId = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
        require(resId != 0) {
            "Chưa bật Google Sign-In trong Firebase Console. Bật xong hãy tải lại google-services.json."
        }
        val option = GetSignInWithGoogleOption.Builder(activity.getString(resId)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(activity).getCredential(activity, request).credential
        require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Không nhận được tài khoản Google"
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }

    suspend fun signOut(context: Context) {
        auth.signOut()
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }
}

/** Đổi lỗi Firebase sang câu thông báo tiếng Việt dễ hiểu. */
fun Throwable.toMessage(): String = when (this) {
    is FirebaseAuthWeakPasswordException -> "Mật khẩu quá yếu (cần ít nhất 6 ký tự)"
    is FirebaseAuthInvalidCredentialsException -> "Email không hợp lệ hoặc mật khẩu không đúng"
    is FirebaseAuthInvalidUserException -> "Tài khoản không tồn tại hoặc đã bị khóa"
    is FirebaseAuthUserCollisionException -> "Email này đã được đăng ký"
    is FirebaseNetworkException -> "Không có kết nối mạng"
    is GetCredentialCancellationException -> "Bạn đã hủy đăng nhập Google"
    is NoCredentialException -> "Máy chưa có tài khoản Google. Vào Cài đặt › Mật khẩu & tài khoản › Thêm tài khoản › Google rồi thử lại."
    else -> message ?: "Lỗi không xác định"
}

fun FirebaseUser?.displayName(): String = when {
    this == null -> ""
    isAnonymous -> "Khách"
    !displayName.isNullOrBlank() -> displayName!!
    else -> email?.substringBefore('@') ?: "Bạn"
}

fun initials(name: String): String {
    val words = name.split(" ").filter { it.isNotBlank() }
    return (listOfNotNull(words.firstOrNull(), words.lastOrNull().takeIf { words.size > 1 }))
        .joinToString("") { it.first().uppercase() }
}

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val PROFILE = "profile"
}

// ===================== UI =====================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = AppColors.Bg) {
                    Box(Modifier.safeDrawingPadding()) { AppNav() }
                }
            }
        }
    }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val start = if (AuthRepository.user != null) Routes.HOME else Routes.LOGIN
    NavHost(nav, startDestination = start) {
        composable(Routes.LOGIN) {
            LoginScreen(onSuccess = {
                nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
            })
        }
        composable(Routes.HOME) { HomeScreen(onProfile = { nav.navigate(Routes.PROFILE) }) }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { nav.popBackStack() },
                onLoggedOut = { nav.navigate(Routes.LOGIN) { popUpTo(0) } },
            )
        }
    }
}

// ---------- Màn hình đăng nhập ----------

@Composable
fun LoginScreen(onSuccess: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current!!
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun login(action: suspend () -> Unit) {
        if (loading) return
        loading = true
        scope.launch {
            try {
                action()
                Toast.makeText(context, "Đăng nhập thành công! Xin chào ${AuthRepository.user.displayName()}", Toast.LENGTH_SHORT).show()
                onSuccess()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.toMessage()
            } finally {
                loading = false
            }
        }
    }

    error?.let {
        AlertDialog(
            onDismissRequest = { error = null },
            confirmButton = { TextButton(onClick = { error = null }) { Text("Đã hiểu") } },
            title = { Text("Đăng nhập thất bại") },
            text = { Text(it) },
        )
    }

    Column(
        Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState()).padding(24.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(AppColors.Primary),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Checklist, null, tint = Color.White, modifier = Modifier.size(36.dp)) }
        Spacer(Modifier.height(16.dp))
        Text("Chào mừng đến với\nUTH Todo", fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
        Spacer(Modifier.height(8.dp))
        Text("Đăng nhập để lưu công việc và đồng bộ trên mọi thiết bị của bạn.", color = AppColors.Gray)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            email, { email = it }, label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            password, { password = it }, label = { Text("Mật khẩu") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { login { AuthRepository.signInEmail(email, password) } },
            enabled = !loading, modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Đăng nhập bằng Email")
        }
        TextButton(
            onClick = { login { AuthRepository.signUpEmail(email, password) } },
            enabled = !loading, modifier = Modifier.align(Alignment.CenterHorizontally),
        ) { Text("Chưa có tài khoản? Đăng ký bằng email này") }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { login { AuthRepository.signInGoogle(activity) } },
            enabled = !loading, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(10.dp),
        ) {
            Text("G", fontWeight = FontWeight.Bold, color = AppColors.Red)
            Spacer(Modifier.width(10.dp))
            Text("Tiếp tục với Google", color = Color.Black)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f))
            Text("hoặc", color = AppColors.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
            HorizontalDivider(Modifier.weight(1f))
        }
        TextButton(
            onClick = { login { AuthRepository.signInGuest() } },
            enabled = !loading, modifier = Modifier.align(Alignment.CenterHorizontally),
        ) { Text("Dùng thử không cần tài khoản", color = AppColors.Primary, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(24.dp))
        Text(
            "Khi tiếp tục, bạn đồng ý với Điều khoản sử dụng và Chính sách quyền riêng tư của UTH Todo.",
            fontSize = 11.sp, color = AppColors.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---------- Trang chủ ----------

@Composable
fun Avatar(name: String, size: Int, onClick: (() -> Unit)? = null) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(Color(0xFFE3E9FF))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) { Text(initials(name), color = AppColors.Primary, fontWeight = FontWeight.Bold, fontSize = (size / 2.6).sp) }
}

@Composable
fun HomeScreen(onProfile: () -> Unit) {
    val name = AuthRepository.user.displayName()
    val tasks = TaskRepository.tasks
    val done = tasks.count { it.done }
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'tháng' M", Locale.forLanguageTag("vi")))
        .replaceFirstChar { it.uppercase() }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            // Nhấn vào avatar/tên để mở thông tin cá nhân
            Row(Modifier.fillMaxWidth().clickable(onClick = onProfile), verticalAlignment = Alignment.CenterVertically) {
                Avatar(name, 44)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(today, fontSize = 12.sp, color = AppColors.Gray)
                    Text("Chào $name!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { ProgressCard(done, tasks.size) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Category.entries.forEach { c ->
                    val left = tasks.count { it.category == c && !it.done }
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color.White).padding(14.dp)) {
                        Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(c.bg), contentAlignment = Alignment.Center) {
                            Icon(c.icon, null, tint = c.text, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(c.label, fontWeight = FontWeight.SemiBold)
                        Text("$left việc còn lại", fontSize = 12.sp, color = AppColors.Gray)
                    }
                }
            }
        }
        item { Text("Việc hôm nay", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
        itemsIndexed(tasks) { i, t -> TaskRow(t) { TaskRepository.toggle(i) } }
    }
}

@Composable
fun ProgressCard(done: Int, total: Int) {
    val pct = if (total == 0) 0f else done.toFloat() / total
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AppColors.Primary).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Tiến độ hôm nay", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            Text("$done/$total việc", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                when {
                    total > 0 && done == total -> "Hoàn thành hết rồi, tuyệt vời!"
                    pct >= 0.5f -> "Đã hơn nửa chặng, cố lên!"
                    else -> "Bắt đầu thôi nào!"
                },
                color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp,
            )
        }
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { pct }, modifier = Modifier.size(60.dp), color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f), strokeWidth = 5.dp,
            )
            Text("${(pct * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun TaskRow(t: Task, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).clickable(onClick = onToggle).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (t.done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null,
            tint = if (t.done) AppColors.Primary else Color.LightGray,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                t.title, fontWeight = FontWeight.Medium,
                color = if (t.done) AppColors.Gray else Color.Black,
                textDecoration = if (t.done) TextDecoration.LineThrough else null,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    t.category.label, color = t.category.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(t.category.bg).padding(horizontal = 6.dp, vertical = 1.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(t.time, fontSize = 12.sp, color = AppColors.Gray)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.LightGray)
    }
}

// ---------- Hồ sơ cá nhân ----------

@Composable
fun ProfileScreen(onBack: () -> Unit, onLoggedOut: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = AuthRepository.user
    val name = user.displayName()
    val tasks = TaskRepository.tasks
    val done = tasks.count { it.done }
    val pct = if (tasks.isEmpty()) 0 else done * 100 / tasks.size
    val isGoogle = user?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth()) {
            Row(Modifier.clickable(onClick = onBack).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppColors.Primary, modifier = Modifier.size(18.dp))
                Text(" Trang chủ", color = AppColors.Primary)
            }
            Text("Hồ sơ", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
        }
        Spacer(Modifier.height(24.dp))
        Avatar(name, 80)
        Spacer(Modifier.height(12.dp))
        Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        user?.email?.let { Text(it, color = AppColors.Gray, fontSize = 13.sp) }
        Spacer(Modifier.height(6.dp))
        Text(
            when {
                isGoogle -> "✓ Đã liên kết tài khoản Google"
                user?.isAnonymous == true -> "Tài khoản khách (ẩn danh)"
                else -> "✓ Đăng nhập bằng Email"
            },
            color = AppColors.Green, fontSize = 12.sp,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFE6F6EA)).padding(horizontal = 8.dp, vertical = 3.dp),
        )
        Text("UID: ${user?.uid}", color = AppColors.Gray, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("$done", "Việc đã xong", Modifier.weight(1f))
            StatCard("${tasks.size - done}", "Đang chờ", Modifier.weight(1f))
            StatCard("$pct%", "Hoàn thành", Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(
            onClick = {
                scope.launch {
                    AuthRepository.signOut(context)
                    Toast.makeText(context, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
                    onLoggedOut()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, null, tint = AppColors.Red, modifier = Modifier.size(18.dp))
            Text("  Đăng xuất", color = AppColors.Red)
        }
    }
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(Color.White).padding(12.dp)) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 11.sp, color = AppColors.Gray)
    }
}
