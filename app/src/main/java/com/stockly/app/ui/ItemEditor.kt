package com.stockly.app.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.stockly.app.ItemDraft
import com.stockly.app.Photos
import com.stockly.app.R
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/** Full-screen form for adding or editing an item, with an optional photo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditor(
    title: String,
    initial: ItemDraft,
    accent: Color,
    confirmLabel: String,
    onSave: (ItemDraft) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initial.name) }
    var quantity by remember { mutableStateOf(initial.quantity?.toString() ?: "") }
    var price by remember { mutableStateOf(initial.unitPrice?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var link by remember { mutableStateOf(initial.link ?: "") }
    var imagePath by remember { mutableStateOf(initial.imagePath) }
    var loadingPhoto by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    fun importPhoto(uri: Uri) {
        loadingPhoto = true
        scope.launch {
            val path = Photos.importFromUri(context, uri)
            if (path != null) imagePath = path
            else Toast.makeText(context, "Couldn't load that photo", Toast.LENGTH_SHORT).show()
            loadingPhoto = false
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(::importPhoto)
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let(::importPhoto)
    }
    val openCamera = {
        val uri = Photos.newCameraUri(context)
        cameraUri = uri
        takePhoto.launch(uri)
    }
    val openGallery = {
        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val quantityValue = quantity.trim().toIntOrNull()?.takeIf { it >= 0 }
    val priceValue = parsePrice(price)
    val valid = name.isNotBlank() && quantityValue != null && priceValue != null && !loadingPhoto
    val fieldShape = RoundedCornerShape(14.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Cancel") }
                    },
                    actions = {
                        Button(
                            onClick = {
                                onSave(ItemDraft(name, quantityValue, priceValue, normalizeLink(link), imagePath))
                            },
                            enabled = valid,
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                            modifier = Modifier.padding(end = 12.dp),
                        ) { Text(confirmLabel) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PhotoBox(
                    imagePath = imagePath,
                    loading = loadingPhoto,
                    accent = accent,
                    onCamera = openCamera,
                    onGallery = openGallery,
                    onRemove = { imagePath = null },
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    shape = fieldShape,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { v -> quantity = v.filter { it.isDigit() } },
                        label = { Text("How many") },
                        singleLine = true,
                        shape = fieldShape,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Price per unit") },
                        singleLine = true,
                        shape = fieldShape,
                        isError = price.isNotBlank() && priceValue == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("Product link (optional)") },
                    placeholder = { Text("https://…") },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_link), contentDescription = null) },
                    singleLine = true,
                    shape = fieldShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (quantityValue != null && priceValue != null) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(accent.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Total value", style = MaterialTheme.typography.bodyMedium)
                        Text(formatPrice(quantityValue * priceValue), style = MaterialTheme.typography.titleMedium, color = accent)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PhotoBox(
    imagePath: String?,
    loading: Boolean,
    accent: Color,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onRemove: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        if (imagePath != null) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = "Item photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Row(
                Modifier.align(Alignment.BottomEnd).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OverlayButton(R.drawable.ic_camera, "Take a new photo", onCamera)
                OverlayButton(R.drawable.ic_photo, "Choose another photo", onGallery)
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(40.dp).background(Color.Black.copy(alpha = 0.45f), CircleShape),
                ) { Icon(Icons.Default.Delete, contentDescription = "Remove photo", tint = Color.White) }
            }
        } else if (!loading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconTile(R.drawable.ic_photo, accent, size = 56.dp)
                Spacer(Modifier.height(10.dp))
                Text("Add a photo", style = MaterialTheme.typography.titleSmall)
                Text(
                    "So you know exactly which one it is",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = onCamera) {
                        Icon(painterResource(R.drawable.ic_camera), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Camera")
                    }
                    FilledTonalButton(onClick = onGallery) {
                        Icon(painterResource(R.drawable.ic_photo), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Gallery")
                    }
                }
            }
        }
        if (loading) CircularProgressIndicator(color = accent)
    }
}

@Composable
private fun OverlayButton(icon: Int, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp).background(Color.Black.copy(alpha = 0.45f), CircleShape),
    ) { Icon(painterResource(icon), contentDescription = description, tint = Color.White, modifier = Modifier.size(20.dp)) }
}
