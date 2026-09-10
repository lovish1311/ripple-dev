package app.ripple.mesh.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ripple.mesh.R
import app.ripple.mesh.ui.screens.AppearanceScreen
import app.ripple.mesh.ui.screens.BackupScreen
import app.ripple.mesh.ui.theme.AppTheme
import app.ripple.mesh.ui.screens.ChatScreen
import app.ripple.mesh.ui.screens.DiagnosticsScreen
import app.ripple.mesh.ui.screens.FieldTestScreen
import app.ripple.mesh.ui.screens.HomeScreen
import app.ripple.mesh.ui.screens.PairScreen
import app.ripple.mesh.ui.screens.PowerScreen
import app.ripple.mesh.ui.screens.SettingsScreen
import app.ripple.mesh.ui.screens.SosScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: MeshViewModel by viewModels()
    private val fieldTestVm: FieldTestViewModel by viewModels()
    private var launchIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchIntent = intent
        setContent { RippleTheme(vm) { RippleRoot(vm, fieldTestVm, launchIntent) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchIntent = intent
    }
}

fun requiredPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= 31) {
    buildList {
        add(Manifest.permission.BLUETOOTH_SCAN); add(Manifest.permission.BLUETOOTH_ADVERTISE); add(Manifest.permission.BLUETOOTH_CONNECT)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        add(Manifest.permission.ACCESS_COARSE_LOCATION) // opt-in GPS for SOS beacons
    }.toTypedArray()
} else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

@Composable
fun RippleRoot(vm: MeshViewModel, fieldTestVm: FieldTestViewModel, launchIntent: Intent?) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(requiredPermissions().all { ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED })
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        granted = result.filterKeys { it != Manifest.permission.POST_NOTIFICATIONS }.values.all { it }
    }

    if (!granted) {
        PermissionGate { launcher.launch(requiredPermissions()) }
        return
    }

    LaunchedEffect(Unit) { vm.bind() }

    val nav = rememberNavController()
    LaunchedEffect(launchIntent) {
        val route = launchIntent?.getStringExtra("route")
            ?: launchIntent?.getStringExtra("conversation")?.let { "chat/$it" }
        route?.let { nav.navigate(it) }
    }

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(vm, onOpenChat = { nav.navigate("chat/$it") }, onOpenSettings = { nav.navigate("settings") })
        }
        composable("chat/{conversation}", arguments = listOf(navArgument("conversation") { type = NavType.StringType })) {
            ChatScreen(vm, conversation = it.arguments!!.getString("conversation")!!, onBack = { nav.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(
                vm,
                onBack = { nav.popBackStack() },
                onOpenSos = { nav.navigate("sos") },
                onOpenPower = { nav.navigate("power") },
                onOpenDiagnostics = { nav.navigate("diagnostics") },
                onOpenFieldTest = { nav.navigate("fieldtest") },
                onOpenPair = { nav.navigate("pair") },
                onOpenBackup = { nav.navigate("backup") },
                onOpenAppearance = { nav.navigate("appearance") },
            )
        }
        composable("pair") { PairScreen(vm, onBack = { nav.popBackStack() }) }
        composable("backup") { BackupScreen(vm, onBack = { nav.popBackStack() }) }
        composable("appearance") { AppearanceScreen(vm, onBack = { nav.popBackStack() }) }
        composable("sos") { SosScreen(vm, onBack = { nav.popBackStack() }) }
        composable("power") { PowerScreen(vm, onBack = { nav.popBackStack() }) }
        composable("diagnostics") { DiagnosticsScreen(vm, onBack = { nav.popBackStack() }) }
        composable("fieldtest") { FieldTestScreen(mesh = vm, ft = fieldTestVm, onBack = { nav.popBackStack() }) }
    }
}

@Composable
private fun PermissionGate(onRequest: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.permission_rationale), textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 24.dp))
        Button(onClick = onRequest) { Text(stringResource(R.string.permission_grant)) }
    }
}

@Composable
fun RippleTheme(vm: MeshViewModel, content: @Composable () -> Unit) {
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val appTheme = AppTheme.fromId(themeMode)
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val dark = if (appTheme == AppTheme.SYSTEM) systemDark else appTheme.isDark

    val scheme = if (appTheme == AppTheme.SYSTEM || appTheme == AppTheme.LIGHT || appTheme == AppTheme.DARK) {
        if (dark) darkColorScheme(
            primary = Color(0xFF7FDBCA),
            secondary = Color(0xFF9CC5FF),
            tertiary = Color(0xFFFFB59D),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            surfaceVariant = Color(0xFF252525),
            surfaceContainerLow = Color(0xFF181818),
            surfaceContainer = Color(0xFF1E1E1E),
            surfaceContainerHigh = Color(0xFF282828),
            surfaceContainerHighest = Color(0xFF333333),
            onBackground = Color(0xFFF8FAFC),
            onSurface = Color(0xFFF8FAFC),
            onSurfaceVariant = Color(0xFF94A3B8)
        )
        else lightColorScheme(
            primary = Color(0xFF006B5E),
            secondary = Color(0xFF3B5E8C),
            tertiary = Color(0xFF9C4325),
            background = Color(0xFFF8F9FA),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFF1F3F5),
            surfaceContainerLow = Color(0xFFF8F9FA),
            surfaceContainer = Color(0xFFF1F3F5),
            surfaceContainerHigh = Color(0xFFE9ECEF),
            surfaceContainerHighest = Color(0xFFDEE2E6),
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A),
            onSurfaceVariant = Color(0xFF475569)
        )
    } else {
        if (dark) darkColorScheme(
            primary = appTheme.primaryColor,
            onPrimary = Color.White,
            primaryContainer = appTheme.primaryColor,
            onPrimaryContainer = Color.White,
            secondary = appTheme.secondaryColor,
            onSecondary = Color.White,
            secondaryContainer = appTheme.secondaryColor,
            onSecondaryContainer = Color.White,
            background = appTheme.backgroundColor,
            surface = appTheme.surfaceColor,
            surfaceVariant = appTheme.cardColor,
            surfaceContainerLow = appTheme.backgroundColor,
            surfaceContainer = appTheme.surfaceColor,
            surfaceContainerHigh = appTheme.cardColor,
            surfaceContainerHighest = appTheme.cardColor,
            onBackground = appTheme.textColor,
            onSurface = appTheme.textColor,
            onSurfaceVariant = Color(0xFF94A3B8)
        )
        else lightColorScheme(
            primary = appTheme.primaryColor,
            onPrimary = Color.White,
            primaryContainer = appTheme.primaryColor,
            onPrimaryContainer = Color.White,
            secondary = appTheme.secondaryColor,
            onSecondary = Color.White,
            secondaryContainer = appTheme.secondaryColor,
            onSecondaryContainer = Color.White,
            background = appTheme.backgroundColor,
            surface = appTheme.surfaceColor,
            surfaceVariant = appTheme.cardColor,
            surfaceContainerLow = appTheme.backgroundColor,
            surfaceContainer = appTheme.surfaceColor,
            surfaceContainerHigh = appTheme.cardColor,
            surfaceContainerHighest = appTheme.cardColor,
            onBackground = appTheme.textColor,
            onSurface = appTheme.textColor,
            onSurfaceVariant = Color(0xFF475569)
        )
    }

    MaterialTheme(colorScheme = scheme, content = content)
}
