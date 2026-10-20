package com.example.thuchanh2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlin.random.Random

val MauXanh = Color(0xFF2196F3)
const val SO_LUONG = 1_000_000

val QUOTES = listOf(
    "The only way to do great work is to love what you do.",
    "Stay hungry, stay foolish.",
    "Simplicity is the ultimate sophistication.",
    "Talk is cheap. Show me the code.",
    "First, solve the problem. Then, write the code.",
    "Code is like humor. When you have to explain it, it's bad.",
)

// Item "ngẫu nhiên" nhưng cố định theo số thứ tự, tính khi cần nên không tốn bộ nhớ cho 1 triệu item
fun quoteCua(so: Int) = QUOTES[Random(so).nextInt(QUOTES.size)]

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = MauXanh)) { App() }
        }
    }
}

@Composable
fun App() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "choose") {
        composable("choose") { ChooseScreen(nav) }
        composable("column") { ColumnScreen(nav) }
        composable("lazy") { LazyScreen(nav) }
        composable("detail/{so}") { DetailScreen(nav, it.arguments?.getString("so")!!.toInt()) }
    }
}

@Composable
fun ChooseScreen(nav: NavController) {
    Khung("Choose List Type", null) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
            Button(
                onClick = { nav.navigate("column") }, Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(Color(0xFF4CAF50)), shape = MaterialTheme.shapes.small,
            ) { Text("Column (1,000,000 items)") }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { nav.navigate("lazy") }, Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(Color(0xFFFF9800)), shape = MaterialTheme.shapes.small,
            ) { Text("LazyColumn (1,000,000 items)") }
        }
    }
}

// Column thường dựng cả 1.000.000 item cùng lúc -> app sẽ đơ/treo. Đây là điểm cần so sánh với LazyColumn.
@Composable
fun ColumnScreen(nav: NavController) {
    Khung("Column", nav) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
            for (so in 1..SO_LUONG) The(so, nav)
        }
    }
}

// LazyColumn chỉ dựng các item đang thấy trên màn hình -> cuộn mượt
@Composable
fun LazyScreen(nav: NavController) {
    Khung("LazyColumn", nav) {
        LazyColumn(contentPadding = PaddingValues(12.dp)) {
            items(SO_LUONG) { The(it + 1, nav) }
        }
    }
}

@Composable
fun The(so: Int, nav: NavController) {
    Card(
        onClick = { nav.navigate("detail/$so") },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(Color(0xFFE3F2FD)),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("$so", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(quoteCua(so), color = Color.Gray, fontSize = 12.sp)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
        }
    }
}

@Composable
fun DetailScreen(nav: NavController, so: Int) {
    Khung("Detail", nav) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Item #$so", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(12.dp))
            Text("\"${quoteCua(so)}\"", fontSize = 22.sp)
        }
    }
}

// Thanh tiêu đề xanh; nav = null thì không có nút Back (màn đầu tiên)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Khung(tieuDe: String, nav: NavController?, noiDung: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tieuDe, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (nav != null) IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MauXanh, titleContentColor = Color.White, navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { Box(Modifier.padding(it)) { noiDung() } }
}
