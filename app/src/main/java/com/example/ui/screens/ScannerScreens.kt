package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.model.ScamReport
import com.example.data.security.PhoneNumberHelper
import com.example.ui.components.ErrorRetryBanner
import com.example.ui.components.ScanningProgressCard
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.BluePrimary
import com.example.ui.viewmodel.OcrUiState
import com.example.ui.viewmodel.ScanExecutionState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MessageScannerScreen(
    scanState: ScanExecutionState,
    saveHistoryEnabled: Boolean,
    onAnalyze: (String, String) -> Unit,
    onResetState: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val messageTypes = listOf("WhatsApp", "SMS", "Email", "Job Offer", "Investment Offer", "Other")
    var messageText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("WhatsApp") }

    DisposableEffect(Unit) {
        onDispose { onResetState() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Message Checker") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("message_back_button")) {
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
                    .widthIn(max = 720.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Analyze a Suspicious Message",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Paste any message below and ScamShield AI will help you identify potential warning signs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Message Source (Optional)",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    messageTypes.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type) },
                            modifier = Modifier.testTag("chip_msg_type_$type")
                        )
                    }
                }

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Paste your suspicious message here...") },
                    minLines = 6,
                    maxLines = 12,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("message_input_field")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            messageText = ""
                            onResetState()
                        },
                        enabled = scanState !is ScanExecutionState.Scanning,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("message_clear_button")
                    ) {
                        Icon(Icons.Filled.Clear, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear")
                    }

                    Button(
                        onClick = { onAnalyze(messageText, selectedType) },
                        enabled = scanState !is ScanExecutionState.Scanning,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("analyze_message_button")
                    ) {
                        Icon(Icons.Filled.Security, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.analyzeMessageBtn, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (scanState is ScanExecutionState.Scanning) {
                    ScanningProgressCard(stepMessage = scanState.stepMessage)
                } else if (scanState is ScanExecutionState.Error) {
                    ErrorRetryBanner(
                        message = scanState.message,
                        onRetry = { onAnalyze(messageText, selectedType) },
                        onDismiss = onResetState
                    )
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (saveHistoryEnabled) {
                                "Privacy Note: Only content you explicitly submit is analyzed via Firebase AI Logic. A short privacy-safe summary is saved to your private scan history (adjustable in Settings)."
                            } else {
                                "Privacy Note: Scan History saving is turned OFF in your Settings. Your submission is analyzed securely and not saved to history."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebsiteCheckerScreen(
    scanState: ScanExecutionState,
    onCheckWebsite: (String) -> Unit,
    onResetState: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    var urlInput by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose { onResetState() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Website Safety Checker") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("website_back_button")) {
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
                    .widthIn(max = 720.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Check Website Safety",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Before opening a website or entering your information, check its reputation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = { Text("Paste website address here...") },
                    leadingIcon = { Icon(Icons.Filled.Language, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("website_url_input")
                )

                Button(
                    onClick = { onCheckWebsite(urlInput) },
                    enabled = scanState !is ScanExecutionState.Scanning,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("check_website_button")
                ) {
                    Icon(Icons.Filled.Security, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.checkWebsiteBtn, fontWeight = FontWeight.SemiBold)
                }

                if (scanState is ScanExecutionState.Scanning) {
                    ScanningProgressCard(stepMessage = scanState.stepMessage)
                } else if (scanState is ScanExecutionState.Error) {
                    ErrorRetryBanner(
                        message = scanState.message,
                        onRetry = { onCheckWebsite(urlInput) },
                        onDismiss = onResetState
                    )
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "What ScamShield Verifies",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Lookalike & typosquatted brand domains\n• Punycode obfuscation & shortened links\n• HTTPS encryption & server redirect targets\n• Blocks unsafe private/internal IP targets automatically",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneCheckerScreen(
    scanState: ScanExecutionState,
    myReports: List<ScamReport>,
    defaultCountryIso: String,
    onCheckPhone: (String, String, String) -> Unit,
    onSubmitPhoneReport: (String, String, String, String) -> Unit,
    onResetState: () -> Unit,
    onBack: () -> Unit
) {
    val countries = PhoneNumberHelper.supportedCountries
    var selectedCountry by remember(defaultCountryIso) {
        mutableStateOf(
            countries.firstOrNull { it.isoCode.equals(defaultCountryIso, ignoreCase = true) }
                ?: countries.first()
        )
    }
    var countryMenuOpen by remember { mutableStateOf(false) }
    var phoneInput by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }

    val normalizedPreview = remember(phoneInput, selectedCountry) {
        PhoneNumberHelper.normalizeToE164(phoneInput, selectedCountry.dialCode).getOrNull()
    }
    val matchingReports = remember(normalizedPreview, myReports) {
        if (normalizedPreview == null) emptyList()
        else myReports.filter { it.normalizedTarget == normalizedPreview }
    }

    DisposableEffect(Unit) {
        onDispose { onResetState() }
    }

    if (showReportDialog) {
        PhoneReportDialog(
            initialPhone = normalizedPreview ?: phoneInput,
            countryCode = selectedCountry.isoCode,
            onDismiss = { showReportDialog = false },
            onSubmit = { category, target, desc ->
                onSubmitPhoneReport(category, target, selectedCountry.isoCode, desc)
                showReportDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check a Phone Number") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("phone_back_button")) {
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
                    .widthIn(max = 720.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Check a Phone Number",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Normalize an international phone number, inspect your submitted reports, and get cautious AI scam guidance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = countryMenuOpen,
                    onExpandedChange = { countryMenuOpen = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedCountry.name} (${selectedCountry.dialCode})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Country Calling Code") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryMenuOpen) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("phone_country_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = countryMenuOpen,
                        onDismissRequest = { countryMenuOpen = false }
                    ) {
                        countries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text("${option.name} (${option.dialCode})") },
                                onClick = {
                                    selectedCountry = option
                                    countryMenuOpen = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("e.g. 300 1234567 or +1 415 555 0199") },
                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("phone_number_input")
                )

                if (normalizedPreview != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "International Format: $normalizedPreview",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            onCheckPhone(phoneInput, selectedCountry.dialCode, selectedCountry.name)
                        },
                        enabled = scanState !is ScanExecutionState.Scanning,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("check_phone_button")
                    ) {
                        Text("Check Number", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { showReportDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("report_phone_button")
                    ) {
                        Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Report")
                    }
                }

                if (scanState is ScanExecutionState.Scanning) {
                    ScanningProgressCard(stepMessage = scanState.stepMessage)
                } else if (scanState is ScanExecutionState.Error) {
                    ErrorRetryBanner(
                        message = scanState.message,
                        onRetry = { onCheckPhone(phoneInput, selectedCountry.dialCode, selectedCountry.name) },
                        onDismiss = onResetState
                    )
                }

                if (matchingReports.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Your Submitted Reports (${matchingReports.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            matchingReports.forEach { rep ->
                                Text(
                                    text = "• ${rep.category} (${rep.moderationStatus}): ${rep.description}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Privacy & Accuracy: ScamShield never accesses your contacts or call log automatically, and never guesses personal caller identities without verified telecom records.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneReportDialog(
    initialPhone: String,
    countryCode: String,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    val categories = listOf(
        "Bank impersonation",
        "Fake delivery",
        "Job scam",
        "Investment fraud",
        "Unknown caller",
        "Other"
    )
    var category by remember { mutableStateOf(categories.first()) }
    var target by remember { mutableStateOf(initialPhone) }
    var description by remember { mutableStateOf("") }
    var catExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Suspicious Number", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Reports are queued for moderation. Do not include personal names or private details.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Phone Number ($countryCode)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        categories.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = {
                                    category = c
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("What happened?") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(category, target, description) },
                enabled = target.isNotBlank() && description.isNotBlank()
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScreenshotScannerScreen(
    ocrState: OcrUiState,
    scanState: ScanExecutionState,
    onExtractOcr: (Uri) -> Unit,
    onAnalyzeExtractedText: (String) -> Unit,
    onResetStates: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var editableText by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            onExtractOcr(uri)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            selectedImageUri = uri
            onExtractOcr(uri)
        }
    }

    // Keep editable redaction text synced when OCR succeeds
    androidx.compose.runtime.LaunchedEffect(ocrState) {
        if (ocrState is OcrUiState.Ready) {
            editableText = ocrState.extraction.extractedText
        }
    }

    DisposableEffect(Unit) {
        onDispose { onResetStates() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan a Screenshot") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("screenshot_back_button")) {
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
                    .widthIn(max = 720.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Scan a Screenshot",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Upload a screenshot of a suspicious message and let ScamShield extract text on-device before analyzing it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("choose_gallery_button")
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose from Gallery")
                    }

                    OutlinedButton(
                        onClick = {
                            val dir = File(context.cacheDir, "captures").apply { mkdirs() }
                            val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            pendingCameraUri = uri
                            takePictureLauncher.launch(uri)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("take_photo_button")
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Photo")
                    }
                }

                if (selectedImageUri != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected screenshot preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        selectedImageUri = null
                                        editableText = ""
                                        onResetStates()
                                    },
                                    modifier = Modifier.testTag("remove_screenshot_button")
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove Image")
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DocumentScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No screenshot selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Uses Android's zero-permission Photo Picker. Images stay on your device for OCR extraction until you choose to analyze.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                when (ocrState) {
                    is OcrUiState.Extracting -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text("Extracting text on-device with OCR...")
                        }
                    }
                    is OcrUiState.Error -> {
                        ErrorRetryBanner(
                            message = ocrState.message,
                            onRetry = { selectedImageUri?.let(onExtractOcr) },
                            onDismiss = onResetStates
                        )
                    }
                    is OcrUiState.Ready -> {
                        if (ocrState.extraction.detectedUrls.isNotEmpty() || ocrState.extraction.detectedPhoneNumbers.isNotEmpty()) {
                            Text(
                                text = "Detected Links & Numbers",
                                style = MaterialTheme.typography.labelLarge
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ocrState.extraction.detectedUrls.forEach { url ->
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = url,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                                ocrState.extraction.detectedPhoneNumbers.forEach { phone ->
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = phone,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = editableText,
                            onValueChange = { editableText = it },
                            label = { Text("Extracted Text (Edit or redact private info before scanning)") },
                            minLines = 5,
                            maxLines = 10,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ocr_editable_text")
                        )

                        Button(
                            onClick = { onAnalyzeExtractedText(editableText) },
                            enabled = editableText.isNotBlank() && scanState !is ScanExecutionState.Scanning,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("scan_screenshot_submit_button")
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan Screenshot Text", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    OcrUiState.Idle -> Unit
                }

                if (scanState is ScanExecutionState.Scanning) {
                    ScanningProgressCard(stepMessage = scanState.stepMessage)
                } else if (scanState is ScanExecutionState.Error) {
                    ErrorRetryBanner(
                        message = scanState.message,
                        onRetry = { onAnalyzeExtractedText(editableText) },
                        onDismiss = onResetStates
                    )
                }
            }
        }
    }
}
