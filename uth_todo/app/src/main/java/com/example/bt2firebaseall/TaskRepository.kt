package com.example.bt2firebaseall

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Kho công việc: mọi thao tác đều gọi mock API của thầy (xem MockApi trong Api.kt).

/**
 * Mỗi màn hình gọi đúng API trong đề:
 *   Trang chủ / Danh sách: GET /tasks
 *   Chi tiết:              GET /tasks/{id}, PATCH /tasks/{id}, DELETE /tasks/{id}
 *   Việc mới:              POST /tasks
 *
 * Mock của thầy KHÔNG lưu thay đổi (luôn trả về cùng một bộ mẫu). Vì vậy app giữ các thay đổi
 * trong bộ nhớ (`editedTasks`, `deletedIds`) rồi ghép lên dữ liệu server khi hiển thị.
 * Tắt app hoặc bấm "Đặt lại dữ liệu mẫu" là quay về dữ liệu mẫu, giống prototype của thầy.
 */
object TaskRepository {
    /** Danh sách đang hiển thị (đã ghép thay đổi), sắp theo ngày rồi giờ. Giao diện đọc trực tiếp biến này. */
    val tasks = mutableStateListOf<Task>()
    val loading = mutableStateOf(false)
    val loadError = mutableStateOf<String?>(null)

    private const val LOCAL_ID_PREFIX = "local-" // id của việc thêm mới trong máy
    private val gson = Gson()

    private var currentUid: String? = null
    private var serverTasks = emptyList<Task>()          // kết quả GET /tasks gần nhất
    private val editedTasks = mutableMapOf<String, Task>() // id -> bản đã thêm/sửa trong phiên này
    private val deletedIds = mutableSetOf<String>()        // id đã xóa trong phiên này
    private var dayShift = 0L                              // số ngày cần dời (xem toTask)

    /** Gọi khi đăng nhập: đổi sang người dùng khác thì xóa dữ liệu của người cũ. */
    fun start(uid: String) {
        if (currentUid == uid) return
        stop()
        currentUid = uid
    }

    /** Gọi khi đăng xuất. */
    fun stop() {
        currentUid = null
        serverTasks = emptyList()
        editedTasks.clear()
        deletedIds.clear()
        tasks.clear()
    }

    /** GET /tasks: tải toàn bộ công việc. Lỗi thì ghi vào `loadError` để giao diện hiện nút Thử lại. */
    suspend fun load() {
        loading.value = true
        loadError.value = null
        try {
            val jsonList = Api.mock.getTasks()
            dayShift = daysToShift(jsonList)
            serverTasks = jsonList.map { toTask(it) }
            refreshShownTasks()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            loadError.value = e.toMessage()
        } finally {
            loading.value = false
        }
    }

    /** GET /tasks/{id}: dùng ở màn Chi tiết. Việc đã sửa trong phiên thì trả bản trong máy. */
    suspend fun get(id: String): Task {
        if (id in deletedIds) throw NoSuchElementException("Công việc không còn tồn tại")

        // Việc mới thêm chỉ có trong máy, server không biết id này
        if (id.startsWith(LOCAL_ID_PREFIX)) {
            return editedTasks[id] ?: throw NoSuchElementException("Công việc không còn tồn tại")
        }

        val fromServer = toTask(Api.mock.getTask(id))
        return editedTasks[id] ?: fromServer
    }

    /** POST /tasks. Mock luôn trả về cùng một id, nên app tự tạo id riêng để các việc mới không trùng nhau. */
    suspend fun add(task: Task) {
        Api.mock.addTask(task)
        val newId = LOCAL_ID_PREFIX + System.currentTimeMillis()
        editedTasks[newId] = task.copy(id = newId)
        refreshShownTasks()
    }

    /** PATCH /tasks/{id}: chỉ gửi các trường thay đổi, ví dụ mapOf("done" to true). */
    suspend fun update(id: String, changes: Map<String, Any>) {
        Api.mock.updateTask(id, changes)
        val current = tasks.find { it.id == id } ?: get(id)
        editedTasks[id] = applyChanges(current, changes)
        refreshShownTasks()
    }

    /** Tick xong / bỏ tick ở Trang chủ và Danh sách. */
    suspend fun toggleDone(task: Task) = update(task.id, mapOf("done" to !task.done))

    /** DELETE /tasks/{id}. */
    suspend fun delete(id: String) {
        Api.mock.deleteTask(id)
        editedTasks.remove(id)
        deletedIds += id
        refreshShownTasks()
    }

    /** Cài đặt › Đặt lại dữ liệu mẫu: bỏ mọi thay đổi trong phiên rồi GET /tasks lại. */
    suspend fun resetSamples() {
        editedTasks.clear()
        deletedIds.clear()
        load()
        loadError.value?.let { message -> throw IllegalStateException(message) }
    }

    // ---------- Hàm phụ ----------

    /** Ghép dữ liệu server với thay đổi trong máy rồi cập nhật `tasks`. */
    private fun refreshShownTasks() {
        val serverIds = serverTasks.map { it.id }.toSet()
        val fromServer = serverTasks
            .filter { it.id !in deletedIds }
            .map { editedTasks[it.id] ?: it }
        val addedInApp = editedTasks.values.filter { it.id !in serverIds }

        tasks.clear()
        tasks.addAll((fromServer + addedInApp).sortedWith(compareBy({ it.date }, { it.time })))
    }

    /**
     * Ngày trong mock cố định (2026-10-20 là "hôm nay" của prototype).
     * Dời tất cả đi cùng số ngày sao cho ngày sớm nhất = hôm nay, để màn "Việc hôm nay" luôn có dữ liệu.
     */
    private fun daysToShift(jsonList: List<JsonObject>): Long {
        val earliest = jsonList
            .mapNotNull { runCatching { LocalDate.parse(it["date"].asString) }.getOrNull() }
            .minOrNull() ?: return 0
        return ChronoUnit.DAYS.between(earliest, LocalDate.now())
    }

    /** JSON của API -> Task (lấy thêm `id`, và dời ngày theo `dayShift`). */
    private fun toTask(json: JsonObject): Task {
        val task = gson.fromJson(json, Task::class.java)
        val shiftedDate = runCatching { LocalDate.parse(task.date).plusDays(dayShift).toString() }.getOrDefault(task.date)
        return task.copy(id = json["id"].asString, date = shiftedDate)
    }

    /** Áp các trường trong `changes` lên `task` (giống cách server xử lý PATCH). */
    private fun applyChanges(task: Task, changes: Map<String, Any>): Task {
        val json = gson.toJsonTree(task).asJsonObject
        gson.toJsonTree(changes).asJsonObject.entrySet().forEach { (field, value) -> json.add(field, value) }
        return gson.fromJson(json, Task::class.java).copy(id = task.id)
    }
}
