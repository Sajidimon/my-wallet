package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.TransactionWithDetails
import com.example.presentation.viewmodel.WalletViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: WalletViewModel,
    onMenuClick: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()

    var calendarDate by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDay by remember { mutableStateOf(Calendar.getInstance()) }

    // Re-calculate the list of days for the currently selected month in calendarDate
    val year = calendarDate.get(Calendar.YEAR)
    val month = calendarDate.get(Calendar.MONTH)

    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendarDate.time)

    // Calculate maximum days and starting offset
    val tempCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val startingDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, ...
    val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Match transactions with days of month
    val txsGroupedByDay = remember(transactions, year, month) {
        val groups = mutableMapOf<Int, MutableList<TransactionWithDetails>>()
        val itemCal = Calendar.getInstance()
        transactions.forEach { tx ->
            itemCal.timeInMillis = tx.transaction.date
            if (itemCal.get(Calendar.YEAR) == year && itemCal.get(Calendar.MONTH) == month) {
                val day = itemCal.get(Calendar.DAY_OF_MONTH)
                groups.getOrPut(day) { mutableListOf() }.add(tx)
            }
        }
        groups
    }

    // Filter transactions for the selected date
    val selectedDayTransactions = remember(transactions, selectedDay) {
        val sYear = selectedDay.get(Calendar.YEAR)
        val sMonth = selectedDay.get(Calendar.MONTH)
        val sDay = selectedDay.get(Calendar.DAY_OF_MONTH)
        val itemCal = Calendar.getInstance()

        transactions.filter { tx ->
            itemCal.timeInMillis = tx.transaction.date
            itemCal.get(Calendar.YEAR) == sYear &&
                    itemCal.get(Calendar.MONTH) == sMonth &&
                    itemCal.get(Calendar.DAY_OF_MONTH) == sDay
        }
    }

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
                title = { Text("Finance Calendar", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
        ) {
            // Calendar Switch Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val nextCal = Calendar.getInstance().apply {
                            timeInMillis = calendarDate.timeInMillis
                            add(Calendar.MONTH, -1)
                        }
                        calendarDate = nextCal
                    },
                    modifier = Modifier.testTag("prev_month_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
                }

                Text(
                    text = monthName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(
                    onClick = {
                        val nextCal = Calendar.getInstance().apply {
                            timeInMillis = calendarDate.timeInMillis
                            add(Calendar.MONTH, 1)
                        }
                        calendarDate = nextCal
                    },
                    modifier = Modifier.testTag("next_month_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
                }
            }

            // Weekdays labels (S, M, T, W, T, F, S)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val weekDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                weekDays.forEach { dayLabel ->
                    Text(
                        text = dayLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Month Grid Canvas view
            val weeksCount = kotlin.math.ceil((daysInMonth + (startingDayOfWeek - 1)).toDouble() / 7).toInt()
            var currentDayIndex = 1

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                for (w in 0 until weeksCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (d in 1..7) {
                            val absoluteCellIndex = w * 7 + d
                            if (absoluteCellIndex < startingDayOfWeek || currentDayIndex > daysInMonth) {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val day = currentDayIndex
                                val isSelected = selectedDay.get(Calendar.YEAR) == year &&
                                        selectedDay.get(Calendar.MONTH) == month &&
                                        selectedDay.get(Calendar.DAY_OF_MONTH) == day

                                val dayTxs = txsGroupedByDay[day] ?: emptyList()
                                val expenseCount = dayTxs.count { it.transaction.type == "EXPENSE" }
                                val incomeCount = dayTxs.count { it.transaction.type == "INCOME" }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            selectedDay = Calendar.getInstance().apply {
                                                set(Calendar.YEAR, year)
                                                set(Calendar.MONTH, month)
                                                set(Calendar.DAY_OF_MONTH, day)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface
                                        )

                                        // Dots indicators for transactions
                                        if (dayTxs.isNotEmpty()) {
                                            Row(
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                if (incomeCount > 0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(1.dp)
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) Color.White else Color(0xFF2ECC71))
                                                    )
                                                }
                                                if (expenseCount > 0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(1.dp)
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) Color.White else Color(0xFFE74C3C))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                currentDayIndex++
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Header for Selected Day's details
            val sdfStr = SimpleDateFormat("LLLL dd, yyyy", Locale.getDefault()).format(selectedDay.time)
            Text(
                text = "Records for $sdfStr",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            if (selectedDayTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = "No data",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No financial activity logged for this date.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(selectedDayTransactions) { item ->
                        CalendarTxRow(tx = item, sym = currencySymbol)
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarTxRow(
    tx: TransactionWithDetails,
    sym: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Type Icon indicator
            val indicatorColor = when (tx.transaction.type) {
                "INCOME" -> Color(0xFF2ECC71)
                "EXPENSE" -> Color(0xFFE74C3C)
                else -> Color(0xFFF1C40F)
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(indicatorColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (tx.transaction.type) {
                        "INCOME" -> Icons.Default.ArrowUpward
                        "EXPENSE" -> Icons.Default.ArrowDownward
                        else -> Icons.Default.CompareArrows
                    },
                    contentDescription = tx.transaction.type,
                    tint = indicatorColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (tx.transaction.note.isNotEmpty()) tx.transaction.note else "Untitled Record",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tx.accountName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (tx.categoryName != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tx.categoryName,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Amount Layout
            Text(
                text = "${if (tx.transaction.type == "EXPENSE") "-" else "+"}$sym${String.format(Locale.US, "%,.2f", tx.transaction.amount)}",
                color = indicatorColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}
