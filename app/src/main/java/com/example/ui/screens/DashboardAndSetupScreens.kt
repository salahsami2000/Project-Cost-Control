package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    state: ProjectState,
    snapshot: EvmSnapshot,
    budget: BudgetBreakdown,
    onLoadSample: () -> Unit,
    onOpenSetup: () -> Unit,
    onOpenChangeRequests: () -> Unit,
    onExplain: (MetricId) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = state.plan.roundingPrecision
    val unitLabel = "${state.setup.displayUnit.multiplierHint} ${state.setup.currency}".trim()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!state.setup.isInitialized && state.workPackages.isEmpty() && state.quickEntries.isEmpty()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = BrandPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "مرحبًا بك في إدارة تكلفة المشروع (PMBOK® 6)",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = "ابدأ بإدخال بيانات مشروعك الإنشائي خطوة بخطوة، أو حمّل مثال «عمارة سكنية» الجاهز لاستكشاف الحسابات والرسوم وميزة «اشرح لي» فورًا.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onLoadSample,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ManagementReserveColor,
                                contentColor = Color(0xFF2B1800)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("welcome_load_sample_btn")
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تحميل مثال جاهز", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onOpenSetup,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("إعداد مشروع جديد", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Executive 1-sentence summary
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, BrandPrimary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandPrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Analytics, contentDescription = null, tint = BrandPrimary)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "ملخص الحالة — الشهر ${snapshot.statusMonth} من ${state.setup.durationMonths}",
                        style = MaterialTheme.typography.labelLarge,
                        color = BrandPrimary
                    )
                    Text(
                        text = snapshot.summarySentenceAr,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.testTag("dashboard_summary_text")
                    )
                }
            }
        }

        // Alerts section
        if (snapshot.cpiStatus == MetricStatus.BAD || snapshot.spiStatus == MetricStatus.BAD) {
            AlertCardBanner(
                title = "تنبيه: تجاوز حدود التحكم (Control Thresholds)",
                message = "مؤشر الأداء (${if (snapshot.cpiStatus == MetricStatus.BAD) "CPI=${EvmCalculator.formatIndex(snapshot.cpi)} " else ""}${if (snapshot.spiStatus == MetricStatus.BAD) "SPI=${EvmCalculator.formatIndex(snapshot.spi)}" else ""}) أقل من حد التحذير (${EvmCalculator.formatIndex(state.plan.warningMin)}). يُنصح بمراجعة حزم العمل المتعثرة.",
                isCritical = true,
                icon = Icons.Outlined.ReportProblem
            )
        }
        if (snapshot.fundingShortfallMonths.isNotEmpty()) {
            AlertCardBanner(
                title = "تنبيه تسوية حدود التمويل",
                message = "الإنفاق المخطط يتجاوز التمويل المتاح في الأشهر: ${snapshot.fundingShortfallMonths.joinToString("، ") { "الشهر $it" }}. أعد جدولة الأعمال أو قدّم دفعات التمويل.",
                isCritical = true,
                icon = Icons.Outlined.AccountBalanceWallet
            )
        }
        if (budget.reserveConsumedRatio > 0.5) {
            AlertCardBanner(
                title = "تنبيه استهلاك الاحتياطي الإداري",
                message = "تم استهلاك ${(budget.reserveConsumedRatio * 100).toInt()}% من الاحتياطي الإداري عبر طلبات التغيير المعتمدة.",
                isCritical = false,
                icon = Icons.Outlined.Savings,
                actionLabel = "مراجعة سجل التغيير",
                onAction = onOpenChangeRequests
            )
        }

        // Top KPI Grid (Adaptive 2-col or 1-col)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiMetricCard(
                    metricId = MetricId.BAC,
                    symbol = "BAC",
                    titleAr = "الخط المرجعي",
                    valueText = "${EvmCalculator.formatMoney(snapshot.bac, p)} $unitLabel",
                    meansAr = "إجمالي الموازنة المعتمدة للعمل",
                    accentColor = PvColor,
                    showStatusPill = false,
                    onExplain = onExplain,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    metricId = MetricId.PROJECT_BUDGET,
                    symbol = "Budget",
                    titleAr = "موازنة المشروع",
                    valueText = "${EvmCalculator.formatMoney(snapshot.projectBudget, p)} $unitLabel",
                    meansAr = "BAC + الاحتياطي الإداري (${EvmCalculator.formatMoney(snapshot.managementReserve, p)})",
                    accentColor = ManagementReserveColor,
                    showStatusPill = false,
                    onExplain = onExplain,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiMetricCard(
                    metricId = MetricId.AC,
                    symbol = "AC",
                    titleAr = "التكلفة الفعلية",
                    valueText = "${EvmCalculator.formatMoney(snapshot.ac, p)} $unitLabel",
                    meansAr = "ما تم صرفه فعليًا حتى الشهر ${snapshot.statusMonth}",
                    accentColor = AcColor,
                    showStatusPill = false,
                    onExplain = onExplain,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    metricId = MetricId.EAC,
                    symbol = "EAC",
                    titleAr = "التقدير النهائي",
                    valueText = "${EvmCalculator.formatMoney(snapshot.activeEac, p)} $unitLabel",
                    meansAr = "التكلفة المتوقعة عند اكتمال المشروع",
                    accentColor = EtcColor,
                    status = snapshot.cpiStatus,
                    onExplain = onExplain,
                    modifier = Modifier.weight(1f)
                )
            }
            KpiMetricCard(
                metricId = MetricId.VAC,
                symbol = "VAC",
                titleAr = "الانحراف المتوقع عند الاكتمال",
                valueText = "${EvmCalculator.formatMoney(snapshot.vac, p)} $unitLabel (${EvmCalculator.formatSignedPercent(snapshot.vacPercent)})",
                meansAr = if ((snapshot.vac ?: 0.0) < 0) "عجز متوقع عن الموازنة المعتمدة" else "وفر متوقع عند انتهاء المشروع",
                accentColor = if ((snapshot.vac ?: 0.0) < 0) StatusBad else StatusGood,
                status = if ((snapshot.vac ?: 0.0) < 0) MetricStatus.BAD else MetricStatus.GOOD,
                onExplain = onExplain
            )
        }

        // CPI & SPI Gauges Card
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
                    text = "مؤشرات الأداء (تُلوَّن وفق حدود التحكم 7.1)",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IndexGaugeMeter(
                            symbol = "CPI",
                            titleAr = "أداء التكلفة",
                            value = snapshot.cpi,
                            plan = state.plan,
                            status = snapshot.cpiStatus,
                            modifier = Modifier.testTag("dashboard_cpi_gauge")
                        )
                        ExplainInlineButton(
                            metricId = MetricId.CPI,
                            tint = statusColor(snapshot.cpiStatus),
                            onExplain = onExplain
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IndexGaugeMeter(
                            symbol = "SPI",
                            titleAr = "أداء الجدول",
                            value = snapshot.spi,
                            plan = state.plan,
                            status = snapshot.spiStatus,
                            modifier = Modifier.testTag("dashboard_spi_gauge")
                        )
                        ExplainInlineButton(
                            metricId = MetricId.SPI,
                            tint = statusColor(snapshot.spiStatus),
                            onExplain = onExplain
                        )
                    }
                }
            }
        }

        // Main EVM S-Curve Chart
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "منحنيات القيمة المكتسبة والتوقع (PV / EV / AC / EAC)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    ExplainInlineButton(
                        metricId = MetricId.EV,
                        label = "اشرح المنحنى",
                        onExplain = onExplain
                    )
                }
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

        // Worst 3 Work Packages
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
                    text = "أسوأ 3 حزم عمل (حسب انحراف التكلفة CV)",
                    style = MaterialTheme.typography.titleMedium
                )
                if (snapshot.worstThreePackages.isEmpty()) {
                    Text(
                        text = "لم تُسجَّل تكاليف فعلية لحزم العمل بعد.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                } else {
                    snapshot.worstThreePackages.forEachIndexed { idx, row ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = statusContainerColor(row.cpiStatus).copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, statusColor(row.cpiStatus).copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${idx + 1}. ${row.pkg.code} — ${row.pkg.name}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "الإنجاز ${row.percentComplete.toInt()}% • الموازنة ${EvmCalculator.formatMoney(row.budget, p)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    LtrText(
                                        text = "CV = ${EvmCalculator.formatMoney(row.cv, p)}",
                                        color = if (row.cv < 0) StatusBad else StatusGood,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    LtrText(
                                        text = "CPI = ${EvmCalculator.formatIndex(row.cpi)}",
                                        color = statusColor(row.cpiStatus),
                                        fontSize = 12.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSetupScreen(
    setup: ProjectSetup,
    onSave: (ProjectSetup) -> Unit,
    onLoadSample: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember(setup) { mutableStateOf(setup.name) }
    var type by remember(setup) { mutableStateOf(setup.type) }
    var currency by remember(setup) { mutableStateOf(setup.currency) }
    var displayUnit by remember(setup) { mutableStateOf(setup.displayUnit) }
    var startDate by remember(setup) { mutableStateOf(setup.startDate) }
    var durationStr by remember(setup) { mutableStateOf(setup.durationMonths.toString()) }
    var statusMonthStr by remember(setup) { mutableStateOf(setup.statusDateMonth.toString()) }
    var inputMode by remember(setup) { mutableStateOf(setup.inputMode) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PmbokSectionBanner(
            processNumber = null,
            titleAr = "إعداد المشروع",
            subtitleAr = "تعريف البيانات الأساسية للمشروع ونمط الإدخال (تفصيلي أو سريع)",
            icon = Icons.Outlined.SettingsApplications
        )

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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المشروع") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("نوع المشروع", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProjectType.entries.take(3).forEach { pt ->
                        FilterChip(
                            selected = type == pt,
                            onClick = { type = pt },
                            label = { Text(pt.labelAr) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProjectType.entries.drop(3).forEach { pt ->
                        FilterChip(
                            selected = type == pt,
                            onClick = { type = pt },
                            label = { Text(pt.labelAr) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it },
                        label = { Text("العملة") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("تاريخ البداية") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Text("وحدة العرض", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DisplayUnit.entries.forEach { du ->
                        FilterChip(
                            selected = displayUnit == du,
                            onClick = { displayUnit = du },
                            label = { Text(du.labelAr) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = durationStr,
                        onValueChange = { durationStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("المدة (بالشهور)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = statusMonthStr,
                        onValueChange = { statusMonthStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("شهر المتابعة الحالي") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Text("وضع الإدخال", style = MaterialTheme.typography.labelLarge)
                InputMode.entries.forEach { mode ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (inputMode == mode) BrandPrimaryContainer else PanelBackground,
                        border = BorderStroke(
                            1.dp,
                            if (inputMode == mode) BrandPrimary else CardBorderSubtle
                        ),
                        onClick = { inputMode = mode },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = inputMode == mode,
                                onClick = { inputMode = mode }
                            )
                            Column {
                                Text(mode.labelAr, fontWeight = FontWeight.Bold)
                                Text(
                                    mode.descriptionAr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val dur = (durationStr.toIntOrNull() ?: 10).coerceIn(1, 120)
                            val stat = (statusMonthStr.toIntOrNull() ?: 1).coerceIn(1, dur)
                            onSave(
                                setup.copy(
                                    name = name.ifBlank { "مشروع بناء" },
                                    type = type,
                                    currency = currency.ifBlank { "دولار" },
                                    displayUnit = displayUnit,
                                    startDate = startDate.ifBlank { "2026-01-01" },
                                    durationMonths = dur,
                                    statusDateMonth = stat,
                                    inputMode = inputMode,
                                    isInitialized = true
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("حفظ الإعدادات", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onLoadSample,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("تحميل مثال جاهز")
                    }
                }
            }
        }
    }
}
