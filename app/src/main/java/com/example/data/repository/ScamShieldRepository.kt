package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.FamilyGroup
import com.example.data.model.FamilyInvitation
import com.example.data.model.ScanAnalysisResult
import com.example.data.model.ScanRecord
import com.example.data.model.ScamAlert
import com.example.data.model.ScamReport
import com.example.data.model.SharedFamilyAlert
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ScamShieldRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- USER PROFILE ---

    suspend fun ensureUserProfile(
        uid: String,
        displayName: String,
        email: String?,
        photoUrl: String?,
        defaultCountry: String = "US",
        defaultLanguage: String = "en"
    ): UserProfile {
        val docRef = db.collection("users").document(uid)
        try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val existing = snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existing != null) {
                    val updates = mutableMapOf<String, Any>()
                    if (!email.isNullOrBlank() && existing.email != email) {
                        updates["email"] = email
                    }
                    if (!photoUrl.isNullOrBlank() && existing.photoURL != photoUrl) {
                        updates["photoURL"] = photoUrl
                    }
                    if (updates.isNotEmpty()) {
                        updates["updatedAt"] = FieldValue.serverTimestamp()
                        docRef.update(updates).await()
                    }
                    return existing
                }
            }

            val safeName = displayName.trim().ifBlank { "ScamShield User" }.take(80)
            val payload = mutableMapOf<String, Any>(
                "uid" to uid,
                "displayName" to safeName,
                "country" to defaultCountry.take(60),
                "language" to defaultLanguage.take(10),
                "theme" to "system",
                "subscriptionType" to "free",
                "accountStatus" to "active",
                "saveHistory" to true,
                "notifSecurityAlerts" to true,
                "notifFamilyWarnings" to true,
                "notifTrends" to true,
                "notifEducational" to false,
                "onboardingCompleted" to false,
                "completedLessons" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!email.isNullOrBlank()) payload["email"] = email.take(150)
            if (!photoUrl.isNullOrBlank()) payload["photoURL"] = photoUrl.take(500)

            docRef.set(payload).await()
            val createdSnapshot = docRef.get().await()
            return createdSnapshot.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: UserProfile(uid = uid, displayName = safeName, email = email, photoURL = photoUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error !is CancellationException || auth.currentUser?.uid == userId) {
                        handleFirestoreError(error, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun updateUserSettings(userId: String, updates: Map<String, Any>) {
        val uid = requireUserId()
        require(uid == userId) { "Cannot update another user's profile." }
        val docRef = db.collection("users").document(userId)
        val filtered = updates.toMutableMap().apply {
            remove("uid")
            remove("subscriptionType")
            remove("createdAt")
            put("updatedAt", FieldValue.serverTimestamp())
        }
        try {
            docRef.update(filtered).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun markLessonCompleted(userId: String, lessonId: String) {
        val uid = requireUserId()
        require(uid == userId)
        val docRef = db.collection("users").document(userId)
        try {
            docRef.update(
                mapOf(
                    "completedLessons" to FieldValue.arrayUnion(lessonId),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteUserAccountAndRecords(userId: String) {
        val uid = requireUserId()
        require(uid == userId)
        try {
            val scans = db.collection("users").document(userId).collection("scans").get().await()
            scans.documents.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            val reports = db.collection("scamReports").whereEqualTo("userId", userId).get().await()
            reports.documents.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            val groups = db.collection("familyGroups").whereArrayContains("memberUids", userId).get().await()
            for (groupDoc in groups.documents) {
                val ownerId = groupDoc.getString("ownerId")
                if (ownerId == userId) {
                    val alerts = groupDoc.reference.collection("sharedAlerts").get().await()
                    alerts.documents.forEach { it.reference.delete().await() }
                    groupDoc.reference.delete().await()
                }
            }
            db.collection("users").document(userId).delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$userId")
            throw e
        }
    }

    // --- SCAN HISTORY ---

    fun observeScanHistory(userId: String): Flow<List<ScanRecord>> = flow {
        val path = "users/$userId/scans"
        emitAll(
            db.collection("users").document(userId).collection("scans")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ScanRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveScanRecord(
        userId: String,
        analysis: ScanAnalysisResult,
        country: String
    ): String {
        val uid = requireUserId()
        require(uid == userId)
        val docRef = db.collection("users").document(userId).collection("scans").document()
        val payload = mapOf(
            "userId" to uid,
            "scanType" to analysis.scanType,
            "inputPreview" to analysis.inputPreview.take(200).ifBlank { "Scanned content" },
            "riskLevel" to analysis.riskLevel,
            "riskScore" to analysis.riskScore.coerceIn(0, 100),
            "confidence" to analysis.confidence,
            "category" to analysis.category.take(60).ifBlank { "general" },
            "summary" to analysis.summary.take(600).ifBlank { "Analysis complete." },
            "warningSigns" to analysis.warningSigns.take(15),
            "evidenceObservations" to analysis.evidence.map { "${it.observation} (${it.source})" }.take(15),
            "recommendation" to analysis.recommendation.take(500).ifBlank { "Verify before taking action." },
            "verificationSteps" to analysis.verificationSteps.take(10),
            "verificationStatus" to analysis.verificationStatus,
            "country" to country.take(60).ifBlank { "US" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteScanRecord(userId: String, scanId: String) {
        val uid = requireUserId()
        require(uid == userId)
        val docRef = db.collection("users").document(userId).collection("scans").document(scanId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun clearAllScanHistory(userId: String) {
        val uid = requireUserId()
        require(uid == userId)
        val colRef = db.collection("users").document(userId).collection("scans")
        try {
            val snapshot = colRef.get().await()
            snapshot.documents.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, colRef.path)
            throw e
        }
    }

    // --- FAMILY PROTECTION ---

    fun observeFamilyGroups(userId: String): Flow<List<FamilyGroup>> = flow {
        val path = "familyGroups"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FamilyGroup::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createFamilyGroup(
        userId: String,
        ownerName: String,
        groupName: String
    ): String {
        val uid = requireUserId()
        require(uid == userId)
        val docRef = db.collection("familyGroups").document()
        val payload = mapOf(
            "ownerId" to uid,
            "name" to groupName.trim().take(60),
            "memberUids" to listOf(uid),
            "memberLabels" to listOf(ownerName.trim().ifBlank { "Owner" }.take(60)),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeSharedFamilyAlerts(groupId: String, userId: String): Flow<List<SharedFamilyAlert>> = flow {
        val path = "familyGroups/$groupId/sharedAlerts"
        emitAll(
            db.collection("familyGroups").document(groupId).collection("sharedAlerts")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SharedFamilyAlert::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt }
                }
                .catch { error ->
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun sharePrivacySafeAlertWithFamily(
        group: FamilyGroup,
        senderUid: String,
        senderName: String,
        analysis: ScanAnalysisResult
    ) {
        val uid = requireUserId()
        require(uid == senderUid)
        val docRef = db.collection("familyGroups").document(group.id).collection("sharedAlerts").document()
        val sanitizedSummary = "${analysis.category.replace('_', ' ').replaceFirstChar { it.uppercase() }} " +
            "(Risk ${analysis.riskScore}/100): ${analysis.summary}".take(500)
        val payload = mapOf(
            "groupId" to group.id,
            "senderUid" to uid,
            "senderName" to senderName.trim().ifBlank { "Family Member" }.take(80),
            "memberUids" to group.memberUids.take(5),
            "scanType" to analysis.scanType,
            "riskLevel" to analysis.riskLevel,
            "riskScore" to analysis.riskScore.coerceIn(0, 100),
            "category" to analysis.category.take(60).ifBlank { "scam_warning" },
            "privacySafeSummary" to sanitizedSummary,
            "warningSigns" to analysis.warningSigns.take(10),
            "recommendation" to analysis.recommendation.take(400).ifBlank { "Verify before taking action." },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun inviteFamilyMember(
        group: FamilyGroup,
        inviterUid: String,
        inviterName: String,
        inviteeEmail: String
    ) {
        val uid = requireUserId()
        require(uid == inviterUid)
        require(group.memberUids.size < 5) { "Maximum of 5 family members supported in this plan." }
        val docRef = db.collection("familyInvitations").document()
        val payload = mapOf(
            "groupId" to group.id,
            "groupName" to group.name.take(60),
            "inviterUid" to uid,
            "inviterName" to inviterName.trim().ifBlank { "Family Admin" }.take(80),
            "inviteeEmail" to inviteeEmail.trim().lowercase().take(150),
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeIncomingInvitations(userEmail: String): Flow<List<FamilyInvitation>> = flow {
        val normalized = userEmail.trim().lowercase()
        val path = "familyInvitations"
        emitAll(
            db.collection(path)
                .whereEqualTo("inviteeEmail", normalized)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FamilyInvitation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).filter { it.status == "pending" }
                }
                .catch { error ->
                    handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeSentInvitations(userId: String): Flow<List<FamilyInvitation>> = flow {
        val path = "familyInvitations"
        emitAll(
            db.collection(path)
                .whereEqualTo("inviterUid", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FamilyInvitation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun respondToInvitation(
        invitation: FamilyInvitation,
        accept: Boolean,
        userId: String,
        displayName: String
    ) {
        val uid = requireUserId()
        require(uid == userId)
        val invRef = db.collection("familyInvitations").document(invitation.id)
        try {
            invRef.update(
                mapOf(
                    "status" to if (accept) "accepted" else "declined",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (accept) {
                val groupRef = db.collection("familyGroups").document(invitation.groupId)
                val snap = groupRef.get().await()
                val group = snap.toObject(FamilyGroup::class.java)
                if (group != null && !group.memberUids.contains(uid) && group.memberUids.size < 5) {
                    groupRef.update(
                        mapOf(
                            "memberUids" to group.memberUids + uid,
                            "memberLabels" to group.memberLabels + displayName.trim().ifBlank { "Member" }.take(60),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, invRef.path)
            throw e
        }
    }

    suspend fun removeFamilyMember(group: FamilyGroup, targetUid: String) {
        val uid = requireUserId()
        val idx = group.memberUids.indexOf(targetUid)
        if (idx < 0 || targetUid == group.ownerId) return
        val updatedUids = group.memberUids.filterIndexed { i, _ -> i != idx }
        val updatedLabels = group.memberLabels.filterIndexed { i, _ -> i != idx }
        val docRef = db.collection("familyGroups").document(group.id)
        try {
            docRef.update(
                mapOf(
                    "memberUids" to updatedUids,
                    "memberLabels" to updatedLabels,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- COMMUNITY SCAM ALERTS & REPORTS ---

    fun observeApprovedAlerts(): Flow<List<ScamAlert>> = flow {
        val path = "scamAlerts"
        emitAll(
            db.collection(path)
                .whereEqualTo("moderationStatus", "approved")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ScamAlert::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt }
                }
                .catch { error ->
                    handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun submitScamReport(
        userId: String,
        reportType: String,
        category: String,
        normalizedTarget: String,
        country: String,
        description: String
    ): String {
        val uid = requireUserId()
        require(uid == userId)
        val docRef = db.collection("scamReports").document()
        val payload = mapOf(
            "userId" to uid,
            "reportType" to reportType,
            "category" to category.take(60),
            "normalizedTarget" to normalizedTarget.take(200),
            "country" to country.take(60),
            "description" to description.trim().take(600),
            "moderationStatus" to "pending",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeMyReports(userId: String): Flow<List<ScamReport>> = flow {
        val path = "scamReports"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ScamReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt }
                }
                .catch { error ->
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // --- ADMIN MODERATION (Restricted by Firestore Security Rules) ---

    fun observePendingReportsAdmin(): Flow<List<ScamReport>> = flow {
        val path = "scamReports"
        emitAll(
            db.collection(path)
                .whereEqualTo("moderationStatus", "pending")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ScamReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt }
                }
                .catch { error ->
                    handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun moderateReportAdmin(reportId: String, status: String) {
        requireUserId()
        val docRef = db.collection("scamReports").document(reportId)
        try {
            docRef.update(
                mapOf(
                    "moderationStatus" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun publishScamAlertAdmin(
        title: String,
        explanation: String,
        country: String,
        category: String,
        sourceType: String,
        warningSigns: List<String>,
        recommendedAction: String
    ) {
        requireUserId()
        val docRef = db.collection("scamAlerts").document()
        val payload = mapOf(
            "title" to title.trim().take(120),
            "explanation" to explanation.trim().take(600),
            "country" to country.trim().take(60),
            "category" to category.trim().take(60),
            "sourceType" to sourceType,
            "reportCount" to 1,
            "warningSigns" to warningSigns.take(10),
            "recommendedAction" to recommendedAction.trim().take(400),
            "moderationStatus" to "approved",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }
}
