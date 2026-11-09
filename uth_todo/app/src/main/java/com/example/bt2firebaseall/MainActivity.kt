package com.example.bt2firebaseall

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// Activity + điều hướng giữa các màn.
// Luồng: Khởi động → Onboarding (lần đầu) → Đăng nhập → Trang chủ / Danh sách / Thông báo / Cài đặt.

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppColors.load(this)
        Inbox.init(this)
        RemoteConfigRepo.fetch()
        setContent { AppTheme { AppNav() } }
    }
}

/** Áp dụng màu chủ đạo, chế độ sáng/tối và cỡ chữ đang chọn trong Cài đặt. */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    LaunchedEffect(systemDark) { AppColors.systemDark = systemDark }

    val colorScheme = if (AppColors.dark) {
        darkColorScheme(primary = AppColors.Primary)
    } else {
        lightColorScheme(primary = AppColors.Primary)
    }
    // Cỡ chữ trong Cài đặt được nhân thêm vào cỡ chữ của máy
    val density = LocalDensity.current
    val scaledDensity = Density(density.density, density.fontScale * AppColors.fontScale)

    MaterialTheme(colorScheme = colorScheme) {
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            Surface(Modifier.fillMaxSize(), color = AppColors.Bg, contentColor = AppColors.Text) {
                Box(Modifier.safeDrawingPadding()) { content() }
            }
        }
    }
}

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val HOME = "home"
    const val LIST = "list"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val DETAIL = "detail/{id}"
    const val ADD = "add"
    const val EDIT = "edit/{id}"
    const val CRYPTO = "crypto"

    fun detail(id: String) = "detail/$id"
    fun edit(id: String) = "edit/$id"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route

    /** Sau Khởi động / Onboarding: lần đầu -> Onboarding, đã đăng nhập -> Trang chủ, chưa -> Đăng nhập. */
    fun goToFirstScreen(context: Context) {
        val route = when {
            !Intro.isDone(context) -> Routes.ONBOARDING
            AuthRepository.user != null -> Routes.HOME
            else -> Routes.LOGIN
        }
        nav.navigate(route) { popUpTo(0) }
    }

    /** Chữ trên nút quay lại = tên màn trước đó, giống thiết kế ("< Danh sách", "< Cài đặt"...). */
    fun backLabel(): String = when (nav.previousBackStackEntry?.destination?.route) {
        Routes.LIST -> "Danh sách"
        Routes.SETTINGS -> "Cài đặt"
        Routes.NOTIFICATIONS -> "Thông báo"
        else -> "Trang chủ"
    }

    NavHost(nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            val context = LocalContext.current
            SplashScreen(onFinished = { goToFirstScreen(context) })
        }
        composable(Routes.ONBOARDING) {
            val context = LocalContext.current
            OnboardingScreen(onFinished = {
                Intro.markDone(context)
                goToFirstScreen(context)
            })
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onBack = { nav.navigate(Routes.ONBOARDING) { popUpTo(0) } },
                onSuccess = { nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
            )
        }

        // 4 tab ở thanh điều hướng dưới
        composable(Routes.HOME) { HomeScreen(nav) }
        composable(Routes.LIST) { ListScreen(nav) }
        composable(Routes.NOTIFICATIONS) { NotificationsScreen(nav) }
        composable(Routes.SETTINGS) { SettingsScreen(nav) }

        composable(Routes.DETAIL) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            DetailScreen(
                id = id,
                backLabel = backLabel(),
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(Routes.edit(id)) },
            )
        }
        composable(Routes.ADD) {
            TaskEditScreen(taskId = null, onClose = { nav.popBackStack() })
        }
        composable(Routes.EDIT) { entry ->
            TaskEditScreen(taskId = entry.arguments?.getString("id"), onClose = { nav.popBackStack() })
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                backLabel = backLabel(),
                onBack = { nav.popBackStack() },
                onLoggedOut = { nav.navigate(Routes.LOGIN) { popUpTo(0) } },
            )
        }
        composable(Routes.CRYPTO) { CryptoScreen(onBack = { nav.popBackStack() }) }
    }

    // Nâng cao 01: hỏi quyền 1 lần, nổi lên trên màn Đăng nhập / Trang chủ (sau Khởi động và Onboarding)
    val showPermissions = currentRoute != null && currentRoute != Routes.SPLASH && currentRoute != Routes.ONBOARDING
    if (showPermissions) PermissionOnboarding()
}

/** Chuyển tab ở thanh dưới mà không chồng nhiều bản của cùng một tab. */
fun NavController.openTab(route: String) = navigate(route) {
    popUpTo(Routes.HOME)
    launchSingleTop = true
}

/**
 * Chạy một thao tác gọi API trong coroutine.
 * Thành công -> gọi `onDone`; lỗi -> hiện Toast bằng tiếng Việt (xem toMessage()).
 */
fun CoroutineScope.call(context: Context, onDone: () -> Unit = {}, action: suspend () -> Unit): Job = launch {
    try {
        action()
        onDone()
    } catch (e: CancellationException) {
        throw e // màn hình đã đóng: không phải lỗi, không báo
    } catch (e: Exception) {
        Toast.makeText(context, e.toMessage(), Toast.LENGTH_LONG).show()
    }
}
