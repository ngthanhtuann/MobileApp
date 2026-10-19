package com.example.btapvenha

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Màn hình 1: giới thiệu + nút "I'm ready"
@Composable
fun IntroScreen(nav: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Image(
            painter = painterResource(R.drawable.logo_compose),
            contentDescription = "Logo Jetpack Compose",
            modifier = Modifier.size(120.dp),
        )
        Spacer(Modifier.height(40.dp))
        Text("Jetpack Compose", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Text(
            "Jetpack Compose is a modern UI toolkit for building native Android " +
                "applications using a declarative programming approach.",
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            color = Color.Black.copy(alpha = 0.54f),
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                // popUpTo: xoá màn hình giới thiệu, bấm back không quay lại nữa
                nav.navigate("components") { popUpTo("intro") { inclusive = true } }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text("I'm ready", fontSize = 16.sp)
        }
    }
}
