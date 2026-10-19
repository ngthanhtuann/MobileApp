package com.example.btapvenha

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// Các thành phần UI thêm (lấy từ phần "Tự tìm hiểu" trong đề)

// ===================== INPUT =====================

// Toggle: công tắc bật / tắt
@Composable
fun ToggleDetailScreen(nav: NavController) {
    var wifi by remember { mutableStateOf(true) }
    var denPin by remember { mutableStateOf(false) }
    KhungChiTiet("Toggle", nav) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            DongCongTac(Icons.Filled.Wifi, "Wi-Fi", wifi) { wifi = it }
            DongCongTac(Icons.Filled.FlashlightOn, "Đèn pin", denPin) { denPin = it }
            Spacer(Modifier.height(24.dp))
            // Bật đèn pin thì bóng đèn sáng lên
            Icon(
                Icons.Filled.Lightbulb,
                contentDescription = null,
                tint = if (denPin) Color(0xFFFFC107) else Color(0xFFE0E0E0),
                modifier = Modifier.size(100.dp),
            )
            Text(
                "Wi-Fi: ${if (wifi) "Bật" else "Tắt"}  |  Đèn pin: ${if (denPin) "Bật" else "Tắt"}",
                color = Color.Red,
            )
        }
    }
}

@Composable
fun DongCongTac(icon: ImageVector, ten: String, bat: Boolean, doi: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(16.dp))
        Text(ten, Modifier.weight(1f))
        Switch(checked = bat, onCheckedChange = doi)
    }
}

// Slider: kéo để chọn giá trị trong 1 khoảng
@Composable
fun SliderDetailScreen(nav: NavController) {
    var coChu by remember { mutableStateOf(24f) }
    KhungChiTiet("Slider", nav) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Cỡ chữ: ${coChu.roundToInt()}")
            Slider(
                value = coChu,
                onValueChange = { coChu = it },
                valueRange = 10f..60f,
                steps = 49, // 49 nấc ở giữa => bước nhảy 1
            )
            Spacer(Modifier.height(24.dp))
            // Kéo slider thì chữ to / nhỏ theo
            Text("Xin chào UTH", fontSize = coChu.sp, color = MauXanh)
        }
    }
}

// DatePicker: chọn ngày và giờ
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDetailScreen(nav: NavController) {
    var moNgay by remember { mutableStateOf(false) }
    var moGio by remember { mutableStateOf(false) }
    val ngayState = rememberDatePickerState()
    val gioState = rememberTimePickerState()
    var chuoiNgay by remember { mutableStateOf("--") }
    var chuoiGio by remember { mutableStateOf("--") }

    KhungChiTiet("DatePicker", nav) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedButton(onClick = { moNgay = true }) {
                Icon(Icons.Filled.CalendarToday, null)
                Spacer(Modifier.width(8.dp))
                Text("Chọn ngày")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { moGio = true }) {
                Icon(Icons.Filled.AccessTime, null)
                Spacer(Modifier.width(8.dp))
                Text("Chọn giờ")
            }
            Spacer(Modifier.height(24.dp))
            Text("Ngày: $chuoiNgay   Giờ: $chuoiGio", color = Color.Red, fontSize = 16.sp)
        }
    }

    if (moNgay) {
        DatePickerDialog(
            onDismissRequest = { moNgay = false },
            confirmButton = {
                TextButton(onClick = {
                    // DatePicker trả về mili-giây theo giờ UTC
                    ngayState.selectedDateMillis?.let {
                        val d = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        chuoiNgay = "${d.dayOfMonth}/${d.monthValue}/${d.year}"
                    }
                    moNgay = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { moNgay = false }) { Text("Huỷ") } },
        ) { DatePicker(state = ngayState) }
    }

    if (moGio) {
        AlertDialog(
            onDismissRequest = { moGio = false },
            text = { TimePicker(state = gioState) },
            confirmButton = {
                TextButton(onClick = {
                    chuoiGio = "%02d:%02d".format(gioState.hour, gioState.minute)
                    moGio = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { moGio = false }) { Text("Huỷ") } },
        )
    }
}

// ===================== SELECTION =====================

// Button: bấm để thực hiện hành động (đếm số lần bấm)
@Composable
fun ButtonDetailScreen(nav: NavController) {
    var dem by remember { mutableIntStateOf(0) }
    KhungChiTiet("Button", nav) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Đã bấm: $dem lần", fontSize = 22.sp)
            Spacer(Modifier.height(16.dp))
            // Các kiểu nút thường gặp
            Button(onClick = { dem++ }) { Text("Button +1") }
            ElevatedButton(onClick = { dem++ }) { Text("ElevatedButton +1") }
            FilledTonalButton(onClick = { dem++ }) { Text("FilledTonalButton +1") }
            OutlinedButton(onClick = { dem = 0 }) { Text("OutlinedButton: Reset") }
            TextButton(onClick = { dem-- }) { Text("TextButton -1") }
            IconButton(onClick = { dem++ }) {
                Icon(Icons.Filled.Favorite, "Thích", tint = Color.Red)
            }
        }
    }
}

