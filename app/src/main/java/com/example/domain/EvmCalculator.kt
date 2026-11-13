package com.example.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

data class BudgetBreakdown(
    val activityEstimatesSum: Double,
    val activityContingencySum: Double,
    val workPackagesTotal: Double,
    val controlAccountsMap: Map<String, Double>,
    val approvedReserveTransfers: Double,
    val approvedBudgetIncreases: Double,
    val bac: Double,
    val initialManagementReserve: Double,
    val currentManagementReserve: Double,
    val projectBudget: Double,
    val reserveConsumedRatio: Double
)

data class FundingPoint(
    val month: Int,
    val cumulativePv: Double,
    val cumulativeFunding: Double,
    val isShortfall: Boolean,
    val shortfallAmount: Double
)

data class WorkPackageEvmRow(
    val pkg: WorkPackage,
    val budget: Double,
    val pv: Double,
    val ev: Double,
    val ac: Double,
    val cv: Double,
    val sv: Double,
    val cpi: Double?,
    val spi: Double?,
    val percentComplete: Double,
    val cpiStatus: MetricStatus
)

data class EvmSnapshot(
    val statusMonth: Int,
    val bac: Double,
    val managementReserve: Double,
    val projectBudget: Double,
    val pv: Double,
    val ev: Double,
    val ac: Double,
    val cv: Double,
    val sv: Double,
    val cpi: Double?,
    val spi: Double?,
    val cpiStatus: MetricStatus,
    val spiStatus: MetricStatus,
    val cvStatus: MetricStatus,
    val svStatus: MetricStatus,
    val eacAtypical: Double,
    val eacTypical: Double?,
    val eacCombined: Double?,
    val eacManual: Double,
    val activeEac: Double?,
    val etc: Double?,
    val vac: Double?,
    val vacPercent: Double?,
    val tcpiBac: Double?,
    val tcpiEac: Double?,
    val realismStatus: RealismJudgment,
    val pvSeries: List<Double>,          // size = durationMonths (1..N)
    val evSeries: List<Double>,          // size = statusMonth (1..statusMonth)
    val acSeries: List<Double>,          // size = statusMonth (1..statusMonth)
    val forecastSeries: List<Double>,    // size = durationMonths (from statusMonth..N)
    val fundingPoints: List<FundingPoint>, // months 0..durationMonths
    val packageRows: List<WorkPackageEvmRow>,
    val worstThreePackages: List<WorkPackageEvmRow>,
    val fundingShortfallMonths: List<Int>,
    val summarySentenceAr: String
)

sealed class RealismJudgment {
    data object BudgetExhausted : RealismJudgment()
    data object NotCalculable : RealismJudgment()
    data class Feasible(val tcpiBac: Double, val cpi: Double) : RealismJudgment()
    data class Unrealistic(val tcpiBac: Double, val cpi: Double, val activeEac: Double) : RealismJudgment()
}

/**
 * Pure calculation engine for PMBOK 6th Ed. Chapter 7 & Earned Value Management (EVM).
 * All internal calculations retain full Double precision; rounding is applied ONLY at display time.
 */
object EvmCalculator {

    // ---------------------------------------------------------
    // 1. Estimation Functions (7.2)
    // ---------------------------------------------------------

    fun calculateAnalogous(priorCost: Double, adjustmentPercent: Double): Double =
        priorCost * (1.0 + adjustmentPercent / 100.0)

    fun calculateParametric(quantity: Double, unitPrice: Double): Double =
        quantity * unitPrice

    fun calculateBottomUp(items: List<BottomUpItem>): Double =
        items.sumOf { it.amount }

    fun calculateThreePointTriangular(cO: Double, cM: Double, cP: Double): Double =
        (cO + cM + cP) / 3.0

    fun calculateThreePointBeta(cO: Double, cM: Double, cP: Double): Double =
        (cO + 4.0 * cM + cP) / 6.0

    fun calculateThreePointSigma(cO: Double, cP: Double): Double =
        (cP - cO) / 6.0

