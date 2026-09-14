package com.savemoney.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.savemoney.app.ui.theme.SaveMoneyTheme

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("requests", "申请", Icons.AutoMirrored.Filled.ListAlt),
    Tab("expenses", "消费", Icons.Default.PieChart),
    Tab("profile", "我的", Icons.Default.Person),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SaveMoneyTheme {
                val navController = rememberNavController()
                val viewModel: AppViewModel = viewModel()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                val showBottomBar = TABS.any { it.route == currentRoute }

                Surface {
                    Scaffold(
                        bottomBar = {
                            if (showBottomBar) {
                                NavigationBar {
                                    TABS.forEach { tab ->
                                        NavigationBarItem(
                                            selected = currentRoute == tab.route,
                                            onClick = {
                                                navController.navigate(tab.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                                            label = { Text(tab.label) },
                                        )
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
    }
}
