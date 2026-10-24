package com.stockly.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stockly.app.Inventory
import com.stockly.app.StocklyViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InventoriesScreen(vm: StocklyViewModel) {
    var creating by remember { mutableStateOf(false) }
    var actionsFor by remember { mutableStateOf<Inventory?>(null) }
    var renaming by remember { mutableStateOf<Inventory?>(null) }
    var deleting by remember { mutableStateOf<Inventory?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New inventory") },
                containerColor = Coral,
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 104.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Header(vm.inventories) }
            if (vm.inventories.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "📦",
                        title = "Nothing here yet",
                        message = "Tap \"New inventory\" to start your first list.",
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
            items(vm.inventories, key = { it.id }) { inv ->
                InventoryCard(
                    inv,
                    onClick = { vm.open(inv) },
                    onLongClick = { actionsFor = inv },
                    modifier = Modifier.padding(horizontal = 16.dp).animateItem(),
                )
            }
        }
    }

    if (creating) {
        NameDialog(
            title = "New inventory",
            initial = "",
            confirmLabel = "Create",
            onConfirm = { vm.addInventory(it); creating = false },
            onDismiss = { creating = false },
        )
    }
    actionsFor?.let { inv ->
        AlertDialog(
            onDismissRequest = { actionsFor = null },
            title = { Text(inv.name) },
            text = { Text("What do you want to do with this inventory?") },
            confirmButton = {
                TextButton(onClick = { renaming = inv; actionsFor = null }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = inv; actionsFor = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }
    renaming?.let { inv ->
        NameDialog(
            title = "Rename inventory",
            initial = inv.name,
            confirmLabel = "Save",
            onConfirm = { vm.renameInventory(inv, it); renaming = null },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { inv ->
        ConfirmDialog(
            title = "Delete \"${inv.name}\"?",
            message = "This removes the inventory and all ${inv.items.size} items in it. This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = { vm.deleteInventory(inv); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun Header(inventories: List<Inventory>) {
    val totalItems = inventories.sumOf { it.items.size }
    val totalValue = inventories.sumOf { it.totalValue }
    Box(
        Modifier
            .fillMaxWidth()
            .background(HeroGradient, RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 28.dp),
    ) {
        Column {
            Text("Stockly", style = MaterialTheme.typography.displaySmall, color = Color.White)
            Text(
                "Everything you own, neatly counted ✨",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBubble("${inventories.size}", if (inventories.size == 1) "inventory" else "inventories")
                StatBubble("$totalItems", if (totalItems == 1) "item" else "items")
                StatBubble(formatPrice(totalValue), "total value")
            }
        }
    }
}

@Composable
private fun StatBubble(value: String, label: String) {
    Column(
        Modifier
            .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InventoryCard(inv: Inventory, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = accentFor(inv.id)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            LetterBadge(inv.name, accent, size = 56.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(inv.name, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val count = inv.items.size
                    Pill("$count ${if (count == 1) "item" else "items"}", accent.copy(alpha = 0.15f), accent)
                    Pill(formatPrice(inv.totalValue), MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = accent)
        }
    }
}
