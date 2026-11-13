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
// 5.2 Cost Management Plan (7.1)
// =====================================================================
@Composable
fun CostManagementPlanScreen(
    plan: CostManagementPlan,
    onUpdatePlan: (CostManagementPlan) -> Unit,
    onAddUnit: (String) -> Unit,
    onRemoveUnit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var newUnitText by remember { mutableStateOf("") }
    var accMinStr by remember(plan) { mutableStateOf(plan.acceptableMin.toString()) }
    var accMaxStr by remember(plan) { mutableStateOf(plan.acceptableMax.toString()) }
    var warnMinStr by remember(plan) { mutableStateOf(plan.warningMin.toString()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PmbokSectionBanner(
            processNumber = "7.1",
            titleAr = "خطة إدارة التكاليف",
            subtitleAr = "قواعد القياس والدقة وحدود التحكم المؤثرة على كل الشاشات والتنبيهات",
            icon = Icons.Outlined.Rule
        )

        // Units of Measure
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
                Text("وحدات القياس المعتمدة للبنود", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newUnitText,
                        onValueChange = { newUnitText = it },
                        label = { Text("إضافة وحدة (مثل: مقطوعية)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            onAddUnit(newUnitText)
                            newUnitText = ""
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("إضافة")
                    }
                }
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    plan.unitsOfMeasure.forEach { u ->
                        InputChip(
                            selected = true,
                            onClick = { onRemoveUnit(u) },
                            label = { Text(u) },
                            trailingIcon = {
                                Icon(Icons.Outlined.Close, contentDescription = "حذف $u", modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }
        }

        // Precision & Reporting Frequency
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
                Text("مستوى الدقة (التقريب للعرض فقط)", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 10, 100, 1000).forEach { prec ->
                        FilterChip(
                            selected = plan.roundingPrecision == prec,
                            onClick = { onUpdatePlan(plan.copy(roundingPrecision = prec)) },
                            label = { LtrText(text = prec.toString(), fontSize = 13.sp) }
                        )
                    }
                }

                HorizontalDivider()

                Text("دورية التقارير", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportingFrequency.entries.forEach { freq ->
                        FilterChip(
                            selected = plan.reportingFrequency == freq,
                            onClick = { onUpdatePlan(plan.copy(reportingFrequency = freq)) },
                            label = { Text(freq.labelAr) }
                        )
                    }
                }
            }
        }

        // Control Thresholds
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("حدود التحكم (Control Thresholds لـ CPI و SPI)", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "أي تغيير هنا ينعكس فورًا على تلوين المؤشرات والتنبيهات في لوحة المشروع وضبط التكاليف.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = warnMinStr,
                        onValueChange = { warnMinStr = it },
                        label = { Text("حد التحذير الأدنى") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("threshold_warning_min_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = accMinStr,
                        onValueChange = { accMinStr = it },
                        label = { Text("أدنى المقبول") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("threshold_acceptable_min_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = accMaxStr,
                        onValueChange = { accMaxStr = it },
                        label = { Text("أعلى المقبول") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Live visual range bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = StatusBadContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        LtrText(
                            text = "< $warnMinStr (حرج)",
                            color = StatusBad,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Surface(
                        color = StatusWarningContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        LtrText(
                            text = "$warnMinStr–$accMinStr (تحذير)",
                            color = StatusWarning,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Surface(
                        color = StatusGoodContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        LtrText(
                            text = "≥ $accMinStr (جيد)",
                            color = StatusGood,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        val wMin = warnMinStr.toDoubleOrNull() ?: plan.warningMin
                        val aMin = accMinStr.toDoubleOrNull() ?: plan.acceptableMin
                        val aMax = accMaxStr.toDoubleOrNull() ?: plan.acceptableMax
                        onUpdatePlan(
                            plan.copy(
                                warningMin = wMin.coerceIn(0.1, aMin),
                                acceptableMin = aMin.coerceIn(wMin, aMax),
                                acceptableMax = aMax.coerceAtLeast(aMin)
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("save_plan_thresholds_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("تطبيق حدود التحكم")
                }
            }
        }

        // Default EAC Method
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
                Text("طريقة EAC الافتراضية", style = MaterialTheme.typography.titleMedium)
                listOf(EacMethod.ATYPICAL, EacMethod.TYPICAL, EacMethod.COST_AND_SCHEDULE).forEach { m ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (plan.defaultEacMethod == m) BrandPrimaryContainer else PanelBackground,
                        onClick = { onUpdatePlan(plan.copy(defaultEacMethod = m)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = plan.defaultEacMethod == m,
                                onClick = { onUpdatePlan(plan.copy(defaultEacMethod = m)) }
                            )
                            Column {
                                Text(m.labelAr, fontWeight = FontWeight.SemiBold)
                                LtrText(text = m.formulaText, fontSize = 12.sp, color = BrandPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
// 5.3 Cost Estimation (7.2)
// =====================================================================
@Composable
fun CostEstimateScreen(
    state: ProjectState,
    budget: BudgetBreakdown,
    onUpsertPackage: (WorkPackage) -> Unit,
    onDeletePackage: (String) -> Unit,
    onExplain: (MetricId) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingPackage by remember { mutableStateOf<WorkPackage?>(null) }
    var isNewDialog by remember { mutableStateOf(false) }
    val p = state.plan.roundingPrecision
    val unitLabel = "${state.setup.displayUnit.multiplierHint} ${state.setup.currency}".trim()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PmbokSectionBanner(
            processNumber = "7.2",
            titleAr = "تقدير التكاليف",
            subtitleAr = "بناء حزم العمل (WBS) باستخدام الطرق الأربع المعتمدة واحتياطي الطوارئ",
            icon = Icons.Outlined.Calculate
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "حزم العمل (${state.workPackages.size})",
                style = MaterialTheme.typography.titleLarge
            )
            Button(
                onClick = {
                    val nextNum = state.workPackages.size + 1
                    editingPackage = WorkPackage(
                        id = UUID.randomUUID().toString(),
                        code = "1.$nextNum",
                        name = "",
                        controlAccount = "CA-01",
                        endMonth = state.setup.durationMonths.coerceAtMost(3)
                    )
                    isNewDialog = true
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("إضافة حزمة عمل")
            }
        }

        if (state.workPackages.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, CardBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                    Text("لا توجد حزم عمل مضافة بعد", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "أضف حزمة عمل جديدة أو حمّل المثال الجاهز من الشريط العلوي.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        } else {
            state.workPackages.forEach { pkg ->
                val baseEst = EvmCalculator.calculatePackageBaseEstimate(pkg)
                val cont = EvmCalculator.calculatePackageContingency(pkg)
                val total = baseEst + cont
                val accBadgeColor = when (pkg.estimationMethod.accuracyLevel) {
                    AccuracyLevel.LOW -> StatusWarning
                    AccuracyLevel.MEDIUM -> EtcColor
                    AccuracyLevel.HIGH -> StatusGood
                }

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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = BrandPrimaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    LtrText(
                                        text = pkg.code,
                                        color = BrandPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Column {
                                    Text(pkg.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        pkg.controlAccount,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        editingPackage = pkg
                                        isNewDialog = false
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "تعديل ${pkg.name}", tint = BrandPrimary)
                                }
                                IconButton(
                                    onClick = { onDeletePackage(pkg.id) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "حذف ${pkg.name}", tint = StatusBad)
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = PanelBackground,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "الطريقة: ${pkg.estimationMethod.labelAr}",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = accBadgeColor.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "الدقة: ${pkg.estimationMethod.accuracyLabelAr}",
                                    color = accBadgeColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            if (pkg.estimationMethod == EstimationMethod.THREE_POINT) {
                                val sigma = EvmCalculator.calculateThreePointSigma(pkg.cO, pkg.cP)
                                Surface(
                                    color = EtcColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp),
                                    onClick = { onExplain(MetricId.THREE_POINT_SIGMA) }
                                ) {
                                    LtrText(
                                        text = "σ = ${EvmCalculator.formatIndex(sigma)}",
                                        color = EtcColor,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("التقدير الأساسي", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                LtrText(text = "${EvmCalculator.formatMoney(baseEst, p)} $unitLabel", fontSize = 14.sp)
                            }
                            Column {
                                Text("احتياطي الطوارئ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                LtrText(text = "${EvmCalculator.formatMoney(cont, p)} $unitLabel", fontSize = 14.sp, color = ContingencyReserveColor)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("إجمالي الحزمة", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                LtrText(text = "${EvmCalculator.formatMoney(total, p)} $unitLabel", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                            }
                        }

                        if (pkg.associatedRisk.isNotBlank()) {
                            Text(
                                text = "الخطر المرتبط: ${pkg.associatedRisk}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Totals Footer Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandPrimaryContainer),
            border = BorderStroke(1.5.dp, BrandPrimary),
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
                    Text("إجمالي تقديرات التكاليف", style = MaterialTheme.typography.titleMedium, color = OnBrandPrimaryContainer)
                    ExplainInlineButton(metricId = MetricId.CONTINGENCY, label = "اشرح الطوارئ", onExplain = onExplain)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("مجموع التقديرات", style = MaterialTheme.typography.labelSmall)
                        LtrText(text = "${EvmCalculator.formatMoney(budget.activityEstimatesSum, p)} $unitLabel", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("مجموع احتياطي الطوارئ", style = MaterialTheme.typography.labelSmall)
                        LtrText(text = "${EvmCalculator.formatMoney(budget.activityContingencySum, p)} $unitLabel", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ContingencyReserveColor)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("إجمالي الحزم", style = MaterialTheme.typography.labelSmall)
                        LtrText(text = "${EvmCalculator.formatMoney(budget.workPackagesTotal, p)} $unitLabel", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                    }
                }
            }
        }
    }

    editingPackage?.let { pkgToEdit ->
        WorkPackageEditorDialog(
            initial = pkgToEdit,
            isNew = isNewDialog,
            units = state.plan.unitsOfMeasure,
            onDismiss = { editingPackage = null },
            onSave = { saved ->
                onUpsertPackage(saved)
                editingPackage = null
            }
        )
    }
}

@Composable
private fun WorkPackageEditorDialog(
    initial: WorkPackage,
    isNew: Boolean,
    units: List<String>,
    onDismiss: () -> Unit,
    onSave: (WorkPackage) -> Unit
) {
    var code by remember { mutableStateOf(initial.code) }
    var name by remember { mutableStateOf(initial.name) }
    var controlAccount by remember { mutableStateOf(initial.controlAccount) }
    var method by remember { mutableStateOf(initial.estimationMethod) }

    // Method fields
    var priorCost by remember { mutableStateOf(initial.analogousPriorCost.toString()) }
    var adjPct by remember { mutableStateOf(initial.analogousAdjustmentPct.toString()) }

    var qty by remember { mutableStateOf(initial.paramQuantity.toString()) }
    var unitPrice by remember { mutableStateOf(initial.paramUnitPrice.toString()) }
    var paramUnit by remember { mutableStateOf(initial.paramUnit.ifBlank { units.firstOrNull() ?: "م³" }) }

    var bottomItems by remember {
        mutableStateOf(
            initial.bottomUpItems.ifEmpty {
                listOf(
                    BottomUpItem(UUID.randomUUID().toString(), "عمالة", "أعمال عمالة", 0.0),
                    BottomUpItem(UUID.randomUUID().toString(), "مواد", "توريد مواد", 0.0),
                    BottomUpItem(UUID.randomUUID().toString(), "معدات", "تشغيل معدات", 0.0)
                )
            }
        )
    }

    var cO by remember { mutableStateOf(initial.cO.toString()) }
    var cM by remember { mutableStateOf(initial.cM.toString()) }
    var cP by remember { mutableStateOf(initial.cP.toString()) }
    var dist by remember { mutableStateOf(initial.threePointDist) }

    var contMode by remember { mutableStateOf(initial.contingencyMode) }
    var contVal by remember { mutableStateOf(initial.contingencyValue.toString()) }
    var risk by remember { mutableStateOf(initial.associatedRisk) }

    val previewEstimate = when (method) {
        EstimationMethod.ANALOGOUS -> EvmCalculator.calculateAnalogous(
            priorCost.toDoubleOrNull() ?: 0.0,
            adjPct.toDoubleOrNull() ?: 0.0
        )
        EstimationMethod.PARAMETRIC -> EvmCalculator.calculateParametric(
            qty.toDoubleOrNull() ?: 0.0,
            unitPrice.toDoubleOrNull() ?: 0.0
        )
        EstimationMethod.BOTTOM_UP -> EvmCalculator.calculateBottomUp(bottomItems)
        EstimationMethod.THREE_POINT -> {
            val o = cO.toDoubleOrNull() ?: 0.0
            val m = cM.toDoubleOrNull() ?: 0.0
            val pVal = cP.toDoubleOrNull() ?: 0.0
            if (dist == ThreePointDistribution.TRIANGULAR) {
                EvmCalculator.calculateThreePointTriangular(o, m, pVal)
            } else {
                EvmCalculator.calculateThreePointBeta(o, m, pVal)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "إضافة حزمة عمل" else "تعديل حزمة العمل") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("الكود") },
                        modifier = Modifier.weight(0.35f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الحزمة") },
                        modifier = Modifier.weight(0.65f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = controlAccount,
                    onValueChange = { controlAccount = it },
                    label = { Text("حساب المراقبة (Control Account)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("طريقة التقدير", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    EstimationMethod.entries.forEach { m ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(selected = method == m, onClick = { method = m })
                            Text("${m.labelAr} (دقة ${m.accuracyLabelAr})")
                        }
                    }
                }

                when (method) {
                    EstimationMethod.ANALOGOUS -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = priorCost,
                                onValueChange = { priorCost = it },
                                label = { Text("تكلفة مشروع سابق") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = adjPct,
                                onValueChange = { adjPct = it },
                                label = { Text("معامل التعديل %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                    EstimationMethod.PARAMETRIC -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = qty,
                                onValueChange = { qty = it },
                                label = { Text("الكمية") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = unitPrice,
                                onValueChange = { unitPrice = it },
                                label = { Text("سعر الوحدة") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        OutlinedTextField(
                            value = paramUnit,
                            onValueChange = { paramUnit = it },
                            label = { Text("وحدة القياس") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    EstimationMethod.BOTTOM_UP -> {
                        Text("بنود التكلفة التفصيلية:", style = MaterialTheme.typography.labelLarge)
                        bottomItems.forEachIndexed { idx, item ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = item.category,
                                    onValueChange = { v ->
                                        bottomItems = bottomItems.toMutableList().also { it[idx] = item.copy(category = v) }
                                    },
                                    label = { Text("الفئة") },
                                    modifier = Modifier.weight(0.42f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = item.amount.toString(),
                                    onValueChange = { v ->
                                        bottomItems = bottomItems.toMutableList().also {
                                            it[idx] = item.copy(amount = v.toDoubleOrNull() ?: 0.0)
                                        }
                                    },
                                    label = { Text("المبلغ") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(0.42f),
                                    singleLine = true
                                )
                                IconButton(
                                    onClick = {
                                        if (bottomItems.size > 1) {
                                            bottomItems = bottomItems.filterIndexed { i, _ -> i != idx }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Outlined.RemoveCircleOutline, contentDescription = "حذف بند")
                                }
                            }
                        }
                        TextButton(onClick = {
                            bottomItems = bottomItems + BottomUpItem(UUID.randomUUID().toString(), "بند إضافي", "", 0.0)
                        }) {
                            Text("+ إضافة بند فرعي")
                        }
                    }
                    EstimationMethod.THREE_POINT -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = cO,
                                onValueChange = { cO = it },
                                label = { Text("cO المتفائل") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = cM,
                                onValueChange = { cM = it },
                                label = { Text("cM الأرجح") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = cP,
                                onValueChange = { cP = it },
                                label = { Text("cP المتشائم") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThreePointDistribution.entries.forEach { d ->
                                FilterChip(
                                    selected = dist == d,
                                    onClick = { dist = d },
                                    label = { Text(d.labelAr) }
                                )
                            }
                        }
                        val sigma = EvmCalculator.calculateThreePointSigma(
                            cO.toDoubleOrNull() ?: 0.0,
                            cP.toDoubleOrNull() ?: 0.0
                        )
                        LtrText(
                            text = "σ = (cP − cO)/6 = ${EvmCalculator.formatIndex(sigma)}",
                            color = EtcColor,
                            fontSize = 13.sp
                        )
                    }
                }

                Surface(color = PanelBackground, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("التقدير المحسوب:", fontWeight = FontWeight.Bold)
                        LtrText(text = EvmCalculator.formatExactMoney(previewEstimate), color = BrandPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider()
                Text("احتياطي الطوارئ والمخاطر", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReserveMode.entries.forEach { rm ->
                        FilterChip(
                            selected = contMode == rm,
                            onClick = { contMode = rm },
                            label = { Text(rm.labelAr) }
                        )
                    }
                }
                OutlinedTextField(
                    value = contVal,
                    onValueChange = { contVal = it },
                    label = { Text(if (contMode == ReserveMode.PERCENT) "نسبة الطوارئ %" else "مبلغ الطوارئ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = risk,
                    onValueChange = { risk = it },
                    label = { Text("الخطر المرتبط (اختياري)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    initial.copy(
                        code = code.ifBlank { "1.x" },
                        name = name.ifBlank { "حزمة عمل" },
                        controlAccount = controlAccount.ifBlank { "CA-01" },
                        estimationMethod = method,
                        analogousPriorCost = priorCost.toDoubleOrNull() ?: 0.0,
                        analogousAdjustmentPct = adjPct.toDoubleOrNull() ?: 0.0,
                        paramQuantity = qty.toDoubleOrNull() ?: 0.0,
                        paramUnitPrice = unitPrice.toDoubleOrNull() ?: 0.0,
                        paramUnit = paramUnit,
                        bottomUpItems = bottomItems,
                        cO = cO.toDoubleOrNull() ?: 0.0,
                        cM = cM.toDoubleOrNull() ?: 0.0,
                        cP = cP.toDoubleOrNull() ?: 0.0,
                        threePointDist = dist,
                        contingencyMode = contMode,
                        contingencyValue = contVal.toDoubleOrNull() ?: 0.0,
                        associatedRisk = risk
                    )
                )
            }) {
                Text("حفظ الحزمة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
