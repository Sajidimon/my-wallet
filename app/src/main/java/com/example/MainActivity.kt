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
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.domain.model.Category
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
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    var showCategoryManagerDialog by remember { mutableStateOf(false) }

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
                            Text("Finance Manager", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Triple("Schedule", Icons.Default.CalendarToday, "nav_calendar")
                    ).forEachIndexed { targetIndex, (label, icon, tag) ->
                        val isSelected = selectedTab == targetIndex
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
                                selectedTab = targetIndex
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

                    // Categories Action Item
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = "Categories",
                                fontWeight = FontWeight.Normal,
                                fontSize = 15.sp
                            )
                        },
                        selected = false,
                        onClick = {
                            showCategoryManagerDialog = true
                            scope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = "Categories",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color.Transparent,
                            unselectedContainerColor = Color.Transparent,
                            selectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .testTag("nav_categories")
                    )

                    // Settings Drawer Item
                    val isSettingsSelected = selectedTab == 4
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = "Settings",
                                fontWeight = if (isSettingsSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp
                            )
                        },
                        selected = isSettingsSelected,
                        onClick = {
                            selectedTab = 4
                            scope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (isSettingsSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                            .testTag("nav_settings")
                    )
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

    // Category Manager Dialog in MainActivity
    if (showCategoryManagerDialog) {
        var editingCategory by remember { mutableStateOf<Category?>(null) }
        var newCatName by remember { mutableStateOf("") }
        var selectedCatColor by remember { mutableStateOf(0xFF3498DB) }
        var selectedCatIcon by remember { mutableStateOf("Category") }
        var selectedCategoryType by remember { mutableStateOf("EXPENSE") }

        AlertDialog(
            onDismissRequest = { showCategoryManagerDialog = false },
            title = {
                Text(
                    text = if (editingCategory == null) "Category Manager" else "Edit: ${editingCategory!!.name}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Customize category nodes for income and expense records.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_category_name_input")
                    )

                    // Type Selection Chips
                    Text("Category Type", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = selectedCategoryType == "EXPENSE",
                            onClick = { selectedCategoryType = "EXPENSE" },
                            label = { Text("Expense") },
                            leadingIcon = {
                                if (selectedCategoryType == "EXPENSE") {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                        FilterChip(
                            selected = selectedCategoryType == "INCOME",
                            onClick = { selectedCategoryType = "INCOME" },
                            label = { Text("Income") },
                            leadingIcon = {
                                if (selectedCategoryType == "INCOME") {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                    }

                    // Color selection list
                    Text("Select Color", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp)
                    ) {
                        val colorsList = listOf(
                            0xFFE74C3C, 0xFF2ECC71, 0xFF3498DB, 0xFFF1C40F, 0xFF9B59B6,
                            0xFFE67E22, 0xFF1ABC9C, 0xFF34495E, 0xFFE91E63, 0xFF7F8C8D
                        )
                        colorsList.forEach { colorVal ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorVal))
                                    .clickable { selectedCatColor = colorVal }
                                    .border(
                                        width = if (selectedCatColor == colorVal) 2.dp else 0.dp,
                                        color = if (selectedCatColor == colorVal) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedCatColor == colorVal) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Icon/Symbol Selection
                    Text("Select Icon", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp)
                    ) {
                        val iconsList = listOf(
                            "restaurant", "directions_car", "shopping_bag", "receipt_long",
                            "sports_esports", "medical_services", "school", "flight", "home",
                            "subscriptions", "payments", "redeem", "trending_up", "card_giftcard",
                            "more_horiz"
                        )
                        iconsList.forEach { iconName ->
                            val iconVector = getCategoryIconVector(iconName)
                            val isSelected = selectedCatIcon == iconName
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedCatIcon = iconName },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = iconName,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Form Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        if (editingCategory == null) {
                            Button(
                                onClick = {
                                    if (newCatName.trim().isEmpty()) {
                                        Toast.makeText(context, "Please enter a valid name", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.addCategory(newCatName.trim(), selectedCategoryType, selectedCatIcon, selectedCatColor.toInt())
                                        newCatName = ""
                                        Toast.makeText(context, "Category added successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Add Category")
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (newCatName.trim().isEmpty()) {
                                        Toast.makeText(context, "Please enter a valid name", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.updateCategoryDetails(
                                            Category(
                                                id = editingCategory!!.id,
                                                name = newCatName.trim(),
                                                type = selectedCategoryType,
                                                icon = selectedCatIcon,
                                                color = selectedCatColor.toInt()
                                            )
                                        )
                                        editingCategory = null
                                        newCatName = ""
                                        Toast.makeText(context, "Category updated!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text("Save Changes")
                            }
                            OutlinedButton(
                                onClick = {
                                    editingCategory = null
                                    newCatName = ""
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                        }
                    }

                    // Existing Categories Header and List
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Text("Existing Categories (Tap item to Edit)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    Box(modifier = Modifier.height(140.dp)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(categories) { cat ->
                                val iconVector = getCategoryIconVector(cat.icon)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(cat.color).copy(alpha = 0.08f))
                                        .clickable {
                                            editingCategory = cat
                                            newCatName = cat.name
                                            selectedCatColor = cat.color.toLong() and 0xFFFFFFFFL
                                            selectedCatIcon = cat.icon
                                            selectedCategoryType = cat.type
                                        }
                                        .padding(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(cat.color)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = iconVector,
                                            contentDescription = cat.name,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cat.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(cat.type, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(
                                        onClick = {
                                            editingCategory = cat
                                            newCatName = cat.name
                                            selectedCatColor = cat.color.toLong() and 0xFFFFFFFFL
                                            selectedCatIcon = cat.icon
                                            selectedCategoryType = cat.type
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), modifier = Modifier.size(15.dp))
                                    }
                                    Spacer(modifier = Modifier.width(2.dp))
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteCategory(cat)
                                            if (editingCategory?.id == cat.id) {
                                                editingCategory = null
                                                newCatName = ""
                                            }
                                            Toast.makeText(context, "Deleted ${cat.name}", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCategoryManagerDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
