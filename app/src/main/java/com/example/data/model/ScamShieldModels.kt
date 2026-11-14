package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val email: String? = null,
    val photoURL: String? = null,
    val country: String = "US",
    val language: String = "en",
    val theme: String = "system",
    val subscriptionType: String = "free",
    val accountStatus: String = "active",
    val saveHistory: Boolean = true,
    val notifSecurityAlerts: Boolean = true,
    val notifFamilyWarnings: Boolean = true,
    val notifTrends: Boolean = true,
    val notifEducational: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val completedLessons: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ScanRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val scanType: String = "message",
    val inputPreview: String = "",
    val riskLevel: String = "unable_to_verify",
    val riskScore: Int = 0,
    val confidence: String = "medium",
    val category: String = "unknown",
    val summary: String = "",
    val warningSigns: List<String> = emptyList(),
    val evidenceObservations: List<String> = emptyList(),
    val recommendation: String = "",
    val verificationSteps: List<String> = emptyList(),
    val verificationStatus: String = "unable_to_verify",
    val country: String = "US",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FamilyGroup(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val memberUids: List<String> = emptyList(),
    val memberLabels: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SharedFamilyAlert(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val memberUids: List<String> = emptyList(),
    val scanType: String = "message",
    val riskLevel: String = "high",
    val riskScore: Int = 0,
    val category: String = "",
    val privacySafeSummary: String = "",
    val warningSigns: List<String> = emptyList(),
    val recommendation: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FamilyInvitation(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val inviterUid: String = "",
    val inviterName: String = "",
    val inviteeEmail: String = "",
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ScamReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val reportType: String = "phone",
    val category: String = "Other",
    val normalizedTarget: String = "",
    val country: String = "US",
    val description: String = "",
    val moderationStatus: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ScamAlert(
    @DocumentId val id: String = "",
    val title: String = "",
    val explanation: String = "",
    val country: String = "Global",
    val category: String = "Phishing",
    val sourceType: String = "community_moderated",
    val reportCount: Int = 0,
    val warningSigns: List<String> = emptyList(),
    val recommendedAction: String = "",
    val moderationStatus: String = "approved",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class EvidenceItem(
    val observation: String,
    val source: String
)

data class ScanAnalysisResult(
    val id: String = "",
    val scanType: String,
    val inputPreview: String,
    val riskLevel: String,
    val riskScore: Int,
    val confidence: String,
    val category: String,
    val summary: String,
    val warningSigns: List<String>,
    val evidence: List<EvidenceItem>,
    val recommendation: String,
    val verificationSteps: List<String>,
    val verificationStatus: String,
    val technicalSignals: List<String> = emptyList(),
    val savedToHistory: Boolean = false
)
