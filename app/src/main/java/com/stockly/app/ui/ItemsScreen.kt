package com.stockly.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.stockly.app.Inventory
import com.stockly.app.Item
import com.stockly.app.ItemDraft
import com.stockly.app.LinkImport
import com.stockly.app.Photos
import com.stockly.app.R
import com.stockly.app.Receipt
import com.stockly.app.SortOrder
import com.stockly.app.StocklyViewModel
import com.stockly.app.sortedFor
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ItemsScreen(vm: StocklyViewModel, inventory: Inventory) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accent = accentFor(inventory.id)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var addMenuOpen by remember { mutableStateOf(false) }
    var newDraft by remember { mutableStateOf<ItemDraft?>(null) }
    var editing by remember { mutableStateOf<Item?>(null) }
    var actionsFor by remember { mutableStateOf<Item?>(null) }
    var removing by remember { mutableStateOf<Item?>(null) }
    var viewingPhoto by remember { mutableStateOf<Item?>(null) }
    var linkDialogOpen by remember { mutableStateOf(false) }
    var busyMessage by remember { mutableStateOf<String?>(null) }
    var receiptResult by remember { mutableStateOf<List<ItemDraft>?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val items = inventory.items.sortedFor(vm.sortOrder)

    fun openLink(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No browser found to open this link", Toast.LENGTH_SHORT).show()
        }
    }

    fun scanReceipt(uri: Uri) {
        busyMessage = "Reading your receipt…"
        scope.launch {
            receiptResult = try {
                Receipt.scan(context, uri)
            } catch (e: Exception) {
                emptyList()
            }
            busyMessage = null
        }
    }

    fun importLink(url: String) {
        linkDialogOpen = false
        busyMessage = "Looking up the product…"
        scope.launch {
            val info = runCatching { LinkImport.fetch(url) }.getOrNull()
            val photo = info?.imageUrl?.let { Photos.download(context, it) }
            if (info == null || (info.name == null && info.price == null)) {
                Toast.makeText(context, "Couldn't read that page, fill in the rest yourself", Toast.LENGTH_LONG).show()
            }
            newDraft = ItemDraft(
                name = info?.name ?: "",
                quantity = 1,
                unitPrice = info?.price,
                link = url,
                imagePath = photo,
            )
            busyMessage = null
        }
    }

    val receiptCamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let(::scanReceipt)
    }
    val receiptGallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(::scanReceipt)
    }
    val photographReceipt = {
        val uri = Photos.newCameraUri(context)
        cameraUri = uri
        receiptCamera.launch(uri)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(inventory.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { vm.close() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { addMenuOpen = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add item") },
                containerColor = accent,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 104.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Summary(inventory, accent) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    items(SortOrder.entries) { order ->
                        FilterChip(
                            selected = vm.sortOrder == order,
                            onClick = { vm.sortOrder = order },
                            label = { Text(order.label) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                                selectedLabelColor = MaterialTheme.colorScheme.surface,
                            ),
                            border = null,
                        )
                    }
                }
            }
            if (items.isEmpty()) {
                item {
                    EmptyState(
                        icon = R.drawable.ic_box,
                        title = "Nothing in here yet",
                        message = "Add items by hand, from a product link, or by scanning a receipt. Long-press an item later to move or remove it.",
                    )
                }
            }
            items(items, key = { it.id }) { item ->
                ItemRow(
                    item = item,
                    accent = accent,
                    onClick = { editing = item },
                    onLongClick = { actionsFor = item },
                    onPhotoClick = { viewingPhoto = item },
                    onOpenLink = { item.link?.let(::openLink) },
                    modifier = Modifier.padding(horizontal = 20.dp).animateItem(),
                )
            }
        }
    }

    if (addMenuOpen) {
        ModalBottomSheet(onDismissRequest = { addMenuOpen = false }, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
            Text("Add item", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(8.dp))
            AddOption(R.drawable.ic_edit, accent, "Type it in", "Name, amount, price and a photo") {
                addMenuOpen = false
                newDraft = ItemDraft()
            }
            AddOption(R.drawable.ic_link, Azure, "From a link", "Paste a product page, we fill in the details") {
                addMenuOpen = false
                linkDialogOpen = true
            }
            AddOption(R.drawable.ic_receipt, Teal, "Scan a receipt", "Take a photo, every line becomes an item") {
                addMenuOpen = false
                photographReceipt()
            }
            AddOption(R.drawable.ic_photo, Amber, "Receipt from gallery", "Use a receipt photo you already have") {
                addMenuOpen = false
                receiptGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    if (linkDialogOpen) {
        LinkDialog(accent = accent, onFetch = ::importLink, onDismiss = { linkDialogOpen = false })
    }

    busyMessage?.let { message ->
        Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
            Card(shape = MaterialTheme.shapes.large) {
                Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = accent, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(18.dp))
                    Text(message, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }

    receiptResult?.let { found ->
        ReceiptReview(
            found = found,
            accent = accent,
            onAdd = { vm.addItems(it); receiptResult = null },
            onRetry = { receiptResult = null; photographReceipt() },
            onDismiss = { receiptResult = null },
        )
    }

    newDraft?.let { draft ->
        ItemEditor(
            title = "New item",
            initial = draft,
            accent = accent,
            confirmLabel = "Add",
            onSave = { vm.addItem(it); newDraft = null },
            onDismiss = { newDraft = null },
        )
    }
    editing?.let { item ->
        ItemEditor(
            title = "Edit item",
            initial = ItemDraft(item.name, item.quantity, item.unitPrice, item.link, item.imagePath),
            accent = accent,
            confirmLabel = "Save",
            onSave = { vm.editItem(item, it); editing = null },
            onDismiss = { editing = null },
        )
    }
    viewingPhoto?.let { item ->
        PhotoViewer(item, onDismiss = { viewingPhoto = null })
    }
    actionsFor?.let { item ->
        val inCustomOrder = vm.sortOrder == SortOrder.CUSTOM
        val index = inventory.items.indexOfFirst { it.id == item.id }
        ModalBottomSheet(onDismissRequest = { actionsFor = null }, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
            Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Thumb(item.name, item.imagePath, accent, size = 44.dp)
                Spacer(Modifier.width(14.dp))
                Text(item.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!inCustomOrder) {
                Text(
                    "Switch sorting to \"${SortOrder.CUSTOM.label}\" to move items.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            val sheetItemColors = ListItemDefaults.colors(containerColor = Color.Transparent)
            if (inCustomOrder && index > 0) {
                ListItem(
                    headlineContent = { Text("Move up") },
                    leadingContent = { Icon(Icons.Default.KeyboardArrowUp, contentDescription = null) },
                    colors = sheetItemColors,
                    modifier = Modifier.clickable { vm.moveItem(item, -1) },
                )
            }
            if (inCustomOrder && index < inventory.items.lastIndex) {
                ListItem(
                    headlineContent = { Text("Move down") },
                    leadingContent = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) },
                    colors = sheetItemColors,
                    modifier = Modifier.clickable { vm.moveItem(item, 1) },
                )
            }
            ListItem(
                headlineContent = { Text("Edit") },
                leadingContent = { Icon(painterResource(R.drawable.ic_edit), contentDescription = null) },
                colors = sheetItemColors,
                modifier = Modifier.clickable { editing = item; actionsFor = null },
            )
            ListItem(
                headlineContent = { Text("Remove", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                colors = sheetItemColors,
                modifier = Modifier.clickable { removing = item; actionsFor = null },
            )
            Spacer(Modifier.height(28.dp))
        }
    }
    removing?.let { item ->
        ConfirmDialog(
            title = "Remove \"${item.name}\"?",
            message = "This item will be removed from ${inventory.name}.",
            confirmLabel = "Remove",
            onConfirm = { vm.removeItem(item); removing = null },
            onDismiss = { removing = null },
        )
    }
}

@Composable
private fun Summary(inventory: Inventory, accent: Color) {
    val count = inventory.items.size
    val units = inventory.items.sumOf { it.quantity }
    Row(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SummaryStat("Items", "$count", MaterialTheme.colorScheme.onSurface)
        SummaryStat("Units", "$units", MaterialTheme.colorScheme.onSurface)
        SummaryStat("Value", formatPrice(inventory.totalValue), accent, alignEnd = true)
    }
}

@Composable
private fun SummaryStat(label: String, value: String, color: Color, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, color = color)
    }
}

@Composable
private fun AddOption(icon: Int, tint: Color, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(subtitle) },
        leadingContent = { IconTile(icon, tint, size = 44.dp) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp),
    )
}

@Composable
private fun LinkDialog(accent: Color, onFetch: (String) -> Unit, onDismiss: () -> Unit) {
    var url by remember { mutableStateOf("") }
    val normalized = normalizeLink(url)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { IconTile(R.drawable.ic_link, Azure, size = 44.dp) },
        title = { Text("Add from a link") },
        text = {
            Column {
                Text(
                    "Paste a link to the product page. Stockly reads the name, price and picture for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    placeholder = { Text("https://…") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { normalized?.let(onFetch) }, enabled = normalized != null) {
                Text("Fetch details", color = if (normalized != null) accent else Color.Unspecified)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PhotoViewer(item: Item, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)).clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = item.imagePath?.let(::File),
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().padding(16.dp).clip(MaterialTheme.shapes.large),
            )
            Text(
                item.name,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 12.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemRow(
    item: Item,
    accent: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onOpenLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                if (item.imagePath != null) {
                    Modifier.clip(RoundedCornerShape(17.dp)).clickable(onClick = onPhotoClick)
                } else Modifier,
            ) {
                Thumb(item.name, item.imagePath, accent, size = 60.dp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${formatPrice(item.unitPrice)} each",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.link != null) {
                    Row(
                        Modifier
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onOpenLink)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_link),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Product page", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Pill("× ${item.quantity}", accent.copy(alpha = 0.12f), accent)
                Spacer(Modifier.height(8.dp))
                Text(formatPrice(item.totalValue), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}
