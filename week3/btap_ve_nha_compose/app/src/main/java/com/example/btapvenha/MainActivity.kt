package com.example.btapvenha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = MauXanh)) {
                App()
            }
        }
    }
}

// Navigation: mỗi màn hình là 1 route (tên chuỗi)
@Composable
fun App() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "intro") {
        composable("intro") { IntroScreen(nav) }
        composable("components") { ComponentsScreen(nav) }
        // Mỗi thành phần trong danh sách có 1 màn hình chi tiết, route = tên thành phần
        for (muc in danhSachNhom.flatMap { it.second }) {
            composable(muc.ten) { muc.manHinh(nav) }
        }
        composable("trang_con") {
            KhungChiTiet("Trang con", nav) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Bấm nút ← trên thanh tiêu đề để quay lại")
                }
            }
        }
    }
}
