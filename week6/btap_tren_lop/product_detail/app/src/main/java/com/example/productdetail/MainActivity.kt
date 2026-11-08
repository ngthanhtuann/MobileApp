package com.example.productdetail

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.IOException
import java.net.URL
import java.util.Locale

// ===================== API =====================
// GET https://mock.apidog.com/m1/890655-872447-default/v2/product
// {"name": "...", "id": "...", "des": "...", "price": 649.79, "imgURL": "https://..."}

data class Product(
    val id: String,
    val name: String,
    val des: String,
    val price: Double,
    val imgURL: String,
)

interface ProductApi {
    @GET("v2/product")
    suspend fun getProduct(): Product
}

object Api {
    val product: ProductApi = Retrofit.Builder()
        .baseUrl("https://mock.apidog.com/m1/890655-872447-default/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ProductApi::class.java)
}

/** Tải ảnh từ URL trên luồng IO (không cần thư viện ảnh). */
suspend fun loadImage(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching { URL(url).openStream().use { BitmapFactory.decodeStream(it) }?.asImageBitmap() }.getOrNull()
}

/** 3 trạng thái của một lần gọi API. */
sealed interface UiState {
    data object Loading : UiState
    data class Success(val product: Product, val image: ImageBitmap?) : UiState
    data class Error(val message: String) : UiState
}

// ===================== UI =====================

private val Blue = Color(0xFF2196F3)
private val Red = Color(0xFFD32F2F)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = Color.White) {
                    Box(Modifier.safeDrawingPadding()) { ProductDetailScreen(onBack = { finish() }) }
                }
            }
        }
    }
}

@Composable
fun ProductDetailScreen(onBack: () -> Unit) {
    var state by remember { mutableStateOf<UiState>(UiState.Loading) }
    var reload by remember { mutableIntStateOf(0) }

    // Gọi API mỗi khi mở màn hình hoặc bấm "Tải lại"
    LaunchedEffect(reload) {
        state = UiState.Loading
        state = try {
            val product = Api.product.getProduct()
            UiState.Success(product, loadImage(product.imgURL))
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            UiState.Error("Không có kết nối mạng")
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Lỗi không xác định")
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Box(Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart).size(40.dp).clip(RoundedCornerShape(12.dp)).background(Blue),
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBackIos, "Quay lại", tint = Color.White, modifier = Modifier.padding(start = 6.dp)) }
            Text("Product detail", color = Blue, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            IconButton(onClick = { reload++ }, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Filled.Refresh, "Tải lại", tint = Blue)
            }
        }
        Spacer(Modifier.height(20.dp))

        when (val s = state) {
            UiState.Loading -> Box(Modifier.fillMaxWidth().padding(top = 120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue)
            }

            is UiState.Error -> Column(Modifier.fillMaxWidth().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Không tải được sản phẩm", fontWeight = FontWeight.Bold, color = Red)
                Text(s.message, color = Color.Gray, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { reload++ }, colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("Thử lại") }
            }

            is UiState.Success -> ProductContent(s.product, s.image)
        }
    }
}

@Composable
fun ProductContent(p: Product, image: ImageBitmap?) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)).background(Color(0xFFF1F1F1)),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) Image(image, p.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text("Không tải được ảnh", color = Color.Gray)
    }
    Spacer(Modifier.height(20.dp))
    Text(p.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    Text("Giá: ${String.format(Locale.US, "$%,.2f", p.price)}", color = Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(16.dp))
    Text(
        p.des, fontSize = 14.sp, color = Color(0xFF444444), lineHeight = 20.sp,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF4F4F4)).padding(16.dp),
    )
}
