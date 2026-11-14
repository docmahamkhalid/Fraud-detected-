package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.ScamAnalysisAiService
import com.example.data.firebase.toUserFriendlyFirestoreMessage
import com.example.data.model.EvidenceItem
import com.example.data.model.FamilyGroup
import com.example.data.model.FamilyInvitation
import com.example.data.model.ScanAnalysisResult
import com.example.data.model.ScanRecord
import com.example.data.model.ScamAlert
import com.example.data.model.ScamReport
import com.example.data.model.SharedFamilyAlert
import com.example.data.model.UserProfile
import com.example.data.ocr.OcrExtractionResult
import com.example.data.ocr.ScreenshotOcrService
import com.example.data.repository.ScamShieldRepository
import com.example.data.security.PhoneNumberHelper
import com.example.data.security.ThreatIntelligenceService
import com.example.data.security.UrlInspectionReport
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ScanExecutionState {
    data object Idle : ScanExecutionState
    data class Scanning(val stepMessage: String) : ScanExecutionState
    data class Success(val result: ScanAnalysisResult) : ScanExecutionState
    data class Error(val message: String) : ScanExecutionState
}

sealed interface OcrUiState {
    data object Idle : OcrUiState
    data object Extracting : OcrUiState
    data class Ready(val extraction: OcrExtractionResult) : OcrUiState
    data class Error(val message: String) : OcrUiState
}

sealed interface UiDataState<out T> {
    data object Loading : UiDataState<Nothing>
    data class Success<T>(val data: T) : UiDataState<T>
    data class Error(val message: String) : UiDataState<Nothing>
}

