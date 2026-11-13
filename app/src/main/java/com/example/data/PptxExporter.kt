package com.example.data

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.domain.*
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class SlideSpec(
    val titleAr: String,
    val badge: String,
    val subtitleAr: String,
    val bulletsAr: List<String>,
    val baladiTipAr: String,
    val renderPreview: (Canvas, RectF, Typeface, Typeface) -> Unit
)

/**
 * Generates a standard Office Open XML (.pptx) presentation containing high-resolution
 * visual screen previews populated with the project's live data and detailed Arabic explanations.
 */
object PptxExporter {

    private const val SLIDE_WIDTH_EMU = 12192000L  // 13.333 in (16:9 widescreen)
    private const val SLIDE_HEIGHT_EMU = 6858000L  // 7.5 in

    fun exportToPptx(
        context: Context,
        state: ProjectState,
        snapshot: EvmSnapshot,
        budget: BudgetBreakdown,
        outputStream: OutputStream
    ) {
        val cairo = runCatching { ResourcesCompat.getFont(context, R.font.cairo) }.getOrNull() ?: Typeface.DEFAULT
        val inter = runCatching { ResourcesCompat.getFont(context, R.font.inter) }.getOrNull() ?: Typeface.MONOSPACE

        val slides = buildSlides(state, snapshot, budget)

        ZipOutputStream(outputStream).use { zip ->
            writeZipEntry(zip, "[Content_Types].xml", contentTypesXml(slides.size))
            writeZipEntry(zip, "_rels/.rels", rootRelsXml())
            writeZipEntry(zip, "ppt/presentation.xml", presentationXml(slides.size))
            writeZipEntry(zip, "ppt/_rels/presentation.xml.rels", presentationRelsXml(slides.size))
            writeZipEntry(zip, "ppt/theme/theme1.xml", themeXml())
            writeZipEntry(zip, "ppt/slideMasters/slideMaster1.xml", slideMasterXml())
            writeZipEntry(zip, "ppt/slideMasters/_rels/slideMaster1.xml.rels", slideMasterRelsXml())
            writeZipEntry(zip, "ppt/slideLayouts/slideLayout1.xml", slideLayoutXml())
            writeZipEntry(zip, "ppt/slideLayouts/_rels/slideLayout1.xml.rels", slideLayoutRelsXml())

            slides.forEachIndexed { index, spec ->
                val slideNum = index + 1
                val pngBytes = renderPhoneScreenBitmap(spec, state, cairo, inter)
                zip.putNextEntry(ZipEntry("ppt/media/screen_$slideNum.png"))
                zip.write(pngBytes)
                zip.closeEntry()

                writeZipEntry(zip, "ppt/slides/slide$slideNum.xml", slideXml(slideNum, spec))
                writeZipEntry(zip, "ppt/slides/_rels/slide$slideNum.xml.rels", slideRelsXml(slideNum))
            }
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun renderPhoneScreenBitmap(
        spec: SlideSpec,
        state: ProjectState,
        cairo: Typeface,
        inter: Typeface
    ): ByteArray {
        val w = 540
        val h = 960
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Outer phone frame
        canvas.drawColor(Color.parseColor("#EEF2F6"))
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1E293B") }
        canvas.drawRoundRect(RectF(8f, 8f, w - 8f, h - 8f), 42f, 42f, framePaint)

        // Screen background
        val screenRect = RectF(20f, 20f, w - 20f, h - 20f)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F3F6F9") }
        canvas.drawRoundRect(screenRect, 32f, 32f, bgPaint)

        // Clip inside screen
        canvas.save()
        val clipPath = Path().apply { addRoundRect(screenRect, 32f, 32f, Path.Direction.CW) }
        canvas.clipPath(clipPath)

        // App TopAppBar
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRect(20f, 20f, w - 20f, 108f, barPaint)
        val dividerPaint = Paint().apply { color = Color.parseColor("#E2E8F0"); strokeWidth = 2f }
        canvas.drawLine(20f, 108f, w - 20f, 108f, dividerPaint)

        drawRtlText(
            canvas,
            state.setup.name,
            RectF(36f, 34f, w - 36f, 70f),
            cairo,
            20f,
            Color.parseColor("#111827"),
            bold = true
        )
        drawRtlText(
            canvas,
            "المتابعة: الشهر ${state.setup.statusDateMonth} من ${state.setup.durationMonths}",
            RectF(36f, 68f, w - 36f, 98f),
            cairo,
            14f,
            Color.parseColor("#6B7280")
        )

        // Crimson Section Banner
        val bannerRect = RectF(34f, 124f, w - 34f, 214f)
        val crimsonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#7A1416") }
        canvas.drawRoundRect(bannerRect, 18f, 18f, crimsonPaint)
        drawRtlText(
            canvas,
            "${if (spec.badge.isNotBlank()) "[${spec.badge}] " else ""}${spec.titleAr}",
            RectF(50f, 136f, w - 50f, 174f),
            cairo,
            20f,
            Color.WHITE,
            bold = true
        )
        drawRtlText(
            canvas,
            spec.subtitleAr,
            RectF(50f, 172f, w - 50f, 204f),
            cairo,
            13f,
            Color.parseColor("#FCE8E8")
        )

        // Screen content region
        val contentRect = RectF(34f, 228f, w - 34f, h - 36f)
        spec.renderPreview(canvas, contentRect, cairo, inter)

        canvas.restore()

        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
        bmp.recycle()
        return out.toByteArray()
    }

    private fun buildSlides(
        state: ProjectState,
        snapshot: EvmSnapshot,
        budget: BudgetBreakdown
    ): List<SlideSpec> {
        val p = state.plan.roundingPrecision
        val unit = "${state.setup.displayUnit.multiplierHint} ${state.setup.currency}".trim()

        return listOf(
            // Slide 1: Cover / Overview
            SlideSpec(
                titleAr = "إدارة تكلفة المشروع (PMBOK® 6)",
                badge = "نظرة عامة",
                subtitleAr = "Project Cost Management — الفصل السابع",
                bulletsAr = listOf(
                    "المشروع: ${state.setup.name} (${state.setup.type.labelAr})",
                    "المدة: ${state.setup.durationMonths} شهور | تاريخ المتابعة: الشهر ${snapshot.statusMonth}",
                    "الخط المرجعي للتكلفة (BAC): ${EvmCalculator.formatMoney(snapshot.bac, p)} $unit",
                    "موازنة المشروع الإجمالية: ${EvmCalculator.formatMoney(snapshot.projectBudget, p)} $unit",
                    "المؤشرات الحالية: CPI = ${EvmCalculator.formatIndex(snapshot.cpi)} | SPI = ${EvmCalculator.formatIndex(snapshot.spi)}"
                ),
                baladiTipAr = "التطبيق يجمع بين الأداة العملية لإدارة تكاليف مشاريع البناء والأداة التعليمية بميزة «اشرح لي» وحاسبة المعادلات بالبلدي."
            ) { canvas, area, cairo, inter ->
                drawCard(canvas, RectF(area.left, area.top, area.right, area.top + 170f), "#7A1416", "#FCE8E8")
                drawRtlText(canvas, "ملخص الحالة التنفيذي", RectF(area.left + 16f, area.top + 14f, area.right - 16f, area.top + 48f), cairo, 16f, Color.parseColor("#7A1416"), bold = true)
                drawRtlText(canvas, snapshot.summarySentenceAr, RectF(area.left + 16f, area.top + 52f, area.right - 16f, area.top + 150f), cairo, 15f, Color.parseColor("#111827"))

                drawMiniKpi(canvas, RectF(area.left, area.top + 190f, area.centerX() - 8f, area.top + 310f), "BAC", "الخط المرجعي", EvmCalculator.formatMoney(snapshot.bac, p), "#7A1416", cairo, inter)
                drawMiniKpi(canvas, RectF(area.centerX() + 8f, area.top + 190f, area.right, area.top + 310f), "EAC", "التقدير النهائي", EvmCalculator.formatMoney(snapshot.activeEac, p), "#1AA0DC", cairo, inter)
                drawMiniKpi(canvas, RectF(area.left, area.top + 326f, area.centerX() - 8f, area.top + 446f), "CPI", "مؤشر التكلفة", EvmCalculator.formatIndex(snapshot.cpi), "#D64040", cairo, inter)
                drawMiniKpi(canvas, RectF(area.centerX() + 8f, area.top + 326f, area.right, area.top + 446f), "SPI", "مؤشر الجدول", EvmCalculator.formatIndex(snapshot.spi), "#E0A21B", cairo, inter)

                drawMiniSCurve(canvas, RectF(area.left, area.top + 464f, area.right, area.bottom - 10f), snapshot, cairo, inter)
            },

            // Slide 2: Dashboard
            SlideSpec(
                titleAr = "لوحة المشروع (Dashboard)",
                badge = "اللوحة",
                subtitleAr = "ملخص المؤشرات والعدادات ومنحنى الأداء وأسوأ 3 حزم",
                bulletsAr = listOf(
                    "كروت KPI علوية: BAC (${EvmCalculator.formatMoney(snapshot.bac, p)})، الموازنة (${EvmCalculator.formatMoney(snapshot.projectBudget, p)})، AC (${EvmCalculator.formatMoney(snapshot.ac, p)})، EAC (${EvmCalculator.formatMoney(snapshot.activeEac, p)}).",
                    "عدادات Gauge ملونة لـ CPI (${EvmCalculator.formatIndex(snapshot.cpi)}) و SPI (${EvmCalculator.formatIndex(snapshot.spi)}) وفق حدود التحكم.",
                    "رسم منحنيات PV و EV و AC مع خط التوقع المتقطع حتى EAC.",
                    "قائمة أسوأ 3 حزم عمل حسب انحراف التكلفة CV لتحديد بؤر الخلل فورًا."
                ),
                baladiTipAr = "من نظرة واحدة على اللوحة بتعرف المشروع كسبان ولا خسران، سريع ولا متأخر، وفين المشكلة بالظبط."
            ) { canvas, area, cairo, inter ->
                drawMiniSCurve(canvas, RectF(area.left, area.top, area.right, area.top + 250f), snapshot, cairo, inter)
                drawCard(canvas, RectF(area.left, area.top + 266f, area.right, area.top + 460f), "#E2E8F0", "#FFFFFF")
                drawRtlText(canvas, "أسوأ حزم العمل (حسب CV)", RectF(area.left + 16f, area.top + 278f, area.right - 16f, area.top + 312f), cairo, 16f, Color.parseColor("#111827"), bold = true)
                var y = area.top + 320f
                snapshot.worstThreePackages.take(3).forEach { row ->
                    drawRtlText(canvas, "• ${row.pkg.name}: CV = ${EvmCalculator.formatMoney(row.cv, p)} | CPI = ${EvmCalculator.formatIndex(row.cpi)}", RectF(area.left + 16f, y, area.right - 16f, y + 38f), cairo, 14f, Color.parseColor("#D64040"))
                    y += 42f
                }
                drawMiniKpi(canvas, RectF(area.left, area.top + 476f, area.right, area.top + 596f), "VAC", "الانحراف المتوقع عند الاكتمال", "${EvmCalculator.formatMoney(snapshot.vac, p)} (${EvmCalculator.formatSignedPercent(snapshot.vacPercent)})", "#D64040", cairo, inter)
            },

            // Slide 3: 7.1 Plan Cost Management
            SlideSpec(
                titleAr = "خطة إدارة التكاليف",
                badge = "7.1",
                subtitleAr = "قواعد القياس والدقة وحدود التحكم (Control Thresholds)",
                bulletsAr = listOf(
                    "تحديد وحدات القياس المعتمدة للمقايسة (${state.plan.unitsOfMeasure.joinToString("، ")}).",
                    "مستوى الدقة: التقريب للعرض لأقرب ${state.plan.roundingPrecision} مع بقاء الحسابات الداخلية بدقة كاملة.",
                    "حدود التحكم: النطاق المقبول (${EvmCalculator.formatIndex(state.plan.acceptableMin)}–${EvmCalculator.formatIndex(state.plan.acceptableMax)})، ونطاق التحذير يبدأ من ${EvmCalculator.formatIndex(state.plan.warningMin)}.",
                    "أي تغيير في حدود التحكم يغيّر ألوان كل المؤشرات والتنبيهات في التطبيق فورًا."
                ),
                baladiTipAr = "دي القواعد والدستور اللي المشروع كله هيمشي عليه؛ إمتى نقول المؤشر أخضر وإمتى نضرب جرس إنذار أحمر."
            ) { canvas, area, cairo, inter ->
                drawCard(canvas, RectF(area.left, area.top, area.right, area.top + 220f), "#7A1416", "#FFFFFF")
                drawRtlText(canvas, "نطاقات حدود التحكم (CPI / SPI)", RectF(area.left + 16f, area.top + 16f, area.right - 16f, area.top + 50f), cairo, 16f, Color.parseColor("#111827"), bold = true)
                drawCard(canvas, RectF(area.left + 16f, area.top + 64f, area.right - 16f, area.top + 104f), "#2F9E62", "#E7F6EE")
                drawRtlText(canvas, "حالة جيدة: أكبر من أو يساوي ${EvmCalculator.formatIndex(state.plan.acceptableMin)}", RectF(area.left + 26f, area.top + 70f, area.right - 26f, area.top + 98f), cairo, 14f, Color.parseColor("#2F9E62"), bold = true)
                drawCard(canvas, RectF(area.left + 16f, area.top + 114f, area.right - 16f, area.top + 154f), "#E0A21B", "#FEF6E2")
                drawRtlText(canvas, "تحذير: بين ${EvmCalculator.formatIndex(state.plan.warningMin)} و ${EvmCalculator.formatIndex(state.plan.acceptableMin)}", RectF(area.left + 26f, area.top + 120f, area.right - 26f, area.top + 148f), cairo, 14f, Color.parseColor("#B45309"), bold = true)
                drawCard(canvas, RectF(area.left + 16f, area.top + 164f, area.right - 16f, area.top + 204f), "#D64040", "#FCEAEA")
                drawRtlText(canvas, "حرج / سيئ: أقل من ${EvmCalculator.formatIndex(state.plan.warningMin)}", RectF(area.left + 26f, area.top + 170f, area.right - 26f, area.top + 198f), cairo, 14f, Color.parseColor("#D64040"), bold = true)

                drawCard(canvas, RectF(area.left, area.top + 240f, area.right, area.top + 410f), "#E2E8F0", "#FFFFFF")
                drawRtlText(canvas, "طريقة EAC الافتراضية ودورية التقارير", RectF(area.left + 16f, area.top + 254f, area.right - 16f, area.top + 288f), cairo, 16f, Color.parseColor("#111827"), bold = true)
                drawRtlText(canvas, "• طريقة EAC: ${state.plan.defaultEacMethod.labelAr}", RectF(area.left + 16f, area.top + 298f, area.right - 16f, area.top + 334f), cairo, 14f, Color.parseColor("#4B5563"))
                drawRtlText(canvas, "• دورية المتابعة: ${state.plan.reportingFrequency.labelAr}", RectF(area.left + 16f, area.top + 340f, area.right - 16f, area.top + 376f), cairo, 14f, Color.parseColor("#4B5563"))
            },

            // Slide 4: 7.2 Cost Estimation
            SlideSpec(
                titleAr = "تقدير التكاليف (WBS)",
                badge = "7.2",
                subtitleAr = "طرق التقدير الأربع + احتياطي الطوارئ + شارات الدقة",
                bulletsAr = listOf(
                    "التقدير التناظري (Analogous)، والبارامتري (Parametric)، ومن القاعدة للقمة (Bottom-up)، وثلاثي النقاط (مثلثي وبيتا PERT).",
                    "حساب الانحراف المعياري σ = (cP − cO) / 6 في ثلاثي النقاط كمؤشر لعدم اليقين.",
                    "تخصيص احتياطي طوارئ لكل حزمة مع توثيق الخطر المرتبط بها.",
                    "إجمالي التقديرات: ${EvmCalculator.formatMoney(budget.activityEstimatesSum, p)} + الطوارئ: ${EvmCalculator.formatMoney(budget.activityContingencySum, p)} = ${EvmCalculator.formatMoney(budget.workPackagesTotal, p)} $unit."
                ),
                baladiTipAr = "بنسعّر كل بند في المشروع بالطريقة المناسبة لدقته، ونزود له قرش طوارئ للمخاطر اللي إحنا عارفينها."
            ) { canvas, area, cairo, inter ->
                var y = area.top
                state.workPackages.take(4).forEach { pkg ->
                    val est = EvmCalculator.calculatePackageBaseEstimate(pkg)
                    val cont = EvmCalculator.calculatePackageContingency(pkg)
                    drawCard(canvas, RectF(area.left, y, area.right, y + 110f), "#E2E8F0", "#FFFFFF")
                    drawRtlText(canvas, "${pkg.code} — ${pkg.name} (${pkg.estimationMethod.labelAr})", RectF(area.left + 14f, y + 10f, area.right - 14f, y + 44f), cairo, 15f, Color.parseColor("#111827"), bold = true)
                    drawRtlText(canvas, "التقدير: ${EvmCalculator.formatMoney(est, p)} | الطوارئ: ${EvmCalculator.formatMoney(cont, p)} | الدقة: ${pkg.estimationMethod.accuracyLabelAr}", RectF(area.left + 14f, y + 52f, area.right - 14f, y + 92f), cairo, 13.5f, Color.parseColor("#4B5563"))
                    y += 124f
                }
                drawMiniKpi(canvas, RectF(area.left, y + 10f, area.right, y + 125f), "Total", "مجموع الحزم + الطوارئ", "${EvmCalculator.formatMoney(budget.workPackagesTotal, p)} $unit", "#7A1416", cairo, inter)
            },

            // Slide 5: 7.3 Determine Budget & Funding
            SlideSpec(
                titleAr = "تحديد الموازنة والتمويل",
                badge = "7.3",
                subtitleAr = "سُلّم الموازنة والخط المرجعي BAC والتوزيع الزمني وتسوية التمويل",
                bulletsAr = listOf(
                    "تجميع هرمي تلقائي: تقديرات الأنشطة + الطوارئ → حزم العمل → حسابات المراقبة → BAC (${EvmCalculator.formatMoney(budget.bac, p)}).",
                    "إضافة الاحتياطي الإداري (${EvmCalculator.formatMoney(budget.currentManagementReserve, p)}) فوق BAC لتكوين موازنة المشروع (${EvmCalculator.formatMoney(budget.projectBudget, p)}).",
                    "توليد منحنى القيمة المخططة التراكمي (PV) تلقائيًا حسب نمط التوزيع الزمني.",
                    "رسم متطلبات التمويل (stepAfter) مع تنبيه أحمر فوري إذا تجاوز الإنفاق المخطط التمويل المتاح."
                ),
                baladiTipAr = "بنرص الفلوس على شهور المشروع (منحنى S) ونتأكد إن الدفعات الجاية من المالك مغطية الصرف المخطط في كل شهر."
            ) { canvas, area, cairo, inter ->
                drawCard(canvas, RectF(area.left, area.top, area.right, area.top + 250f), "#F5B323", "#FFFFFF")
                drawRtlText(canvas, "سُلّم مكونات الموازنة", RectF(area.left + 14f, area.top + 12f, area.right - 14f, area.top + 46f), cairo, 16f, Color.parseColor("#7A1416"), bold = true)
                val ladder = listOf(
                    "1. تقديرات الأنشطة: ${EvmCalculator.formatMoney(budget.activityEstimatesSum, p)}",
                    "2. + احتياطي الطوارئ: ${EvmCalculator.formatMoney(budget.activityContingencySum, p)}",
                    "3. = الخط المرجعي (BAC): ${EvmCalculator.formatMoney(budget.bac, p)}",
                    "4. + الاحتياطي الإداري: ${EvmCalculator.formatMoney(budget.currentManagementReserve, p)}",
                    "5. = موازنة المشروع: ${EvmCalculator.formatMoney(budget.projectBudget, p)}"
                )
                var ly = area.top + 54f
                ladder.forEach { item ->
                    drawRtlText(canvas, item, RectF(area.left + 16f, ly, area.right - 16f, ly + 32f), cairo, 14f, Color.parseColor("#111827"))
                    ly += 36f
                }

                drawCard(canvas, RectF(area.left, area.top + 266f, area.right, area.bottom - 16f), "#2F9E62", "#FFFFFF")
                drawRtlText(canvas, "التمويل التراكمي vs الخط المرجعي", RectF(area.left + 14f, area.top + 278f, area.right - 14f, area.top + 312f), cairo, 16f, Color.parseColor("#2F9E62"), bold = true)
                val msg = if (snapshot.fundingShortfallMonths.isEmpty()) {
                    "حالة التمويل: ممتازة — التمويل التراكمي يغطي الإنفاق المخطط في جميع الشهور."
                } else {
                    "تحذير عجز تمويل في الشهور: ${snapshot.fundingShortfallMonths.joinToString("، ")}"
                }
                drawRtlText(canvas, msg, RectF(area.left + 16f, area.top + 324f, area.right - 16f, area.top + 420f), cairo, 15f, Color.parseColor("#111827"))
            },

            // Slide 6: 7.4 Control Costs
            SlideSpec(
                titleAr = "ضبط التكاليف (EVM)",
                badge = "7.4",
                subtitleAr = "متابعة الأداء الدوري وحساب CV و SV و CPI و SPI",
                bulletsAr = listOf(
                    "إدخال نسبة الإنجاز % والتكلفة الفعلية الدورية لكل حزمة عمل حتى تاريخ المتابعة (الشهر ${snapshot.statusMonth}).",
                    "النتائج الحالية: PV = ${EvmCalculator.formatMoney(snapshot.pv, p)}، EV = ${EvmCalculator.formatMoney(snapshot.ev, p)}، AC = ${EvmCalculator.formatMoney(snapshot.ac, p)}.",
                    "الانحرافات: CV = ${EvmCalculator.formatMoney(snapshot.cv, p)} | SV = ${EvmCalculator.formatMoney(snapshot.sv, p)}.",
                    "ترتيب حزم العمل حسب الأسوأ أداءً لمعرفة أين يحدث الاستنزاف المالي."
                ),
                baladiTipAr = "كل المعادلات بتبدأ بـ EV! بنقارن اللي خلص على الأرض باللي كان مخطط واللي اتصرف فعليًا عشان نعرف إحنا فين."
            ) { canvas, area, cairo, inter ->
                drawMiniKpi(canvas, RectF(area.left, area.top, area.centerX() - 8f, area.top + 120f), "CV", "انحراف التكلفة", EvmCalculator.formatMoney(snapshot.cv, p), "#D64040", cairo, inter)
                drawMiniKpi(canvas, RectF(area.centerX() + 8f, area.top, area.right, area.top + 120f), "SV", "انحراف الجدول", EvmCalculator.formatMoney(snapshot.sv, p), "#E0A21B", cairo, inter)
                drawMiniKpi(canvas, RectF(area.left, area.top + 136f, area.centerX() - 8f, area.top + 256f), "CPI", "مؤشر التكلفة", EvmCalculator.formatIndex(snapshot.cpi), "#D64040", cairo, inter)
                drawMiniKpi(canvas, RectF(area.centerX() + 8f, area.top + 136f, area.right, area.top + 256f), "SPI", "مؤشر الجدول", EvmCalculator.formatIndex(snapshot.spi), "#E0A21B", cairo, inter)
                drawMiniSCurve(canvas, RectF(area.left, area.top + 276f, area.right, area.bottom - 12f), snapshot, cairo, inter)
            },

            // Slide 7: Forecasting
            SlideSpec(
                titleAr = "التنبؤ بالتكلفة النهائية",
                badge = "التنبؤ",
                subtitleAr = "طرق EAC الثلاث و ETC و VAC و TCPI وحكم واقعية الموازنة",
                bulletsAr = listOf(
                    "EAC (استثنائي): ${EvmCalculator.formatMoney(snapshot.eacAtypical, p)} | EAC (مستمر BAC/CPI): ${EvmCalculator.formatMoney(snapshot.eacTypical, p)} | EAC (مركب): ${EvmCalculator.formatMoney(snapshot.eacCombined, p)}.",
                    "المتبقي صرفه ETC = ${EvmCalculator.formatMoney(snapshot.etc, p)} والانحراف النهائي VAC = ${EvmCalculator.formatMoney(snapshot.vac, p)}.",
                    "مؤشر الأداء المطلوب للموازنة الأصلية TCPI(BAC) = ${EvmCalculator.formatIndex(snapshot.tcpiBac)}.",
                    "كارت «حكم واقعية الموازنة» ينبهك فورًا إذا كان الالتزام بـ BAC غير واقعي مع زر إنشاء طلب تغيير جاهز."
                ),
                baladiTipAr = "التنبؤ بيقولك من بدري المشروع هيقفل على كام في الآخر، وهل لسة في أمل نلحق الميزانية الأصلية ولا لازم نرفع طلب تغيير."
            ) { canvas, area, cairo, inter ->
                drawMiniKpi(canvas, RectF(area.left, area.top, area.right, area.top + 115f), "EAC₁", "انحراف استثنائي: AC + (BAC − EV)", EvmCalculator.formatMoney(snapshot.eacAtypical, p), "#1AA0DC", cairo, inter)
                drawMiniKpi(canvas, RectF(area.left, area.top + 130f, area.right, area.top + 245f), "EAC₂", "استمرار الأداء: BAC / CPI", EvmCalculator.formatMoney(snapshot.eacTypical, p), "#7A1416", cairo, inter)
                drawMiniKpi(canvas, RectF(area.left, area.top + 260f, area.right, area.top + 375f), "EAC₃", "مركب التكلفة والجدول", EvmCalculator.formatMoney(snapshot.eacCombined, p), "#F28C28", cairo, inter)
                drawMiniKpi(canvas, RectF(area.left, area.top + 390f, area.centerX() - 8f, area.top + 510f), "ETC", "المتبقي صرفه", EvmCalculator.formatMoney(snapshot.etc, p), "#1E9C9C", cairo, inter)
                drawMiniKpi(canvas, RectF(area.centerX() + 8f, area.top + 390f, area.right, area.top + 510f), "TCPI", "الكفاءة المطلوبة", EvmCalculator.formatIndex(snapshot.tcpiBac), "#D64040", cairo, inter)
            },

            // Slide 8: Change Requests & Explain To Me
            SlideSpec(
                titleAr = "سجل التغيير وميزة «اشرح لي»",
                badge = "التعليم والحوكمة",
                subtitleAr = "التحكم المتكامل في التغيير + الشرح التفاعلي خطوة بخطوة",
                bulletsAr = listOf(
                    "اعتماد طلب تغيير من الاحتياطي الإداري ينقل المبلغ إلى BAC مع بقاء إجمالي موازنة المشروع ثابتًا.",
                    "حفظ تاريخ كل نسخة من الخط المرجعي (Baseline History) للمراجعة والتدقيق.",
                    "زر «اشرح لي» (Info) بجانب كل رقم يعرض 7 أقسام: الاسم العربي والإنجليزي، المعادلة، التعويض الفعلي، التفسير، والأخطاء الشائعة.",
                    "قاموس مرجعي قابل للبحث وجدول مقارنة بين احتياطي الطوارئ والاحتياطي الإداري."
                ),
                baladiTipAr = "أي تعديل في الميزانية لازم يتم بطلب تغيير رسمي، وأي رقم قدامك تقدر تدوس جنبه على علامة (i) يشرحلك اتحسب إزاي بأرقام مشروعك."
            ) { canvas, area, cairo, inter ->
                drawCard(canvas, RectF(area.left, area.top, area.right, area.top + 270f), "#7A1416", "#FFFFFF")
                drawRtlText(canvas, "بطاقة «اشرح لي» التفاعلية", RectF(area.left + 16f, area.top + 14f, area.right - 16f, area.top + 48f), cairo, 16f, Color.parseColor("#7A1416"), bold = true)
                drawRtlText(canvas, "• الاسم: مؤشر أداء التكلفة (Cost Performance Index)", RectF(area.left + 16f, area.top + 56f, area.right - 16f, yPlus(area.top + 56f, 34f)), cairo, 13.5f, Color.parseColor("#111827"))
                drawLtrBox(canvas, RectF(area.left + 16f, area.top + 100f, area.right - 16f, area.top + 150f), "CPI = EV / AC = 80 / 120 = 0.67", inter)
                drawRtlText(canvas, "• خطأ شائع: CPI يقيس الأداء الحالي، بينما TCPI يقيس الأداء المطلوب مستقبلًا.", RectF(area.left + 16f, area.top + 164f, area.right - 16f, area.top + 250f), cairo, 13.5f, Color.parseColor("#92400E"))

                drawCard(canvas, RectF(area.left, area.top + 290f, area.right, area.bottom - 16f), "#1AA0DC", "#EEF6FF")
                drawRtlText(canvas, "سجل طلبات التغيير (${state.changeRequests.size} طلبات)", RectF(area.left + 16f, area.top + 306f, area.right - 16f, area.top + 340f), cairo, 16f, Color.parseColor("#0F3D6E"), bold = true)
                drawRtlText(canvas, "القاعدة: إدارة التكلفة تكتشف الانحراف وتقترح الحل، والاعتماد يتم عبر التحكم المتكامل في التغيير.", RectF(area.left + 16f, area.top + 350f, area.right - 16f, area.top + 450f), cairo, 14f, Color.parseColor("#111827"))
            },

            // Slide 9: Baladi Equations Calculator & Reports
            SlideSpec(
                titleAr = "حاسبة المعادلات بالبلدي والتقارير",
                badge = "الحاسبة والتقارير",
                subtitleAr = "شرح بالبلدي أثناء إدخال كل قيمة + تصدير PDF و Excel و JSON و PowerPoint",
                bulletsAr = listOf(
                    "حاسبة مستقلة بـ 3 تبويبات: معادلات EVM والتنبؤ، التقدير ثلاثي النقاط PERT، والموازنة والاحتياطيات.",
                    "شرح فوري بالبلدي تحت كل خانة إدخال وأعلى الشاشة أثناء الكتابة، وتفسير بالبلدي لكل نتيجة.",
                    "زر «سحب أرقام مشروعي» لتجربة السيناريوهات دون تعديل بيانات المشروع الفعلية.",
                    "تصدير تقرير شهري عربي A4 (PDF)، جداول Excel (CSV)، نسخة احتياطية JSON، وعرض PowerPoint (.pptx)."
                ),
                baladiTipAr = "تقدر تجرب أي أرقام في الحاسبة وتفهم كل خانة معناها إيه بالبلدي، وفي الآخر تطبع تقرير PDF أو تعرض شغلك في PowerPoint جاهز."
            ) { canvas, area, cairo, inter ->
                drawCard(canvas, RectF(area.left, area.top, area.right, area.top + 145f), "#F5B323", "#FFF8EB")
                drawRtlText(canvas, "بتكتب دلوقتي في: EV (القيمة المكتسبة)", RectF(area.left + 16f, area.top + 12f, area.right - 16f, area.top + 46f), cairo, 15f, Color.parseColor("#92400E"), bold = true)
                drawRtlText(canvas, "بالبلدي: الشغل اللي خلص فعلًا على الأرض لحد دلوقتي يساوي كام بأسعار الميزانية الأصلية.", RectF(area.left + 16f, area.top + 52f, area.right - 16f, area.top + 132f), cairo, 13.5f, Color.parseColor("#111827"))

                drawCard(canvas, RectF(area.left, area.top + 162f, area.right, area.top + 370f), "#7A1416", "#FFFFFF")
                drawRtlText(canvas, "نتيجة فورية مع التعويض:", RectF(area.left + 16f, area.top + 174f, area.right - 16f, area.top + 206f), cairo, 15f, Color.parseColor("#111827"), bold = true)
                drawLtrBox(canvas, RectF(area.left + 16f, area.top + 214f, area.right - 16f, area.top + 264f), "CV = 80 − 120 = −40", inter)
                drawRtlText(canvas, "بالبلدي: صرفنا أكتر من حق الشغل اللي خلص بـ 40؛ يعني خسرانين في التكلفة لحد دلوقتي.", RectF(area.left + 16f, area.top + 276f, area.right - 16f, area.top + 356f), cairo, 13.5f, Color.parseColor("#D64040"))

                drawCard(canvas, RectF(area.left, area.top + 388f, area.right, area.bottom - 16f), "#2F9E62", "#E7F6EE")
                drawRtlText(canvas, "التقارير والتصدير المتاحة: A4 PDF • Excel CSV • JSON • PowerPoint (.pptx)", RectF(area.left + 16f, area.top + 408f, area.right - 16f, area.bottom - 30f), cairo, 15f, Color.parseColor("#2F9E62"), bold = true)
            }
        )
    }

    private fun yPlus(y: Float, delta: Float) = y + delta

    private fun drawCard(canvas: Canvas, rect: RectF, borderHex: String, fillHex: String) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(fillHex) }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(borderHex)
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(rect, 18f, 18f, fill)
        canvas.drawRoundRect(rect, 18f, 18f, stroke)
    }

