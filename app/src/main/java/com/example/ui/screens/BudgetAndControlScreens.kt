package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.*
import com.example.ui.components.*
import com.example.ui.theme.*
import java.util.UUID

// =====================================================================
// 5.4 Determine Budget (7.3)
// =====================================================================
@Composable
fun DetermineBudgetScreen(
    state: ProjectState,
    snapshot: EvmSnapshot,
    budget: BudgetBreakdown,
    onUpdateManagementReserve: (ManagementReserveConfig) -> Unit,
    onUpdatePackagePhasing: (String, Int, Int, DistributionPattern) -> Unit,
    onAddOrUpdateTranche: (FundingTranche) -> Unit,
    onDeleteTranche: (String) -> Unit,
    onExplain: (MetricId) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = state.plan.roundingPrecision
    val unitLabel = "${state.setup.displayUnit.multiplierHint} ${state.setup.currency}".trim()

    var mrMode by remember(state.managementReserveConfig) { mutableStateOf(state.managementReserveConfig.mode) }
    var mrValStr by remember(state.managementReserveConfig) { mutableStateOf(state.managementReserveConfig.value.toString()) }

    var showTrancheDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PmbokSectionBanner(
            processNumber = "7.3",
            titleAr = "تحديد الموازنة",
            subtitleAr = "تجميع سُلّم الموازنة والخط المرجعي للتكلفة (BAC) والتوزيع الزمني ومتطلبات التمويل",
            icon = Icons.Outlined.AccountBalance
        )

        // A. Budget Components Ladder
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, CardBorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("أ. سُلّم مكونات الموازنة (تجميعي تلقائي)", style = MaterialTheme.typography.titleMedium)
                    ExplainInlineButton(metricId = MetricId.BAC, label = "اشرح BAC", onExplain = onExplain)
                }

                BudgetLadderChart(breakdown = budget, precision = p)

                HorizontalDivider()

                BudgetStepRow("1. تقديرات تكلفة الأنشطة", EvmCalculator.formatMoney(budget.activityEstimatesSum, p), unitLabel, EtcColor)
                BudgetStepRow("2. + احتياطي طوارئ الأنشطة", "+ ${EvmCalculator.formatMoney(budget.activityContingencySum, p)}", unitLabel, ContingencyReserveColor)
                BudgetStepRow("3. = تقديرات حزم العمل", EvmCalculator.formatMoney(budget.workPackagesTotal, p), unitLabel, EvColor)
                if (budget.controlAccountsMap.isNotEmpty()) {
                    Text("4. حسابات المراقبة (Control Accounts):", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    budget.controlAccountsMap.forEach { (caName, caSum) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("• $caName", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            LtrText(text = "${EvmCalculator.formatMoney(caSum, p)} $unitLabel", fontSize = 13.sp)
                        }
                    }
                }
                BudgetStepRow("5. = الخط المرجعي للتكلفة (BAC)", EvmCalculator.formatMoney(budget.bac, p), unitLabel, PvColor, bold = true)
                BudgetStepRow("6. + الاحتياطي الإداري", "+ ${EvmCalculator.formatMoney(budget.currentManagementReserve, p)}", unitLabel, ManagementReserveColor)
                BudgetStepRow("7. = موازنة المشروع الإجمالية", EvmCalculator.formatMoney(budget.projectBudget, p), unitLabel, FundingColor, bold = true)

                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("إعداد الاحتياطي الإداري", style = MaterialTheme.typography.labelLarge)
                    ExplainInlineButton(metricId = MetricId.MANAGEMENT_RESERVE, label = "اشرح الإداري", tint = ManagementReserveColor, onExplain = onExplain)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReserveMode.entries.forEach { m ->
                        FilterChip(
                            selected = mrMode == m,
                            onClick = { mrMode = m },
                            label = { Text(m.labelAr) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = mrValStr,
                        onValueChange = { mrValStr = it },
                        label = { Text(if (mrMode == ReserveMode.PERCENT) "نسبة من BAC %" else "مبلغ الاحتياطي الإداري") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            onUpdateManagementReserve(
                                ManagementReserveConfig(mrMode, mrValStr.toDoubleOrNull() ?: 0.0)
                            )
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("تطبيق")
                    }
                }
            }
        }

        // B. Time Phasing
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, CardBorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ب. التوزيع الزمني ومنحنى PV التراكمي", style = MaterialTheme.typography.titleMedium)
                    ExplainInlineButton(metricId = MetricId.PV, label = "اشرح PV", onExplain = onExplain)
                }

                if (state.workPackages.isEmpty()) {
                    Text("أضف حزم عمل في شاشة 7.2 لتوزيعها زمنيًا.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                } else {
                    state.workPackages.forEach { pkg ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PanelBackground,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${pkg.code} — ${pkg.name}", fontWeight = FontWeight.Bold)
                                    LtrText(
                                        text = "M${pkg.startMonth} → M${pkg.endMonth}",
                                        color = BrandPrimary,
                                        fontSize = 13.sp
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            if (pkg.startMonth > 1) {
                                                onUpdatePackagePhasing(pkg.id, pkg.startMonth - 1, pkg.endMonth, pkg.distributionPattern)
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) { Text("بداية -") }
                                    OutlinedButton(
                                        onClick = {
                                            if (pkg.startMonth < pkg.endMonth) {
                                                onUpdatePackagePhasing(pkg.id, pkg.startMonth + 1, pkg.endMonth, pkg.distributionPattern)
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) { Text("بداية +") }
                                    OutlinedButton(
                                        onClick = {
                                            if (pkg.endMonth > pkg.startMonth) {
                                                onUpdatePackagePhasing(pkg.id, pkg.startMonth, pkg.endMonth - 1, pkg.distributionPattern)
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) { Text("نهاية -") }
                                    OutlinedButton(
                                        onClick = {
                                            if (pkg.endMonth < state.setup.durationMonths) {
                                                onUpdatePackagePhasing(pkg.id, pkg.startMonth, pkg.endMonth + 1, pkg.distributionPattern)
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) { Text("نهاية +") }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    DistributionPattern.entries.forEach { pat ->
                                        FilterChip(
                                            selected = pkg.distributionPattern == pat,
                                            onClick = {
                                                onUpdatePackagePhasing(pkg.id, pkg.startMonth, pkg.endMonth, pat)
                                            },
                                            label = { Text(pat.labelAr, fontSize = 11.5.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // C. Funding Requirements & Reconciliation
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, CardBorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ج. متطلبات التمويل وتسوية حدود التمويل", style = MaterialTheme.typography.titleMedium)
                    Button(
                        onClick = { showTrancheDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("إضافة دفعة")
                    }
                }

                if (snapshot.fundingShortfallMonths.isNotEmpty()) {
                    snapshot.fundingShortfallMonths.forEach { m ->
                        val pt = snapshot.fundingPoints.find { it.month == m }
                        AlertCardBanner(
                            title = "تحذير في الشهر $m",
                            message = "الإنفاق المخطط (${EvmCalculator.formatMoney(pt?.cumulativePv, p)}) يتجاوز التمويل المتاح (${EvmCalculator.formatMoney(pt?.cumulativeFunding, p)}). أعد جدولة الأعمال أو قدّم الدفعة.",
                            isCritical = true,
                            icon = Icons.Outlined.Warning
                        )
                    }
                }

                FundingVsBaselineChart(
                    fundingPoints = snapshot.fundingPoints,
                    bac = snapshot.bac,
                    projectBudget = snapshot.projectBudget
                )

                HorizontalDivider()

                var runningFunding = 0.0
                state.fundingTranches.sortedBy { it.month }.forEach { tranche ->
                    runningFunding += tranche.amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("دفعة الشهر ${tranche.month}", fontWeight = FontWeight.SemiBold)
                            LtrText(
                                text = "المبلغ: ${EvmCalculator.formatMoney(tranche.amount, p)} | التراكمي: ${EvmCalculator.formatMoney(runningFunding, p)} $unitLabel",
                                fontSize = 12.5.sp,
                                color = FundingColor
                            )
                        }
                        IconButton(onClick = { onDeleteTranche(tranche.id) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "حذف الدفعة", tint = StatusBad)
                        }
                    }
                }
            }
        }
    }

    if (showTrancheDialog) {
        var monthStr by remember { mutableStateOf("0") }
        var amtStr by remember { mutableStateOf("50") }
        AlertDialog(
            onDismissRequest = { showTrancheDialog = false },
            title = { Text("إضافة دفعة تمويل") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = monthStr,
                        onValueChange = { monthStr = it.filter { c -> c.isDigit() } },
                        label = { Text("الشهر (0 للبداية .. ${state.setup.durationMonths})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = amtStr,
                        onValueChange = { amtStr = it },
                        label = { Text("مبلغ الدفعة ($unitLabel)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val m = (monthStr.toIntOrNull() ?: 0).coerceIn(0, state.setup.durationMonths)
                    val a = (amtStr.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                    onAddOrUpdateTranche(FundingTranche(UUID.randomUUID().toString(), m, a))
                    showTrancheDialog = false
                }) { Text("إضافة") }
            },
            dismissButton = {
                TextButton(onClick = { showTrancheDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun BudgetStepRow(
    label: String,
    valueText: String,
    unitLabel: String,
    color: Color,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = if (bold) color else TextPrimary
        )
        LtrText(
            text = "$valueText $unitLabel",
            color = color,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = if (bold) 15.5.sp else 14.sp
        )
    }
}

// =====================================================================
// 5.5 Control Costs (7.4)
// =====================================================================
@Composable
fun ControlCostsScreen(
    state: ProjectState,
    snapshot: EvmSnapshot,
    onSelectStatusMonth: (Int) -> Unit,
    onUpdatePackagePerformance: (String, Int, Double, Double) -> Unit,
    onUpdateQuickEntry: (Int, Double, Double, Double) -> Unit,
    onExplain: (MetricId) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = state.plan.roundingPrecision
    val unitLabel = "${state.setup.displayUnit.multiplierHint} ${state.setup.currency}".trim()
    var selectedInputMonth by remember(state.setup.statusDateMonth) {
        mutableIntStateOf(state.setup.statusDateMonth)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PmbokSectionBanner(
            processNumber = "7.4",
            titleAr = "ضبط التكاليف",
            subtitleAr = "إدخال الأداء الدوري ومراقبة الانحرافات (CV / SV) والمؤشرات (CPI / SPI)",
            icon = Icons.Outlined.TrackChanges
        )

        // Status Date selector
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, CardBorderSubtle),
            modifier = Modifier.fillMaxWidth()
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
                    Text("تاريخ المتابعة الفعلي (Status Date)", style = MaterialTheme.typography.titleMedium)
                    LtrText(
                        text = "الشهر ${snapshot.statusMonth} / ${state.setup.durationMonths}",
                        color = BrandPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = snapshot.statusMonth.toFloat(),
                    onValueChange = { onSelectStatusMonth(it.toInt()) },
                    valueRange = 1f..state.setup.durationMonths.coerceAtLeast(1).toFloat(),
                    steps = (state.setup.durationMonths - 2).coerceAtLeast(0)
                )
            }
        }

        // 2x2 EVM Matrix (CV, SV, CPI, SPI)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiMetricCard(
                metricId = MetricId.CV,
                symbol = "CV",
                titleAr = "انحراف التكلفة",
                valueText = "${EvmCalculator.formatMoney(snapshot.cv, p)} $unitLabel",
                meansAr = if (snapshot.cv >= 0) "وفر في التكلفة مقارنة بالمنجز" else "تجاوز في التكلفة (المرجع: 0)",
                accentColor = statusColor(snapshot.cvStatus),
                status = snapshot.cvStatus,
                onExplain = onExplain,
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                metricId = MetricId.SV,
                symbol = "SV",
                titleAr = "انحراف الجدول",
                valueText = "${EvmCalculator.formatMoney(snapshot.sv, p)} $unitLabel",
                meansAr = if (snapshot.sv >= 0) "متقدم على الخطة الزمنية" else "متأخر عن الجدول (بالقيمة المالية)",
                accentColor = statusColor(snapshot.svStatus),
                status = snapshot.svStatus,
                onExplain = onExplain,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiMetricCard(
                metricId = MetricId.CPI,
                symbol = "CPI",
                titleAr = "مؤشر التكلفة",
                valueText = EvmCalculator.formatIndex(snapshot.cpi),
                meansAr = "قيمة العمل الناتج لكل 1 ${state.setup.currency} منصرف",
                accentColor = statusColor(snapshot.cpiStatus),
                status = snapshot.cpiStatus,
                onExplain = onExplain,
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                metricId = MetricId.SPI,
                symbol = "SPI",
                titleAr = "مؤشر الجدول",
                valueText = EvmCalculator.formatIndex(snapshot.spi),
                meansAr = "معدل سرعة الإنجاز مقارنة بالخطة (المرجع: 1)",
                accentColor = statusColor(snapshot.spiStatus),
                status = snapshot.spiStatus,
                onExplain = onExplain,
                modifier = Modifier.weight(1f)
            )
        }

        // Main EVM Chart with CV & SV bars
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, CardBorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("منحنيات الأداء (PV كامل حتى النهاية، EV و AC حتى تاريخ المتابعة)", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LtrText(text = "PV = ${EvmCalculator.formatMoney(snapshot.pv, p)}", color = PvColor, fontSize = 13.5.sp)
                    LtrText(text = "EV = ${EvmCalculator.formatMoney(snapshot.ev, p)}", color = EvColor, fontSize = 13.5.sp)
                    LtrText(text = "AC = ${EvmCalculator.formatMoney(snapshot.ac, p)}", color = AcColor, fontSize = 13.5.sp)
                }
                EvmCurveChart(
                    pvSeries = snapshot.pvSeries,
                    evSeries = snapshot.evSeries,
                    acSeries = snapshot.acSeries,
                    forecastSeries = emptyList(),
                    bac = snapshot.bac,
                    statusMonth = snapshot.statusMonth,
                    showVarianceArrows = true
                )
            }
        }

        // Periodic Data Entry (Detailed vs Quick mode)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, CardBorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (state.setup.inputMode == InputMode.QUICK) {
                        "إدخال الأداء الشهري المباشر (الوضع السريع)"
                    } else {
                        "إدخال الأداء الدوري لحزم العمل — الشهر $selectedInputMonth"
                    },
                    style = MaterialTheme.typography.titleMedium
                )

                if (state.setup.inputMode == InputMode.QUICK) {
                    (1..state.setup.durationMonths).forEach { m ->
                        val entry = state.quickEntries.find { it.month == m } ?: MonthlyQuickEntry(m, 0.0, 0.0, 0.0)
                        var pvStr by remember(m, entry.cumulativePv) { mutableStateOf(entry.cumulativePv.toString()) }
                        var evStr by remember(m, entry.cumulativeEv) { mutableStateOf(entry.cumulativeEv.toString()) }
                        var acStr by remember(m, entry.cumulativeAc) { mutableStateOf(entry.cumulativeAc.toString()) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LtrText(text = "M$m", fontWeight = FontWeight.Bold, modifier = Modifier.width(34.dp))
                            OutlinedTextField(
                                value = pvStr,
                                onValueChange = {
                                    pvStr = it
                                    onUpdateQuickEntry(m, it.toDoubleOrNull() ?: 0.0, evStr.toDoubleOrNull() ?: 0.0, acStr.toDoubleOrNull() ?: 0.0)
                                },
                                label = { Text("PV") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = evStr,
                                onValueChange = {
                                    evStr = it
                                    onUpdateQuickEntry(m, pvStr.toDoubleOrNull() ?: 0.0, it.toDoubleOrNull() ?: 0.0, acStr.toDoubleOrNull() ?: 0.0)
                                },
                                label = { Text("EV") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = acStr,
                                onValueChange = {
                                    acStr = it
                                    onUpdateQuickEntry(m, pvStr.toDoubleOrNull() ?: 0.0, evStr.toDoubleOrNull() ?: 0.0, it.toDoubleOrNull() ?: 0.0)
                                },
                                label = { Text("AC") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                } else {
                    // Month chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..snapshot.statusMonth).forEach { m ->
                            FilterChip(
                                selected = selectedInputMonth == m,
                                onClick = { selectedInputMonth = m },
                                label = { Text("ش$m") }
                            )
                        }
                    }

                    state.workPackages.forEach { pkg ->
                        val perf = state.packagePerformances.find { it.packageId == pkg.id && it.month == selectedInputMonth }
                        val latestPct = state.packagePerformances
                            .filter { it.packageId == pkg.id && it.month <= selectedInputMonth }
                            .maxByOrNull { it.month }?.percentComplete ?: 0.0
                        var pctStr by remember(pkg.id, selectedInputMonth, latestPct) {
                            mutableStateOf(String.format("%.1f", latestPct))
                        }
                        var acStr by remember(pkg.id, selectedInputMonth, perf?.periodActualCost) {
                            mutableStateOf((perf?.periodActualCost ?: 0.0).toString())
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PanelBackground,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("${pkg.code} — ${pkg.name}", fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = pctStr,
                                        onValueChange = {
                                            pctStr = it
                                            onUpdatePackagePerformance(
                                                pkg.id,
                                                selectedInputMonth,
                                                it.toDoubleOrNull() ?: 0.0,
                                                acStr.toDoubleOrNull() ?: 0.0
                                            )
                                        },
                                        label = { Text("الإنجاز التراكمي %") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = acStr,
                                        onValueChange = {
                                            acStr = it
                                            onUpdatePackagePerformance(
                                                pkg.id,
                                                selectedInputMonth,
                                                pctStr.toDoubleOrNull() ?: 0.0,
                                                it.toDoubleOrNull() ?: 0.0
                                            )
                                        },
                                        label = { Text("التكلفة الفعلية للفترة") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Detailed Work Package Table sorted by worst performance
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, CardBorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "تحليل الأداء لكل حزمة عمل (مرتبة حسب الأسوأ أداءً)",
                    style = MaterialTheme.typography.titleMedium
                )
                snapshot.packageRows.forEach { row ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusContainerColor(row.cpiStatus).copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, statusColor(row.cpiStatus).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${row.pkg.code} — ${row.pkg.name}", fontWeight = FontWeight.Bold)
                                LtrText(
                                    text = "CPI = ${EvmCalculator.formatIndex(row.cpi)}",
                                    color = statusColor(row.cpiStatus),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                LtrText(text = "PV: ${EvmCalculator.formatMoney(row.pv, p)}", fontSize = 12.5.sp, color = PvColor)
                                LtrText(text = "EV: ${EvmCalculator.formatMoney(row.ev, p)}", fontSize = 12.5.sp, color = EvColor)
                                LtrText(text = "AC: ${EvmCalculator.formatMoney(row.ac, p)}", fontSize = 12.5.sp, color = AcColor)
                                LtrText(
                                    text = "CV: ${EvmCalculator.formatMoney(row.cv, p)}",
                                    fontSize = 12.5.sp,
                                    color = if (row.cv < 0) StatusBad else StatusGood,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
