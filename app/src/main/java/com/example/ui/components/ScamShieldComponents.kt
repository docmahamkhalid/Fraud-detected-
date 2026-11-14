package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.ScanAnalysisResult
import com.example.ui.theme.BlueBright
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.LocalScamShieldColors
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.White

@Composable
fun ScamShieldLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    animated: Boolean = false
) {
    val scale = if (animated) {
        val transition = rememberInfiniteTransition(label = "shieldPulse")
        val pulse by transition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
        pulse
    } else 1f

    Canvas(
        modifier = modifier
            .size(size)
            .scale(scale)
    ) {
        val w = this.size.width
        val h = this.size.height

        val shieldPath = Path().apply {
            moveTo(w * 0.50f, h * 0.06f)
            lineTo(w * 0.88f, h * 0.21f)
            lineTo(w * 0.88f, h * 0.48f)
            cubicTo(w * 0.88f, h * 0.72f, w * 0.70f, h * 0.87f, w * 0.50f, h * 0.95f)
            cubicTo(w * 0.30f, h * 0.87f, w * 0.12f, h * 0.72f, w * 0.12f, h * 0.48f)
            lineTo(w * 0.12f, h * 0.21f)
            close()
        }

        drawPath(
            path = shieldPath,
            brush = Brush.linearGradient(
                colors = listOf(BlueBright, BluePrimary, NavyPrimary),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        drawPath(
            path = shieldPath,
            color = White.copy(alpha = 0.85f),
            style = Stroke(width = w * 0.04f, join = StrokeJoin.Round)
        )

        val checkPath = Path().apply {
            moveTo(w * 0.33f, h * 0.50f)
            lineTo(w * 0.46f, h * 0.63f)
            lineTo(w * 0.69f, h * 0.38f)
        }
        drawPath(
            path = checkPath,
            color = White,
            style = Stroke(
                width = w * 0.09f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
fun riskColorFor(riskLevel: String): Pair<Color, Color> {
    val ext = LocalScamShieldColors.current
    return when (riskLevel.lowercase()) {
        "critical" -> ext.criticalRisk to ext.criticalRiskBg
        "high" -> ext.highRisk to ext.highRiskBg
        "caution" -> ext.caution to ext.cautionBg
        "low" -> ext.safe to ext.safeBg
        else -> ext.neutralStatus to ext.neutralStatusBg
    }
}

fun riskDisplayLabel(riskLevel: String, verificationStatus: String): String {
    return when {
        verificationStatus == "unable_to_verify" || riskLevel == "unable_to_verify" -> "UNABLE TO VERIFY"
        riskLevel == "critical" -> "CRITICAL RISK"
        riskLevel == "high" -> "HIGH RISK"
        riskLevel == "caution" -> "SUSPICIOUS"
        verificationStatus == "no_known_threat" -> "NO KNOWN THREAT"
        else -> "SUSPICIOUS"
    }
}

@Composable
fun RiskBadgeChip(
    riskLevel: String,
    verificationStatus: String,
    modifier: Modifier = Modifier
) {
    val (fg, bg) = riskColorFor(riskLevel)
    val label = riskDisplayLabel(riskLevel, verificationStatus)
    val icon = when (riskLevel.lowercase()) {
        "critical", "high" -> Icons.Filled.ErrorOutline
        "caution" -> Icons.Filled.WarningAmber
        "low" -> Icons.Filled.CheckCircle
        else -> Icons.AutoMirrored.Filled.HelpOutline
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = fg
            )
        }
    }
}

@Composable
fun RiskCircularGauge(
    score: Int,
    riskLevel: String,
    verificationStatus: String,
    modifier: Modifier = Modifier
) {
    val (fg, bg) = riskColorFor(riskLevel)
    val animatedSweep by animateFloatAsState(
        targetValue = (score.coerceIn(0, 100) / 100f) * 360f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "gaugeSweep"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(164.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                val strokeWidth = 14.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                drawArc(
                    color = fg.copy(alpha = 0.16f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = fg,
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$score/100",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = fg
                )
                Text(
                    text = "Risk Score",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        RiskBadgeChip(riskLevel = riskLevel, verificationStatus = verificationStatus)
    }
}

@Composable
fun ScanningProgressCard(
    stepMessage: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("scanning_progress_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 4.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = "Scanning",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = stepMessage,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Analyzing indicators securely with ScamShield AI",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ErrorRetryBanner(
    message: String,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val ext = LocalScamShieldColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ext.highRiskBg),
        border = BorderStroke(1.dp, ext.highRisk.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = "Error",
                tint = ext.highRisk
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (onRetry != null) {
                TextButton(onClick = onRetry, modifier = Modifier.testTag("error_retry_button")) {
                    Text("Retry")
                }
            }
            if (onDismiss != null) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        }
    }
}

object ShareWarningHelper {

    fun buildPrivacySafeWarningText(result: ScanAnalysisResult): String {
        val statusLabel = riskDisplayLabel(result.riskLevel, result.verificationStatus)
        val warningsText = if (result.warningSigns.isEmpty()) {
            "- Verify sender identity before taking action."
        } else {
            result.warningSigns.take(4).joinToString("\n") { "- $it" }
        }
        return buildString {
            appendLine("SCAMSHIELD SECURITY ALERT")
            appendLine("Status: $statusLabel (${result.riskScore}/100)")
            appendLine()
            appendLine(result.summary)
            appendLine()
            appendLine("Warning signs:")
            appendLine(warningsText)
            appendLine()
            appendLine("Recommended action: ${result.recommendation}")
            appendLine()
            append("Before you click, pay or reply — check it with ScamShield AI.")
        }
    }

    fun shareText(context: Context, title: String, content: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(sendIntent, title))
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}

@Composable
fun PolicyInfoDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
