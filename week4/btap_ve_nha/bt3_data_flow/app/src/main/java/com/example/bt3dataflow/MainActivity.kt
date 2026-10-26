package com.example.bt3dataflow

import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// ===================== DỮ LIỆU =====================

/** Dữ liệu chảy qua 4 màn hình: email (màn 1) → otp (màn 2) → password (màn 3) → xác nhận (màn 4). */
class ResetRequest(val email: String, val otp: String, val password: String) {
    fun saveTo(handle: SavedStateHandle) {
        handle[KEY_EMAIL] = email
        handle[KEY_OTP] = otp
        handle[KEY_PASS] = password
    }

    companion object {
        private const val KEY_EMAIL = "email"
        private const val KEY_OTP = "otp"
        private const val KEY_PASS = "password"
        const val OTP_LENGTH = 5
        const val MIN_PASSWORD = 6

        fun from(handle: SavedStateHandle): ResetRequest? {
            val email = handle.get<String>(KEY_EMAIL) ?: return null
            return ResetRequest(email, handle[KEY_OTP] ?: "", handle[KEY_PASS] ?: "")
        }

        fun from(entry: NavBackStackEntry) = ResetRequest(entry.arg(KEY_EMAIL), entry.arg(KEY_OTP), entry.arg(KEY_PASS))

        private fun NavBackStackEntry.arg(key: String) = arguments?.getString(key).orEmpty()

        fun isValidEmail(email: String) = Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}

object Routes {
    const val FORGOT = "forgot"
    const val VERIFY = "verify/{email}"
    const val RESET = "reset/{email}/{otp}"
    const val CONFIRM = "confirm/{email}/{otp}/{password}"

    private fun e(s: String) = Uri.encode(s)
    fun verify(email: String) = "verify/${e(email)}"
    fun reset(email: String, otp: String) = "reset/${e(email)}/${e(otp)}"
    fun confirm(email: String, otp: String, pass: String) = "confirm/${e(email)}/${e(otp)}/${e(pass)}"
}

private val Blue = Color(0xFF3D8BEF)
private val FieldBg = Color(0xFFF7F7F9)

// ===================== UI =====================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AppNav() } }
    }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(nav, Routes.FORGOT) {
        composable(Routes.FORGOT) { entry ->
            // Dữ liệu trả về từ màn 4 (nếu có)
            ForgotScreen(ResetRequest.from(entry.savedStateHandle)) { email -> nav.navigate(Routes.verify(email)) }
        }
        composable(Routes.VERIFY) { entry ->
            val email = ResetRequest.from(entry).email
            VerifyScreen(onBack = { nav.popBackStack() }) { otp -> nav.navigate(Routes.reset(email, otp)) }
        }
        composable(Routes.RESET) { entry ->
            val req = ResetRequest.from(entry)
            ResetScreen(onBack = { nav.popBackStack() }) { pass -> nav.navigate(Routes.confirm(req.email, req.otp, pass)) }
        }
        composable(Routes.CONFIRM) { entry ->
            val req = ResetRequest.from(entry)
            ConfirmScreen(req, onBack = { nav.popBackStack() }) {
                req.saveTo(nav.getBackStackEntry(Routes.FORGOT).savedStateHandle)
                nav.popBackStack(Routes.FORGOT, inclusive = false)
            }
        }
    }
}

@Composable
fun AuthLayout(title: String, subtitle: String, onBack: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color.White).systemBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(40.dp)) {
            if (onBack != null) {
                FilledIconButton(
                    onClick = onBack, shape = RoundedCornerShape(10.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Blue),
                ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Quay lại", tint = Color.White) }
            }
        }
        Spacer(Modifier.height(24.dp))
        Image(painterResource(R.drawable.uth_logo), "UTH", Modifier.height(70.dp))
        Text("SmartTasks", color = Blue, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = Color.Gray, textAlign = TextAlign.Center, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        content()
    }
}

