package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.presentation.screens.*
import com.example.presentation.viewmodel.WalletViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: WalletViewModel = viewModel()
            val theme by viewModel.appTheme.collectAsState()
            val isAppLocked by viewModel.isAppLocked.collectAsState()

            val isDark = when (theme) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isAppLocked) {
                            SecurityLockScreen(
                                viewModel = viewModel,
                                onSuccess = {
                                    viewModel.isAppLocked.value = false
                                }
                            )
                        } else {
                            MainContentLayout(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainContentLayout(viewModel: WalletViewModel) {
    var currentScreen by remember { mutableStateOf("MAIN") } // MAIN, ACCOUNTS
    var selectedTab by remember { mutableStateOf(0) } // 0=Dashboard, 1=History, 2=Analytics, 3=Calendar, 4=Settings

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val userName by viewModel.userNameState.collectAsState()
    val userAvatarPath by viewModel.userAvatarPathState.collectAsState()

    if (currentScreen == "ACCOUNTS") {
        AccountsScreen(
            viewModel = viewModel,
            onNavigateBack = { currentScreen = "MAIN" }
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    val initials = remember(userName) {
                        val parts = userName.trim().split("\\s+".toRegex())
                        if (parts.size >= 2) {
                            "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}".uppercase()
                        } else {
                            (userName.take(2)).uppercase()
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    when (userAvatarPath) {
                                        "Indigo" -> Color(0xFF6366F1)
                                        "Teal" -> Color(0xFF14B8A6)
                                        "Orange" -> Color(0xFFF97316)
                                        "Rose" -> Color(0xFFF43F5E)
                                        "Purple" -> Color(0xFFA855F7)
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    }
                                ),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            val avatar = userAvatarPath ?: "Default"
                            if (avatar.startsWith("emoji:")) {
                                Text(avatar.removePrefix("emoji:"), fontSize = 28.sp)
                            } else {
                                Text(
                                    text = initials,
                                    fontWeight = FontWeight.Bold,
                                    color = if (avatar != "Default") Color.White else MaterialTheme.colorScheme.primary,
                                    fontSize = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Vault Manager", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Drawer navigation items
                    listOf(
                        Triple("Home", Icons.Default.Dashboard, "nav_dashboard"),
                        Triple("Records", Icons.Default.List, "nav_ledger"),
                        Triple("Insight", Icons.Default.PieChart, "nav_analytics"),
                        Triple("Schedule", Icons.Default.CalendarToday, "nav_calendar"),
                        Triple("Settings", Icons.Default.Settings, "nav_settings")
                    ).forEachIndexed { index, (label, icon, tag) ->
                        val isSelected = selectedTab == index
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 15.sp
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                selectedTab = index
                                scope.launch { drawerState.close() }
                            },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                unselectedContainerColor = Color.Transparent,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .testTag(tag)
                        )
                    }
                }
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize()
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (selectedTab) {
                        0 -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToAccounts = { currentScreen = "ACCOUNTS" },
                            onNavigateToHistory = { selectedTab = 1 },
                            onMenuClick = { scope.launch { drawerState.open() } }
                        )
                        1 -> TransactionsScreen(
                            viewModel = viewModel,
                            onMenuClick = { scope.launch { drawerState.open() } }
                        )
                        2 -> AnalyticsScreen(
                            viewModel = viewModel,
                            onMenuClick = { scope.launch { drawerState.open() } }
                        )
                        3 -> CalendarScreen(
                            viewModel = viewModel,
                            onMenuClick = { scope.launch { drawerState.open() } }
                        )
                        4 -> SettingsScreen(
                            viewModel = viewModel,
                            onMenuClick = { scope.launch { drawerState.open() } }
                        )
                    }
                }
            }
        }
    }
}