    private fun drawMiniKpi(
        canvas: Canvas,
        rect: RectF,
        symbol: String,
        titleAr: String,
        valueLtr: String,
        accentHex: String,
        cairo: Typeface,
        inter: Typeface
    ) {
        drawCard(canvas, rect, accentHex, "#FFFFFF")
        drawLtrText(canvas, symbol, RectF(rect.left + 14f, rect.top + 12f, rect.right - 14f, rect.top + 38f), inter, 14f, Color.parseColor(accentHex), bold = true)
        drawRtlText(canvas, titleAr, RectF(rect.left + 14f, rect.top + 36f, rect.right - 14f, rect.top + 66f), cairo, 13.5f, Color.parseColor("#4B5563"))
        drawLtrText(canvas, valueLtr, RectF(rect.left + 14f, rect.top + 68f, rect.right - 14f, rect.bottom - 10f), inter, 22f, Color.parseColor(accentHex), bold = true)
    }

    private fun drawLtrBox(canvas: Canvas, rect: RectF, text: String, inter: Typeface) {
        drawCard(canvas, rect, "#CBD5E1", "#F1F5F9")
        drawLtrText(canvas, text, RectF(rect.left + 12f, rect.top + 12f, rect.right - 12f, rect.bottom - 10f), inter, 15f, Color.parseColor("#0F3D6E"), bold = true)
    }

