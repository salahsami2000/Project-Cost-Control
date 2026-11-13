package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ProjectRepository
import com.example.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen(
    val route: String,
    val numberPrefix: String,
    val titleAr: String
) {
    DASHBOARD("dashboard", "", "لوحة المشروع"),
    PLAN_7_1("plan_7_1", "7.1", "خطة إدارة التكاليف"),
    ESTIMATE_7_2("estimate_7_2", "7.2", "تقدير التكاليف"),
    BUDGET_7_3("budget_7_3", "7.3", "تحديد الموازنة"),
    CONTROL_7_4("control_7_4", "7.4", "ضبط التكاليف"),
    FORECASTING("forecasting", "", "التنبؤ"),
    CHANGE_REQUESTS("change_requests", "", "سجل طلبات التغيير"),
    GLOSSARY("glossary", "", "القاموس والمعادلات"),
    CALCULATOR("calculator", "", "حاسبة المعادلات"),
    REPORTS("reports", "", "التقارير والتصدير"),
    PROJECT_SETUP("project_setup", "", "إعداد المشروع")
}

class ProjectViewModel(application: Application) : AndroidViewModel(application) {

    val repository = ProjectRepository(application)

    private val _state = MutableStateFlow(ProjectState())
    val state: StateFlow<ProjectState> = _state.asStateFlow()

