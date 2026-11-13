package com.example.domain

enum class MetricId(val symbol: String) {
    BAC("BAC"),
    PV("PV"),
    EV("EV"),
    AC("AC"),
    CV("CV"),
    SV("SV"),
    CPI("CPI"),
    SPI("SPI"),
    EAC_ATYPICAL("EAC₁"),
    EAC_TYPICAL("EAC₂"),
    EAC_COMBINED("EAC₃"),
    EAC_MANUAL("EAC₄"),
    EAC("EAC"),
    ETC("ETC"),
    VAC("VAC"),
    TCPI_BAC("TCPI(BAC)"),
    TCPI_EAC("TCPI(EAC)"),
    MANAGEMENT_RESERVE("MR"),
    PROJECT_BUDGET("Budget"),
    CONTINGENCY("CR"),
    THREE_POINT_BETA("PERT"),
    THREE_POINT_TRIANGULAR("Triangular"),
    THREE_POINT_SIGMA("σ")
}

data class MetricExplanation(
    val id: MetricId,
    val symbol: String,
    val nameAr: String,
    val nameEn: String,
    val questionAr: String,
    val formulaLtr: String,
    val substitutionLtr: String,
    val resultLtr: String,
    val interpretationAr: String,
    val commonMistakeAr: String,
    val status: MetricStatus,
    val relatedMetrics: List<MetricId>
)

data class GlossaryEntry(
    val id: MetricId,
    val symbol: String,
    val nameAr: String,
    val nameEn: String,
    val formulaLtr: String,
    val questionAr: String,
    val interpretationRuleAr: String,
    val commonMistakeAr: String
)

object ExplainContent {

