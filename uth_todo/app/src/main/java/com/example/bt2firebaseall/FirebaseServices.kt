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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import java.io.IOException

// Các dịch vụ Firebase dùng trong app (bài Tuần 5): Authentication, Storage, Remote Config,
// Analytics + Crashlytics, Cloud Messaging. Công việc (tasks) nằm ở TaskRepository.kt.

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

    /** Hồ sơ › Sửa: đổi tên hiển thị trên Firebase Auth. */
    suspend fun updateName(name: String) {
        val request = UserProfileChangeRequest.Builder().setDisplayName(name).build()
        user!!.updateProfile(request).await()
    }

    /** Hiện bảng chọn tài khoản Google (Credential Manager), lấy idToken rồi đăng nhập Firebase. */
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

    /** Đăng xuất Firebase, xóa dữ liệu công việc trong máy và quên tài khoản Google đã chọn. */
    suspend fun signOut(context: Context) {
        TaskRepository.stop()
        auth.signOut()
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }
}

/** Đổi lỗi (Firebase, Google, gọi API) sang câu thông báo tiếng Việt dễ hiểu. */
fun Throwable.toMessage(): String = when (this) {
    is FirebaseAuthWeakPasswordException -> "Mật khẩu quá yếu (cần ít nhất 6 ký tự)"
    is FirebaseAuthInvalidCredentialsException -> "Email không hợp lệ hoặc mật khẩu không đúng"
    is FirebaseAuthInvalidUserException -> "Tài khoản không tồn tại hoặc đã bị khóa"
    is FirebaseAuthUserCollisionException -> "Email này đã được đăng ký"
    is FirebaseNetworkException -> "Không có kết nối mạng"
    is GetCredentialCancellationException -> "Bạn đã hủy đăng nhập Google"
    is NoCredentialException -> "Máy chưa có tài khoản Google. Vào Cài đặt › Mật khẩu & tài khoản › Thêm tài khoản › Google rồi thử lại."
    is HttpException -> when (code()) {
        401 -> "Token API không hợp lệ (401)"
        429 -> "Gọi API quá nhiều, thử lại sau ít phút (429)"
        else -> "Lỗi máy chủ (${code()})"
    }
    is IOException -> "Không có kết nối mạng"
    else -> message ?: "Lỗi không xác định"
}

// ===================== 2. Storage =====================

/** Ảnh đại diện lưu tại avatars/{uid}.jpg */
object AvatarRepository {
    private fun ref(uid: String) = FirebaseStorage.getInstance().reference.child("avatars/$uid.jpg")

    suspend fun upload(uid: String, uri: Uri) {
        ref(uid).putFile(uri).await()
    }

    suspend fun load(uid: String): ImageBitmap? = runCatching {
        val bytes = ref(uid).getBytes(10L * 1024 * 1024).await()
        // shortcut: thu nhỏ cố định 4 lần với ảnh > 1MB, đủ cho avatar; muốn đẹp hơn thì resize trước khi upload
        val options = BitmapFactory.Options().apply { inSampleSize = if (bytes.size > 1_000_000) 4 else 1 }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
    }.onFailure { Log.i(TAG, "Chưa có avatar: ${it.message}") }.getOrNull()
}

// ===================== 3. Remote Config =====================

/** Câu nhắn trong thẻ "Tiến độ hôm nay" ở Trang chủ, đổi được từ Firebase Console (key `home_message`). */
object RemoteConfigRepo {
    private const val KEY = "home_message"
    val homeMessage = mutableStateOf("Cố lên, bạn làm được!")

    fun fetch() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val settings = FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(60).build()
        remoteConfig.setConfigSettingsAsync(settings)
        remoteConfig.setDefaultsAsync(mapOf(KEY to homeMessage.value))
        remoteConfig.fetchAndActivate().addOnCompleteListener { result ->
            if (!result.isSuccessful) Log.w(TAG, "Remote Config lỗi", result.exception)
            homeMessage.value = remoteConfig.getString(KEY)
        }
    }
}

// ===================== 4. Analytics + Crashlytics =====================

object Tracker {
    /** Ghi một sự kiện Analytics, ví dụ log(context, "add_task", "category" to "study"). */
    fun log(context: Context, event: String, vararg params: Pair<String, String>) {
        val bundle = Bundle()
        params.forEach { (key, value) -> bundle.putString(key, value) }
        FirebaseAnalytics.getInstance(context).logEvent(event, bundle)
    }

    fun setUser(context: Context, uid: String) {
        FirebaseAnalytics.getInstance(context).setUserId(uid)
        FirebaseCrashlytics.getInstance().setUserId(uid)
    }
}

// ===================== 5. Cloud Messaging =====================

const val CHANNEL_ID = "uth_todo"
const val TOPIC = "uth_todo"

/** Thông báo đã nhận + FCM token của máy, hiển thị ở tab Thông báo. */
object Inbox {
    val messages = mutableStateListOf<String>()
    val token = mutableStateOf("Đang lấy token...")

    fun init(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "UTH Todo", NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val messaging = FirebaseMessaging.getInstance()
        messaging.subscribeToTopic(TOPIC)
        messaging.token.addOnCompleteListener { result ->
            token.value = if (result.isSuccessful) result.result else "Lỗi: ${result.exception?.message}"
        }
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
