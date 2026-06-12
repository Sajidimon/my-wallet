package com.example.presentation.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Account
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionWithDetails
import com.example.presentation.viewmodel.WalletViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: WalletViewModel,
    onMenuClick: () -> Unit
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()

    // Search and filter states
    val query by viewModel.searchQuery.collectAsState()
    val currentCatFilter by viewModel.filterCategoryId.collectAsState()
    val currentAccFilter by viewModel.filterAccountId.collectAsState()
    val currentTypeFilter by viewModel.filterType.collectAsState()
    val currentSort by viewModel.filterSortOrder.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTxForEdit by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var showCategoryCreator by remember { mutableStateOf(false) }

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
                title = { Text("Vault Records", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_tx_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Search Input Row
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search description, tag, account name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("records_search_field")
            )

            // Filtering Bar - Quick Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Filter Menu Button
                var isTypeDropdownExpanded by remember { mutableStateOf(false) }
                Box {
                    FilterChip(
                        selected = currentTypeFilter != "ALL",
                        onClick = { isTypeDropdownExpanded = true },
                        label = { Text("Type: $currentTypeFilter") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand") }
                    )
                    DropdownMenu(
                        expanded = isTypeDropdownExpanded,
                        onDismissRequest = { isTypeDropdownExpanded = false }
                    ) {
                        listOf("ALL", "INCOME", "EXPENSE", "TRANSFER").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    viewModel.filterType.value = type
                                    isTypeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Sort Order Filter dropdown
                var isSortDropdownExpanded by remember { mutableStateOf(false) }
                Box {
                    FilterChip(
                        selected = currentSort != "DATE_DESC",
                        onClick = { isSortDropdownExpanded = true },
                        label = { Text(if (currentSort == "DATE_ASC") "Oldest First" else "Newest First") },
                        trailingIcon = { Icon(Icons.Default.Sort, contentDescription = "Sort Icon") }
                    )
                    DropdownMenu(
                        expanded = isSortDropdownExpanded,
                        onDismissRequest = { isSortDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Newest (Default)") },
                            onClick = {
                                viewModel.filterSortOrder.value = "DATE_DESC"
                                isSortDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Oldest First") },
                            onClick = {
                                viewModel.filterSortOrder.value = "DATE_ASC"
                                isSortDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Highest Amount") },
                            onClick = {
                                viewModel.filterSortOrder.value = "AMOUNT_DESC"
                                isSortDropdownExpanded = false
                            }
                        )
                    }
                }

                // Custom Category section button
                TextButton(
                    onClick = { showCategoryCreator = true },
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(Icons.Default.Category, contentDescription = "Creator")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Categories", fontSize = 12.sp)
                }
            }

            // Records List view
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No matching transactions found.",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        RecordItemRow(
                            tx = tx,
                            sym = currencySymbol,
                            onClick = { selectedTxForEdit = tx }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    // Transactions Add Dialog
    if (showAddDialog) {
        TransactionFormDialog(
            title = "Log Transaction",
            accounts = accounts,
            categories = categories,
            onDismiss = { showAddDialog = false },
            onSave = { type, amt, accId, toAccId, catId, date, note, tags ->
                viewModel.addTransaction(amt, type, accId, toAccId, catId, date, note, tags)
                showAddDialog = false
                Toast.makeText(context, "Logged inside Offline Vault!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Transactions Edit Dialog
    selectedTxForEdit?.let { orig ->
        TransactionFormDialog(
            title = "Update Transaction Details",
            accounts = accounts,
            categories = categories,
            initialTx = orig.transaction,
            onDismiss = { selectedTxForEdit = null },
            onSave = { type, amt, accId, toAccId, catId, date, note, tags ->
                viewModel.editTransaction(
                    orig.id, amt, type, accId, toAccId, catId, date, note, tags
                )
                selectedTxForEdit = null
                Toast.makeText(context, "Log updated successfully!", Toast.LENGTH_SHORT).show()
            },
            onDelete = {
                viewModel.deleteTransaction(orig.id)
                selectedTxForEdit = null
                Toast.makeText(context, "Record deleted from secure storage", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Category Manager dialog
    if (showCategoryCreator) {
        var editingCategory by remember { mutableStateOf<Category?>(null) }
        var newCatName by remember { mutableStateOf("") }
        var selectedCatColor by remember { mutableStateOf(0xFF3498DB) }
        var selectedCatIcon by remember { mutableStateOf("Category") }
        var selectedCategoryType by remember { mutableStateOf("EXPENSE") }

        AlertDialog(
            onDismissRequest = { showCategoryCreator = false },
            title = {
                Text(
                    text = if (editingCategory == null) "Offline Category Manager" else "Edit: ${editingCategory!!.name}",
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
                                        Toast.makeText(context, "Category added!", Toast.LENGTH_SHORT).show()
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
                TextButton(onClick = { showCategoryCreator = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TransactionFormDialog(
    title: String,
    accounts: List<Account>,
    categories: List<Category>,
    initialTx: Transaction? = null,
    onDismiss: () -> Unit,
    onSave: (type: String, amount: Double, accountId: Int, toAccountId: Int?, categoryId: Int?, date: Long, note: String, tags: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(initialTx?.type ?: "EXPENSE") }
    var amountStr by remember { mutableStateOf(initialTx?.amount?.toString() ?: "") }
    var accountId by remember { mutableStateOf(initialTx?.accountId ?: accounts.firstOrNull()?.id ?: 0) }
    var toAccountId by remember { mutableStateOf(initialTx?.toAccountId ?: accounts.getOrNull(1)?.id) }
    var categoryId by remember { mutableStateOf(initialTx?.categoryId) }
    
    androidx.compose.runtime.LaunchedEffect(type) {
        if (initialTx != null && initialTx.type == type) {
            categoryId = initialTx.categoryId
        } else {
            categoryId = categories.find { it.type == type }?.id
        }
    }

    var note by remember { mutableStateOf(initialTx?.note ?: "") }
    var tags by remember { mutableStateOf(initialTx?.tags ?: "") }
    var dateTs by remember { mutableStateOf(initialTx?.date ?: System.currentTimeMillis()) }

    var accountExpanded by remember { mutableStateOf(false) }
    var toAccountExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val dateFormater = SimpleDateFormat("LLLL dd, yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type segmented switches
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("EXPENSE" to "Expense", "INCOME" to "Income", "TRANSFER" to "Transfer").forEach { (valType, label) ->
                        val active = type == valType
                        Button(
                            onClick = { type = valType },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                                .height(38.dp)
                        ) {
                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }

                // Amount Decimal form field input
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.toDoubleOrNull() != null || input.all { it.isDigit() || it == '.' }) {
                            amountStr = input
                        }
                    },
                    label = { Text("Transaction Amount") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tx_amount_field")
                )

                // Selected account field dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    val selAccName = accounts.find { it.id == accountId }?.name ?: "Select From Account"
                    OutlinedTextField(
                        value = selAccName,
                        onValueChange = {},
                        label = { Text(if (type == "TRANSFER") "Source Account" else "Payment Origin Account") },
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Expand link") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { accountExpanded = true }
                    )
                    DropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name) },
                                onClick = {
                                    accountId = acc.id
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }

                // Selected target transfer account option field dropdown (Only if TRANSFER)
                if (type == "TRANSFER") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val selTargetAccName = accounts.find { it.id == toAccountId }?.name ?: "Select Target Account"
                        OutlinedTextField(
                            value = selTargetAccName,
                            onValueChange = {},
                            label = { Text("Target Destination Account") },
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Expand link") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { toAccountExpanded = true }
                        )
                        DropdownMenu(
                            expanded = toAccountExpanded,
                            onDismissRequest = { toAccountExpanded = false }
                        ) {
                            accounts.forEach { acc ->
                                if (acc.id != accountId) {
                                    DropdownMenuItem(
                                        text = { Text(acc.name) },
                                        onClick = {
                                            toAccountId = acc.id
                                            toAccountExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Category selection dropdown (Only if not TRANSFER)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val selCatName = categories.find { it.id == categoryId }?.name ?: "Uncategorized"
                        OutlinedTextField(
                            value = selCatName,
                            onValueChange = {},
                            label = { Text("Category Classification") },
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Expand link") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { categoryExpanded = true }
                        )
                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Uncategorized") },
                                onClick = {
                                    categoryId = null
                                    categoryExpanded = false
                                }
                            )
                            val filteredCategories = categories.filter { it.type == type }
                            filteredCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        categoryId = cat.id
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Datepicker trigger row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = dateTs }
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, day)
                                    }
                                    dateTs = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Date picker", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Log Date", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(dateFormater.format(Date(dateTs)), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // Note form text field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Core Brief Note") },
                    placeholder = { Text("Shopping, Salary payment, Dinner with clients") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Tags inline input
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Search Tags (comma separated)") },
                    placeholder = { Text("groceries, coffee, business") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    } else if (accountId == 0) {
                        Toast.makeText(context, "Select a valid source account", Toast.LENGTH_SHORT).show()
                    } else if (type == "TRANSFER" && toAccountId == null) {
                        Toast.makeText(context, "Destination required for transfer", Toast.LENGTH_SHORT).show()
                    } else {
                        onSave(type, amt, accountId, if (type == "TRANSFER") toAccountId else null, if (type != "TRANSFER") categoryId else null, dateTs, note, tags)
                    }
                }
            ) {
                Text("Process Record Block")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun RecordItemRow(
    tx: TransactionWithDetails,
    sym: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val indicatorColor = when (tx.transaction.type) {
                "INCOME" -> Color(0xFF2ECC71)
                "EXPENSE" -> Color(0xFFE74C3C)
                else -> Color(0xFFF1C40F)
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(indicatorColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (tx.transaction.type) {
                        "INCOME" -> Icons.Default.VerticalAlignBottom
                        "EXPENSE" -> Icons.Default.VerticalAlignTop
                        else -> Icons.Default.SwapCalls
                    },
                    contentDescription = tx.transaction.type,
                    tint = indicatorColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (tx.transaction.note.isNotEmpty()) tx.transaction.note else "Untitled Record",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = tx.accountName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (tx.categoryName != null) {
                        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)))
                        Text(
                            text = tx.categoryName,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (tx.transaction.tags.isNotEmpty()) {
                        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)))
                        Text(
                            text = "#${tx.transaction.tags.split(",").firstOrNull()?.trim()}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Price layout Column showing positive/negative values
            Spacer(modifier = Modifier.width(8.dp))
            val prefix = when (tx.transaction.type) {
                "EXPENSE" -> "-"
                "INCOME" -> "+"
                else -> ""
            }
            Text(
                text = "$prefix$sym${String.format(Locale.US, "%,.2f", tx.transaction.amount)}",
                color = indicatorColor,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                maxLines = 1
            )
        }
    }
}

fun getCategoryIconVector(iconName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (iconName.lowercase()) {
        "restaurant", "food" -> Icons.Default.Restaurant
        "directions_car", "transport", "car" -> Icons.Default.DirectionsCar
        "shopping_bag", "shopping" -> Icons.Default.ShoppingBag
        "receipt_long", "bills", "bill" -> Icons.Default.ReceiptLong
        "sports_esports", "entertainment", "game" -> Icons.Default.SportsEsports
        "medical_services", "health", "medical" -> Icons.Default.MedicalServices
        "school", "education" -> Icons.Default.School
        "flight", "travel" -> Icons.Default.Flight
        "home", "rent", "housing" -> Icons.Default.Home
        "subscriptions", "sub" -> Icons.Default.Subscriptions
        "payments", "salary", "income" -> Icons.Default.Payments
        "redeem", "bonus" -> Icons.Default.Redeem
        "trending_up", "invest", "investment" -> Icons.Default.TrendingUp
        "card_giftcard", "gift" -> Icons.Default.CardGiftcard
        "category" -> Icons.Default.Category
        else -> Icons.Default.MoreHoriz
    }
}
