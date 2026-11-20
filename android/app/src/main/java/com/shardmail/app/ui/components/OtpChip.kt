package com.shardmail.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shardmail.app.theme.CopiedGreenBg
import com.shardmail.app.theme.CopiedGreenFg
import com.shardmail.app.theme.M3Easing
import com.shardmail.app.theme.MotionTapDuration
import kotlinx.coroutines.delay

@Composable
fun OtpChip(
    code: String,
    onCopied: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(1500)
            isCopied = false
        }
    }

    val bgColor by animateColorAsState(
        targetValue = if (isCopied) CopiedGreenBg else MaterialTheme.colorScheme.primaryContainer,
        animationSpec = tween(durationMillis = MotionTapDuration, easing = M3Easing),
        label = "otpBg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isCopied) CopiedGreenFg else MaterialTheme.colorScheme.onPrimaryContainer,
        animationSpec = tween(durationMillis = MotionTapDuration, easing = M3Easing),
        label = "otpFg"
    )

    Row(
        modifier = modifier
            .height(28.dp)
            .clip(MaterialTheme.shapes.small)
            .background(bgColor)
            .pressScale {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("OTP Code", code)
                clipboard.setPrimaryClip(clip)
                isCopied = true
                onCopied(code)
            }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isCopied) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Code copied",
                modifier = Modifier.size(14.dp),
                tint = contentColor
            )
            Text(
                text = "Copied!",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = "Copy code",
                modifier = Modifier.size(14.dp),
                tint = contentColor
            )
            Text(
                text = "Copy $code",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }
    }
}