    fun calculatePackageBaseEstimate(pkg: WorkPackage): Double = when (pkg.estimationMethod) {
        EstimationMethod.ANALOGOUS -> calculateAnalogous(pkg.analogousPriorCost, pkg.analogousAdjustmentPct)
        EstimationMethod.PARAMETRIC -> calculateParametric(pkg.paramQuantity, pkg.paramUnitPrice)
        EstimationMethod.BOTTOM_UP -> calculateBottomUp(pkg.bottomUpItems)
        EstimationMethod.THREE_POINT -> when (pkg.threePointDist) {
            ThreePointDistribution.TRIANGULAR -> calculateThreePointTriangular(pkg.cO, pkg.cM, pkg.cP)
            ThreePointDistribution.BETA -> calculateThreePointBeta(pkg.cO, pkg.cM, pkg.cP)
        }
    }

    fun calculatePackageContingency(pkg: WorkPackage): Double {
        val base = calculatePackageBaseEstimate(pkg)
        return when (pkg.contingencyMode) {
            ReserveMode.PERCENT -> base * (pkg.contingencyValue / 100.0)
            ReserveMode.AMOUNT -> pkg.contingencyValue
        }
    }

    fun calculatePackageTotalBudget(pkg: WorkPackage): Double =
        calculatePackageBaseEstimate(pkg) + calculatePackageContingency(pkg)

    // ---------------------------------------------------------
    // 2. Budget & Baseline Breakdown (7.3 & 7.4 Change Requests)
    // ---------------------------------------------------------

    fun calculateBudgetBreakdown(state: ProjectState): BudgetBreakdown {
        val estimatesSum = state.workPackages.sumOf { calculatePackageBaseEstimate(it) }
        val contingencySum = state.workPackages.sumOf { calculatePackageContingency(it) }
        val wpTotal = estimatesSum + contingencySum

        val controlAccounts = state.workPackages
            .groupBy { it.controlAccount.ifBlank { "حساب مراقبة عام" } }
            .mapValues { (_, list) -> list.sumOf { calculatePackageTotalBudget(it) } }

        val approvedTransfers = state.changeRequests
            .filter { it.status == ChangeStatus.APPROVED && it.source == ChangeSource.MANAGEMENT_RESERVE }
            .sumOf { it.costImpact }

        val approvedIncreases = state.changeRequests
            .filter { it.status == ChangeStatus.APPROVED && it.source == ChangeSource.BUDGET_INCREASE }
            .sumOf { it.costImpact }

        // In Quick Mode with no work packages, fallback to last quick PV as base BAC if needed
        val baseBac = if (wpTotal > 0.0) wpTotal else state.quickEntries.maxOfOrNull { it.cumulativePv } ?: 0.0
        val bac = baseBac + approvedTransfers + approvedIncreases

        val initialMr = when (state.managementReserveConfig.mode) {
            ReserveMode.PERCENT -> baseBac * (state.managementReserveConfig.value / 100.0)
            ReserveMode.AMOUNT -> state.managementReserveConfig.value
        }
        val currentMr = (initialMr - approvedTransfers).coerceAtLeast(0.0)
        val projectBudget = bac + currentMr
        val consumedRatio = if (initialMr > 0.0) (approvedTransfers / initialMr).coerceIn(0.0, 1.0) else 0.0

        return BudgetBreakdown(
            activityEstimatesSum = estimatesSum,
            activityContingencySum = contingencySum,
            workPackagesTotal = wpTotal,
            controlAccountsMap = controlAccounts,
            approvedReserveTransfers = approvedTransfers,
            approvedBudgetIncreases = approvedIncreases,
            bac = bac,
            initialManagementReserve = initialMr,
            currentManagementReserve = currentMr,
            projectBudget = projectBudget,
            reserveConsumedRatio = consumedRatio
        )
    }

    // ---------------------------------------------------------
    // 3. Time-Phased PV & Funding Reconciliation (7.3)
    // ---------------------------------------------------------

    fun packageMonthlyWeights(
        startMonth: Int,
        endMonth: Int,
        pattern: DistributionPattern
    ): Map<Int, Double> {
        val start = startMonth.coerceAtLeast(1)
        val end = endMonth.coerceAtLeast(start)
        val count = (end - start + 1)
        if (count == 1) return mapOf(start to 1.0)

        val raw = (0 until count).map { idx ->
            val t = idx.toDouble() / (count - 1).toDouble()
            when (pattern) {
                DistributionPattern.UNIFORM -> 1.0
                DistributionPattern.S_CURVE -> {
                    // Bell-shaped density producing a smooth S-curve cumulatively
                    val centered = (t - 0.5) * 2.0 // -1..1
                    1.0 - 0.65 * centered * centered
                }
                DistributionPattern.FRONT_LOADED -> 1.6 - 1.1 * t
                DistributionPattern.BACK_LOADED -> 0.5 + 1.1 * t
            }
        }
        val sum = raw.sum().coerceAtLeast(1e-9)
        return (start..end).mapIndexed { idx, month -> month to (raw[idx] / sum) }.toMap()
    }

