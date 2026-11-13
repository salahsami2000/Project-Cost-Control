package com.example.domain

/**
 * Builds the Residential Building ("عمارة سكنية") sample project from Section 6 of the specification:
 * - Unit: Thousand USD (ألف دولار), Duration: 10 months
 * - 5 Work Packages totaling 190 + 10 Contingency = 200 BAC
 * - Management Reserve = 30 -> Project Budget = 230
 * - Status Date: Month 5
 * - At Month 5: PV = 100, EV = 80, AC = 120
 */
object SampleProjectFactory {

    fun createSampleProject(): ProjectState {
        val wp1 = WorkPackage(
            id = "wp_1",
            code = "1.1",
            name = "الحفر والأساسات",
            controlAccount = "CA-01 الأعمال الترابية والخرسانية",
            estimationMethod = EstimationMethod.PARAMETRIC,
            paramQuantity = 300.0,
            paramUnitPrice = 0.1,
            paramUnit = "م³",
            contingencyMode = ReserveMode.AMOUNT,
            contingencyValue = 2.0,
            associatedRisk = "احتمال ظهور مياه جوفية أو تربة ضعيفة",
            startMonth = 1,
            endMonth = 3,
            distributionPattern = DistributionPattern.UNIFORM
        )

        val wp2 = WorkPackage(
            id = "wp_2",
            code = "1.2",
            name = "الهيكل الخرساني",
            controlAccount = "CA-01 الأعمال الترابية والخرسانية",
            estimationMethod = EstimationMethod.THREE_POINT,
            cO = 50.0,
            cM = 56.0,
            cP = 74.0,
            threePointDist = ThreePointDistribution.BETA, // (50 + 4*56 + 74)/6 = 348/6 = 58
            contingencyMode = ReserveMode.AMOUNT,
            contingencyValue = 3.0,
            associatedRisk = "تقلبات أسعار حديد التسليح والخرسانة الجاهزة",
            startMonth = 2,
            endMonth = 6,
            distributionPattern = DistributionPattern.S_CURVE
        )

        val wp3 = WorkPackage(
            id = "wp_3",
            code = "1.3",
            name = "أعمال المباني",
            controlAccount = "CA-02 الأعمال المعمارية",
            estimationMethod = EstimationMethod.PARAMETRIC,
            paramQuantity = 1000.0,
            paramUnitPrice = 0.025,
            paramUnit = "م²",
            contingencyMode = ReserveMode.AMOUNT,
            contingencyValue = 1.0,
            associatedRisk = "تأخر توريد الطوب والمونة",
            startMonth = 3,
            endMonth = 7,
            distributionPattern = DistributionPattern.UNIFORM
        )

        val wp4 = WorkPackage(
            id = "wp_4",
            code = "1.4",
            name = "الكهرباء والسباكة",
            controlAccount = "CA-03 الأعمال الكهروميكانيكية",
            estimationMethod = EstimationMethod.BOTTOM_UP,
            bottomUpItems = listOf(
                BottomUpItem("bu_1", "عمالة", "فنيو كهرباء وسباكة", 14.0),
                BottomUpItem("bu_2", "مواد", "كابلات، لوحات، مواسير صحية", 16.0),
                BottomUpItem("bu_3", "معدات", "اختبارات الضغط والعزل", 5.0)
            ),
            contingencyMode = ReserveMode.AMOUNT,
            contingencyValue = 2.0,
            associatedRisk = "تعديلات المخططات التنفيذية مع الاستشاري",
            startMonth = 4,
            endMonth = 9,
            distributionPattern = DistributionPattern.S_CURVE
        )

        val wp5 = WorkPackage(
            id = "wp_5",
            code = "1.5",
            name = "التشطيبات",
            controlAccount = "CA-02 الأعمال المعمارية",
            estimationMethod = EstimationMethod.ANALOGOUS,
            analogousPriorCost = 40.0,
            analogousAdjustmentPct = 5.0, // 40 * 1.05 = 42
            contingencyMode = ReserveMode.AMOUNT,
            contingencyValue = 2.0,
            associatedRisk = "ارتفاع تكلفة الرخام والأدوات الصحية المستوردة",
            startMonth = 7,
            endMonth = 10,
            distributionPattern = DistributionPattern.BACK_LOADED
        )

        // Exact monthly cumulative PV curve from Section 6:
        val pvCurve = listOf(8.0, 22.0, 42.0, 68.0, 100.0, 132.0, 158.0, 178.0, 192.0, 200.0)

        // Package PV breakdown at Month 5 summing exactly to 100: 32 + 45 + 16 + 7 + 0 = 100
        val packagePvAtMonth5 = mapOf(
            "wp_1" to mapOf(5 to 32.0),
            "wp_2" to mapOf(5 to 45.0),
            "wp_3" to mapOf(5 to 16.0),
            "wp_4" to mapOf(5 to 7.0),
            "wp_5" to mapOf(5 to 0.0)
        )

        // Per-package monthly performance so cumulative EV(1..5) = [6, 17, 33, 55, 80]
        // and cumulative AC(1..5) = [10, 28, 55, 88, 120]
        // Budgets: wp1=32, wp2=61, wp3=26, wp4=37, wp5=44 (total 200)
        val performances = listOf(
            // Month 1: EV = 6 (wp1=6), AC = 10 (wp1=10)
            PackageMonthlyPerformance("wp_1", 1, 18.75, 10.0),

            // Month 2: Cum EV = 17 (wp1=14, wp2=3), Period AC = 18 (wp1=10, wp2=8) -> Cum AC = 28
            PackageMonthlyPerformance("wp_1", 2, 43.75, 10.0),
            PackageMonthlyPerformance("wp_2", 2, (3.0 / 61.0) * 100.0, 8.0),

            // Month 3: Cum EV = 33 (wp1=24, wp2=9), Period AC = 27 (wp1=12, wp2=15) -> Cum AC = 55
            PackageMonthlyPerformance("wp_1", 3, 75.0, 12.0),
            PackageMonthlyPerformance("wp_2", 3, (9.0 / 61.0) * 100.0, 15.0),

            // Month 4: Cum EV = 55 (wp1=32, wp2=18, wp3=5), Period AC = 33 (wp1=10, wp2=15, wp3=8) -> Cum AC = 88
            PackageMonthlyPerformance("wp_1", 4, 100.0, 10.0),
            PackageMonthlyPerformance("wp_2", 4, (18.0 / 61.0) * 100.0, 15.0),
            PackageMonthlyPerformance("wp_3", 4, (5.0 / 26.0) * 100.0, 8.0),

            // Month 5: Cum EV = 80 (wp1=32, wp2=30.5, wp3=13, wp4=4.5), Period AC = 32 (wp2=14, wp3=10, wp4=8) -> Cum AC = 120
            PackageMonthlyPerformance("wp_1", 5, 100.0, 0.0),
            PackageMonthlyPerformance("wp_2", 5, 50.0, 14.0),
            PackageMonthlyPerformance("wp_3", 5, 50.0, 10.0),
            PackageMonthlyPerformance("wp_4", 5, (4.5 / 37.0) * 100.0, 8.0),
            PackageMonthlyPerformance("wp_5", 5, 0.0, 0.0)
        )

        val quickEntries = listOf(
            MonthlyQuickEntry(1, 8.0, 6.0, 10.0),
            MonthlyQuickEntry(2, 22.0, 17.0, 28.0),
            MonthlyQuickEntry(3, 42.0, 33.0, 55.0),
            MonthlyQuickEntry(4, 68.0, 55.0, 88.0),
            MonthlyQuickEntry(5, 100.0, 80.0, 120.0),
            MonthlyQuickEntry(6, 132.0, 80.0, 120.0),
            MonthlyQuickEntry(7, 158.0, 80.0, 120.0),
            MonthlyQuickEntry(8, 178.0, 80.0, 120.0),
            MonthlyQuickEntry(9, 192.0, 80.0, 120.0),
            MonthlyQuickEntry(10, 200.0, 80.0, 120.0)
        )

        // Cumulative funding: 40 (M0), 90 (M2), 150 (M4), 190 (M6), 230 (M8)
        val tranches = listOf(
            FundingTranche("f_0", 0, 40.0),
            FundingTranche("f_2", 2, 50.0),
            FundingTranche("f_4", 4, 60.0),
            FundingTranche("f_6", 6, 40.0),
            FundingTranche("f_8", 8, 40.0)
        )

        return ProjectState(
            setup = ProjectSetup(
                name = "عمارة سكنية — حي النخيل",
                type = ProjectType.RESIDENTIAL,
                currency = "دولار",
                displayUnit = DisplayUnit.THOUSAND,
                startDate = "2026-01-01",
                durationMonths = 10,
                inputMode = InputMode.DETAILED,
                statusDateMonth = 5,
                isInitialized = true
            ),
            plan = CostManagementPlan(
                roundingPrecision = 1,
                acceptableMin = 0.95,
                acceptableMax = 1.05,
                warningMin = 0.90,
                defaultEacMethod = EacMethod.TYPICAL,
                reportingFrequency = ReportingFrequency.MONTHLY
            ),
            workPackages = listOf(wp1, wp2, wp3, wp4, wp5),
            managementReserveConfig = ManagementReserveConfig(
                mode = ReserveMode.AMOUNT,
                value = 30.0
            ),
            fundingTranches = tranches,
            quickEntries = quickEntries,
            packagePerformances = performances,
            manualPvOverride = pvCurve,
            manualPackagePvAtMonth = packagePvAtMonth5,
            selectedEacMethod = EacMethod.TYPICAL,
            manualEtcInput = 180.0,
            changeRequests = emptyList(),
            baselineHistory = listOf(
                BaselineRevision(
                    version = 1,
                    date = "2026-01-01",
                    bac = 200.0,
                    managementReserve = 30.0,
                    note = "الخط المرجعي الأولي المعتمد عند بدء المشروع"
                )
            )
        )
    }
}
