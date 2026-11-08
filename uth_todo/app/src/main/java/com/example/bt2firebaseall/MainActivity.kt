package com.example.bt2firebaseall

import android.content.Context
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
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// ===================== DỮ LIỆU =====================

object AppColors {
    val Primary = Color(0xFF3B5BDB)
    val Green = Color(0xFF2F9E44)
    val Red = Color(0xFFE03131)

    // Cài đặt › Giao diện tối: đổi biến này là toàn app vẽ lại
    var dark by mutableStateOf(false)
    val Bg get() = if (dark) Color(0xFF121317) else Color(0xFFF5F6FA)
    val Card get() = if (dark) Color(0xFF1F2128) else Color.White
    val Text get() = if (dark) Color(0xFFECECF1) else Color(0xFF1C1C1E)
    val Gray get() = if (dark) Color(0xFF9A9BA3) else Color(0xFF6B6B6B)

    fun saveDark(context: Context, value: Boolean) {
        dark = value
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit { putBoolean("dark", value) }
    }
}

enum class Category(val label: String, val text: Color, val bg: Color, val icon: ImageVector) {
    STUDY("Học tập", Color(0xFF3B5BDB), Color(0xFFE3E9FF), Icons.Outlined.School),
    LIFE("Cuộc sống", Color(0xFFD9822B), Color(0xFFFFEBD6), Icons.Outlined.Home);

    val key get() = name.lowercase()

    companion object {
        fun from(key: String) = entries.find { it.name.equals(key, ignoreCase = true) } ?: STUDY
    }
}

enum class Priority(val label: String) {
    LOW("Ưu tiên thấp"), MEDIUM("Ưu tiên vừa"), HIGH("Gấp");

    val key get() = name.lowercase()

    companion object {
        fun from(key: String) = entries.find { it.name.equals(key, ignoreCase = true) } ?: MEDIUM
    }
}

val Task.cat get() = Category.from(category)
val Task.isToday get() = date == LocalDate.now().toString()

/** "Hôm nay, 14:00", "Ngày mai, 21:00", "Tuần này, 20:00"... */
fun Task.whenLabel(): String {
    val d = runCatching { LocalDate.parse(date) }.getOrNull() ?: return time
    val day = when (val diff = ChronoUnit.DAYS.between(LocalDate.now(), d)) {
        0L -> "Hôm nay"
        1L -> "Ngày mai"
        -1L -> "Hôm qua"
        in 2L..6L -> "Tuần này"
        else -> if (diff < 0) "Quá hạn ${d.format(DateTimeFormatter.ofPattern("dd/MM"))}" else d.format(DateTimeFormatter.ofPattern("dd/MM"))
    }
    return "$day, $time"
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
    const val LIST = "list"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val DETAIL = "detail/{id}"
    const val CRYPTO = "crypto"
}

/** Chuyển tab ở thanh dưới: không chồng nhiều bản của cùng một tab. */
fun NavController.openTab(route: String) = navigate(route) {
    popUpTo(Routes.HOME)
    launchSingleTop = true
}

/** Gọi API trong coroutine, lỗi thì hiện Toast. */
fun CoroutineScope.call(context: Context, onDone: () -> Unit = {}, block: suspend () -> Unit) = launch {
    try {
        block()
        onDone()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Toast.makeText(context, e.toMessage(), Toast.LENGTH_LONG).show()
    }
}

