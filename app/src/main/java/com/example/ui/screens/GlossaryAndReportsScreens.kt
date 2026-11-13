package com.example.ui.screens

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProjectRepository
import com.example.domain.*
import com.example.ui.components.*
import com.example.ui.theme.*

// =====================================================================
// 5.10 Glossary & Equations (القاموس والمعادلات)
// =====================================================================
@Composable
fun GlossaryScreen(
    onExplain: (MetricId) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) ExplainContent.glossaryEntries
        else ExplainContent.glossaryEntries.filter {
            it.symbol.lowercase().contains(q) ||
                it.nameAr.contains(q) ||
                it.nameEn.lowercase().contains(q) ||
                it.formulaLtr.lowercase().contains(q)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PmbokSectionBanner(
            processNumber = null,
            titleAr = "القاموس والمعادلات",
            subtitleAr = "مرجع شامل لجميع رموز ومعادلات القيمة المكتسبة وفق دليل PMBOK® 6",
            icon = Icons.Outlined.MenuBook
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("ابحث بالرمز أو الاسم العربي أو الإنجليزي (مثل CPI أو Variance)") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        filtered.forEach { entry ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, CardBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BrandPrimaryContainer)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                LtrText(
                                    text = entry.symbol,
                                    color = BrandPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Column {
                                Text(entry.nameAr, style = MaterialTheme.typography.titleMedium)
                                // English name in Bold Black per specification 5.10
                                LtrText(
                                    text = entry.nameEn,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            }
                        }

                        ExplainInlineButton(
                            metricId = entry.id,
                            label = "بأرقام مشروعي",
                            onExplain = onExplain
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PanelBackground)
                            .padding(10.dp)
                    ) {
                        LtrText(
                            text = entry.formulaLtr,
                            color = BrandPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text("• يجيب عن: ${entry.questionAr}", style = MaterialTheme.typography.bodyMedium)
                    Text("• التفسير: ${entry.interpretationRuleAr}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("• خطأ شائع: ${entry.commonMistakeAr}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF92400E))
                }
            }
        }

        // Contingency Reserve vs Management Reserve Comparison Table
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, ManagementReserveColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "احتياطي الطوارئ مقابل الاحتياطي الإداري",
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandPrimary
                )

                ComparisonRow(
                    aspect = "المعيار",
                    contingency = "احتياطي الطوارئ (Contingency)",
                    management = "الاحتياطي الإداري (Management)",
                    isHeader = true
                )
                ComparisonRow(
                    aspect = "نوع المخاطر",
                    contingency = "مخاطر معروفة محددة (Known-Unknowns)",
                    management = "مخاطر مجهولة غير متوقعة (Unknown-Unknowns)"
                )
                ComparisonRow(
                    aspect = "الموقع في الموازنة",
                    contingency = "داخل الخط المرجعي للتكلفة (جزء من BAC)",
                    management = "فوق الخط المرجعي (خارج BAC، داخل موازنة المشروع)"
                )
                ComparisonRow(
                    aspect = "الصلاحية",
                    contingency = "مدير المشروع",
                    management = "الإدارة العليا (يتطلب طلب تغيير لنقله إلى BAC)"
                )
                ComparisonRow(
                    aspect = "الدخول في EVM",
                    contingency = "يدخل في حسابات PV و EV و CPI و EAC",
                    management = "لا يدخل إطلاقًا في حسابات القيمة المكتسبة"
                )
            }
        }
    }
}

@Composable
private fun ComparisonRow(
    aspect: String,
    contingency: String,
    management: String,
    isHeader: Boolean = false
) {
    Surface(
        color = if (isHeader) BrandPrimaryContainer else PanelBackground,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = aspect,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.8f)
            )
            Text(
                text = contingency,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                color = if (isHeader) ContingencyReserveColor else TextPrimary,
                modifier = Modifier.weight(1.1f)
            )
            Text(
                text = management,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                color = if (isHeader) Color(0xFF9A6700) else TextPrimary,
                modifier = Modifier.weight(1.1f)
            )
        }
    }
}

