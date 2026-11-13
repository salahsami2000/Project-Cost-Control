package com.example

import com.example.domain.*
import org.junit.Assert.*
import org.junit.Test

class EvmCalculatorTest {

    // -----------------------------------------------------------------
    // Section 7 Mandatory EVM Test Cases: BAC = 200, PV = 100, EV = 80, AC = 120
    // -----------------------------------------------------------------
    @Test
    fun `mandatory EVM metrics with BAC=200 PV=100 EV=80 AC=120`() {
        val bac = 200.0
        val pv = 100.0
        val ev = 80.0
        val ac = 120.0

        val cv = EvmCalculator.cv(ev, ac)
        val sv = EvmCalculator.sv(ev, pv)
        val cpi = EvmCalculator.cpi(ev, ac)!!
        val spi = EvmCalculator.spi(ev, pv)!!

        val eacAtypical = EvmCalculator.eacAtypical(ac, bac, ev)
        val eacTypical = EvmCalculator.eacTypical(bac, ev, ac)!!
        val eacCombined = EvmCalculator.eacCostAndSchedule(ac, bac, ev, pv)!!

        val etcVal = EvmCalculator.etc(eacTypical, ac)
        val vacVal = EvmCalculator.vac(bac, eacTypical)
        val vacPct = EvmCalculator.vacPercent(bac, eacTypical)!!

        val tcpiBac = EvmCalculator.tcpiBac(bac, ev, ac)!!
        val tcpiEac = EvmCalculator.tcpiEac(bac, ev, eacTypical, ac)!!

        assertEquals(-40.0, cv, 1e-6)
        assertEquals(-20.0, sv, 1e-6)
        assertEquals(0.6667, cpi, 1e-4)
        assertEquals("0.67", EvmCalculator.formatIndex(cpi))
        assertEquals(0.80, spi, 1e-6)
        assertEquals("0.80", EvmCalculator.formatIndex(spi))

        assertEquals(240.0, eacAtypical, 1e-6)
        // Must use exact CPI (80/120 = 2/3) so EAC = 300.0, NOT 298.5!
        assertEquals(300.0, eacTypical, 1e-6)
        assertEquals(345.0, eacCombined, 1e-6)

        assertEquals(180.0, etcVal, 1e-6)
        assertEquals(-100.0, vacVal, 1e-6)
        assertEquals(-50.0, vacPct, 1e-6)

        assertEquals(1.50, tcpiBac, 1e-6)
        assertEquals("1.50", EvmCalculator.formatIndex(tcpiBac))
        assertEquals(0.6667, tcpiEac, 1e-4)
        assertEquals("0.67", EvmCalculator.formatIndex(tcpiEac))
    }

    // -----------------------------------------------------------------
    // Section 7 Three-Point Estimation: cO = 80, cM = 100, cP = 150
    // -----------------------------------------------------------------
    @Test
    fun `three point estimation triangular beta and sigma`() {
        val cO = 80.0
        val cM = 100.0
        val cP = 150.0

        val triangular = EvmCalculator.calculateThreePointTriangular(cO, cM, cP)
        val beta = EvmCalculator.calculateThreePointBeta(cO, cM, cP)
        val sigma = EvmCalculator.calculateThreePointSigma(cO, cP)

        assertEquals(110.0, triangular, 1e-6)
        assertEquals(105.0, beta, 1e-6)
        assertEquals(11.6667, sigma, 1e-4)
        assertEquals("11.67", EvmCalculator.formatIndex(sigma))
    }

    // -----------------------------------------------------------------
    // Section 7 Edge Cases
    // -----------------------------------------------------------------
    @Test
    fun `edge cases handle zero denominators and exhausted budget safely`() {
        // AC = 0 -> CPI is null, EAC typical is null
        assertNull(EvmCalculator.cpi(50.0, 0.0))
        assertEquals("غير قابل للحساب", EvmCalculator.formatIndex(EvmCalculator.cpi(50.0, 0.0)))
        assertNull(EvmCalculator.eacTypical(200.0, 50.0, 0.0))

        // PV = 0 -> SPI is null, EAC combined is null
        assertNull(EvmCalculator.spi(50.0, 0.0))
        assertNull(EvmCalculator.eacCostAndSchedule(40.0, 200.0, 50.0, 0.0))

        // BAC - AC <= 0 -> TCPI(BAC) is null
        assertNull(EvmCalculator.tcpiBac(200.0, 150.0, 200.0))
        assertNull(EvmCalculator.tcpiBac(200.0, 150.0, 220.0))

        // CPI * SPI = 0 (EV = 0) -> EAC combined is null
        assertNull(EvmCalculator.eacCostAndSchedule(100.0, 200.0, 0.0, 100.0))
    }

