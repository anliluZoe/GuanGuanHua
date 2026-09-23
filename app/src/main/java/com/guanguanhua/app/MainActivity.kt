package com.guanguanhua.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.guanguanhua.app.notify.CycleReminder
import com.guanguanhua.app.notify.ReviewActivityWorker
import com.guanguanhua.app.ui.ExpensesScreen
import com.guanguanhua.app.ui.CycleScreen
import com.guanguanhua.app.ui.LoadingScrim
import com.guanguanhua.app.ui.NewRequestScreen
import com.guanguanhua.app.ui.ProfileEditScreen
import com.guanguanhua.app.ui.ProfileScreen
import com.guanguanhua.app.ui.RequestDetailScreen
import com.guanguanhua.app.ui.RequestListScreen
import com.guanguanhua.app.ui.WidgetScreen
import com.guanguanhua.app.ui.theme.QTheme
import com.guanguanhua.app.ui.theme.GuanGuanHuaTheme
import com.guanguanhua.app.update.AppUpdates
import com.guanguanhua.app.update.UpdateCheckResult
import com.guanguanhua.app.widget.WidgetRefreshWorker
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

private const val FOREGROUND_POLL_MS = 30_000L

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("requests", "申请", Icons.AutoMirrored.Outlined.Assignment),
    Tab("expenses", "账本", Icons.AutoMirrored.Outlined.MenuBook),
    Tab("cycle", "周期", Icons.Outlined.CalendarMonth),
    Tab("profile", "我们", Icons.Outlined.People),
)

class MainActivity : ComponentActivity() {

    /** 从通知点进来要打开的申请 id；0 表示没有。 */
    private val openRequestId = mutableLongStateOf(0L)

    /** 从桌面组件点进来，打开「我们 → 桌面组件」。 */
    private val openWidget = mutableStateOf(false)

    /** 从经期提醒点进来，打开周期页。 */
    private val openCycle = mutableStateOf(false)

