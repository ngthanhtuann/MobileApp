package com.example.thuchanh1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

val MauXanh = Color(0xFF2196F3)
const val QUOTE = "The only way to do great work is to love what you do."

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = MauXanh)) { App() }
        }
    }
}

// Root -> List -> Detail; Detail có thể Back (về List) hoặc Back to Root
@Composable
fun App() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "root") {
        composable("root") { RootScreen(nav) }
        composable("list") { ListScreen(nav) }
        composable("detail/{id}") { DetailScreen(nav, it.arguments?.getString("id")!!.toInt()) }
    }
}

@Composable
fun RootScreen(nav: NavController) {
    Column(
        Modifier.fillMaxSize().background(Color.White).systemBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Image(painterResource(R.drawable.logo_compose), null, Modifier.size(120.dp))
        Spacer(Modifier.height(24.dp))
        Text("Navigation", fontWeight = FontWeight.Bold)
        Text(
            "is a framework that simplifies the implementation of navigation between different " +
                "UI components (activities, fragments, or composables) in an app",
            fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        Button(onClick = { nav.navigate("list") }, Modifier.fillMaxWidth()) { Text("PUSH") }
    }
}

@Composable
fun ListScreen(nav: NavController) {
    Khung("LazyColumn", nav) {
        // LazyColumn chỉ tạo các item đang hiển thị nên 1.000.000 item vẫn mượt
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(1_000_000) { i ->
                val so = i + 1
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFBBDEFB))
                        .clickable { nav.navigate("detail/$so") }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("%02d | %s".format(so, QUOTE), Modifier.weight(1f), fontSize = 13.sp)
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color.Black),
                    )
                }
            }
        }
    }
}

@Composable
fun DetailScreen(nav: NavController, id: Int) {
    Khung("Detail", nav) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("\"$QUOTE\"", textAlign = TextAlign.Center, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFBBDEFB), MauXanh)))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Item #$id", fontSize = 12.sp)
                Text("\"$QUOTE\"", fontFamily = FontFamily.Monospace, fontSize = 26.sp, textAlign = TextAlign.Center)
                Text("Steve Jobs", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            // Xoá List và Detail khỏi back stack, quay thẳng về Root
            Button(onClick = { nav.popBackStack("root", inclusive = false) }, Modifier.fillMaxWidth()) {
                Text("BACK TO ROOT")
            }
        }
    }
}

// Khung chung: thanh tiêu đề có nút Back
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Khung(tieuDe: String, nav: NavController, noiDung: @Composable () -> Unit) {
    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(tieuDe, color = MauXanh, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MauXanh)
                    }
                },
            )
        },
    ) { Box(Modifier.padding(it)) { noiDung() } }
}
