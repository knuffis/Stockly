package com.stockly.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stockly.app.Inventory
import com.stockly.app.Item
import com.stockly.app.SortOrder
import com.stockly.app.StocklyViewModel
import com.stockly.app.sortedFor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ItemsScreen(vm: StocklyViewModel, inventory: Inventory) {
    val context = LocalContext.current
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Item?>(null) }
    var actionsFor by remember { mutableStateOf<Item?>(null) }
    var removing by remember { mutableStateOf<Item?>(null) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    val items = inventory.items.sortedFor(vm.sortOrder)

    fun openLink(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No browser found to open this link", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(inventory.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { vm.close() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        TextButton(onClick = { sortMenuOpen = true }) { Text("Sort: ${vm.sortOrder.label}") }
                        DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                            SortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.label) },
                                    leadingIcon = {
                                        if (order == vm.sortOrder) Icon(Icons.Default.Check, contentDescription = null)
                                    },
                                    onClick = { vm.sortOrder = order; sortMenuOpen = false },
                                )
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add item") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${items.size} items · ${items.sumOf { it.quantity }} units", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Total ${formatPrice(inventory.totalValue)}", style = MaterialTheme.typography.titleSmall)
            }
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No items yet.\nTap \"Add item\" to add one.\nLong-press an item to remove or move it.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .combinedClickable(
                                    onClick = { editing = item },
                                    onLongClick = { actionsFor = item },
                                ),
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${item.quantity} × ${formatPrice(item.unitPrice)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    item.link?.let { url ->
                                        Text(
                                            "Product page",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .combinedClickable(onClick = { openLink(url) }),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(formatPrice(item.totalValue), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
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
            Text(item.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp))
            if (!inCustomOrder) {
                Text(
                    "Switch sorting to \"${SortOrder.CUSTOM.label}\" to move items.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
            }
            HorizontalDivider(Modifier.padding(top = 8.dp))
            if (inCustomOrder && index > 0) {
                ListItem(
                    headlineContent = { Text("Move up") },
                    leadingContent = { Icon(Icons.Default.KeyboardArrowUp, contentDescription = null) },
                    modifier = Modifier.combinedClickable(onClick = { vm.moveItem(item, -1) }),
                )
            }
            if (inCustomOrder && index < inventory.items.lastIndex) {
                ListItem(
                    headlineContent = { Text("Move down") },
                    leadingContent = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) },
                    modifier = Modifier.combinedClickable(onClick = { vm.moveItem(item, 1) }),
                )
            }
            ListItem(
                headlineContent = { Text("Edit") },
                leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                modifier = Modifier.combinedClickable(onClick = { editing = item; actionsFor = null }),
            )
            ListItem(
                headlineContent = { Text("Remove", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.combinedClickable(onClick = { removing = item; actionsFor = null }),
            )
            Spacer(Modifier.padding(bottom = 24.dp))
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
