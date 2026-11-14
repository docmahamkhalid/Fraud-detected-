package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.EvidenceItem
import com.example.data.model.ScanAnalysisResult
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ScamShieldRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun sampleAnalysis() = ScanAnalysisResult(
        scanType = "message",
        inputPreview = "Urgent: verify your bank account now",
        riskLevel = "high",
        riskScore = 89,
        confidence = "high",
        category = "bank_impersonation",
        summary = "Requests urgent verification via suspicious link.",
        warningSigns = listOf("Urgency", "Suspicious domain"),
        evidence = listOf(EvidenceItem("Urgent phrasing", "Submitted message")),
        recommendation = "Do not click the link.",
        verificationSteps = listOf("Open your official bank app"),
        verificationStatus = "suspicious"
    )

    @Test
    fun ensureUserProfile_createsProfileWithFreeTier() = runBlocking {
        val uid = signInTestUser("alice_${UUID.randomUUID()}@test.com")
        val repo = ScamShieldRepository(firestore)

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile(uid, "Alice Khan", "alice@test.com", null)
        }
        assertEquals(uid, profile.uid)
        assertEquals("free", profile.subscriptionType)
        assertEquals("active", profile.accountStatus)
    }

    @Test
    fun saveScanRecord_andObserveScanHistory_emitsCreatedScan() = runBlocking {
        val uid = signInTestUser("alice_scan_${UUID.randomUUID()}@test.com")
        val repo = ScamShieldRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile(uid, "Alice", "alice@test.com", null)
        }

        val scanId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveScanRecord(uid, sampleAnalysis(), "US")
        }
        assertTrue(scanId.isNotBlank())

        val scans = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeScanHistory(uid).first { list -> list.any { it.id == scanId } }
        }
        assertTrue(scans.any { it.id == scanId && it.riskScore == 89 })
    }

    @Test
    fun crossUserScanRead_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_iso_${UUID.randomUUID()}@test.com")
        val aliceRepo = ScamShieldRepository(firestore)
        val scanId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveScanRecord(aliceUid, sampleAnalysis(), "US")
        }

        signInTestUser("bob_iso_${UUID.randomUUID()}@test.com")
        var failedCode: FirebaseFirestoreException.Code? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("scans").document(scanId).get().await()
            }
        } catch (e: Exception) {
            failedCode = e.unwrapFirestoreException()?.code
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, failedCode)
    }

    @Test
    fun observeScanHistory_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = ScamShieldRepository(firestore)
        var failedCode: FirebaseFirestoreException.Code? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeScanHistory("some_uid_123").first()
            }
        } catch (e: Exception) {
            failedCode = e.unwrapFirestoreException()?.code
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, failedCode)
    }

    @Test
    fun submitScamReport_createsPendingModerationReport() = runBlocking {
        val uid = signInTestUser("reporter_${UUID.randomUUID()}@test.com")
        val repo = ScamShieldRepository(firestore)
        val reportId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitScamReport(
                userId = uid,
                reportType = "phone",
                category = "Bank impersonation",
                normalizedTarget = "+14155550199",
                country = "US",
                description = "Claimed card was locked."
            )
        }
        val reports = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMyReports(uid).first { list -> list.any { it.id == reportId } }
        }
        assertTrue(reports.any { it.id == reportId && it.moderationStatus == "pending" })
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
