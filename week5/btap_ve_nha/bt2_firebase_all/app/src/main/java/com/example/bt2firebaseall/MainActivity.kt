package com.example.bt2firebaseall

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
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

data class Task(val id: String, val title: String, val category: Category, val time: String, val done: Boolean)

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
    const val NOTIFICATIONS = "notifications"
}

// ===================== UI =====================

class MainActivity : ComponentActivity() {
    private val askNotification = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Inbox.init(this)
        RemoteConfigRepo.fetch()
        if (Build.VERSION.SDK_INT >= 33) askNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
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
        composable(Routes.HOME) {
            HomeScreen(onProfile = { nav.navigate(Routes.PROFILE) }, onNotifications = { nav.navigate(Routes.NOTIFICATIONS) })
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { nav.popBackStack() },
                onLoggedOut = { nav.navigate(Routes.LOGIN) { popUpTo(0) } },
            )
        }
        composable(Routes.NOTIFICATIONS) { NotificationsScreen(onBack = { nav.popBackStack() }) }
    }
}

@Composable
fun BackBar(title: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Row(Modifier.clickable(onClick = onBack).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppColors.Primary, modifier = Modifier.size(18.dp))
            Text(" Trang chủ", color = AppColors.Primary)
        }
        Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
    }
}

// ---------- Màn hình đăng nhập (Authentication + Analytics) ----------

@Composable
fun LoginScreen(onSuccess: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current!!
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun login(method: String, action: suspend () -> Unit) {
        if (loading) return
        loading = true
        scope.launch {
            try {
                action()
                Tracker.log(context, FirebaseAnalytics.Event.LOGIN, FirebaseAnalytics.Param.METHOD to method)
                Toast.makeText(context, "Đăng nhập thành công! Xin chào ${AuthRepository.user.displayName()}", Toast.LENGTH_SHORT).show()
                onSuccess()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Tracker.log(context, "login_error", FirebaseAnalytics.Param.METHOD to method)
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
            onClick = { login("email") { AuthRepository.signInEmail(email, password) } },
            enabled = !loading, modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Đăng nhập bằng Email")
        }
        TextButton(
            onClick = { login("email_signup") { AuthRepository.signUpEmail(email, password) } },
            enabled = !loading, modifier = Modifier.align(Alignment.CenterHorizontally),
        ) { Text("Chưa có tài khoản? Đăng ký bằng email này") }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { login("google") { AuthRepository.signInGoogle(activity) } },
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
            onClick = { login("anonymous") { AuthRepository.signInGuest() } },
            enabled = !loading, modifier = Modifier.align(Alignment.CenterHorizontally),
        ) { Text("Dùng thử không cần tài khoản", color = AppColors.Primary, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(24.dp))
        Text(
            "Khi tiếp tục, bạn đồng ý với Điều khoản sử dụng và Chính sách quyền riêng tư của UTH Todo.",
            fontSize = 11.sp, color = AppColors.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---------- Trang chủ (Realtime Database + Remote Config) ----------

@Composable
fun Avatar(name: String, size: Int, photo: ImageBitmap? = null, onClick: (() -> Unit)? = null) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(Color(0xFFE3E9FF))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) Image(photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(initials(name), color = AppColors.Primary, fontWeight = FontWeight.Bold, fontSize = (size / 2.6).sp)
    }
}

@Composable
fun HomeScreen(onProfile: () -> Unit, onNotifications: () -> Unit) {
    val context = LocalContext.current
    val user = AuthRepository.user ?: return
    val name = user.displayName()
    val tasks = TaskRepository.tasks
    val done = tasks.count { it.done }
    var showAdd by remember { mutableStateOf(false) }
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'tháng' M", Locale.forLanguageTag("vi")))
        .replaceFirstChar { it.uppercase() }

    LaunchedEffect(user.uid) {
        TaskRepository.listen(user.uid)
        Tracker.setUser(context, user.uid)
    }
    if (showAdd) AddTaskDialog(onDismiss = { showAdd = false }) { title, c, time ->
        TaskRepository.add(title, c, time)
        Tracker.log(context, "add_task", "category" to c.name)
        showAdd = false
    }

    Scaffold(
        containerColor = AppColors.Bg,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = AppColors.Primary, shape = CircleShape) {
                Icon(Icons.Filled.Add, "Thêm việc", tint = Color.White)
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // Nhấn vào avatar/tên để mở thông tin cá nhân
                    Row(Modifier.weight(1f).clickable(onClick = onProfile), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(name, 44)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(today, fontSize = 12.sp, color = AppColors.Gray)
                            Text("Chào $name!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(onClick = onNotifications) {
                        BadgedBox(badge = { if (Inbox.messages.isNotEmpty()) Badge() }) {
                            Icon(Icons.Outlined.Notifications, "Thông báo")
                        }
                    }
                }
            }
            item { ProgressCard(done, tasks.size, RemoteConfigRepo.homeMessage.value) }
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
            items(tasks, key = { it.id }) { t -> TaskRow(t) { TaskRepository.toggle(t) } }
            item { Spacer(Modifier.height(64.dp)) }
        }
    }
}

