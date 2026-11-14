package com.example.ui.navigation

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.data.ai.ScamAnalysisAiService
import com.example.data.auth.attemptAutoSignIn
import com.example.data.auth.authStateFlow
import com.example.data.auth.signOutUser
import com.example.data.notifications.SecurityNotificationHelper
import com.example.data.prefs.LocalPreferencesManager
import com.example.data.repository.ScamShieldRepository
import com.example.ui.i18n.LocalStrings
import com.example.ui.i18n.ProvideAppLanguage
import com.example.ui.screens.AdminModerationScreen
import com.example.ui.screens.CommunityAlertsScreen
import com.example.ui.screens.FamilyScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.LearningCentreScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MessageScannerScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PhoneCheckerScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.ScanResultScreen
import com.example.ui.screens.ScreenshotScannerScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SubscriptionScreen
import com.example.ui.screens.WebsiteCheckerScreen
import com.example.ui.theme.ScamShieldTheme
import com.example.ui.viewmodel.ScanExecutionState
import com.example.ui.viewmodel.ScamShieldViewModel
import com.example.ui.viewmodel.UiDataState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data object RouteHome
@Serializable data object RouteHistory
@Serializable data object RouteAlerts
@Serializable data object RouteFamily
@Serializable data object RouteProfile
@Serializable data object RouteMessageScanner
@Serializable data object RouteWebsiteChecker
@Serializable data object RoutePhoneChecker
@Serializable data object RouteScreenshotScanner
@Serializable data object RouteScanResult
@Serializable data class RouteLearningCentre(val lessonId: String? = null)
@Serializable data object RouteSubscriptions
@Serializable data object RouteAdminModeration

