package top.met6.cquptnet

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var settings: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsManager(this)

        setContent {
            CQUPTNetTheme {
                MainScreen(settings)
            }
        }
    }
}

@Composable
fun CQUPTNetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF64B5F6),
            secondary = Color(0xFF03DAC6),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(settings: SettingsManager) {
    var status by remember { mutableStateOf("未知") }
    var ipAddr by remember { mutableStateOf("0.0.0.0") }
    var isLoading by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val refreshStatus = {
        scope.launch {
            isLoading = true
            val sdk = CQUPTNetSDK(settings.studentId, settings.password, settings.isp)
            val loggedIn = withContext(Dispatchers.IO) { sdk.checkStatus() }
            status = if (loggedIn) "已登录" else "未登录"
            ipAddr = sdk.ipAddr.ifEmpty { "0.0.0.0" }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("重邮校园网", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A237E), Color(0xFF121212))
                    )
                )
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Status Card
                StatusCard(status, ipAddr, isLoading)

                Spacer(modifier = Modifier.height(48.dp))

                // Action Buttons
                ActionButton(
                    text = "登录",
                    icon = Icons.AutoMirrored.Filled.Login,
                    color = Color(0xFF4CAF50),
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val sdk = CQUPTNetSDK(settings.studentId, settings.password, settings.isp)
                            val res = withContext(Dispatchers.IO) { sdk.login() }
                            status = if (res.result == "1" || res.msg.contains("已登录")) "已登录" else "未登录"
                            ipAddr = sdk.ipAddr.ifEmpty { "0.0.0.0" }
                            isLoading = false
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ActionButton(
                    text = "登出",
                    icon = Icons.AutoMirrored.Filled.Logout,
                    color = Color(0xFFF44336),
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val sdk = CQUPTNetSDK(settings.studentId, settings.password, settings.isp)
                            val res = withContext(Dispatchers.IO) { sdk.logout() }
                            status = if (res.result == "1") "未登录" else "已登录"
                            ipAddr = sdk.ipAddr.ifEmpty { "0.0.0.0" }
                            isLoading = false
                        }
                    }
                )
            }

            // Bottom Refresh Button
            TextButton(
                onClick = { refreshStatus() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("刷新状态", fontSize = 14.sp)
            }
        }

        if (showSettings) {
            SettingsDialog(
                settings = settings,
                onDismiss = { showSettings = false },
                onSave = { refreshStatus() }
            )
        }
    }
}

@Composable
fun StatusCard(status: String, ip: String, isLoading: Boolean) {
    Card(
        modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(64.dp))
                } else {
                    Icon(
                        imageVector = if (status == "已登录") Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = if (status == "已登录") Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = status,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "当前 IP: $ip",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ActionButton(text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
fun SettingsDialog(settings: SettingsManager, onDismiss: () -> Unit, onSave: () -> Unit) {
    var studentId by remember { mutableStateOf(settings.studentId) }
    var password by remember { mutableStateOf(settings.password) }
    var isp by remember { mutableStateOf(settings.isp) }
    var expanded by remember { mutableStateOf(false) }

    val isps = listOf(
        "xyw" to "教师",
        "telecom" to "电信",
        "cmcc" to "移动",
        "unicom" to "联通"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("校园网账号") },
        text = {
            Column {
                OutlinedTextField(
                    value = studentId,
                    onValueChange = { studentId = it },
                    label = { Text("账号") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("运营商: ${isps.find { it.first == isp }?.second ?: "未知"}")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        isps.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    isp = key
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                settings.studentId = studentId
                settings.password = password
                settings.isp = isp
                onSave()
                onDismiss()
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
