package com.savemoney.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
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
import com.savemoney.app.ui.ExpensesScreen
import com.savemoney.app.ui.NewRequestScreen
import com.savemoney.app.ui.ProfileScreen
import com.savemoney.app.ui.RequestDetailScreen
import com.savemoney.app.ui.RequestListScreen
import com.savemoney.app.ui.SetupScreen
import com.savemoney.app.ui.theme.Cute
import com.savemoney.app.ui.theme.SaveMoneyTheme
import kotlinx.coroutines.delay

private const val FOREGROUND_POLL_MS = 30_000L

private data class Tab(val route: String, val label: String, val emoji: String)

private val TABS = listOf(
    Tab("requests", "申请", "📝"),
    Tab("expenses", "账本", "🐷"),
    Tab("profile", "我们", "💛"),
)

class MainActivity : ComponentActivity() {

    /** 从通知点进来要打开的申请 id；0 表示没有。 */
    private val openRequestId = mutableLongStateOf(0L)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openRequestId.longValue = intent.getLongExtra(EXTRA_REQUEST_ID, 0L)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        openRequestId.longValue = intent.getLongExtra(EXTRA_REQUEST_ID, 0L)
        setContent {
            SaveMoneyTheme {
                val navController = rememberNavController()
                val viewModel: AppViewModel = viewModel()
                val session by viewModel.session.collectAsStateWithLifecycle()
                val status by viewModel.statusMessage.collectAsStateWithLifecycle()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                val showBottomBar = session.joined && TABS.any { it.route == currentRoute }
                val snackbar = remember { SnackbarHostState() }

                if (!session.joined) {
                    SetupScreen(viewModel)
                    return@SaveMoneyTheme
                }

                val requestNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                LaunchedEffect(lifecycleOwner) {
                    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        while (true) {
                            delay(FOREGROUND_POLL_MS)
                            viewModel.refresh()
                        }
                    }
                }

                LaunchedEffect(status) {
                    if (!status.isNullOrBlank()) {
                        snackbar.showSnackbar(status!!)
                        viewModel.consumeStatus()
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
                    containerColor = Cute.Cream,
                    snackbarHost = { SnackbarHost(snackbar) },
                    bottomBar = {
                        if (showBottomBar) {
                            Row(
                                modifier = Modifier
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0x14000000), RoundedCornerShape(32.dp))
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                TABS.forEach { tab ->
                                    val selected = currentRoute == tab.route
                                    Column(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(if (selected) Cute.PeachSoft else Color.Transparent)
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
                                        Text(tab.emoji)
                                        Text(
                                            tab.label,
                                            color = if (selected) Color(0xFF5A2A12) else Cute.Muted,
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
        }
    }

    companion object {
        const val EXTRA_REQUEST_ID = "requestId"
    }
}
