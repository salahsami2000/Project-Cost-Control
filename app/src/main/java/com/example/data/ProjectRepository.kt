package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.EvmCalculator
import com.example.domain.EvmSnapshot
import com.example.domain.ProjectState
import com.example.domain.SampleProjectFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.projectDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "project_cost_prefs"
)

class ProjectRepository(private val context: Context) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(ProjectState::class.java).indent("  ")

    private val stateKey = stringPreferencesKey("project_state_json")

    val projectStateFlow: Flow<ProjectState> = context.projectDataStore.data.map { prefs ->
        val raw = prefs[stateKey]
        if (raw.isNullOrBlank()) {
            // Initialize smoothly with empty uninitialized project (user can setup or load ready example)
            ProjectState()
        } else {
            runCatching { adapter.fromJson(raw) }.getOrNull() ?: ProjectState()
        }
    }

    suspend fun saveState(state: ProjectState) {
        val json = adapter.toJson(state)
        context.projectDataStore.edit { prefs ->
            prefs[stateKey] = json
        }
    }

    fun exportToJson(state: ProjectState): String = adapter.toJson(state)

    fun importFromJson(json: String): Result<ProjectState> = runCatching {
        adapter.fromJson(json) ?: error("تعذر قراءة بيانات المشروع من الملف")
    }

    fun createReadySample(): ProjectState = SampleProjectFactory.createSampleProject()

    /**
     * Generates a UTF-8 BOM CSV file compatible with Microsoft Excel (Arabic columns intact).
     */
    fun exportExcelCsv(state: ProjectState, snapshot: EvmSnapshot): String {
        val sb = StringBuilder()
        // UTF-8 BOM so Excel opens Arabic characters cleanly
        sb.append('\uFEFF')
        sb.appendLine("تقرير تكلفة المشروع,${escapeCsv(state.setup.name)},تاريخ المتابعة,الشهر ${snapshot.statusMonth}")
        sb.appendLine("الوحدة,${escapeCsv(state.setup.displayUnit.labelAr)} ${escapeCsv(state.setup.currency)},BAC,${snapshot.bac},موازنة المشروع,${snapshot.projectBudget}")
        sb.appendLine()
        sb.appendLine("جدول حزم العمل (WBS & EVM)")
        sb.appendLine("الكود,الحزمة,حساب المراقبة,طريقة التقدير,التقدير الأساسي,احتياطي الطوارئ,الموازنة الإجمالية,PV,EV,AC,CV,CPI,نسبة الإنجاز %")
        for (row in snapshot.packageRows) {
            val baseEst = EvmCalculator.calculatePackageBaseEstimate(row.pkg)
            val cont = EvmCalculator.calculatePackageContingency(row.pkg)
            sb.appendLine(
                listOf(
                    escapeCsv(row.pkg.code),
                    escapeCsv(row.pkg.name),
                    escapeCsv(row.pkg.controlAccount),
                    escapeCsv(row.pkg.estimationMethod.labelAr),
                    String.format("%.2f", baseEst),
                    String.format("%.2f", cont),
                    String.format("%.2f", row.budget),
                    String.format("%.2f", row.pv),
                    String.format("%.2f", row.ev),
                    String.format("%.2f", row.ac),
                    String.format("%.2f", row.cv),
                    row.cpi?.let { String.format("%.2f", it) } ?: "N/A",
                    String.format("%.1f", row.percentComplete)
                ).joinToString(",")
            )
        }
        sb.appendLine()
        sb.appendLine("الأداء الشهري التراكمي")
        sb.appendLine("الشهر,PV التراكمي,EV التراكمي,AC التراكمي,التوقع المستقبلي EAC")
        for (m in 1..state.setup.durationMonths) {
            val pv = snapshot.pvSeries.getOrNull(m - 1) ?: 0.0
            val ev = snapshot.evSeries.getOrNull(m - 1)
            val ac = snapshot.acSeries.getOrNull(m - 1)
            val fc = if (m >= snapshot.statusMonth) {
                snapshot.forecastSeries.getOrNull(m - snapshot.statusMonth)
            } else null
            sb.appendLine(
                listOf(
                    "الشهر $m",
                    String.format("%.2f", pv),
                    ev?.let { String.format("%.2f", it) } ?: "",
                    ac?.let { String.format("%.2f", it) } ?: "",
                    fc?.let { String.format("%.2f", it) } ?: ""
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    /**
     * Generates a clean A4 RTL Arabic HTML report ready for Android PrintManager (Save as PDF).
     */
    fun buildPrintableHtmlReport(state: ProjectState, snapshot: EvmSnapshot): String {
        val p = state.plan.roundingPrecision
        val unit = "${state.setup.displayUnit.multiplierHint} ${state.setup.currency}".trim()
        val rowsHtml = snapshot.packageRows.joinToString("\n") { row ->
            "<tr>" +
                "<td>${row.pkg.code}</td>" +
                "<td>${row.pkg.name}</td>" +
                "<td dir='ltr'>${EvmCalculator.formatMoney(row.budget, p)}</td>" +
                "<td dir='ltr'>${EvmCalculator.formatMoney(row.pv, p)}</td>" +
                "<td dir='ltr'>${EvmCalculator.formatMoney(row.ev, p)}</td>" +
                "<td dir='ltr'>${EvmCalculator.formatMoney(row.ac, p)}</td>" +
                "<td dir='ltr'>${EvmCalculator.formatMoney(row.cv, p)}</td>" +
                "<td dir='ltr'>${EvmCalculator.formatIndex(row.cpi)}</td>" +
                "</tr>"
        }
        val worstHtml = if (snapshot.worstThreePackages.isEmpty()) {
            "<li>لا توجد انحرافات مسجلة حتى الآن.</li>"
        } else {
            snapshot.worstThreePackages.joinToString("\n") {
                "<li><strong>${it.pkg.name}</strong>: CV = <span dir='ltr'>${EvmCalculator.formatMoney(it.cv, p)}</span> $unit | CPI = <span dir='ltr'>${EvmCalculator.formatIndex(it.cpi)}</span></li>"
            }
        }
        return """
            <!DOCTYPE html>
            <html lang="ar" dir="rtl">
            <head>
              <meta charset="UTF-8" />
              <style>
                @page { size: A4; margin: 16mm; }
                body { font-family: sans-serif; color: #111827; line-height: 1.6; }
                .header { border-bottom: 3px solid #7A1416; padding-bottom: 10px; margin-bottom: 16px; }
                h1 { color: #7A1416; margin: 0 0 4px 0; font-size: 22px; }
                .sub { color: #4B5563; font-size: 13px; }
                .grid { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 16px; }
                .kpi { border: 1px solid #E2E8F0; border-radius: 10px; padding: 10px 14px; min-width: 120px; flex: 1; background: #F8FAFC; }
                .kpi-title { font-size: 11px; color: #6B7280; }
                .kpi-val { font-size: 18px; font-weight: bold; color: #111827; margin-top: 4px; }
                .summary { background: #FCE8E8; border-right: 4px solid #7A1416; padding: 10px 14px; border-radius: 8px; margin-bottom: 16px; }
                table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 12px; }
                th, td { border: 1px solid #CBD5E1; padding: 7px; text-align: center; }
                th { background: #7A1416; color: #FFFFFF; }
              </style>
            </head>
            <body>
              <div class="header">
                <h1>تقرير الأداء الشهري لتكلفة المشروع</h1>
                <div class="sub">المشروع: <strong>${state.setup.name}</strong> | النوع: ${state.setup.type.labelAr} | تاريخ المتابعة: الشهر ${snapshot.statusMonth} من ${state.setup.durationMonths} | الوحدة: $unit</div>
              </div>
              <div class="summary"><strong>الملخص التنفيذي:</strong> ${snapshot.summarySentenceAr}</div>
              <div class="grid">
                <div class="kpi"><div class="kpi-title">الخط المرجعي (BAC)</div><div class="kpi-val" dir="ltr">${EvmCalculator.formatMoney(snapshot.bac, p)}</div></div>
                <div class="kpi"><div class="kpi-title">القيمة المخططة (PV)</div><div class="kpi-val" dir="ltr">${EvmCalculator.formatMoney(snapshot.pv, p)}</div></div>
                <div class="kpi"><div class="kpi-title">القيمة المكتسبة (EV)</div><div class="kpi-val" dir="ltr">${EvmCalculator.formatMoney(snapshot.ev, p)}</div></div>
                <div class="kpi"><div class="kpi-title">التكلفة الفعلية (AC)</div><div class="kpi-val" dir="ltr">${EvmCalculator.formatMoney(snapshot.ac, p)}</div></div>
                <div class="kpi"><div class="kpi-title">مؤشر التكلفة (CPI)</div><div class="kpi-val" dir="ltr">${EvmCalculator.formatIndex(snapshot.cpi)}</div></div>
                <div class="kpi"><div class="kpi-title">مؤشر الجدول (SPI)</div><div class="kpi-val" dir="ltr">${EvmCalculator.formatIndex(snapshot.spi)}</div></div>
                <div class="kpi"><div class="kpi-title">التقدير النهائي (EAC)</div><div class="kpi-val" dir="ltr">${EvmCalculator.formatMoney(snapshot.activeEac, p)}</div></div>
                <div class="kpi"><div class="kpi-title">الانحراف النهائي (VAC)</div><div class="kpi-val" dir="ltr">${EvmCalculator.formatMoney(snapshot.vac, p)}</div></div>
              </div>
              <h3>أكثر 3 حزم عمل انحرافًا</h3>
              <ul>$worstHtml</ul>
              <h3>تفصيل حزم العمل (WBS)</h3>
              <table>
                <thead>
                  <tr><th>الكود</th><th>الحزمة</th><th>الموازنة</th><th>PV</th><th>EV</th><th>AC</th><th>CV</th><th>CPI</th></tr>
                </thead>
                <tbody>$rowsHtml</tbody>
              </table>
            </body>
            </html>
        """.trimIndent()
    }

    private fun escapeCsv(text: String): String {
        val escaped = text.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
