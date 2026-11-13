package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.*
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun ChartLegendRow(items: List<Pair<String, Color>>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (label, color) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Main EVM S-Curve Chart:
 * - PV full curve (#7A1416)
 * - EV up to statusMonth (#1E9C9C)
 * - AC up to statusMonth (#F28C28)
 * - Dashed Forecast line from AC(statusMonth) to EAC (#1AA0DC)
 * - Vertical line at statusMonth
 * - Optional CV & SV vertical variance arrows
 */
@Composable
fun EvmCurveChart(
    pvSeries: List<Double>,
    evSeries: List<Double>,
    acSeries: List<Double>,
    forecastSeries: List<Double>,
    bac: Double,
    statusMonth: Int,
    showVarianceArrows: Boolean = false,
    modifier: Modifier = Modifier
) {
    val maxVal = listOfNotNull(
        pvSeries.maxOrNull(),
        evSeries.maxOrNull(),
        acSeries.maxOrNull(),
        forecastSeries.maxOrNull(),
        bac
    ).maxOrNull()?.coerceAtLeast(10.0) ?: 100.0
    val yMax = maxVal * 1.12
    val totalMonths = pvSeries.size.coerceAtLeast(1)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChartLegendRow(
            items = buildList {
                add("PV المخططة" to PvColor)
                add("EV المكتسبة" to EvColor)
                add("AC الفعلية" to AcColor)
                if (forecastSeries.size > 1) add("التوقع (EAC)" to EtcColor)
            }
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(235.dp)
                .padding(top = 6.dp)
        ) {
            val leftPad = 48.dp.toPx()
            val rightPad = 16.dp.toPx()
            val topPad = 14.dp.toPx()
            val bottomPad = 28.dp.toPx()
            val chartW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
            val chartH = (size.height - topPad - bottomPad).coerceAtLeast(1f)

            val labelPaint = Paint().apply {
                color = Color(0xFF6B7280).toArgb()
                textSize = 10.sp.toPx()
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
                typeface = Typeface.DEFAULT
            }
            val xLabelPaint = Paint().apply {
                color = Color(0xFF6B7280).toArgb()
                textSize = 10.sp.toPx()
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            fun xForMonth(m: Int): Float {
                return leftPad + (m.toFloat() / totalMonths.toFloat()) * chartW
            }

            fun yForVal(v: Double): Float {
                return topPad + chartH - ((v / yMax).toFloat().coerceIn(0f, 1.05f) * chartH)
            }

            // Gridlines
            val steps = 4
            for (i in 0..steps) {
                val valAt = yMax * (i.toDouble() / steps)
                val y = yForVal(valAt)
                drawLine(
                    color = Color(0xFFE5E7EB),
                    start = Offset(leftPad, y),
                    end = Offset(leftPad + chartW, y),
                    strokeWidth = 1.dp.toPx()
                )
                drawContext.canvas.nativeCanvas.drawText(
                    valAt.toInt().toString(),
                    leftPad - 6.dp.toPx(),
                    y + 4.dp.toPx(),
                    labelPaint
                )
            }

            // X-axis month labels
            val monthStep = if (totalMonths > 12) 2 else 1
            for (m in 0..totalMonths step monthStep) {
                val x = xForMonth(m)
                drawContext.canvas.nativeCanvas.drawText(
                    "M$m",
                    x,
                    topPad + chartH + 18.dp.toPx(),
                    xLabelPaint
                )
            }

            // BAC horizontal dashed reference
            if (bac > 0) {
                val yBac = yForVal(bac)
                drawLine(
                    color = BrandPrimary.copy(alpha = 0.4f),
                    start = Offset(leftPad, yBac),
                    end = Offset(leftPad + chartW, yBac),
                    strokeWidth = 1.3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            }

            // Vertical Status Date line
            val statusX = xForMonth(statusMonth.coerceIn(1, totalMonths))
            drawLine(
                color = Color(0xFF475569).copy(alpha = 0.55f),
                start = Offset(statusX, topPad),
                end = Offset(statusX, topPad + chartH),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )

            // Helper to draw curve starting from M0 = 0
            fun drawSeries(values: List<Double>, color: Color, dashed: Boolean = false, startMonth: Int = 0) {
                if (values.isEmpty()) return
                val path = Path()
                if (startMonth == 0) {
                    path.moveTo(xForMonth(0), yForVal(0.0))
                    values.forEachIndexed { idx, v ->
                        path.lineTo(xForMonth(idx + 1), yForVal(v))
                    }
                } else {
                    values.forEachIndexed { idx, v ->
                        val x = xForMonth(startMonth + idx)
                        val y = yForVal(v)
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = 2.8.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(12f, 8f)) else null
                    )
                )
                // Dots at points
                values.forEachIndexed { idx, v ->
                    val m = if (startMonth == 0) idx + 1 else startMonth + idx
                    drawCircle(
                        color = color,
                        radius = 3.6.dp.toPx(),
                        center = Offset(xForMonth(m), yForVal(v))
                    )
                }
            }

            drawSeries(pvSeries, PvColor)
            drawSeries(evSeries, EvColor)
            drawSeries(acSeries, AcColor)
            if (forecastSeries.size > 1) {
                drawSeries(forecastSeries, EtcColor, dashed = true, startMonth = statusMonth)
            }

            // Optional CV & SV indicator brackets at statusMonth
            if (showVarianceArrows && evSeries.isNotEmpty() && acSeries.isNotEmpty() && pvSeries.size >= statusMonth) {
                val yEv = yForVal(evSeries.last())
                val yAc = yForVal(acSeries.last())
                val yPv = yForVal(pvSeries[statusMonth - 1])
                // CV vertical bar slightly left of statusX
                val cvX = statusX - 8.dp.toPx()
                drawLine(
                    color = StatusBad,
                    start = Offset(cvX, yEv),
                    end = Offset(cvX, yAc),
                    strokeWidth = 2.5.dp.toPx()
                )
                // SV vertical bar slightly right of statusX
                val svX = statusX + 8.dp.toPx()
                drawLine(
                    color = StatusWarning,
                    start = Offset(svX, yEv),
                    end = Offset(svX, yPv),
                    strokeWidth = 2.5.dp.toPx()
                )
            }
        }
    }
}

/**
 * Semi-circular Gauge for CPI / SPI colored by 7.1 Control Thresholds.
 */
@Composable
fun IndexGaugeMeter(
    symbol: String,
    titleAr: String,
    value: Double?,
    plan: CostManagementPlan,
    status: MetricStatus,
    modifier: Modifier = Modifier
) {
    val activeColor = statusColor(status)
    val normalized = ((value ?: 0.0) / 1.4).toFloat().coerceIn(0f, 1f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(
                modifier = Modifier
                    .size(150.dp, 95.dp)
            ) {
                val strokeW = 14.dp.toPx()
                val arcSize = Size(size.width - strokeW, (size.width - strokeW))
                val topLeft = Offset(strokeW / 2f, strokeW / 2f)

                // Background arc segments mapped to 0..1.4 range:
                val warnEnd = (plan.warningMin / 1.4).toFloat().coerceIn(0f, 1f)
                val accStart = (plan.acceptableMin / 1.4).toFloat().coerceIn(warnEnd, 1f)

                // Bad zone
                drawArc(
                    color = StatusBad.copy(alpha = 0.25f),
                    startAngle = 180f,
                    sweepAngle = 180f * warnEnd,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                )
                // Warning zone
                drawArc(
                    color = StatusWarning.copy(alpha = 0.28f),
                    startAngle = 180f + 180f * warnEnd,
                    sweepAngle = 180f * (accStart - warnEnd),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                )
                // Good zone
                drawArc(
                    color = StatusGood.copy(alpha = 0.28f),
                    startAngle = 180f + 180f * accStart,
                    sweepAngle = 180f * (1f - accStart),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                )

                // Active progress arc
                drawArc(
                    color = activeColor,
                    startAngle = 180f,
                    sweepAngle = 180f * normalized,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )

                // Needle
                val angleRad = (180.0 + 180.0 * normalized) * (PI / 180.0)
                val cx = size.width / 2f
                val cy = size.width / 2f
                val needleLen = (size.width / 2f) - strokeW - 6.dp.toPx()
                val nx = cx + (needleLen * cos(angleRad)).toFloat()
                val ny = cy + (needleLen * sin(angleRad)).toFloat()
                drawLine(
                    color = TextPrimary,
                    start = Offset(cx, cy),
                    end = Offset(nx, ny),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = activeColor,
                    radius = 5.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
        }
        LtrText(
            text = EvmCalculator.formatIndex(value),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = activeColor
        )
        Text(
            text = "$symbol — $titleAr",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Section 5.4A: Budget Components Ladder (Waterfall / Stepped Bar Chart)
 * 1. Activity Estimates -> 2. + Contingency -> 3. Work Packages -> 4. Control Accounts
 * -> 5. BAC (dashed line) -> 6. + Management Reserve (Yellow) -> 7. Total Project Budget
 */
@Composable
fun BudgetLadderChart(
    breakdown: BudgetBreakdown,
    precision: Int,
    modifier: Modifier = Modifier
) {
    val maxBudget = max(breakdown.projectBudget, breakdown.bac).coerceAtLeast(10.0) * 1.15
    val bars = listOf(
        Triple("تقدير الأنشطة", breakdown.activityEstimatesSum, EtcColor),
        Triple("+ طوارئ", breakdown.activityContingencySum, ContingencyReserveColor),
        Triple("حزم العمل", breakdown.workPackagesTotal, EvColor),
        Triple("الخط المرجعي BAC", breakdown.bac, PvColor),
        Triple("الاحتياطي الإداري", breakdown.currentManagementReserve, ManagementReserveColor),
        Triple("موازنة المشروع", breakdown.projectBudget, FundingColor)
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(215.dp)
        ) {
            val leftPad = 14.dp.toPx()
            val rightPad = 14.dp.toPx()
            val topPad = 24.dp.toPx()
            val bottomPad = 36.dp.toPx()
            val chartW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
            val chartH = (size.height - topPad - bottomPad).coerceAtLeast(1f)
            val slotW = chartW / bars.size
            val barW = slotW * 0.56f

            fun yFor(v: Double): Float = topPad + chartH - ((v / maxBudget).toFloat().coerceIn(0f, 1f) * chartH)

            val valuePaint = Paint().apply {
                color = TextPrimary.toArgb()
                textSize = 10.sp.toPx()
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            val labelPaint = Paint().apply {
                color = TextSecondary.toArgb()
                textSize = 9.5.sp.toPx()
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            // BAC dashed reference line
            val bacY = yFor(breakdown.bac)
            drawLine(
                color = PvColor,
                start = Offset(leftPad, bacY),
                end = Offset(leftPad + chartW, bacY),
                strokeWidth = 1.6.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            )

            bars.forEachIndexed { idx, (label, value, color) ->
                val cx = leftPad + slotW * idx + slotW / 2f
                val x = cx - barW / 2f
                val baseY = topPad + chartH

                if (idx == 1) {
                    // Stacked Contingency on top of Estimates
                    val startVal = breakdown.activityEstimatesSum
                    val endVal = startVal + value
                    val yTop = yFor(endVal)
                    val yBot = yFor(startVal)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, yTop),
                        size = Size(barW, (yBot - yTop).coerceAtLeast(3.dp.toPx())),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        "+${EvmCalculator.formatMoney(value, precision)}",
                        cx,
                        yTop - 6.dp.toPx(),
                        valuePaint
                    )
                } else if (idx == 4) {
                    // Stacked Management Reserve (Yellow) above BAC
                    val startVal = breakdown.bac
                    val endVal = startVal + value
                    val yTop = yFor(endVal)
                    val yBot = yFor(startVal)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, yTop),
                        size = Size(barW, (yBot - yTop).coerceAtLeast(3.dp.toPx())),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        "+${EvmCalculator.formatMoney(value, precision)}",
                        cx,
                        yTop - 6.dp.toPx(),
                        valuePaint
                    )
                } else {
                    val yTop = yFor(value)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, yTop),
                        size = Size(barW, (baseY - yTop).coerceAtLeast(2.dp.toPx())),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        EvmCalculator.formatMoney(value, precision),
                        cx,
                        yTop - 6.dp.toPx(),
                        valuePaint
                    )
                }

                // Short label at bottom
                val shortLabel = when (idx) {
                    0 -> "الأنشطة"
                    1 -> "الطوارئ"
                    2 -> "الحزم"
                    3 -> "BAC"
                    4 -> "الإداري"
                    else -> "الموازنة"
                }
                drawContext.canvas.nativeCanvas.drawText(
                    shortLabel,
                    cx,
                    baseY + 16.dp.toPx(),
                    labelPaint
                )
            }
        }
    }
}

/**
 * Section 5.4C: Combined Baseline Curve + StepAfter Funding Staircase + BAC & Project Budget + Shaded MR.
 */
@Composable
fun FundingVsBaselineChart(
    fundingPoints: List<FundingPoint>,
    bac: Double,
    projectBudget: Double,
    modifier: Modifier = Modifier
) {
    val totalMonths = (fundingPoints.size - 1).coerceAtLeast(1)
    val maxVal = listOfNotNull(
        fundingPoints.maxOfOrNull { max(it.cumulativePv, it.cumulativeFunding) },
        projectBudget,
        bac
    ).maxOrNull()?.coerceAtLeast(10.0) ?: 100.0
    val yMax = maxVal * 1.12

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChartLegendRow(
            items = listOf(
                "الخط المرجعي PV" to EtcColor,
                "التمويل التراكمي" to FundingColor,
                "الاحتياطي الإداري" to ManagementReserveColor,
                "عجز تمويل" to StatusBad
            )
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(225.dp)
        ) {
            val leftPad = 46.dp.toPx()
            val rightPad = 14.dp.toPx()
            val topPad = 14.dp.toPx()
            val bottomPad = 28.dp.toPx()
            val chartW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
            val chartH = (size.height - topPad - bottomPad).coerceAtLeast(1f)

            fun xFor(m: Int) = leftPad + (m.toFloat() / totalMonths.toFloat()) * chartW
            fun yFor(v: Double) = topPad + chartH - ((v / yMax).toFloat().coerceIn(0f, 1.05f) * chartH)

            val labelPaint = Paint().apply {
                color = Color(0xFF6B7280).toArgb()
                textSize = 10.sp.toPx()
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
            val xLabelPaint = Paint().apply {
                color = Color(0xFF6B7280).toArgb()
                textSize = 10.sp.toPx()
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            // Grid
            for (i in 0..4) {
                val valAt = yMax * (i / 4.0)
                val y = yFor(valAt)
                drawLine(Color(0xFFE5E7EB), Offset(leftPad, y), Offset(leftPad + chartW, y), 1.dp.toPx())
                drawContext.canvas.nativeCanvas.drawText(valAt.toInt().toString(), leftPad - 6.dp.toPx(), y + 4.dp.toPx(), labelPaint)
            }

            for (m in 0..totalMonths step (if (totalMonths > 12) 2 else 1)) {
                drawContext.canvas.nativeCanvas.drawText("M$m", xFor(m), topPad + chartH + 18.dp.toPx(), xLabelPaint)
            }

            // Shaded Management Reserve region between BAC and Project Budget
            if (projectBudget > bac) {
                val yBudget = yFor(projectBudget)
                val yBac = yFor(bac)
                drawRect(
                    color = ManagementReserveColor.copy(alpha = 0.16f),
                    topLeft = Offset(leftPad, yBudget),
                    size = Size(chartW, (yBac - yBudget).coerceAtLeast(1f))
                )
                drawLine(
                    color = ManagementReserveColor,
                    start = Offset(leftPad, yBudget),
                    end = Offset(leftPad + chartW, yBudget),
                    strokeWidth = 1.6.dp.toPx()
                )
            }

            // BAC line
            val yBac = yFor(bac)
            drawLine(
                color = PvColor,
                start = Offset(leftPad, yBac),
                end = Offset(leftPad + chartW, yBac),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            )

            // StepAfter Funding staircase (Green)
            if (fundingPoints.isNotEmpty()) {
                val stepPath = Path()
                fundingPoints.forEachIndexed { idx, pt ->
                    val x = xFor(pt.month)
                    val y = yFor(pt.cumulativeFunding)
                    if (idx == 0) {
                        stepPath.moveTo(x, y)
                    } else {
                        val prevY = yFor(fundingPoints[idx - 1].cumulativeFunding)
                        stepPath.lineTo(x, prevY) // horizontal step
                        stepPath.lineTo(x, y)     // vertical jump
                    }
                }
                drawPath(
                    path = stepPath,
                    color = FundingColor,
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Baseline PV curve (Blue #1AA0DC)
            val pvPath = Path()
            fundingPoints.forEachIndexed { idx, pt ->
                val x = xFor(pt.month)
                val y = yFor(pt.cumulativePv)
                if (idx == 0) pvPath.moveTo(x, y) else pvPath.lineTo(x, y)
            }
            drawPath(
                path = pvPath,
                color = EtcColor,
                style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Red shortfall alert dots
            fundingPoints.filter { it.isShortfall }.forEach { pt ->
                drawCircle(
                    color = StatusBad,
                    radius = 5.5.dp.toPx(),
                    center = Offset(xFor(pt.month), yFor(pt.cumulativePv))
                )
            }
        }
    }
}
