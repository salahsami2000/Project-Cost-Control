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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.*
import com.example.ui.components.*
import com.example.ui.theme.*

private enum class CalculatorTab(val titleAr: String) {
    EVM("معادلات EVM والتنبؤ"),
    ESTIMATION("تقدير التكاليف (PERT)"),
    BUDGET("الموازنة والاحتياطيات")
}

private data class BaladiFieldTip(
    val title: String,
    val baladiMeaning: String,
    val example: String
)

@Composable
fun CalculatorScreen(
    state: ProjectState,
    snapshot: EvmSnapshot,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(CalculatorTab.EVM) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PmbokSectionBanner(
            processNumber = null,
            titleAr = "حاسبة المعادلات الهندسية",
            subtitleAr = "اكتب أرقامك وهتلاقي شرح بالبلدي لكل خانة بتكتبها ولكل نتيجة بتطلع فورًا",
            icon = Icons.Outlined.Functions
        )

        // Mode Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CalculatorTab.entries.forEach { tab ->
                FilterChip(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    label = {
                        Text(
                            text = tab.titleAr,
                            fontSize = 12.sp,
                            fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        when (activeTab) {
            CalculatorTab.EVM -> EvmEquationsCalculatorTab(state = state, snapshot = snapshot)
            CalculatorTab.ESTIMATION -> EstimationCalculatorTab()
            CalculatorTab.BUDGET -> BudgetReservesCalculatorTab()
        }
    }
}

@Composable
private fun ActiveFieldBaladiCard(tip: BaladiFieldTip) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8EB)),
        border = BorderStroke(1.2.dp, ManagementReserveColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("baladi_active_tip_card")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Lightbulb,
                contentDescription = null,
                tint = Color(0xFFB45309)
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "بتكتب دلوقتي في: ${tip.title}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF92400E),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tip.baladiMeaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "مثال بسيط: ${tip.example}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun BaladiInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    baladiHint: String,
    tip: BaladiFieldTip,
    onFocused: (BaladiFieldTip) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = {
            Text(
                text = baladiHint,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier.onFocusChanged { focusState ->
            if (focusState.isFocused) onFocused(tip)
        }
    )
}

@Composable
private fun EvmEquationsCalculatorTab(
    state: ProjectState,
    snapshot: EvmSnapshot
) {
    var bacStr by remember { mutableStateOf("200") }
    var pvStr by remember { mutableStateOf("100") }
    var evStr by remember { mutableStateOf("80") }
    var acStr by remember { mutableStateOf("120") }
    var manualEtcStr by remember { mutableStateOf("180") }

    val bacTip = remember {
        BaladiFieldTip(
            title = "BAC (الموازنة عند الاكتمال)",
            baladiMeaning = "بالبلدي: دي الميزانية الكلية المتفق عليها من أول يوم عشان نخلص المشروع كله من الألف للياء.",
            example = "لو المقاول متفق يبني العمارة بميزانية 200 ألف، يبقى BAC = 200."
        )
    }
    val pvTip = remember {
        BaladiFieldTip(
            title = "PV (القيمة المخططة)",
            baladiMeaning = "بالبلدي: قيمة الشغل اللي كان المفروض نكون خلصناه لحد النهارده حسب الجدول الزمني على الورق.",
            example = "احنا في نص المدة والمفروض نكون خلصنا نص المشروع، يبقى PV = 100."
        )
    }
    val evTip = remember {
        BaladiFieldTip(
            title = "EV (القيمة المكتسبة)",
            baladiMeaning = "بالبلدي: الشغل اللي خلص فعلًا على الأرض لحد دلوقتي يساوي كام بأسعار الميزانية الأصلية (مش اللي اتصرف عليه).",
            example = "خلصنا 40% بس من عمارة ميزانيتها 200، يبقى EV = 80."
        )
    }
    val acTip = remember {
        BaladiFieldTip(
            title = "AC (التكلفة الفعلية)",
            baladiMeaning = "بالبلدي: الفلوس الحقيقية اللي طلعت من جيبنا واتدفعت فواتير ومستخلصات وعمالة لحد النهارده.",
            example = "دفعنا فواتير ومقاولين بمجموع 120 ألف لحد دلوقتي، يبقى AC = 120."
        )
    }
    val etcTip = remember {
        BaladiFieldTip(
            title = "ETC يدوي (التكلفة المتبقية التقديرية)",
            baladiMeaning = "بالبلدي: لو سعّرت الباقي من الشغل من تاني بأسعار السوق النهارده، لسة فاضل نصرف كام عشان نسلم المشروع؟",
            example = "لو لسة محتاجين 180 ألف عشان نقفل التشطيبات، اكتب 180."
        )
    }

    var activeTip by remember { mutableStateOf(bacTip) }

    val bac = bacStr.toDoubleOrNull() ?: 0.0
    val pv = pvStr.toDoubleOrNull() ?: 0.0
    val ev = evStr.toDoubleOrNull() ?: 0.0
    val ac = acStr.toDoubleOrNull() ?: 0.0
    val manualEtc = manualEtcStr.toDoubleOrNull() ?: 0.0

    val cv = EvmCalculator.cv(ev, ac)
    val sv = EvmCalculator.sv(ev, pv)
    val cpi = EvmCalculator.cpi(ev, ac)
    val spi = EvmCalculator.spi(ev, pv)

    val eac1 = EvmCalculator.eacAtypical(ac, bac, ev)
    val eac2 = EvmCalculator.eacTypical(bac, ev, ac)
    val eac3 = EvmCalculator.eacCostAndSchedule(ac, bac, ev, pv)
    val eac4 = EvmCalculator.eacManual(ac, manualEtc)

    val etcVal = eac2?.let { EvmCalculator.etc(it, ac) }
    val vacVal = eac2?.let { EvmCalculator.vac(bac, it) }
    val vacPct = eac2?.let { EvmCalculator.vacPercent(bac, it) }
    val tcpiBac = EvmCalculator.tcpiBac(bac, ev, ac)
    val tcpiEac = eac2?.let { EvmCalculator.tcpiEac(bac, ev, it, ac) }

    val cpiStatus = EvmCalculator.evaluateIndexStatus(cpi, state.plan)
    val spiStatus = EvmCalculator.evaluateIndexStatus(spi, state.plan)

    // Quick Presets
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = {
                bacStr = EvmCalculator.formatExactMoney(snapshot.bac).replace(",", "")
                pvStr = EvmCalculator.formatExactMoney(snapshot.pv).replace(",", "")
                evStr = EvmCalculator.formatExactMoney(snapshot.ev).replace(",", "")
                acStr = EvmCalculator.formatExactMoney(snapshot.ac).replace(",", "")
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("سحب أرقام مشروعي", fontSize = 12.sp)
        }
        OutlinedButton(
            onClick = {
                bacStr = "200"
                pvStr = "100"
                evStr = "80"
                acStr = "120"
                manualEtcStr = "180"
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Outlined.Science, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("مثال الاختبار (200/80/120)", fontSize = 12.sp)
        }
    }

    // Active field live Baladi explanation banner
    ActiveFieldBaladiCard(tip = activeTip)

    // Inputs Card with inline Baladi explanations under every input
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, CardBorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("أدخل القيم (اضغط على أي خانة لشرح أكبر)", style = MaterialTheme.typography.titleMedium)

            BaladiInputField(
                value = bacStr,
                onValueChange = { bacStr = it },
                label = "BAC — الموازنة عند الاكتمال",
                baladiHint = "بالبلدي: الميزانية الكلية المتفق عليها عشان نخلص المشروع كله.",
                tip = bacTip,
                onFocused = { activeTip = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calc_input_bac")
            )

            BaladiInputField(
                value = pvStr,
                onValueChange = { pvStr = it },
                label = "PV — القيمة المخططة",
                baladiHint = "بالبلدي: المفروض نكون خلصنا شغل بكام لحد النهارده حسب الجدول.",
                tip = pvTip,
                onFocused = { activeTip = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calc_input_pv")
            )

            BaladiInputField(
                value = evStr,
                onValueChange = { evStr = it },
                label = "EV — القيمة المكتسبة",
                baladiHint = "بالبلدي: الشغل اللي خلص فعلًا على الأرض لحد دلوقتي يساوي كام بالميزانية الأصلية.",
                tip = evTip,
                onFocused = { activeTip = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calc_input_ev")
            )

            BaladiInputField(
                value = acStr,
                onValueChange = { acStr = it },
                label = "AC — التكلفة الفعلية",
                baladiHint = "بالبلدي: الفلوس اللي اتدفعت فعلًا من جيبنا (فواتير وعمالة ومواد) لحد النهارده.",
                tip = acTip,
                onFocused = { activeTip = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calc_input_ac")
            )

            BaladiInputField(
                value = manualEtcStr,
                onValueChange = { manualEtcStr = it },
                label = "ETC يدوي — المتبقي بتقدير جديد (اختياري)",
                baladiHint = "بالبلدي: لو قدّرنا الشغل الباقي من جديد، لسة فاضل نصرف كام لحد التسليم؟",
                tip = etcTip,
                onFocused = { activeTip = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Results: Variances & Indices
    Text("1. الانحرافات ومؤشرات الأداء (مع التفسير بالبلدي)", style = MaterialTheme.typography.titleMedium)

    FormulaResultCard(
        symbol = "CV",
        titleAr = "انحراف التكلفة",
        formula = "CV = EV − AC",
        substitution = "CV = ${fmt(ev)} − ${fmt(ac)} = ${EvmCalculator.formatExactMoney(cv)}",
        result = EvmCalculator.formatExactMoney(cv),
        baladiExplain = when {
            cv > 1e-6 -> "بالبلدي: وفرنا ${EvmCalculator.formatExactMoney(cv)} عن الميزانية في الشغل اللي خلص؛ ماشيين كويس في الصرف."
            cv < -1e-6 -> "بالبلدي: صرفنا أكتر من حق الشغل اللي خلص بـ ${EvmCalculator.formatExactMoney(-cv)}؛ يعني خسرانين في التكلفة لحد دلوقتي."
            else -> "بالبلدي: صرفنا قد حق الشغل اللي خلص بالظبط، ولا زيادة ولا نقص."
        },
        accent = if (cv >= 0) StatusGood else StatusBad
    )
    FormulaResultCard(
        symbol = "SV",
        titleAr = "انحراف الجدول",
        formula = "SV = EV − PV",
        substitution = "SV = ${fmt(ev)} − ${fmt(pv)} = ${EvmCalculator.formatExactMoney(sv)}",
        result = EvmCalculator.formatExactMoney(sv),
        baladiExplain = when {
            sv > 1e-6 -> "بالبلدي: سابقين الجدول الزمني بشغل قيمته ${EvmCalculator.formatExactMoney(sv)}."
            sv < -1e-6 -> "بالبلدي: متأخرين عن الجدول الزمني بشغل قيمته ${EvmCalculator.formatExactMoney(-sv)} كان المفروض يكون خلص."
            else -> "بالبلدي: ماشيين على الجدول الزمني بالمللي."
        },
        accent = if (sv >= 0) StatusGood else StatusWarning
    )
    FormulaResultCard(
        symbol = "CPI",
        titleAr = "مؤشر أداء التكلفة",
        formula = "CPI = EV / AC",
        substitution = if (ac == 0.0) "AC = 0 (القسمة على صفر غير معرفة)" else "CPI = ${fmt(ev)} / ${fmt(ac)} = ${EvmCalculator.formatIndex(cpi)}",
        result = EvmCalculator.formatIndex(cpi),
        baladiExplain = if (cpi == null) {
            "بالبلدي: لسة ما صرفناش أي فلوس (AC = 0)، فمش قادرين نحكم على كفاءة الصرف."
        } else if (cpi >= 1.0) {
            "بالبلدي: كل وحدة فلوس بتدفعها بتطلع لك شغل بقيمة ${EvmCalculator.formatIndex(cpi)} — يعني إيدك خفيفة وموفر."
        } else {
            "بالبلدي: كل 1 ${state.setup.currency} بتصرفه بيطلع شغل بـ ${EvmCalculator.formatIndex(cpi)} بس — الباقي ضايع في هدر أو غلاء!"
        },
        accent = statusColor(cpiStatus)
    )
    FormulaResultCard(
        symbol = "SPI",
        titleAr = "مؤشر أداء الجدول",
        formula = "SPI = EV / PV",
        substitution = if (pv == 0.0) "PV = 0 (القسمة على صفر غير معرفة)" else "SPI = ${fmt(ev)} / ${fmt(pv)} = ${EvmCalculator.formatIndex(spi)}",
        result = EvmCalculator.formatIndex(spi),
        baladiExplain = if (spi == null) {
            "بالبلدي: لسة الجدول المخطط مبدأش (PV = 0)."
        } else if (spi >= 1.0) {
            "بالبلدي: سرعة التنفيذ ${(spi * 100).toInt()}% من السرعة المخططة — سابقين الوقت."
        } else {
            "بالبلدي: بننفذ بسرعة ${(spi * 100).toInt()}% بس من المطلوب في الخطة — يعني أبطأ من الجدول."
        },
        accent = statusColor(spiStatus)
    )

    // Results: Forecasting
    Text("2. معادلات التنبؤ بالمستقبل (EAC / ETC / VAC / TCPI)", style = MaterialTheme.typography.titleMedium)

    FormulaResultCard(
        symbol = "EAC₁",
        titleAr = "التقدير عند الاكتمال (مشكلة استثنائية وعدّت)",
        formula = "EAC = AC + (BAC − EV)",
        substitution = "EAC = ${fmt(ac)} + (${fmt(bac)} − ${fmt(ev)}) = ${EvmCalculator.formatExactMoney(eac1)}",
        result = EvmCalculator.formatExactMoney(eac1),
        baladiExplain = "بالبلدي: اللي اتصرف اتصرف خلاص، والباقي هيمشي زي الخطة الأصلية؛ فالمشروع كله هيقفل على ${EvmCalculator.formatExactMoney(eac1)}.",
        accent = EtcColor
    )
    FormulaResultCard(
        symbol = "EAC₂",
        titleAr = "التقدير عند الاكتمال (بنفس الأداء الحالي)",
        formula = "EAC = BAC / CPI",
        substitution = if (eac2 == null) "غير قابل للحساب" else "EAC = ${fmt(bac)} / (${fmt(ev)} / ${fmt(ac)}) = ${EvmCalculator.formatExactMoney(eac2)}",
        result = EvmCalculator.formatExactMoney(eac2),
        baladiExplain = if (eac2 == null) {
            "بالبلدي: محتاجين EV و AC أكبر من صفر عشان نتوقع التكلفة النهائية."
        } else {
            "بالبلدي: لو فضلنا بنفس الصرف والتكلفة دي لحد الآخر، المشروع هيقف علينا بـ ${EvmCalculator.formatExactMoney(eac2)} بدل ${fmt(bac)}."
        },
        accent = BrandPrimary
    )
    FormulaResultCard(
        symbol = "EAC₃",
        titleAr = "التقدير عند الاكتمال (تأثير التكلفة والجدول معًا)",
        formula = "EAC = AC + (BAC − EV) / (CPI × SPI)",
        substitution = if (eac3 == null) "غير متاح (CPI × SPI = 0)" else "EAC = ${fmt(ac)} + (${fmt(bac)} − ${fmt(ev)}) / (${EvmCalculator.formatIndex(cpi)} × ${EvmCalculator.formatIndex(spi)}) = ${EvmCalculator.formatExactMoney(eac3)}",
        result = EvmCalculator.formatExactMoney(eac3),
        baladiExplain = if (eac3 == null) {
            "بالبلدي: لازم CPI و SPI ما يساووش صفر عشان نحسب التوقع المركب."
        } else {
            "بالبلدي: التأخير كمان بيكلفنا فلوس زيادة (عمالة ومعدات ووقت)، فالتكلفة النهائية المتوقعة هتوصل لـ ${EvmCalculator.formatExactMoney(eac3)}."
        },
        accent = AcColor
    )
    FormulaResultCard(
        symbol = "EAC₄",
        titleAr = "التقدير عند الاكتمال (بتقديرك اليدوي)",
        formula = "EAC = AC + Manual ETC",
        substitution = "EAC = ${fmt(ac)} + ${fmt(manualEtc)} = ${EvmCalculator.formatExactMoney(eac4)}",
        result = EvmCalculator.formatExactMoney(eac4),
        baladiExplain = "بالبلدي: اللي اتصرف (${fmt(ac)}) + تقديرك للي فاضل (${fmt(manualEtc)}) = إجمالي المشروع هيكلف ${EvmCalculator.formatExactMoney(eac4)}.",
        accent = EvColor
    )
    FormulaResultCard(
        symbol = "ETC",
        titleAr = "المتبقي صرفه حتى التسليم",
        formula = "ETC = EAC − AC",
        substitution = if (etcVal == null) "غير قابل للحساب" else "ETC = ${EvmCalculator.formatExactMoney(eac2)} − ${fmt(ac)} = ${EvmCalculator.formatExactMoney(etcVal)}",
        result = EvmCalculator.formatExactMoney(etcVal),
        baladiExplain = if (etcVal == null) {
            "بالبلدي: احسب EAC الأول عشان تعرف فاضل تصرف كام."
        } else {
            "بالبلدي: جهّز في الخزنة مبلغ ${EvmCalculator.formatExactMoney(etcVal)} عشان تكمل باقي المشروع من النهارده لحد التسليم."
        },
        accent = EtcColor
    )
    FormulaResultCard(
        symbol = "VAC",
        titleAr = "العجز أو الوفر المتوقع في النهاية",
        formula = "VAC = BAC − EAC",
        substitution = if (vacVal == null) "غير قابل للحساب" else "VAC = ${fmt(bac)} − ${EvmCalculator.formatExactMoney(eac2)} = ${EvmCalculator.formatExactMoney(vacVal)} (${EvmCalculator.formatSignedPercent(vacPct)})",
        result = "${EvmCalculator.formatExactMoney(vacVal)} (${EvmCalculator.formatSignedPercent(vacPct)})",
        baladiExplain = when {
            vacVal == null -> "بالبلدي: غير قابل للحساب حاليًا."
            vacVal >= 0 -> "بالبلدي: في الآخر هيتفضل معانا وفر ${EvmCalculator.formatExactMoney(vacVal)} من الميزانية الأصلية."
            else -> "بالبلدي: في الآخر هنزيد عن الميزانية الأصلية بـ ${EvmCalculator.formatExactMoney(-vacVal)} (${EvmCalculator.formatSignedPercent(vacPct)})؛ يعني عجز متوقع."
        },
        accent = if ((vacVal ?: 0.0) >= 0) StatusGood else StatusBad
    )
    FormulaResultCard(
        symbol = "TCPI(BAC)",
        titleAr = "الكفاءة المطلوبة للحاق بالموازنة الأصلية",
        formula = "TCPI(BAC) = (BAC − EV) / (BAC − AC)",
        substitution = if (bac - ac <= 0.0) "BAC − AC ≤ 0 (تم استنفاد الموازنة؛ غير قابل للحساب)" else "TCPI = (${fmt(bac)} − ${fmt(ev)}) / (${fmt(bac)} − ${fmt(ac)}) = ${EvmCalculator.formatIndex(tcpiBac)}",
        result = EvmCalculator.formatIndex(tcpiBac),
        baladiExplain = when {
            bac - ac <= 0.0 -> "بالبلدي: الميزانية الأصلية خلصت خلاص، ومستحيل نلتزم بيها بعد كده؛ لازم إعادة تقدير أو طلب زيادة."
            tcpiBac != null && cpi != null && tcpiBac - cpi > 0.10 ->
                "بالبلدي: عشان نلحق نقفّل على ${fmt(bac)} لازم كل وحدة فلوس باقية تطلع شغل بـ ${EvmCalculator.formatIndex(tcpiBac)}، وإحنا حاليا بنطلع ${EvmCalculator.formatIndex(cpi)} بس — ده صعب ومش واقعي!"
            else -> "بالبلدي: الكفاءة المطلوبة (${EvmCalculator.formatIndex(tcpiBac)}) قريبة من أدائنا، وممكن نلحق نلتزم بالميزانية لو شدّينا حيلنا شوية."
        },
        accent = if (tcpiBac != null && cpi != null && tcpiBac - cpi <= 0.10) StatusGood else StatusBad
    )
    FormulaResultCard(
        symbol = "TCPI(EAC)",
        titleAr = "الكفاءة المطلوبة للالتزام بالتقدير الجديد",
        formula = "TCPI(EAC) = (BAC − EV) / (EAC − AC)",
        substitution = if (tcpiEac == null) "غير قابل للحساب" else "TCPI = (${fmt(bac)} − ${fmt(ev)}) / (${EvmCalculator.formatExactMoney(eac2)} − ${fmt(ac)}) = ${EvmCalculator.formatIndex(tcpiEac)}",
        result = EvmCalculator.formatIndex(tcpiEac),
        baladiExplain = "بالبلدي: دي الكفاءة اللي محتاجين نحافظ عليها في الشغل الباقي عشان ما نعدّيش التقدير الجديد (${EvmCalculator.formatExactMoney(eac2)}).",
        accent = EvColor
    )
}

@Composable
private fun EstimationCalculatorTab() {
    var cOStr by remember { mutableStateOf("80") }
    var cMStr by remember { mutableStateOf("100") }
    var cPStr by remember { mutableStateOf("150") }

    var priorStr by remember { mutableStateOf("40") }
    var adjStr by remember { mutableStateOf("5") }
    var qtyStr by remember { mutableStateOf("300") }
    var priceStr by remember { mutableStateOf("0.1") }

    val cOTip = remember {
        BaladiFieldTip(
            title = "cO (التقدير المتفائل)",
            baladiMeaning = "بالبلدي: أقل سعر ممكن البند يكلفه لو كل الظروف ساعدتنا والأسعار ثابتة والعمالة سريعة.",
            example = "البند في أحسن الظروف هيكلف 80."
        )
    }
    val cMTip = remember {
        BaladiFieldTip(
            title = "cM (التقدير الأكثر احتمالًا)",
            baladiMeaning = "بالبلدي: السعر الطبيعي العادي اللي السوق شغال بيه في معظم المشروعات المشابهة.",
            example = "في العادي البند ده بيكلف 100."
        )
    }
    val cPTip = remember {
        BaladiFieldTip(
            title = "cP (التقدير المتشائم)",
            baladiMeaning = "بالبلدي: أعلى تكلفة ممكن تحصل لا قدر الله لو الخامات غليت أو حصل تعطيل أو إعادة شغل.",
            example = "لو الدنيا اتأخرت والحديد غلي ممكن يوصل 150."
        )
    }
    val priorTip = remember {
        BaladiFieldTip(
            title = "تكلفة مشروع سابق (التناظري)",
            baladiMeaning = "بالبلدي: مشروع قديم عملناه قبل كده شبه البند ده كلّفنا كام؟",
            example = "تشطيب عمارة قبل كده كلف 40 ألف."
        )
    }
    val adjTip = remember {
        BaladiFieldTip(
            title = "معامل التعديل %",
            baladiMeaning = "بالبلدي: الزيادة أو النقصان في الأسعار والمساحة مقارنة بالمشروع القديم (ممكن موجب أو سالب).",
            example = "الأسعار زادت 5% عن السنة اللي فاتت -> اكتب 5."
        )
    }
    val qtyTip = remember {
        BaladiFieldTip(
            title = "الكمية (البارامتري)",
            baladiMeaning = "بالبلدي: مقاس البند من اللوحات والمقايسة (كام متر مكعب خرسانة، أو كام متر مربع مباني).",
            example = "عندنا 300 م³ حفر."
        )
    }
    val unitPriceTip = remember {
        BaladiFieldTip(
            title = "سعر الوحدة (البارامتري)",
            baladiMeaning = "بالبلدي: المتر الواحد أو الطن الواحد بيكلفنا كام شامل مصنعية ومواد.",
            example = "المتر بـ 0.1 ألف دولار (100 دولار)."
        )
    }

    var activeTip by remember { mutableStateOf(cOTip) }

    val cO = cOStr.toDoubleOrNull() ?: 0.0
    val cM = cMStr.toDoubleOrNull() ?: 0.0
    val cP = cPStr.toDoubleOrNull() ?: 0.0

    val beta = EvmCalculator.calculateThreePointBeta(cO, cM, cP)
    val tri = EvmCalculator.calculateThreePointTriangular(cO, cM, cP)
    val sigma = EvmCalculator.calculateThreePointSigma(cO, cP)

    val analogousRes = EvmCalculator.calculateAnalogous(
        priorStr.toDoubleOrNull() ?: 0.0,
        adjStr.toDoubleOrNull() ?: 0.0
    )
    val paramRes = EvmCalculator.calculateParametric(
        qtyStr.toDoubleOrNull() ?: 0.0,
        priceStr.toDoubleOrNull() ?: 0.0
    )

    ActiveFieldBaladiCard(tip = activeTip)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, CardBorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("حاسبة التقدير ثلاثي النقاط (PERT & مثلثي)", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { cOStr = "80"; cMStr = "100"; cPStr = "150" }) {
                    Text("مثال 80/100/150")
                }
            }

            BaladiInputField(
                value = cOStr,
                onValueChange = { cOStr = it },
                label = "cO — المتفائل (Optimistic)",
                baladiHint = "بالبلدي: أقل سعر ممكن لو الدنيا مشيت حلاوة من غير مشاكل.",
                tip = cOTip,
                onFocused = { activeTip = it },
                modifier = Modifier.fillMaxWidth()
            )
            BaladiInputField(
                value = cMStr,
                onValueChange = { cMStr = it },
                label = "cM — الأرجح (Most Likely)",
                baladiHint = "بالبلدي: السعر الطبيعي اللي بيحصل في أغلب الظروف العادية.",
                tip = cMTip,
                onFocused = { activeTip = it },
                modifier = Modifier.fillMaxWidth()
            )
            BaladiInputField(
                value = cPStr,
                onValueChange = { cPStr = it },
                label = "cP — المتشائم (Pessimistic)",
                baladiHint = "بالبلدي: أعلى سعر متوقع لو حصلت مشاكل أو غلاء أو تأخير.",
                tip = cPTip,
                onFocused = { activeTip = it },
                modifier = Modifier.fillMaxWidth()
            )

            FormulaResultCard(
                symbol = "PERT",
                titleAr = "توزيع بيتا (المرجح)",
                formula = "E = (cO + 4×cM + cP) / 6",
                substitution = "E = (${fmt(cO)} + 4×${fmt(cM)} + ${fmt(cP)}) / 6 = ${EvmCalculator.formatExactMoney(beta)}",
                result = EvmCalculator.formatExactMoney(beta),
                baladiExplain = "بالبلدي: بيدي ثقل كبير للسعر الطبيعي (4 مرات) مع حساب احتمالات الغلاء والتوفير، فالمتوسط المتوقع هو ${EvmCalculator.formatExactMoney(beta)}.",
                accent = BrandPrimary
            )
            FormulaResultCard(
                symbol = "Tri",
                titleAr = "التوزيع المثلثي",
                formula = "E = (cO + cM + cP) / 3",
                substitution = "E = (${fmt(cO)} + ${fmt(cM)} + ${fmt(cP)}) / 3 = ${EvmCalculator.formatExactMoney(tri)}",
                result = EvmCalculator.formatExactMoney(tri),
                baladiExplain = "بالبلدي: متوسط حسابي بسيط بين التلات أرقام بالتساوي.",
                accent = EvColor
            )
            FormulaResultCard(
                symbol = "σ",
                titleAr = "الانحراف المعياري (مقياس المخاطرة)",
                formula = "σ = (cP − cO) / 6",
                substitution = "σ = (${fmt(cP)} − ${fmt(cO)}) / 6 = ${EvmCalculator.formatIndex(sigma)}",
                result = EvmCalculator.formatIndex(sigma),
                baladiExplain = "بالبلدي: كل ما الرقم ده يكبر، ده معناه إن البند ده مقلق وفيه تفاوت كبير في الأسعار ولازم تزود له احتياطي طوارئ.",
                accent = EtcColor
            )
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, CardBorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("التقدير التناظري والبارامتري", style = MaterialTheme.typography.titleMedium)

            BaladiInputField(
                value = priorStr,
                onValueChange = { priorStr = it },
                label = "تكلفة مشروع سابق مماثل",
                baladiHint = "بالبلدي: مشروع شبه ده عملناه قبل كده كلّفنا كام؟",
                tip = priorTip,
                onFocused = { activeTip = it },
                modifier = Modifier.fillMaxWidth()
            )
            BaladiInputField(
                value = adjStr,
                onValueChange = { adjStr = it },
                label = "معامل التعديل %",
                baladiHint = "بالبلدي: الأسعار أو المساحة زادت أو قلت بكام % عن المشروع القديم؟",
                tip = adjTip,
                onFocused = { activeTip = it },
                modifier = Modifier.fillMaxWidth()
            )
            FormulaResultCard(
                symbol = "Analogous",
                titleAr = "التقدير التناظري",
                formula = "Est = PriorCost × (1 + Adj% / 100)",
                substitution = "Est = $priorStr × (1 + $adjStr%) = ${EvmCalculator.formatExactMoney(analogousRes)}",
                result = EvmCalculator.formatExactMoney(analogousRes),
                baladiExplain = "بالبلدي: خدنا تكلفة المشروع القديم وزودنا/نقصنا عليها نسبة التغير، فطلع التقدير التقريبي ${EvmCalculator.formatExactMoney(analogousRes)}.",
                accent = StatusWarning
            )

            HorizontalDivider()

            BaladiInputField(
                value = qtyStr,
                onValueChange = { qtyStr = it },
                label = "الكمية",
                baladiHint = "بالبلدي: عندنا كام متر أو طن أو وحدة من البند ده؟",
                tip = qtyTip,
                onFocused = { activeTip = it },
                modifier = Modifier.fillMaxWidth()
            )
            BaladiInputField(
                value = priceStr,
                onValueChange = { priceStr = it },
                label = "سعر الوحدة",
                baladiHint = "بالبلدي: المتر أو الوحدة الواحدة بتكلف كام؟",
                tip = unitPriceTip,
                onFocused = { activeTip = it },
                modifier = Modifier.fillMaxWidth()
            )
            FormulaResultCard(
                symbol = "Parametric",
                titleAr = "التقدير البارامتري",
                formula = "Est = Quantity × UnitPrice",
                substitution = "Est = $qtyStr × $priceStr = ${EvmCalculator.formatExactMoney(paramRes)}",
                result = EvmCalculator.formatExactMoney(paramRes),
                baladiExplain = "بالبلدي: ضربنا الكمية في سعر المتر/الوحدة، فإجمالي البند يطلع ${EvmCalculator.formatExactMoney(paramRes)}.",
                accent = EvColor
            )
        }
    }
}

@Composable
private fun BudgetReservesCalculatorTab() {
    var estStr by remember { mutableStateOf("190") }
    var contPctStr by remember { mutableStateOf("5.26") }
    var mrPctStr by remember { mutableStateOf("15") }
    var transferStr by remember { mutableStateOf("0") }

    val estTip = remember {
        BaladiFieldTip(
            title = "تقديرات الأنشطة",
            baladiMeaning = "بالبلدي: مجموع أسعار كل البنود والأعمال الصافية قبل ما نحط أي احتياطيات.",
            example = "مجموع بنود العمارة 190 ألف."
        )
    }
    val contTip = remember {
        BaladiFieldTip(
            title = "احتياطي الطوارئ %",
            baladiMeaning = "بالبلدي: قرش زيادة تحت إيد مدير المشروع للمشاكل اللي متوقعين إنها ممكن تحصل (زي فرق سعر أو تربة ضعيفة).",
            example = "5% من البنود كطوارئ معروفة."
        )
    }
    val mrTip = remember {
        BaladiFieldTip(
            title = "الاحتياطي الإداري %",
            baladiMeaning = "بالبلدي: قرش أبيض لليوم الأسود مع الإدارة العليا للمفاجآت اللي محدش متوقعها، ومش بيتحسب في أداء المشروع.",
            example = "15% فوق الخط المرجعي."
        )
    }
    val transferTip = remember {
        BaladiFieldTip(
            title = "طلب تغيير معتمد من الاحتياطي الإداري",
            baladiMeaning = "بالبلدي: مبلغ الإدارة وافقت تفكه من الاحتياطي الإداري وتدخله جوه ميزانية التنفيذ (BAC) بعد مشكلة مفاجئة.",
            example = "لو اعتمدنا 10، الـ BAC هيزيد 10 والاحتياطي الإداري هيقل 10 والموازنة الكلية ثابتة."
        )
    }

    var activeTip by remember { mutableStateOf(estTip) }

    val est = estStr.toDoubleOrNull() ?: 0.0
    val cont = est * ((contPctStr.toDoubleOrNull() ?: 0.0) / 100.0)
    val baseBac = est + cont
    val initialMr = baseBac * ((mrPctStr.toDoubleOrNull() ?: 0.0) / 100.0)
    val transfer = (transferStr.toDoubleOrNull() ?: 0.0).coerceIn(0.0, initialMr)
    val newBac = baseBac + transfer
    val remainingMr = initialMr - transfer
    val totalBudget = newBac + remainingMr

    ActiveFieldBaladiCard(tip = activeTip)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, CardBorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("حاسبة بناء الموازنة والتحويل من الاحتياطي الإداري", style = MaterialTheme.typography.titleMedium)

            BaladiInputField(
                value = estStr,
                onValueChange = { estStr = it },
                label = "تقديرات الأنشطة الصافية",
                baladiHint = "بالبلدي: مجموع تكلفة الشغل الصافي من غير أي احتياطيات.",
                tip = estTip,
                onFocused = { activeTip = it },
                modifier = Modifier.fillMaxWidth()
            )
            BaladiInputField(
                value = contPctStr,
                onValueChange = { contPctStr = it },
                label = "نسبة احتياطي الطوارئ %",
                baladiHint = "بالبلدي: نسبة الأمان للمخاطر المعروفة (تحت إيد مدير المشروع).",
                tip = contTip,
                onFocused = { activeTip = it },
                modifier = Modifier.fillMaxWidth()
            )
            BaladiInputField(
                value = mrPctStr,
                onValueChange = { mrPctStr = it },
                label = "نسبة الاحتياطي الإداري %",
                baladiHint = "بالبلدي: قرش الطوارئ المجهولة (تحت إيد الإدارة العليا وخارج BAC).",
                tip = mrTip,
                onFocused = { activeTip = it },
                modifier = Modifier.fillMaxWidth()
            )
            BaladiInputField(
                value = transferStr,
                onValueChange = { transferStr = it },
                label = "مبلغ طلب التغيير المعتمد من الاحتياطي الإداري",
                baladiHint = "بالبلدي: لو فكينا جزء من الاحتياطي الإداري ودخلناه للخط المرجعي.",
                tip = transferTip,
                onFocused = { activeTip = it },
                modifier = Modifier.fillMaxWidth()
            )

            FormulaResultCard(
                symbol = "BAC",
                titleAr = "الخط المرجعي للتكلفة",
                formula = "BAC = Estimates + Contingency + Approved MR Transfer",
                substitution = "BAC = ${fmt(est)} + ${EvmCalculator.formatExactMoney(cont)} + ${fmt(transfer)} = ${EvmCalculator.formatExactMoney(newBac)}",
                result = EvmCalculator.formatExactMoney(newBac),
                baladiExplain = "بالبلدي: ده السقف الرسمي اللي هيتحاسب عليه مدير المشروع في القيمة المكتسبة.",
                accent = PvColor
            )
            FormulaResultCard(
                symbol = "MR",
                titleAr = "الاحتياطي الإداري المتبقي",
                formula = "Remaining MR = Initial MR − Approved Transfer",
                substitution = "MR = ${EvmCalculator.formatExactMoney(initialMr)} − ${fmt(transfer)} = ${EvmCalculator.formatExactMoney(remainingMr)}",
                result = EvmCalculator.formatExactMoney(remainingMr),
                baladiExplain = "بالبلدي: الفلوس اللي لسة محتفظة بيها الإدارة للمفاجآت غير المتوقعة.",
                accent = ManagementReserveColor
            )
            FormulaResultCard(
                symbol = "Budget",
                titleAr = "موازنة المشروع الإجمالية",
                formula = "Project Budget = BAC + Remaining MR",
                substitution = "Budget = ${EvmCalculator.formatExactMoney(newBac)} + ${EvmCalculator.formatExactMoney(remainingMr)} = ${EvmCalculator.formatExactMoney(totalBudget)}",
                result = EvmCalculator.formatExactMoney(totalBudget),
                baladiExplain = "بالبلدي: إجمالي الفلوس المرصودة للمشروع كله من المالك؛ لاحظ إنها بتفضل ثابتة لما ننقل من الاحتياطي الإداري للـ BAC.",
                accent = FundingColor
            )
        }
    }
}

@Composable
private fun FormulaResultCard(
    symbol: String,
    titleAr: String,
    formula: String,
    substitution: String,
    result: String,
    baladiExplain: String,
    accent: Color
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                            .background(accent.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        LtrText(text = symbol, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(titleAr, style = MaterialTheme.typography.titleMedium, fontSize = 14.sp)
                }
                LtrText(text = result, color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PanelBackground)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    LtrText(text = formula, fontSize = 11.5.sp, color = TextMuted)
                    LtrText(text = substitution, fontSize = 12.5.sp, color = TextPrimary)
                }
            }
            Text(
                text = baladiExplain,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun fmt(v: Double): String = EvmCalculator.formatExactMoney(v)