    fun generatePackageCumulativePv(pkg: WorkPackage, durationMonths: Int): List<Double> {
        val budget = calculatePackageTotalBudget(pkg)
        val weights = packageMonthlyWeights(pkg.startMonth, pkg.endMonth, pkg.distributionPattern)
        var running = 0.0
        return (1..durationMonths).map { m ->
            running += budget * (weights[m] ?: 0.0)
            running
        }
    }

    fun generateCumulativePvSeries(state: ProjectState, bac: Double): List<Double> {
        val duration = state.setup.durationMonths.coerceAtLeast(1)
        if (state.setup.inputMode == InputMode.QUICK && state.quickEntries.isNotEmpty()) {
            val map = state.quickEntries.associateBy { it.month }
            var last = 0.0
            return (1..duration).map { m ->
                val v = map[m]?.cumulativePv ?: last
                last = v
                v
            }
        }
        val override = state.manualPvOverride
        if (override != null && override.size == duration && state.workPackages.isNotEmpty()) {
            // Scale smoothly if BAC changed via approved Change Request
            val overrideEnd = override.lastOrNull() ?: 0.0
            return if (overrideEnd > 0.0 && abs(bac - overrideEnd) > 1e-6) {
                override.map { it * (bac / overrideEnd) }
            } else {
                override
            }
        }
        val baseSeries = MutableList(duration) { 0.0 }
        for (pkg in state.workPackages) {
            val pkgCum = generatePackageCumulativePv(pkg, duration)
            for (i in 0 until duration) {
                baseSeries[i] += pkgCum[i]
            }
        }
        val baseEnd = baseSeries.lastOrNull() ?: 0.0
        return if (baseEnd > 0.0 && abs(bac - baseEnd) > 1e-6) {
            baseSeries.map { it * (bac / baseEnd) }
        } else {
            baseSeries
        }
    }

    fun calculateFundingPoints(
        pvSeries: List<Double>,
        tranches: List<FundingTranche>,
        durationMonths: Int
    ): List<FundingPoint> {
        val sortedTranches = tranches.sortedBy { it.month }
        return (0..durationMonths).map { m ->
            val cumPv = if (m == 0) 0.0 else pvSeries.getOrElse(m - 1) { pvSeries.lastOrNull() ?: 0.0 }
            val cumFunding = sortedTranches.filter { it.month <= m }.sumOf { it.amount }
            val shortfall = (cumPv - cumFunding).coerceAtLeast(0.0)
            FundingPoint(
                month = m,
                cumulativePv = cumPv,
                cumulativeFunding = cumFunding,
                isShortfall = m > 0 && cumPv > cumFunding + 1e-6,
                shortfallAmount = shortfall
            )
        }
    }

    // ---------------------------------------------------------
    // 4. Core EVM Pure Functions (Section 7 Mandatory Precision)
    // ---------------------------------------------------------

    fun cv(ev: Double, ac: Double): Double = ev - ac

    fun sv(ev: Double, pv: Double): Double = ev - pv

    fun cpi(ev: Double, ac: Double): Double? =
        if (ac == 0.0) null else ev / ac

    fun spi(ev: Double, pv: Double): Double? =
        if (pv == 0.0) null else ev / pv

    /** Assumption 1: Variance is atypical and will not recur */
    fun eacAtypical(ac: Double, bac: Double, ev: Double): Double =
        ac + (bac - ev)

    /** Assumption 2: Current cost performance (CPI) will continue */
    fun eacTypical(bac: Double, ev: Double, ac: Double): Double? {
        val cpiVal = cpi(ev, ac) ?: return null
        if (cpiVal == 0.0) return null
        return bac / cpiVal
    }

    /** Assumption 3: Both cost and schedule performance affect remaining work */
    fun eacCostAndSchedule(ac: Double, bac: Double, ev: Double, pv: Double): Double? {
        val cpiVal = cpi(ev, ac) ?: return null
        val spiVal = spi(ev, pv) ?: return null
        val product = cpiVal * spiVal
        if (product == 0.0) return null
        return ac + (bac - ev) / product
    }