// ===================== UI =====================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppColors.dark = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("dark", false)
        Inbox.init(this)
        RemoteConfigRepo.fetch()
        setContent {
            val scheme = if (AppColors.dark) darkColorScheme(primary = AppColors.Primary) else lightColorScheme(primary = AppColors.Primary)
            MaterialTheme(colorScheme = scheme) {
                Surface(Modifier.fillMaxSize(), color = AppColors.Bg, contentColor = AppColors.Text) {
                    Box(Modifier.safeDrawingPadding()) {
                        AppNav()
                        PermissionOnboarding() // Nâng cao 01: hiện 1 lần khi mở app lần đầu
                    }
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
        composable(Routes.HOME) { HomeScreen(nav) }
        composable(Routes.LIST) { ListScreen(nav) }
        composable(Routes.NOTIFICATIONS) { NotificationsScreen(nav) }
        composable(Routes.SETTINGS) { SettingsScreen(nav) }
        composable(Routes.DETAIL) { entry ->
            DetailScreen(entry.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { nav.popBackStack() },
                onLoggedOut = { nav.navigate(Routes.LOGIN) { popUpTo(0) } },
            )
        }
        composable(Routes.CRYPTO) { CryptoScreen(onBack = { nav.popBackStack() }) }
    }
}

@Composable
fun BackBar(title: String, onBack: () -> Unit, backLabel: String = "Quay lại") {
    Box(Modifier.fillMaxWidth()) {
        Row(Modifier.clickable(onClick = onBack).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppColors.Primary, modifier = Modifier.size(18.dp))
            Text(" $backLabel", color = AppColors.Primary)
        }
        Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
    }
}

/** Thẻ nền trắng (nền tối khi bật giao diện tối). */
fun Modifier.card(radius: Int = 12) = this.clip(RoundedCornerShape(radius.dp)).background(AppColors.Card)

// ---------- Khung chung cho 4 tab: thanh điều hướng dưới + nút + ----------

@Composable
fun TabScaffold(nav: NavController, current: String, content: @Composable (PaddingValues) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = AuthRepository.user ?: return
    var showAdd by remember { mutableStateOf(false) }

    // GET /tasks mỗi lần mở màn hình
    LaunchedEffect(user.uid) {
        TaskRepository.start(user.uid)
        Tracker.setUser(context, user.uid)
        TaskRepository.load()
    }
    if (showAdd) TaskDialog(null, onDismiss = { showAdd = false }) { task ->
        showAdd = false
        scope.call(context, onDone = { Toast.makeText(context, "Đã thêm công việc", Toast.LENGTH_SHORT).show() }) {
            TaskRepository.add(task)
            Tracker.log(context, "add_task", "category" to task.category)
        }
    }

    Scaffold(
        containerColor = AppColors.Bg,
        bottomBar = {
            Row(Modifier.fillMaxWidth().background(AppColors.Card).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                TabItem(Icons.Outlined.Home, "Trang chủ", Routes.HOME, current, nav)
                TabItem(Icons.AutoMirrored.Outlined.List, "Danh sách", Routes.LIST, current, nav)
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    FloatingActionButton(
                        onClick = { showAdd = true }, shape = CircleShape, containerColor = AppColors.Primary,
                        modifier = Modifier.size(50.dp),
                    ) { Icon(Icons.Filled.Add, "Thêm việc", tint = Color.White) }
                }
                TabItem(Icons.Outlined.Notifications, "Thông báo", Routes.NOTIFICATIONS, current, nav)
                TabItem(Icons.Outlined.Tune, "Cài đặt", Routes.SETTINGS, current, nav)
            }
        },
    ) { content(it) }
}

@Composable
fun RowScope.TabItem(icon: ImageVector, label: String, route: String, current: String, nav: NavController) {
    val color = if (route == current) AppColors.Primary else AppColors.Gray
    Column(
        Modifier.weight(1f).clickable { if (route != current) nav.openTab(route) }.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = color, modifier = Modifier.size(22.dp))
        Text(label, fontSize = 10.sp, color = color)
    }
}

