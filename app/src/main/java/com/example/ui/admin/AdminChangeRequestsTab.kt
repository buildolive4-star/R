package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminChangeRequest
import com.example.data.model.AdminChangeRequestStatus
import com.example.data.model.AdminChangeRequestType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminChangeRequestsTab(
    isSuperAdmin: Boolean,
    isBusy: Boolean,
    changeRequests: List<AdminChangeRequest>,
    onSubmitRequest: (requestType: String, targetId: String, targetTitle: String, details: String, onDone: () -> Unit) -> Unit,
    onReviewRequest: (requestId: String, approve: Boolean, mainAdminNote: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedType by remember { mutableStateOf(AdminChangeRequestType.PROFILE_EDIT) }
    var targetTitle by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }

    val pendingCount = remember(changeRequests) {
        changeRequests.count { it.status == AdminChangeRequestStatus.PENDING }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_change_requests_tab")
    ) {
        if (!isSuperAdmin) {
            item(key = "coadmin_compose_request_card") {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.EditNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Send Change Request to Main Admin",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Co-Admin can only reply to Client Chats directly. Request profile edits, price updates, staff access, or filter changes here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "Change Category",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AdminChangeRequestType.ALL_OPTIONS.forEach { (key, label) ->
                                FilterChip(
                                    selected = selectedType == key,
                                    onClick = { selectedType = key },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                    modifier = Modifier.testTag("request_type_chip_$key")
                                )
                            }
                        }

                        OutlinedTextField(
                            value = targetTitle,
                            onValueChange = { targetTitle = it.take(100) },
                            singleLine = true,
                            label = { Text("Profile / Feature Name") },
                            placeholder = { Text("e.g. Ananya (23) · Update Dinner Price") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("coadmin_request_title_input")
                        )

                        OutlinedTextField(
                            value = details,
                            onValueChange = { details = it.take(1000) },
                            minLines = 3,
                            maxLines = 6,
                            label = { Text("Exact Change Requested") },
                            placeholder = { Text("Describe clearly what Main Admin should add, edit, approve, or change...") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("coadmin_request_details_input")
                        )

                        Button(
                            onClick = {
                                onSubmitRequest(selectedType, "", targetTitle, details) {
                                    targetTitle = ""
                                    details = ""
                                }
                            },
                            enabled = !isBusy && targetTitle.trim().isNotBlank() && details.trim().length >= 4,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("coadmin_submit_request_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Request to Main Admin", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item(key = "requests_list_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isSuperAdmin) {
                            "Co-Admin Change Requests (${changeRequests.size})"
                        } else {
                            "My Submitted Requests (${changeRequests.size})"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isSuperAdmin) {
                            "$pendingCount pending review · Approve or Reject Co-Admin change requests below."
                        } else {
                            "Live status updates from Main Admin appear right here."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (changeRequests.isEmpty()) {
            item(key = "requests_empty_state") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isSuperAdmin) "No Co-Admin requests yet" else "No requests submitted yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isSuperAdmin) {
                                "Whenever a Co-Admin requests a profile edit, new listing, price update, or staff permission change, it appears here for your approval."
                            } else {
                                "Use the form above or tap 'Request Main Admin' on any profile/staff card to request changes."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(changeRequests, key = { it.id }) { req ->
                AdminChangeRequestCard(
                    request = req,
                    isSuperAdmin = isSuperAdmin,
                    isBusy = isBusy,
                    onApprove = { note -> onReviewRequest(req.id, true, note) },
                    onReject = { note -> onReviewRequest(req.id, false, note) },
                )
            }
        }
    }
}

@Composable
private fun AdminChangeRequestCard(
    request: AdminChangeRequest,
    isSuperAdmin: Boolean,
    isBusy: Boolean,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
) {
    var noteText by remember(request.id, request.mainAdminNote) { mutableStateOf(request.mainAdminNote) }
    val statusColor = when (request.status) {
        AdminChangeRequestStatus.APPROVED -> Color(0xFF16A34A)
        AdminChangeRequestStatus.REJECTED -> Color(0xFFDC2626)
        else -> Color(0xFFD97706)
    }
    val statusLabel = when (request.status) {
        AdminChangeRequestStatus.APPROVED -> "APPROVED ✓"
        AdminChangeRequestStatus.REJECTED -> "REJECTED"
        else -> "PENDING"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.2.dp, statusColor.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("change_request_card_${request.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = AdminChangeRequestType.labelFor(request.requestType),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = statusColor.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (request.status) {
                                AdminChangeRequestStatus.APPROVED -> Icons.Rounded.AssignmentTurnedIn
                                AdminChangeRequestStatus.REJECTED -> Icons.Rounded.Close
                                else -> Icons.Rounded.Schedule
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor
                        )
                    }
                }
            }

            Text(
                text = request.targetTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )

            if (isSuperAdmin) {
                Text(
                    text = "Requested by: ${request.requesterName.ifBlank { "Co-Admin" }} (${request.requesterEmail})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = request.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(10.dp)
                )
            }

            if (request.mainAdminNote.isNotBlank() && !isSuperAdmin) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Main Admin Note: ${request.mainAdminNote}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (isSuperAdmin) {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it.take(500) },
                    singleLine = true,
                    label = { Text("Main Admin Note (Optional)") },
                    placeholder = { Text("e.g. Updated profile price & verified ✓") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onApprove(noteText) },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF16A34A),
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .minimumInteractiveComponentSize()
                            .testTag("approve_change_req_${request.id}")
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Approve ✓", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { onReject(noteText) },
                        enabled = !isBusy,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .minimumInteractiveComponentSize()
                            .testTag("reject_change_req_${request.id}")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickChangeRequestDialog(
    initialType: String,
    targetId: String,
    initialTitle: String,
    initialDetails: String = "",
    isBusy: Boolean,
    onSubmit: (requestType: String, targetId: String, targetTitle: String, details: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var details by remember(initialDetails) { mutableStateOf(initialDetails) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.EditNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Request Main Admin",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Co-Admin cannot edit directly. Send this request to Main Admin (${AdminChangeRequestType.labelFor(initialType)}):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(100) },
                    singleLine = true,
                    label = { Text("Subject / Profile") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it.take(1000) },
                    minLines = 3,
                    maxLines = 5,
                    label = { Text("Change Details") },
                    placeholder = { Text("Write what needs to be changed or approved...") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(initialType, targetId, title, details) },
                enabled = !isBusy && title.trim().isNotBlank() && details.trim().length >= 4,
                modifier = Modifier.testTag("quick_request_submit_button")
            ) {
                Text("Send Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
