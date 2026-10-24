package com.stockly.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stockly.app.ItemDraft
import com.stockly.app.R
import java.util.Locale

private data class ReviewRow(val selected: Boolean, val name: String, val quantity: String, val price: String) {
    val quantityValue get() = quantity.trim().toIntOrNull()?.takeIf { it >= 0 }
    val priceValue get() = parsePrice(price)
    val valid get() = name.isNotBlank() && quantityValue != null && priceValue != null
}

/** Shows what was read off a receipt so it can be checked and fixed before adding. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptReview(
    found: List<ItemDraft>,
    accent: Color,
    onAdd: (List<ItemDraft>) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val rows = remember(found) {
        mutableStateListOf<ReviewRow>().apply {
            found.forEach {
                add(
                    ReviewRow(
                        selected = true,
                        name = it.name,
                        quantity = (it.quantity ?: 1).toString(),
                        price = String.format(Locale.US, "%.2f", it.unitPrice ?: 0.0),
                    ),
                )
            }
        }
    }
    val chosen = rows.filter { it.selected }
    val canAdd = chosen.isNotEmpty() && chosen.all { it.valid }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(if (found.isEmpty()) "Scan receipt" else "Found ${found.size} items") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Cancel") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
            bottomBar = {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier.weight(1f),
                    ) { Text("Scan again") }
                    if (found.isNotEmpty()) {
                        Button(
                            onClick = {
                                onAdd(chosen.map { ItemDraft(it.name.trim(), it.quantityValue, it.priceValue) })
                            },
                            enabled = canAdd,
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                            modifier = Modifier.weight(1f),
                        ) { Text("Add ${chosen.size}") }
                    }
                }
            },
        ) { padding ->
            if (found.isEmpty()) {
                EmptyState(
                    icon = R.drawable.ic_receipt,
                    title = "No items found",
                    message = "Try again with the whole receipt flat, in good light and filling the photo.",
                    modifier = Modifier.padding(padding).padding(top = 48.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            "Check the names and prices, untick anything that isn't an item.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                        )
                    }
                    itemsIndexed(rows) { index, row ->
                        ReviewCard(row, accent) { rows[index] = it }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(row: ReviewRow, accent: Color, onChange: (ReviewRow) -> Unit) {
    val fieldShape = RoundedCornerShape(12.dp)
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.Top) {
            Checkbox(
                checked = row.selected,
                onCheckedChange = { onChange(row.copy(selected = it)) },
                colors = CheckboxDefaults.colors(checkedColor = accent),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = row.name,
                    onValueChange = { onChange(row.copy(name = it)) },
                    label = { Text("Name") },
                    singleLine = true,
                    shape = fieldShape,
                    enabled = row.selected,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = row.quantity,
                        onValueChange = { v -> onChange(row.copy(quantity = v.filter { it.isDigit() })) },
                        label = { Text("Qty") },
                        singleLine = true,
                        shape = fieldShape,
                        enabled = row.selected,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = row.price,
                        onValueChange = { onChange(row.copy(price = it)) },
                        label = { Text("Price each") },
                        singleLine = true,
                        shape = fieldShape,
                        enabled = row.selected,
                        isError = row.selected && row.priceValue == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.4f),
                    )
                }
            }
        }
    }
}
