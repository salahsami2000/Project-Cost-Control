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

// =====================================================================
// 5.6 Forecasting (التنبؤ)
// =====================================================================
@Composable
fun ForecastingScreen(
    state: ProjectState,
    snapshot: EvmSnapshot,
    onSelectEacMethod: (EacMethod) -> Unit,
    onUpdateManualEtc: (Double) -> Unit,
    onCreateChangeRequestFromEac: (Double) -> Unit,
    onExplain: (MetricId) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = state.plan.roundingPrecision
    val unitLabel = "${state.setup.displayUnit.multiplierHint} ${state.setup.currency}".trim()
    var manualEtcStr by remember(state.manualEtcInput) { mutableStateOf(state.manualEtcInput.toString()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PmbokSectionBanner(
            processNumber = null,
            titleAr = "التنبؤ بالتكلفة النهائية (Forecasting)",
            subtitleAr = "مقارنة طرق التقدير عند الاكتمال (EAC) وحكم واقعية الالتزام بالموازنة",
            icon = Icons.Outlined.TrendingUp
        )

        // A. Three EAC Methods + Manual ETC
        Text("أ. التقدير عند الاكتمال (EAC): اختر الطريقة المعتمدة", style = MaterialTheme.typography.titleMedium)

        EacMethodCard(
            method = EacMethod.ATYPICAL,
            metricId = MetricId.EAC_ATYPICAL,
            valueText = "${EvmCalculator.formatMoney(snapshot.eacAtypical, p)} $unitLabel",
            whenToUse = "تُستخدم عندما يكون الانحراف استثنائيًا (مثل ارتفاع سعر مؤقت) ولن يتكرر في المتبقي.",
            isSelected = state.selectedEacMethod == EacMethod.ATYPICAL,
            onSelect = { onSelectEacMethod(EacMethod.ATYPICAL) },
            onExplain = onExplain
        )

        EacMethodCard(
            method = EacMethod.TYPICAL,
            metricId = MetricId.EAC_TYPICAL,
            valueText = if (snapshot.eacTypical == null) "غير قابل للحساب" else "${EvmCalculator.formatMoney(snapshot.eacTypical, p)} $unitLabel",
            whenToUse = "تُستخدم عندما يُتوقع استمرار نفس معدل كفاءة التكلفة الحالي (مثل ضعف إنتاجية مستمر).",
            isSelected = state.selectedEacMethod == EacMethod.TYPICAL,
            onSelect = { onSelectEacMethod(EacMethod.TYPICAL) },
            onExplain = onExplain
        )

        EacMethodCard(
            method = EacMethod.COST_AND_SCHEDULE,
            metricId = MetricId.EAC_COMBINED,
            valueText = if (snapshot.eacCombined == null) "غير متاح (CPI×SPI = 0)" else "${EvmCalculator.formatMoney(snapshot.eacCombined, p)} $unitLabel",
            whenToUse = "تُستخدم عندما يؤثر التأخير الزمني نفسه في زيادة التكلفة المباشرة أو غير المباشرة.",
            isSelected = state.selectedEacMethod == EacMethod.COST_AND_SCHEDULE,
            onSelect = { onSelectEacMethod(EacMethod.COST_AND_SCHEDULE) },
            onExplain = onExplain
        )

        // Optional Manual Bottom-Up ETC
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (state.selectedEacMethod == EacMethod.MANUAL) BrandPrimaryContainer else MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
                1.5.dp,
                if (state.selectedEacMethod == EacMethod.MANUAL) BrandPrimary else CardBorderSubtle
            ),
            onClick = { onSelectEacMethod(EacMethod.MANUAL) },
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RadioButton(
                            selected = state.selectedEacMethod == EacMethod.MANUAL,
                            onClick = { onSelectEacMethod(EacMethod.MANUAL) }
                        )
                        Text("إضافة اختيارية: EAC يدوي (تقدير متبقٍ من القاعدة للقمة)", fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { onExplain(MetricId.EAC_MANUAL) }) {
                        Icon(Icons.Outlined.Info, contentDescription = "اشرح لي EAC يدوي", tint = EtcColor)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualEtcStr,
                        onValueChange = {
                            manualEtcStr = it
                            onUpdateManualEtc(it.toDoubleOrNull() ?: 0.0)
                        },
                        label = { Text("ETC يدوي ($unitLabel)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text("EAC = AC + ETC", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        LtrText(
                            text = "${EvmCalculator.formatMoney(snapshot.eacManual, p)} $unitLabel",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = EtcColor
                        )
                    }
                }
            }
        }

        // B. Derived Metrics
        Text("ب. المؤشرات المشتقة", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiMetricCard(
                metricId = MetricId.ETC,
                symbol = "ETC",
                titleAr = "المتبقي صرفه",
                valueText = "${EvmCalculator.formatMoney(snapshot.etc, p)} $unitLabel",
                meansAr = "فاضل نصرف كام لإنهاء المشروع؟",
                accentColor = EtcColor,
                showStatusPill = false,
                onExplain = onExplain,
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                metricId = MetricId.VAC,
                symbol = "VAC",
                titleAr = "الانحراف النهائي",
                valueText = "${EvmCalculator.formatMoney(snapshot.vac, p)} (${EvmCalculator.formatSignedPercent(snapshot.vacPercent)})",
                meansAr = "هنزيد أو نوفر كام عن BAC؟",
                accentColor = if ((snapshot.vac ?: 0.0) < 0) StatusBad else StatusGood,
                status = if ((snapshot.vac ?: 0.0) < 0) MetricStatus.BAD else MetricStatus.GOOD,
                onExplain = onExplain,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiMetricCard(
                metricId = MetricId.TCPI_BAC,
                symbol = "TCPI(BAC)",
                titleAr = "كفاءة للموازنة",
                valueText = EvmCalculator.formatIndex(snapshot.tcpiBac),
                meansAr = if (snapshot.bac - snapshot.ac <= 0) "تم استنفاد BAC بالكامل" else "الكفاءة المطلوبة للالتزام بـ BAC",
                accentColor = if (snapshot.realismStatus is RealismJudgment.Feasible) StatusGood else StatusBad,
                status = if (snapshot.realismStatus is RealismJudgment.Feasible) MetricStatus.GOOD else MetricStatus.BAD,
                onExplain = onExplain,
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                metricId = MetricId.TCPI_EAC,
                symbol = "TCPI(EAC)",
                titleAr = "كفاءة للتقدير الجديد",
                valueText = EvmCalculator.formatIndex(snapshot.tcpiEac),
                meansAr = "الكفاءة المطلوبة للالتزام بـ EAC",
                accentColor = EtcColor,
                showStatusPill = false,
                onExplain = onExplain,
                modifier = Modifier.weight(1f)
            )
        }

        // C. Budget Realism Judgment Card
        when (val judgment = snapshot.realismStatus) {
            is RealismJudgment.BudgetExhausted -> {
                AlertCardBanner(
                    title = "حكم واقعية الموازنة: تم استنفاد الموازنة",
                    message = "تم استنفاد الموازنة بالكامل (BAC − AC ≤ 0)؛ TCPI على أساس BAC غير قابل للحساب.",
                    isCritical = true,
                    icon = Icons.Outlined.Dangerous,
                    actionLabel = if (snapshot.activeEac != null) "أنشئ طلب تغيير" else null,
                    onAction = if (snapshot.activeEac != null) { { onCreateChangeRequestFromEac(snapshot.activeEac) } } else null
                )
            }
            is RealismJudgment.Feasible -> {
                AlertCardBanner(
                    title = "حكم واقعية الموازنة: ممكن",
                    message = "الالتزام بالموازنة الأصلية ممكن مع تحسين بسيط في الأداء (TCPI = ${EvmCalculator.formatIndex(judgment.tcpiBac)}، و CPI = ${EvmCalculator.formatIndex(judgment.cpi)}).",
                    isCritical = false,
                    icon = Icons.Outlined.CheckCircleOutline
                )
            }
            is RealismJudgment.Unrealistic -> {
                AlertCardBanner(
                    title = "حكم واقعية الموازنة: غير واقعي!",
                    message = "الالتزام بالموازنة الأصلية غير واقعي. المطلوب كفاءة ${EvmCalculator.formatIndex(judgment.tcpiBac)} والأداء الحالي ${EvmCalculator.formatIndex(judgment.cpi)}. يُنصح بإعادة التقدير ورفع طلب تغيير.",
                    isCritical = true,
                    icon = Icons.Outlined.ReportProblem,
                    actionLabel = "أنشئ طلب تغيير",
                    onAction = { onCreateChangeRequestFromEac(judgment.activeEac) }
                )
            }
            is RealismJudgment.NotCalculable -> {
                AlertCardBanner(
                    title = "حكم واقعية الموازنة",
                    message = "أدخل التكلفة الفعلية والقيمة المكتسبة لحساب واقعية الالتزام بالموازنة.",
                    isCritical = false,
                    icon = Icons.Outlined.Info
                )
            }
        }

        // D. Forecast Projection Chart
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
                Text("مسار التوقع المستقبلي (AC الممتد إلى EAC مقارنةً بـ BAC)", style = MaterialTheme.typography.titleMedium)
                EvmCurveChart(
                    pvSeries = snapshot.pvSeries,
                    evSeries = snapshot.evSeries,
                    acSeries = snapshot.acSeries,
                    forecastSeries = snapshot.forecastSeries,
                    bac = snapshot.bac,
                    statusMonth = snapshot.statusMonth
                )
            }
        }
    }
}

