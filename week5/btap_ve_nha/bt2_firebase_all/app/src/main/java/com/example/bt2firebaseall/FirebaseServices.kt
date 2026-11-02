package com.example.bt2firebaseall

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.*
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

private const val TAG = "UthFirebase"

// ===================== 1. Authentication =====================

object AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    val user: FirebaseUser? get() = auth.currentUser

    suspend fun signInEmail(email: String, password: String) {
        require(email.isNotBlank() && password.isNotBlank()) { "Vui lòng nhập email và mật khẩu" }
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun signUpEmail(email: String, password: String) {
        require(email.isNotBlank() && password.isNotBlank()) { "Vui lòng nhập email và mật khẩu" }
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun signInGuest() {
        auth.signInAnonymously().await()
    }

    suspend fun signInGoogle(activity: Activity) {
        // default_web_client_id do plugin google-services sinh ra từ google-services.json
        val resId = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
        require(resId != 0) {
            "Chưa bật Google Sign-In trong Firebase Console. Bật xong hãy tải lại google-services.json."
        }
        val option = GetSignInWithGoogleOption.Builder(activity.getString(resId)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(activity).getCredential(activity, request).credential
        require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Không nhận được tài khoản Google"
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }

    suspend fun signOut(context: Context) {
        TaskRepository.stop()
        auth.signOut()
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }
}

/** Đổi lỗi Firebase sang câu thông báo tiếng Việt dễ hiểu. */
fun Throwable.toMessage(): String = when (this) {
    is FirebaseAuthWeakPasswordException -> "Mật khẩu quá yếu (cần ít nhất 6 ký tự)"
    is FirebaseAuthInvalidCredentialsException -> "Email không hợp lệ hoặc mật khẩu không đúng"
    is FirebaseAuthInvalidUserException -> "Tài khoản không tồn tại hoặc đã bị khóa"
    is FirebaseAuthUserCollisionException -> "Email này đã được đăng ký"
    is FirebaseNetworkException -> "Không có kết nối mạng"
    is GetCredentialCancellationException -> "Bạn đã hủy đăng nhập Google"
    is NoCredentialException -> "Máy chưa có tài khoản Google. Vào Cài đặt › Mật khẩu & tài khoản › Thêm tài khoản › Google rồi thử lại."
    else -> message ?: "Lỗi không xác định"
}

// ===================== 2. Realtime Database =====================

/** Công việc lưu tại users/{uid}/tasks/{id} và đồng bộ realtime. */
object TaskRepository {
    val tasks = mutableStateListOf<Task>()
    private var ref: DatabaseReference? = null

    private val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (!snapshot.exists()) return seedSamples()
            val list = snapshot.children.map {
                Task(
                    id = it.key!!,
                    title = it.child("title").getValue(String::class.java).orEmpty(),
                    category = runCatching { Category.valueOf(it.child("category").getValue(String::class.java)!!) }
                        .getOrDefault(Category.STUDY),
                    time = it.child("time").getValue(String::class.java).orEmpty(),
                    done = it.child("done").getValue(Boolean::class.java) == true,
                )
            }.sortedBy { it.time }
            tasks.clear()
            tasks.addAll(list)
        }

        override fun onCancelled(error: DatabaseError) {
            Log.w(TAG, "Database: ${error.message}")
        }
    }

    fun listen(uid: String) {
        stop()
        ref = FirebaseDatabase.getInstance().getReference("users/$uid/tasks").also { it.addValueEventListener(listener) }
    }

    fun stop() {
        ref?.removeEventListener(listener)
        ref = null
        tasks.clear()
    }

    fun add(title: String, category: Category, time: String) {
        ref?.push()?.setValue(mapOf("title" to title, "category" to category.name, "time" to time, "done" to false))
    }

    fun toggle(task: Task) {
        ref?.child(task.id)?.child("done")?.setValue(!task.done)
    }

    // Tài khoản mới chưa có việc nào -> tạo sẵn vài việc mẫu như trong đề
    private fun seedSamples() = listOf(
        Triple("Tập thể dục 30 phút", Category.LIFE, "06:30"),
        Triple("Nộp bài tập Lập trình di động", Category.STUDY, "09:00"),
        Triple("Ôn chương 3 – Cấu trúc dữ liệu", Category.STUDY, "14:00"),
        Triple("Đi chợ mua đồ cho tuần", Category.LIFE, "17:30"),
    ).forEach { (title, c, time) -> add(title, c, time) }
}

// ===================== 3. Storage =====================

/** Ảnh đại diện lưu tại avatars/{uid}.jpg */
object AvatarRepository {
    private fun ref(uid: String) = FirebaseStorage.getInstance().reference.child("avatars/$uid.jpg")

    suspend fun upload(uid: String, uri: Uri) {
        ref(uid).putFile(uri).await()
    }

    suspend fun load(uid: String): ImageBitmap? = runCatching {
        val bytes = ref(uid).getBytes(10L * 1024 * 1024).await()
        // shortcut: thu nhỏ cố định 4 lần với ảnh > 1MB, đủ cho avatar; muốn đẹp hơn thì resize trước khi upload
        val opts = BitmapFactory.Options().apply { inSampleSize = if (bytes.size > 1_000_000) 4 else 1 }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.asImageBitmap()
    }.onFailure { Log.i(TAG, "Chưa có avatar: ${it.message}") }.getOrNull()
}

// ===================== 4. Remote Config =====================

object RemoteConfigRepo {
    private const val KEY = "home_message"
    val homeMessage = mutableStateOf("Cố lên, bạn làm được!")

    fun fetch() {
        val rc = FirebaseRemoteConfig.getInstance()
        rc.setConfigSettingsAsync(FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(60).build())
        rc.setDefaultsAsync(mapOf(KEY to homeMessage.value))
        rc.fetchAndActivate().addOnCompleteListener {
            if (!it.isSuccessful) Log.w(TAG, "Remote Config lỗi", it.exception)
            homeMessage.value = rc.getString(KEY)
        }
    }
}

// ===================== 5. Analytics + Crashlytics =====================

object Tracker {
    fun log(context: Context, event: String, vararg params: Pair<String, String>) {
        FirebaseAnalytics.getInstance(context).logEvent(event, Bundle().apply { params.forEach { putString(it.first, it.second) } })
    }

    fun setUser(context: Context, uid: String) {
        FirebaseAnalytics.getInstance(context).setUserId(uid)
        FirebaseCrashlytics.getInstance().setUserId(uid)
    }
}

// ===================== 6. Cloud Messaging =====================

const val CHANNEL_ID = "uth_todo"
const val TOPIC = "uth_todo"

object Inbox {
    val messages = mutableStateListOf<String>()
    val token = mutableStateOf("Đang lấy token...")

    fun init(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "UTH Todo", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val fcm = FirebaseMessaging.getInstance()
        fcm.subscribeToTopic(TOPIC)
        fcm.token.addOnCompleteListener { token.value = if (it.isSuccessful) it.result else "Lỗi: ${it.exception?.message}" }
    }
}

class UthMessagingService : FirebaseMessagingService() {
    // Gọi khi app đang mở (hoặc tin chỉ có data). App ở nền thì hệ thống tự hiện thông báo.
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "UTH Todo"
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        Handler(Looper.getMainLooper()).post { Inbox.messages.add(0, "$title: $body") }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token mới: $token")
        Handler(Looper.getMainLooper()).post { Inbox.token.value = token }
    }
}