    private fun drawMiniSCurve(
        canvas: Canvas,
        rect: RectF,
        snapshot: EvmSnapshot,
        cairo: Typeface,
        inter: Typeface
    ) {
        drawCard(canvas, rect, "#CBD5E1", "#FFFFFF")
        drawRtlText(canvas, "منحنيات القيمة المكتسبة (PV / EV / AC)", RectF(rect.left + 14f, rect.top + 10f, rect.right - 14f, rect.top + 42f), cairo, 14f, Color.parseColor("#111827"), bold = true)

        val plot = RectF(rect.left + 38f, rect.top + 48f, rect.right - 20f, rect.bottom - 26f)
        val gridPaint = Paint().apply { color = Color.parseColor("#E5E7EB"); strokeWidth = 1.5f }
        for (i in 0..3) {
            val gy = plot.top + plot.height() * (i / 3f)
            canvas.drawLine(plot.left, gy, plot.right, gy, gridPaint)
        }

        val pv = snapshot.pvSeries
        val ev = snapshot.evSeries
        val ac = snapshot.acSeries
        val maxVal = listOfNotNull(pv.maxOrNull(), ev.maxOrNull(), ac.maxOrNull(), snapshot.activeEac, snapshot.bac).maxOrNull()?.coerceAtLeast(10.0) ?: 200.0
        val yMax = maxVal * 1.1
        val n = pv.size.coerceAtLeast(1)

        fun px(m: Int) = plot.left + (m.toFloat() / n.toFloat()) * plot.width()
        fun py(v: Double) = plot.bottom - ((v / yMax).toFloat().coerceIn(0f, 1f) * plot.height())

        fun strokeSeries(series: List<Double>, hex: String, dashed: Boolean = false, startM: Int = 0) {
            if (series.isEmpty()) return
            val path = Path()
            if (startM == 0) {
                path.moveTo(px(0), py(0.0))
                series.forEachIndexed { idx, v -> path.lineTo(px(idx + 1), py(v)) }
            } else {
                series.forEachIndexed { idx, v ->
                    if (idx == 0) path.moveTo(px(startM), py(v)) else path.lineTo(px(startM + idx), py(v))
                }
            }
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(hex)
                style = Paint.Style.STROKE
                strokeWidth = 4f
                strokeCap = Paint.Cap.ROUND
                if (dashed) pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
            }
            canvas.drawPath(path, p)
        }

