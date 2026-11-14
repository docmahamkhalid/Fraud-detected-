package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.education.CybersecurityLesson
import com.example.data.education.LearningRepository
import com.example.data.model.FamilyGroup
import com.example.data.model.FamilyInvitation
import com.example.data.model.SharedFamilyAlert
import com.example.ui.components.ErrorRetryBanner
import com.example.ui.components.RiskBadgeChip
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.LocalScamShieldColors
import com.example.ui.viewmodel.UiDataState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun FamilyScreen(
    currentUserId: String,
    familyGroupsState: UiDataState<List<FamilyGroup>>,
    incomingInvitations: List<FamilyInvitation>,
    sentInvitations: List<FamilyInvitation>,
    familyAlerts: List<SharedFamilyAlert>,
    onCreateGroup: (String) -> Unit,
    onInviteMember: (FamilyGroup, String) -> Unit,
    onRespondInvitation: (FamilyInvitation, Boolean) -> Unit,
    onRemoveMember: (FamilyGroup, String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var inviteTargetGroup by remember { mutableStateOf<FamilyGroup?>(null) }
    var newGroupName by remember { mutableStateOf("") }
    var inviteEmail by remember { mutableStateOf("") }
    val dateFormatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Family Group", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Protect up to 5 family members by sharing privacy-safe scam warnings without exposing raw chats.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("Group Name (e.g. Khalid Family)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_family_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateGroup(newGroupName)
                        newGroupName = ""
                        showCreateDialog = false
                    },
                    enabled = newGroupName.trim().length >= 2,
                    modifier = Modifier.testTag("confirm_create_family_button")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    inviteTargetGroup?.let { group ->
        AlertDialog(
            onDismissRequest = { inviteTargetGroup = null },
            title = { Text("Invite Family Member", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter the Google account email of a loved one to invite to ${group.name} (${group.memberUids.size}/5 members).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = { inviteEmail = it },
                        label = { Text("Member Email") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invite_member_email_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onInviteMember(group, inviteEmail)
                        inviteEmail = ""
                        inviteTargetGroup = null
                    },
                    enabled = inviteEmail.contains("@") && group.memberUids.size < 5,
                    modifier = Modifier.testTag("confirm_invite_member_button")
                ) {
                    Text("Send Invite")
                }
            },
            dismissButton = {
                TextButton(onClick = { inviteTargetGroup = null }) { Text("Cancel") }
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Family Protection",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Share privacy-safe scam warnings with up to 5 loved ones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier.testTag("create_family_group_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Group")
                    }
                }
            }

            if (incomingInvitations.isNotEmpty()) {
                item {
                    Text(
                        text = "Incoming Invitations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(incomingInvitations, key = { it.id }) { invite ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.MailOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${invite.inviterName} invited you to ${invite.groupName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { onRespondInvitation(invite, true) },
                                    modifier = Modifier.testTag("accept_invite_${invite.id}")
                                ) {
                                    Text("Accept")
                                }
                                OutlinedButton(
                                    onClick = { onRespondInvitation(invite, false) },
                                    modifier = Modifier.testTag("decline_invite_${invite.id}")
                                ) {
                                    Text("Decline")
                                }
                            }
                        }
                    }
                }
            }

            when (familyGroupsState) {
                is UiDataState.Loading -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiDataState.Error -> {
                    item {
                        ErrorRetryBanner(message = familyGroupsState.message)
                    }
                }
                is UiDataState.Success -> {
                    val groups = familyGroupsState.data
                    if (groups.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FamilyRestroom,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Text(
                                        text = "No Family Group Yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Create a group to invite parents, relatives, or trusted contacts. Only anonymized warning summaries are shared—never raw messages.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(onClick = { showCreateDialog = true }) {
                                        Text("Create Family Group")
                                    }
                                }
                            }
                        }
                    } else {
                        items(groups, key = { it.id }) { group ->
                            val isOwner = group.ownerId == currentUserId
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = group.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${group.memberUids.size} / 5 protected members",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isOwner && group.memberUids.size < 5) {
                                            OutlinedButton(
                                                onClick = { inviteTargetGroup = group },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.testTag("invite_to_group_${group.id}")
                                            ) {
                                                Icon(Icons.Filled.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Invite")
                                            }
                                        }
                                    }

                                    group.memberUids.forEachIndexed { idx, uid ->
                                        val label = group.memberLabels.getOrNull(idx) ?: "Member ${idx + 1}"
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Filled.Shield,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (uid == group.ownerId) "$label (Owner)" else label,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            if (isOwner && uid != currentUserId) {
                                                IconButton(
                                                    onClick = { onRemoveMember(group, uid) },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.PersonRemove,
                                                        contentDescription = "Remove member",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
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

            if (sentInvitations.isNotEmpty()) {
                item {
                    Text(
                        text = "Sent Invitations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(sentInvitations, key = { it.id }) { invite ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(invite.inviteeEmail, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(invite.groupName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface) {
                                Text(
                                    text = invite.status.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Shared Family Warnings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (familyAlerts.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No family scam warnings shared yet. After scanning a risky item, tap 'Share With Family' on the result screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(familyAlerts, key = { it.id }) { alert ->
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Shared by ${alert.senderName}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                RiskBadgeChip(
                                    riskLevel = alert.riskLevel,
                                    verificationStatus = "verified_assessment"
                                )
                            }
                            Text(
                                text = alert.privacySafeSummary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Advice: ${alert.recommendation}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = alert.createdAt?.toDate()?.let { dateFormatter.format(it) } ?: "Recently shared",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningCentreScreen(
    completedLessons: List<String>,
    initialLessonId: String?,
    onMarkLessonCompleted: (String) -> Unit,
    onBack: () -> Unit
) {
    val lessons = LearningRepository.lessons
    var selectedLesson by remember(initialLessonId) {
        mutableStateOf(
            if (!initialLessonId.isNullOrBlank()) lessons.firstOrNull { it.id == initialLessonId }
            else null
        )
    }

    if (selectedLesson != null) {
        LessonDetailView(
            lesson = selectedLesson!!,
            isCompleted = completedLessons.contains(selectedLesson!!.id),
            onComplete = {
                onMarkLessonCompleted(selectedLesson!!.id)
            },
            onBack = { selectedLesson = null }
        )
        return
    }

    val progress = if (lessons.isEmpty()) 0f else completedLessons.distinct().size.toFloat() / lessons.size.toFloat()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cybersecurity Learning Centre") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("learning_back_button")) {
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Your Safety Progress",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${completedLessons.distinct().size} / ${lessons.size} completed",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                            )
                        }
                    }
                }

                items(lessons, key = { it.id }) { lesson ->
                    val done = completedLessons.contains(lesson.id)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLesson = lesson }
                            .testTag("lesson_card_${lesson.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${lesson.number}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${lesson.category} • ${lesson.readMinutes} min",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = lesson.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (done) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Completed",
                                    tint = LocalScamShieldColors.current.safe
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.School,
                                    contentDescription = "Open lesson",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonDetailView(
    lesson: CybersecurityLesson,
    isCompleted: Boolean,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    val ext = LocalScamShieldColors.current
    var selectedAnswer by remember(lesson.id) { mutableIntStateOf(-1) }
    var submittedQuiz by remember(lesson.id) { mutableStateOf(false) }
    val question = lesson.quiz.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lesson ${lesson.number}") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("lesson_detail_back")) {
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
                    text = lesson.category.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = lesson.introduction,
                    style = MaterialTheme.typography.bodyLarge
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("How It Works", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(lesson.explanation, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ext.cautionBg),
                    border = BorderStroke(1.dp, ext.caution.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Real-World Example", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ext.caution)
                        Text(lesson.practicalExample, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Text("Actionable Recommendations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                lesson.recommendations.forEach { rec ->
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ext.safe, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(rec, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (question != null) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Quiz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Quick Safety Quiz", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text(question.question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)

                            question.options.forEachIndexed { idx, opt ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedAnswer = idx
                                            submittedQuiz = false
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedAnswer == idx,
                                        onClick = {
                                            selectedAnswer = idx
                                            submittedQuiz = false
                                        }
                                    )
                                    Text(opt, style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            if (submittedQuiz) {
                                val correct = selectedAnswer == question.correctIndex
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (correct) ext.safeBg else ext.highRiskBg
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = if (correct) Icons.Filled.CheckCircle else Icons.Filled.Close,
                                            contentDescription = null,
                                            tint = if (correct) ext.safe else ext.highRisk
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (correct) "Correct! ${question.explanation}" else "Not quite. ${question.explanation}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    submittedQuiz = true
                                    if (selectedAnswer == question.correctIndex) {
                                        onComplete()
                                    }
                                },
                                enabled = selectedAnswer >= 0,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("submit_quiz_button")
                            ) {
                                Text(if (isCompleted) "Check Answer (Completed)" else "Check Answer & Complete")
                            }
                        }
                    }
                }
            }
        }
    }
}
