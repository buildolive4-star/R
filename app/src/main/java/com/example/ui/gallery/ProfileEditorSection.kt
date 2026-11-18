package com.example.ui.gallery

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.StaffStatus
import com.example.data.model.UserAccount
import com.example.ui.components.PixoraNetworkImage
import com.example.util.ProfileCompletionCalculator
import com.google.firebase.auth.FirebaseUser

private val PREFERENCE_ROLES = listOf("Looking for GF", "Looking for BF", "Open to Both")
private val STAFF_ROLES = listOf("GF", "BF")
private val STAFF_PRICE_PRESETS = listOf("₹299", "₹499", "₹799", "₹1,499")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileEditorSection(
    firebaseUser: FirebaseUser?,
    userAccount: UserAccount?,
    isBusy: Boolean,
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
) {
    val context = LocalContext.current
    var name by remember(userAccount?.uid, userAccount?.name, firebaseUser?.displayName) {
        mutableStateOf(userAccount?.name?.ifBlank { firebaseUser?.displayName ?: "" } ?: (firebaseUser?.displayName ?: ""))
    }
    var phone by remember(userAccount?.uid, userAccount?.phone) {
        mutableStateOf(userAccount?.phone.orEmpty())
    }
    var ageText by remember(userAccount?.uid, userAccount?.age) {
        mutableStateOf(userAccount?.age?.takeIf { it > 0 }?.toString().orEmpty())
    }
    var city by remember(userAccount?.uid, userAccount?.city) {
        mutableStateOf(userAccount?.city.orEmpty())
    }
    var address by remember(userAccount?.uid, userAccount?.address) {
        mutableStateOf(userAccount?.address.orEmpty())
    }
    var bio by remember(userAccount?.uid, userAccount?.bio) {
        mutableStateOf(userAccount?.bio.orEmpty())
    }
    var interests by remember(userAccount?.uid, userAccount?.interests) {
        mutableStateOf(userAccount?.interests.orEmpty())
    }
    var genderPref by remember(userAccount?.uid, userAccount?.genderPreference) {
        mutableStateOf(userAccount?.genderPreference?.ifBlank { PREFERENCE_ROLES.first() } ?: PREFERENCE_ROLES.first())
    }

    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var selectedGalleryUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    var staffRole by remember(userAccount?.staffRole) {
        mutableStateOf(userAccount?.staffRole?.ifBlank { "GF" } ?: "GF")
    }
    var staffPrice by remember(userAccount?.staffPrice) {
        mutableStateOf(userAccount?.staffPrice?.ifBlank { "₹499" } ?: "₹499")
    }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) selectedAvatarUri = uri }
    )
    val multiPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6),
        onResult = { uris -> if (uris.isNotEmpty()) selectedGalleryUris = uris }
    )

    val effectiveAvatar = when {
        selectedAvatarUri != null -> selectedAvatarUri.toString()
        !userAccount?.photoUrl.isNullOrBlank() -> userAccount?.photoUrl.orEmpty()
        else -> firebaseUser?.photoUrl?.toString().orEmpty()
    }

    val liveCompletion = remember(
        name, effectiveAvatar, phone, ageText, address, city, bio, interests, genderPref
    ) {
        ProfileCompletionCalculator.calculate(
            name = name,
            photoUrl = effectiveAvatar,
            phone = phone,
            age = ageText.toIntOrNull() ?: 0,
            address = address,
            city = city,
            bio = bio,
            interests = interests,
            genderPreference = genderPref,
        )
    }

    // Google Account & Profile Header Card
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(
            1.dp,
            if (userAccount?.isApprovedStaff == true) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_account_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (firebaseUser == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sign in to JustUs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Save your profile, share photos, open chats & apply as Staff.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onSignInGoogle,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("profile_google_signin_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Continue with Google")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable {
                                avatarPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("profile_avatar_picker")
                    ) {
                        if (selectedAvatarUri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(selectedAvatarUri).crossfade(true).build(),
                                contentDescription = "Profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (effectiveAvatar.isNotBlank()) {
                            PixoraNetworkImage(
                                imageUrl = effectiveAvatar,
                                contentDescription = "Profile photo",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.AddAPhoto,
                                    contentDescription = "Add profile photo",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = name.ifBlank { firebaseUser.displayName ?: "JustUs Member" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (userAccount?.isApprovedStaff == true) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = "Verified Staff",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = firebaseUser.email.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(
                        onClick = onSignOut,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Sign out",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Completion Progress Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profile Completion",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${liveCompletion.percent}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (liveCompletion.percent == 100) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                        modifier = Modifier.testTag("profile_completion_percent")
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { liveCompletion.percent / 100f },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                )
                if (liveCompletion.missingFields.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Missing for 100%: ${liveCompletion.missingFields.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Photo gallery picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            avatarPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Avatar", maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = {
                            multiPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .minimumInteractiveComponentSize()
                            .testTag("profile_gallery_picker")
                    ) {
                        Icon(Icons.Rounded.Collections, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedGalleryUris.isNotEmpty()) {
                                "Photos (${selectedGalleryUris.size})"
                            } else {
                                "Share Photos"
                            },
                            maxLines = 1
                        )
                    }
                }

                val savedPhotos = userAccount?.galleryUrls.orEmpty()
                if (selectedGalleryUris.isNotEmpty() || savedPhotos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(selectedGalleryUris) { uri ->
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(uri).crossfade(true).build(),
                                contentDescription = "Selected photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        }
                        items(savedPhotos) { url ->
                            PixoraNetworkImage(
                                imageUrl = url,
                                contentDescription = "Uploaded photo",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Form fields
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Full Name *") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        singleLine = true,
                        label = { Text("Mobile No. *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("profile_phone_input")
                    )
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { ageText = it.filter { ch -> ch.isDigit() }.take(2) },
                        singleLine = true,
                        label = { Text("Age *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(0.75f)
                            .testTag("profile_age_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        singleLine = true,
                        label = { Text("City *") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = interests,
                        onValueChange = { interests = it },
                        singleLine = true,
                        label = { Text("Vibe / Interests *") },
                        placeholder = { Text("Coffee, Night Drive") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.3f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    singleLine = true,
                    label = { Text("Full Address *") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    minLines = 2,
                    maxLines = 4,
                    label = { Text("About You (Bio) *") },
                    placeholder = { Text("Write a short intro about yourself…") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Preference",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PREFERENCE_ROLES.forEach { option ->
                        FilterChip(
                            selected = genderPref == option,
                            onClick = { genderPref = option },
                            label = { Text(option) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        onSaveProfile(
                            name,
                            selectedAvatarUri,
                            selectedGalleryUris,
                            phone,
                            ageText,
                            address,
                            city,
                            bio,
                            interests,
                            genderPref,
                        )
                        selectedAvatarUri = null
                        selectedGalleryUris = emptyList()
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("profile_save_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving…")
                    } else {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Profile")
                    }
                }
            }
        }
    }

    if (firebaseUser != null) {
        Spacer(modifier = Modifier.height(14.dp))

        // Become Companion / Staff (GF / BF) Application Card
        val savedPercent = userAccount?.completionPercent ?: 0
        val status = userAccount?.staffStatus ?: StaffStatus.NONE

        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(
                1.dp,
                when (status) {
                    StaffStatus.APPROVED -> MaterialTheme.colorScheme.primary
                    StaffStatus.PENDING -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.outlineVariant
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("become_staff_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (status) {
                                    StaffStatus.APPROVED -> Icons.Rounded.Verified
                                    StaffStatus.PENDING -> Icons.Rounded.HourglassTop
                                    else -> Icons.Rounded.AutoAwesome
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Become Companion / Staff (GF / BF)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (status) {
                                StaffStatus.APPROVED -> "Verified Staff Active · Staff Theme & Inbox unlocked"
                                StaffStatus.PENDING -> "Under Review · Admin is verifying your profile"
                                StaffStatus.REJECTED -> "Previous request declined · Update profile & reapply"
                                else -> "Reach 100% profile completion to apply"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                when (status) {
                    StaffStatus.APPROVED -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "You are live on JustUs as verified ${userAccount?.staffRole ?: "Companion"} (${userAccount?.staffPrice ?: "₹499"}). Open Chats -> Staff Inbox to reply to clients. Admin monitors all staff chats & external contact details are blocked.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    StaffStatus.PENDING -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Status: UNDER REVIEW. Requested as ${userAccount?.staffRole ?: "GF"} at ${userAccount?.staffPrice ?: "₹499"}. As soon as Admin verifies you, your entire app switches to Verified Staff Theme and unlocks Staff Chat Inbox.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    else -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Select Companion Role & Session Rate",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            STAFF_ROLES.forEach { role ->
                                FilterChip(
                                    selected = staffRole == role,
                                    onClick = { staffRole = role },
                                    label = { Text(if (role == "GF") "Girlfriend (GF)" else "Boyfriend (BF)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            STAFF_PRICE_PRESETS.forEach { preset ->
                                FilterChip(
                                    selected = staffPrice == preset,
                                    onClick = { staffPrice = preset },
                                    label = { Text(preset) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = staffPrice,
                            onValueChange = { staffPrice = it },
                            singleLine = true,
                            label = { Text("Your Rate (e.g. ₹499)") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val canApply = savedPercent == 100 && !isBusy
                        Button(
                            onClick = { onApplyForStaff(staffRole, staffPrice) },
                            enabled = canApply,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("apply_become_staff_button")
                        ) {
                            Text(
                                text = if (savedPercent == 100) {
                                    "Submit Staff Verification Request"
                                } else {
                                    "Save 100% Profile First ($savedPercent% saved)"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