    fun eacManual(ac: Double, manualEtc: Double): Double =
        ac + manualEtc

    fun etc(eac: Double, ac: Double): Double =
        eac - ac

    fun vac(bac: Double, eac: Double): Double =
        bac - eac

    fun vacPercent(bac: Double, eac: Double): Double? =
        if (bac == 0.0) null else ((bac - eac) / bac) * 100.0

    fun tcpiBac(bac: Double, ev: Double, ac: Double): Double? {
        val denom = bac - ac
        if (denom <= 0.0) return null
        return (bac - ev) / denom
    }

    fun tcpiEac(bac: Double, ev: Double, eac: Double, ac: Double): Double? {
        val denom = eac - ac
        if (denom <= 0.0) return null
        return (bac - ev) / denom
    }

    // ---------------------------------------------------------
    // 5. Status & Thresholds (7.1 Control Thresholds)
    // ---------------------------------------------------------

    fun evaluateIndexStatus(value: Double?, plan: CostManagementPlan): MetricStatus {
        if (value == null) return MetricStatus.NEUTRAL
        return when {
            value >= plan.acceptableMin -> MetricStatus.GOOD
            value >= plan.warningMin -> MetricStatus.WARNING
            else -> MetricStatus.BAD
        }
    }

    fun evaluateVarianceStatus(variance: Double, baseReference: Double): MetricStatus {
        if (baseReference <= 0.0) return MetricStatus.NEUTRAL
        val ratio = variance / baseReference
        return when {
            variance >= -1e-6 -> MetricStatus.GOOD
            ratio >= -0.05 -> MetricStatus.WARNING
            else -> MetricStatus.BAD
        }
    }

    fun evaluateRealism(
        bac: Double,
        ac: Double,
        cpiVal: Double?,
        tcpiBacVal: Double?,
        activeEac: Double?
    ): RealismJudgment {
        if (bac - ac <= 0.0) return RealismJudgment.BudgetExhausted
        if (cpiVal == null || tcpiBacVal == null || activeEac == null) return RealismJudgment.NotCalculable
        return if (tcpiBacVal - cpiVal <= 0.10) {
            RealismJudgment.Feasible(tcpiBacVal, cpiVal)
        } else {
            RealismJudgment.Unrealistic(tcpiBacVal, cpiVal, activeEac)
        }
    }

    // ---------------------------------------------------------
    // 6. Full Project Snapshot Computation
    // ---------------------------------------------------------

