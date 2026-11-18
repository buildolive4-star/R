package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.firebase.GoogleAuthHelper
import com.example.data.model.AdminAccessCode
import com.example.data.model.AdminEmailEntry
import com.example.data.model.AdminMemberProfile
import com.google.firebase.auth.FirebaseUser
import kotlin.random.Random

@Composable
fun AdminAccessGateView(
    authUser: FirebaseUser?,
    isBusy: Boolean,
    authErrorMessage: String?,
    onSignInGoogle: () -> Unit,
    onSignOut: () -> Unit,
    onRedeemAccessCode: (name: String, phone: String, email: String, code: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val diagInfo = remember(context) { GoogleAuthHelper.getAuthDiagnosticInfo(context) }

    var name by remember(authUser?.uid) {
        mutableStateOf(authUser?.displayName.orEmpty())
    }
    var phone by remember { mutableStateOf("") }
    var email by remember(authUser?.uid, authUser?.email) {
        mutableStateOf(authUser?.email.orEmpty())
    }
    var accessCode by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (authUser == null) Icons.Rounded.Shield else Icons.Rounded.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (authUser == null) {
                                "Step 1: Verify Google Account"
                            } else {
                                "Step 2: Enter Main Admin Code"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (authUser == null) {
                                "Main Admin opens directly. Partners enter the code created by Main Admin."
                            } else {
                                "Signed in as ${authUser.email}. Enter your details & active code from Main Admin."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (authUser == null) {
                    Button(
                        onClick = onSignInGoogle,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("admin_gate_google_signin_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifying Google…")
                        } else {
                            Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Continue with Google", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (diagInfo.isSha1Matched && diagInfo.hasWebClientId) {
                            "Build Key: Verified (${diagInfo.packageName})"
                        } else {
                            "Build Key Warning: SHA-1 ${diagInfo.runtimeSha1Colon ?: "unknown"}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (diagInfo.isSha1Matched && diagInfo.hasWebClientId) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        label = { Text("Your Full Name *") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_redeem_name_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(15) },
                        singleLine = true,
                        label = { Text("Mobile Number *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_redeem_phone_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        singleLine = true,
                        label = { Text("Email Address *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_redeem_email_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = accessCode,
                        onValueChange = { accessCode = it.uppercase().filter { ch -> ch.isLetterOrDigit() || ch == '-' || ch == '_' }.take(24) },
                        singleLine = true,
                        label = { Text("Main Admin Transfer Code *") },
                        placeholder = { Text("e.g. JU-4829") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_redeem_code_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onRedeemAccessCode(name, phone, email, accessCode) },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_redeem_submit_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifying Code…")
                        } else {
                            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unlock Admin Panel", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Switch Google Account")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Only Main Admin can create or delete Transfer Codes. Once Main Admin deletes a code, panel access stops immediately.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!authErrorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_auth_error_banner")
                    ) {
                        SelectionContainer {
                            Text(
                                text = authErrorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainAdminTransferDialog(
    authUserEmail: String,
    myAdminMember: AdminMemberProfile?,
    adminAccessCodes: List<AdminAccessCode>,
    adminMembers: List<AdminMemberProfile>,
    adminEmails: List<AdminEmailEntry>,
    isBusy: Boolean,
    onSaveMainAdminProfile: (name: String, phone: String, email: String) -> Unit,
    onCreateAccessCode: (code: String, label: String) -> Unit,
    onDeleteAccessCode: (code: String) -> Unit,
    onRemoveAdminMember: (uid: String) -> Unit,
    onAddEmail: (String) -> Unit,
    onRemoveEmail: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var mainName by remember(myAdminMember?.name) { mutableStateOf(myAdminMember?.name.orEmpty()) }
    var mainPhone by remember(myAdminMember?.phone) { mutableStateOf(myAdminMember?.phone.orEmpty()) }
    var mainEmail by remember(myAdminMember?.email, authUserEmail) {
        mutableStateOf(myAdminMember?.email?.ifBlank { authUserEmail } ?: authUserEmail)
    }

    var codeDraft by remember { mutableStateOf("") }
    var codeLabelDraft by remember { mutableStateOf("") }
    var emailDraft by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null) },
        title = { Text("Main Admin & Transfer Codes") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Main Admin Identity
                Text(
                    text = "Main Admin Identity",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = mainName,
                    onValueChange = { mainName = it },
                    singleLine = true,
                    label = { Text("Main Admin Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mainPhone,
                    onValueChange = { mainPhone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(15) },
                    singleLine = true,
                    label = { Text("Main Admin Mobile No.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mainEmail,
                    onValueChange = { mainEmail = it },
                    singleLine = true,
                    label = { Text("Main Admin Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                FilledTonalButton(
                    onClick = { onSaveMainAdminProfile(mainName, mainPhone, mainEmail) },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Main Admin Details")
                }

                HorizontalDivider()

                // 2. Create Revocable Transfer Code
                Text(
                    text = "Admin Transfer Code (Only Main Admin)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Generate or type a code to share Admin access. Delete the code anytime to lock out that person immediately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = codeDraft,
                        onValueChange = {
                            codeDraft = it.uppercase().filter { ch -> ch.isLetterOrDigit() || ch == '-' || ch == '_' }.take(24)
                        },
                        singleLine = true,
                        label = { Text("Access Code") },
                        placeholder = { Text("JU-4829") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_transfer_code_input")
                    )
                    FilledTonalButton(
                        onClick = {
                            codeDraft = "JU-${Random.nextInt(1000, 9999)}"
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Random code", modifier = Modifier.size(16.dp))
                    }
                }
                OutlinedTextField(
                    value = codeLabelDraft,
                    onValueChange = { codeLabelDraft = it.take(60) },
                    singleLine = true,
                    label = { Text("Assigned To (Optional Label)") },
                    placeholder = { Text("Partner Manager") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (codeDraft.isNotBlank()) {
                            onCreateAccessCode(codeDraft, codeLabelDraft)
                            codeDraft = ""
                            codeLabelDraft = ""
                        }
                    },
                    enabled = !isBusy && codeDraft.length >= 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_transfer_code_button")
                ) {
                    Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Transfer Code")
                }

                // 3. Active Codes List (Delete revokes immediately)
                if (adminAccessCodes.isNotEmpty()) {
                    Text(
                        text = "Active Codes (${adminAccessCodes.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        adminAccessCodes.forEach { entry ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.code,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (entry.label.isNotBlank()) {
                                            Text(
                                                text = entry.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onDeleteAccessCode(entry.code) },
                                        modifier = Modifier.testTag("delete_code_${entry.code}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete code",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Active Operators using codes
                val partners = adminMembers.filter { it.codeUsed != "MASTER" }
                if (partners.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "Logged Admin Operators (${partners.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        partners.forEach { member ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${member.name} · Code: ${member.codeUsed}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${member.phone} · ${member.email}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(onClick = { onRemoveAdminMember(member.uid) }) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Remove operator",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // 5. Optional Direct Co-Admin Gmail Whitelist
                Text(
                    text = "Direct Co-Admin Gmail Whitelist",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = emailDraft,
                        onValueChange = { emailDraft = it },
                        singleLine = true,
                        label = { Text("Co-Admin Gmail") },
                        placeholder = { Text("partner@gmail.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("co_admin_email_input")
                    )
                    Button(
                        onClick = {
                            if (emailDraft.isNotBlank()) {
                                onAddEmail(emailDraft)
                                emailDraft = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(),
                        modifier = Modifier.testTag("add_co_admin_button")
                    ) {
                        Text("Add")
                    }
                }

                if (adminEmails.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        adminEmails.forEach { entry ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = entry.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { onRemoveEmail(entry.email) }) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Remove co-admin",
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