    val glossaryEntries: List<GlossaryEntry> = listOf(
        GlossaryEntry(
            id = MetricId.BAC,
            symbol = "BAC",
            nameAr = "الموازنة عند الاكتمال",
            nameEn = "Budget at Completion",
            formulaLtr = "BAC = Σ(Work Packages + Contingency Reserves)",
            questionAr = "ما إجمالي الموازنة المعتمدة في الخط المرجعي لإنجاز كامل المشروع؟",
            interpretationRuleAr = "هو المرجع الثابت لقياس التقدم والأداء؛ لا يتغير إلا عبر طلب تغيير معتمد.",
            commonMistakeAr = "الاحتياطي الإداري (Management Reserve) لا يدخل في حسابات القيمة المكتسبة، ولا يُضاف إلى BAC."
        ),
        GlossaryEntry(
            id = MetricId.PV,
            symbol = "PV",
            nameAr = "القيمة المخططة",
            nameEn = "Planned Value",
            formulaLtr = "PV(t) = Planned % at t × BAC",
            questionAr = "ما قيمة الأعمال التي كان يُفترض إنجازها حتى تاريخ المتابعة حسب الخطة؟",
            interpretationRuleAr = "يمثل المسار الزمني للخط المرجعي للتكلفة (منحنى S)؛ عند نهاية المشروع يصبح PV = BAC.",
            commonMistakeAr = "الخلط بين PV والتكلفة الفعلية AC؛ القيمة المخططة هي الموازنة المعتمدة للعمل المجدول وليس ما أُنفق فعلًا."
        ),
        GlossaryEntry(
            id = MetricId.EV,
            symbol = "EV",
            nameAr = "القيمة المكتسبة",
            nameEn = "Earned Value",
            formulaLtr = "EV = Σ(% Complete × Package Budget)",
            questionAr = "ما قيمة العمل الفعلي الذي أُنجز على أرض الواقع مقوّمًا بموازنة الخط المرجعي؟",
            interpretationRuleAr = "هو نقطة الانطلاق في جميع معادلات الانحراف والمؤشرات (CV, SV, CPI, SPI).",
            commonMistakeAr = "كل معادلات CV و SV تبدأ دائمًا بـ EV، وليس PV أو AC. كتابة AC − EV تعكس الإشارة وتضلل القرار!"
        ),
        GlossaryEntry(
            id = MetricId.AC,
            symbol = "AC",
            nameAr = "التكلفة الفعلية",
            nameEn = "Actual Cost",
            formulaLtr = "AC = Σ(Period Actual Costs up to t)",
            questionAr = "كم أنفقنا فعليًا (فواتير، مستخلصات، عمالة، مواد) حتى تاريخ المتابعة؟",
            interpretationRuleAr = "يُقارن مع EV لمعرفة كفاءة الصرف، ومع التمويل التراكمي للتأكد من السيولة.",
            commonMistakeAr = "إدخال أوامر الشراء المستقبلية قبل تحقق التكلفة، أو مقارنة AC مع PV مباشرة دون استخدام EV."
        ),
        GlossaryEntry(
            id = MetricId.CV,
            symbol = "CV",
            nameAr = "انحراف التكلفة",
            nameEn = "Cost Variance",
            formulaLtr = "CV = EV − AC",
            questionAr = "هل أنفقنا أكثر أم أقل من قيمة العمل المنجز حتى الآن؟",
            interpretationRuleAr = "موجب (+) = وفر في التكلفة، صفر = مطابق للموازنة، سالب (−) = تجاوز للتكلفة.",
            commonMistakeAr = "CV رقم مالي مطلق لا يصلح للمقارنة بين مشروعات مختلفة الحجم؛ للمقارنة نستخدم CPI. وتذكر أن المعادلة تبدأ بـ EV وليس AC."
        ),
        GlossaryEntry(
            id = MetricId.SV,
            symbol = "SV",
            nameAr = "انحراف الجدول",
            nameEn = "Schedule Variance",
            formulaLtr = "SV = EV − PV",
            questionAr = "هل نحن متقدمون أم متأخرون عن الجدول الزمني المخطط؟",
            interpretationRuleAr = "موجب (+) = متقدم عن الجدول، صفر = مطابق للخطة، سالب (−) = متأخر عن الجدول.",
            commonMistakeAr = "SV يقيس الانحراف بالقيمة المالية وليس بالأيام! وفي نهاية المشروع يعود SV إلى صفر و SPI إلى 1 دائمًا حتى لو تأخر المشروع، لأن EV = PV = BAC."
        ),
        GlossaryEntry(
            id = MetricId.CPI,
            symbol = "CPI",
            nameAr = "مؤشر أداء التكلفة",
            nameEn = "Cost Performance Index",
            formulaLtr = "CPI = EV / AC",
            questionAr = "كل وحدة نقدية نصرفها فعليًا، كم تنتج من قيمة العمل المخطط؟",
            interpretationRuleAr = "أكبر من 1.00 = كفاءة ممتازة (وفر)، يساوي 1.00 = على الموازنة، أقل من 1.00 = تجاوز تكلفة.",
            commonMistakeAr = "مقارنة CPI مع TCPI كأنهما نفس الشيء: CPI يقيس الأداء الفعلي الحالي، بينما TCPI يقيس الأداء المطلوب مستقبلًا."
        ),
        GlossaryEntry(
            id = MetricId.SPI,
            symbol = "SPI",
            nameAr = "مؤشر أداء الجدول",
            nameEn = "Schedule Performance Index",
            formulaLtr = "SPI = EV / PV",
            questionAr = "بأي سرعة ننجز الأعمال مقارنةً بالجدول الزمني المخطط؟",
            interpretationRuleAr = "أكبر من 1.00 = متقدم زمنيًا، يساوي 1.00 = مطابق للخطة، أقل من 1.00 = متأخر زمنيًا.",
            commonMistakeAr = "الاعتماد على SPI وحده دون المسار الحرج، ونسيان أن SPI يصل إلى 1.00 تلقائيًا عند اكتمال كل الأعمال حتى مع التأخير."
        ),
        GlossaryEntry(
            id = MetricId.EAC,
            symbol = "EAC",
            nameAr = "التقدير عند الاكتمال",
            nameEn = "Estimate at Completion",
            formulaLtr = "EAC = BAC / CPI  |  AC + (BAC − EV)  |  AC + (BAC − EV)/(CPI × SPI)",
            questionAr = "كم ستكون التكلفة الإجمالية المتوقعة للمشروع كله عند انتهائه؟",
            interpretationRuleAr = "يُقارن مباشرة مع BAC لتحديد العجز أو الوفر النهائي المتوقع (VAC).",
            commonMistakeAr = "الخلط بين EAC و ETC: الأول هو التكلفة الإجمالية من البداية للنهاية، أما ETC فهو المتبقي صرفه من الآن فصاعدًا فقط."
        ),
        GlossaryEntry(
            id = MetricId.ETC,
            symbol = "ETC",
            nameAr = "التقدير حتى الاكتمال",
            nameEn = "Estimate to Complete",
            formulaLtr = "ETC = EAC − AC",
            questionAr = "كم يتبقى علينا أن نصرف من تاريخ المتابعة وحتى إنهاء المشروع؟",
            interpretationRuleAr = "يُستخدم لتخطيط التدفقات النقدية المتبقية ومراجعة كفاية التمويل.",
            commonMistakeAr = "الخلط بين ETC (المتبقي فقط) و EAC (الإجمالي من أول المشروع لآخره)."
        ),
        GlossaryEntry(
            id = MetricId.VAC,
            symbol = "VAC",
            nameAr = "الانحراف عند الاكتمال",
            nameEn = "Variance at Completion",
            formulaLtr = "VAC = BAC − EAC",
            questionAr = "عند انتهاء المشروع، كم سنوفر أو نتجاوز مقارنةً بالموازنة الأصلية؟",
            interpretationRuleAr = "موجب (+) = وفر متوقع عند النهاية، سالب (−) = عجز / تجاوز متوقع للموازنة.",
            commonMistakeAr = "كتابة EAC − BAC فتظهر الإشارة معكوسة؛ القاعدة الثابتة في PMBOK هي الموازنة ناقص التوقع (BAC − EAC)."
        ),
        GlossaryEntry(
            id = MetricId.TCPI_BAC,
            symbol = "TCPI",
            nameAr = "مؤشر الأداء حتى الاكتمال",
            nameEn = "To-Complete Performance Index",
            formulaLtr = "TCPI(BAC) = (BAC − EV) / (BAC − AC)",
            questionAr = "ما الكفاءة المطلوب تحقيقها في الأعمال المتبقية للالتزام بالموازنة؟",
            interpretationRuleAr = "إذا كان TCPI أعلى بكثير من CPI الحالي (بأكثر من 0.10) فالالتزام بالموازنة الأصلية غير واقعي.",
            commonMistakeAr = "الخلط بين CPI (الأداء الفعلي الماضي) و TCPI (الأداء المطلوب مستقبلًا)، أو حساب TCPI(BAC) بعد استنفاد BAC بالكامل (BAC − AC ≤ 0)."
        )
    )