    val snapshot: StateFlow<EvmSnapshot> = _state
        .map { EvmCalculator.computeSnapshot(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EvmCalculator.computeSnapshot(ProjectState()))

    val budgetBreakdown: StateFlow<BudgetBreakdown> = _state
        .map { EvmCalculator.calculateBudgetBreakdown(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EvmCalculator.calculateBudgetBreakdown(ProjectState()))

    private val _explainMetric = MutableStateFlow<MetricId?>(null)
    val explainMetric: StateFlow<MetricId?> = _explainMetric.asStateFlow()

    private val _prefilledChangeRequestCost = MutableStateFlow<Double?>(null)
    val prefilledChangeRequestCost: StateFlow<Double?> = _prefilledChangeRequestCost.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var hasLoadedInitial = false

    init {
        viewModelScope.launch {
            repository.projectStateFlow.collect { loaded ->
                if (!hasLoadedInitial) {
                    hasLoadedInitial = true
                    _state.value = loaded
                }
            }
        }
    }

    private fun updateAndSave(transform: (ProjectState) -> ProjectState) {
        val next = transform(_state.value)
        _state.value = next
        viewModelScope.launch {
            repository.saveState(next)
        }
    }

    fun openExplanation(metricId: MetricId) {
        _explainMetric.value = metricId
    }

    fun closeExplanation() {
        _explainMetric.value = null
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun loadReadySample() {
        val sample = repository.createReadySample()
        updateAndSave { sample }
        showMessage("تم تحميل مثال «عمارة سكنية» (BAC = 200، PV = 100، EV = 80، AC = 120)")
    }

    // --- 5.1 Project Setup ---
    fun saveProjectSetup(setup: ProjectSetup) {
        updateAndSave { cur ->
            val clampedStatus = setup.statusDateMonth.coerceIn(1, setup.durationMonths.coerceAtLeast(1))
            val updated = cur.copy(
                setup = setup.copy(statusDateMonth = clampedStatus, isInitialized = true)
            )
            // Ensure initial baseline revision exists
            if (updated.baselineHistory.isEmpty()) {
                val b = EvmCalculator.calculateBudgetBreakdown(updated)
                updated.copy(
                    baselineHistory = listOf(
                        BaselineRevision(
                            version = 1,
                            date = setup.startDate,
                            bac = b.bac,
                            managementReserve = b.currentManagementReserve,
                            note = "الخط المرجعي الأولي"
                        )
                    )
                )
            } else updated
        }
        showMessage("تم حفظ إعدادات المشروع")
    }

    fun setStatusDateMonth(month: Int) {
        updateAndSave { cur ->
            val clamped = month.coerceIn(1, cur.setup.durationMonths.coerceAtLeast(1))
            cur.copy(setup = cur.setup.copy(statusDateMonth = clamped))
        }
    }

    // --- 5.2 Cost Management Plan (7.1) ---
    fun updatePlan(plan: CostManagementPlan) {
        updateAndSave { cur ->
            cur.copy(
                plan = plan,
                selectedEacMethod = plan.defaultEacMethod
            )
        }
        showMessage("تم تحديث خطة إدارة التكاليف وحدود التحكم")
    }

    fun addUnitOfMeasure(unit: String) {
        val clean = unit.trim()
        if (clean.isEmpty()) return
        updateAndSave { cur ->
            if (clean in cur.plan.unitsOfMeasure) cur
            else cur.copy(plan = cur.plan.copy(unitsOfMeasure = cur.plan.unitsOfMeasure + clean))
        }
    }

    fun removeUnitOfMeasure(unit: String) {
        updateAndSave { cur ->
            cur.copy(plan = cur.plan.copy(unitsOfMeasure = cur.plan.unitsOfMeasure.filterNot { it == unit }))
        }
    }

    // --- 5.3 Cost Estimation (7.2) ---
    fun upsertWorkPackage(pkg: WorkPackage) {
        updateAndSave { cur ->
            val exists = cur.workPackages.any { it.id == pkg.id }
            val nextList = if (exists) {
                cur.workPackages.map { if (it.id == pkg.id) pkg else it }
            } else {
                cur.workPackages + pkg
            }
            // Clear manual sample PV override once user edits work packages
            cur.copy(
                workPackages = nextList,
                manualPvOverride = null,
                manualPackagePvAtMonth = null
            )
        }
    }

    fun deleteWorkPackage(id: String) {
        updateAndSave { cur ->
            cur.copy(
                workPackages = cur.workPackages.filterNot { it.id == id },
                packagePerformances = cur.packagePerformances.filterNot { it.packageId == id },
                manualPvOverride = null,
                manualPackagePvAtMonth = null
            )
        }
    }

    // --- 5.4 Budget & Phasing & Funding (7.3) ---
    fun updateManagementReserve(config: ManagementReserveConfig) {
        updateAndSave { cur -> cur.copy(managementReserveConfig = config) }
    }

    fun updatePackagePhasing(packageId: String, startMonth: Int, endMonth: Int, pattern: DistributionPattern) {
        updateAndSave { cur ->
            val maxM = cur.setup.durationMonths.coerceAtLeast(1)
            val s = startMonth.coerceIn(1, maxM)
            val e = endMonth.coerceIn(s, maxM)
            cur.copy(
                workPackages = cur.workPackages.map {
                    if (it.id == packageId) it.copy(startMonth = s, endMonth = e, distributionPattern = pattern) else it
                },
                manualPvOverride = null,
                manualPackagePvAtMonth = null
            )
        }
    }

    fun addOrUpdateFundingTranche(tranche: FundingTranche) {
        updateAndSave { cur ->
            val exists = cur.fundingTranches.any { it.id == tranche.id }
            val next = if (exists) {
                cur.fundingTranches.map { if (it.id == tranche.id) tranche else it }
            } else {
                cur.fundingTranches + tranche
            }.sortedBy { it.month }
            cur.copy(fundingTranches = next)
        }
    }

    fun deleteFundingTranche(id: String) {
        updateAndSave { cur ->
            cur.copy(fundingTranches = cur.fundingTranches.filterNot { it.id == id })
        }
    }

    // --- 5.5 Control Costs (7.4) ---
    fun updatePackagePerformance(packageId: String, month: Int, percentComplete: Double, periodActualCost: Double) {
        updateAndSave { cur ->
            val filtered = cur.packagePerformances.filterNot { it.packageId == packageId && it.month == month }
            val next = filtered + PackageMonthlyPerformance(
                packageId = packageId,
                month = month,
                percentComplete = percentComplete.coerceIn(0.0, 100.0),
                periodActualCost = periodActualCost.coerceAtLeast(0.0)
            )
            cur.copy(packagePerformances = next)
        }
    }

    fun updateQuickEntry(month: Int, pv: Double, ev: Double, ac: Double) {
        updateAndSave { cur ->
            val filtered = cur.quickEntries.filterNot { it.month == month }
            val next = (filtered + MonthlyQuickEntry(month, pv, ev, ac)).sortedBy { it.month }
            cur.copy(quickEntries = next)
        }
    }

    // --- 5.6 Forecasting ---
    fun selectEacMethod(method: EacMethod) {
        updateAndSave { cur -> cur.copy(selectedEacMethod = method) }
    }

    fun updateManualEtc(value: Double) {
        updateAndSave { cur -> cur.copy(manualEtcInput = value.coerceAtLeast(0.0)) }
    }

    fun prepareChangeRequestFromForecast(eacValue: Double) {
        val overrun = (eacValue - snapshot.value.bac).coerceAtLeast(0.0)
        _prefilledChangeRequestCost.value = overrun
    }

    fun clearPrefilledChangeRequest() {
        _prefilledChangeRequestCost.value = null
    }

    // --- 5.7 Change Requests ---
    fun addChangeRequest(
        description: String,
        reason: String,
        costImpact: Double,
        scheduleImpactDays: Int,
        source: ChangeSource,
        date: String
    ) {
        updateAndSave { cur ->
            val nextNum = cur.changeRequests.size + 1
            val cr = ChangeRequest(
                id = UUID.randomUUID().toString(),
                code = "CR-${nextNum.toString().padStart(2, '0')}",
                date = date,
                description = description,
                reason = reason,
                costImpact = costImpact,
                scheduleImpactDays = scheduleImpactDays,
                source = source,
                status = ChangeStatus.SUBMITTED
            )
            cur.copy(changeRequests = cur.changeRequests + cr)
        }
        showMessage("تم تسجيل طلب التغيير بحالة «مقدّم»")
    }

    fun updateChangeRequestStatus(id: String, newStatus: ChangeStatus) {
        updateAndSave { cur ->
            val target = cur.changeRequests.find { it.id == id } ?: return@updateAndSave cur
            if (target.status == newStatus) return@updateAndSave cur

            val updatedRequests = cur.changeRequests.map {
                if (it.id == id) it.copy(status = newStatus) else it
            }
            val intermediate = cur.copy(changeRequests = updatedRequests)

            // Save Baseline History whenever a request is approved and modifies BAC
            if (newStatus == ChangeStatus.APPROVED && target.source != ChangeSource.REALLOCATION) {
                val breakdown = EvmCalculator.calculateBudgetBreakdown(intermediate)
                val nextVer = (cur.baselineHistory.maxOfOrNull { it.version } ?: 1) + 1
                val rev = BaselineRevision(
                    version = nextVer,
                    date = target.date,
                    bac = breakdown.bac,
                    managementReserve = breakdown.currentManagementReserve,
                    note = "اعتماد ${target.code}: ${target.description} (${target.source.labelAr})"
                )
                intermediate.copy(baselineHistory = cur.baselineHistory + rev)
            } else {
                intermediate
            }
        }
    }

    // --- 5.11 Import / Export ---
    fun importProjectJson(json: String): Boolean {
        val result = repository.importFromJson(json)
        return result.fold(
            onSuccess = { imported ->
                updateAndSave { imported }
                showMessage("تم استيراد بيانات المشروع بنجاح")
                true
            },
            onFailure = {
                showMessage("فشل استيراد الملف: تأكد من صحة تنسيق JSON")
                false
            }
        )
    }
}
