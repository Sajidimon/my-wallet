package com.example.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Category
import com.example.presentation.viewmodel.WalletViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: WalletViewModel,
    onMenuClick: () -> Unit
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()

    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var newCatName by remember { mutableStateOf("") }
    var selectedCatColor by remember { mutableStateOf(0xFF3498DB) }
    var selectedCatIcon by remember { mutableStateOf("Category") }
    var selectedCategoryType by remember { mutableStateOf("EXPENSE") }

    LaunchedEffect(editingCategory) {
        editingCategory?.let { cat ->
            newCatName = cat.name
            selectedCatColor = cat.color.toLong() and 0xFFFFFFFFL
            selectedCatIcon = cat.icon
            selectedCategoryType = cat.type
        } ?: run {
            newCatName = ""
            selectedCatColor = 0xFF3498DB
            selectedCatIcon = "Category"
            selectedCategoryType = "EXPENSE"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Open drawer")
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Add and manage custom categories for your ledger entries.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Category Creator Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (editingCategory == null) "Create Category" else "Edit Category: ${editingCategory!!.name}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("categories_screen_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Type Selection Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Category Type",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    }

                    // Color Selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Select Palette Color",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorVal))
                                        .clickable { selectedCatColor = colorVal }
                                        .border(
                                            width = if (selectedCatColor == colorVal) 2.5.dp else 0.dp,
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
                    }

                    // Icon/Symbol Selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Select Symbol Icon",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                        .size(42.dp)
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
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                if (newCatName.trim().isEmpty()) {
                                    Toast.makeText(context, "Please enter a valid category name", Toast.LENGTH_SHORT).show()
                                } else {
                                    if (editingCategory == null) {
                                        viewModel.addCategory(
                                            newCatName.trim(),
                                            selectedCategoryType,
                                            selectedCatIcon,
                                            selectedCatColor.toInt()
                                        )
                                        Toast.makeText(context, "Category added successfully!", Toast.LENGTH_SHORT).show()
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
                                        Toast.makeText(context, "Category updated!", Toast.LENGTH_SHORT).show()
                                        editingCategory = null
                                    }
                                    newCatName = ""
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (editingCategory == null) "Add Category" else "Save Changes")
                        }

                        if (editingCategory != null) {
                            OutlinedButton(
                                onClick = { editingCategory = null },
                                modifier = Modifier.weight(0.7f)
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }

            // Existing Categories Title
            Text(
                text = "Existing Categories",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (categories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No categories created yet.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { cat ->
                        val iconVector = getCategoryIconVector(cat.icon)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingCategory = cat },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(cat.color)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = cat.name,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cat.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = cat.type,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { editingCategory = cat }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit category",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteCategory(cat)
                                            if (editingCategory?.id == cat.id) {
                                                editingCategory = null
                                            }
                                            Toast.makeText(context, "Deleted ${cat.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete category",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