    // -----------------------------------------------------------------
    // Section 6 Sample Project Full Integration Verification
    // -----------------------------------------------------------------
    @Test
    fun `sample project produces exact BAC=200 PV=100 EV=80 AC=120 CPI=0_67 SPI=0_80 EAC=300`() {
        val sample = SampleProjectFactory.createSampleProject()
        val snapshot = EvmCalculator.computeSnapshot(sample)
        val budget = EvmCalculator.calculateBudgetBreakdown(sample)

        assertEquals(190.0, budget.activityEstimatesSum, 1e-6)
        assertEquals(10.0, budget.activityContingencySum, 1e-6)
        assertEquals(200.0, snapshot.bac, 1e-6)
        assertEquals(30.0, snapshot.managementReserve, 1e-6)
        assertEquals(230.0, snapshot.projectBudget, 1e-6)

        assertEquals(100.0, snapshot.pv, 1e-6)
        assertEquals(80.0, snapshot.ev, 1e-6)
        assertEquals(120.0, snapshot.ac, 1e-6)
        assertEquals(-40.0, snapshot.cv, 1e-6)
        assertEquals(-20.0, snapshot.sv, 1e-6)
        assertEquals("0.67", EvmCalculator.formatIndex(snapshot.cpi))
        assertEquals("0.80", EvmCalculator.formatIndex(snapshot.spi))
        assertEquals(300.0, snapshot.activeEac!!, 1e-6)
        assertEquals(180.0, snapshot.etc!!, 1e-6)
        assertEquals(-100.0, snapshot.vac!!, 1e-6)
        assertEquals("1.50", EvmCalculator.formatIndex(snapshot.tcpiBac))
        assertTrue(snapshot.realismStatus is RealismJudgment.Unrealistic)
        assertTrue(snapshot.fundingShortfallMonths.isEmpty())
    }

    // -----------------------------------------------------------------
    // Section 5.7 Change Request Reserve Transfer vs Budget Increase
    // -----------------------------------------------------------------
    @Test
    fun `approving management reserve change request transfers to BAC keeping project budget constant`() {
        val base = SampleProjectFactory.createSampleProject()
        val withApprovedMr = base.copy(
            changeRequests = listOf(
                ChangeRequest(
                    id = "cr1",
                    code = "CR-01",
                    date = "2026-05-15",
                    description = "تغطية فارق الحديد",
                    reason = "طوارئ مجهولة",
                    costImpact = 15.0,
                    scheduleImpactDays = 0,
                    source = ChangeSource.MANAGEMENT_RESERVE,
                    status = ChangeStatus.APPROVED
                )
            )
        )
        val budget = EvmCalculator.calculateBudgetBreakdown(withApprovedMr)
        assertEquals(215.0, budget.bac, 1e-6)
        assertEquals(15.0, budget.currentManagementReserve, 1e-6)
        assertEquals(230.0, budget.projectBudget, 1e-6) // Project budget stays constant!

        val withBudgetIncrease = base.copy(
            changeRequests = listOf(
                ChangeRequest(
                    id = "cr2",
                    code = "CR-02",
                    date = "2026-05-20",
                    description = "إضافة دور جديد",
                    reason = "توسعة النطاق",
                    costImpact = 25.0,
                    scheduleImpactDays = 10,
                    source = ChangeSource.BUDGET_INCREASE,
                    status = ChangeStatus.APPROVED
                )
            )
        )
        val increased = EvmCalculator.calculateBudgetBreakdown(withBudgetIncrease)
        assertEquals(225.0, increased.bac, 1e-6)
        assertEquals(30.0, increased.currentManagementReserve, 1e-6)
        assertEquals(255.0, increased.projectBudget, 1e-6)
    }

    // -----------------------------------------------------------------
    // Section 5.4 Funding Shortfall Detection
    // -----------------------------------------------------------------
    @Test
    fun `funding shortfall detected when cumulative PV exceeds cumulative funding`() {
        val pvSeries = listOf(10.0, 30.0, 60.0)
        val tranches = listOf(FundingTranche("t0", 0, 25.0))
        val points = EvmCalculator.calculateFundingPoints(pvSeries, tranches, 3)

        assertFalse(points[1].isShortfall) // M1: PV=10 <= 25
        assertTrue(points[2].isShortfall)  // M2: PV=30 > 25
        assertTrue(points[3].isShortfall)  // M3: PV=60 > 25
    }
}
