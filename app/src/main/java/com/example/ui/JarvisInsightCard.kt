package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.DebtTransactionEntity
import com.example.data.local.entity.DebtType
import com.example.data.local.entity.PersonEntity
import com.example.feature.dashboard.presentation.DashboardViewModel
import com.example.ui.debt.DebtViewModel
import com.example.utils.Result
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisInsightCard(
    dashboardViewModel: DashboardViewModel,
    debtViewModel: DebtViewModel,
    modifier: Modifier = Modifier,
    onNavigateToDebt: () -> Unit
) {
    // Collect states
    val weeklySummaryState by dashboardViewModel.weeklySummary.collectAsStateWithLifecycle()
    val debts by debtViewModel.debts.collectAsStateWithLifecycle()
    val persons by debtViewModel.persons.collectAsStateWithLifecycle()
    val payments by debtViewModel.payments.collectAsStateWithLifecycle()
    val totalReceivables by debtViewModel.totalReceivables.collectAsStateWithLifecycle()
    val totalPayables by debtViewModel.totalPayables.collectAsStateWithLifecycle()
    val overdueCount by debtViewModel.overdueCount.collectAsStateWithLifecycle()
    val dueTodayCount by debtViewModel.dueTodayCount.collectAsStateWithLifecycle()
    val currencyCode by dashboardViewModel.currencyCode.collectAsStateWithLifecycle()

    var isExpanded by remember { mutableStateOf(true) }
    val arrowRotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrow_rotation")

    // Dynamic aggregated analysis of debts
    val netBalance = totalReceivables - totalPayables
    val isNetBalancePositive = netBalance >= 0

    // Compute the soonest due pending debt
    val soonestDueDebt = remember(debts) {
        debts.filter { !it.isSettled && it.dueDate > 0 }
            .minByOrNull { it.dueDate }
    }
    
    val soonestDuePersonName = remember(soonestDueDebt, persons) {
        soonestDueDebt?.let { debt ->
            persons.firstOrNull { it.id == debt.personId }?.name
        } ?: "Someone"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with custom primary gradient & Jarvis logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "LifeOS AI Portfolio Advisor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Continuous Intelligent Analysis",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { dashboardViewModel.getWeeklySummary() },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh summary",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                modifier = Modifier
                                    .rotate(arrowRotation)
                                    .size(24.dp)
                                )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // 1. Dynamic Actionable Heuristic Bullet Highlights
                Text(
                    text = "ACTIONABLE PORTFOLIO HIGHLIGHTS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Highlight 1: Net Balance Progress
                HighlightItem(
                    icon = if (isNetBalancePositive) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                    iconTint = if (isNetBalancePositive) Color(0xFF81C784) else Color(0xFFFFB74D),
                    title = if (isNetBalancePositive) "Your net balance is improving" else "Your net balance is in deficit",
                    description = buildAnnotatedString {
                        append("Currently a net ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(if (isNetBalancePositive) "creditor" else "debtor")
                        }
                        append(" by ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                            append("$currencyCode ${String.format("%,.2f", Math.abs(netBalance))}")
                        }
                        append(".")
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Highlight 2: Soon-due or Overdue Alerts
                when {
                    overdueCount > 0 -> {
                        HighlightItem(
                            icon = Icons.Rounded.NotificationImportant,
                            iconTint = MaterialTheme.colorScheme.error,
                            title = "Urgent: Accounts Overdue",
                            description = buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)) {
                                    append("$overdueCount debts ")
                                }
                                append("have passed their due dates. Initiate settlement discussions to avoid penalties.")
                            }
                        )
                    }
                    soonestDueDebt != null -> {
                        HighlightItem(
                            icon = Icons.Rounded.HourglassBottom,
                            iconTint = MaterialTheme.colorScheme.tertiary,
                            title = "Reminder: Soon-due Settlement",
                            description = buildAnnotatedString {
                                append("Reminder: ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("$soonestDuePersonName's ")
                                }
                                append("debt of ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                                    append("$currencyCode ${String.format("%,.2f", soonestDueDebt.remainingAmount)}")
                                }
                                append(" is due soon.")
                            }
                        )
                    }
                    else -> {
                        HighlightItem(
                            icon = Icons.Rounded.CheckCircleOutline,
                            iconTint = Color(0xFF81C784),
                            title = "Outstanding Repayment Schedule",
                            description = buildAnnotatedString {
                                append("All active debt entries are fully synchronized with zero upcoming due date emergencies.")
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                // 2. Expandable AI processed Detailed Summary
                AnimatedVisibility(visible = isExpanded) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "DETAILED DYNAMIC CORE SUMMARY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        when (val state = weeklySummaryState) {
                            is Result.Loading -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 3.dp
                                        )
                                        Text(
                                            text = "LifeOS AI is analyzing your data...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            is Result.Error -> {
                                GemmaErrorBoundary(
                                    errorMessage = state.message ?: "An on-device Gemma4 local model initialization failure or inference timeout occurred.",
                                    onRetry = { dashboardViewModel.getWeeklySummary() }
                                ) {}
                            }
                            is Result.Success -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(14.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Custom elegant renderer for markdown headers, bolding and bullet highlights
                                        val formattedLines = state.data.split("\n")
                                        formattedLines.forEach { line ->
                                            if (line.isNotBlank()) {
                                                when {
                                                    line.startsWith("###") -> {
                                                        Text(
                                                            text = line.replace("###", "").trim(),
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                                        )
                                                    }
                                                    line.startsWith("####") -> {
                                                        Text(
                                                            text = line.replace("####", "").trim(),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.secondary,
                                                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                                                        )
                                                    }
                                                    line.startsWith("##") -> {
                                                        Text(
                                                            text = line.replace("##", "").trim(),
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
                                                        )
                                                    }
                                                    line.startsWith("-") || line.startsWith("*") -> {
                                                        val content = line.substring(1).trim()
                                                        Row(
                                                            modifier = Modifier.padding(vertical = 2.dp),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = "•",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                            Text(
                                                                text = parseBoldText(content),
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    else -> {
                                                        Text(
                                                            text = parseBoldText(line.trim()),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action buttons: Navigate to Debt advisory panel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onNavigateToDebt,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Consult LifeOS AI Advisor", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    description: androidx.compose.ui.text.AnnotatedString,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Inline helper to render **bold** strings beautifully in Jetpack Compose
private fun parseBoldText(text: String): androidx.compose.ui.text.AnnotatedString {
    val parts = text.split("**")
    return buildAnnotatedString {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.Unspecified)) {
                    append(part)
                }
            } else {
                append(part)
            }
        }
    }
}
