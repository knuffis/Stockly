package com.stockly.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stockly.app.Inventory
import com.stockly.app.Item
import com.stockly.app.SortOrder
import com.stockly.app.StocklyViewModel
import com.stockly.app.sortedFor

private val SortEmoji = mapOf(
    SortOrder.CUSTOM to "✋",
    SortOrder.NAME_ASC to "🔤",
    SortOrder.NAME_DESC to "🔤",
    SortOrder.DATE_NEWEST to "🆕",
    SortOrder.DATE_OLDEST to "🕰️",
    SortOrder.PRICE_HIGH to "💰",
    SortOrder.PRICE_LOW to "🪙",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ItemsScreen(vm: StocklyViewModel, inventory: Inventory) {
    val context = LocalContext.current
    val accent = accentFor(inventory.id)
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Item?>(null) }
    var actionsFor by remember { mutableStateOf<Item?>(null) }
    var removing by remember { mutableStateOf<Item?>(null) }

    val items = inventory.items.sortedFor(vm.sortOrder)

    fun openLink(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No browser found to open this link", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add item") },
                containerColor = accent,
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 104.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Header(inventory, accent, onBack = { vm.close() })
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                ) {
                    items(SortOrder.entries) { order ->
                        FilterChip(
                            selected = vm.sortOrder == order,
                            onClick = { vm.sortOrder = order },
                            label = { Text("${SortEmoji[order]} ${order.label}") },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent,
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }
            }
            if (items.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🧺",
                        title = "This one's empty",
                        message = "Tap \"Add item\" to put something in.\nLong-press an item later to move or remove it.",
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
            items(items, key = { it.id }) { item ->
                ItemCard(
                    item = item,
                    accent = accent,
                    onClick = { editing = item },
                    onLongClick = { actionsFor = item },
                    onOpenLink = { item.link?.let(::openLink) },
                    modifier = Modifier.padding(horizontal = 16.dp).animateItem(),
                )
            }
        }
    }

    if (adding) {
        ItemDialog(
            item = null,
            onConfirm = { name, qty, price, link -> vm.addItem(name, qty, price, link); adding = false },
            onDismiss = { adding = false },
        )
    }
    editing?.let { item ->
        ItemDialog(
            item = item,
            onConfirm = { name, qty, price, link -> vm.editItem(item, name, qty, price, link); editing = null },
            onDismiss = { editing = null },
        )
    }
    actionsFor?.let { item ->
        val inCustomOrder = vm.sortOrder == SortOrder.CUSTOM
        val index = inventory.items.indexOfFirst { it.id == item.id }
        ModalBottomSheet(onDismissRequest = { actionsFor = null }) {
            Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                LetterBadge(item.name, accent, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Text(item.name, style = MaterialTheme.typography.titleLarge)
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
                    leadingContent = { SheetIcon(Icons.Default.KeyboardArrowUp, accent) },
                    colors = sheetItemColors,
                    modifier = Modifier.clickable { vm.moveItem(item, -1) },
                )
            }
            if (inCustomOrder && index < inventory.items.lastIndex) {
                ListItem(
                    headlineContent = { Text("Move down") },
                    leadingContent = { SheetIcon(Icons.Default.KeyboardArrowDown, accent) },
                    colors = sheetItemColors,
                    modifier = Modifier.clickable { vm.moveItem(item, 1) },
                )
            }
            ListItem(
                headlineContent = { Text("Edit") },
                leadingContent = { SheetIcon(Icons.Default.Edit, Sky) },
                colors = sheetItemColors,
                modifier = Modifier.clickable { editing = item; actionsFor = null },
            )
            ListItem(
                headlineContent = { Text("Remove", color = MaterialTheme.colorScheme.error) },
                leadingContent = { SheetIcon(Icons.Default.Delete, MaterialTheme.colorScheme.error) },
                colors = sheetItemColors,
                modifier = Modifier.clickable { removing = item; actionsFor = null },
            )
            Spacer(Modifier.height(32.dp))
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
private fun SheetIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Box(
        Modifier.size(40.dp).background(tint.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = tint) }
}

@Composable
private fun Header(inventory: Inventory, accent: Color, onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(accentGradient(accent), RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .statusBarsPadding()
            .padding(start = 8.dp, end = 24.dp, top = 4.dp, bottom = 24.dp),
    ) {
        Column {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(Modifier.padding(start = 16.dp)) {
                Text(
                    inventory.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
                val count = inventory.items.size
                val units = inventory.items.sumOf { it.quantity }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("$count ${if (count == 1) "item" else "items"}", Color.White.copy(alpha = 0.22f), Color.White)
                    Pill("$units ${if (units == 1) "unit" else "units"}", Color.White.copy(alpha = 0.22f), Color.White)
                    Pill("Worth ${formatPrice(inventory.totalValue)}", Color.White, accent)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemCard(
    item: Item,
    accent: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
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
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            LetterBadge(item.name, accent)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${formatPrice(item.unitPrice)} each",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.link != null) {
                    Text(
                        "🔗 Product page",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenLink)
                            .padding(vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Pill("× ${item.quantity}", accent.copy(alpha = 0.15f), accent)
                Spacer(Modifier.height(6.dp))
                Text(formatPrice(item.totalValue), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