/** Trạng thái gọi API: đang tải / lỗi + nút Thử lại. */
@Composable
fun LoadState() {
    val scope = rememberCoroutineScope()
    val error = TaskRepository.loadError.value
    if (error != null) {
        Column(Modifier.fillMaxWidth().card().padding(14.dp)) {
            Text("Không tải được công việc", fontWeight = FontWeight.SemiBold, color = AppColors.Red)
            Text(error, fontSize = 12.sp, color = AppColors.Gray)
            TextButton(onClick = { scope.launch { TaskRepository.load() } }) { Text("Thử lại") }
        }
    } else if (TaskRepository.loading.value) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
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
            } catch (e: CancellationException) {
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
        Modifier.fillMaxSize().background(AppColors.Card).verticalScroll(rememberScrollState()).padding(24.dp),
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
            else Text("Đăng nhập bằng Email", color = Color.White)
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
            Text("Tiếp tục với Google", color = AppColors.Text)
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

// ---------- Trang chủ: tiến độ hôm nay + thống kê + việc hôm nay ----------

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
fun HomeScreen(nav: NavController) = TabScaffold(nav, Routes.HOME) { padding ->
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val name = AuthRepository.user.displayName()
    val all = TaskRepository.tasks
    val today = all.filter { it.isToday }
    val done = today.count { it.done }
    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'tháng' M", Locale.forLanguageTag("vi")))
        .replaceFirstChar { it.uppercase() }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Nhấn vào avatar/tên để mở thông tin cá nhân
                Row(Modifier.weight(1f).clickable { nav.navigate(Routes.PROFILE) }, verticalAlignment = Alignment.CenterVertically) {
                    Avatar(name, 44)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(dateText, fontSize = 12.sp, color = AppColors.Gray)
                        Text("Chào $name!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = { nav.openTab(Routes.NOTIFICATIONS) }) {
                    BadgedBox(badge = { if (Inbox.messages.isNotEmpty()) Badge() }) {
                        Icon(Icons.Outlined.Notifications, "Thông báo")
                    }
                }
            }
        }
        item { ProgressCard(done, today.size, RemoteConfigRepo.homeMessage.value) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Category.entries.forEach { c ->
                    val left = all.count { it.cat == c && !it.done }
                    Column(Modifier.weight(1f).card(14).clickable { nav.openTab(Routes.LIST) }.padding(14.dp)) {
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
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Việc hôm nay", fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Xem tất cả", color = AppColors.Primary, fontSize = 13.sp, modifier = Modifier.clickable { nav.openTab(Routes.LIST) })
            }
        }
        item { LoadState() }
        if (today.isEmpty() && !TaskRepository.loading.value) item { Text("Hôm nay chưa có việc nào 🎉", color = AppColors.Gray) }
        items(today, key = { it.id }) { t ->
            TaskRow(
                t, label = t.time,
                onOpen = { nav.navigate("detail/${t.id}") },
                onToggle = { scope.call(context) { TaskRepository.update(t.id, mapOf("done" to !t.done)) } },
            )
        }
    }
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
fun Chip(text: String, color: Color, bg: Color) {
    Text(
        text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(bg).padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** Một dòng công việc: bấm vòng tròn = tick xong, bấm dòng = mở chi tiết. */
@Composable
fun TaskRow(t: Task, label: String, onOpen: () -> Unit, onToggle: () -> Unit, compact: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().card().clickable(onClick = onOpen).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (t.done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            if (t.done) "Bỏ đánh dấu" else "Đánh dấu xong",
            tint = if (t.done) AppColors.Primary else Color.LightGray,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onToggle),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                t.title, fontWeight = FontWeight.Medium,
                color = if (t.done) AppColors.Gray else AppColors.Text,
                textDecoration = if (t.done) TextDecoration.LineThrough else null,
            )
            if (!compact) Row(verticalAlignment = Alignment.CenterVertically) {
                Chip(t.cat.label, t.cat.text, t.cat.bg)
                Spacer(Modifier.width(8.dp))
                Text(label, fontSize = 12.sp, color = AppColors.Gray)
            }
        }
        if (Priority.from(t.priority) == Priority.HIGH && !t.done) Chip("Gấp", AppColors.Red, Color(0xFFFFE3E3))
        else if (!compact) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.LightGray)
    }
}

// ---------- Danh sách: tìm kiếm + lọc + nhóm Cần làm / Đã xong ----------

