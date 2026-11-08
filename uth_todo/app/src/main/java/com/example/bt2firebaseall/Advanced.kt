package com.example.bt2firebaseall

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.util.Locale

// ===================== NÂNG CAO 01: màn xin quyền =====================

private val Orange = Color(0xFFE8673C)

class PermStep(val icon: ImageVector, val title: String, val desc: String, val button: String, val permissions: Array<String>)

/** Lần đầu mở app: lần lượt hỏi Vị trí → Thông báo → Camera. Mỗi bước có "Bật" hoặc "Để sau". */
@Composable
fun PermissionOnboarding() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val steps = remember {
        listOfNotNull(
            PermStep(
                Icons.Filled.LocationOn, "Vị trí", "Cho phép UTH Todo truy cập vị trí của bạn khi đang dùng ứng dụng?", "Cho phép",
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            ),
            if (Build.VERSION.SDK_INT >= 33) PermStep(
                Icons.Filled.NotificationsActive, "Thông báo", "Bật thông báo để nhận cập nhật và lời nhắc công việc.", "Bật",
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            ) else null,
            PermStep(
                Icons.Filled.PhotoCamera, "Camera", "Chúng tôi cần quyền camera để quét mã QR.", "Bật",
                arrayOf(Manifest.permission.CAMERA),
            ),
        ).filter { step -> step.permissions.any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED } }
    }
    var index by rememberSaveable { mutableIntStateOf(if (prefs.getBoolean("permissions_asked", false)) Int.MAX_VALUE else 0) }

    fun next() {
        index++
        if (index >= steps.size) prefs.edit { putBoolean("permissions_asked", true) }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { next() }
    val step = steps.getOrNull(index) ?: return

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}, // chặn bấm xuyên xuống dưới
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth(0.85f).clip(RoundedCornerShape(24.dp)).background(Color.White).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(120.dp).clip(CircleShape).background(Color(0xFFE8F5EE)), contentAlignment = Alignment.Center) {
                Icon(step.icon, null, tint = Orange, modifier = Modifier.size(64.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(step.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1E))
            Spacer(Modifier.height(10.dp))
            Text(step.desc, textAlign = TextAlign.Center, color = Color(0xFF555555))
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { launcher.launch(step.permissions) },
                modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Orange),
            ) { Text(step.button, color = Color.White) }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { next() },
                modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Orange),
            ) { Text("Để sau", color = Orange) }
            Text("${index + 1}/${steps.size}", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

// ===================== NÂNG CAO 02: giá crypto realtime (Retrofit + Coroutines) =====================

private val CryptoBg = Color(0xFF1B1D2B)
private val CryptoCard = Color(0xFF2A2D3E)
private val UpGreen = Color(0xFF4CD07D)
private val DownRed = Color(0xFFFF5C5C)
private const val COIN_IDS = "ethereum,bitcoin,litecoin,monero"

fun money(v: Double): String = String.format(Locale.US, "$%,.2f", v)

@Composable
fun CryptoScreen(onBack: () -> Unit) {
    var coins by remember { mutableStateOf<List<Coin>>(emptyList()) }
    var hot by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    // Tự cập nhật mỗi 30 giây. Gọi song song 2 API bằng async (concurrency).
    LaunchedEffect(refresh) {
        while (true) {
            try {
                coroutineScope {
                    val markets = async { Api.crypto.markets(COIN_IDS) }
                    val trending = async { Api.crypto.trending() }
                    coins = markets.await().sortedBy { COIN_IDS.split(',').indexOf(it.id) }
                    hot = trending.await().coins.firstOrNull()?.item?.let { "${it.name} (${it.symbol.uppercase()})" }
                }
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.toMessage()
            }
            delay(30_000)
        }
    }

    Column(Modifier.fillMaxSize().background(CryptoBg).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Box(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = Color.White)
            }
            Text("Home", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Center))
            IconButton(onClick = { refresh++ }, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Filled.Refresh, "Làm mới", tint = Color.White)
            }
        }
        error?.let { Text("Lỗi: $it", color = DownRed, fontSize = 12.sp) }

        CryptoTitle("New")
        Box(
            Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF0D2A6B), Color(0xFF3A5FCD), Color(0xFF0D2A6B)))),
        ) {
            Text(
                "LIVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).background(DownRed).padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Column(Modifier.align(Alignment.Center).fillMaxWidth()) {
                Text(
                    "BREAKING NEWS", color = Color.Black, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().background(Color(0xFFFFD43B)).padding(vertical = 6.dp),
                )
                Text(
                    hot?.let { "🔥 Đang được tìm nhiều nhất: $it" } ?: "Đang tải tin...",
                    color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }

        CryptoTitle("Favorites")
        if (coins.isEmpty() && error == null) CircularProgressIndicator(color = Color.White)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            coins.filter { it.symbol in setOf("eth", "btc") }.forEach { c ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(CryptoCard).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CoinIcon(c.symbol)
                        Text("  ${c.symbol.uppercase()}", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(money(c.price), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    ChangeText(c.changePct)
                    Spacer(Modifier.height(8.dp))
                    Sparkline(c.sparkline?.price.orEmpty(), Modifier.fillMaxWidth().height(50.dp))
                }
            }
        }

        CryptoTitle("All Fluctuations")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            coins.forEach { c ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CryptoCard).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoinIcon(c.symbol)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(c.symbol.uppercase(), color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(c.name, color = Color(0xFF9A9BB0), fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(money(c.price), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        ChangeText(c.changePct)
                    }
                }
            }
        }
        Text(
            "Nguồn: CoinGecko API • tự cập nhật mỗi 30 giây", color = Color(0xFF6E7085), fontSize = 11.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun CryptoTitle(text: String) =
    Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))

@Composable
private fun ChangeText(pct: Double?) {
    val v = pct ?: 0.0
    Text(String.format(Locale.US, "%+.2f%%", v), color = if (v >= 0) UpGreen else DownRed, fontSize = 12.sp)
}

@Composable
private fun CoinIcon(symbol: String) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF3A3E55)), contentAlignment = Alignment.Center) {
        Text(symbol.take(1).uppercase(), color = Color(0xFF7B9CFF), fontWeight = FontWeight.Bold)
    }
}

/** Biểu đồ giá 7 ngày, vẽ bằng Canvas. */
@Composable
fun Sparkline(points: List<Double>, modifier: Modifier) {
    val color = if ((points.lastOrNull() ?: 0.0) >= (points.firstOrNull() ?: 0.0)) UpGreen else Color(0xFF7B9CFF)
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val min = points.min()
        val range = (points.max() - min).takeIf { it > 0 } ?: 1.0
        val path = Path()
        points.forEachIndexed { i, p ->
            val x = size.width * i / (points.size - 1)
            val y = size.height * (1 - ((p - min) / range)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
    }
}
