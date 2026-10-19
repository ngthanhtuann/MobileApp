package com.example.btapvenha

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage

val MauXanh = Color(0xFF2196F3)
val XanhNhat = Color(0xFF9DB9F5)
val XanhDam = Color(0xFF4F86EC)
val NenXam = Color(0xFFF2F4F7)
const val LOGO_URL = "https://giaothongvantaitphcm.edu.vn/wp-content/uploads/2025/01/Logo-GTVT.png"

// Khung chung cho các màn hình chi tiết: tiêu đề xanh + nút back
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhungChiTiet(tieuDe: String, nav: NavController, content: @Composable BoxScope.() -> Unit) {
    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(tieuDe, color = MauXanh) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = MauXanh)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) { content() }
    }
}

// Text Detail: 1 câu với nhiều kiểu chữ khác nhau
@Composable
fun TextDetailScreen(nav: NavController) {
    KhungChiTiet("Text Detail", nav) {
        Text(
            buildAnnotatedString {
                append("The ")
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append("quick") }
                withStyle(SpanStyle(fontSize = 36.sp, color = Color(0xFFB8860B))) { append(" Brown\n") }
                append("fox ")
                withStyle(SpanStyle(letterSpacing = 8.sp)) { append("jumps") }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) { append(" over\n") }
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append("the") }
                withStyle(SpanStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic)) { append(" lazy") }
                append(" dog.")
            },
            fontSize = 28.sp,
            lineHeight = 44.sp,
            modifier = Modifier.align(Alignment.Center).padding(24.dp),
        )
    }
}

// Images: 1 ảnh từ mạng (link) + 1 ảnh trong app (res/drawable)
@Composable
fun ImageDetailScreen(nav: NavController) {
    KhungChiTiet("Images", nav) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
            AsyncImage(
                model = LOGO_URL,
                contentDescription = "Logo UTH từ link",
                error = painterResource(R.drawable.uth_01), // link hỏng thì hiện ảnh có sẵn trong app
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                LOGO_URL,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
            Image(
                painter = painterResource(R.drawable.uth),
                contentDescription = "Ảnh trường UTH",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.height(8.dp))
            Text("In app", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

// TextField: gõ tới đâu, dòng chữ đỏ bên dưới cập nhật tới đó
@Composable
fun TextFieldDetailScreen(nav: NavController) {
    var noiDung by remember { mutableStateOf("") }
    KhungChiTiet("TextField", nav) {
        Column(
            Modifier.align(Alignment.Center).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedTextField(
                value = noiDung,
                onValueChange = { noiDung = it },
                placeholder = { Text("Thông tin nhập") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                noiDung.ifEmpty { "Tự động cập nhật dữ liệu theo textfield" },
                color = Color.Red,
            )
            Spacer(Modifier.height(8.dp))
            Text("Số ký tự: ${noiDung.length}", color = Color.Gray)
        }
    }
}

// PasswordField: ẩn mật khẩu, bấm icon con mắt để hiện/ẩn
@Composable
fun PasswordDetailScreen(nav: NavController) {
    var matKhau by remember { mutableStateOf("") }
    var an by remember { mutableStateOf(true) }
    KhungChiTiet("PasswordField", nav) {
        OutlinedTextField(
            value = matKhau,
            onValueChange = { matKhau = it },
            placeholder = { Text("Mật khẩu") },
            visualTransformation = if (an) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = {
                IconButton(onClick = { an = !an }) {
                    Icon(
                        if (an) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (an) "Hiện mật khẩu" else "Ẩn mật khẩu",
                    )
                }
            },
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(32.dp),
        )
    }
}

// Ô vuông màu xanh dùng cho Column / Row
@Composable
fun OVuong(mau: Color) {
    Box(
        Modifier
            .padding(6.dp)
            .size(width = 70.dp, height = 50.dp)
            .background(mau, RoundedCornerShape(10.dp))
    )
}

// Column Layout: các phần tử xếp theo chiều dọc
@Composable
fun ColumnDetailScreen(nav: NavController) {
    KhungChiTiet("Column Layout", nav) {
        Column(
            Modifier
                .align(Alignment.Center)
                .background(NenXam, RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            OVuong(XanhNhat)
            OVuong(XanhDam)
            OVuong(XanhNhat)
        }
    }
}

// Row Layout: 4 hàng, mỗi hàng 3 ô xếp theo chiều ngang
@Composable
fun RowDetailScreen(nav: NavController) {
    KhungChiTiet("Row Layout", nav) {
        Column(Modifier.padding(24.dp)) {
            repeat(4) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .background(NenXam, RoundedCornerShape(12.dp)),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    OVuong(XanhNhat)
                    OVuong(XanhDam)
                    OVuong(XanhNhat)
                }
            }
        }
    }
}