@Composable
fun ListScreen(nav: NavController) = TabScaffold(nav, Routes.LIST) { padding ->
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<Category?>(null) }
    val shown = TaskRepository.tasks.filter { (filter == null || it.cat == filter) && it.title.contains(query.trim(), ignoreCase = true) }
    val (done, todo) = shown.partition { it.done }

    @Composable
    fun ListRow(t: Task) = TaskRow(
        t, label = t.whenLabel(), compact = t.done,
        onOpen = { nav.navigate("detail/${t.id}") },
        onToggle = { scope.call(context) { TaskRepository.update(t.id, mapOf("done" to !t.done)) } },
    )

    LazyColumn(
        Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Danh sách", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${TaskRepository.tasks.count { !it.done }} việc chưa xong", fontSize = 12.sp, color = AppColors.Gray)
            }
        }
        item {
            OutlinedTextField(
                query, { query = it }, placeholder = { Text("Tìm công việc") }, singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(filter == null, { filter = null }, label = { Text("Tất cả") })
                Category.entries.forEach { c -> FilterChip(filter == c, { filter = c }, label = { Text(c.label) }) }
            }
        }
        item { LoadState() }
        if (todo.isNotEmpty()) item { SectionLabel("CẦN LÀM") }
        items(todo, key = { it.id }) { ListRow(it) }
        if (done.isNotEmpty()) item { SectionLabel("ĐÃ XONG") }
        items(done, key = { it.id }) { ListRow(it) }
        if (shown.isEmpty() && !TaskRepository.loading.value) item { Text("Không có công việc phù hợp", color = AppColors.Gray) }
    }
}

@Composable
fun SectionLabel(text: String) =
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.Gray, modifier = Modifier.padding(top = 6.dp))

// ---------- Chi tiết: GET /tasks/{id}, PATCH, DELETE ----------