    private val requestNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            ReviewActivityWorker.enqueueSoon(this)
            CycleReminder.scheduleFromCache(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openRequestId.longValue = intent.getLongExtra(EXTRA_REQUEST_ID, 0L)
        openWidget.value = wantsWidget(intent)
        openCycle.value = wantsCycle(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        openRequestId.longValue = intent.getLongExtra(EXTRA_REQUEST_ID, 0L)
        openWidget.value = wantsWidget(intent)
        openCycle.value = wantsCycle(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val viewModel: AppViewModel = viewModel()
            val appearance by viewModel.appearance.collectAsStateWithLifecycle()
            GuanGuanHuaTheme(darkTheme = appearance.isDark(isSystemInDarkTheme())) {
                val navController = rememberNavController()
                val session by viewModel.session.collectAsStateWithLifecycle()
                val status by viewModel.statusMessage.collectAsStateWithLifecycle()
                val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                val showBottomBar = TABS.any { it.route == currentRoute }
                val snackbar = remember { SnackbarHostState() }

                BackHandler(enabled = isBusy) { }

                Box(modifier = Modifier.fillMaxSize()) {
                    val lifecycleOwner = LocalLifecycleOwner.current
                    LaunchedEffect(lifecycleOwner, session.joined) {
                        if (!session.joined) return@LaunchedEffect
                        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                            while (true) {
                                delay(FOREGROUND_POLL_MS)
                                viewModel.refresh(quiet = true)
                            }
                        }
                    }

                    LaunchedEffect(status) {
                        if (!status.isNullOrBlank()) {
                            snackbar.showSnackbar(status!!)
                            viewModel.consumeStatus()
                        }
                    }

                    LaunchedEffect(Unit) {
                        val result = AppUpdates.maybeCheckDaily(this@MainActivity)
                        if (result is UpdateCheckResult.Available) {
                            snackbar.showSnackbar(
                                message = "发现新版本 ${result.update.versionName}，去「我们」页更新",
                                duration = SnackbarDuration.Long,
                            )
                        }
                    }

                    val pendingRequestId = openRequestId.longValue
                    LaunchedEffect(pendingRequestId, session.joined) {
                        if (session.joined && pendingRequestId != 0L) {
                            navController.navigate("requests/$pendingRequestId") { launchSingleTop = true }
                            openRequestId.longValue = 0L
                        }
                    }

                    val pendingWidget = openWidget.value
                    LaunchedEffect(pendingWidget, session.joined) {
                        if (!pendingWidget) return@LaunchedEffect
                        navController.navigate("profile") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        if (session.joined) {
                            navController.navigate("profile/widget") { launchSingleTop = true }
                        }
                        openWidget.value = false
                    }

                    val pendingCycle = openCycle.value
                    LaunchedEffect(pendingCycle) {
                        if (!pendingCycle) return@LaunchedEffect
                        navController.navigate("cycle") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        openCycle.value = false
                    }

                    Scaffold(
                        containerColor = QTheme.colors.canvas,
                        snackbarHost = { SnackbarHost(snackbar) },
                        bottomBar = {
                            if (showBottomBar) {
                                val q = QTheme.colors
                                Row(
                                    modifier = Modifier
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .clip(RoundedCornerShape(28.dp))
                                        .background(q.paper)
                                        .border(1.dp, q.lineStrong, RoundedCornerShape(28.dp))
                                        .padding(horizontal = 6.dp, vertical = 6.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    TABS.forEach { tab ->
                                        val selected = currentRoute == tab.route
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(22.dp))
                                                .background(if (selected) q.skySoft else Color.Transparent)
                                                .clickable {
                                                    navController.navigate(tab.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                                .padding(horizontal = 2.dp, vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Icon(
                                                tab.icon,
                                                contentDescription = tab.label,
                                                tint = if (selected) q.sky else q.muted,
                                                modifier = Modifier.size(20.dp),
                                            )
                                            Text(
                                                tab.label,
                                                color = if (selected) q.sky else q.muted,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontSize = 11.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        },
                    ) { padding ->
                        NavHost(
                            navController = navController,
                            startDestination = "requests",
                            modifier = Modifier.padding(padding),
                        ) {
                            composable("requests") {
                                RequestListScreen(
                                    viewModel = viewModel,
                                    onCreate = { navController.navigate("requests/new") },
                                    onOpen = { id -> navController.navigate("requests/$id") },
                                )
                            }
                            composable("requests/new") {
                                NewRequestScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                            }
                            composable(
                                route = "requests/{id}",
                                arguments = listOf(navArgument("id") { type = NavType.LongType }),
                            ) { entry ->
                                RequestDetailScreen(
                                    viewModel = viewModel,
                                    requestId = entry.arguments?.getLong("id") ?: 0L,
                                    onBack = { navController.popBackStack() },
                                )
                            }
                            composable("expenses") { ExpensesScreen(viewModel = viewModel) }
                            composable("cycle") { CycleScreen(viewModel = viewModel) }
                            composable("profile") {
                                ProfileScreen(
                                    viewModel = viewModel,
                                    onOpenWidget = { navController.navigate("profile/widget") },
                                    onOpenEdit = { navController.navigate("profile/edit") },
                                )
                            }
                            composable("profile/edit") {
                                ProfileEditScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                            }
                            composable("profile/widget") {
                                WidgetScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                            }
                        }
                    }
                    LoadingScrim(visible = isBusy)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as GuanGuanHuaApp).inForeground = true
    }

    override fun onPause() {
        (application as GuanGuanHuaApp).inForeground = false
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            ReviewActivityWorker.enqueueSoon(this, delay = 20, unit = TimeUnit.SECONDS)
            WidgetRefreshWorker.enqueueSoon(this, delay = 20, unit = TimeUnit.SECONDS)
        }
    }

    companion object {
        const val EXTRA_REQUEST_ID = "requestId"
        const val EXTRA_OPEN_WIDGET = "openWidget"
        const val EXTRA_OPEN_CYCLE = "openCycle"
        const val ACTION_OPEN_WIDGET = "com.guanguanhua.app.OPEN_WIDGET"

        fun wantsWidget(intent: Intent?): Boolean =
            intent?.getBooleanExtra(EXTRA_OPEN_WIDGET, false) == true || intent?.action == ACTION_OPEN_WIDGET

        fun wantsCycle(intent: Intent?): Boolean = intent?.getBooleanExtra(EXTRA_OPEN_CYCLE, false) == true
    }
}
