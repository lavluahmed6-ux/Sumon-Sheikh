package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DokanTheme
import com.example.ui.viewmodel.DailyChartPoint
import com.example.util.Formatters

@Composable
fun SevenDayBarChartCard(
    points: List<DailyChartPoint>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val dokanColors = DokanTheme.colors
    var selectedIdx by remember(points) { mutableIntStateOf((points.size - 1).coerceAtLeast(0)) }
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 550),
        label = "barChartAnim"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, dokanColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "গত ৭ দিনের বিক্রি ও আদায় বিশ্লেষণ",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartLegendDot(color = dokanColors.salesBlue, label = "বিক্রি")
                ChartLegendDot(color = dokanColors.paymentGreen, label = "আদায়")
                ChartLegendDot(color = dokanColors.dueOrange, label = "খরচ")
            }

            Spacer(modifier = Modifier.height(14.dp))

            val maxVal = remember(points) {
                points.maxOfOrNull { maxOf(it.totalSales, it.cashCollected, it.expenses) }
                    ?.coerceAtLeast(100.0) ?: 100.0
            }

            val gridLineColor = MaterialTheme.colorScheme.outlineVariant
            val highlightBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
            ) {
                val count = points.size.coerceAtLeast(1)
                val groupWidth = size.width / count
                val barWidth = (groupWidth * 0.19f).coerceIn(8f, 22f)
                val gap = barWidth * 0.35f
                val chartHeight = size.height - 8f

                // Subtle horizontal guidelines
                listOf(0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
                    val yPos = chartHeight * (1f - fraction)
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, yPos),
                        end = Offset(size.width, yPos),
                        strokeWidth = 1.2f
                    )
                }

                points.forEachIndexed { index, point ->
                    val centerX = index * groupWidth + groupWidth / 2f
                    if (index == selectedIdx) {
                        drawRoundRect(
                            color = highlightBg,
                            topLeft = Offset(index * groupWidth + 4f, 0f),
                            size = Size(groupWidth - 8f, chartHeight),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                    }

                    val totalBarGroupW = barWidth * 3f + gap * 2f
                    val startX = centerX - totalBarGroupW / 2f

                    val values = listOf(
                        point.totalSales to dokanColors.salesBlue,
                        point.cashCollected to dokanColors.paymentGreen,
                        point.expenses to dokanColors.dueOrange
                    )

                    values.forEachIndexed { vIdx, (amt, barColor) ->
                        val ratio = ((amt / maxVal).toFloat()).coerceIn(0f, 1f) * animProgress
                        val barH = (ratio * (chartHeight - 10f)).coerceAtLeast(if (amt > 0) 6f else 2f)
                        val x = startX + vIdx * (barWidth + gap)
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, chartHeight - barH),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                points.forEachIndexed { idx, pt ->
                    val selected = idx == selectedIdx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedIdx = idx }
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else Color.Transparent
                            )
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pt.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            points.getOrNull(selectedIdx)?.let { active ->
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Formatters.formatDateBn(active.dateKey),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "বিক্রি: ${Formatters.formatCurrency(active.totalSales, currencySymbol)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = dokanColors.salesBlue
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "আদায়: ${Formatters.formatCurrency(active.cashCollected, currencySymbol)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = dokanColors.paymentGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "খরচ: ${Formatters.formatCurrency(active.expenses, currencySymbol)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = dokanColors.dueOrange
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BreakdownDonutCard(
    cashSales: Double,
    creditSales: Double,
    paymentCollected: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val dokanColors = DokanTheme.colors
    val total = (cashSales + creditSales + paymentCollected).coerceAtLeast(1.0)
    val hasData = cashSales > 0.0 || creditSales > 0.0 || paymentCollected > 0.0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, dokanColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "লেনদেনের অনুপাত",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                Box(
                    modifier = Modifier.size(116.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(104.dp)) {
                        val stroke = Stroke(width = 22f, cap = StrokeCap.Butt)
                        if (!hasData) {
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = stroke
                            )
                        } else {
                            var start = -90f
                            val segments = listOf(
                                cashSales to dokanColors.salesBlue,
                                paymentCollected to dokanColors.paymentGreen,
                                creditSales to dokanColors.dueRed
                            )
                            segments.forEach { (value, segColor) ->
                                if (value > 0.0) {
                                    val sweep = ((value / total) * 360.0).toFloat()
                                    drawArc(
                                        color = segColor,
                                        startAngle = start,
                                        sweepAngle = sweep.coerceAtLeast(3f),
                                        useCenter = false,
                                        style = stroke
                                    )
                                    start += sweep
                                }
                            }
                        }
                    }
                    Text(
                        text = if (hasData) "১০০%" else "০%",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(18.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DonutLegendRow(
                        color = dokanColors.salesBlue,
                        label = "নগদ বিক্রি",
                        amount = Formatters.formatCurrency(cashSales, currencySymbol)
                    )
                    DonutLegendRow(
                        color = dokanColors.paymentGreen,
                        label = "বাকি আদায়",
                        amount = Formatters.formatCurrency(paymentCollected, currencySymbol)
                    )
                    DonutLegendRow(
                        color = dokanColors.dueRed,
                        label = "বাকি বিক্রি",
                        amount = Formatters.formatCurrency(creditSales, currencySymbol)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DonutLegendRow(color: Color, label: String, amount: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = amount,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
