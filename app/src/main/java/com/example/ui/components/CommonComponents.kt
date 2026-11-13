package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.MetricId
import com.example.domain.MetricStatus
import com.example.ui.theme.*

fun statusColor(status: MetricStatus): Color = when (status) {
    MetricStatus.GOOD -> StatusGood
    MetricStatus.WARNING -> StatusWarning
    MetricStatus.BAD -> StatusBad
    MetricStatus.NEUTRAL -> BrandPrimary
}

fun statusContainerColor(status: MetricStatus): Color = when (status) {
    MetricStatus.GOOD -> StatusGoodContainer
    MetricStatus.WARNING -> StatusWarningContainer
    MetricStatus.BAD -> StatusBadContainer
    MetricStatus.NEUTRAL -> Color(0xFFF1F5F9)
}

fun statusLabelAr(status: MetricStatus): String = when (status) {
    MetricStatus.GOOD -> "جيدة"
    MetricStatus.WARNING -> "تحذير"
    MetricStatus.BAD -> "سيئة / حرجة"
    MetricStatus.NEUTRAL -> "مرجعي"
}

/**
 * Forces LTR layout direction and Inter font for formulas, negative numbers, and index values
 * so negative signs never flip in RTL Arabic screens (renders −40, never 40−).
 */
@Composable
fun LtrText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = 15.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(
            text = text,
            modifier = modifier,
            style = TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = fontWeight,
                fontSize = fontSize
            ),
            color = color,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun KpiMetricCard(
    metricId: MetricId,
    symbol: String,
    titleAr: String,
    valueText: String,
    meansAr: String,
    accentColor: Color,
    status: MetricStatus = MetricStatus.NEUTRAL,
    showStatusPill: Boolean = true,
    onExplain: (MetricId) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (status != MetricStatus.NEUTRAL && showStatusPill) {
        statusColor(status).copy(alpha = 0.55f)
    } else {
        accentColor.copy(alpha = 0.35f)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        LtrText(
                            text = symbol,
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = titleAr,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { onExplain(metricId) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("explain_btn_${metricId.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "اشرح لي $titleAr",
                        tint = accentColor
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LtrText(
                    text = valueText,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (status != MetricStatus.NEUTRAL && showStatusPill) {
                        statusColor(status)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                if (showStatusPill && status != MetricStatus.NEUTRAL) {
                    Surface(
                        color = statusContainerColor(status),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = statusLabelAr(status),
                            color = statusColor(status),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Text(
                text = "يعني: $meansAr",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.5.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun ExplainInlineButton(
    metricId: MetricId,
    label: String = "اشرح لي",
    tint: Color = BrandPrimary,
    onExplain: (MetricId) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onExplain(metricId) }
            .testTag("explain_inline_${metricId.name.lowercase()}"),
        color = tint.copy(alpha = 0.1f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PmbokSectionBanner(
    processNumber: String?,
    titleAr: String,
    subtitleAr: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = titleAr,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!processNumber.isNullOrBlank()) {
                        Surface(
                            color = ManagementReserveColor,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            LtrText(
                                text = processNumber,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3E2200),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = titleAr,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitleAr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
        }
    }
}

@Composable
fun AlertCardBanner(
    title: String,
    message: String,
    isCritical: Boolean = true,
    icon: ImageVector,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val color = if (isCritical) StatusBad else StatusWarning
    val bg = if (isCritical) StatusBadContainer else StatusWarningContainer
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = color)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 14.sp,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            if (actionLabel != null && onAction != null) {
                TextButton(
                    onClick = onAction,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = actionLabel, color = color, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