@Composable
fun Field(
    value: String, onChange: (String) -> Unit, placeholder: String, icon: ImageVector,
    password: Boolean = false, readOnly: Boolean = false, keyboard: KeyboardType = KeyboardType.Text,
) = OutlinedTextField(
    value, onChange, Modifier.fillMaxWidth().padding(vertical = 6.dp),
    placeholder = { Text(placeholder) }, leadingIcon = { Icon(icon, null) },
    singleLine = true, readOnly = readOnly, shape = RoundedCornerShape(10.dp),
    visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
    keyboardOptions = KeyboardOptions(keyboardType = keyboard),
    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = FieldBg, focusedContainerColor = FieldBg),
)

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit) = Button(
    onClick, Modifier.fillMaxWidth().padding(top = 16.dp).height(50.dp),
    shape = RoundedCornerShape(25.dp), colors = ButtonDefaults.buttonColors(Blue),
) { Text(text, fontSize = 16.sp) }

@Composable
fun ErrorText(error: String?) {
    if (error != null) Text(error, color = Color.Red, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
}

@Composable
fun ForgotScreen(result: ResetRequest?, onNext: (String) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AuthLayout("Forget Password?", "Enter your Email, we will send you a verification code.", onBack = null) {
        Field(email, { email = it; error = null }, "Your Email", Icons.Outlined.Email, keyboard = KeyboardType.Email)
        ErrorText(error)
        PrimaryButton("Next") {
            if (ResetRequest.isValidEmail(email.trim())) onNext(email.trim()) else error = "Email không hợp lệ"
        }
        if (result != null) {
            Spacer(Modifier.height(24.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFFDCE8F8)).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Dữ liệu từ màn hình 4", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Email: ${result.email}", color = Color(0xFFC62828))
                Text("OTP: ${result.otp}", color = Color(0xFFC62828))
                Text("Password: ${result.password}", color = Color(0xFFC62828))
            }
        }
    }
}

@Composable
fun VerifyScreen(onBack: () -> Unit, onNext: (String) -> Unit) {
    var otp by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AuthLayout("Verify Code", "Enter the the code\nwe just sent you on your registered Email", onBack) {
        // Một ô nhập ẩn, vẽ thành 5 ô OTP
        BasicTextField(
            value = otp,
            onValueChange = { v -> otp = v.filter(Char::isDigit).take(ResetRequest.OTP_LENGTH); error = null },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            decorationBox = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    repeat(ResetRequest.OTP_LENGTH) { i ->
                        Box(
                            Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)).background(FieldBg)
                                .border(1.dp, if (i == otp.length) Blue else Color(0xFFE0E0E0), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Text(otp.getOrNull(i)?.toString() ?: "", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            },
        )
        ErrorText(error)
        PrimaryButton("Next") {
            if (otp.length == ResetRequest.OTP_LENGTH) onNext(otp) else error = "Vui lòng nhập đủ ${ResetRequest.OTP_LENGTH} số"
        }
    }
}

@Composable
fun ResetScreen(onBack: () -> Unit, onNext: (String) -> Unit) {
    var pass by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AuthLayout("Create new password", "Your new password must be different form previously used password", onBack) {
        Field(pass, { pass = it; error = null }, "Password", Icons.Outlined.Lock, password = true)
        Field(confirm, { confirm = it; error = null }, "Confirm Password", Icons.Outlined.Lock, password = true)
        ErrorText(error)
        PrimaryButton("Next") {
            error = when {
                pass.length < ResetRequest.MIN_PASSWORD -> "Mật khẩu tối thiểu ${ResetRequest.MIN_PASSWORD} ký tự"
                pass != confirm -> "Mật khẩu xác nhận không khớp"
                else -> null
            }
            if (error == null) onNext(pass)
        }
    }
}

@Composable
fun ConfirmScreen(req: ResetRequest, onBack: () -> Unit, onSubmit: () -> Unit) {
    AuthLayout("Confirm", "We are here to help you!", onBack) {
        Field(req.email, {}, "", Icons.Outlined.Person, readOnly = true)
        Field(req.otp, {}, "", Icons.Outlined.Email, readOnly = true)
        Field(req.password, {}, "", Icons.Outlined.Lock, password = true, readOnly = true)
        PrimaryButton("Summit", onSubmit)
    }
}
