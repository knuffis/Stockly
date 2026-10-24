package com.stockly.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stockly.app.Inventory
import com.stockly.app.R
import com.stockly.app.StocklyViewModel

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
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = padding.calculateBottomPadding() + 104.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(top = 12.dp, bottom = 8.dp)) {
                    Text(
                        "Welcome back",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("Stockly", style = MaterialTheme.typography.headlineLarge)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { SummaryCard(vm.inventories) }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Inventories",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            if (vm.inventories.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        icon = R.drawable.ic_box,
                        title = "No inventories yet",
                        message = "Create one for your pantry, your tools, your shop, anything you want to keep count of.",
                    )
                }
            }
            items(vm.inventories, key = { it.id }) { inv ->
                InventoryTile(
                    inv,
                    onClick = { vm.open(inv) },
                    onLongClick = { actionsFor = inv },
                    modifier = Modifier.animateItem(),
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
private fun SummaryCard(inventories: List<Inventory>) {
    val totalItems = inventories.sumOf { it.items.size }
    val totalUnits = inventories.sumOf { inv -> inv.items.sumOf { it.quantity } }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(HeroGradient)
            .padding(24.dp),
    ) {
        Text("Total value", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.7f))
        Spacer(Modifier.height(4.dp))
        Text(formatPrice(inventories.sumOf { it.totalValue }), style = MaterialTheme.typography.displaySmall, color = Color.White)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Stat("${inventories.size}", if (inventories.size == 1) "Inventory" else "Inventories")
            Stat("$totalItems", if (totalItems == 1) "Item" else "Items")
            Stat("$totalUnits", if (totalUnits == 1) "Unit" else "Units")
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InventoryTile(inv: Inventory, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = accentFor(inv.id)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp)) {
            Thumb(inv.name, null, accent, size = 44.dp)
            Spacer(Modifier.height(20.dp))
            Text(
                inv.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val count = inv.items.size
            Text(
                "$count ${if (count == 1) "item" else "items"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Pill(formatPrice(inv.totalValue), accent.copy(alpha = 0.12f), accent)
        }
    }
}
