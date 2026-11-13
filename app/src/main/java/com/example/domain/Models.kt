package com.example.domain

import com.squareup.moshi.JsonClass

enum class ProjectType(val labelAr: String) {
    RESIDENTIAL("سكني"),
    COMMERCIAL("تجاري"),
    INFRASTRUCTURE("بنية تحتية"),
    INDUSTRIAL("صناعي"),
    ADMINISTRATIVE("إداري / خدمي")
}

enum class DisplayUnit(val labelAr: String, val multiplierHint: String) {
    UNIT("وحدة كاملة", ""),
    THOUSAND("ألف", "ألف"),
    MILLION("مليون", "مليون")
}

enum class InputMode(val labelAr: String, val descriptionAr: String) {
    DETAILED("تفصيلي", "تبني حزم العمل والتقديرات، ويحسب التطبيق PV تلقائيًا"),
    QUICK("سريع", "تدخل قيم PV و EV و AC التراكمية شهريًا مباشرة")
}

enum class EstimationMethod(
    val labelAr: String,
    val accuracyLabelAr: String,
    val accuracyLevel: AccuracyLevel
) {
    ANALOGOUS("تناظري", "منخفضة", AccuracyLevel.LOW),
    PARAMETRIC("بارامتري", "متوسطة", AccuracyLevel.MEDIUM),
    BOTTOM_UP("من القاعدة للقمة", "عالية", AccuracyLevel.HIGH),
    THREE_POINT("ثلاثي النقاط", "متوسطة", AccuracyLevel.MEDIUM)
}

enum class AccuracyLevel(val labelAr: String) {
    LOW("منخفضة"),
    MEDIUM("متوسطة"),
    HIGH("عالية")
}

enum class ThreePointDistribution(val labelAr: String) {
    TRIANGULAR("مثلثي"),
    BETA("بيتا (PERT)")
}

enum class ReserveMode(val labelAr: String) {
    PERCENT("نسبة %"),
    AMOUNT("مبلغ ثابت")
}

enum class DistributionPattern(val labelAr: String) {
    UNIFORM("منتظم"),
    S_CURVE("منحنى S"),
    FRONT_LOADED("مقدم"),
    BACK_LOADED("مؤخر")
}

enum class EacMethod(val labelAr: String, val shortLabelAr: String, val formulaText: String) {
    ATYPICAL(
        "انحراف استثنائي (لن يتكرر)",
        "استثنائي",
        "EAC = AC + (BAC − EV)"
    ),
    TYPICAL(
        "استمرار أداء التكلفة الحالي",
        "مستمر (CPI)",
        "EAC = BAC / CPI"
    ),
    COST_AND_SCHEDULE(
        "تأثير التكلفة والجدول معًا",
        "مركب (CPI×SPI)",
        "EAC = AC + (BAC − EV) / (CPI × SPI)"
    ),
    MANUAL(
        "تقدير يدوي (Bottom-up ETC)",
        "يدوي",
        "EAC = AC + ETC_manual"
    )
}

enum class ReportingFrequency(val labelAr: String) {
    MONTHLY("شهري"),
    WEEKLY("أسبوعي")
}

enum class ChangeSource(val labelAr: String) {
    MANAGEMENT_RESERVE("احتياطي إداري"),
    BUDGET_INCREASE("زيادة موازنة"),
    REALLOCATION("إعادة توزيع")
}

enum class ChangeStatus(val labelAr: String) {
    SUBMITTED("مقدّم"),
    APPROVED("معتمد"),
    REJECTED("مرفوض")
}

enum class MetricStatus {
    GOOD,
    WARNING,
    BAD,
    NEUTRAL
}

