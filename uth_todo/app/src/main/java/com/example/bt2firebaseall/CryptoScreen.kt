package com.example.bt2firebaseall

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.util.Locale

// Nâng cao 02: màn Home giá crypto realtime (Retrofit + Coroutines), dữ liệu từ CoinGecko.

private val CryptoBackground = Color(0xFF1B1D2B)
private val CryptoCard = Color(0xFF2A2D3E)
private val PriceUp = Color(0xFF4CD07D)
private val PriceDown = Color(0xFFFF5C5C)
private val CoinIds = listOf("ethereum", "bitcoin", "litecoin", "monero") // thứ tự hiển thị
private val FavoriteSymbols = setOf("eth", "btc")
private const val REFRESH_INTERVAL_MS = 30_000L

/** 1245.3 -> "$1,245.30" */
private fun formatMoney(value: Double): String = String.format(Locale.US, "$%,.2f", value)

@Composable
fun CryptoScreen(onBack: () -> Unit) {
    var coins by remember { mutableStateOf<List<Coin>>(emptyList()) }
    var trendingCoin by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshCount by remember { mutableIntStateOf(0) } // bấm nút làm mới => tăng lên => chạy lại vòng lặp

    // Gọi API ngay khi mở, sau đó cứ 30 giây gọi lại (realtime).
    // 2 API được gọi song song bằng async (concurrency), rồi chờ cả hai xong bằng await.
    LaunchedEffect(refreshCount) {
        while (true) {
            try {
                coroutineScope {
                    val marketsRequest = async { Api.crypto.markets(CoinIds.joinToString(",")) }
                    val trendingRequest = async { Api.crypto.trending() }
                    coins = marketsRequest.await().sortedBy { CoinIds.indexOf(it.id) }
                    val hot = trendingRequest.await().coins.firstOrNull()?.item
                    trendingCoin = hot?.let { "${it.name} (${it.symbol.uppercase()})" }
                }
                errorMessage = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage = e.toMessage()
            }
            delay(REFRESH_INTERVAL_MS)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(CryptoBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
            }
            Text(
                "Home",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center),
            )
            IconButton(onClick = { refreshCount++ }, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Filled.Refresh, contentDescription = "Làm mới", tint = Color.White)
            }
        }
        errorMessage?.let { Text("Lỗi: $it", color = PriceDown, fontSize = 12.sp) }

        SectionTitle("New")
        BreakingNewsBanner(trendingCoin)

        SectionTitle("Favorites")
        if (coins.isEmpty() && errorMessage == null) {
            CircularProgressIndicator(color = Color.White)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            coins.filter { it.symbol in FavoriteSymbols }.forEach { coin ->
                FavoriteCoinCard(coin, Modifier.weight(1f))
            }
        }

        SectionTitle("All Fluctuations")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            coins.forEach { coin -> CoinRow(coin) }
        }
        Text(
            "Nguồn: CoinGecko API • tự cập nhật mỗi 30 giây",
            color = Color(0xFF6E7085),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

/** Banner "LIVE / BREAKING NEWS" với tin: đồng coin đang được tìm nhiều nhất. */
@Composable
private fun BreakingNewsBanner(trendingCoin: String?) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0D2A6B), Color(0xFF3A5FCD), Color(0xFF0D2A6B)))),
    ) {
        Text(
            "LIVE",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(PriceDown)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Column(Modifier.align(Alignment.Center).fillMaxWidth()) {
            Text(
                "BREAKING NEWS",
                color = Color.Black,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().background(Color(0xFFFFD43B)).padding(vertical = 6.dp),
            )
            Text(
                if (trendingCoin != null) "🔥 Đang được tìm nhiều nhất: $trendingCoin" else "Đang tải tin...",
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

/** Thẻ coin yêu thích: ký hiệu, giá, % thay đổi, biểu đồ 7 ngày. */
@Composable
private fun FavoriteCoinCard(coin: Coin, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(CryptoCard).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoinIcon(coin.symbol)
            Text("  ${coin.symbol.uppercase()}", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        Text(formatMoney(coin.price), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        PriceChangeText(coin.changePct)
        Spacer(Modifier.height(8.dp))
        Sparkline(coin.sparkline?.price.orEmpty(), Modifier.fillMaxWidth().height(50.dp))
    }
}

/** Một dòng trong "All Fluctuations": biểu tượng, ký hiệu + tên, giá + % thay đổi. */
@Composable
private fun CoinRow(coin: Coin) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CryptoCard).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinIcon(coin.symbol)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(coin.symbol.uppercase(), color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(coin.name, color = Color(0xFF9A9BB0), fontSize = 13.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatMoney(coin.price), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            PriceChangeText(coin.changePct)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
    )
}

/** "+1.25%" màu xanh khi tăng, "-0.30%" màu đỏ khi giảm. */
@Composable
private fun PriceChangeText(percent: Double?) {
    val value = percent ?: 0.0
    Text(String.format(Locale.US, "%+.2f%%", value), color = if (value >= 0) PriceUp else PriceDown, fontSize = 12.sp)
}

/** Vòng tròn có chữ cái đầu của ký hiệu coin. */
@Composable
private fun CoinIcon(symbol: String) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF3A3E55)), contentAlignment = Alignment.Center) {
        Text(symbol.take(1).uppercase(), color = Color(0xFF7B9CFF), fontWeight = FontWeight.Bold)
    }
}

/** Biểu đồ đường giá 7 ngày, vẽ bằng Canvas. Giá cuối >= giá đầu thì màu xanh lá. */
@Composable
private fun Sparkline(prices: List<Double>, modifier: Modifier) {
    val isUp = (prices.lastOrNull() ?: 0.0) >= (prices.firstOrNull() ?: 0.0)
    val lineColor = if (isUp) PriceUp else Color(0xFF7B9CFF)

    Canvas(modifier) {
        if (prices.size < 2) return@Canvas
        val minPrice = prices.min()
        val priceRange = (prices.max() - minPrice).takeIf { it > 0 } ?: 1.0

        val path = Path()
        prices.forEachIndexed { index, price ->
            val x = size.width * index / (prices.size - 1)
            val y = size.height * (1 - ((price - minPrice) / priceRange)).toFloat() // giá cao -> gần đỉnh
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(width = 2.dp.toPx()))
    }
}