// Checkbox: chọn được nhiều mục
@Composable
fun CheckboxDetailScreen(nav: NavController) {
    val soThich = listOf("Đọc sách", "Bóng đá", "Nghe nhạc", "Du lịch")
    var daChon by remember { mutableStateOf(setOf<String>()) }
    KhungChiTiet("Checkbox", nav) {
        Column(Modifier.padding(24.dp)) {
            Text("Sở thích của bạn (chọn nhiều):")
            for (s in soThich) {
                val chon = s in daChon
                Row(
                    Modifier.fillMaxWidth().clickable { daChon = if (chon) daChon - s else daChon + s },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = chon, onCheckedChange = null)
                    Text(s, Modifier.padding(12.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                if (daChon.isEmpty()) "Chưa chọn gì" else "Đã chọn: ${daChon.joinToString(", ")}",
                color = Color.Red,
            )
        }
    }
}

// RadioButton: chỉ chọn được 1 mục
@Composable
fun RadioDetailScreen(nav: NavController) {
    var gioiTinh by remember { mutableStateOf("Nam") }
    KhungChiTiet("RadioButton", nav) {
        Column(Modifier.padding(24.dp)) {
            Text("Giới tính (chỉ chọn 1):")
            for (g in listOf("Nam", "Nữ", "Khác")) {
                Row(
                    Modifier.fillMaxWidth().clickable { gioiTinh = g },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = gioiTinh == g, onClick = null)
                    Text(g, Modifier.padding(12.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Bạn chọn: $gioiTinh", color = Color.Red)
        }
    }
}

// Picker: chọn 1 giá trị từ danh sách xổ xuống
@Composable
fun PickerDetailScreen(nav: NavController) {
    val dsThanhPho = listOf("TP. Hồ Chí Minh", "Hà Nội", "Đà Nẵng", "Cần Thơ")
    var thanhPho by remember { mutableStateOf(dsThanhPho[0]) }
    var mo by remember { mutableStateOf(false) }
    KhungChiTiet("Picker", nav) {
        Column(Modifier.padding(24.dp)) {
            Box {
                OutlinedButton(onClick = { mo = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(thanhPho, Modifier.weight(1f))
                    Icon(Icons.Filled.ArrowDropDown, null)
                }
                DropdownMenu(expanded = mo, onDismissRequest = { mo = false }) {
                    for (t in dsThanhPho) {
                        DropdownMenuItem(text = { Text(t) }, onClick = { thanhPho = t; mo = false })
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Thành phố: $thanhPho", color = Color.Red)
        }
    }
}

// ===================== NAVIGATION =====================

// NavigationView: thanh tiêu đề + nút back, bấm nút để sang trang con
@Composable
fun NavigationDetailScreen(nav: NavController) {
    KhungChiTiet("NavigationView", nav) {
        Button(onClick = { nav.navigate("trang_con") }, modifier = Modifier.align(Alignment.Center)) {
            Text("Sang trang con")
        }
    }
}

// TabBar: chuyển giữa các phần chính, vuốt ngang cũng chuyển được
@Composable
fun TabBarDetailScreen(nav: NavController) {
    val tabs = listOf(
        Triple("Trang chủ", Icons.Filled.Home, "Nội dung Trang chủ"),
        Triple("Tìm kiếm", Icons.Filled.Search, "Nội dung Tìm kiếm"),
        Triple("Cá nhân", Icons.Filled.Person, "Nội dung Cá nhân"),
    )
    val pager = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    KhungChiTiet("TabBar", nav) {
        Column {
            PrimaryTabRow(selectedTabIndex = pager.currentPage, containerColor = Color.White) {
                tabs.forEachIndexed { i, (ten, icon, _) ->
                    Tab(
                        selected = pager.currentPage == i,
                        onClick = { scope.launch { pager.animateScrollToPage(i) } },
                        text = { Text(ten) },
                        icon = { Icon(icon, null) },
                    )
                }
            }
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { i ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(tabs[i].third)
                }
            }
        }
    }
}

// Menu: bấm để hiện danh sách nhiều hành động
@Composable
fun MenuDetailScreen(nav: NavController) {
    var hanhDong by remember { mutableStateOf("Chưa chọn") }
    var mo by remember { mutableStateOf(false) }
    val cacMuc = listOf("Sửa" to Icons.Filled.Edit, "Chia sẻ" to Icons.Filled.Share, "Xoá" to Icons.Filled.Delete)
    KhungChiTiet("Menu", nav) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                AssistChip(
                    onClick = { mo = true },
                    label = { Text("Bấm để mở menu") },
                    leadingIcon = { Icon(Icons.Filled.MoreVert, null) },
                )
                DropdownMenu(expanded = mo, onDismissRequest = { mo = false }) {
                    for ((ten, icon) in cacMuc) {
                        DropdownMenuItem(
                            text = { Text(ten) },
                            leadingIcon = { Icon(icon, null) },
                            onClick = { hanhDong = ten; mo = false },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Bạn chọn: $hanhDong", color = Color.Red)
        }
    }
}

// ===================== CONTENT =====================

// List: danh sách nhiều phần tử, cuộn được
@Composable
fun ListDetailScreen(nav: NavController) {
    KhungChiTiet("List", nav) {
        // LazyColumn: chỉ vẽ những dòng đang hiện trên màn hình
        LazyColumn {
            items(30) { i ->
                ListItem(
                    leadingContent = {
                        Box(
                            Modifier.size(40.dp).background(Color(0xFFBBDEFB), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Text("${i + 1}") }
                    },
                    headlineContent = { Text("Sinh viên ${i + 1}") },
                    supportingContent = { Text("MSSV: 2251${(i + 1).toString().padStart(4, '0')}") },
                )
            }
        }
    }
}

// Card: khung chứa nội dung có bo góc, đổ bóng
@Composable
fun CardDetailScreen(nav: NavController) {
    KhungChiTiet("Card", nav) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ElevatedCard(elevation = CardDefaults.elevatedCardElevation(4.dp)) {
                Image(
                    painter = painterResource(R.drawable.uth),
                    contentDescription = "Ảnh trường UTH",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                )
                Column(Modifier.padding(16.dp)) {
                    Text("Trường ĐH Giao thông vận tải TP.HCM", fontWeight = FontWeight.SemiBold)
                    Text("Card gồm ảnh + chữ + bóng đổ")
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFBBDEFB))) {
                ListItem(
                    leadingContent = { Icon(Icons.Filled.Info, null) },
                    headlineContent = { Text("Card màu xanh") },
                    supportingContent = { Text("Đổi màu nền bằng thuộc tính colors") },
                    colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}

// Divider: đường kẻ chia các phần
@Composable
fun DividerDetailScreen(nav: NavController) {
    KhungChiTiet("Divider", nav) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Phần 1")
            HorizontalDivider() // mặc định: mảnh, màu xám
            Text("Phần 2")
            HorizontalDivider(thickness = 3.dp, color = MauXanh) // dày, màu xanh
            Text("Phần 3")
            HorizontalDivider(Modifier.padding(horizontal = 40.dp), color = Color.Red) // thụt 2 đầu
            Text("Phần 4")
        }
    }
}