@Composable
fun DetailScreen(id: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var task by remember { mutableStateOf<Task?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editSub by remember { mutableStateOf<Int?>(null) }
    var newSub by remember { mutableStateOf("") }

    // GET /tasks/{id}: lúc mở màn hình và sau mỗi lần sửa
    LaunchedEffect(reload) {
        error = null
        try {
            task = TaskRepository.get(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.toMessage()
        }
    }

    /** PATCH /tasks/{id} rồi tải lại. */
    fun patch(vararg fields: Pair<String, Any>) = scope.call(context, onDone = { reload++ }) { TaskRepository.update(id, mapOf(*fields)) }

    val t = task
    if (editing && t != null) TaskDialog(t, onDismiss = { editing = false }) { edited ->
        editing = false
        patch(
            "title" to edited.title, "note" to edited.note, "category" to edited.category,
            "priority" to edited.priority, "date" to edited.date, "time" to edited.time,
        )
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Xóa công việc?") },
        text = { Text("\"${t?.title}\" sẽ bị xóa vĩnh viễn.") },
        confirmButton = {
            TextButton(onClick = {
                confirmDelete = false
                scope.call(context, onDone = {
                    Toast.makeText(context, "Đã xóa công việc", Toast.LENGTH_SHORT).show()
                    onBack()
                }) { TaskRepository.delete(id) }
            }) { Text("Xóa", color = AppColors.Red) }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Hủy") } },
    )
    editSub?.let { i ->
        val subs = t?.subtasks.orEmpty()
        TextInputDialog("Sửa việc nhỏ", subs.getOrNull(i)?.title.orEmpty(), onDismiss = { editSub = null }) { text ->
            editSub = null
            patch("subtasks" to subs.mapIndexed { j, s -> if (j == i) s.copy(title = text) else s })
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f)) { BackBar("", onBack, backLabel = "Trang chủ") }
            if (t != null) Text("Sửa", color = AppColors.Primary, modifier = Modifier.clickable { editing = true }.padding(4.dp))
        }
        Spacer(Modifier.height(12.dp))
        when {
            t == null && error != null -> Column(Modifier.fillMaxWidth().card().padding(14.dp)) {
                Text("Không tải được công việc", fontWeight = FontWeight.SemiBold, color = AppColors.Red)
                Text(error.orEmpty(), fontSize = 12.sp, color = AppColors.Gray)
                TextButton(onClick = { reload++ }) { Text("Thử lại") }
            }

            t == null -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

            else -> {
                val doneSubs = t.subtasks.count { it.done }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip(t.cat.label, t.cat.text, t.cat.bg)
                    val p = Priority.from(t.priority)
                    if (p == Priority.HIGH) Chip(p.label, AppColors.Red, Color(0xFFFFE3E3)) else Chip(p.label, Color(0xFF6B6B6B), Color(0xFFEDEDED))
                    if (t.done) Chip("Đã xong", AppColors.Green, Color(0xFFE6F6EA))
                }
                Spacer(Modifier.height(6.dp))
                Text(t.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
                Spacer(Modifier.height(14.dp))
                Column(Modifier.fillMaxWidth().card()) {
                    InfoRow(Icons.Outlined.CalendarMonth, "Hạn chót", t.whenLabel())
                    HorizontalDivider(color = AppColors.Bg)
                    InfoRow(Icons.Outlined.Notifications, "Nhắc nhở", if (t.remind) "Trước 15 phút" else "Tắt") {
                        patch("remind" to !t.remind)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Ghi chú", fontSize = 12.sp, color = AppColors.Gray)
                Spacer(Modifier.height(6.dp))
                Text(
                    t.note.ifBlank { "Chưa có ghi chú" }, color = if (t.note.isBlank()) AppColors.Gray else AppColors.Text,
                    modifier = Modifier.fillMaxWidth().card().padding(14.dp),
                )
                Spacer(Modifier.height(14.dp))
                Row {
                    Text("Việc nhỏ", fontSize = 12.sp, color = AppColors.Gray, modifier = Modifier.weight(1f))
                    Text("$doneSubs/${t.subtasks.size}", fontSize = 12.sp, color = AppColors.Gray)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (t.subtasks.isEmpty()) 0f else doneSubs.toFloat() / t.subtasks.size },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Column(Modifier.fillMaxWidth().card()) {
                    t.subtasks.forEachIndexed { i, s ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(s.done, { checked ->
                                patch("subtasks" to t.subtasks.mapIndexed { j, x -> if (j == i) x.copy(done = checked) else x })
                            })
                            Text(
                                s.title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = if (s.done) AppColors.Gray else AppColors.Text,
                                textDecoration = if (s.done) TextDecoration.LineThrough else null,
                            )
                            IconButton(onClick = { editSub = i }) { Icon(Icons.Outlined.Edit, "Sửa", tint = AppColors.Gray, modifier = Modifier.size(18.dp)) }
                            IconButton(onClick = { patch("subtasks" to t.subtasks.filterIndexed { j, _ -> j != i }) }) {
                                Icon(Icons.Outlined.Delete, "Xóa", tint = AppColors.Red, modifier = Modifier.size(18.dp))
                            }
                        }
                        HorizontalDivider(color = AppColors.Bg)
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, null, tint = AppColors.Primary, modifier = Modifier.padding(8.dp))
                        TextField(
                            newSub, { newSub = it }, placeholder = { Text("Thêm việc nhỏ") }, singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                            ),
                        )
                        TextButton(onClick = {
                            patch("subtasks" to t.subtasks + Subtask(newSub.trim()))
                            newSub = ""
                        }, enabled = newSub.isNotBlank()) { Text("Thêm") }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { patch("done" to !t.done) },
                    modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                ) { Text(if (t.done) "Đánh dấu chưa xong" else "Đánh dấu hoàn thành", color = Color.White) }
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Xóa công việc", color = AppColors.Red)
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = AppColors.Gray, modifier = Modifier.size(18.dp))
        Text("  $label", color = AppColors.Gray, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TextInputDialog(title: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(text, { text = it }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onSave(text.trim()) }, enabled = text.isNotBlank()) { Text("Lưu") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

/** Thêm mới (initial = null) hoặc sửa công việc. */
@Composable
fun TaskDialog(initial: Task?, onDismiss: () -> Unit, onSave: (Task) -> Unit) {
    val today = LocalDate.now().toString()
    val tomorrow = LocalDate.now().plusDays(1).toString()
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var category by remember { mutableStateOf(initial?.cat ?: Category.STUDY) }
    var priority by remember { mutableStateOf(Priority.from(initial?.priority ?: "medium")) }
    var date by remember { mutableStateOf(initial?.date ?: today) }
    var time by remember { mutableStateOf(initial?.time ?: "08:00") }
    val timeOk = Regex("([01]?\\d|2[0-3]):[0-5]\\d").matches(time.trim())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Thêm việc mới" else "Sửa công việc") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Tên công việc") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Ghi chú") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Category.entries.forEach { c -> FilterChip(category == c, { category = c }, label = { Text(c.label) }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Priority.entries.forEach { p -> FilterChip(priority == p, { priority = p }, label = { Text(p.label.removePrefix("Ưu tiên ")) }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(date == today, { date = today }, label = { Text("Hôm nay") })
                    FilterChip(date == tomorrow, { date = tomorrow }, label = { Text("Ngày mai") })
                }
                OutlinedTextField(
                    time, { time = it }, label = { Text("Giờ (HH:mm)") }, singleLine = true, isError = !timeOk,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val base = initial ?: Task()
                    onSave(
                        base.copy(
                            title = title.trim(), note = note.trim(), category = category.key,
                            priority = priority.key, date = date, time = time.trim().padStart(5, '0'),
                        )
                    )
                },
                enabled = title.isNotBlank() && timeOk,
            ) { Text(if (initial == null) "Thêm" else "Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
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
            } catch (e: CancellationException) {
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
    Column(modifier.card().padding(12.dp)) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 11.sp, color = AppColors.Gray)
    }
}

// ---------- Thông báo (Cloud Messaging) ----------

@Composable
fun NotificationsScreen(nav: NavController) = TabScaffold(nav, Routes.NOTIFICATIONS) { padding ->
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Thông báo", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Column(Modifier.fillMaxWidth().card().padding(14.dp)) {
            Text("FCM token của máy này", fontWeight = FontWeight.SemiBold)
            Text("Dùng token này (hoặc chủ đề \"$TOPIC\") để gửi thử từ Firebase Console › Messaging.", fontSize = 12.sp, color = AppColors.Gray)
            Spacer(Modifier.height(6.dp))
            SelectionContainer { Text(Inbox.token.value, fontSize = 11.sp) }
        }
        Text("Đã nhận (khi app đang mở)", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        if (Inbox.messages.isEmpty()) Text("Chưa có thông báo nào", color = AppColors.Gray)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Inbox.messages) { Text(it, Modifier.fillMaxWidth().card(10).padding(12.dp)) }
        }
    }
}

