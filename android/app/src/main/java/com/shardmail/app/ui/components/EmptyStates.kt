package com.shardmail.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shardmail.app.theme.FullShape

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmptyStateContainer(
    icon: ImageVector,
    title: String,
    subtext: String,
    modifier: Modifier = Modifier,
    extraContent: (@Composable () -> Unit)? = null,
    actions: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .padding(bottom = 120.dp), // 120dp bottom clearance
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 72dp tonal circle icon (surfaceContainer fill, primary icon, 32dp icon)
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }

        // Title 18sp / 600
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )

        // Subtext 14sp / onSurfaceVariant (max 280dp wide, 1.45 line height, 6dp top margin)
        Text(
            text = subtext,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 20.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(top = 6.dp)
        )

        if (extraContent != null) {
            Box(modifier = Modifier.padding(top = 12.dp)) {
                extraContent()
            }
        }

        // Pill action buttons in a centered wrapping row (20dp top margin, 10dp gaps)
        FlowRow(
            modifier = Modifier.padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            actions()
        }
    }
}

@Composable
fun EmptyPillButton(
    text: String,
    icon: ImageVector? = null,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    val bg = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val fg = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer

    Row(
        modifier = Modifier
            .height(38.dp)
            .clip(FullShape)
            .background(bg)
            .pressScale(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = fg
        )
    }
}

// State 1: Account with no mail
@Composable
fun AccountEmptyMailState(
    accountAddress: String,
    onRefresh: () -> Unit,
    onViewAllMail: () -> Unit,
    onCopied: (String) -> Unit = {}
) {
    val context = LocalContext.current
    EmptyStateContainer(
        icon = Icons.Outlined.Mail,
        title = "No mail for this account yet",
        subtext = "Emails sent to this alias will automatically appear here.",
        extraContent = {
            // Address in a monospace tonal chip
            Box(
                modifier = Modifier
                    .clip(FullShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = accountAddress,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    ) {
        EmptyPillButton(
            text = "Copy email",
            icon = Icons.Outlined.ContentCopy,
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("ShardMail Address", accountAddress))
                onCopied(accountAddress)
            }
        )
        EmptyPillButton(
            text = "Refresh",
            icon = Icons.Outlined.Refresh,
            onClick = onRefresh
        )
        EmptyPillButton(
            text = "View all mail",
            isPrimary = true,
            onClick = onViewAllMail
        )
    }
}

// State 2: Unified inbox empty
@Composable
fun UnifiedInboxEmptyState(
    onRefresh: () -> Unit,
    onSendTest: () -> Unit
) {
    EmptyStateContainer(
        icon = Icons.Outlined.Inbox,
        title = "Your inbox is empty",
        subtext = "Messages received through Cloudflare Email Routing will show up here in real time."
    ) {
        EmptyPillButton(
            text = "Refresh",
            icon = Icons.Outlined.Refresh,
            onClick = onRefresh
        )
        EmptyPillButton(
            text = "Send test email",
            icon = Icons.AutoMirrored.Outlined.Send,
            isPrimary = true,
            onClick = onSendTest
        )
    }
}

// State 3: No search/filter results
@Composable
fun SearchEmptyState(
    onClearFilters: () -> Unit
) {
    EmptyStateContainer(
        icon = Icons.Outlined.SearchOff,
        title = "No matching emails",
        subtext = "Try adjusting your search terms or clearing the active filters."
    ) {
        EmptyPillButton(
            text = "Clear filters",
            isPrimary = true,
            onClick = onClearFilters
        )
    }
}

// State 4: No accounts
@Composable
fun AccountsEmptyState(
    onCreateAccount: () -> Unit
) {
    EmptyStateContainer(
        icon = Icons.Outlined.PersonAdd,
        title = "No active accounts",
        subtext = "Create your first alias to start receiving emails at your custom domain."
    ) {
        EmptyPillButton(
            text = "+ Create account",
            isPrimary = true,
            onClick = onCreateAccount
        )
    }
}

// State 5: API Error with Retry & Check Keys
@Composable
fun ErrorEmptyState(
    errorMessage: String,
    onRetry: () -> Unit,
    onCheckKeys: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateContainer(
        icon = Icons.Outlined.Error,
        title = "Unable to connect",
        subtext = errorMessage,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(top = 20.dp)
                .fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(FullShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .pressScale { onRetry() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Retry",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(FullShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .pressScale { onCheckKeys() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Check Keys",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
