package com.example.presentation.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Account
import com.example.presentation.viewmodel.WalletViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: WalletViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedAccountForEdit by remember { mutableStateOf<Account?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounts", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                    }
                },
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
                modifier = Modifier.testTag("add_account_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
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
            Text(
                text = "Manage your cash, bank cards, credit cards or specific savings folders securely offline.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (accounts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Empty accounts", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No accounts declared. Tap '+' to create one.", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(accounts) { acc ->
                        PremiumAccountCard(
                            account = acc,
                            sym = currencySymbol,
                            onClick = { selectedAccountForEdit = acc }
                        )
                    }
                }
            }
        }
    }

    // Add Account Dialog
    if (showAddDialog) {
        AccountFormDialog(
            title = "Open Account",
            onDismiss = { showAddDialog = false },
            onSave = { name, type, balance, icon, color ->
                viewModel.addAccount(name, type, balance, icon, color)
                showAddDialog = false
                Toast.makeText(context, "Account added successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Edit Account Dialog
    selectedAccountForEdit?.let { orig ->
        AccountFormDialog(
            title = "Update Account",
            initialAccount = orig,
            onDismiss = { selectedAccountForEdit = null },
            onSave = { name, type, balance, icon, color ->
                viewModel.editAccount(orig.id, name, type, balance, icon, color)
                selectedAccountForEdit = null
                Toast.makeText(context, "Account settings updated!", Toast.LENGTH_SHORT).show()
            },
            onDelete = {
                viewModel.deleteAccount(orig)
                selectedAccountForEdit = null
                Toast.makeText(context, "Account permanently removed.", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun PremiumAccountCard(
    account: Account,
    sym: String,
    onClick: () -> Unit
) {
    val gradientBrush = Brush.horizontalGradient(
        colors = when (account.type) {
            "BANK" -> listOf(Color(0xFF3a7bd5), Color(0xFF3a6073))  // Blue gradient
            "CREDIT_CARD" -> listOf(Color(0xFF8e2de2), Color(0xFF4a00e0)) // Violet
            "SAVINGS" -> listOf(Color(0xFF11998e), Color(0xFF38ef7d))  // Minty Teal
            else -> listOf(Color(0xFFeb3349), Color(0xFFf45c43)) // warm cash
        }
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = account.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = account.type.replace("_", " "),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (account.type) {
                                "BANK" -> Icons.Default.AccountBalance
                                "CREDIT_CARD" -> Icons.Default.CreditCard
                                "SAVINGS" -> Icons.Default.Savings
                                else -> Icons.Default.Paid
                            },
                            contentDescription = account.type,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                Text(
                    text = "AVAILABLE BALANCE",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "$sym${String.format(Locale.US, "%,.2f", account.balance)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun AccountFormDialog(
    title: String,
    initialAccount: Account? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, balance: Double, icon: String, color: Int) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialAccount?.name ?: "") }
    var type by remember { mutableStateOf(initialAccount?.type ?: "CASH") }
    var balanceStr by remember { mutableStateOf(initialAccount?.balance?.toString() ?: "") }
    var selectedColor by remember { mutableStateOf(initialAccount?.color ?: 0xFF3498DB.toInt()) }
    var selectedIcon by remember { mutableStateOf(initialAccount?.icon ?: "wallet") }

    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold)
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    placeholder = { Text("Main Bank, Wallet, Credit Card") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("account_name_field")
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = type.replace("_", " "),
                        onValueChange = {},
                        label = { Text("Account Classification") },
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Expand link") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { typeExpanded = true }
                    )
                    DropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        listOf("CASH", "BANK", "CREDIT_CARD", "SAVINGS").forEach { categoryType ->
                            DropdownMenuItem(
                                text = { Text(categoryType.replace("_", " ")) },
                                onClick = {
                                    type = categoryType
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = balanceStr,
                    onValueChange = { balanceStr = it },
                    label = { Text("Starting Balance") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("account_balance_field")
                )

                // Color picker block
                Text("Theme Accent", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val accColors = listOf(0xFFeb3349, 0xFF3a7bd5, 0xFF8e2de2, 0xFF11998e, 0xFFf1c40f)
                    accColors.forEach { colorVal ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .clickable { selectedColor = colorVal.toInt() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == colorVal.toInt()) {
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = balanceStr.toDoubleOrNull() ?: 0.0
                    if (name.trim().isEmpty()) {
                        Toast.makeText(context, "Please write a name", Toast.LENGTH_SHORT).show()
                    } else {
                        onSave(name, type, bal, selectedIcon, selectedColor)
                    }
                }
            ) {
                Text("Confirm & Launch")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}