// ---------- Cài đặt ----------

@Composable
fun SettingsScreen(nav: NavController) = TabScaffold(nav, Routes.SETTINGS) { padding ->
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Cài đặt", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        SettingRow(Icons.Outlined.Person, "Hồ sơ cá nhân", AuthRepository.user.displayName()) { nav.navigate(Routes.PROFILE) }
        Row(Modifier.fillMaxWidth().card().padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.DarkMode, null, tint = AppColors.Primary)
            Text("  Giao diện tối", Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Switch(AppColors.dark, { AppColors.saveDark(context, it) })
        }
        SettingRow(Icons.Outlined.CurrencyBitcoin, "Giá crypto realtime", "Retrofit + Coroutines (Nâng cao 02)") { nav.navigate(Routes.CRYPTO) }
        SettingRow(Icons.Outlined.Refresh, "Đặt lại dữ liệu mẫu", "Ghi đè danh sách việc bằng dữ liệu mẫu") {
            scope.call(context, onDone = { Toast.makeText(context, "Đã đặt lại dữ liệu mẫu", Toast.LENGTH_SHORT).show() }) {
                TaskRepository.resetSamples()
            }
        }
    }
}

@Composable
fun SettingRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().card().clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = AppColors.Primary)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = AppColors.Gray)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.LightGray)
    }
}