@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String, Category, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("08:00") }
    var category by remember { mutableStateOf(Category.STUDY) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm việc mới") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Tên công việc") }, singleLine = true)
                OutlinedTextField(time, { time = it }, label = { Text("Giờ (HH:mm)") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Category.entries.forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(title.trim(), category, time.trim()) }, enabled = title.isNotBlank()) { Text("Thêm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

@Composable
fun ProgressCard(done: Int, total: Int, message: String) {
    val pct = if (total == 0) 0f else done.toFloat() / total
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AppColors.Primary).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Tiến độ hôm nay", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            Text("$done/$total việc", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(message, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp) // lấy từ Remote Config
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

// ---------- Hồ sơ (Storage + Crashlytics) ----------

@Composable
fun ProfileScreen(onBack: () -> Unit, onLoggedOut: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = AuthRepository.user ?: return
    val name = user.displayName()
    val tasks = TaskRepository.tasks
    val done = tasks.count { it.done }
    val pct = if (tasks.isEmpty()) 0 else done * 100 / tasks.size
    val isGoogle = user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
    var photo by remember { mutableStateOf<ImageBitmap?>(null) }
    var uploading by remember { mutableStateOf(false) }

    LaunchedEffect(user.uid) { photo = AvatarRepository.load(user.uid) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploading = true
        scope.launch {
            try {
                AvatarRepository.upload(user.uid, uri)
                photo = AvatarRepository.load(user.uid)
                Toast.makeText(context, "Đã tải ảnh lên Firebase Storage", Toast.LENGTH_SHORT).show()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(context, "Tải ảnh thất bại: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                uploading = false
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BackBar("Hồ sơ", onBack)
        Spacer(Modifier.height(24.dp))
        Box(contentAlignment = Alignment.Center) {
            Avatar(name, 80, photo) { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            if (uploading) CircularProgressIndicator(Modifier.size(86.dp))
        }
        Text("Nhấn vào ảnh để đổi avatar", fontSize = 10.sp, color = AppColors.Gray)
        Spacer(Modifier.height(8.dp))
        Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        user.email?.let { Text(it, color = AppColors.Gray, fontSize = 13.sp) }
        Spacer(Modifier.height(6.dp))
        Text(
            when {
                isGoogle -> "✓ Đã liên kết tài khoản Google"
                user.isAnonymous -> "Tài khoản khách (ẩn danh)"
                else -> "✓ Đăng nhập bằng Email"
            },
            color = AppColors.Green, fontSize = 12.sp,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFE6F6EA)).padding(horizontal = 8.dp, vertical = 3.dp),
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("$done", "Việc đã xong", Modifier.weight(1f))
            StatCard("${tasks.size - done}", "Đang chờ", Modifier.weight(1f))
            StatCard("$pct%", "Hoàn thành", Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { throw RuntimeException("Test crash từ UTH Todo (Crashlytics)") }) {
            Icon(Icons.Filled.BugReport, null, tint = AppColors.Gray, modifier = Modifier.size(16.dp))
            Text("  Gửi crash thử (Crashlytics)", color = AppColors.Gray, fontSize = 12.sp)
        }
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

// ---------- Thông báo (Cloud Messaging) ----------

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BackBar("Thông báo", onBack)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(14.dp)) {
            Text("FCM token của máy này", fontWeight = FontWeight.SemiBold)
            Text("Dùng token này (hoặc chủ đề \"$TOPIC\") để gửi thử từ Firebase Console › Messaging.", fontSize = 12.sp, color = AppColors.Gray)
            Spacer(Modifier.height(6.dp))
            SelectionContainer { Text(Inbox.token.value, fontSize = 11.sp) }
        }
        Text("Đã nhận (khi app đang mở)", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        if (Inbox.messages.isEmpty()) Text("Chưa có thông báo nào", color = AppColors.Gray)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Inbox.messages) {
                Text(it, Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White).padding(12.dp))
            }
        }
    }
}