    fun explain(
        metricId: MetricId,
        state: ProjectState,
        snapshot: EvmSnapshot
    ): MetricExplanation {
        val unitLabel = buildString {
            if (state.setup.displayUnit.multiplierHint.isNotEmpty()) {
                append(state.setup.displayUnit.multiplierHint)
                append(" ")
            }
            append(state.setup.currency)
        }
        val p = state.plan.roundingPrecision
        val bac = snapshot.bac
        val pv = snapshot.pv
        val ev = snapshot.ev
        val ac = snapshot.ac
        val cpi = snapshot.cpi
        val spi = snapshot.spi
        val activeEac = snapshot.activeEac

        fun fmtM(v: Double?) = EvmCalculator.formatMoney(v, p)
        fun fmtEx(v: Double?) = EvmCalculator.formatExactMoney(v)
        fun fmtI(v: Double?) = EvmCalculator.formatIndex(v)

        return when (metricId) {
            MetricId.BAC -> MetricExplanation(
                id = MetricId.BAC,
                symbol = "BAC",
                nameAr = "الموازنة عند الاكتمال",
                nameEn = "Budget at Completion",
                questionAr = "ما إجمالي الموازنة المعتمدة في الخط المرجعي لإنجاز كامل المشروع؟",
                formulaLtr = "BAC = Work Packages Total + Approved Baseline Changes",
                substitutionLtr = "BAC = ${fmtEx(bac)} $unitLabel",
                resultLtr = "${fmtM(bac)} $unitLabel",
                interpretationAr = "يمثل BAC السقف المالي المعتمد لقياس الأداء بالقيمة المكتسبة طوال المشروع. أي تغيير عليه لا يتم إلا باعتماد طلب تغيير رسمي.",
                commonMistakeAr = "الاحتياطي الإداري (Management Reserve) لا يدخل في حسابات القيمة المكتسبة، ولا يُضاف إلى BAC.",
                status = MetricStatus.NEUTRAL,
                relatedMetrics = listOf(MetricId.PROJECT_BUDGET, MetricId.MANAGEMENT_RESERVE, MetricId.EAC)
            )

            MetricId.PV -> MetricExplanation(
                id = MetricId.PV,
                symbol = "PV",
                nameAr = "القيمة المخططة",
                nameEn = "Planned Value",
                questionAr = "ما قيمة الأعمال التي كان مخططًا إنجازها حتى الشهر ${snapshot.statusMonth}؟",
                formulaLtr = "PV = Cumulative Planned Baseline at Month ${snapshot.statusMonth}",
                substitutionLtr = "PV(M${snapshot.statusMonth}) = ${fmtEx(pv)} $unitLabel",
                resultLtr = "${fmtM(pv)} $unitLabel",
                interpretationAr = "يمثل ${if (bac > 0) "${((pv / bac) * 100).toInt()}%" else "0%"} من إجمالي الخط المرجعي (BAC) المقرر إنجازها حتى تاريخ المتابعة.",
                commonMistakeAr = "الخلط بين القيمة المخططة PV والتكلفة الفعلية AC؛ القيمة المخططة تُستخرج من خط الأساس المعتمد وليس من الفواتير.",
                status = MetricStatus.NEUTRAL,
                relatedMetrics = listOf(MetricId.EV, MetricId.SV, MetricId.SPI)
            )

            MetricId.EV -> MetricExplanation(
                id = MetricId.EV,
                symbol = "EV",
                nameAr = "القيمة المكتسبة",
                nameEn = "Earned Value",
                questionAr = "ما قيمة الشغل الفعلي المُنجز على أرض الواقع بتسعير الموازنة المعتمدة؟",
                formulaLtr = "EV = Σ(% Complete × Package Budget)",
                substitutionLtr = "EV(M${snapshot.statusMonth}) = ${fmtEx(ev)} $unitLabel",
                resultLtr = "${fmtM(ev)} $unitLabel",
                interpretationAr = "أنجز المشروع فعليًا ${if (bac > 0) "${((ev / bac) * 100).toInt()}%" else "0%"} من قيمة الأعمال المعتمدة في الخط المرجعي.",
                commonMistakeAr = "تذكّر دائمًا: كل معادلات CV و SV تبدأ بـ EV، وليس PV أو AC.",
                status = MetricStatus.NEUTRAL,
                relatedMetrics = listOf(MetricId.CV, MetricId.SV, MetricId.CPI, MetricId.SPI)
            )

            MetricId.AC -> MetricExplanation(
                id = MetricId.AC,
                symbol = "AC",
                nameAr = "التكلفة الفعلية",
                nameEn = "Actual Cost",
                questionAr = "كم صرفنا فعليًا حتى الشهر ${snapshot.statusMonth}؟",
                formulaLtr = "AC = Σ(Actual Costs from Month 1 to ${snapshot.statusMonth})",
                substitutionLtr = "AC(M${snapshot.statusMonth}) = ${fmtEx(ac)} $unitLabel",
                resultLtr = "${fmtM(ac)} $unitLabel",
                interpretationAr = "إجمالي التكاليف الفعلية المتراكمة المنصرفة على حزم العمل حتى تاريخ المتابعة.",
                commonMistakeAr = "مقارنة AC مع PV مباشرة دون EV يعطي صورة مضللة؛ قد يكون الصرف أقل بسبب تأخر التنفيذ وليس بسبب التوفير!",
                status = MetricStatus.NEUTRAL,
                relatedMetrics = listOf(MetricId.CV, MetricId.CPI, MetricId.ETC)
            )

            MetricId.CV -> MetricExplanation(
                id = MetricId.CV,
                symbol = "CV",
                nameAr = "انحراف التكلفة",
                nameEn = "Cost Variance",
                questionAr = "هل نحن موفرون أم متجاوزون للموازنة في الشغل الذي أُنجز؟",
                formulaLtr = "CV = EV − AC",
                substitutionLtr = "CV = ${fmtEx(ev)} − ${fmtEx(ac)} = ${fmtEx(snapshot.cv)}",
                resultLtr = "${fmtM(snapshot.cv)} $unitLabel",
                interpretationAr = when {
                    snapshot.cv > 1e-6 -> "وفر في التكلفة بمقدار ${fmtM(snapshot.cv)} $unitLabel؛ العمل المنجز كلف أقل من المخطط له."
                    snapshot.cv < -1e-6 -> "تجاوز في التكلفة بمقدار ${fmtM(-snapshot.cv)} $unitLabel؛ صرفنا أكثر من قيمة العمل المنجز."
                    else -> "التكلفة الفعلية مطابقة تمامًا لقيمة العمل المنجز."
                },
                commonMistakeAr = "1) كل معادلات الانحراف تبدأ بـ EV وليس AC. 2) CV رقم مطلق لا يصلح للمقارنة بين مشروعات مختلفة الحجم، بينما CPI نسبة تصلح لذلك.",
                status = snapshot.cvStatus,
                relatedMetrics = listOf(MetricId.EV, MetricId.AC, MetricId.CPI)
            )

            MetricId.SV -> MetricExplanation(
                id = MetricId.SV,
                symbol = "SV",
                nameAr = "انحراف الجدول",
                nameEn = "Schedule Variance",
                questionAr = "هل نحن متقدمون أم متأخرون عن خطة العمل المجدولة؟",
                formulaLtr = "SV = EV − PV",
                substitutionLtr = "SV = ${fmtEx(ev)} − ${fmtEx(pv)} = ${fmtEx(snapshot.sv)}",
                resultLtr = "${fmtM(snapshot.sv)} $unitLabel",
                interpretationAr = when {
                    snapshot.sv > 1e-6 -> "متقدم عن الجدول بقيمة أعمال ${fmtM(snapshot.sv)} $unitLabel."
                    snapshot.sv < -1e-6 -> "متأخر عن الجدول بقيمة أعمال غير منجزة قدرها ${fmtM(-snapshot.sv)} $unitLabel."
                    else -> "حجم الأعمال المنجزة مطابق للجدول المخطط."
                },
                commonMistakeAr = "SV يقيس الانحراف بالقيمة المالية وليس بالأيام. وفي نهاية المشروع يعود SV إلى صفر و SPI إلى 1 دائمًا حتى لو كان المشروع متأخرًا، لأن EV = PV = BAC.",
                status = snapshot.svStatus,
                relatedMetrics = listOf(MetricId.EV, MetricId.PV, MetricId.SPI)
            )

            MetricId.CPI -> MetricExplanation(
                id = MetricId.CPI,
                symbol = "CPI",
                nameAr = "مؤشر أداء التكلفة",
                nameEn = "Cost Performance Index",
                questionAr = "كل ${state.setup.currency} بنصرفه بيطلّع شغل بكام؟",
                formulaLtr = "CPI = EV / AC",
                substitutionLtr = if (ac == 0.0) "CPI = ${fmtEx(ev)} / 0 (لا يمكن القسمة على صفر)" else "CPI = ${fmtEx(ev)} / ${fmtEx(ac)} = ${fmtI(cpi)}",
                resultLtr = fmtI(cpi),
                interpretationAr = buildIndexInterpretation(cpi, "التكلفة", state.plan),
                commonMistakeAr = "CPI يقيس الأداء الفعلي الحالي، بينما TCPI يقيس الأداء المطلوب مستقبلًا. كما أن CPI يصلح للمقارنة بين مشروعات مختلفة الحجم عكس CV.",
                status = snapshot.cpiStatus,
                relatedMetrics = listOf(MetricId.CV, MetricId.EAC_TYPICAL, MetricId.TCPI_BAC)
            )

            MetricId.SPI -> MetricExplanation(
                id = MetricId.SPI,
                symbol = "SPI",
                nameAr = "مؤشر أداء الجدول",
                nameEn = "Schedule Performance Index",
                questionAr = "ما معدل سرعة التنفيذ مقارنةً بالخطة الزمنية المعتمدة؟",
                formulaLtr = "SPI = EV / PV",
                substitutionLtr = if (pv == 0.0) "SPI = ${fmtEx(ev)} / 0 (لا يمكن القسمة على صفر)" else "SPI = ${fmtEx(ev)} / ${fmtEx(pv)} = ${fmtI(spi)}",
                resultLtr = fmtI(spi),
                interpretationAr = buildIndexInterpretation(spi, "الجدول", state.plan),
                commonMistakeAr = "في نهاية المشروع يعود SV إلى صفر و SPI إلى 1 دائمًا حتى لو كان المشروع متأخرًا زمنيًا، لأن EV = PV = BAC عند اكتمال كل الأعمال.",
                status = snapshot.spiStatus,
                relatedMetrics = listOf(MetricId.SV, MetricId.EAC_COMBINED, MetricId.PV)
            )

            MetricId.EAC_ATYPICAL -> MetricExplanation(
                id = MetricId.EAC_ATYPICAL,
                symbol = "EAC₁",
                nameAr = "التقدير عند الاكتمال (انحراف استثنائي)",
                nameEn = "Estimate at Completion (Atypical)",
                questionAr = "كم سيكلف المشروع إجمالًا إذا اعتبرنا أن ما حدث كان طارئًا ولن يتكرر؟",
                formulaLtr = "EAC = AC + (BAC − EV)",
                substitutionLtr = "EAC = ${fmtEx(ac)} + (${fmtEx(bac)} − ${fmtEx(ev)}) = ${fmtEx(snapshot.eacAtypical)}",
                resultLtr = "${fmtM(snapshot.eacAtypical)} $unitLabel",
                interpretationAr = "تفترض هذه الطريقة أن الأعمال المتبقية ستُنفذ بالتكلفة المخططة الأصلية (CPI = 1.00).",
                commonMistakeAr = "الخلط بين EAC و ETC: الأول هو الإجمالي من البداية للنهاية، والثاني هو المتبقي صرفه فقط.",
                status = snapshot.cvStatus,
                relatedMetrics = listOf(MetricId.EAC_TYPICAL, MetricId.ETC, MetricId.VAC)
            )

            MetricId.EAC_TYPICAL, MetricId.EAC -> MetricExplanation(
                id = MetricId.EAC,
                symbol = "EAC",
                nameAr = "التقدير عند الاكتمال (استمرار الأداء)",
                nameEn = "Estimate at Completion (Typical CPI)",
                questionAr = "كم ستكون التكلفة الإجمالية للمشروع إذا استمر معدل كفاءة الصرف الحالي؟",
                formulaLtr = "EAC = BAC / CPI",
                substitutionLtr = if (cpi == null || cpi == 0.0) {
                    "EAC = ${fmtEx(bac)} / ${fmtI(cpi)} (غير قابل للحساب)"
                } else {
                    "EAC = ${fmtEx(bac)} / (${fmtEx(ev)} / ${fmtEx(ac)}) = ${fmtEx(snapshot.eacTypical)}"
                },
                resultLtr = "${fmtM(snapshot.eacTypical)} $unitLabel",
                interpretationAr = "تُحسب القيمة بأخذ CPI بدقته الكاملة (${if (cpi != null) String.format("%.4f", cpi) else "—"}) لتجنب أخطاء التقريب. وهي الطريقة الأكثر شيوعًا في مشروعات البناء.",
                commonMistakeAr = "1) الخلط بين EAC (الإجمالي من البداية للنهاية) و ETC (المتبقي فقط). 2) استخدام CPI مقربًا لمنزلتين داخل المعادلة يعطي رقمًا غير دقيق (مثل 298.5 بدل 300).",
                status = snapshot.cpiStatus,
                relatedMetrics = listOf(MetricId.CPI, MetricId.ETC, MetricId.VAC, MetricId.TCPI_EAC)
            )

            MetricId.EAC_COMBINED -> MetricExplanation(
                id = MetricId.EAC_COMBINED,
                symbol = "EAC₃",
                nameAr = "التقدير عند الاكتمال (التكلفة والجدول معًا)",
                nameEn = "Estimate at Completion (CPI × SPI)",
                questionAr = "كم سيكلف المشروع إذا أثر كل من ضعف التكلفة وتأخر الجدول على الأعمال المتبقية؟",
                formulaLtr = "EAC = AC + (BAC − EV) / (CPI × SPI)",
                substitutionLtr = if (cpi == null || spi == null || cpi * spi == 0.0) {
                    "غير متاح لأن CPI × SPI = 0 أو غير قابل للحساب"
                } else {
                    "EAC = ${fmtEx(ac)} + (${fmtEx(bac)} − ${fmtEx(ev)}) / (${fmtI(cpi)} × ${fmtI(spi)}) = ${fmtEx(snapshot.eacCombined)}"
                },
                resultLtr = "${fmtM(snapshot.eacCombined)} $unitLabel",
                interpretationAr = "أكثر التقديرات تحفظًا؛ يُستخدم عندما يؤدي التأخير الزمني إلى زيادة التكاليف غير المباشرة أو غرامات أو تسريع أعمال.",
                commonMistakeAr = "التقريب المبكر لـ CPI أو SPI قبل ضربهما يؤدي لانحراف النتيجة النهائية.",
                status = if (snapshot.cpiStatus == MetricStatus.BAD || snapshot.spiStatus == MetricStatus.BAD) MetricStatus.BAD else snapshot.cpiStatus,
                relatedMetrics = listOf(MetricId.CPI, MetricId.SPI, MetricId.EAC_TYPICAL)
            )

            MetricId.EAC_MANUAL -> MetricExplanation(
                id = MetricId.EAC_MANUAL,
                symbol = "EAC₄",
                nameAr = "التقدير عند الاكتمال (تقدير يدوي)",
                nameEn = "Estimate at Completion (Bottom-up ETC)",
                questionAr = "كم سيكلف المشروع بناءً على إعادة تقدير فعلي للأعمال المتبقية من القاعدة للقمة؟",
                formulaLtr = "EAC = AC + Manual ETC",
                substitutionLtr = "EAC = ${fmtEx(ac)} + ${fmtEx(state.manualEtcInput)} = ${fmtEx(snapshot.eacManual)}",
                resultLtr = "${fmtM(snapshot.eacManual)} $unitLabel",
                interpretationAr = "مفيد عندما تتغير ظروف السوق أو أسعار المقاولين بشكل يجعل المعادلات التاريخية غير معبرة.",
                commonMistakeAr = "نسيان جمع التكلفة الفعلية المنصرفة سابقًا (AC) على التقدير الجديد للمتبقي (ETC).",
                status = MetricStatus.NEUTRAL,
                relatedMetrics = listOf(MetricId.ETC, MetricId.AC)
            )

            MetricId.ETC -> MetricExplanation(
                id = MetricId.ETC,
                symbol = "ETC",
                nameAr = "التقدير حتى الاكتمال",
                nameEn = "Estimate to Complete",
                questionAr = "فاضل نصرف كام من الآن حتى إغلاق المشروع؟",
                formulaLtr = "ETC = EAC − AC",
                substitutionLtr = if (activeEac == null) "غير قابل للحساب لأن EAC غير متاح" else "ETC = ${fmtEx(activeEac)} − ${fmtEx(ac)} = ${fmtEx(snapshot.etc)}",
                resultLtr = "${fmtM(snapshot.etc)} $unitLabel",
                interpretationAr = "هذا هو التمويل الإضافي المطلوب لتغطية الأعمال المتبقية بناءً على طريقة EAC المختارة (${state.selectedEacMethod.shortLabelAr}).",
                commonMistakeAr = "EAC مقابل ETC: الأول هو الإجمالي من البداية للنهاية، والثاني هو المتبقي فقط.",
                status = MetricStatus.NEUTRAL,
                relatedMetrics = listOf(MetricId.EAC, MetricId.AC, MetricId.TCPI_EAC)
            )

            MetricId.VAC -> MetricExplanation(
                id = MetricId.VAC,
                symbol = "VAC",
                nameAr = "الانحراف عند الاكتمال",
                nameEn = "Variance at Completion",
                questionAr = "هنزيد كام عن الموازنة الأصلية (أو هنوفر كام) عند نهاية المشروع؟",
                formulaLtr = "VAC = BAC − EAC",
                substitutionLtr = if (activeEac == null) "غير قابل للحساب" else "VAC = ${fmtEx(bac)} − ${fmtEx(activeEac)} = ${fmtEx(snapshot.vac)} (${EvmCalculator.formatSignedPercent(snapshot.vacPercent)})",
                resultLtr = "${fmtM(snapshot.vac)} $unitLabel",
                interpretationAr = when {
                    snapshot.vac == null -> "غير قابل للحساب."
                    snapshot.vac >= 0 -> "من المتوقع إنهاء المشروع بوفر قدره ${fmtM(snapshot.vac)} $unitLabel عن الموازنة المعتمدة."
                    else -> "من المتوقع تجاوز الموازنة الأصلية بمقدار ${fmtM(-snapshot.vac)} $unitLabel (${EvmCalculator.formatSignedPercent(snapshot.vacPercent)})."
                },
                commonMistakeAr = "القاعدة تبدأ بالموازنة الأصلية BAC مطروحًا منها التقدير النهائي EAC، وليس العكس.",
                status = if ((snapshot.vac ?: 0.0) >= 0) MetricStatus.GOOD else MetricStatus.BAD,
                relatedMetrics = listOf(MetricId.BAC, MetricId.EAC, MetricId.TCPI_BAC)
            )

            MetricId.TCPI_BAC -> MetricExplanation(
                id = MetricId.TCPI_BAC,
                symbol = "TCPI(BAC)",
                nameAr = "مؤشر الأداء المطلوب للموازنة الأصلية",
                nameEn = "To-Complete Performance Index (BAC)",
                questionAr = "ما الكفاءة المطلوب تحقيقها في المتبقي لكي نلتزم بالموازنة الأصلية BAC؟",
                formulaLtr = "TCPI(BAC) = (BAC − EV) / (BAC − AC)",
                substitutionLtr = if (bac - ac <= 0.0) {
                    "BAC − AC = ${fmtEx(bac - ac)} ≤ 0 (تم استنفاد الموازنة بالكامل؛ غير قابل للحساب)"
                } else {
                    "TCPI = (${fmtEx(bac)} − ${fmtEx(ev)}) / (${fmtEx(bac)} − ${fmtEx(ac)}) = ${fmtI(snapshot.tcpiBac)}"
                },
                resultLtr = fmtI(snapshot.tcpiBac),
                interpretationAr = when {
                    bac - ac <= 0.0 -> "تم استنفاد الموازنة الأصلية بالكامل أو تجاوزها؛ لم يعد ممكنًا الالتزام بـ BAC ويجب مراجعة التقدير."
                    snapshot.tcpiBac != null && cpi != null && snapshot.tcpiBac - cpi > 0.10 ->
                        "المطلوب كفاءة ${fmtI(snapshot.tcpiBac)} بينما الأداء الحالي ${fmtI(cpi)} فقط؛ الالتزام بالموازنة الأصلية غير واقعي عمليًا."
                    else -> "المطلوب كفاءة ${fmtI(snapshot.tcpiBac)}، وهو قريب من الأداء الحالي ويمكن تحقيقه بضبط التكاليف."
                },
                commonMistakeAr = "CPI مقابل TCPI: الأول هو الأداء الفعلي الحالي، والثاني هو الأداء المطلوب تحقيقه مستقبلًا.",
                status = when {
                    bac - ac <= 0.0 -> MetricStatus.BAD
                    snapshot.tcpiBac != null && cpi != null && snapshot.tcpiBac - cpi > 0.10 -> MetricStatus.BAD
                    snapshot.tcpiBac != null && snapshot.tcpiBac > 1.05 -> MetricStatus.WARNING
                    else -> MetricStatus.GOOD
                },
                relatedMetrics = listOf(MetricId.CPI, MetricId.TCPI_EAC, MetricId.VAC)
            )

            MetricId.TCPI_EAC -> MetricExplanation(
                id = MetricId.TCPI_EAC,
                symbol = "TCPI(EAC)",
                nameAr = "مؤشر الأداء المطلوب للتقدير الجديد",
                nameEn = "To-Complete Performance Index (EAC)",
                questionAr = "ما الكفاءة المطلوبة للالتزام بالتقدير النهائي الجديد EAC؟",
                formulaLtr = "TCPI(EAC) = (BAC − EV) / (EAC − AC)",
                substitutionLtr = if (activeEac == null || activeEac - ac <= 0.0) {
                    "غير قابل للحساب لأن EAC − AC ≤ 0"
                } else {
                    "TCPI = (${fmtEx(bac)} − ${fmtEx(ev)}) / (${fmtEx(activeEac)} − ${fmtEx(ac)}) = ${fmtI(snapshot.tcpiEac)}"
                },
                resultLtr = fmtI(snapshot.tcpiEac),
                interpretationAr = "عند اختيار EAC = BAC / CPI فإن TCPI(EAC) يساوي CPI الحالي تمامًا، مما يعني أن التقدير الجديد واقعي إذا استمر الأداء نفسه.",
                commonMistakeAr = "الخلط بين مقام TCPI(BAC) الذي يستخدم BAC ومقام TCPI(EAC) الذي يستخدم التقدير الجديد EAC.",
                status = MetricStatus.NEUTRAL,
                relatedMetrics = listOf(MetricId.TCPI_BAC, MetricId.EAC, MetricId.CPI)
            )

            MetricId.MANAGEMENT_RESERVE -> MetricExplanation(
                id = MetricId.MANAGEMENT_RESERVE,
                symbol = "MR",
                nameAr = "الاحتياطي الإداري",
                nameEn = "Management Reserve",
                questionAr = "ما المبلغ المحتجز لمواجهة المخاطر المجهولة غير المتوقعة (Unknown-Unknowns)؟",
                formulaLtr = "MR = Initial MR − Approved MR Transfers",
                substitutionLtr = "MR = ${fmtEx(snapshot.managementReserve)} $unitLabel",
                resultLtr = "${fmtM(snapshot.managementReserve)} $unitLabel",
                interpretationAr = "يخضع لصلاحية الإدارة العليا، ولا يُنفق منه إلا بعد اعتماد طلب تغيير رسمي ينقله إلى BAC.",
                commonMistakeAr = "الاحتياطي الإداري لا يدخل في حسابات القيمة المكتسبة (EVM)، ولا يدخل في BAC.",
                status = MetricStatus.NEUTRAL,
                relatedMetrics = listOf(MetricId.BAC, MetricId.PROJECT_BUDGET, MetricId.CONTINGENCY)
            )

            MetricId.PROJECT_BUDGET -> MetricExplanation(
                id = MetricId.PROJECT_BUDGET,
                symbol = "Budget",
                nameAr = "موازنة المشروع الإجمالية",
                nameEn = "Total Project Budget",
                questionAr = "ما إجمالي الأموال المخصصة للمشروع شاملةً الخط المرجعي والاحتياطي الإداري؟",
                formulaLtr = "Project Budget = BAC + Management Reserve",
                substitutionLtr = "Project Budget = ${fmtEx(bac)} + ${fmtEx(snapshot.managementReserve)} = ${fmtEx(snapshot.projectBudget)}",
                resultLtr = "${fmtM(snapshot.projectBudget)} $unitLabel",
                interpretationAr = "تمثل إجمالي التمويل المطلوب توفيره عبر دفعات المشروع لتغطية التكاليف المرجعية والاحتياطي الإداري.",
                commonMistakeAr = "قياس أداء المشروع (EV, CPI, VAC) يتم مقارنةً بـ BAC وليس مقارنةً بموازنة المشروع الإجمالية.",
                status = MetricStatus.NEUTRAL,
                relatedMetrics = listOf(MetricId.BAC, MetricId.MANAGEMENT_RESERVE)
            )

            MetricId.CONTINGENCY -> MetricExplanation(
                id = MetricId.CONTINGENCY,
                symbol = "CR",
                nameAr = "احتياطي الطوارئ",
                nameEn = "Contingency Reserve",
                questionAr = "ما المبلغ المخصص للمخاطر المعروفة المحددة داخل حزم العمل (Known-Unknowns)؟",
                formulaLtr = "Contingency = Σ(Package Contingency Reserves)",
                substitutionLtr = "Contingency = ${fmtEx(EvmCalculator.calculateBudgetBreakdown(state).activityContingencySum)} $unitLabel",
                resultLtr = "${fmtM(EvmCalculator.calculateBudgetBreakdown(state).activityContingencySum)} $unitLabel",
                interpretationAr = "يُدار بواسطة مدير المشروع، ويدخل ضمن الخط المرجعي للتكلفة BAC وحسابات القيمة المكتسبة.",
                commonMistakeAr = "الخلط بين احتياطي الطوارئ (جزء من BAC للمخاطر المعروفة) والاحتياطي الإداري (خارج BAC للمخاطر المجهولة).",
                status = MetricStatus.NEUTRAL,
                relatedMetrics = listOf(MetricId.BAC, MetricId.MANAGEMENT_RESERVE)
            )

            MetricId.THREE_POINT_BETA, MetricId.THREE_POINT_TRIANGULAR, MetricId.THREE_POINT_SIGMA -> {
                val samplePkg = state.workPackages.firstOrNull { it.estimationMethod == EstimationMethod.THREE_POINT }
                val cO = samplePkg?.cO ?: 50.0
                val cM = samplePkg?.cM ?: 56.0
                val cP = samplePkg?.cP ?: 74.0
                val beta = EvmCalculator.calculateThreePointBeta(cO, cM, cP)
                val tri = EvmCalculator.calculateThreePointTriangular(cO, cM, cP)
                val sigma = EvmCalculator.calculateThreePointSigma(cO, cP)
                MetricExplanation(
                    id = metricId,
                    symbol = if (metricId == MetricId.THREE_POINT_SIGMA) "σ" else "PERT",
                    nameAr = "التقدير ثلاثي النقاط والانحراف المعياري",
                    nameEn = "Three-Point Estimate & Standard Deviation",
                    questionAr = "كيف نحسب التقدير المتوقع ومؤشر عدم اليقين من القيم المتفائلة والأرجح والمتشائمة؟",
                    formulaLtr = "Beta = (cO + 4cM + cP)/6  |  Tri = (cO + cM + cP)/3  |  σ = (cP − cO)/6",
                    substitutionLtr = "cO=${fmtEx(cO)}, cM=${fmtEx(cM)}, cP=${fmtEx(cP)} -> Beta=${fmtI(beta)}, Tri=${fmtI(tri)}, σ=${fmtI(sigma)}",
                    resultLtr = "Beta=${fmtI(beta)} | σ=${fmtI(sigma)}",
                    interpretationAr = "توزيع بيتا (PERT) يعطي وزنًا أكبر (4×) للقيمة الأكثر احتمالًا cM. وكلما زادت قيمة σ زادت درجة المخاطرة وعدم اليقين في البند.",
                    commonMistakeAr = "نسيان استخدام توزيع بيتا في تقديرات البناء عالية التباين أو إهمال معامل الانحراف المعياري σ عند تحديد احتياطي الطوارئ.",
                    status = MetricStatus.NEUTRAL,
                    relatedMetrics = listOf(MetricId.CONTINGENCY, MetricId.BAC)
                )
            }
        }
    }

    private fun buildIndexInterpretation(value: Double?, subject: String, plan: CostManagementPlan): String {
        if (value == null) return "القيمة غير قابلة للحساب لأن المقام يساوي صفرًا."
        val formatted = EvmCalculator.formatIndex(value)
        return when {
            value > plan.acceptableMax -> "ممتاز ($formatted): الأداء يتجاوز الحد الأعلى المقبول (${EvmCalculator.formatIndex(plan.acceptableMax)})؛ كفاءة عالية في $subject."
            value >= plan.acceptableMin -> "مقبول ($formatted): ضمن النطاق المعتمد (${EvmCalculator.formatIndex(plan.acceptableMin)}–${EvmCalculator.formatIndex(plan.acceptableMax)})."
            value >= plan.warningMin -> "تحذير ($formatted): في منطقة التحذير (${EvmCalculator.formatIndex(plan.warningMin)}–${EvmCalculator.formatIndex(plan.acceptableMin)})؛ يتطلب متابعة وإجراءً تصحيحيًا."
            else -> "حرج ($formatted): أقل من حد التحذير (${EvmCalculator.formatIndex(plan.warningMin)})؛ يوجد انحراف خطير في $subject."
        }
    }
}
