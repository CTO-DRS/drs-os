/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS Security Center — main activity and navigation. Every screen
 * shown here is implemented; there are no dead routes and no
 * placeholder destinations.
 */
package com.drs.os.securitycenter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.drs.os.securitycenter.platform.AndroidCheckProvider
import com.drs.os.securitycenter.ui.ChecklistViewModel
import com.drs.os.securitycenter.ui.theme.DrsTheme
import com.drs.os.securitycenter.ui.screens.ChecklistScreen
import com.drs.os.securitycenter.ui.screens.DashboardScreen
import com.drs.os.securitycenter.ui.screens.PrivacyScreen
import com.drs.os.securitycenter.ui.screens.StatusScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DrsTheme {
                SecurityCenterApp()
            }
        }
    }
}

private data class Tab(val route: String, val labelRes: Int)

private val TABS = listOf(
    Tab("dashboard", R.string.nav_dashboard),
    Tab("checklist", R.string.nav_checklist),
    Tab("privacy", R.string.nav_privacy),
    Tab("status", R.string.nav_status)
)

@Composable
fun SecurityCenterApp() {
    val appContext = LocalContext.current.applicationContext
    val viewModel: ChecklistViewModel = viewModel {
        ChecklistViewModel { AndroidCheckProvider(appContext) }
    }
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: "dashboard"

    Scaffold(
        bottomBar = {
            NavigationBar {
                TABS.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo("dashboard") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(stringResource(tab.labelRes)) },
                        icon = { Icon(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_drs_launcher),
                            contentDescription = stringResource(R.string.a11y_state_icon)) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(padding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    onOpenChecklist = { navController.navigate("checklist") },
                    onOpenPrivacy = { navController.navigate("privacy") },
                    onOpenStatus = { navController.navigate("status") }
                )
            }
            composable("checklist") { ChecklistScreen(viewModel = viewModel) }
            composable("privacy") { PrivacyScreen() }
            composable("status") { StatusScreen() }
        }
    }
}
