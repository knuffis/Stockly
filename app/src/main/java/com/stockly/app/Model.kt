package com.stockly.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Item(
    val id: Long,
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val link: String?,
    val createdAt: Long,
) {
    val totalValue: Double get() = quantity * unitPrice
}

data class Inventory(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val items: List<Item> = emptyList(),
) {
    val totalValue: Double get() = items.sumOf { it.totalValue }
}

enum class SortOrder(val label: String) {
    CUSTOM("My order"),
    NAME_ASC("Name A–Z"),
    NAME_DESC("Name Z–A"),
    DATE_NEWEST("Newest first"),
    DATE_OLDEST("Oldest first"),
    PRICE_HIGH("Price high to low"),
    PRICE_LOW("Price low to high"),
}

fun List<Item>.sortedFor(order: SortOrder): List<Item> = when (order) {
    SortOrder.CUSTOM -> this
    SortOrder.NAME_ASC -> sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    SortOrder.NAME_DESC -> sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name })
    SortOrder.DATE_NEWEST -> sortedByDescending { it.createdAt }
    SortOrder.DATE_OLDEST -> sortedBy { it.createdAt }
    SortOrder.PRICE_HIGH -> sortedByDescending { it.unitPrice }
    SortOrder.PRICE_LOW -> sortedBy { it.unitPrice }
}

/** Saves all inventories as one JSON file in the app's private storage. */
class InventoryStore(context: Context) {
    private val file = File(context.filesDir, "inventories.json")

    fun load(): List<Inventory> {
        if (!file.exists()) return emptyList()
        return try {
            val array = JSONArray(file.readText())
            (0 until array.length()).map { inventoryFromJson(array.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(inventories: List<Inventory>) {
        val array = JSONArray()
        inventories.forEach { array.put(inventoryToJson(it)) }
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(array.toString())
        tmp.renameTo(file)
    }

    private fun inventoryToJson(inv: Inventory) = JSONObject().apply {
        put("id", inv.id)
        put("name", inv.name)
        put("createdAt", inv.createdAt)
        put("items", JSONArray().apply { inv.items.forEach { put(itemToJson(it)) } })
    }

    private fun itemToJson(item: Item) = JSONObject().apply {
        put("id", item.id)
        put("name", item.name)
        put("quantity", item.quantity)
        put("unitPrice", item.unitPrice)
        put("link", item.link ?: JSONObject.NULL)
        put("createdAt", item.createdAt)
    }

    private fun inventoryFromJson(o: JSONObject): Inventory {
        val items = o.optJSONArray("items") ?: JSONArray()
        return Inventory(
            id = o.getLong("id"),
            name = o.getString("name"),
            createdAt = o.optLong("createdAt"),
            items = (0 until items.length()).map { itemFromJson(items.getJSONObject(it)) },
        )
    }

    private fun itemFromJson(o: JSONObject) = Item(
        id = o.getLong("id"),
        name = o.getString("name"),
        quantity = o.optInt("quantity"),
        unitPrice = o.optDouble("unitPrice", 0.0),
        link = if (o.isNull("link")) null else o.optString("link").ifBlank { null },
        createdAt = o.optLong("createdAt"),
    )
}
