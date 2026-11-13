package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.*
import com.example.ui.theme.*

/**
 * Educational "اشرح لي" ModalBottomSheet displaying the 7 structured teaching blocks:
 * 1. Full Arabic & English Name
 * 2. Plain-language question answered
 * 3. Formula in symbols (LTR Inter)
 * 4. Step-by-step substitution with current project numbers (LTR Inter)
 * 5. Interpretation against Control Thresholds
 * 6. Common Mistake / Pitfall
 * 7. Clickable links to related concepts in the Glossary / other metrics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplainMetricSheet(
    metricId: MetricId,
    state: ProjectState,
    snapshot: EvmSnapshot,
    onDismiss: () -> Unit,
    onSelectMetric: (MetricId) -> Unit,
    onNavigateToGlossary: () -> Unit
) {
    val explanation = ExplainContent.explain(metricId, state, snapshot)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header: Symbol + Arabic & Bold Black English Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandPrimaryContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            LtrText(
                                text = explanation.symbol,
                                color = BrandPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (explanation.status != MetricStatus.NEUTRAL) {
                            Surface(
                                color = statusContainerColor(explanation.status),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = statusLabelAr(explanation.status),
                                    color = statusColor(explanation.status),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = explanation.nameAr,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    LtrText(
                        text = explanation.nameEn,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "إغلاق الشرح")
                }
            }

            // 2. Question answered
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PanelBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = null,
                        tint = BrandPrimary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "السؤال الذي يجيب عنه المؤشر:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = explanation.questionAr,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }

            // 3 & 4. Formula + Step-by-step substitution in LTR Inter
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, CardBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "المعادلة بالرموز:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .padding(10.dp)
                    ) {
                        LtrText(
                            text = explanation.formulaLtr,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary
                        )
                    }

                    Text(
                        text = "التعويض بأرقام مشروعك الحالية:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEEF6FF))
                            .padding(10.dp)
                    ) {
                        LtrText(
                            text = explanation.substitutionLtr,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F3D6E)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "النتيجة:",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                        LtrText(
                            text = explanation.resultLtr,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor(explanation.status)
                        )
                    }
                }
            }

            // 5. Interpretation
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "التفسير العملي للنتيجة",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = explanation.interpretationAr,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }

            // 6. Common Mistake
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = StatusWarningContainer),
                border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = StatusWarning
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "خطأ شائع انتبه له:",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF8A5D00)
                        )
                        Text(
                            text = explanation.commonMistakeAr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }

            // 7. Related Concepts & Glossary Link
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "مفاهيم مرتبطة:",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    explanation.relatedMetrics.forEach { rel ->
                        AssistChip(
                            onClick = { onSelectMetric(rel) },
                            label = {
                                LtrText(text = rel.symbol, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onNavigateToGlossary()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("فتح القاموس والمعادلات الكاملة")
                }
            }
        }
    }
}
