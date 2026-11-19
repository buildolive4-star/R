package com.example.ui.gallery

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.UserAccount
import com.example.ui.components.Animated3DMascotOrb
import com.example.ui.components.Animated3DPageBackdrop
import com.example.ui.components.DateMate3DGlowingButton
import com.example.ui.components.DateMateMascot
import com.example.ui.components.GlowButtonStyle
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    firebaseUser: FirebaseUser?,
    userAccount: UserAccount?,
    isProfileBusy: Boolean,
    galleryCount: Int,
    savedCount: Int,
    hasNotificationPermission: Boolean,
    oneSignalPushConfigured: Boolean = false,
    backgroundSyncEnabled: Boolean,
    lastSyncCheckEpochMs: Long,
    onSignInGoogle: () -> Unit,
    onSignOut: () -> Unit,
    onSaveProfile: (
        name: String,
        avatarUri: Uri?,
        galleryUris: List<Uri>,
        phone: String,
        ageText: String,
        address: String,
        city: String,
        bio: String,
        interests: String,
        genderPreference: String,
    ) -> Unit,
    onApplyForStaff: (staffRole: String, staffPrice: String) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onToggleBackgroundSync: (Boolean) -> Unit,
    onCheckSyncNow: () -> Unit,
    onClearSaved: () -> Unit,
    onSecretAdminTrigger: () -> Unit,
    onSendTestAlert: () -> Unit = {},
    authErrorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    var showClearDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    )
                )
            )
    ) {
        Animated3DPageBackdrop()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.profile_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileEditorSection(
                firebaseUser = firebaseUser,
                userAccount = userAccount,
                isBusy = isProfileBusy,
                onSignInGoogle = onSignInGoogle,
                onSignOut = onSignOut,
                onSaveProfile = onSaveProfile,
                onApplyForStaff = onApplyForStaff,
                authErrorMessage = authErrorMessage,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    value = galleryCount.toString(),
                    label = stringResource(R.string.gallery_count_label),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = savedCount.toString(),
                    label = stringResource(R.string.saved_count_label),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Instant Heads-Up Push Alerts Card (Sound + Vibration)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(
                    1.3.dp,
                    Brush.linearGradient(
                        listOf(
                            if (hasNotificationPermission) Color(0xFF00BFA5) else MaterialTheme.colorScheme.primary,
                            Color(0xFFFF80AB).copy(alpha = 0.65f)
                        )
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color(0xFFE91E63).copy(alpha = 0.16f),
                        spotColor = Color(0xFFE91E63).copy(alpha = 0.22f)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Animated3DMascotOrb(
                            mascot = DateMateMascot.SAFETY_MATE,
                            size = 48.dp,
                            showOrbitSparkles = true,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (hasNotificationPermission) {
                                    "SafetyMate & Push Alerts Active"
                                } else {
                                    stringResource(R.string.enable_alerts_title)
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Instant system push alerts with notification chime & vibration even outside the app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (oneSignalPushConfigured) {
                                    stringResource(R.string.onesignal_push_active)
                                } else {
                                    "Live Background Push Engine Active ✓"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (oneSignalPushConfigured) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (!hasNotificationPermission) {
                        Spacer(modifier = Modifier.height(12.dp))
                        DateMate3DGlowingButton(
                            text = stringResource(R.string.enable_alerts_button),
                            onClick = onRequestNotificationPermission,
                            style = GlowButtonStyle.ROSE_PINK,
                            leadingIcon = Icons.Rounded.NotificationsActive,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_enable_alerts_button")
                        )
                    }
                }
            }

            if (savedCount > 0) {
                Spacer(modifier = Modifier.height(18.dp))

                DateMate3DGlowingButton(
                    text = stringResource(R.string.clear_saved_button),
                    onClick = { showClearDialog = true },
                    style = GlowButtonStyle.DANGER_RED,
                    leadingIcon = Icons.Rounded.DeleteSweep,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clear_saved_button")
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Discreet footer: long-press opens hidden Admin Studio passcode gate
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onLongClick = onSecretAdminTrigger
                    )
                    .testTag("studio_version_footer"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.studio_version_footer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                )
            }

            Spacer(modifier = Modifier.height(96.dp))
        }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(text = stringResource(R.string.clear_saved_confirm_title)) },
            text = { Text(text = stringResource(R.string.clear_saved_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        onClearSaved()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_clear_saved_button")
                ) {
                    Text(text = stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(
            1.2.dp,
            Brush.linearGradient(
                listOf(
                    Color(0xFFE91E63).copy(alpha = 0.45f),
                    Color(0xFFFF80AB).copy(alpha = 0.35f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier.shadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(22.dp),
            ambientColor = Color(0xFFE91E63).copy(alpha = 0.14f),
            spotColor = Color(0xFFE91E63).copy(alpha = 0.18f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
