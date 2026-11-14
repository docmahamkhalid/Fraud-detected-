package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.billing.BillingUiState
import com.example.data.billing.PlayBillingManager
import com.example.data.model.ScamReport
import com.example.data.model.UserProfile
import com.example.data.notifications.SecurityNotificationHelper
import com.example.data.security.PhoneNumberHelper
import com.example.ui.components.PolicyInfoDialog
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.LocalScamShieldColors
import com.example.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    profile: UserProfile?,
    displayName: String,
    email: String?,
    photoUrl: String?,
    currentThemeMode: String,
    currentLanguageCode: String,
    isAdminUser: Boolean,
    onUpdateTheme: (String) -> Unit,
    onUpdateLanguage: (String) -> Unit,
    onUpdateProfileFields: (Map<String, Any>, String?) -> Unit,
    onOpenLearningCentre: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onReplayOnboarding: () -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onDeleteAccount: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var countryExpanded by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            SecurityNotificationHelper.ensureNotificationChannels(context)
        }
    }

    fun requestNotifPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !SecurityNotificationHelper.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            SecurityNotificationHelper.ensureNotificationChannels(context)
        }
    }

    if (showHelpDialog) {
        PolicyInfoDialog(
            title = "Help & Support",
            body = "ScamShield AI helps you check suspicious SMS, WhatsApp chats, websites, phone numbers, and screenshots.\n\nIf you suspect financial fraud:\n1. Stop replying immediately.\n2. Contact your bank via the phone number on your physical card.\n3. Change compromised passwords and enable 2FA.\n4. Report the pattern in the Alerts tab to help warn others.",
            onDismiss = { showHelpDialog = false }
        )
    }
    if (showPrivacyDialog) {
        PolicyInfoDialog(
            title = "Privacy Policy",
            body = "ScamShield AI only analyzes items you intentionally submit. We never access SMS, emails, WhatsApp messages, or contacts in the background. You can disable Scan History saving or permanently delete your account data anytime.",
            onDismiss = { showPrivacyDialog = false }
        )
    }
    if (showTermsDialog) {
        PolicyInfoDialog(
            title = "Terms & Conditions",
            body = "ScamShield AI delivers AI-assisted risk indicators and educational safety guidance. Results are informational and do not replace bank, carrier, or law-enforcement verification.",
            onDismiss = { showTermsDialog = false }
        )
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account & Data?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This permanently deletes your ScamShield profile and saved scan history from Cloud Firestore, then signs you out.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_account_button")
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    val effectiveName = (profile?.displayName ?: displayName).ifBlank { "ScamShield User" }
    val effectiveEmail = profile?.email ?: email ?: ""
    val selectedCountryCode = profile?.country ?: "US"
    val selectedCountryOption = PhoneNumberHelper.supportedCountries.firstOrNull {
        it.isoCode.equals(selectedCountryCode, ignoreCase = true)
    } ?: PhoneNumberHelper.supportedCountries.first()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 800.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Profile & Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Account Header Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = effectiveName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(effectiveName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (effectiveEmail.isNotBlank()) {
                            Text(
                                effectiveEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Plan: ${(profile?.subscriptionType ?: "free").uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Quick Feature Links
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsActionRow(
                        icon = Icons.Filled.School,
                        title = "Learning Centre",
                        subtitle = "${profile?.completedLessons?.size ?: 0}/10 lessons completed",
                        testTag = "settings_learning_centre",
                        onClick = onOpenLearningCentre
                    )
                    HorizontalDivider()
                    SettingsActionRow(
                        icon = Icons.Filled.WorkspacePremium,
                        title = "Protection Plans & Subscriptions",
                        subtitle = "Manage Free, Premium & Family plans",
                        testTag = "settings_subscriptions",
                        onClick = onOpenSubscriptions
                    )
                    if (isAdminUser) {
                        HorizontalDivider()
                        SettingsActionRow(
                            icon = Icons.Filled.AdminPanelSettings,
                            title = "Admin Moderation Dashboard",
                            subtitle = "Review reported scams & publish verified advisories",
                            testTag = "settings_admin_dashboard",
                            onClick = onOpenAdminDashboard
                        )
                    }
                }
            }

            // Preferences: Country, Theme, Language
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Country & Region", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    ExposedDropdownMenuBox(
                        expanded = countryExpanded,
                        onExpandedChange = { countryExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = "${selectedCountryOption.name} (${selectedCountryOption.isoCode})",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .testTag("settings_country_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = countryExpanded,
                            onDismissRequest = { countryExpanded = false }
                        ) {
                            PhoneNumberHelper.supportedCountries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text("${option.name} (${option.isoCode})") },
                                    onClick = {
                                        countryExpanded = false
                                        onUpdateProfileFields(
                                            mapOf("country" to option.isoCode),
                                            "Region updated to ${option.name}."
                                        )
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Appearance Theme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (mode, label) ->
                            FilterChip(
                                selected = currentThemeMode == mode,
                                onClick = {
                                    onUpdateTheme(mode)
                                    onUpdateProfileFields(mapOf("theme" to mode), null)
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("theme_chip_$mode")
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Language", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "en" to "English",
                            "ur" to "اردو",
                            "hi" to "हिन्दी",
                            "ar" to "العربية"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = currentLanguageCode == code,
                                onClick = {
                                    onUpdateLanguage(code)
                                    onUpdateProfileFields(mapOf("language" to code), null)
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("lang_chip_$code")
                            )
                        }
                    }
                }
            }

            // Notifications & Privacy Controls
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Notifications & Privacy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }

                    SettingsSwitchRow(
                        title = "Security & High-Risk Alerts",
                        subtitle = "Notify when high-risk scams are detected",
                        checked = profile?.notifSecurityAlerts ?: true,
                        onCheckedChange = { enabled ->
                            if (enabled) requestNotifPermissionIfNeeded()
                            onUpdateProfileFields(mapOf("notifSecurityAlerts" to enabled), null)
                        }
                    )
                    SettingsSwitchRow(
                        title = "Family Scam Warnings",
                        subtitle = "Alerts shared by members of your family group",
                        checked = profile?.notifFamilyWarnings ?: true,
                        onCheckedChange = { enabled ->
                            if (enabled) requestNotifPermissionIfNeeded()
                            onUpdateProfileFields(mapOf("notifFamilyWarnings" to enabled), null)
                        }
                    )
                    SettingsSwitchRow(
                        title = "Regional Scam Trends",
                        subtitle = "Important advisories in your country",
                        checked = profile?.notifTrends ?: true,
                        onCheckedChange = { enabled ->
                            if (enabled) requestNotifPermissionIfNeeded()
                            onUpdateProfileFields(mapOf("notifTrends" to enabled), null)
                        }
                    )
                    SettingsSwitchRow(
                        title = "Educational Safety Tips",
                        subtitle = "Optional cybersecurity learning reminders",
                        checked = profile?.notifEducational ?: false,
                        onCheckedChange = { enabled ->
                            if (enabled) requestNotifPermissionIfNeeded()
                            onUpdateProfileFields(mapOf("notifEducational" to enabled), null)
                        }
                    )
                    HorizontalDivider()
                    SettingsSwitchRow(
                        title = "Save Scan History",
                        subtitle = "Sync privacy-safe scan summaries across devices",
                        checked = profile?.saveHistory ?: true,
                        onCheckedChange = { enabled ->
                            onUpdateProfileFields(mapOf("saveHistory" to enabled), null)
                        }
                    )
                }
            }

            // Support & Legal
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsActionRow(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        title = "Help & Support",
                        subtitle = "Safety steps & guidance",
                        testTag = "settings_help",
                        onClick = { showHelpDialog = true }
                    )
                    HorizontalDivider()
                    SettingsActionRow(
                        icon = Icons.Filled.PrivacyTip,
                        title = "Privacy Policy",
                        subtitle = "How ScamShield protects your submissions",
                        testTag = "settings_privacy",
                        onClick = { showPrivacyDialog = true }
                    )
                    HorizontalDivider()
                    SettingsActionRow(
                        icon = Icons.Filled.Description,
                        title = "Terms & Conditions",
                        subtitle = "Usage disclosures & terms",
                        testTag = "settings_terms",
                        onClick = { showTermsDialog = true }
                    )
                    HorizontalDivider()
                    SettingsActionRow(
                        icon = Icons.Filled.Replay,
                        title = "Replay Welcome Guide",
                        subtitle = "View the onboarding introduction again",
                        testTag = "settings_replay_onboarding",
                        onClick = onReplayOnboarding
                    )
                }
            }

            // Account Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onLogout,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Out")
                }

                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("delete_account_button")
                ) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Account")
                }
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    currentPlan: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val billingManager = remember(context) { PlayBillingManager(context) }
    val billingState by billingManager.state.collectAsStateWithLifecycle()
    var billingNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        billingManager.startConnection()
    }
    DisposableEffect(Unit) {
        onDispose { billingManager.destroy() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Protection Plans") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("subs_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Choose Your ScamShield Plan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Core message, website, phone, and screenshot protection tools remain accessible to keep everyone safe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when (val state = billingState) {
                    is BillingUiState.Connecting -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Text("Connecting to Google Play Billing...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    is BillingUiState.Unavailable -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = state.reason,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    is BillingUiState.PurchaseUpdated -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = LocalScamShieldColors.current.safeBg
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    is BillingUiState.Ready -> Unit
                }

                billingNotice?.let { notice ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = notice,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                PlanCard(
                    title = "Free Essential Protection",
                    priceLabel = "$0 / month",
                    isCurrent = currentPlan.equals("free", ignoreCase = true),
                    features = listOf(
                        "Message, Website, Phone & Screenshot AI checks",
                        "Detailed risk scores & verification steps",
                        "Community Alerts & Cybersecurity Learning Centre"
                    ),
                    buttonLabel = if (currentPlan.equals("free", ignoreCase = true)) "Current Plan" else "Included",
                    enabled = false,
                    onClick = {}
                )

                val readyProducts = (billingState as? BillingUiState.Ready)?.products.orEmpty()
                val premiumPrice = readyProducts[PlayBillingManager.PRODUCT_PREMIUM_MONTHLY]
                    ?.subscriptionOfferDetails?.firstOrNull()
                    ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                    ?: "Configured in Play Console"

                PlanCard(
                    title = "ScamShield Premium",
                    priceLabel = premiumPrice,
                    isCurrent = currentPlan.equals("premium", ignoreCase = true),
                    features = listOf(
                        "Priority AI scam analysis & deep threat checks",
                        "Unlimited synced scan history & report exports",
                        "Priority regional threat advisories"
                    ),
                    buttonLabel = "Subscribe via Google Play",
                    enabled = !currentPlan.equals("premium", ignoreCase = true) && activity != null,
                    onClick = {
                        activity?.let {
                            billingNotice = billingManager.launchSubscriptionFlow(
                                it,
                                PlayBillingManager.PRODUCT_PREMIUM_MONTHLY
                            )
                        }
                    }
                )

                val familyPrice = readyProducts[PlayBillingManager.PRODUCT_FAMILY_MONTHLY]
                    ?.subscriptionOfferDetails?.firstOrNull()
                    ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                    ?: "Configured in Play Console"

                PlanCard(
                    title = "ScamShield Family (Up to 5 Members)",
                    priceLabel = familyPrice,
                    isCurrent = currentPlan.equals("family", ignoreCase = true),
                    features = listOf(
                        "All Premium features for up to 5 loved ones",
                        "Shared Family Scam Alert feed",
                        "Privacy-first warnings without exposing private chats"
                    ),
                    buttonLabel = "Subscribe via Google Play",
                    enabled = !currentPlan.equals("family", ignoreCase = true) && activity != null,
                    onClick = {
                        activity?.let {
                            billingNotice = billingManager.launchSubscriptionFlow(
                                it,
                                PlayBillingManager.PRODUCT_FAMILY_MONTHLY
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    priceLabel: String,
    isCurrent: Boolean,
    features: List<String>,
    buttonLabel: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isCurrent) 1.5.dp else 1.dp,
            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(priceLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            features.forEach { feat ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(feat, style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(
                onClick = onClick,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonLabel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminModerationScreen(
    pendingReports: List<ScamReport>,
    onModerateReport: (ScamReport, Boolean) -> Unit,
    onPublishAdvisory: (String, String, String, String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var explanation by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Global") }
    var category by remember { mutableStateOf("Phishing") }
    var warningSign by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Moderation") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Publish Verified Threat Advisory",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("Country (e.g. Global, US, PK)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = explanation, onValueChange = { explanation = it }, label = { Text("Explanation") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = warningSign, onValueChange = { warningSign = it }, label = { Text("Primary Warning Sign") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = action, onValueChange = { action = it }, label = { Text("Recommended Action") }, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    onPublishAdvisory(title, explanation, country, category, warningSign, action)
                                    title = ""
                                    explanation = ""
                                    warningSign = ""
                                    action = ""
                                },
                                enabled = title.isNotBlank() && explanation.isNotBlank() && action.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Publish Advisory")
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Pending Scam Reports (${pendingReports.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (pendingReports.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Moderation queue is clear.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(pendingReports, key = { it.id }) { report ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${report.category} • ${report.reportType} • ${report.country}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("Target: ${report.normalizedTarget}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(report.description, style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(onClick = { onModerateReport(report, true) }) {
                                        Text("Approve & Publish")
                                    }
                                    OutlinedButton(onClick = { onModerateReport(report, false) }) {
                                        Text("Reject")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
