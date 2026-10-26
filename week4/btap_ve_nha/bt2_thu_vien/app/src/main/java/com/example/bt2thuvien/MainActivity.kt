package com.example.bt2thuvien

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

// ===================== MODEL (4 đặc trưng OOP) =====================

// TRỪU TƯỢNG (Abstraction): lớp trừu tượng chỉ mô tả "người" có gì, không tạo trực tiếp được.
abstract class Person(val id: String, val name: String) {
    abstract fun role(): String
    open fun info() = "$id · ${role()}"
}

// KẾ THỪA (Inheritance): Student kế thừa Person.
class Student(id: String, name: String) : Person(id, name) {
    override fun role() = "Sinh viên" // ĐA HÌNH (Polymorphism): ghi đè hàm của lớp cha
}

// TRỪU TƯỢNG: interface định nghĩa hành vi "có thể mượn".
interface Borrowable {
    val isAvailable: Boolean
    fun borrowBy(student: Student): Boolean
    fun giveBack()
}

abstract class LibraryItem(val id: String, val title: String) : Borrowable {
    // ĐÓNG GÓI (Encapsulation): người mượn chỉ đổi được qua borrowBy/giveBack.
    private var _borrower by mutableStateOf<Student?>(null)
    val borrower: Student? get() = _borrower

    override val isAvailable get() = _borrower == null

    override fun borrowBy(student: Student): Boolean {
        if (!isAvailable) return false
        _borrower = student
        return true
    }

    override fun giveBack() { _borrower = null }

    abstract fun description(): String
}

// KẾ THỪA + ĐA HÌNH: mỗi loại tài liệu tự mô tả theo cách riêng.
class Book(id: String, title: String, val author: String) : LibraryItem(id, title) {
    override fun description() = "Sách · Tác giả: $author"
}

class Magazine(id: String, title: String, val issue: Int) : LibraryItem(id, title) {
    override fun description() = "Tạp chí · Số $issue"
}

class Library {
    // ĐÓNG GÓI: danh sách gốc là private, bên ngoài chỉ đọc.
    private val _items: SnapshotStateList<LibraryItem> = mutableStateListOf()
    private val _students: SnapshotStateList<Student> = mutableStateListOf()
    val items: List<LibraryItem> get() = _items
    val students: List<Student> get() = _students

    fun addBook(title: String, author: String): Boolean {
        if (title.isBlank()) return false
        _items += Book("S%02d".format(_items.size + 1), title.trim(), author.trim().ifBlank { "Không rõ" })
        return true
    }

    fun addMagazine(title: String, issue: Int) {
        _items += Magazine("S%02d".format(_items.size + 1), title, issue)
    }

    fun addStudent(name: String): Boolean {
        if (name.isBlank()) return false
        _students += Student("SV%02d".format(_students.size + 1), name.trim())
        return true
    }

    fun findStudent(id: String) = _students.firstOrNull { it.id == id }
    fun borrowedBy(student: Student) = _items.filter { it.borrower?.id == student.id }
    fun available() = _items.filter { it.isAvailable }

    companion object {
        fun sample() = Library().apply {
            addStudent("Nguyen Van A")
            addStudent("Tran Thi B")
            addStudent("Le Van C")
            addBook("Sách 01", "Tác giả A")
            addBook("Sách 02", "Tác giả B")
            addBook("Lập trình Kotlin", "JetBrains")
            addBook("Android Jetpack Compose", "Google")
            addMagazine("Tạp chí Khoa học", 12)
            items[0].borrowBy(students[0])
            items[1].borrowBy(students[0])
        }
    }
}

class Tab(val route: String, val label: String, val icon: ImageVector)

object Tabs {
    val MANAGE = Tab("manage", "Quản lý", Icons.Filled.Home)
    val BOOKS = Tab("books", "DS Sách", Icons.Filled.LibraryBooks)
    val STUDENTS = Tab("students", "Sinh viên", Icons.Filled.Person)
    val all = listOf(MANAGE, BOOKS, STUDENTS)
}

private val Primary = Color(0xFF1E4DB7)
private val CheckRed = Color(0xFFA0153E)

// ===================== UI =====================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { LibraryApp() } }
    }
}

@Composable
fun LibraryApp() {
    val library = remember { Library.sample() }
    var currentId by remember { mutableStateOf(library.students.first().id) }
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                Tabs.all.forEach { tab ->
                    NavigationBarItem(
                        selected = route == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(Tabs.MANAGE.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, null) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Primary, selectedTextColor = Primary),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, Tabs.MANAGE.route, Modifier.padding(padding).background(Color.White)) {
            composable(Tabs.MANAGE.route) {
                val student = library.findStudent(currentId) ?: library.students.first()
                ManageScreen(library, student) { currentId = it.id }
            }
            composable(Tabs.BOOKS.route) { BooksScreen(library) }
            composable(Tabs.STUDENTS.route) {
                StudentsScreen(library) { currentId = it.id; nav.navigate(Tabs.MANAGE.route) { popUpTo(0) } }
            }
        }
    }
}

