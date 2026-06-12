package com.example.presentation.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.*
import com.example.presentation.viewmodel.WalletViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: WalletViewModel,
    onNavigateToAccounts: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onMenuClick: () -> Unit
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val userName by viewModel.userNameState.collectAsState()
    val userAvatarPath by viewModel.userAvatarPathState.collectAsState()

    // Dialog triggering states
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showQuickTransferDialog by remember { mutableStateOf(false) }
    var showBudgetControlDialog by remember { mutableStateOf(false) }
    var showGoalControlDialog by remember { mutableStateOf(false) }
    var editingTxFromRecent by remember { mutableStateOf<TransactionWithDetails?>(null) }

    // Aggregate monthly statistics
    val (totalBalance, monthlyIncome, monthlyExpense) = remember(accounts, transactions) {
        val totalBal = accounts.sumOf { it.balance }
        
        val cal = Calendar.getInstance()
        val curMonth = cal.get(Calendar.MONTH)
        val curYear = cal.get(Calendar.YEAR)
        
        var inc = 0.0
        var exp = 0.0

        transactions.forEach { tx ->
            cal.timeInMillis = tx.transaction.date
            if (cal.get(Calendar.MONTH) == curMonth && cal.get(Calendar.YEAR) == curYear) {
                if (tx.transaction.type == "INCOME") {
                    inc += tx.transaction.amount
                } else if (tx.transaction.type == "EXPENSE") {
                    exp += tx.transaction.amount
                }
            }
        }
        Triple(totalBal, inc, exp)
    }

    val savings = monthlyIncome - monthlyExpense

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.testTag("nav_menu_trigger")
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
                    }
                },
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        val initials = remember(userName) {
                            val parts = userName.trim().split("\\s+".toRegex())
                            if (parts.size >= 2) {
                                "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}".uppercase()
                            } else {
                                (userName.take(2)).uppercase()
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(40.dp)
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
                            contentAlignment = Alignment.Center
                        ) {
                            val avatar = userAvatarPath ?: "Default"
                            if (avatar.startsWith("emoji:")) {
                                Text(avatar.removePrefix("emoji:"), fontSize = 20.sp)
                            } else {
                                Text(
                                    text = initials,
                                    fontWeight = FontWeight.Bold,
                                    color = if (avatar != "Default") Color.White else MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("HELLO,", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            Text(userName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- HEADER SUMMARY BALANCES CARD (Sleek Fintech Card with Overlay shapes) ---
            Card(
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "TOTAL BALANCE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.weight(1f)
                            )
                            val savingsColor = if (savings >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.18f), shape = RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(
                                        imageVector = if (savings >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Saved: $currencySymbol${String.format(Locale.US, "%,.0f", savings)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%,.2f", totalBalance)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier
                                .testTag("dashboard_total_balance")
                                .padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Monthly stats sub-row in glassmorphic layout style
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Income Box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(16.dp))
                                    .border(width = 1.dp, color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text("INCOME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                                    Text("+$currencySymbol${String.format(Locale.US, "%,.0f", monthlyIncome)}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                            
                            // Expense Box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(16.dp))
                                    .border(width = 1.dp, color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text("EXPENSES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                                    Text("-$currencySymbol${String.format(Locale.US, "%,.0f", monthlyExpense)}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // --- QUICK ACTION CHIPS PANELS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardActionItem(
                    label = "Quick Add",
                    icon = Icons.Default.Add,
                    tag = "quick_add_btn",
                    modifier = Modifier.weight(1f),
                    onClick = { showQuickAddDialog = true }
                )
                DashboardActionItem(
                    label = "Bank Transfer",
                    icon = Icons.Default.SwapHoriz,
                    tag = "quick_transfer_btn",
                    modifier = Modifier.weight(1f),
                    onClick = { showQuickTransferDialog = true }
                )
                DashboardActionItem(
                    label = "Accounts",
                    icon = Icons.Default.AccountBalance,
                    tag = "navigate_accounts_btn",
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAccounts
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardActionItem(
                    label = "Setup Limits",
                    icon = Icons.Default.NotificationsActive,
                    tag = "manage_budgets_btn",
                    modifier = Modifier.weight(1f),
                    onClick = { showBudgetControlDialog = true }
                )
                DashboardActionItem(
                    label = "Savings Goals",
                    icon = Icons.Default.Stars,
                    tag = "manage_goals_btn",
                    modifier = Modifier.weight(1f),
                    onClick = { showGoalControlDialog = true }
                )
            }

            // --- BUDGET PROGRESS SECTION BAR INDICATORS ---
            if (budgets.isNotEmpty()) {
                Text("Limits & Budget Warnings", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        budgets.forEach { budget ->
                            val cat = categories.find { it.id == budget.categoryId }
                            val catName = cat?.name ?: "Uncategorized"
                            val color = Color(cat?.color ?: 0xFF95A5A6.toInt())

                            // Sum category spending in current month
                            val spent = remember(transactions, budget) {
                                val cal = Calendar.getInstance()
                                val m = cal.get(Calendar.MONTH) + 1
                                val y = cal.get(Calendar.YEAR)
                                transactions.filter {
                                    cal.timeInMillis = it.transaction.date
                                    it.transaction.categoryId == budget.categoryId &&
                                            it.transaction.type == "EXPENSE" &&
                                            (cal.get(Calendar.MONTH) + 1) == budget.month &&
                                            cal.get(Calendar.YEAR) == budget.year
                                }.sumOf { it.transaction.amount }
                            }

                            val ratio = (spent / budget.limitAmount).toFloat().coerceIn(0f..1f)
                            val isExceeded = spent > budget.limitAmount

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(catName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "$currencySymbol${String.format(Locale.US, "%,.0f", spent)} / ${budget.limitAmount.toInt()}",
                                        fontSize = 13.sp,
                                        color = if (isExceeded) Color(0xFFE74C3C) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isExceeded) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    color = if (isExceeded) Color(0xFFE74C3C) else color,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                                )
                                if (isExceeded) {
                                    Text(
                                        text = "⚠️ OVER-LIMIT WARNING!",
                                        color = Color(0xFFE74C3C),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- SAVINGS GOALS DYNAMIC VIEW CARDS ___
            if (goals.isNotEmpty()) {
                Text("Your Active Savings Goals", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    goals.forEach { goal ->
                        val ratio = (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f..1f)
                        val pct = ratio * 100f
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Circular goal progress tracker icon
                                Box(
                                    modifier = Modifier.size(50.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        progress = { ratio },
                                        color = Color(0xFF1ABC9C),
                                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                        strokeWidth = 4.dp
                                    )
                                    Text("${pct.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(goal.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        text = "$currencySymbol${String.format(Locale.US, "%,.0f", goal.currentAmount)} saved of $currencySymbol${String.format(Locale.US, "%,.0f", goal.targetAmount)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Quick increment contribution button trigger
                                OutlinedIconButton(
                                    onClick = {
                                        viewModel.addGoalContribution(goal.id, 50.0)
                                        Toast.makeText(context, "${currencySymbol}50 contributed to ${goal.name}!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.VolunteerActivism, contentDescription = "Add funds", tint = Color(0xFF1ABC9C))
                                }
                            }
                        }
                    }
                }
            }

            // --- RECENT TRANSACTIONS RECORDS CHIPS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Records", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = onNavigateToHistory) {
                    Text("See All")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Arrow right link", modifier = Modifier.size(16.dp))
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 80.dp)
            ) {
                val recent = transactions.take(5)
                if (recent.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No records. Tap '+' to begin.", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    recent.forEach { item ->
                        RecordItemRow(tx = item, sym = currencySymbol, onClick = { editingTxFromRecent = item })
                    }
                }
            }
        }
    }

    // --- DIALOG MODALS OVERLAYS ---

    // Quick Add Expense Dialog Form
    if (showQuickAddDialog) {
        TransactionFormDialog(
            title = "Log Quick Expense",
            accounts = accounts,
            categories = categories,
            onDismiss = { showQuickAddDialog = false },
            onSave = { type, amt, accId, toAccId, catId, date, note, tags ->
                viewModel.addTransaction(amt, type, accId, toAccId, catId, date, note, tags)
                showQuickAddDialog = false
                Toast.makeText(context, "Logged successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Quick Transfer Dialog Form
    if (showQuickTransferDialog) {
        TransactionFormDialog(
            title = "Log Internal Transfer",
            accounts = accounts,
            categories = categories,
            initialTx = Transaction(0, 0.0, "TRANSFER", accounts.firstOrNull()?.id ?: 0, null, null, System.currentTimeMillis()),
            onDismiss = { showQuickTransferDialog = false },
            onSave = { type, amt, accId, toAccId, catId, date, note, tags ->
                viewModel.addTransaction(amt, "TRANSFER", accId, toAccId, null, date, note, tags)
                showQuickTransferDialog = false
                Toast.makeText(context, "Accounts updated recursively!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Recent item edit dialog
    editingTxFromRecent?.let { orig ->
        TransactionFormDialog(
            title = "Modify Transaction",
            accounts = accounts,
            categories = categories,
            initialTx = orig.transaction,
            onDismiss = { editingTxFromRecent = null },
            onSave = { type, amt, accId, toAccId, catId, date, note, tags ->
                viewModel.editTransaction(orig.id, amt, type, accId, toAccId, catId, date, note, tags)
                editingTxFromRecent = null
                Toast.makeText(context, "Log updated!", Toast.LENGTH_SHORT).show()
            },
            onDelete = {
                viewModel.deleteTransaction(orig.id)
                editingTxFromRecent = null
                Toast.makeText(context, "Cleared from DB.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Budget Limits control dialog list manager
    if (showBudgetControlDialog) {
        var budgetAmountStr by remember { mutableStateOf("") }
        var selectedCatId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 0) }
        var dropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showBudgetControlDialog = false },
            title = { Text("Category Limits Controller", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Define monthly limit controls for individual divisions.", fontSize = 12.sp)

                    Box(modifier = Modifier.fillMaxWidth()) {
                        val selName = categories.find { it.id == selectedCatId }?.name ?: "No categories"
                        OutlinedTextField(
                            value = selName,
                            onValueChange = {},
                            label = { Text("Limit Category") },
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Expand") },
                            modifier = Modifier.fillMaxWidth().clickable { dropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        selectedCatId = cat.id
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = budgetAmountStr,
                        onValueChange = { budgetAmountStr = it },
                        label = { Text("Max Budget Limit Amount") },
                        placeholder = { Text("500.00") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("budget_limit_input")
                    )

                    // Display active limits
                    Text("Active Budgets", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Box(modifier = Modifier.height(100.dp)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(budgets) { b ->
                                val cName = categories.find { it.id == b.categoryId }?.name ?: "Cat #${b.categoryId}"
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("$cName Max Limit: $currencySymbol${b.limitAmount.toInt()}", fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = { viewModel.deleteBudget(b) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limitAmnt = budgetAmountStr.toDoubleOrNull()
                        if (limitAmnt == null || limitAmnt <= 0.0) {
                            Toast.makeText(context, "Fill a valid limit", Toast.LENGTH_SHORT).show()
                        } else if (selectedCatId == 0) {
                            Toast.makeText(context, "Please configure/add category first", Toast.LENGTH_SHORT).show()
                        } else {
                            val cal = Calendar.getInstance()
                            viewModel.addBudget(selectedCatId, limitAmnt, cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
                            budgetAmountStr = ""
                            Toast.makeText(context, "Category budget locked!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save Limit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetControlDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Savings Goals manager list dialog
    if (showGoalControlDialog) {
        var goalName by remember { mutableStateOf("") }
        var goalTargetStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showGoalControlDialog = false },
            title = { Text("Savings Goal Tracker", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Car, vacation, investment - plan specific offline goals safely.", fontSize = 12.sp)

                    OutlinedTextField(
                        value = goalName,
                        onValueChange = { goalName = it },
                        label = { Text("Goal Name") },
                        placeholder = { Text("New Tesla Model S, Summer trip") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("goal_name_input")
                    )

                    OutlinedTextField(
                        value = goalTargetStr,
                        onValueChange = { goalTargetStr = it },
                        label = { Text("Target Sinking Fund Amount") },
                        placeholder = { Text("25000.00") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("goal_target_input")
                    )

                    Text("Active Sinking Goals", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Box(modifier = Modifier.height(100.dp)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(goals) { g ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("${g.name} Target: $currencySymbol${g.targetAmount.toInt()}", fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = { viewModel.deleteGoal(g) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tar = goalTargetStr.toDoubleOrNull()
                        if (goalName.trim().isEmpty() || tar == null || tar <= 0.0) {
                            Toast.makeText(context, "Fill valid credentials", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addGoal(goalName, tar, System.currentTimeMillis() + 31536000000L, 0.0) // +1 year
                            goalName = ""
                            goalTargetStr = ""
                            Toast.makeText(context, "New target designated!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Establish Goal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalControlDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun DashboardStatItem(
    label: String,
    value: String,
    color: Color,
    icon: ImageVector
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(14.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
fun DashboardActionItem(
    label: String,
    icon: ImageVector,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline),
        modifier = modifier
            .testTag(tag)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
