package com.example.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.WalletViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WalletViewModel,
    onMenuClick: () -> Unit
) {
    val context = LocalContext.current
    val appTheme by viewModel.appTheme.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val pinProtected by viewModel.pinProtected.collectAsState()
    val userName by viewModel.userNameState.collectAsState()
    val userAvatarPath by viewModel.userAvatarPathState.collectAsState()

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }

    val currencies = listOf(
        "$" to "United States Dollar (USD)",
        "৳" to "Bangladeshi Taka (BDT)",
        "€" to "Euro (EUR)",
        "£" to "British Pound (GBP)",
        "¥" to "Japanese Yen / Chinese Yuan (JPY/CNY)",
        "₹" to "Indian Rupee (INR)",
        "₽" to "Russian Ruble (RUB)",
        "₩" to "South Korean Won (KRW)",
        "₪" to "Israeli Shekel (ILS)"
    )

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
                    Text("Settings & Security", fontWeight = FontWeight.Bold, fontSize = 20.sp) 
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
            // Section 0: User Profile
            SettingsSectionHeader(title = "User Profile")
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showProfileDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                            .size(52.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
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
                            Text(avatar.removePrefix("emoji:"), fontSize = 24.sp)
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

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(userName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Personalize your finance profile", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Section 1: Appearance
            SettingsSectionHeader(title = "Appearance")
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline)
            ) {
                Column {
                    SettingsRowToggle(
                        icon = Icons.Default.Palette,
                        title = "Dark Visual Theme",
                        subtitle = "Enable premium dark visual layout",
                        checked = appTheme == "DARK",
                        onCheckedChange = { isDark ->
                            viewModel.updateTheme(if (isDark) "DARK" else "LIGHT")
                        },
                        tag = "dark_theme_toggle"
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    SettingsRowClick(
                        icon = Icons.Default.Paid,
                        title = "Base Currency",
                        subtitle = "Selected: $currencySymbol",
                        onClick = { showCurrencyDialog = true },
                        tag = "currency_select_button"
                    )
                }
            }

            // Section 2: Security & App Lock
            SettingsSectionHeader(title = "Security")
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline)
            ) {
                Column {
                    SettingsRowToggle(
                        icon = Icons.Default.Lock,
                        title = "Access PIN",
                        subtitle = if (pinProtected) "Lock active (Requires 4-digit PIN)" else "Inactive (Unsecured access)",
                        checked = pinProtected,
                        onCheckedChange = { enable ->
                            if (enable) {
                                showPinDialog = true
                            } else {
                                viewModel.disablePin()
                                Toast.makeText(context, "PIN Locker disabled", Toast.LENGTH_SHORT).show()
                            }
                        },
                        tag = "pin_security_toggle"
                    )
                    
                    if (pinProtected) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsRowClick(
                            icon = Icons.Default.Password,
                            title = "Change Access PIN",
                            subtitle = "Modify current 4-digit lockout code",
                            onClick = { showPinDialog = true },
                            tag = "change_pin_button"
                        )
                    }
                }
            }

            // Section 3: Backup & CSV Export
            SettingsSectionHeader(title = "Data Management")
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline)
            ) {
                Column {
                    SettingsRowClick(
                        icon = Icons.Default.FileDownload,
                        title = "Export to Excel/CSV",
                        subtitle = "Compile current transactions to clean CSV table",
                        onClick = {
                            viewModel.exportTransactionsToCsv { csv ->
                                if (csv != null) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Wallet CSV", csv)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "CSV copied to clipboard successfully!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Export mapping failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        tag = "csv_export_button"
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    SettingsRowClick(
                        icon = Icons.Default.Backup,
                        title = "Sync Backups",
                        subtitle = "Backup or restore database values via JSON string",
                        onClick = { showBackupDialog = true },
                        tag = "json_backup_button"
                    )
                }
            }

            // Version and Footer
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Wallet Premium Client • Offline-First",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "v1.4.2 (Secure Room DB Local Storage)",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    // Currency Selector Dialog
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Base Currency", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currencies.forEach { (sym, desc) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.updateCurrency(sym)
                                    showCurrencyDialog = false
                                    Toast.makeText(context, "Base set to $sym", Toast.LENGTH_SHORT).show()
                                }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = sym,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(40.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = desc,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // PIN Setup Dialog
    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { 
                showPinDialog = false
                viewModel.pinProtected.value = viewModel.settingsService.isAppLockEnabled() // revert state if dismissed
            },
            title = { Text("Setup Lock PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Enter a secure 4-digit code to protect your local financial logs.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 4) {
                                pinInput = input
                                pinError = null
                            }
                        },
                        label = { Text("4-Digit PIN") },
                        placeholder = { Text("0000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = pinError != null,
                        supportingText = pinError?.let { { Text(it) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pin_code_text_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length != 4) {
                            pinError = "PIN must be exactly 4 digits"
                        } else {
                            viewModel.setupPin(pinInput)
                            showPinDialog = false
                            Toast.makeText(context, "Secure Access PIN set successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Secure App")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showPinDialog = false
                        viewModel.pinProtected.value = viewModel.settingsService.isAppLockEnabled() // revert
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sync Backups Dialog
    if (showBackupDialog) {
        var backupText by remember { mutableStateOf("") }
        var showImportBox by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Sync Backups", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Wallet is 100% offline. Copy the JSON backup string below to save your data elsewhere, or paste a backup to restore it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!showImportBox) {
                        Button(
                            onClick = {
                                viewModel.exportBackupJson { json ->
                                    if (json != null) {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Wallet JSON Backup", json)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "JSON string copied to clipboard!", Toast.LENGTH_LONG).show()
                                        showBackupDialog = false
                                    } else {
                                        Toast.makeText(context, "Export mapping error", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy JSON")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy JSON Backup String")
                        }

                        OutlinedButton(
                            onClick = { showImportBox = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = "Import JSON")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore JSON Backup")
                        }
                    } else {
                        OutlinedTextField(
                            value = backupText,
                            onValueChange = { backupText = it },
                            label = { Text("Paste JSON Backup") },
                            placeholder = { Text("Pasted text starting with { ... }") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("json_backup_input_field"),
                            maxLines = 5
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showImportBox = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Back")
                            }

                            Button(
                                onClick = {
                                    if (backupText.trim().isEmpty()) {
                                        Toast.makeText(context, "Backup data is empty", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.restoreData(backupText) { success ->
                                            if (success) {
                                                Toast.makeText(context, "All records restored successfully!", Toast.LENGTH_LONG).show()
                                                showBackupDialog = false
                                            } else {
                                                Toast.makeText(context, "Invalid JSON structure. Import aborted.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Import Now")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Profile Edit Dialog
    if (showProfileDialog) {
        var tempName by remember { mutableStateOf(userName) }
        var tempAvatar by remember { mutableStateOf(userAvatarPath ?: "Default") }
        val avatarColors = listOf("Default", "Indigo", "Teal", "Orange", "Rose", "Purple")
        val avatarEmojis = listOf("emoji:💼", "emoji:🦊", "emoji:👑", "emoji:💰", "emoji:📈", "emoji:🎯", "emoji:🔥", "emoji:⭐")

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Customize Personal Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Set your nickname and choose a profile style to personalize your local reports.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Preferred Nickname") },
                        placeholder = { Text("John Doe") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                    )

                    Text(
                        text = "Profile Theme Colors",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        avatarColors.forEach { col ->
                            val isSelected = tempAvatar == col
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(
                                        when (col) {
                                            "Indigo" -> Color(0xFF6366F1)
                                            "Teal" -> Color(0xFF14B8A6)
                                            "Orange" -> Color(0xFFF97316)
                                            "Rose" -> Color(0xFFF43F5E)
                                            "Purple" -> Color(0xFFA855F7)
                                            else -> MaterialTheme.colorScheme.primaryContainer
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.outline else Color.Transparent,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                                    .clickable { tempAvatar = col },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = if (col == "Default") MaterialTheme.colorScheme.primary else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Aesthetic Emojis",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        avatarEmojis.take(4).forEach { emojiStr ->
                            val isSelected = tempAvatar == emojiStr
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                                    .clickable { tempAvatar = emojiStr },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emojiStr.removePrefix("emoji:"), fontSize = 18.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        avatarEmojis.drop(4).forEach { emojiStr ->
                            val isSelected = tempAvatar == emojiStr
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                                    .clickable { tempAvatar = emojiStr },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emojiStr.removePrefix("emoji:"), fontSize = 18.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalName = if (tempName.trim().isEmpty()) "John Doe" else tempName.trim()
                        viewModel.updateProfile(finalName, tempAvatar)
                        showProfileDialog = false
                        Toast.makeText(context, "Profile settings saved!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
fun SettingsRowToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
fun SettingsRowClick(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Go",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
    }
}