    fun computeSnapshot(state: ProjectState): EvmSnapshot {
        val duration = state.setup.durationMonths.coerceAtLeast(1)
        val statusMonth = state.setup.statusDateMonth.coerceIn(1, duration)
        val budget = calculateBudgetBreakdown(state)
        val bac = budget.bac

        val pvSeries = generateCumulativePvSeries(state, bac)
        val pvAtStatus = pvSeries.getOrElse(statusMonth - 1) { 0.0 }

        val evSeries = mutableListOf<Double>()
        val acSeries = mutableListOf<Double>()

        if (state.setup.inputMode == InputMode.QUICK) {
            val map = state.quickEntries.associateBy { it.month }
            var lastEv = 0.0
            var lastAc = 0.0
            for (m in 1..statusMonth) {
                lastEv = map[m]?.cumulativeEv ?: lastEv
                lastAc = map[m]?.cumulativeAc ?: lastAc
                evSeries.add(lastEv)
                acSeries.add(lastAc)
            }
        } else {
            // Detailed mode: compute cumulative EV & AC from package monthly performance
            for (m in 1..statusMonth) {
                var monthEv = 0.0
                var monthAc = 0.0
                for (pkg in state.workPackages) {
                    val pkgBudget = calculatePackageTotalBudget(pkg)
                    // Latest reported percentComplete up to month m
                    val latestPerf = state.packagePerformances
                        .filter { it.packageId == pkg.id && it.month <= m }
                        .maxByOrNull { it.month }
                    val pct = (latestPerf?.percentComplete ?: 0.0).coerceIn(0.0, 100.0)
                    monthEv += pkgBudget * (pct / 100.0)
                    monthAc += state.packagePerformances
                        .filter { it.packageId == pkg.id && it.month <= m }
                        .sumOf { it.periodActualCost }
                }
                evSeries.add(monthEv)
                acSeries.add(monthAc)
            }
        }

        val evAtStatus = evSeries.lastOrNull() ?: 0.0
        val acAtStatus = acSeries.lastOrNull() ?: 0.0

        val cvVal = cv(evAtStatus, acAtStatus)
        val svVal = sv(evAtStatus, pvAtStatus)
        val cpiVal = cpi(evAtStatus, acAtStatus)
        val spiVal = spi(evAtStatus, pvAtStatus)

        val eacAtyp = eacAtypical(acAtStatus, bac, evAtStatus)
        val eacTyp = eacTypical(bac, evAtStatus, acAtStatus)
        val eacComb = eacCostAndSchedule(acAtStatus, bac, evAtStatus, pvAtStatus)
        val eacMan = eacManual(acAtStatus, state.manualEtcInput)

        val activeEac = when (state.selectedEacMethod) {
            EacMethod.ATYPICAL -> eacAtyp
            EacMethod.TYPICAL -> eacTyp
            EacMethod.COST_AND_SCHEDULE -> eacComb
            EacMethod.MANUAL -> eacMan
        }

        val etcVal = activeEac?.let { etc(it, acAtStatus) }
        val vacVal = activeEac?.let { vac(bac, it) }
        val vacPctVal = activeEac?.let { vacPercent(bac, it) }
        val tcpiBacVal = tcpiBac(bac, evAtStatus, acAtStatus)
        val tcpiEacVal = activeEac?.let { tcpiEac(bac, evAtStatus, it, acAtStatus) }

        val cpiStatus = evaluateIndexStatus(cpiVal, state.plan)
        val spiStatus = evaluateIndexStatus(spiVal, state.plan)
        val cvStatus = evaluateVarianceStatus(cvVal, pvAtStatus.coerceAtLeast(1.0))
        val svStatus = evaluateVarianceStatus(svVal, pvAtStatus.coerceAtLeast(1.0))

        val realism = evaluateRealism(bac, acAtStatus, cpiVal, tcpiBacVal, activeEac)

        // Build forecast series from statusMonth..durationMonths linearly to activeEac
        val forecastSeries = if (activeEac != null) {
            val remainingMonths = (duration - statusMonth).coerceAtLeast(0)
            (0..remainingMonths).map { step ->
                if (remainingMonths == 0) activeEac
                else acAtStatus + (activeEac - acAtStatus) * (step.toDouble() / remainingMonths.toDouble())
            }
        } else emptyList()

        // Package-level breakdown
        val pkgRows = state.workPackages.map { pkg ->
            val pkgBudget = calculatePackageTotalBudget(pkg)
            val pkgPv = state.manualPackagePvAtMonth?.get(pkg.id)?.get(statusMonth)
                ?: generatePackageCumulativePv(pkg, duration).getOrElse(statusMonth - 1) { 0.0 }
            val latestPerf = state.packagePerformances
                .filter { it.packageId == pkg.id && it.month <= statusMonth }
                .maxByOrNull { it.month }
            val pct = (latestPerf?.percentComplete ?: 0.0).coerceIn(0.0, 100.0)
            val pkgEv = pkgBudget * (pct / 100.0)
            val pkgAc = state.packagePerformances
                .filter { it.packageId == pkg.id && it.month <= statusMonth }
                .sumOf { it.periodActualCost }
            val pkgCv = cv(pkgEv, pkgAc)
            val pkgSv = sv(pkgEv, pkgPv)
            val pkgCpi = cpi(pkgEv, pkgAc)
            val pkgSpi = spi(pkgEv, pkgPv)
            WorkPackageEvmRow(
                pkg = pkg,
                budget = pkgBudget,
                pv = pkgPv,
                ev = pkgEv,
                ac = pkgAc,
                cv = pkgCv,
                sv = pkgSv,
                cpi = pkgCpi,
                spi = pkgSpi,
                percentComplete = pct,
                cpiStatus = evaluateIndexStatus(pkgCpi, state.plan)
            )
        }.sortedBy { it.cv } // Worst performing first

        val worstThree = pkgRows.filter { it.ac > 0.0 || it.pv > 0.0 }.take(3)

        val fundingPoints = calculateFundingPoints(pvSeries, state.fundingTranches, duration)
        val shortfallMonths = fundingPoints.filter { it.isShortfall }.map { it.month }

        val summaryAr = buildSummarySentence(cpiVal, spiVal, cvVal, svVal, state.setup.currency)

        return EvmSnapshot(
            statusMonth = statusMonth,
            bac = bac,
            managementReserve = budget.currentManagementReserve,
            projectBudget = budget.projectBudget,
            pv = pvAtStatus,
            ev = evAtStatus,
            ac = acAtStatus,
            cv = cvVal,
            sv = svVal,
            cpi = cpiVal,
            spi = spiVal,
            cpiStatus = cpiStatus,
            spiStatus = spiStatus,
            cvStatus = cvStatus,
            svStatus = svStatus,
            eacAtypical = eacAtyp,
            eacTypical = eacTyp,
            eacCombined = eacComb,
            eacManual = eacMan,
            activeEac = activeEac,
            etc = etcVal,
            vac = vacVal,
            vacPercent = vacPctVal,
            tcpiBac = tcpiBacVal,
            tcpiEac = tcpiEacVal,
            realismStatus = realism,
            pvSeries = pvSeries,
            evSeries = evSeries,
            acSeries = acSeries,
            forecastSeries = forecastSeries,
            fundingPoints = fundingPoints,
            packageRows = pkgRows,
            worstThreePackages = worstThree,
            fundingShortfallMonths = shortfallMonths,
            summarySentenceAr = summaryAr
        )
    }