@JsonClass(generateAdapter = true)
data class ProjectSetup(
    val name: String = "مشروع جديد",
    val type: ProjectType = ProjectType.RESIDENTIAL,
    val currency: String = "دولار",
    val displayUnit: DisplayUnit = DisplayUnit.THOUSAND,
    val startDate: String = "2026-01-01",
    val durationMonths: Int = 10,
    val inputMode: InputMode = InputMode.DETAILED,
    val statusDateMonth: Int = 1,
    val isInitialized: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CostManagementPlan(
    val unitsOfMeasure: List<String> = listOf("م³", "طن", "م²", "م.ط", "ساعة عمل", "مقطوعية"),
    val roundingPrecision: Int = 1, // 1, 10, 100, 1000
    val acceptableMin: Double = 0.95,
    val acceptableMax: Double = 1.05,
    val warningMin: Double = 0.90,
    val defaultEacMethod: EacMethod = EacMethod.TYPICAL,
    val reportingFrequency: ReportingFrequency = ReportingFrequency.MONTHLY
)

@JsonClass(generateAdapter = true)
data class BottomUpItem(
    val id: String,
    val category: String, // عمالة، مواد، معدات
    val description: String,
    val amount: Double
)

@JsonClass(generateAdapter = true)
data class WorkPackage(
    val id: String,
    val code: String,
    val name: String,
    val controlAccount: String,
    val estimationMethod: EstimationMethod = EstimationMethod.PARAMETRIC,
    // Analogous
    val analogousPriorCost: Double = 0.0,
    val analogousAdjustmentPct: Double = 0.0,
    // Parametric
    val paramQuantity: Double = 0.0,
    val paramUnitPrice: Double = 0.0,
    val paramUnit: String = "م³",
    // Bottom-up
    val bottomUpItems: List<BottomUpItem> = emptyList(),
    // Three-point
    val cO: Double = 0.0,
    val cM: Double = 0.0,
    val cP: Double = 0.0,
    val threePointDist: ThreePointDistribution = ThreePointDistribution.BETA,
    // Contingency reserve
    val contingencyMode: ReserveMode = ReserveMode.AMOUNT,
    val contingencyValue: Double = 0.0,
    val associatedRisk: String = "",
    // Time phasing
    val startMonth: Int = 1,
    val endMonth: Int = 3,
    val distributionPattern: DistributionPattern = DistributionPattern.UNIFORM
)

@JsonClass(generateAdapter = true)
data class ManagementReserveConfig(
    val mode: ReserveMode = ReserveMode.AMOUNT,
    val value: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class FundingTranche(
    val id: String,
    val month: Int,
    val amount: Double
)

@JsonClass(generateAdapter = true)
data class MonthlyQuickEntry(
    val month: Int,
    val cumulativePv: Double,
    val cumulativeEv: Double,
    val cumulativeAc: Double
)

@JsonClass(generateAdapter = true)
data class PackageMonthlyPerformance(
    val packageId: String,
    val month: Int,
    val percentComplete: Double, // 0..100
    val periodActualCost: Double
)

@JsonClass(generateAdapter = true)
data class ChangeRequest(
    val id: String,
    val code: String,
    val date: String,
    val description: String,
    val reason: String,
    val costImpact: Double,
    val scheduleImpactDays: Int,
    val source: ChangeSource,
    val status: ChangeStatus
)

@JsonClass(generateAdapter = true)
data class BaselineRevision(
    val version: Int,
    val date: String,
    val bac: Double,
    val managementReserve: Double,
    val note: String
)

@JsonClass(generateAdapter = true)
data class ProjectState(
    val setup: ProjectSetup = ProjectSetup(),
    val plan: CostManagementPlan = CostManagementPlan(),
    val workPackages: List<WorkPackage> = emptyList(),
    val managementReserveConfig: ManagementReserveConfig = ManagementReserveConfig(),
    val fundingTranches: List<FundingTranche> = emptyList(),
    val quickEntries: List<MonthlyQuickEntry> = emptyList(),
    val packagePerformances: List<PackageMonthlyPerformance> = emptyList(),
    val manualPvOverride: List<Double>? = null, // Optional exact PV cumulative curve (e.g., ready example)
    val manualPackagePvAtMonth: Map<String, Map<Int, Double>>? = null, // Optional exact package PV breakdown
    val selectedEacMethod: EacMethod = EacMethod.TYPICAL,
    val manualEtcInput: Double = 0.0,
    val changeRequests: List<ChangeRequest> = emptyList(),
    val baselineHistory: List<BaselineRevision> = emptyList()
)
