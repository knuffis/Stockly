package com.stockly.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

class StocklyViewModel(app: Application) : AndroidViewModel(app) {
    private val store = InventoryStore(app)

    var inventories by mutableStateOf(store.load())
        private set

    var openInventoryId by mutableStateOf<Long?>(null)
        private set

    var sortOrder by mutableStateOf(SortOrder.CUSTOM)

    val openInventory: Inventory?
        get() = inventories.firstOrNull { it.id == openInventoryId }

    private var lastId = 0L

    init {
        Photos.cleanup(app, inventories.flatMap { inv -> inv.items.mapNotNull { it.imagePath } }.toSet())
    }

    /** Unique, increasing ids even when two things are created in the same millisecond. */
    private fun newId(): Long {
        val now = System.currentTimeMillis()
        lastId = if (now > lastId) now else lastId + 1
        return lastId
    }

    private fun update(newList: List<Inventory>) {
        inventories = newList
        store.save(newList)
    }

    private fun updateOpen(transform: (List<Item>) -> List<Item>) {
        val id = openInventoryId ?: return
        update(inventories.map { if (it.id == id) it.copy(items = transform(it.items)) else it })
    }

    fun open(inventory: Inventory) {
        openInventoryId = inventory.id
        sortOrder = SortOrder.CUSTOM
    }

    fun close() {
        openInventoryId = null
    }

    fun addInventory(name: String) {
        update(inventories + Inventory(id = newId(), name = name.trim(), createdAt = System.currentTimeMillis()))
    }

    fun renameInventory(inventory: Inventory, name: String) {
        update(inventories.map { if (it.id == inventory.id) it.copy(name = name.trim()) else it })
    }

    fun deleteInventory(inventory: Inventory) {
        inventory.items.forEach { Photos.delete(it.imagePath) }
        update(inventories.filterNot { it.id == inventory.id })
    }

    private fun ItemDraft.toItem() = Item(
        id = newId(),
        name = name.trim(),
        quantity = quantity ?: 1,
        unitPrice = unitPrice ?: 0.0,
        link = link,
        createdAt = System.currentTimeMillis(),
        imagePath = imagePath,
    )

    fun addItem(draft: ItemDraft) {
        val item = draft.toItem()
        updateOpen { it + item }
    }

    fun addItems(drafts: List<ItemDraft>) {
        val items = drafts.map { it.toItem() }
        updateOpen { it + items }
    }

    fun editItem(item: Item, draft: ItemDraft) {
        if (item.imagePath != draft.imagePath) Photos.delete(item.imagePath)
        updateOpen { items ->
            items.map {
                if (it.id == item.id) {
                    it.copy(
                        name = draft.name.trim(),
                        quantity = draft.quantity ?: it.quantity,
                        unitPrice = draft.unitPrice ?: it.unitPrice,
                        link = draft.link,
                        imagePath = draft.imagePath,
                    )
                } else it
            }
        }
    }

    fun removeItem(item: Item) {
        Photos.delete(item.imagePath)
        updateOpen { items -> items.filterNot { it.id == item.id } }
    }

    /** Moves an item within the custom order; [offset] is -1 for up, +1 for down. */
    fun moveItem(item: Item, offset: Int) {
        updateOpen { items ->
            val from = items.indexOfFirst { it.id == item.id }
            val to = from + offset
            if (from < 0 || to !in items.indices) items
            else items.toMutableList().apply { add(to, removeAt(from)) }
        }
    }
}