@Composable
fun ScreenTitle(text: String) = Text(
    text, Modifier.fillMaxWidth().padding(vertical = 24.dp),
    fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
)

@Composable
fun ManageScreen(library: Library, student: Student, onChangeStudent: (Student) -> Unit) {
    var pickStudent by remember { mutableStateOf(false) }
    var pickBooks by remember { mutableStateOf(false) }
    val borrowed = library.borrowedBy(student)

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        ScreenTitle("Hệ thống\nQuản lý Thư viện")
        Text("Sinh viên", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(student.name, {}, Modifier.weight(1f), readOnly = true, singleLine = true)
            Spacer(Modifier.width(10.dp))
            Button(onClick = { pickStudent = true }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(Primary)) {
                Text("Thay đổi")
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Danh sách sách", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE0E0E0)).padding(12.dp),
        ) {
            if (borrowed.isEmpty()) {
                Text(
                    "Bạn chưa mượn quyển sách nào\nNhấn 'Thêm' để bắt đầu hành trình đọc sách!",
                    Modifier.align(Alignment.Center), textAlign = TextAlign.Center, color = Color.Gray,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(borrowed, key = { it.id }) { item ->
                        // Bỏ tick = trả sách
                        ItemRow(item, checked = true) { item.giveBack() }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { pickBooks = true }, modifier = Modifier.align(Alignment.CenterHorizontally).width(180.dp),
            shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(Primary),
        ) { Text("Thêm", fontSize = 16.sp) }
        Spacer(Modifier.height(16.dp))
    }

    if (pickStudent) {
        AlertDialog(
            onDismissRequest = { pickStudent = false },
            confirmButton = { TextButton({ pickStudent = false }) { Text("Đóng") } },
            title = { Text("Chọn sinh viên") },
            text = {
                LazyColumn {
                    items(library.students, key = { it.id }) { s ->
                        Text(
                            "${s.name}  (${s.info()})",
                            Modifier.fillMaxWidth().clickable { onChangeStudent(s); pickStudent = false }.padding(12.dp),
                            fontWeight = if (s.id == student.id) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            },
        )
    }

    if (pickBooks) {
        val selected = remember { mutableStateListOf<LibraryItem>() }
        val available = library.available()
        AlertDialog(
            onDismissRequest = { pickBooks = false },
            title = { Text("Mượn sách cho ${student.name}") },
            text = {
                if (available.isEmpty()) Text("Thư viện đã hết sách để mượn.")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(available, key = { it.id }) { item ->
                        ItemRow(item, checked = item in selected) { if (item in selected) selected -= item else selected += item }
                    }
                }
            },
            confirmButton = {
                TextButton({ selected.forEach { it.borrowBy(student) }; pickBooks = false }, enabled = selected.isNotEmpty()) {
                    Text("Mượn")
                }
            },
            dismissButton = { TextButton({ pickBooks = false }) { Text("Hủy") } },
        )
    }
}

@Composable
fun ItemRow(item: LibraryItem, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(12.dp)).background(Color.White, RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked, { onToggle() }, colors = CheckboxDefaults.colors(checkedColor = CheckRed))
        Column {
            Text(item.title)
            Text(item.description(), fontSize = 12.sp, color = Color.Gray) // đa hình: Book/Magazine tự mô tả
        }
    }
}

@Composable
fun BooksScreen(library: Library) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        ScreenTitle("Danh sách sách")
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Tên sách") }, singleLine = true)
        OutlinedTextField(author, { author = it }, Modifier.fillMaxWidth(), label = { Text("Tác giả") }, singleLine = true)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { if (library.addBook(title, author)) { title = ""; author = "" } },
            enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(Primary),
        ) { Text("Thêm sách") }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(library.items, key = { it.id }) { item ->
                InfoCard(
                    "${item.id} · ${item.title}",
                    item.description(),
                    item.borrower?.let { "Đang mượn: ${it.name}" } ?: "Còn trong thư viện",
                    if (item.isAvailable) Color(0xFF2E7D32) else CheckRed,
                )
            }
        }
    }
}

@Composable
fun StudentsScreen(library: Library, onSelect: (Student) -> Unit) {
    var name by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        ScreenTitle("Danh sách sinh viên")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(name, { name = it }, Modifier.weight(1f), label = { Text("Tên sinh viên") }, singleLine = true)
            Spacer(Modifier.width(8.dp))
            Button({ if (library.addStudent(name)) name = "" }, enabled = name.isNotBlank(), colors = ButtonDefaults.buttonColors(Primary)) {
                Text("Thêm")
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(library.students, key = { it.id }) { s ->
                val books = library.borrowedBy(s)
                Box(Modifier.clickable { onSelect(s) }) {
                    InfoCard(
                        s.name, s.info(),
                        if (books.isEmpty()) "Chưa mượn sách" else "Đang mượn: " + books.joinToString { it.title },
                        Primary,
                    )
                }
            }
        }
    }
}

@Composable
fun InfoCard(title: String, subtitle: String, status: String, statusColor: Color) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF2F4F8)).padding(14.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        Text(status, fontSize = 13.sp, color = statusColor)
    }
}