        strokeSeries(pv, "#7A1416")
        strokeSeries(ev, "#1E9C9C")
        strokeSeries(ac, "#F28C28")
        if (snapshot.forecastSeries.size > 1) {
            strokeSeries(snapshot.forecastSeries, "#1AA0DC", dashed = true, startM = snapshot.statusMonth)
        }
        drawLtrText(canvas, "PV • EV • AC • Forecast", RectF(plot.left, rect.bottom + 2f, plot.right, rect.bottom + 22f), inter, 11f, Color.parseColor("#6B7280"))
    }

    private fun drawRtlText(
        canvas: Canvas,
        text: String,
        rect: RectF,
        typeface: Typeface,
        sizePx: Float,
        color: Int,
        bold: Boolean = false
    ) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.textSize = sizePx
            this.typeface = if (bold) Typeface.create(typeface, Typeface.BOLD) else typeface
        }
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, rect.width().toInt().coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
            .setLineSpacing(0f, 1.15f)
            .build()
        canvas.save()
        canvas.translate(rect.left, rect.top)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawLtrText(
        canvas: Canvas,
        text: String,
        rect: RectF,
        typeface: Typeface,
        sizePx: Float,
        color: Int,
        bold: Boolean = false
    ) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.textSize = sizePx
            this.typeface = if (bold) Typeface.create(typeface, Typeface.BOLD) else typeface
        }
        val layout = StaticLayout.Builder
            .obtain("\u200E$text", 0, text.length + 1, paint, rect.width().toInt().coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        canvas.save()
        canvas.translate(rect.left, rect.top)
        layout.draw(canvas)
        canvas.restore()
    }

    // =========================================================================
    // Standard Office Open XML (.pptx) Templates
    // =========================================================================

    private fun escapeXml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun contentTypesXml(slideCount: Int): String {
        val slideOverrides = (1..slideCount).joinToString("\n") { i ->
            """  <Override PartName="/ppt/slides/slide$i.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Default Extension="png" ContentType="image/png"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
  <Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>
  <Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>
  <Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>
$slideOverrides
</Types>"""
    }

    private fun rootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
</Relationships>"""

    private fun presentationXml(slideCount: Int): String {
        val slideIds = (1..slideCount).joinToString("\n") { i ->
            """    <p:sldId id="${255 + i}" r:id="rId${i + 1}"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
                xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:sldMasterIdLst>
    <p:sldMasterId id="2147483648" r:id="rId1"/>
  </p:sldMasterIdLst>
  <p:sldIdLst>
$slideIds
  </p:sldIdLst>
  <p:sldSz cx="$SLIDE_WIDTH_EMU" cy="$SLIDE_HEIGHT_EMU"/>
  <p:notesSz cx="$SLIDE_HEIGHT_EMU" cy="$SLIDE_WIDTH_EMU"/>
</p:presentation>"""
    }

    private fun presentationRelsXml(slideCount: Int): String {
        val slideRels = (1..slideCount).joinToString("\n") { i ->
            """  <Relationship Id="rId${i + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide$i.xml"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>
$slideRels
  <Relationship Id="rId${slideCount + 2}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="theme/theme1.xml"/>
</Relationships>"""
    }

    private fun slideRelsXml(slideNum: Int): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="../media/screen_$slideNum.png"/>
</Relationships>"""

    private fun slideXml(slideNum: Int, spec: SlideSpec): String {
        val bulletsXml = spec.bulletsAr.joinToString("\n") { bullet ->
            """          <a:p>
            <a:pPr algn="r" rtl="1" marR="0" indent="0"/>
            <a:r>
              <a:rPr lang="ar-EG" sz="1650" dirty="0">
                <a:solidFill><a:srgbClr val="111827"/></a:solidFill>
              </a:rPr>
              <a:t>• ${escapeXml(bullet)}</a:t>
            </a:r>
          </a:p>"""
        }

        val titlePrefix = if (spec.badge.isNotBlank()) "[${spec.badge}] " else ""

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
       xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
       xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:bg>
      <p:bgPr>
        <a:solidFill><a:srgbClr val="F3F6F9"/></a:solidFill>
        <a:effectLst/>
      </p:bgPr>
    </p:bg>
    <p:spTree>
      <p:nvGrpSpPr>
        <p:cNvPr id="1" name=""/>
        <p:cNvGrpSpPr/>
        <p:nvPr/>
      </p:nvGrpSpPr>
      <p:grpSpPr/>

      <!-- Top Brand Header Bar -->
      <p:sp>
        <p:nvSpPr><p:cNvPr id="2" name="HeaderBar"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>
        <p:spPr>
          <a:xfrm><a:off x="0" y="0"/><a:ext cx="$SLIDE_WIDTH_EMU" cy="1150000"/></a:xfrm>
          <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
          <a:solidFill><a:srgbClr val="7A1416"/></a:solidFill>
          <a:ln><a:noFill/></a:ln>
        </p:spPr>
        <p:txBody>
          <a:bodyPr wrap="square" anchor="ctr" lIns="350000" rIns="450000" tIns="100000" bIns="100000"/>
          <a:lstStyle/>
          <a:p>
            <a:pPr algn="r" rtl="1"/>
            <a:r>
              <a:rPr lang="ar-EG" sz="2800" b="1"><a:solidFill><a:srgbClr val="FFFFFF"/></a:solidFill></a:rPr>
              <a:t>${escapeXml(titlePrefix + spec.titleAr)}</a:t>
            </a:r>
          </a:p>
          <a:p>
            <a:pPr algn="r" rtl="1"/>
            <a:r>
              <a:rPr lang="ar-EG" sz="1500"><a:solidFill><a:srgbClr val="FCE8E8"/></a:solidFill></a:rPr>
              <a:t>${escapeXml(spec.subtitleAr)}</a:t>
            </a:r>
          </a:p>
        </p:txBody>
      </p:sp>

      <!-- Left Screen Preview Image (540x960 scaled nicely to fit slide height) -->
      <p:pic>
        <p:nvPicPr>
          <p:cNvPr id="3" name="ScreenPreview_$slideNum"/>
          <p:cNvPicPr><a:picLocks noChangeAspect="1"/></p:cNvPicPr>
          <p:nvPr/>
        </p:nvPicPr>
        <p:blipFill>
          <a:blip r:embed="rId2"/>
          <a:stretch><a:fillRect/></a:stretch>
        </p:blipFill>
        <p:spPr>
          <a:xfrm>
            <a:off x="480000" y="1350000"/>
            <a:ext cx="2925000" cy="5200000"/>
          </a:xfrm>
          <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
        </p:spPr>
      </p:pic>

      <!-- Right Explanation Card -->
      <p:sp>
        <p:nvSpPr><p:cNvPr id="4" name="ExplanationCard"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>
        <p:spPr>
          <a:xfrm><a:off x="3800000" y="1400000"/><a:ext cx="7900000" cy="3650000"/></a:xfrm>
          <a:prstGeom prst="roundRect"><a:avLst><a:gd name="adj" fmla="val 4000"/></a:avLst></a:prstGeom>
          <a:solidFill><a:srgbClr val="FFFFFF"/></a:solidFill>
          <a:ln w="19050"><a:solidFill><a:srgbClr val="CBD5E1"/></a:solidFill></a:ln>
        </p:spPr>
        <p:txBody>
          <a:bodyPr wrap="square" lIns="280000" rIns="280000" tIns="220000" bIns="180000"/>
          <a:lstStyle/>
          <a:p>
            <a:pPr algn="r" rtl="1"/>
            <a:r>
              <a:rPr lang="ar-EG" sz="2000" b="1"><a:solidFill><a:srgbClr val="7A1416"/></a:solidFill></a:rPr>
              <a:t>شرح الشاشة والمكونات الأساسية:</a:t>
            </a:r>
          </a:p>
$bulletsXml
        </p:txBody>
      </p:sp>

      <!-- Bottom-Right Baladi Summary Callout -->
      <p:sp>
        <p:nvSpPr><p:cNvPr id="5" name="BaladiCallout"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>
        <p:spPr>
          <a:xfrm><a:off x="3800000" y="5250000"/><a:ext cx="7900000" cy="1250000"/></a:xfrm>
          <a:prstGeom prst="roundRect"><a:avLst><a:gd name="adj" fmla="val 6000"/></a:avLst></a:prstGeom>
          <a:solidFill><a:srgbClr val="FFF8EB"/></a:solidFill>
          <a:ln w="19050"><a:solidFill><a:srgbClr val="F5B323"/></a:solidFill></a:ln>
        </p:spPr>
        <p:txBody>
          <a:bodyPr wrap="square" anchor="ctr" lIns="240000" rIns="240000" tIns="120000" bIns="120000"/>
          <a:lstStyle/>
          <a:p>
            <a:pPr algn="r" rtl="1"/>
            <a:r>
              <a:rPr lang="ar-EG" sz="1650" b="1"><a:solidFill><a:srgbClr val="92400E"/></a:solidFill></a:rPr>
              <a:t>الخلاصة بالبلدي: </a:t>
            </a:r>
            <a:r>
              <a:rPr lang="ar-EG" sz="1600"><a:solidFill><a:srgbClr val="111827"/></a:solidFill></a:rPr>
              <a:t>${escapeXml(spec.baladiTipAr)}</a:t>
            </a:r>
          </a:p>
        </p:txBody>
      </p:sp>
    </p:spTree>
  </p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sld>"""
    }

    private fun slideMasterXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldMaster xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
             xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
             xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr/>
    </p:spTree>
  </p:cSld>
  <p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2" accent3="accent3" accent4="accent4" accent5="accent5" accent6="accent6" hlink="hlink" folHlink="folHlink"/>
  <p:sldLayoutIdLst>
    <p:sldLayoutId id="2147483649" r:id="rId1"/>
  </p:sldLayoutIdLst>
</p:sldMaster>"""

    private fun slideMasterRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="../theme/theme1.xml"/>
</Relationships>"""

    private fun slideLayoutXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldLayout xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
             xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
             xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" type="blank" preserve="1">
  <p:cSld name="Blank">
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr/>
    </p:spTree>
  </p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sldLayout>"""

    private fun slideLayoutRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="../slideMasters/slideMaster1.xml"/>
</Relationships>"""

    private fun themeXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="ProjectCostTheme">
  <a:themeElements>
    <a:clrScheme name="ProjectCost">
      <a:dk1><a:srgbClr val="111827"/></a:dk1>
      <a:lt1><a:srgbClr val="FFFFFF"/></a:lt1>
      <a:dk2><a:srgbClr val="4B5563"/></a:dk2>
      <a:lt2><a:srgbClr val="F3F6F9"/></a:lt2>
      <a:accent1><a:srgbClr val="7A1416"/></a:accent1>
      <a:accent2><a:srgbClr val="1E9C9C"/></a:accent2>
      <a:accent3><a:srgbClr val="F28C28"/></a:accent3>
      <a:accent4><a:srgbClr val="1AA0DC"/></a:accent4>
      <a:accent5><a:srgbClr val="F5B323"/></a:accent5>
      <a:accent6><a:srgbClr val="2F9E62"/></a:accent6>
      <a:hlink><a:srgbClr val="1AA0DC"/></a:hlink>
      <a:folHlink><a:srgbClr val="7A1416"/></a:folHlink>
    </a:clrScheme>
    <a:fontScheme name="CairoInter">
      <a:majorFont><a:latin typeface="Inter"/><a:ea typeface=""/><a:cs typeface="Cairo"/></a:majorFont>
      <a:minorFont><a:latin typeface="Inter"/><a:ea typeface=""/><a:cs typeface="Cairo"/></a:minorFont>
    </a:fontScheme>
    <a:fmtScheme name="Default">
      <a:fillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:fillStyleLst>
      <a:lnStyleLst><a:ln w="9525"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:ln></a:lnStyleLst>
      <a:effectStyleLst><a:effectStyle><a:effectLst/></a:effectStyle></a:effectStyleLst>
      <a:bgFillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:bgFillStyleLst>
    </a:fmtScheme>
  </a:themeElements>
</a:theme>"""
}