    private fun buildSummarySentence(
        cpiVal: Double?,
        spiVal: Double?,
        cvVal: Double,
        svVal: Double,
        currency: String
    ): String {
        if (cpiVal == null && spiVal == null) {
            return "لم يتم تسجيل أداء فعلي حتى الآن؛ أدخل نسب الإنجاز والتكاليف الفعلية في شاشة ضبط التكاليف."
        }
        val schedulePart = when {
            svVal > 1e-6 -> "المشروع متقدم عن الجدول الزمني"
            svVal < -1e-6 -> "المشروع متأخر عن الجدول الزمني"
            else -> "المشروع مطابق للجدول الزمني"
        }
        val costPart = when {
            cvVal > 1e-6 -> "وموفّر في الموازنة"
            cvVal < -1e-6 -> "ومتجاوز للموازنة"
            else -> "وملتزم بالموازنة"
        }
        val cpiSentence = if (cpiVal != null) {
            "؛ كل ${currency.ifBlank { "دولار" }} يُنتج عملًا بقيمة ${formatIndex(cpiVal)} ${currency.ifBlank { "دولار" }}."
        } else "؛ لم تُسجَّل تكلفة فعلية بعد."
        return "$schedulePart $costPart$cpiSentence"
    }

    // ---------------------------------------------------------
    // 7. Display Formatting Helpers
    // ---------------------------------------------------------

    private val intFormatter = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
    private val decFormatter = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    fun roundToPrecision(value: Double, precision: Int): Long {
        val p = precision.coerceAtLeast(1).toDouble()
        return (kotlin.math.round(value / p) * p).roundToLong()
    }

    fun formatMoney(value: Double?, precision: Int = 1): String {
        if (value == null || value.isNaN() || value.isInfinite()) return "غير قابل للحساب"
        val rounded = roundToPrecision(value, precision)
        return if (rounded < 0) {
            "−${intFormatter.format(abs(rounded))}"
        } else {
            intFormatter.format(rounded)
        }
    }

    fun formatExactMoney(value: Double?): String {
        if (value == null || value.isNaN() || value.isInfinite()) return "غير قابل للحساب"
        val rounded = value.roundToLong()
        return if (abs(value - rounded) < 0.05) {
            if (rounded < 0) "−${intFormatter.format(abs(rounded))}" else intFormatter.format(rounded)
        } else {
            if (value < 0) "−${decFormatter.format(abs(value))}" else decFormatter.format(value)
        }
    }

    fun formatIndex(value: Double?): String {
        if (value == null || value.isNaN() || value.isInfinite()) return "غير قابل للحساب"
        return if (value < 0) {
            "−${decFormatter.format(abs(value))}"
        } else {
            decFormatter.format(value)
        }
    }

    fun formatSignedPercent(value: Double?): String {
        if (value == null || value.isNaN() || value.isInfinite()) return "—"
        val rounded = value.roundToLong()
        val numText = if (abs(value - rounded) < 0.05) "${abs(rounded)}%" else "${decFormatter.format(abs(value))}%"
        return when {
            value < -0.001 -> "−$numText"
            value > 0.001 -> "+$numText"
            else -> "0%"
        }
    }
}
