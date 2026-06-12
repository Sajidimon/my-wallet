package com.example.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.TransactionWithDetails
import com.example.presentation.viewmodel.WalletViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: WalletViewModel,
    onMenuClick: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()

    var activeTab by remember { mutableStateOf("EXPENSE") } // EXPENSE, INCOME

    // Compute aggregations inside standard remember statements to guarantee fast UI thread performance
    val categoryTotals = remember(transactions, activeTab) {
        val totals = mutableMapOf<String, Double>()
        val colors = mutableMapOf<String, Int>()
        transactions.filter { it.transaction.type == activeTab }.forEach { tx ->
            val catName = tx.categoryName ?: "Uncategorized"
            totals[catName] = (totals[catName] ?: 0.0) + tx.transaction.amount
            colors[catName] = tx.categoryColor ?: 0xFF95A5A6.toInt()
        }
        totals.map { (name, amt) -> CategoryPieSlice(name, amt, Color(colors[name] ?: 0xFF95A5A6.toInt())) }
            .sortedByDescending { it.amount }
    }

    val totalSum = categoryTotals.sumOf { it.amount }

    // Last 7 days spending trend data calculation (Daily line plot calculation)
    val dailyTrendList = remember(transactions) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val dates = (0..6).map { offset ->
            val temp = Calendar.getInstance()
            temp.add(Calendar.DAY_OF_YEAR, -offset)
            sdf.format(temp.time) to temp.get(Calendar.DAY_OF_MONTH)
        }.reversed()

        val dailyTotals = mutableMapOf<String, Double>()
        transactions.filter { it.transaction.type == "EXPENSE" }.forEach { tx ->
            val dateStr = sdf.format(Date(tx.transaction.date))
            dailyTotals[dateStr] = (dailyTotals[dateStr] ?: 0.0) + tx.transaction.amount
        }

        dates.map { (dateStr, dayNum) ->
            DailySpendData(
                label = dayNum.toString(),
                amount = dailyTotals[dateStr] ?: 0.0
            )
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
                title = { Text("Performance & Reports", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
            // Tab switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                TabToggleBtn(
                    label = "Expenses Analysis",
                    active = activeTab == "EXPENSE",
                    modifier = Modifier.weight(1f).testTag("analytics_expense_tab_btn"),
                    onClick = { activeTab = "EXPENSE" }
                )
                TabToggleBtn(
                    label = "Incomes Analysis",
                    active = activeTab == "INCOME",
                    modifier = Modifier.weight(1f).testTag("analytics_income_tab_btn"),
                    onClick = { activeTab = "INCOME" }
                )
            }

            // Pie chart card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PieChart, contentDescription = "Pie chart theme", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Category Breakdown", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    if (totalSum == 0.0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Info, contentDescription = "No records", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("No transactions logged for this category graph", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                            }
                        }
                    } else {
                        // Drawing custom Arc-based Pie Chart on a high-contrast canvas
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .align(Alignment.CenterHorizontally),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                var currentStartAngle = -90f
                                categoryTotals.forEach { slice ->
                                    val sweepAngle = ((slice.amount / totalSum) * 360f).toFloat()
                                    drawArc(
                                        color = slice.color,
                                        startAngle = currentStartAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        style = Stroke(width = 30.dp.toPx())
                                    )
                                    currentStartAngle += sweepAngle
                                }
                            }

                            // Centered Total Info
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$currencySymbol${String.format(Locale.US, "%,.0f", totalSum)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Legends Row
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            categoryTotals.forEach { slice ->
                                val pct = (slice.amount / totalSum) * 100f
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(slice.color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = slice.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", pct)}%",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$currencySymbol${String.format(Locale.US, "%,.2f", slice.amount)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Daily trend card (7 days expense line chart)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ShowChart, contentDescription = "Trend graph icon", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Last 7 Days Spending", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    val maxLimit = remember(dailyTrendList) { (dailyTrendList.maxOfOrNull { it.amount } ?: 0.0).coerceAtLeast(10.0) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val sizeCount = dailyTrendList.size
                            val stepX = w / (sizeCount - 1).coerceAtLeast(1)

                            // Generate smooth line path
                            val path = Path()
                            val fillPath = Path()

                            dailyTrendList.forEachIndexed { idx, point ->
                                val pctHeight = (point.amount / maxLimit).toFloat()
                                val x = idx * stepX
                                val y = h - (pctHeight * (h - 20f)) - 10f

                                if (idx == 0) {
                                    path.moveTo(x, y)
                                    fillPath.moveTo(x, h)
                                    fillPath.lineTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                    fillPath.lineTo(x, y)
                                }

                                if (idx == sizeCount - 1) {
                                    fillPath.lineTo(x, h)
                                    fillPath.close()
                                }
                            }

                            // Draw gradient under-fill
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF1ABC9C).copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            )

                            // Draw core trend line
                            drawPath(
                                path = path,
                                color = Color(0xFF1ABC9C),
                                style = Stroke(width = 3.dp.toPx())
                            )

                            // Draw data dots
                            dailyTrendList.forEachIndexed { idx, point ->
                                val pctHeight = (point.amount / maxLimit).toFloat()
                                val x = idx * stepX
                                val y = h - (pctHeight * (h - 20f)) - 10f
                                drawCircle(
                                    color = Color(0xFF1ABC9C),
                                    radius = 4.dp.toPx(),
                                    center = Offset(x, y)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }

                    // Bottom labels for 7 days
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        dailyTrendList.forEach { point ->
                            Text(
                                text = "Day ${point.label}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabToggleBtn(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(10.dp),
        modifier = modifier
    ) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

data class CategoryPieSlice(val name: String, val amount: Double, val color: Color)
data class DailySpendData(val label: String, val amount: Double)