class ScamShieldViewModel(
    private val repository: ScamShieldRepository,
    private val aiService: ScamAnalysisAiService,
    val currentUserId: String,
    val currentUserDisplayName: String,
    val currentUserEmail: String?,
    val currentUserPhotoUrl: String?
) : ViewModel() {

    val isAdminUser: Boolean = currentUserEmail.equals(ADMIN_EMAIL, ignoreCase = true)

    private val _profileError = MutableStateFlow<String?>(null)
    val profileError: StateFlow<String?> = _profileError.asStateFlow()

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { err ->
            val fs = unwrapFirestore(err)
            if (fs == null && err is CancellationException) throw err
            _profileError.value = (fs ?: err).toUserFriendlyFirestoreMessage("Failed to load profile.")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val scanHistoryState: StateFlow<UiDataState<List<ScanRecord>>> =
        repository.observeScanHistory(currentUserId)
            .map<List<ScanRecord>, UiDataState<List<ScanRecord>>> { UiDataState.Success(it) }
            .catch { err ->
                val fs = unwrapFirestore(err)
                if (fs == null && err is CancellationException) throw err
                emit(UiDataState.Error((fs ?: err).toUserFriendlyFirestoreMessage("Failed to load scan history.")))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiDataState.Loading)

    val familyGroupsState: StateFlow<UiDataState<List<FamilyGroup>>> =
        repository.observeFamilyGroups(currentUserId)
            .map<List<FamilyGroup>, UiDataState<List<FamilyGroup>>> { UiDataState.Success(it) }
            .catch { err ->
                val fs = unwrapFirestore(err)
                if (fs == null && err is CancellationException) throw err
                emit(UiDataState.Error((fs ?: err).toUserFriendlyFirestoreMessage("Failed to load family groups.")))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiDataState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    val familyAlerts: StateFlow<List<SharedFamilyAlert>> = familyGroupsState
        .flatMapLatest { state ->
            val group = (state as? UiDataState.Success)?.data?.firstOrNull()
            if (group != null) {
                repository.observeSharedFamilyAlerts(group.id, currentUserId)
                    .catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val incomingInvitations: StateFlow<List<FamilyInvitation>> =
        if (!currentUserEmail.isNullOrBlank()) {
            repository.observeIncomingInvitations(currentUserEmail)
                .catch { emit(emptyList()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
        } else {
            MutableStateFlow(emptyList())
        }

    val sentInvitations: StateFlow<List<FamilyInvitation>> =
        repository.observeSentInvitations(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val approvedAlertsState: StateFlow<UiDataState<List<ScamAlert>>> =
        repository.observeApprovedAlerts()
            .map<List<ScamAlert>, UiDataState<List<ScamAlert>>> { UiDataState.Success(it) }
            .catch { err ->
                val fs = unwrapFirestore(err)
                if (fs == null && err is CancellationException) throw err
                emit(UiDataState.Error((fs ?: err).toUserFriendlyFirestoreMessage("Failed to load community alerts.")))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiDataState.Loading)

    val myReports: StateFlow<List<ScamReport>> =
        repository.observeMyReports(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val pendingAdminReports: StateFlow<List<ScamReport>> =
        if (isAdminUser) {
            repository.observePendingReportsAdmin()
                .catch { emit(emptyList()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
        } else {
            MutableStateFlow(emptyList())
        }

    private val _scanExecutionState = MutableStateFlow<ScanExecutionState>(ScanExecutionState.Idle)
    val scanExecutionState: StateFlow<ScanExecutionState> = _scanExecutionState.asStateFlow()

    private val _activeScanResult = MutableStateFlow<ScanAnalysisResult?>(null)
    val activeScanResult: StateFlow<ScanAnalysisResult?> = _activeScanResult.asStateFlow()

    private val _lastUrlInspection = MutableStateFlow<UrlInspectionReport?>(null)
    val lastUrlInspection: StateFlow<UrlInspectionReport?> = _lastUrlInspection.asStateFlow()

    private val _ocrState = MutableStateFlow<OcrUiState>(OcrUiState.Idle)
    val ocrState: StateFlow<OcrUiState> = _ocrState.asStateFlow()

    private val _actionStatusMessage = MutableStateFlow<String?>(null)
    val actionStatusMessage: StateFlow<String?> = _actionStatusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.ensureUserProfile(
                    uid = currentUserId,
                    displayName = currentUserDisplayName,
                    email = currentUserEmail,
                    photoUrl = currentUserPhotoUrl
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "ensureUserProfile failed", e)
                _profileError.value = e.toUserFriendlyFirestoreMessage("Could not sync your profile.")
            }
        }
    }

    fun clearActionMessage() {
        _actionStatusMessage.value = null
    }

    fun resetScanState() {
        _scanExecutionState.value = ScanExecutionState.Idle
    }

    fun resetOcrState() {
        _ocrState.value = OcrUiState.Idle
    }

    fun openHistoryRecord(record: ScanRecord) {
        val result = ScanAnalysisResult(
            id = record.id,
            scanType = record.scanType,
            inputPreview = record.inputPreview,
            riskLevel = record.riskLevel,
            riskScore = record.riskScore,
            confidence = record.confidence,
            category = record.category,
            summary = record.summary,
            warningSigns = record.warningSigns,
            evidence = record.evidenceObservations.map { EvidenceItem(it, "Saved scan evidence") },
            recommendation = record.recommendation,
            verificationSteps = record.verificationSteps,
            verificationStatus = record.verificationStatus,
            savedToHistory = true
        )
        _activeScanResult.value = result
    }

    fun analyzeMessage(
        message: String,
        messageType: String,
        scanType: String = "message",
        onComplete: () -> Unit
    ) {
        val trimmed = message.trim()
        if (trimmed.isBlank()) {
            _scanExecutionState.value = ScanExecutionState.Error("Please paste or enter a message to analyze.")
            return
        }
        viewModelScope.launch {
            try {
                _scanExecutionState.value = ScanExecutionState.Scanning("Examining suspicious patterns...")
                val profile = userProfile.value
                val country = profile?.country ?: "US"
                val lang = profile?.language ?: "en"

                _scanExecutionState.value = ScanExecutionState.Scanning("Checking potential warning signs...")
                val analysis = aiService.analyzeMessage(
                    rawMessage = trimmed,
                    messageType = messageType,
                    country = country,
                    languageCode = lang,
                    scanType = scanType
                )

                _scanExecutionState.value = ScanExecutionState.Scanning("Preparing your security report...")
                val finalResult = persistScanIfEnabled(analysis, profile)
                _activeScanResult.value = finalResult
                _scanExecutionState.value = ScanExecutionState.Success(finalResult)
                onComplete()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Message analysis failed", e)
                _scanExecutionState.value = ScanExecutionState.Error(
                    e.localizedMessage ?: "Unable to complete analysis. Check your internet connection and retry."
                )
            }
        }
    }

    fun extractScreenshotText(context: Context, uri: Uri) {
        viewModelScope.launch {
            _ocrState.value = OcrUiState.Extracting
            try {
                val extracted = ScreenshotOcrService.extractTextFromUri(context, uri)
                _ocrState.value = OcrUiState.Ready(extracted)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Screenshot OCR failed", e)
                _ocrState.value = OcrUiState.Error(
                    e.localizedMessage ?: "Could not extract text from this image."
                )
            }
        }
    }

    fun analyzeWebsite(rawUrl: String, onComplete: () -> Unit) {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            _scanExecutionState.value = ScanExecutionState.Error("Please enter a website address to check.")
            return
        }
        viewModelScope.launch {
            try {
                _scanExecutionState.value = ScanExecutionState.Scanning("Validating URL and blocking unsafe targets...")
                val inspection = ThreatIntelligenceService.inspectUrl(trimmed)
                _lastUrlInspection.value = inspection

                if (!inspection.isValidFormat || inspection.isBlockedPrivateTarget) {
                    _scanExecutionState.value = ScanExecutionState.Error(
                        inspection.rejectionReason ?: "Invalid or prohibited URL."
                    )
                    return@launch
                }

                _scanExecutionState.value = ScanExecutionState.Scanning("Checking domain reputation and HTTPS status...")
                val profile = userProfile.value
                val analysis = aiService.analyzeWebsite(
                    urlInput = trimmed,
                    inspection = inspection,
                    country = profile?.country ?: "US",
                    languageCode = profile?.language ?: "en"
                )

                _scanExecutionState.value = ScanExecutionState.Scanning("Preparing your security report...")
                val finalResult = persistScanIfEnabled(analysis, profile)
                _activeScanResult.value = finalResult
                _scanExecutionState.value = ScanExecutionState.Success(finalResult)
                onComplete()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Website analysis failed", e)
                _scanExecutionState.value = ScanExecutionState.Error(
                    e.localizedMessage ?: "Website check failed. Please check your connection and retry."
                )
            }
        }
    }

    fun analyzePhoneNumber(
        rawPhone: String,
        dialCode: String,
        countryName: String,
        onComplete: () -> Unit
    ) {
        val normalizedRes = PhoneNumberHelper.normalizeToE164(rawPhone, dialCode)
        if (normalizedRes.isFailure) {
            _scanExecutionState.value = ScanExecutionState.Error(
                normalizedRes.exceptionOrNull()?.message ?: "Invalid phone number."
            )
            return
        }
        val e164 = normalizedRes.getOrThrow()

        viewModelScope.launch {
            try {
                _scanExecutionState.value = ScanExecutionState.Scanning("Checking available phone reports...")
                val matchingReports = myReports.value.filter { it.normalizedTarget == e164 }
                val categories = matchingReports.map { it.category }.distinct()

                val profile = userProfile.value
                _scanExecutionState.value = ScanExecutionState.Scanning("Preparing phone safety assessment...")
                val analysis = aiService.analyzePhoneNumber(
                    e164Number = e164,
                    countryName = countryName,
                    localUserReportCount = matchingReports.size,
                    reportCategories = categories,
                    languageCode = profile?.language ?: "en"
                )

                val finalResult = persistScanIfEnabled(analysis, profile)
                _activeScanResult.value = finalResult
                _scanExecutionState.value = ScanExecutionState.Success(finalResult)
                onComplete()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Phone analysis failed", e)
                _scanExecutionState.value = ScanExecutionState.Error(
                    e.localizedMessage ?: "Phone safety check failed."
                )
            }
        }
    }

    private suspend fun persistScanIfEnabled(
        analysis: ScanAnalysisResult,
        profile: UserProfile?
    ): ScanAnalysisResult {
        val shouldSave = profile?.saveHistory ?: true
        if (!shouldSave) return analysis
        return try {
            val id = repository.saveScanRecord(
                userId = currentUserId,
                analysis = analysis,
                country = profile?.country ?: "US"
            )
            analysis.copy(id = id, savedToHistory = true)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to auto-save scan history", e)
            analysis
        }
    }

    fun saveCurrentReportManually() {
        val current = _activeScanResult.value ?: return
        if (current.savedToHistory) {
            _actionStatusMessage.value = "Report is already saved to your scan history."
            return
        }
        viewModelScope.launch {
            try {
                val id = repository.saveScanRecord(
                    userId = currentUserId,
                    analysis = current,
                    country = userProfile.value?.country ?: "US"
                )
                _activeScanResult.value = current.copy(id = id, savedToHistory = true)
                _actionStatusMessage.value = "Security report saved to history."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionStatusMessage.value = e.toUserFriendlyFirestoreMessage("Could not save report.")
            }
        }
    }

    fun deleteScan(scanId: String) {
        viewModelScope.launch {
            try {
                repository.deleteScanRecord(currentUserId, scanId)
                _actionStatusMessage.value = "Scan deleted."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionStatusMessage.value = e.toUserFriendlyFirestoreMessage("Could not delete scan.")
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            try {
                repository.clearAllScanHistory(currentUserId)
                _actionStatusMessage.value = "All scan history cleared."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionStatusMessage.value = e.toUserFriendlyFirestoreMessage("Could not clear history.")
            }
        }
    }

    // --- FAMILY PROTECTION ---

    fun createFamilyGroup(name: String) {
        if (name.trim().length < 2) {
            _actionStatusMessage.value = "Group name must be at least 2 characters."
            return
        }
        viewModelScope.launch {
            try {
                repository.createFamilyGroup(
                    userId = currentUserId,
                    ownerName = userProfile.value?.displayName ?: currentUserDisplayName,
                    groupName = name
                )
                _actionStatusMessage.value = "Family group created."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionStatusMessage.value = e.toUserFriendlyFirestoreMessage("Failed to create family group.")
            }
        }
    }

    fun inviteFamilyMember(group: FamilyGroup, email: String) {
        val trimmed = email.trim().lowercase()
        if (!trimmed.contains("@") || !trimmed.contains(".")) {
            _actionStatusMessage.value = "Enter a valid email address."
            return
        }
        if (group.memberUids.size >= 5) {
            _actionStatusMessage.value = "Maximum of 5 members reached in Family plan."
            return
        }
        viewModelScope.launch {
            try {
                repository.inviteFamilyMember(
                    group = group,
                    inviterUid = currentUserId,
                    inviterName = userProfile.value?.displayName ?: currentUserDisplayName,
                    inviteeEmail = trimmed
                )
                _actionStatusMessage.value = "Invitation sent to $trimmed."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionStatusMessage.value = e.toUserFriendlyFirestoreMessage("Failed to send invitation.")
            }
        }
    }

    fun respondToInvitation(invitation: FamilyInvitation, accept: Boolean) {
        viewModelScope.launch {
            try {
                repository.respondToInvitation(
                    invitation = invitation,
                    accept = accept,
                    userId = currentUserId,
                    displayName = userProfile.value?.displayName ?: currentUserDisplayName
                )
                _actionStatusMessage.value = if (accept) "Joined ${invitation.groupName}." else "Invitation declined."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionStatusMessage.value = e.toUserFriendlyFirestoreMessage("Could not update invitation.")
            }
        }
    }

    fun removeFamilyMember(group: FamilyGroup, memberUid: String) {
        viewModelScope.launch {
            try {
                repository.removeFamilyMember(group, memberUid)
                _actionStatusMessage.value = "Member removed from group."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionStatusMessage.value = e.toUserFriendlyFirestoreMessage("Could not remove member.")
            }
        }
    }

    fun shareActiveReportWithFamily(group: FamilyGroup) {
        val analysis = _activeScanResult.value ?: return
        viewModelScope.launch {
            try {
                repository.sharePrivacySafeAlertWithFamily(
                    group = group,
                    senderUid = currentUserId,
                    senderName = userProfile.value?.displayName ?: currentUserDisplayName,
                    analysis = analysis
                )
                _actionStatusMessage.value = "Privacy-safe warning shared with ${group.name}."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionStatusMessage.value = e.toUserFriendlyFirestoreMessage("Failed to share warning.")
            }
        }
    }

    // --- REPORTS & MODERATION ---

    fun submitScamReport(
        reportType: String,
        category: String,
        target: String,
        country: String,
        description: String
    ) {
        if (target.isBlank() || description.isBlank()) {
            _actionStatusMessage.value = "Please fill in all required report fields."
            return
        }
        viewModelScope.launch {
            try {
                repository.submitScamReport(
                    userId = currentUserId,
                    reportType = reportType,
                    category = category,
                    normalizedTarget = target,
                    country = country,
                    description = description
                )
                _actionStatusMessage.value = "Report submitted for moderation review."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionStatusMessage.value = e.toUserFriendlyFirestoreMessage("Failed to submit report.")
            }
        }
    }

    fun moderateReportAdmin(report: ScamReport, approve: Boolean) {
        if (!isAdminUser) return
        viewModelScope.launch {
            try {
                repository.moderateReportAdmin(report.id, if (approve) "approved" else "rejected")
                if (approve) {
                    repository.publishScamAlertAdmin(
                        title = "${report.category} Warning (${report.country})",
                        explanation = report.description,
                        country = report.country,
                        category = report.category,
                        sourceType = "community_moderated",
                        warningSigns = listOf("Reported pattern: ${report.normalizedTarget.take(60)}"),
                        recommendedAction = "Verify independently via official channels before responding."
                    )
                }
                _actionStatusMessage.value = if (approve) "Report approved and alert published." else "Report rejected."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionStatusMessage.value = e.toUserFriendlyFirestoreMessage("Moderation update failed.")
            }
        }
    }

    fun publishAlertAdmin(
        title: String,
        explanation: String,
        country: String,
        category: String,
        warningSign: String,
        recommendedAction: String
    ) {
        if (!isAdminUser) return
        viewModelScope.launch {
            try {
                repository.publishScamAlertAdmin(
                    title = title,
                    explanation = explanation,
                    country = country,
                    category = category,
                    sourceType = "verified_intelligence",
                    warningSigns = listOf(warningSign.ifBlank { "Verified advisory" }),
                    recommendedAction = recommendedAction
                )
                _actionStatusMessage.value = "Verified advisory published."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionStatusMessage.value = e.toUserFriendlyFirestoreMessage("Failed to publish advisory.")
            }
        }
    }

    // --- LEARNING & PROFILE SETTINGS ---

    fun completeLesson(lessonId: String) {
        viewModelScope.launch {
            try {
                repository.markLessonCompleted(currentUserId, lessonId)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to mark lesson completed", e)
            }
        }
    }

    fun updateProfileSettings(updates: Map<String, Any>, successNote: String? = null) {
        viewModelScope.launch {
            try {
                repository.updateUserSettings(currentUserId, updates)
                if (successNote != null) _actionStatusMessage.value = successNote
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionStatusMessage.value = e.toUserFriendlyFirestoreMessage("Failed to update settings.")
            }
        }
    }

    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteUserAccountAndRecords(currentUserId)
                onComplete()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionStatusMessage.value = e.toUserFriendlyFirestoreMessage("Account deletion failed.")
            }
        }
    }

    private fun unwrapFirestore(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private companion object {
        const val TAG = "ScamShieldViewModel"
        const val ADMIN_EMAIL = "docmahamkhalid@gmail.com"
    }
}
