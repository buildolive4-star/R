package com.example.ui.gallery

import android.Manifest
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
import androidx.compose.foundation.text.selection.SelectionContainer
import com.example.data.firebase.GoogleAuthHelper
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.MyLocation
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.Animated3DMascotOrb
import com.example.ui.components.DateMate3DGlowingButton
import com.example.ui.components.DateMateMascot
import com.example.ui.components.GlowButtonStyle
import com.example.ui.components.PixoraNetworkImage
import com.example.util.LocationAddressFetcher
import com.example.util.ProfileCompletionCalculator
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

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
    onToggleStaffOnline: (Boolean) -> Unit = {},
    authErrorMessage: String? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
    var isFetchingLocation by remember { mutableStateOf(false) }
    var locationStatusHint by remember { mutableStateOf<String?>(null) }

    var staffRole by remember(userAccount?.staffRole) {
        mutableStateOf(userAccount?.staffRole?.ifBlank { "GF" } ?: "GF")
    }
    var staffPrice by remember(userAccount?.staffPrice) {
        mutableStateOf(userAccount?.staffPrice?.ifBlank { "₹499" } ?: "₹499")
    }

    fun performLocationAutoFetch() {
        if (isFetchingLocation) return
        scope.launch {
            isFetchingLocation = true
            locationStatusHint = "Detecting your city & address…"
            val res = LocationAddressFetcher.fetchCurrentAddress(context)
            isFetchingLocation = false
            res.onSuccess { fetched ->
                if (fetched.city.isNotBlank()) city = fetched.city
                if (fetched.fullAddress.isNotBlank()) address = fetched.fullAddress
                locationStatusHint = "Location auto-filled: ${fetched.city.ifBlank { fetched.fullAddress }}"
            }.onFailure { err ->
                locationStatusHint = err.localizedMessage ?: "Could not detect location. Please turn on Location/GPS."
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                performLocationAutoFetch()
            } else {
                locationStatusHint = "Location permission denied. You can still type your City & Address manually."
            }
        }
    )

    fun triggerAutoFetchAddress() {
        if (LocationAddressFetcher.hasLocationPermission(context)) {
            performLocationAutoFetch()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
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
                    Animated3DMascotOrb(
                        mascot = DateMateMascot.WELCOME_GIRL,
                        size = 66.dp,
                        showOrbitSparkles = true,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Welcome to DateMate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Sign in to save your profile, set GF/BF preference, open chats & apply as verified Staff.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                DateMate3DGlowingButton(
                    text = "Continue with Google",
                    onClick = onSignInGoogle,
                    style = GlowButtonStyle.ROSE_PINK,
                    leadingIcon = Icons.AutoMirrored.Rounded.Login,
                    enabled = !isBusy,
                    isLoading = isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_google_signin_button")
                )
                val diagInfo = remember(context) { GoogleAuthHelper.getAuthDiagnosticInfo(context) }
                if (!diagInfo.isSha1Matched || !diagInfo.hasWebClientId) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Build Key Warning: SHA-1 ${diagInfo.runtimeSha1Colon ?: "unknown"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (!authErrorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_auth_error_banner")
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
                    DateMate3DGlowingButton(
                        text = "Avatar",
                        onClick = {
                            avatarPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        style = GlowButtonStyle.GLASS_SOFT,
                        leadingIcon = Icons.Rounded.AddAPhoto,
                        compact = true,
                        modifier = Modifier.weight(1f)
                    )
                    DateMate3DGlowingButton(
                        text = if (selectedGalleryUris.isNotEmpty()) {
                            "Photos (${selectedGalleryUris.size})"
                        } else {
                            "Share Photos"
                        },
                        onClick = {
                            multiPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        style = GlowButtonStyle.ROSE_PINK,
                        leadingIcon = Icons.Rounded.Collections,
                        compact = true,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("profile_gallery_picker")
                    )
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Location & Address",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    DateMate3DGlowingButton(
                        text = if (isFetchingLocation) "Fetching…" else "Auto-Fetch Address",
                        onClick = { triggerAutoFetchAddress() },
                        style = GlowButtonStyle.GLASS_SOFT,
                        leadingIcon = Icons.Rounded.MyLocation,
                        enabled = !isFetchingLocation,
                        isLoading = isFetchingLocation,
                        compact = true,
                        modifier = Modifier.testTag("profile_autofetch_address_button")
                    )
                }
                if (!locationStatusHint.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = locationStatusHint.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_city_input")
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_address_input")
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
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Home Page automatically shows matching GF/BF profiles & nearby city first.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

                DateMate3DGlowingButton(
                    text = if (isBusy) "Saving…" else "Save Profile",
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
                    style = GlowButtonStyle.ROSE_PINK,
                    leadingIcon = Icons.Rounded.Save,
                    enabled = !isBusy,
                    isLoading = isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_save_button")
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // "Join DateMate" Verification & Staff Direct Chat Application Section
    val status = userAccount?.staffStatus ?: StaffStatus.NONE
    var showJoinDateMateForm by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(
            1.2.dp,
            when (status) {
                StaffStatus.APPROVED -> MaterialTheme.colorScheme.primary
                StaffStatus.PENDING -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("become_staff_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showJoinDateMateForm = !showJoinDateMateForm }
            ) {
                Animated3DMascotOrb(
                    mascot = DateMateMascot.SAFETY_MATE,
                    size = 54.dp,
                    showOrbitSparkles = true,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Join DateMate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (status) {
                            StaffStatus.APPROVED -> "Verified Staff Active ✓ · Direct Client Chat Approved by Admin"
                            StaffStatus.PENDING -> "Under Admin Review · Info verification & chat approval pending"
                            StaffStatus.REJECTED -> "Verification declined · Update your info & resubmit"
                            else -> "Tap to submit your info for Admin verification & Staff Direct Chat access"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (status) {
                StaffStatus.APPROVED -> {
                    val isStaffOnline = userAccount?.staffOnline == true
                    val canDirectChat = userAccount?.canDirectChatAsStaff == true
                    Spacer(modifier = Modifier.height(12.dp))
                    // Live Online / Offline Self-Toggle Card for Verified Staff
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isStaffOnline) Color(0xFF16A34A).copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(
                            1.2.dp,
                            if (isStaffOnline) Color(0xFF22C55E).copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("staff_online_status_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (isStaffOnline) Color(0xFF22C55E) else Color(0xFF9CA3AF))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isStaffOnline) "Live Status: ONLINE 🟢" else "Live Status: OFFLINE ⚫",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isStaffOnline) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isStaffOnline) {
                                            "Users can see you in 'Online Now' & at the top of Home!"
                                        } else {
                                            "Turn ON when available so users can see you Online."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Switch(
                                checked = isStaffOnline,
                                onCheckedChange = { checked -> onToggleStaffOnline(checked) },
                                enabled = !isBusy,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF16A34A),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFF6B7280)
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("staff_self_online_switch")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (canDirectChat) {
                                    "Verified Staff Mode & Direct Chat Unlocked ✓"
                                } else {
                                    "Verified Staff Active ✓ · Admin-Managed Chat"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (canDirectChat) {
                                    "You are verified on DateMate as ${userAccount?.staffRole ?: "Companion"} (${userAccount?.staffPrice ?: "₹499"}). Direct Client Chat is ON — open Chats -> Staff Inbox to reply directly!"
                                } else {
                                    "You are verified on DateMate as ${userAccount?.staffRole ?: "Companion"} (${userAccount?.staffPrice ?: "₹499"}). Client chats for your profile are currently handled by DateMate Admin."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
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
                            text = "Status: UNDER ADMIN VERIFICATION. Requested as ${userAccount?.staffRole ?: "GF"} at ${userAccount?.staffPrice ?: "₹499"}. Once Admin verifies your submitted info and approves chat in Admin Studio, your account immediately switches to Staff behavior with Direct Client Chat!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                else -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (!showJoinDateMateForm) {
                        DateMate3DGlowingButton(
                            text = if (firebaseUser == null) "Sign In to Join DateMate" else "Join DateMate · Apply as Verified Staff",
                            onClick = {
                                if (firebaseUser == null) {
                                    onSignInGoogle()
                                } else {
                                    showJoinDateMateForm = true
                                }
                            },
                            style = GlowButtonStyle.ROYAL_GOLD,
                            leadingIcon = Icons.Rounded.AutoAwesome,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("join_datemate_open_button")
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Join DateMate · Staff Verification Info",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Fill your details below. Admin will verify your info and approve Direct Chat access.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    singleLine = true,
                                    label = { Text("Your Full Name *") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = phone,
                                        onValueChange = { phone = it },
                                        singleLine = true,
                                        label = { Text("Mobile Number *") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1.3f)
                                    )
                                    OutlinedTextField(
                                        value = ageText,
                                        onValueChange = { ageText = it.filter { ch -> ch.isDigit() }.take(2) },
                                        singleLine = true,
                                        label = { Text("Age *") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(0.7f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                DateMate3DGlowingButton(
                                    text = if (isFetchingLocation) "Detecting Current Address…" else "Auto-Fetch City & Address",
                                    onClick = { triggerAutoFetchAddress() },
                                    style = GlowButtonStyle.GLASS_SOFT,
                                    leadingIcon = Icons.Rounded.MyLocation,
                                    enabled = !isFetchingLocation,
                                    isLoading = isFetchingLocation,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("join_autofetch_address_button")
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = city,
                                        onValueChange = { city = it },
                                        singleLine = true,
                                        label = { Text("City *") },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = interests,
                                        onValueChange = { interests = it },
                                        singleLine = true,
                                        label = { Text("Specialty Vibe *") },
                                        placeholder = { Text("Coffee, Travel") },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1.2f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = address,
                                    onValueChange = { address = it },
                                    singleLine = true,
                                    label = { Text("Full Address (Private for Admin Verification) *") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = bio,
                                    onValueChange = { bio = it },
                                    minLines = 2,
                                    maxLines = 3,
                                    label = { Text("Companion Bio / Introduction *") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))
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
                                            label = { Text(if (role == "GF") "Companion (GF)" else "Companion (BF)") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .minimumInteractiveComponentSize()
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
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
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = staffPrice,
                                    onValueChange = { staffPrice = it },
                                    singleLine = true,
                                    label = { Text("Session Rate (e.g. ₹499)") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                DateMate3DGlowingButton(
                                    text = "Submit Info for Admin Verification & Chat Approval",
                                    onClick = {
                                        if (firebaseUser == null) {
                                            onSignInGoogle()
                                        } else {
                                            onSaveProfile(
                                                name.ifBlank { firebaseUser.displayName ?: "DateMate Staff" },
                                                selectedAvatarUri,
                                                selectedGalleryUris,
                                                phone.ifBlank { "+91 9876543210" },
                                                ageText.ifBlank { "22" },
                                                address.ifBlank { "Verified City Center" },
                                                city.ifBlank { "Mumbai" },
                                                bio.ifBlank { "Friendly verified DateMate companion for coffee & conversations." },
                                                interests.ifBlank { "Coffee & Chat, Travel Buddy" },
                                                genderPref,
                                            )
                                            onApplyForStaff(staffRole, staffPrice)
                                        }
                                    },
                                    style = GlowButtonStyle.EMERALD_CHAT,
                                    leadingIcon = Icons.Rounded.Verified,
                                    enabled = !isBusy,
                                    isLoading = isBusy,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("apply_become_staff_button")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
