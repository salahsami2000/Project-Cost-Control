package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ExplainMetricSheet
import com.example.ui.components.LtrText
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProjectCostApp(viewModel: ProjectViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val budget by viewModel.budgetBreakdown.collectAsStateWithLifecycle()
    val explainMetric by viewModel.explainMetric.collectAsStateWithLifecycle()
    val prefilledCr by viewModel.prefilledChangeRequestCost.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMsg) {
        val msg = snackbarMsg
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Support system back button from secondary screens back to Dashboard
    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
        currentScreen = AppScreen.DASHBOARD
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 840.dp

        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Persistent Sidebar on Wide Tablets
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .width(270.dp)
                        .fillMaxHeight()
                ) {
                    AppSidebarContent(
                        currentScreen = currentScreen,
                        onSelectScreen = { currentScreen = it },
                        onLoadSample = { viewModel.loadReadySample() },
                        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
                    )
                }
                AppMainScaffold(
                    currentScreen = currentScreen,
                    state = state,
                    snapshot = snapshot,
                    budget = budget,
                    prefilledCr = prefilledCr,
                    snackbarHostState = snackbarHostState,
                    showMenuButton = false,
                    onOpenDrawer = {},
                    onNavigate = { currentScreen = it },
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(modifier = Modifier.width(290.dp)) {
                        AppSidebarContent(
                            currentScreen = currentScreen,
                            onSelectScreen = {
                                currentScreen = it
                                scope.launch { drawerState.close() }
                            },
                            onLoadSample = {
                                viewModel.loadReadySample()
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                }
            ) {
                AppMainScaffold(
                    currentScreen = currentScreen,
                    state = state,
                    snapshot = snapshot,
                    budget = budget,
                    prefilledCr = prefilledCr,
                    snackbarHostState = snackbarHostState,
                    showMenuButton = true,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNavigate = { currentScreen = it },
                    viewModel = viewModel
                )
            }
        }
    }

    explainMetric?.let { metric ->
        ExplainMetricSheet(
            metricId = metric,
            state = state,
            snapshot = snapshot,
            onDismiss = { viewModel.closeExplanation() },
            onSelectMetric = { viewModel.openExplanation(it) },
            onNavigateToGlossary = { currentScreen = AppScreen.GLOSSARY }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppMainScaffold(
    currentScreen: AppScreen,
    state: com.example.domain.ProjectState,
    snapshot: com.example.domain.EvmSnapshot,
    budget: com.example.domain.BudgetBreakdown,
    prefilledCr: Double?,
    snackbarHostState: SnackbarHostState,
    showMenuButton: Boolean,
    onOpenDrawer: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
    viewModel: ProjectViewModel,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PanelBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = TextPrimary
                ),
                navigationIcon = {
                    if (showMenuButton) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("open_drawer_btn")
                        ) {
                            Icon(Icons.Outlined.Menu, contentDescription = "فتح القائمة")
                        }
                    }
                },
                title = {
                    Column {
                        Text(
                            text = state.setup.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "المتابعة: الشهر ${snapshot.statusMonth} من ${state.setup.durationMonths}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { viewModel.loadReadySample() },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = BrandPrimaryContainer,
                            contentColor = BrandPrimary
                        ),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("topbar_load_sample_btn")
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("مثال جاهز", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = { onNavigate(AppScreen.CALCULATOR) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("topbar_calculator_btn")
                    ) {
                        Icon(Icons.Outlined.Functions, contentDescription = "حاسبة المعادلات", tint = BrandPrimary)
                    }
                    IconButton(
                        onClick = { onNavigate(AppScreen.PROJECT_SETUP) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Outlined.Tune, contentDescription = "إعداد المشروع", tint = BrandPrimary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.DASHBOARD -> DashboardScreen(
                    state = state,
                    snapshot = snapshot,
                    budget = budget,
                    onLoadSample = { viewModel.loadReadySample() },
                    onOpenSetup = { onNavigate(AppScreen.PROJECT_SETUP) },
                    onOpenChangeRequests = { onNavigate(AppScreen.CHANGE_REQUESTS) },
                    onExplain = { viewModel.openExplanation(it) }
                )
                AppScreen.PROJECT_SETUP -> ProjectSetupScreen(
                    setup = state.setup,
                    onSave = {
                        viewModel.saveProjectSetup(it)
                        onNavigate(AppScreen.DASHBOARD)
                    },
                    onLoadSample = {
                        viewModel.loadReadySample()
                        onNavigate(AppScreen.DASHBOARD)
                    }
                )
                AppScreen.PLAN_7_1 -> CostManagementPlanScreen(
                    plan = state.plan,
                    onUpdatePlan = { viewModel.updatePlan(it) },
                    onAddUnit = { viewModel.addUnitOfMeasure(it) },
                    onRemoveUnit = { viewModel.removeUnitOfMeasure(it) }
                )
                AppScreen.ESTIMATE_7_2 -> CostEstimateScreen(
                    state = state,
                    budget = budget,
                    onUpsertPackage = { viewModel.upsertWorkPackage(it) },
                    onDeletePackage = { viewModel.deleteWorkPackage(it) },
                    onExplain = { viewModel.openExplanation(it) }
                )
                AppScreen.BUDGET_7_3 -> DetermineBudgetScreen(
                    state = state,
                    snapshot = snapshot,
                    budget = budget,
                    onUpdateManagementReserve = { viewModel.updateManagementReserve(it) },
                    onUpdatePackagePhasing = { id, s, e, p -> viewModel.updatePackagePhasing(id, s, e, p) },
                    onAddOrUpdateTranche = { viewModel.addOrUpdateFundingTranche(it) },
                    onDeleteTranche = { viewModel.deleteFundingTranche(it) },
                    onExplain = { viewModel.openExplanation(it) }
                )
                AppScreen.CONTROL_7_4 -> ControlCostsScreen(
                    state = state,
                    snapshot = snapshot,
                    onSelectStatusMonth = { viewModel.setStatusDateMonth(it) },
                    onUpdatePackagePerformance = { id, m, pct, ac -> viewModel.updatePackagePerformance(id, m, pct, ac) },
                    onUpdateQuickEntry = { m, pv, ev, ac -> viewModel.updateQuickEntry(m, pv, ev, ac) },
                    onExplain = { viewModel.openExplanation(it) }
                )
                AppScreen.FORECASTING -> ForecastingScreen(
                    state = state,
                    snapshot = snapshot,
                    onSelectEacMethod = { viewModel.selectEacMethod(it) },
                    onUpdateManualEtc = { viewModel.updateManualEtc(it) },
                    onCreateChangeRequestFromEac = { eac ->
                        viewModel.prepareChangeRequestFromForecast(eac)
                        onNavigate(AppScreen.CHANGE_REQUESTS)
                    },
                    onExplain = { viewModel.openExplanation(it) }
                )
                AppScreen.CHANGE_REQUESTS -> ChangeRequestsScreen(
                    state = state,
                    budget = budget,
                    prefilledCostImpact = prefilledCr,
                    onClearPrefilled = { viewModel.clearPrefilledChangeRequest() },
                    onAddChangeRequest = { desc, reason, cost, days, src, date ->
                        viewModel.addChangeRequest(desc, reason, cost, days, src, date)
                    },
                    onUpdateStatus = { id, status -> viewModel.updateChangeRequestStatus(id, status) },
                    onExplain = { viewModel.openExplanation(it) }
                )
                AppScreen.GLOSSARY -> GlossaryScreen(
                    onExplain = { viewModel.openExplanation(it) }
                )
                AppScreen.CALCULATOR -> CalculatorScreen(
                    state = state,
                    snapshot = snapshot
                )
                AppScreen.REPORTS -> ReportsScreen(
                    state = state,
                    snapshot = snapshot,
                    repository = viewModel.repository,
                    onImportJson = { viewModel.importProjectJson(it) },
                    onShowMessage = { viewModel.showMessage(it) }
                )
            }
        }
    }
}

private fun iconForScreen(screen: AppScreen): ImageVector = when (screen) {
    AppScreen.DASHBOARD -> Icons.Outlined.Dashboard
    AppScreen.PLAN_7_1 -> Icons.Outlined.Rule
    AppScreen.ESTIMATE_7_2 -> Icons.Outlined.Calculate
    AppScreen.BUDGET_7_3 -> Icons.Outlined.AccountBalance
    AppScreen.CONTROL_7_4 -> Icons.Outlined.TrackChanges
    AppScreen.FORECASTING -> Icons.Outlined.TrendingUp
    AppScreen.CHANGE_REQUESTS -> Icons.Outlined.EditNote
    AppScreen.GLOSSARY -> Icons.Outlined.MenuBook
    AppScreen.CALCULATOR -> Icons.Outlined.Functions
    AppScreen.REPORTS -> Icons.Outlined.Assessment
    AppScreen.PROJECT_SETUP -> Icons.Outlined.SettingsApplications
}

@Composable
private fun AppSidebarContent(
    currentScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit,
    onLoadSample: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        AppScreen.DASHBOARD,
        AppScreen.PLAN_7_1,
        AppScreen.ESTIMATE_7_2,
        AppScreen.BUDGET_7_3,
        AppScreen.CONTROL_7_4,
        AppScreen.FORECASTING,
        AppScreen.CHANGE_REQUESTS,
        AppScreen.GLOSSARY,
        AppScreen.CALCULATOR,
        AppScreen.REPORTS,
        AppScreen.PROJECT_SETUP
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // App Brand Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BrandPrimary)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "إدارة تكلفة المشروع",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                LtrText(
                    text = "Project Cost Management",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = ManagementReserveColor,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "PMBOK® 6 — الفصل السابع",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF3E2200),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        navItems.forEach { screen ->
            val selected = currentScreen == screen
            NavigationDrawerItem(
                selected = selected,
                onClick = { onSelectScreen(screen) },
                icon = {
                    Icon(
                        imageVector = iconForScreen(screen),
                        contentDescription = screen.titleAr
                    )
                },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (screen.numberPrefix.isNotEmpty()) {
                            LtrText(
                                text = screen.numberPrefix,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) BrandPrimary else TextMuted
                            )
                        }
                        Text(screen.titleAr, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                    }
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = BrandPrimaryContainer,
                    selectedIconColor = BrandPrimary,
                    selectedTextColor = BrandPrimary
                ),
                modifier = Modifier.testTag("nav_${screen.route}")
            )
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onLoadSample,
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
        ) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("تحميل مثال جاهز (عمارة سكنية)")
        }
    }
}
