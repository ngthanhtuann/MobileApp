package com.example.btapvenha

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

// Một thành phần UI: tên, mô tả, và màn hình chi tiết của nó
class Muc(val ten: String, val moTa: String, val manHinh: @Composable (NavController) -> Unit)

val danhSachNhom: List<Pair<String, List<Muc>>> = listOf(
    "Display" to listOf(
        Muc("Text", "Displays text") { TextDetailScreen(it) },
        Muc("Image", "Displays an image") { ImageDetailScreen(it) },
    ),
    "Input" to listOf(
        Muc("TextField", "Input field for text") { TextFieldDetailScreen(it) },
        Muc("PasswordField", "Input field for passwords") { PasswordDetailScreen(it) },
        Muc("Toggle", "On/off switch") { ToggleDetailScreen(it) },
        Muc("Slider", "Selects a value from a range") { SliderDetailScreen(it) },
        Muc("DatePicker", "Selects a date and time") { DatePickerDetailScreen(it) },
    ),
    "Layout" to listOf(
        Muc("Column", "Arranges elements vertically") { ColumnDetailScreen(it) },
        Muc("Row", "Arranges elements horizontally") { RowDetailScreen(it) },
    ),
    "Selection" to listOf(
        Muc("Button", "Triggers an action when tapped") { ButtonDetailScreen(it) },
        Muc("Checkbox", "Selects one or multiple options") { CheckboxDetailScreen(it) },
        Muc("RadioButton", "Selects a single option") { RadioDetailScreen(it) },
        Muc("Picker", "Selects a value from a list") { PickerDetailScreen(it) },
    ),
    "Navigation" to listOf(
        Muc("NavigationView", "Navigation container with title and back button") { NavigationDetailScreen(it) },
        Muc("TabBar", "Switches between main sections") { TabBarDetailScreen(it) },
        Muc("Menu", "Shows a menu with multiple actions") { MenuDetailScreen(it) },
    ),
    "Content" to listOf(
        Muc("List", "Displays a list of items") { ListDetailScreen(it) },
        Muc("Card", "Container with content and style") { CardDetailScreen(it) },
        Muc("Divider", "Separates content into sections") { DividerDetailScreen(it) },
    ),
)

// Màn hình 2: danh sách các thành phần UI, bấm vào để xem chi tiết
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentsScreen(nav: NavController) {
    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("UI Components List", color = MauXanh) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
        ) {
            for ((nhom, cacMuc) in danhSachNhom) {
                // Tiêu đề nhóm: Display, Input, Layout, ...
                item {
                    Text(
                        nhom,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                    )
                }
                for (muc in cacMuc) {
                    item {
                        Card(
                            onClick = { nav.navigate(muc.ten) },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFBBDEFB)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(muc.ten, fontWeight = FontWeight.SemiBold)
                                Text(muc.moTa)
                            }
                        }
                    }
                }
            }
        }
    }
}
