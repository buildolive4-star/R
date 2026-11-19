package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.data.model.AppUpdateConfig
import com.example.data.model.PromoTickerConfig

/**
 * Option 1: Main Admin App Update Publisher Dialog.
 * Lets Main Admin push a new APK download link + Version Code + Release Notes + Force Update toggle live to all installed apps.
 */
@Composable
fun AdminAppUpdateDialog(
    currentConfig: AppUpdateConfig,
    isBusy: Boolean,
    onSave: (
        latestVersionCode: Int,
        latestVersionName: String,
        apkDownloadUrl: String,
        releaseNotes: String,
        forceUpdate: Boolean,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val nextCode = (BuildConfig.VERSION_CODE + 1).coerceAtLeast(2)
    var versionCodeText by remember(currentConfig.latestVersionCode, currentConfig.apkDownloadUrl) {
        val defaultCode = if (currentConfig.apkDownloadUrl.isBlank()) {
            nextCode
        } else {
            currentConfig.latestVersionCode.coerceAtLeast(BuildConfig.VERSION_CODE)
        }
        mutableStateOf(defaultCode.toString())
    }
    var versionNameText by remember(currentConfig.latestVersionName, currentConfig.apkDownloadUrl) {
        val defaultName = if (currentConfig.apkDownloadUrl.isBlank()) {
            "$nextCode.0"
        } else {
            currentConfig.latestVersionName.ifBlank { BuildConfig.VERSION_NAME }
        }
        mutableStateOf(defaultName)
    }
    var apkUrlText by remember(currentConfig.apkDownloadUrl) {
        mutableStateOf(currentConfig.apkDownloadUrl)
    }
    var releaseNotesText by remember(currentConfig.releaseNotes) {
        mutableStateOf(currentConfig.releaseNotes)
    }
    var forceUpdate by remember(currentConfig.forceUpdate, currentConfig.apkDownloadUrl) {
        mutableStateOf(if (currentConfig.apkDownloadUrl.isBlank()) true else currentConfig.forceUpdate)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .testTag("admin_app_update_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("App Live Update Manager", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            Text(
                                "Current App: v${BuildConfig.VERSION_NAME} (Code ${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "100% Hard-Block Rule: Latest Code purane app (${BuildConfig.VERSION_CODE}) se bada rakho (jaise $nextCode), naya APK link paste karo aur Force Update Lock ON karo — kisi bhi user ka purana app open hi nahi hoga (offline bhi nahi) jab tak update na kare!",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                OutlinedButton(
                    onClick = {
                        versionCodeText = nextCode.toString()
                        versionNameText = "$nextCode.0"
                        forceUpdate = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_auto_next_version_button")
                ) {
                    Text(
                        text = "Auto-Fill Next Mandatory Version (Code $nextCode · v$nextCode.0)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = versionCodeText,
                        onValueChange = { versionCodeText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Latest Code") },
                        placeholder = { Text("$nextCode") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = versionNameText,
                        onValueChange = { versionNameText = it },
                        label = { Text("Version Name") },
                        placeholder = { Text("$nextCode.0") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.2f)
                    )
                }

                OutlinedTextField(
                    value = apkUrlText,
                    onValueChange = { apkUrlText = it },
                    label = { Text("Direct APK Download Link (https://)") },
                    placeholder = { Text("https://jaiambey.shop/datemate-v2.apk") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = releaseNotesText,
                    onValueChange = { releaseNotesText = it },
                    label = { Text("What's New / Release Notes") },
                    placeholder = { Text("• New VIP features\n• Faster booking & chat") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (forceUpdate) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hard Force Update Lock (No Bypass)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                if (forceUpdate) "ON: App 100% LOCKED (no Home, no Back, offline anti-bypass) until new APK is installed. Main Admin can long-press version text at bottom."
                                else "OFF: Soft popup — users can tap 'Later' and keep using old app.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = forceUpdate,
                            onCheckedChange = { forceUpdate = it },
                            modifier = Modifier.testTag("admin_force_update_switch")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val code = versionCodeText.toIntOrNull()?.coerceAtLeast(1) ?: BuildConfig.VERSION_CODE
                            onSave(code, versionNameText, apkUrlText, releaseNotesText, forceUpdate)
                        },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.25f)
                            .height(48.dp)
                            .testTag("admin_publish_update_button")
                    ) {
                        Text(if (isBusy) "Publishing..." else "Publish Live", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Feature 2: Main Admin Live Flash Offer / Home Announcement Ticker Dialog.
 */
@Composable
fun AdminPromoTickerDialog(
    currentConfig: PromoTickerConfig,
    isBusy: Boolean,
    onSave: (active: Boolean, badgeLabel: String, text: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var active by remember(currentConfig.active) { mutableStateOf(currentConfig.active) }
    var badgeLabel by remember(currentConfig.badgeLabel) {
        mutableStateOf(currentConfig.badgeLabel.ifBlank { "FLASH OFFER" })
    }
    var text by remember(currentConfig.text) { mutableStateOf(currentConfig.text) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .testTag("admin_promo_ticker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text("Home Live Offer Ticker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Live Offer on Home", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Appears instantly as a glowing announcement bar on every user's Home screen",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = active,
                        onCheckedChange = { active = it },
                        modifier = Modifier.testTag("admin_promo_ticker_switch")
                    )
                }

                OutlinedTextField(
                    value = badgeLabel,
                    onValueChange = { badgeLabel = it },
                    label = { Text("Badge Tag") },
                    placeholder = { Text("FLASH OFFER / VIP LIVE") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Live Announcement / Offer Message") },
                    placeholder = { Text("Weekend VIP Special: Flat ₹100 OFF on Dinner Advance Booking! ✨") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSave(active, badgeLabel, text) },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.25f)
                            .height(48.dp)
                            .testTag("admin_save_ticker_button")
                    ) {
                        Text(if (isBusy) "Saving..." else "Save Ticker", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