// =====================================================================
// 5.11 Reports & Export (التقارير والتصدير)
// =====================================================================
@Composable
fun ReportsScreen(
    state: ProjectState,
    snapshot: EvmSnapshot,
    repository: ProjectRepository,
    onImportJson: (String) -> Boolean,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val p = state.plan.roundingPrecision
    val unitLabel = "${state.setup.displayUnit.multiplierHint} ${state.setup.currency}".trim()

    // Retain WebView reference during print so GC does not drop it before PrintManager finishes
    var activePrintWebView by remember { mutableStateOf<WebView?>(null) }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val csv = repository.exportExcelCsv(state, snapshot)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                onShowMessage("تم تصدير ملف Excel (CSV) بنجاح")
            }.onFailure {
                onShowMessage("تعذر حفظ ملف Excel")
            }
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val json = repository.exportToJson(state)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                onShowMessage("تم حفظ النسخة الاحتياطية JSON بنجاح")
            }.onFailure {
                onShowMessage("تعذر حفظ ملف JSON")
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: ""
            }.onSuccess { raw ->
                onImportJson(raw)
            }.onFailure {
                onShowMessage("تعذر قراءة الملف المختار")
            }
        }
    }

    val exportPptxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.presentationml.presentation")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val budget = EvmCalculator.calculateBudgetBreakdown(state)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    com.example.data.PptxExporter.exportToPptx(context, state, snapshot, budget, out)
                }
            }.onSuccess {
                onShowMessage("تم تصدير عرض PowerPoint (.pptx) بصور الشاشات والشرح بنجاح")
            }.onFailure {
                onShowMessage("تعذر إنشاء ملف PowerPoint")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PmbokSectionBanner(
            processNumber = null,
            titleAr = "التقارير والتصدير",
            subtitleAr = "إصدار تقرير أداء شهري A4 (PDF) وتصدير جداول Excel والنسخ الاحتياطي JSON",
            icon = Icons.Outlined.Assessment
        )

        // Monthly Report Preview Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, CardBorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("معاينة تقرير الأداء الشهري (A4)", style = MaterialTheme.typography.titleMedium, color = BrandPrimary)
                Text("المشروع: ${state.setup.name} — الشهر ${snapshot.statusMonth}", fontWeight = FontWeight.Bold)
                Text(snapshot.summarySentenceAr, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    LtrText(text = "BAC: ${EvmCalculator.formatMoney(snapshot.bac, p)} $unitLabel", fontSize = 13.sp)
                    LtrText(text = "CPI: ${EvmCalculator.formatIndex(snapshot.cpi)}", fontSize = 13.sp, color = statusColor(snapshot.cpiStatus))
                    LtrText(text = "SPI: ${EvmCalculator.formatIndex(snapshot.spi)}", fontSize = 13.sp, color = statusColor(snapshot.spiStatus))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    LtrText(text = "EAC: ${EvmCalculator.formatMoney(snapshot.activeEac, p)} $unitLabel", fontSize = 13.sp, color = EtcColor)
                    LtrText(text = "VAC: ${EvmCalculator.formatMoney(snapshot.vac, p)} $unitLabel", fontSize = 13.sp, color = if ((snapshot.vac ?: 0.0) < 0) StatusBad else StatusGood)
                }

                Button(
                    onClick = {
                        val html = repository.buildPrintableHtmlReport(state, snapshot)
                        val webView = WebView(context)
                        webView.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                                val adapter = webView.createPrintDocumentAdapter("ProjectCostReport_M${snapshot.statusMonth}")
                                val attrs = PrintAttributes.Builder()
                                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                    .build()
                                printManager.print("تقرير تكلفة المشروع", adapter, attrs)
                            }
                        }
                        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                        activePrintWebView = webView
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("طباعة / حفظ تقرير PDF (A4 عربي)")
                }
            }
        }

        // Export Excel & JSON Backup
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, CardBorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("تصدير واستيراد البيانات والعروض", style = MaterialTheme.typography.titleMedium)

                Button(
                    onClick = {
                        exportPptxLauncher.launch("project_cost_screens_explained.pptx")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AcColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Slideshow, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("تصدير عرض PowerPoint (.pptx) بصور الشاشات والشرح", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        exportCsvLauncher.launch("project_cost_report_m${snapshot.statusMonth}.csv")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.TableChart, contentDescription = null, tint = StatusGood)
                    Spacer(Modifier.width(8.dp))
                    Text("تصدير جداول حزم العمل والأداء إلى Excel (CSV)")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            exportJsonLauncher.launch("project_cost_backup.json")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null, tint = EtcColor)
                        Spacer(Modifier.width(6.dp))
                        Text("تصدير JSON")
                    }

                    OutlinedButton(
                        onClick = {
                            importJsonLauncher.launch(arrayOf("application/json", "text/*"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = BrandPrimary)
                        Spacer(Modifier.width(6.dp))
                        Text("استيراد JSON")
                    }
                }
            }
        }
    }
    // Keep activePrintWebView referenced cleanly
    DisposableEffect(activePrintWebView) {
        onDispose { activePrintWebView = null }
    }
}
