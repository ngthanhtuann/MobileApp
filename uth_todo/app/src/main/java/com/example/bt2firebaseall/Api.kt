package com.example.bt2firebaseall

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// ===================== Model: cùng cấu trúc JSON với API trong đề =====================

data class Subtask(val title: String = "", val done: Boolean = false)

/** Mọi tham số đều có giá trị mặc định -> có constructor rỗng cho Gson. `id` nằm trên URL (/tasks/{id}) nên không gửi trong body. */
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

// ===================== Task API: mock API của thầy =====================
// Mọi request phải kèm header "Authorization: Bearer <token>" (thiếu là 401), gắn bằng interceptor bên dưới.

private const val MOCK_TOKEN = "Bearer TuanTruong-uth-student"

interface MockApi {
    @GET("tasks")
    suspend fun getTasks(): List<JsonObject>

    @GET("tasks/{id}")
    suspend fun getTask(@Path("id") id: String): JsonObject

    @POST("tasks")
    suspend fun addTask(@Body task: Task)

    @PATCH("tasks/{id}")
    suspend fun updateTask(@Path("id") id: String, @Body fields: Map<String, @JvmSuppressWildcards Any>)

    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") id: String)
}

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
    /** Gắn header "Authorization: Bearer <token>" vào mọi request gửi tới mock API. */
    private val addToken = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("Authorization", MOCK_TOKEN)
            .build()
        chain.proceed(request)
    }

    val mock: MockApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://share.tryapps.store/mock/teach/")
            .client(OkHttpClient.Builder().addInterceptor(addToken).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MockApi::class.java)
    }

    val crypto: CryptoApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CryptoApi::class.java)
    }
}
