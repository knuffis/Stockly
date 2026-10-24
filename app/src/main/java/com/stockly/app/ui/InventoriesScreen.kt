package com.stockly.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stockly.app.Inventory
import com.stockly.app.StocklyViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InventoriesScreen(vm: StocklyViewModel) {
    var creating by remember { mutableStateOf(false) }
    var actionsFor by remember { mutableStateOf<Inventory?>(null) }
    var renaming by remember { mutableStateOf<Inventory?>(null) }
    var deleting by remember { mutableStateOf<Inventory?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Stockly", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New inventory") },
            )
        },
    ) { padding ->
        if (vm.inventories.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No inventories yet.\nTap \"New inventory\" to create your first one.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(vm.inventories, key = { it.id }) { inv ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { vm.open(inv) },
                                onLongClick = { actionsFor = inv },
                            ),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(inv.name, style = MaterialTheme.typography.titleLarge)
                            val count = inv.items.size
                            Text(
                                "$count ${if (count == 1) "item" else "items"} · ${formatPrice(inv.totalValue)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
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
