package com.savemoney.app

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
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
import com.savemoney.app.notify.ReviewActivityWorker
import com.savemoney.app.ui.ExpensesScreen
import com.savemoney.app.ui.LoadingScrim
import com.savemoney.app.ui.NewRequestScreen
import com.savemoney.app.ui.ProfileScreen
import com.savemoney.app.ui.RequestDetailScreen
import com.savemoney.app.ui.RequestListScreen
import com.savemoney.app.ui.SetupScreen
import com.savemoney.app.ui.theme.QTheme
import com.savemoney.app.ui.theme.SaveMoneyTheme
import com.savemoney.app.update.AppUpdates
import com.savemoney.app.update.UpdateCheckResult
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

private const val FOREGROUND_POLL_MS = 30_000L

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("requests", "申请", Icons.AutoMirrored.Outlined.Assignment),
    Tab("expenses", "账本", Icons.AutoMirrored.Outlined.MenuBook),
    Tab("profile", "我们", Icons.Outlined.People),
)

class MainActivity : ComponentActivity() {

    /** 从通知点进来要打开的申请 id；0 表示没有。 */
    private val openRequestId = mutableLongStateOf(0L)

    private val requestNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) ReviewActivityWorker.enqueueSoon(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openRequestId.longValue = intent.getLongExtra(EXTRA_REQUEST_ID, 0L)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        openRequestId.longValue = intent.getLongExtra(EXTRA_REQUEST_ID, 0L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val viewModel: AppViewModel = viewModel()
            val appearance by viewModel.appearance.collectAsStateWithLifecycle()
            SaveMoneyTheme(darkTheme = appearance.isDark(isSystemInDarkTheme())) {
                val navController = rememberNavController()
                val session by viewModel.session.collectAsStateWithLifecycle()
                val status by viewModel.statusMessage.collectAsStateWithLifecycle()
                val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                val showBottomBar = session.joined && TABS.any { it.route == currentRoute }
                val snackbar = remember { SnackbarHostState() }

                BackHandler(enabled = isBusy) { }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (!session.joined) {
                        SetupScreen(viewModel)
                    } else {
                        val lifecycleOwner = LocalLifecycleOwner.current
                        LaunchedEffect(lifecycleOwner) {
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
                        LaunchedEffect(pendingRequestId) {
                            if (pendingRequestId != 0L) {
                                navController.navigate("requests/$pendingRequestId") { launchSingleTop = true }
                                openRequestId.longValue = 0L
                            }
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
                                            .padding(horizontal = 20.dp, vertical = 10.dp)
                                            .clip(RoundedCornerShape(28.dp))
                                            .background(q.paper)
                                            .border(1.dp, q.lineStrong, RoundedCornerShape(28.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                    ) {
                                        TABS.forEach { tab ->
                                            val selected = currentRoute == tab.route
                                            Column(
                                                modifier = Modifier
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
                                                    .padding(horizontal = 22.dp, vertical = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                            ) {
                                                Icon(
                                                    tab.icon,
                                                    contentDescription = tab.label,
                                                    tint = if (selected) q.sky else q.muted,
                                                    modifier = Modifier.size(22.dp),
                                                )
                                                Text(
                                                    tab.label,
                                                    color = if (selected) q.sky else q.muted,
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
                                composable("profile") { ProfileScreen(viewModel = viewModel) }
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
        (application as SaveMoneyApp).inForeground = true
    }

    override fun onPause() {
        (application as SaveMoneyApp).inForeground = false
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            ReviewActivityWorker.enqueueSoon(this, delay = 20, unit = TimeUnit.SECONDS)
        }
    }

    companion object {
        const val EXTRA_REQUEST_ID = "requestId"
    }
}
