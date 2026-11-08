package com.example.bt2firebaseall

import com.google.firebase.FirebaseApp
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ===================== Model: cùng cấu trúc JSON với API trong đề =====================

data class Subtask(val title: String = "", val done: Boolean = false)

/** Mọi tham số đều có giá trị mặc định -> có constructor rỗng cho Gson. `id` là key trong database, không lưu vào body. */
data class Task(
    @Transient val id: String = "",
    val title: String = "",
    val note: String = "",
    val category: String = "study", // study | life
    val date: String = "",          // yyyy-MM-dd
    val time: String = "",          // HH:mm
    val priority: String = "medium", // low | medium | high
    val remind: Boolean = true,
    val done: Boolean = false,
    val subtasks: List<Subtask> = emptyList(),
)

// ===================== Task API =====================
// Mock API của thầy (share.tryapps.store/mock/teach/tasks) cần token riêng (trả 401),
// nên dùng REST API của chính Firebase Realtime Database: cùng các lệnh GET / PATCH / DELETE.

interface TaskApi {
    @GET("users/{uid}/tasks.json")
    suspend fun getTasks(@Path("uid") uid: String, @Query("auth") auth: String): Response<Map<String, Task>>

    @GET("users/{uid}/tasks/{id}.json")
    suspend fun getTask(@Path("uid") uid: String, @Path("id") id: String, @Query("auth") auth: String): Response<Task>

    @POST("users/{uid}/tasks.json")
    suspend fun addTask(@Path("uid") uid: String, @Query("auth") auth: String, @Body task: Task)

    @PATCH("users/{uid}/tasks/{id}.json")
    suspend fun updateTask(
        @Path("uid") uid: String, @Path("id") id: String, @Query("auth") auth: String,
        @Body fields: Map<String, @JvmSuppressWildcards Any>,
    )

    @DELETE("users/{uid}/tasks/{id}.json")
    suspend fun deleteTask(@Path("uid") uid: String, @Path("id") id: String, @Query("auth") auth: String)

    @PUT("users/{uid}/tasks.json")
    suspend fun replaceAll(@Path("uid") uid: String, @Query("auth") auth: String, @Body tasks: Map<String, Task>)
}

/** Database trả "null" khi chưa có dữ liệu, nên GET dùng Response<T> và tự kiểm tra lỗi HTTP. */
fun <T> Response<T>.bodyOrThrow(): T? = if (isSuccessful) body() else throw HttpException(this)

// ===================== Crypto API (Nâng cao 02) =====================

data class SparkData(val price: List<Double>)

data class Coin(
    val id: String,
    val symbol: String,
    val name: String,
    @SerializedName("current_price") val price: Double,
    @SerializedName("price_change_percentage_24h") val changePct: Double?,
    @SerializedName("sparkline_in_7d") val sparkline: SparkData?,
)

data class TrendingItem(val name: String, val symbol: String)
data class TrendingCoin(val item: TrendingItem)
data class Trending(val coins: List<TrendingCoin>)

interface CryptoApi {
    @GET("coins/markets?vs_currency=usd&sparkline=true")
    suspend fun markets(@Query("ids") ids: String): List<Coin>

    @GET("search/trending")
    suspend fun trending(): Trending
}

object Api {
    val tasks: TaskApi by lazy {
        Retrofit.Builder()
            .baseUrl(FirebaseApp.getInstance().options.databaseUrl!! + "/")
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(TaskApi::class.java)
    }

    val mock: MockApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://share.tryapps.store/mock/teach/")
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(MockApi::class.java)
    }

    val crypto: CryptoApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(CryptoApi::class.java)
    }
}

// ===================== Mock API của thầy: nguồn dữ liệu mẫu =====================
// Mock luôn trả cùng một bộ mẫu (PATCH/POST/DELETE không lưu lại), nên chỉ dùng để lấy dữ liệu ban đầu.

private const val MOCK_TOKEN = "Bearer TuanTruong-uth-student"

interface MockApi {
    @Headers("Authorization: $MOCK_TOKEN")
    @GET("tasks")
    suspend fun getTasks(): List<JsonObject>
}

private val gson = Gson()

/**
 * GET /mock/teach/tasks rồi đổi thành map để lưu vào Firebase.
 * Ngày trong mock cố định (2026-10-20 là "hôm nay" của prototype) -> dời sao cho ngày sớm nhất = hôm nay.
 */
suspend fun mockSampleTasks(): Map<String, Task> {
    val list = Api.mock.getTasks().map { it["id"].asString to gson.fromJson(it, Task::class.java) }
    val first = list.minOf { LocalDate.parse(it.second.date) }
    val shift = ChronoUnit.DAYS.between(first, LocalDate.now())
    // key dạng chữ: key toàn số thì Realtime Database sẽ trả về mảng thay vì object
    return list.associate { (id, t) -> "task$id" to t.copy(date = LocalDate.parse(t.date).plusDays(shift).toString()) }
}
