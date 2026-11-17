package com.example.ui.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.GalleryImage
import com.example.data.remote.ImgBbApiService
import com.example.ui.components.PixoraBlurredImage
import com.example.ui.components.PixoraNetworkImage
import kotlinx.coroutines.launch

private val STUDIO_CATEGORIES = listOf(
    "Date Night",
    "Candid",
    "Selfies",
    "Morning",
    "Outfits",
    "Travel",
    "Exclusive",
)

private enum class StudioFilter(val label: String) {
    ALL("All"),
    FEATURED("Spotlight"),
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdminStudioScreen(
    publishState: AdminPublishState,
    uploadedImages: List<GalleryImage>,
    onUploadPicture: (
        uri: Uri?,
        manualUrl: String,
        title: String,
        description: String,
        category: String,
        tagsRaw: String,
        featured: Boolean,
        isHidden: Boolean,
    ) -> Unit,
    onResetPublishState: () -> Unit,
    onToggleFeatured: (GalleryImage) -> Unit,
    onUpdateMetadata: (
        imageId: String,
        title: String,
        description: String,
        category: String,
        tagsRaw: String,
        featured: Boolean,
        isHidden: Boolean,
    ) -> Unit,
    onRemoveUpload: (String) -> Unit,
    onClearAllUploads: () -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val isImgBbConfigured = remember { ImgBbApiService.isApiKeyConfigured() }

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var manualUrl by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(STUDIO_CATEGORIES.first()) }
    var customCategory by remember { mutableStateOf("") }
    var tagsRaw by remember { mutableStateOf("") }
    var featured by remember { mutableStateOf(true) }
    var isHidden by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }

    var selectedFilter by remember { mutableStateOf(StudioFilter.ALL) }
    var editingImage by remember { mutableStateOf<GalleryImage?>(null) }
    var deletingImage by remember { mutableStateOf<GalleryImage?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedUri = uri
                manualUrl = ""
                onResetPublishState()
            }
        }
    )

    LaunchedEffect(publishState) {
        if (publishState is AdminPublishState.Success) {
            selectedUri = null
            manualUrl = ""
            title = ""
            description = ""
            tagsRaw = ""
            featured = true
            isHidden = false
        }
    }

    val filteredUploads = remember(uploadedImages, selectedFilter) {
        when (selectedFilter) {
            StudioFilter.ALL -> uploadedImages
            StudioFilter.FEATURED -> uploadedImages.filter { it.featured }
        }
    }

    val copiedText = stringResource(R.string.link_copied)

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item(key = "admin_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("admin_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.admin_studio_title),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.admin_studio_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ADMIN",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Quick Upload Composer
            item(key = "admin_upload_composer") {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.admin_upload_section_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.admin_upload_section_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (!isImgBbConfigured) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.imgbb_bridge_missing),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (selectedUri != null) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .testTag("admin_pick_photo_surface")
                        ) {
                            if (selectedUri != null) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(selectedUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = title.ifBlank { "Selected artwork" },
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(10.dp)
                                            .minimumInteractiveComponentSize()
                                            .testTag("admin_change_photo_button")
                                    ) {
                                        Text(text = stringResource(R.string.change_photo_button))
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(38.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("admin_pick_photo_button")
                                    ) {
                                        Text(text = stringResource(R.string.pick_photo_button))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(R.string.upload_category_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            STUDIO_CATEGORIES.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat && customCategory.isBlank(),
                                    onClick = {
                                        selectedCategory = cat
                                        customCategory = ""
                                    },
                                    label = { Text(text = cat) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            singleLine = true,
                            label = { Text(text = stringResource(R.string.admin_title_optional_label)) },
                            placeholder = { Text(text = stringResource(R.string.admin_title_optional_hint)) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_title_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isHidden) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isHidden) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VisibilityOff,
                                        contentDescription = null,
                                        tint = if (isHidden) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.admin_hide_on_switch),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.admin_hide_on_subtitle),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = isHidden,
                                    onCheckedChange = { isHidden = it },
                                    modifier = Modifier.testTag("admin_hide_on_switch")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        TextButton(
                            onClick = { showMoreOptions = !showMoreOptions },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("admin_more_options_toggle")
                        ) {
                            Icon(
                                imageVector = if (showMoreOptions) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(
                                    if (showMoreOptions) R.string.admin_hide_options else R.string.admin_more_options
                                )
                            )
                        }

                        AnimatedVisibility(visible = showMoreOptions) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = manualUrl,
                                    onValueChange = {
                                        manualUrl = it
                                        if (it.isNotBlank()) selectedUri = null
                                    },
                                    singleLine = true,
                                    label = { Text(text = stringResource(R.string.admin_direct_url_label)) },
                                    placeholder = { Text(text = "https://i.ibb.co/...") },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_url_input")
                                )

                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    minLines = 2,
                                    maxLines = 4,
                                    label = { Text(text = stringResource(R.string.admin_description_label)) },
                                    placeholder = { Text(text = stringResource(R.string.admin_description_hint)) },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_description_input")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customCategory,
                                        onValueChange = { customCategory = it },
                                        singleLine = true,
                                        label = { Text(text = stringResource(R.string.admin_custom_category)) },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = tagsRaw,
                                        onValueChange = { tagsRaw = it },
                                        singleLine = true,
                                        label = { Text(text = stringResource(R.string.admin_tags_label)) },
                                        placeholder = { Text(text = "sunset, 4k") },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.admin_featured_switch),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Switch(
                                        checked = featured,
                                        onCheckedChange = { featured = it },
                                        modifier = Modifier.testTag("admin_featured_switch")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        AnimatedVisibility(visible = publishState is AdminPublishState.Error) {
                            val msg = (publishState as? AdminPublishState.Error)?.message.orEmpty()
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = msg, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        AnimatedVisibility(visible = publishState is AdminPublishState.Success) {
                            val success = publishState as? AdminPublishState.Success
                            if (success != null) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = success.message,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                            Text(
                                                text = success.imageUrl,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                clipboard.setText(AnnotatedString(success.imageUrl))
                                                scope.launch { snackbarHostState.showSnackbar(copiedText) }
                                            },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ContentCopy,
                                                contentDescription = copiedText
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val isBusy = publishState is AdminPublishState.UploadingToImgBb

                        Button(
                            onClick = {
                                val effectiveCategory = customCategory.trim().ifBlank { selectedCategory }
                                onUploadPicture(
                                    selectedUri,
                                    manualUrl,
                                    title,
                                    description,
                                    effectiveCategory,
                                    tagsRaw,
                                    featured,
                                    isHidden,
                                )
                            },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("admin_publish_submit_button")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = stringResource(R.string.uploading_to_imgbb))
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = stringResource(R.string.admin_upload_cta))
                            }
                        }
                    }
                }
            }

            // Uploaded Pictures Management
            if (uploadedImages.isNotEmpty()) {
                item(key = "admin_uploads_header") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.admin_device_previews_title, uploadedImages.size),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = stringResource(R.string.admin_device_previews_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = onClearAllUploads,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text(text = stringResource(R.string.clear))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StudioFilter.entries.forEach { filter ->
                                FilterChip(
                                    selected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter },
                                    label = { Text(text = filter.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                )
                            }
                        }
                    }
                }

                items(filteredUploads, key = { "upload_${it.id}" }) { upload ->
                    StudioUploadCard(
                        image = upload,
                        onToggleFeatured = { onToggleFeatured(upload) },
                        onEdit = { editingImage = upload },
                        onCopyLink = {
                            clipboard.setText(AnnotatedString(upload.imageUrl))
                            scope.launch { snackbarHostState.showSnackbar(copiedText) }
                        },
                        onDelete = { deletingImage = upload },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                    )
                }
            }

            item(key = "admin_bottom_spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (editingImage != null) {
        val current = editingImage!!
        AdminEditArtworkSheet(
            image = current,
            onDismiss = { editingImage = null },
            onSave = { newTitle, newDesc, newCat, newTags, newFeat, newHidden ->
                onUpdateMetadata(
                    current.id,
                    newTitle,
                    newDesc,
                    newCat,
                    newTags,
                    newFeat,
                    newHidden,
                )
                editingImage = null
            }
        )
    }

    if (deletingImage != null) {
        val target = deletingImage!!
        AlertDialog(
            onDismissRequest = { deletingImage = null },
            title = { Text(text = stringResource(R.string.admin_delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.admin_delete_confirm_body, target.title)) },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveUpload(target.id)
                        deletingImage = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(text = stringResource(R.string.admin_delete_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingImage = null }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun StudioUploadCard(
    image: GalleryImage,
    onToggleFeatured: () -> Unit,
    onEdit: () -> Unit,
    onCopyLink: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.testTag("admin_artwork_card_${image.id}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (image.isDeletedBlurred) {
                PixoraBlurredImage(
                    imageUrl = image.displayThumbnailUrl,
                    contentDescription = image.title,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            } else {
                PixoraNetworkImage(
                    imageUrl = image.displayThumbnailUrl,
                    contentDescription = image.title,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = image.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusLabel = when {
                        image.isDeletedBlurred -> "BLURRED"
                        image.isHidden -> "SECRET · 20s"
                        else -> "LIVE"
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = when {
                            image.isDeletedBlurred -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            image.isHidden -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
                        }
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                image.isDeletedBlurred -> MaterialTheme.colorScheme.secondary
                                image.isHidden -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.tertiary
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = image.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = onToggleFeatured,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = if (image.featured) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = "Toggle spotlight",
                    tint = if (image.featured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onCopyLink,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = stringResource(R.string.copy_link)
                )
            }
            IconButton(
                onClick = onEdit,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit artwork"
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.admin_delete_button),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminEditArtworkSheet(
    image: GalleryImage,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        category: String,
        tagsRaw: String,
        featured: Boolean,
        isHidden: Boolean,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(image.title) }
    var description by remember { mutableStateOf(image.description) }
    var category by remember { mutableStateOf(image.category) }
    var tagsRaw by remember { mutableStateOf(image.tags.joinToString(", ")) }
    var featured by remember { mutableStateOf(image.featured) }
    var isHidden by remember { mutableStateOf(image.isHidden) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.admin_edit_title),
                style = MaterialTheme.typography.headlineSmall
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                label = { Text(text = stringResource(R.string.upload_title_label)) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                minLines = 2,
                maxLines = 4,
                label = { Text(text = stringResource(R.string.admin_description_label)) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                singleLine = true,
                label = { Text(text = stringResource(R.string.upload_category_label)) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = tagsRaw,
                onValueChange = { tagsRaw = it },
                singleLine = true,
                label = { Text(text = stringResource(R.string.admin_tags_label)) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.admin_hide_on_switch))
                Switch(checked = isHidden, onCheckedChange = { isHidden = it })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.admin_featured_switch))
                Switch(checked = featured, onCheckedChange = { featured = it })
            }
            Button(
                onClick = {
                    onSave(
                        title,
                        description,
                        category,
                        tagsRaw,
                        featured,
                        isHidden,
                    )
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .minimumInteractiveComponentSize()
            ) {
                Text(text = stringResource(R.string.admin_save_changes))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
