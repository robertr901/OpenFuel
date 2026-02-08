package com.openfuel.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.openfuel.app.ui.theme.Dimens

@Composable
fun ProPaywallDialog(
    show: Boolean,
    isActionInProgress: Boolean,
    message: String?,
    onDismiss: () -> Unit,
    onPurchaseClick: () -> Unit,
    onRestoreClick: () -> Unit,
) {
    if (!show) return

    AlertDialog(
        modifier = Modifier.testTag("paywall_dialog"),
        onDismissRequest = {
            if (!isActionInProgress) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = "OpenFuel Pro",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.s)) {
                Text(
                    text = "Free includes local logging, search, and standard export.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Pro includes advanced exports (CSV + redacted mode) and Insights.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "No ads, no trackers, no telemetry. Purchases are managed by Google Play.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (message != null) {
                    Text(
                        text = message,
                        modifier = Modifier.testTag("paywall_message"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isActionInProgress) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.testTag("paywall_progress"))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier.testTag("paywall_upgrade_button"),
                enabled = !isActionInProgress,
                onClick = onPurchaseClick,
            ) {
                Text("Upgrade to Pro")
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    modifier = Modifier.testTag("paywall_restore_button"),
                    enabled = !isActionInProgress,
                    onClick = onRestoreClick,
                ) {
                    Text("Restore")
                }
                TextButton(
                    modifier = Modifier.testTag("paywall_close_button"),
                    enabled = !isActionInProgress,
                    onClick = onDismiss,
                ) {
                    Text("Not now")
                }
            }
        },
    )
}
