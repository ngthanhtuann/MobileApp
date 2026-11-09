package com.example.bt2firebaseall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Tab Thông báo (Firebase Cloud Messaging): hiện FCM token để gửi thử, và các thông báo đã nhận.

@Composable
fun NotificationsScreen(nav: NavController) = TabScaffold(nav, Routes.NOTIFICATIONS) { padding ->
    Column(
        Modifier.fillMaxSize().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Thông báo", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Column(Modifier.fillMaxWidth().card().padding(14.dp)) {
            Text("FCM token của máy này", fontWeight = FontWeight.SemiBold)
            Text(
                "Dùng token này (hoặc chủ đề \"$TOPIC\") để gửi thử từ Firebase Console › Messaging.",
                fontSize = 12.sp,
                color = AppColors.Gray,
            )
            Spacer(Modifier.height(6.dp))
            // Cho phép bôi đen để copy token
            SelectionContainer { Text(Inbox.token.value, fontSize = 11.sp) }
        }

        Text("Đã nhận (khi app đang mở)", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        if (Inbox.messages.isEmpty()) {
            Text("Chưa có thông báo nào", color = AppColors.Gray)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Inbox.messages) { message ->
                Text(message, Modifier.fillMaxWidth().card(10).padding(12.dp))
            }
        }
    }
}
