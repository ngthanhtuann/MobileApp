package com.example.bt2firebaseall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Danh sách: tìm kiếm, lọc theo nhóm, chia hai phần CẦN LÀM / ĐÃ XONG.

@Composable
fun ListScreen(nav: NavController) = TabScaffold(nav, Routes.LIST) { padding ->
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchText by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<Category?>(null) } // null = Tất cả

    val shownTasks = TaskRepository.tasks.filter { task ->
        val matchesCategory = selectedCategory == null || task.cat == selectedCategory
        val matchesSearch = task.title.contains(searchText.trim(), ignoreCase = true)
        matchesCategory && matchesSearch
    }
    val (doneTasks, todoTasks) = shownTasks.partition { it.done }
    val notDoneCount = TaskRepository.tasks.count { !it.done }

    @Composable
    fun TaskItem(task: Task) = TaskRow(
        task = task,
        timeLabel = task.whenLabel(),
        compact = task.done,
        onOpen = { nav.navigate(Routes.detail(task.id)) },
        onToggleDone = { scope.call(context) { TaskRepository.toggleDone(task) } },
    )

    LazyColumn(
        Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Danh sách", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("$notDoneCount việc chưa xong", fontSize = 12.sp, color = AppColors.Gray)
            }
        }
        item {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Tìm công việc") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("Tất cả") },
                )
                Category.entries.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.label) },
                    )
                }
            }
        }
        item { LoadState() }

        if (todoTasks.isNotEmpty()) item { SectionLabel("CẦN LÀM") }
        items(todoTasks, key = { it.id }) { TaskItem(it) }

        if (doneTasks.isNotEmpty()) item { SectionLabel("ĐÃ XONG") }
        items(doneTasks, key = { it.id }) { TaskItem(it) }

        if (shownTasks.isEmpty() && !TaskRepository.loading.value) {
            item { Text("Không có công việc phù hợp", color = AppColors.Gray) }
        }
    }
}