@Composable
private fun EacMethodCard(
    method: EacMethod,
    metricId: MetricId,
    valueText: String,
    whenToUse: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onExplain: (MetricId) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BrandPrimaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.5.dp, if (isSelected) BrandPrimary else CardBorderSubtle),
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(selected = isSelected, onClick = onSelect)
                    Text(method.labelAr, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { onExplain(metricId) }) {
                    Icon(Icons.Outlined.Info, contentDescription = "اشرح لي ${method.labelAr}", tint = BrandPrimary)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LtrText(text = method.formulaText, fontSize = 12.5.sp, color = TextSecondary)
                LtrText(text = valueText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
            }
            Text(
                text = "متى تُستخدم: $whenToUse",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

// =====================================================================
// 5.7 Change Requests Log (سجل طلبات التغيير)
// =====================================================================
@Composable
fun ChangeRequestsScreen(
    state: ProjectState,
    budget: BudgetBreakdown,
    prefilledCostImpact: Double?,
    onClearPrefilled: () -> Unit,
    onAddChangeRequest: (String, String, Double, Int, ChangeSource, String) -> Unit,
    onUpdateStatus: (String, ChangeStatus) -> Unit,
    onExplain: (MetricId) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = state.plan.roundingPrecision
    val unitLabel = "${state.setup.displayUnit.multiplierHint} ${state.setup.currency}".trim()
    var showAddDialog by remember(prefilledCostImpact) { mutableStateOf(prefilledCostImpact != null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PmbokSectionBanner(
            processNumber = "4.6 / 7.4",
            titleAr = "سجل طلبات التغيير",
            subtitleAr = "إدارة التغييرات على الخط المرجعي للتكلفة والاحتياطي الإداري",
            icon = Icons.Outlined.EditNote
        )

        // Fixed governance note
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF6FF)),
            border = BorderStroke(1.dp, EtcColor.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Gavel, contentDescription = null, tint = EtcColor)
                Text(
                    text = "إدارة التكلفة تكتشف الانحراف وتقترح الحل، أما اعتماد أي تعديل على الخط المرجعي فيتم عبر التحكم المتكامل في التغيير.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }

        // Current BAC vs Management Reserve Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiMetricCard(
                metricId = MetricId.BAC,
                symbol = "BAC",
                titleAr = "الخط المرجعي الحالي",
                valueText = "${EvmCalculator.formatMoney(budget.bac, p)} $unitLabel",
                meansAr = "يتحدث تلقائيًا عند اعتماد التغييرات",
                accentColor = PvColor,
                showStatusPill = false,
                onExplain = onExplain,
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                metricId = MetricId.MANAGEMENT_RESERVE,
                symbol = "MR",
                titleAr = "الاحتياطي الإداري المتاح",
                valueText = "${EvmCalculator.formatMoney(budget.currentManagementReserve, p)} $unitLabel",
                meansAr = "موازنة المشروع: ${EvmCalculator.formatMoney(budget.projectBudget, p)}",
                accentColor = ManagementReserveColor,
                showStatusPill = false,
                onExplain = onExplain,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("طلبات التغيير (${state.changeRequests.size})", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("add_change_request_btn")
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("طلب تغيير جديد")
            }
        }

        if (state.changeRequests.isEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, CardBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "لا توجد طلبات تغيير مسجلة حتى الآن.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(18.dp)
                )
            }
        } else {
            state.changeRequests.forEach { cr ->
                val statusCol = when (cr.status) {
                    ChangeStatus.SUBMITTED -> StatusWarning
                    ChangeStatus.APPROVED -> StatusGood
                    ChangeStatus.REJECTED -> StatusBad
                }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, statusCol.copy(alpha = 0.45f)),
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = BrandPrimaryContainer, shape = RoundedCornerShape(8.dp)) {
                                    LtrText(
                                        text = cr.code,
                                        color = BrandPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(cr.date, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                            Surface(color = statusCol.copy(alpha = 0.14f), shape = RoundedCornerShape(999.dp)) {
                                Text(
                                    text = cr.status.labelAr,
                                    color = statusCol,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(cr.description, style = MaterialTheme.typography.titleMedium)
                        Text("السبب: ${cr.reason}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LtrText(
                                text = "+${EvmCalculator.formatMoney(cr.costImpact, p)} $unitLabel",
                                color = BrandPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text("المصدر: ${cr.source.labelAr}", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                            Text("الجدول: ${cr.scheduleImpactDays} يوم", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }

                        if (cr.status == ChangeStatus.SUBMITTED) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onUpdateStatus(cr.id, ChangeStatus.APPROVED) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusGood),
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("approve_cr_${cr.code}")
                                ) {
                                    Text("اعتماد الطلب")
                                }
                                OutlinedButton(
                                    onClick = { onUpdateStatus(cr.id, ChangeStatus.REJECTED) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusBad),
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Text("رفض")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Baseline History
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
                Text("سجل نسخ الخط المرجعي (Baseline History)", style = MaterialTheme.typography.titleMedium)
                if (state.baselineHistory.isEmpty()) {
                    Text("لم تُحفظ نسخ تاريخية بعد.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                } else {
                    state.baselineHistory.reversed().forEach { rev ->
                        Surface(
                            color = PanelBackground,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("النسخة v${rev.version} (${rev.date})", fontWeight = FontWeight.Bold)
                                    LtrText(
                                        text = "BAC=${EvmCalculator.formatMoney(rev.bac, p)} | MR=${EvmCalculator.formatMoney(rev.managementReserve, p)}",
                                        color = BrandPrimary,
                                        fontSize = 12.5.sp
                                    )
                                }
                                Text(rev.note, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var desc by remember {
            mutableStateOf(
                if (prefilledCostImpact != null) "تعديل الخط المرجعي وفق توقع EAC" else ""
            )
        }
        var reason by remember {
            mutableStateOf(
                if (prefilledCostImpact != null) "انحراف مستمر في الأداء يتجاوز واقعية الموازنة الأصلية" else ""
            )
        }
        var costStr by remember {
            mutableStateOf(prefilledCostImpact?.toInt()?.toString() ?: "10")
        }
        var daysStr by remember { mutableStateOf("0") }
        var dateStr by remember { mutableStateOf("2026-05-15") }
        var source by remember { mutableStateOf(ChangeSource.MANAGEMENT_RESERVE) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                onClearPrefilled()
            },
            title = { Text("إنشاء طلب تغيير جديد") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("الوصف") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("السبب / المبرر") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = costStr,
                            onValueChange = { costStr = it },
                            label = { Text("التأثير على التكلفة") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = daysStr,
                            onValueChange = { daysStr = it.filter { c -> c.isDigit() || c == '-' } },
                            label = { Text("الجدول (أيام)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text("التاريخ") },
                        singleLine = true
                    )
                    Text("المصدر:", style = MaterialTheme.typography.labelLarge)
                    ChangeSource.entries.forEach { s ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = source == s, onClick = { source = s })
                            Text(s.labelAr)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddChangeRequest(
                        desc.ifBlank { "طلب تعديل تكلفة" },
                        reason.ifBlank { "تحديث تقديرات المشروع" },
                        costStr.toDoubleOrNull() ?: 0.0,
                        daysStr.toIntOrNull() ?: 0,
                        source,
                        dateStr.ifBlank { "2026-05-15" }
                    )
                    showAddDialog = false
                    onClearPrefilled()
                }) { Text("تقديم الطلب") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    onClearPrefilled()
                }) { Text("إلغاء") }
            }
        )
    }
}