private data class BottomTabItem(
    val route: Any,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

private enum class AuthStage {
    SPLASH, ONBOARDING, LOGIN, AUTHENTICATED
}

@Composable
fun ScamShieldAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { LocalPreferencesManager(context.applicationContext) }
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val themeMode by prefs.themeMode.collectAsStateWithLifecycle(initialValue = "system")
    val languageCode by prefs.languageCode.collectAsStateWithLifecycle(initialValue = "en")
    val onboardingSeen by prefs.onboardingSeen.collectAsStateWithLifecycle(initialValue = false)
    val firebaseUser by remember { Firebase.auth.authStateFlow() }.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    var splashFinished by remember { mutableStateOf(false) }
    var silentAuthAttempted by remember { mutableStateOf(false) }
    var replayOnboarding by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!silentAuthAttempted) {
            silentAuthAttempted = true
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val stage = when {
        !splashFinished -> AuthStage.SPLASH
        replayOnboarding -> AuthStage.ONBOARDING
        !onboardingSeen && firebaseUser == null -> AuthStage.ONBOARDING
        firebaseUser == null -> AuthStage.LOGIN
        else -> AuthStage.AUTHENTICATED
    }

    ProvideAppLanguage(languageCode = languageCode) {
        ScamShieldTheme(themeMode = themeMode) {
            when (stage) {
                AuthStage.SPLASH -> {
                    SplashScreen(onFinished = { splashFinished = true })
                }
                AuthStage.ONBOARDING -> {
                    OnboardingScreen(
                        onComplete = {
                            replayOnboarding = false
                            scope.launch { prefs.setOnboardingSeen(true) }
                        }
                    )
                }
                AuthStage.LOGIN -> {
                    LoginScreen(
                        onAuthSuccess = {
                            scope.launch { prefs.setOnboardingSeen(true) }
                        },
                        onViewOnboarding = {
                            replayOnboarding = true
                        }
                    )
                }
                AuthStage.AUTHENTICATED -> {
                    val user = firebaseUser!!
                    key(user.uid) {
                        AuthenticatedScamShieldShell(
                            userId = user.uid,
                            displayName = user.displayName ?: "ScamShield User",
                            email = user.email,
                            photoUrl = user.photoUrl?.toString(),
                            themeMode = themeMode,
                            languageCode = languageCode,
                            onUpdateTheme = { mode -> scope.launch { prefs.setThemeMode(mode) } },
                            onUpdateLanguage = { code -> scope.launch { prefs.setLanguageCode(code) } },
                            onReplayOnboarding = { replayOnboarding = true },
                            onSignOut = {
                                signOutUser(credentialManager = credentialManager, scope = scope)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedScamShieldShell(
    userId: String,
    displayName: String,
    email: String?,
    photoUrl: String?,
    themeMode: String,
    languageCode: String,
    onUpdateTheme: (String) -> Unit,
    onUpdateLanguage: (String) -> Unit,
    onReplayOnboarding: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: ScamShieldViewModel = viewModel(
        key = "vm_$userId",
        factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val dbId = app.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(dbId)
                ScamShieldViewModel(
                    repository = ScamShieldRepository(firestore),
                    aiService = ScamAnalysisAiService(),
                    currentUserId = userId,
                    currentUserDisplayName = displayName,
                    currentUserEmail = email,
                    currentUserPhotoUrl = photoUrl
                )
            }
        }
    )

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val scanHistoryState by viewModel.scanHistoryState.collectAsStateWithLifecycle()
    val alertsState by viewModel.approvedAlertsState.collectAsStateWithLifecycle()
    val familyGroupsState by viewModel.familyGroupsState.collectAsStateWithLifecycle()
    val incomingInvites by viewModel.incomingInvitations.collectAsStateWithLifecycle()
    val sentInvites by viewModel.sentInvitations.collectAsStateWithLifecycle()
    val familyAlerts by viewModel.familyAlerts.collectAsStateWithLifecycle()
    val myReports by viewModel.myReports.collectAsStateWithLifecycle()
    val pendingAdminReports by viewModel.pendingAdminReports.collectAsStateWithLifecycle()
    val scanState by viewModel.scanExecutionState.collectAsStateWithLifecycle()
    val activeResult by viewModel.activeScanResult.collectAsStateWithLifecycle()
    val ocrState by viewModel.ocrState.collectAsStateWithLifecycle()
    val actionStatusMessage by viewModel.actionStatusMessage.collectAsStateWithLifecycle()
    val profileError by viewModel.profileError.collectAsStateWithLifecycle()

    // Sync cloud preferences once user profile loads
    LaunchedEffect(userProfile?.theme, userProfile?.language) {
        userProfile?.let { prof ->
            if (prof.theme.isNotBlank() && prof.theme != themeMode) {
                onUpdateTheme(prof.theme)
            }
            if (prof.language.isNotBlank() && prof.language != languageCode) {
                onUpdateLanguage(prof.language)
            }
        }
    }

    // Trigger local security notification when high/critical threat is detected
    LaunchedEffect(scanState) {
        if (scanState is ScanExecutionState.Success) {
            val res = (scanState as ScanExecutionState.Success).result
            if ((res.riskLevel == "high" || res.riskLevel == "critical") &&
                (userProfile?.notifSecurityAlerts ?: true)
            ) {
                SecurityNotificationHelper.showSecurityAlertNotification(
                    context = context,
                    channelId = SecurityNotificationHelper.CHANNEL_SECURITY_ALERTS,
                    title = "High-Risk Threat Detected (${res.riskScore}/100)",
                    body = res.recommendation
                )
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionStatusMessage) {
        actionStatusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }
    LaunchedEffect(profileError) {
        profileError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    val strings = LocalStrings.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val topLevelTabs = listOf(
        BottomTabItem(RouteHome, strings.navHome, Icons.Filled.Home, "nav_home"),
        BottomTabItem(RouteHistory, strings.navHistory, Icons.Filled.History, "nav_history"),
        BottomTabItem(RouteAlerts, strings.navAlerts, Icons.Filled.Notifications, "nav_alerts"),
        BottomTabItem(RouteFamily, strings.navFamily, Icons.Filled.FamilyRestroom, "nav_family"),
        BottomTabItem(RouteProfile, strings.navProfile, Icons.Filled.Person, "nav_profile")
    )

    val showChrome = currentDestination?.let { dest ->
        dest.hasRoute<RouteHome>() ||
            dest.hasRoute<RouteHistory>() ||
            dest.hasRoute<RouteAlerts>() ||
            dest.hasRoute<RouteFamily>() ||
            dest.hasRoute<RouteProfile>()
    } ?: true

    fun navigateTopTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showChrome && !useRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        topLevelTabs.forEach { tab ->
                            val selected = when (tab.route) {
                                RouteHome -> currentDestination?.hasRoute<RouteHome>() == true
                                RouteHistory -> currentDestination?.hasRoute<RouteHistory>() == true
                                RouteAlerts -> currentDestination?.hasRoute<RouteAlerts>() == true
                                RouteFamily -> currentDestination?.hasRoute<RouteFamily>() == true
                                RouteProfile -> currentDestination?.hasRoute<RouteProfile>() == true
                                else -> false
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showChrome && useRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        topLevelTabs.forEach { tab ->
                            val selected = when (tab.route) {
                                RouteHome -> currentDestination?.hasRoute<RouteHome>() == true
                                RouteHistory -> currentDestination?.hasRoute<RouteHistory>() == true
                                RouteAlerts -> currentDestination?.hasRoute<RouteAlerts>() == true
                                RouteFamily -> currentDestination?.hasRoute<RouteFamily>() == true
                                RouteProfile -> currentDestination?.hasRoute<RouteProfile>() == true
                                else -> false
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = RouteHome
                    ) {
                        composable<RouteHome> {
                            HomeDashboardScreen(
                                profile = userProfile,
                                displayName = displayName,
                                photoUrl = photoUrl,
                                scanHistoryState = scanHistoryState,
                                alertsState = alertsState,
                                onOpenMessageChecker = { navController.navigate(RouteMessageScanner) },
                                onOpenWebsiteChecker = { navController.navigate(RouteWebsiteChecker) },
                                onOpenPhoneChecker = { navController.navigate(RoutePhoneChecker) },
                                onOpenScreenshotScanner = { navController.navigate(RouteScreenshotScanner) },
                                onOpenScanRecord = { record ->
                                    viewModel.openHistoryRecord(record)
                                    navController.navigate(RouteScanResult)
                                },
                                onViewAllHistory = { navigateTopTab(RouteHistory) },
                                onOpenAlerts = { navigateTopTab(RouteAlerts) },
                                onOpenSettings = { navigateTopTab(RouteProfile) },
                                onOpenLearningCentre = { lessonId ->
                                    navController.navigate(RouteLearningCentre(lessonId = lessonId))
                                }
                            )
                        }

                        composable<RouteHistory> {
                            HistoryScreen(
                                scanHistoryState = scanHistoryState,
                                onOpenScan = { record ->
                                    viewModel.openHistoryRecord(record)
                                    navController.navigate(RouteScanResult)
                                },
                                onDeleteScan = { scanId -> viewModel.deleteScan(scanId) },
                                onClearAll = { viewModel.clearAllHistory() }
                            )
                        }

                        composable<RouteAlerts> {
                            CommunityAlertsScreen(
                                alertsState = alertsState,
                                userCountry = userProfile?.country ?: "US",
                                onSubmitCommunityReport = { category, target, country, desc ->
                                    viewModel.submitScamReport(
                                        reportType = "community_pattern",
                                        category = category,
                                        target = target,
                                        country = country,
                                        description = desc
                                    )
                                }
                            )
                        }

                        composable<RouteFamily> {
                            FamilyScreen(
                                currentUserId = userId,
                                familyGroupsState = familyGroupsState,
                                incomingInvitations = incomingInvites,
                                sentInvitations = sentInvites,
                                familyAlerts = familyAlerts,
                                onCreateGroup = { name -> viewModel.createFamilyGroup(name) },
                                onInviteMember = { group, inviteEmail ->
                                    viewModel.inviteFamilyMember(group, inviteEmail)
                                },
                                onRespondInvitation = { invite, accept ->
                                    viewModel.respondToInvitation(invite, accept)
                                },
                                onRemoveMember = { group, uid ->
                                    viewModel.removeFamilyMember(group, uid)
                                }
                            )
                        }

                        composable<RouteProfile> {
                            ProfileSettingsScreen(
                                profile = userProfile,
                                displayName = displayName,
                                email = email,
                                photoUrl = photoUrl,
                                currentThemeMode = themeMode,
                                currentLanguageCode = languageCode,
                                isAdminUser = viewModel.isAdminUser,
                                onUpdateTheme = onUpdateTheme,
                                onUpdateLanguage = onUpdateLanguage,
                                onUpdateProfileFields = { updates, note ->
                                    viewModel.updateProfileSettings(updates, note)
                                },
                                onOpenLearningCentre = {
                                    navController.navigate(RouteLearningCentre(null))
                                },
                                onOpenSubscriptions = {
                                    navController.navigate(RouteSubscriptions)
                                },
                                onReplayOnboarding = onReplayOnboarding,
                                onOpenAdminDashboard = {
                                    navController.navigate(RouteAdminModeration)
                                },
                                onDeleteAccount = {
                                    viewModel.deleteAccount { onSignOut() }
                                },
                                onLogout = onSignOut
                            )
                        }

                        composable<RouteMessageScanner> {
                            MessageScannerScreen(
                                scanState = scanState,
                                saveHistoryEnabled = userProfile?.saveHistory ?: true,
                                onAnalyze = { text, type ->
                                    viewModel.analyzeMessage(text, type, "message") {
                                        navController.navigate(RouteScanResult)
                                    }
                                },
                                onResetState = { viewModel.resetScanState() },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<RouteWebsiteChecker> {
                            WebsiteCheckerScreen(
                                scanState = scanState,
                                onCheckWebsite = { url ->
                                    viewModel.analyzeWebsite(url) {
                                        navController.navigate(RouteScanResult)
                                    }
                                },
                                onResetState = { viewModel.resetScanState() },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<RoutePhoneChecker> {
                            PhoneCheckerScreen(
                                scanState = scanState,
                                myReports = myReports,
                                defaultCountryIso = userProfile?.country ?: "US",
                                onCheckPhone = { phone, dialCode, countryName ->
                                    viewModel.analyzePhoneNumber(phone, dialCode, countryName) {
                                        navController.navigate(RouteScanResult)
                                    }
                                },
                                onSubmitPhoneReport = { category, target, country, desc ->
                                    viewModel.submitScamReport(
                                        reportType = "phone",
                                        category = category,
                                        target = target,
                                        country = country,
                                        description = desc
                                    )
                                },
                                onResetState = { viewModel.resetScanState() },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<RouteScreenshotScanner> {
                            ScreenshotScannerScreen(
                                ocrState = ocrState,
                                scanState = scanState,
                                onExtractOcr = { uri -> viewModel.extractScreenshotText(context, uri) },
                                onAnalyzeExtractedText = { extracted ->
                                    viewModel.analyzeMessage(extracted, "Screenshot", "screenshot") {
                                        navController.navigate(RouteScanResult)
                                    }
                                },
                                onResetStates = {
                                    viewModel.resetOcrState()
                                    viewModel.resetScanState()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<RouteScanResult> {
                            val groups = (familyGroupsState as? UiDataState.Success)?.data.orEmpty()
                            ScanResultScreen(
                                result = activeResult,
                                familyGroups = groups,
                                onSaveReport = { viewModel.saveCurrentReportManually() },
                                onShareWithFamily = { group -> viewModel.shareActiveReportWithFamily(group) },
                                onReportIncorrect = { notes ->
                                    activeResult?.let { res ->
                                        viewModel.submitScamReport(
                                            reportType = "accuracy_feedback",
                                            category = res.category,
                                            target = res.inputPreview.take(80),
                                            country = userProfile?.country ?: "US",
                                            description = notes
                                        )
                                    }
                                },
                                onScanAnother = {
                                    navController.popBackStack(RouteHome, inclusive = false)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<RouteLearningCentre> { backStackEntry ->
                            val args = backStackEntry.toRoute<RouteLearningCentre>()
                            LearningCentreScreen(
                                completedLessons = userProfile?.completedLessons.orEmpty(),
                                initialLessonId = args.lessonId,
                                onMarkLessonCompleted = { lessonId ->
                                    viewModel.completeLesson(lessonId)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<RouteSubscriptions> {
                            SubscriptionScreen(
                                currentPlan = userProfile?.subscriptionType ?: "free",
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<RouteAdminModeration> {
                            AdminModerationScreen(
                                pendingReports = pendingAdminReports,
                                onModerateReport = { report, approve ->
                                    viewModel.moderateReportAdmin(report, approve)
                                },
                                onPublishAdvisory = { title, explanation, country, category, warningSign, action ->
                                    viewModel.publishAlertAdmin(
                                        title = title,
                                        explanation = explanation,
                                        country = country,
                                        category = category,
                                        warningSign = warningSign,
                                        recommendedAction = action
                                    )
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
