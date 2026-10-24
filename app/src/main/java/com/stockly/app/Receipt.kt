package com.stockly.app

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** One line read off a receipt, as text positioned on the photo. */
data class OcrLine(val text: String, val top: Int, val bottom: Int, val left: Int)

/** Reads receipts on the phone (no internet needed) and guesses which lines are items. */
object Receipt {
    suspend fun scan(context: Context, uri: Uri): List<ItemDraft> {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val text = try {
            suspendCancellableCoroutine { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
        } finally {
            recognizer.close()
        }
        val lines = text.textBlocks.flatMap { block ->
            block.lines.mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                OcrLine(line.text, box.top, box.bottom, box.left)
            }
        }
        return parse(lines)
    }

    private val ignoreWords = listOf(
        "total", "summe", "subtotal", "zwischensumme", "gesamt", "mwst", "mwst.", "vat", "tax", "tva", "iva",
        "bar", "cash", "change", "rückgeld", "ruckgeld", "retour", "karte", "card", "visa", "mastercard",
        "maestro", "twint", "postfinance", "rabatt", "discount", "aktion", "saldo", "betrag", "zahlung", "payment",
        "trinkgeld", "tip", "kassenbon", "quittung", "beleg", "datum", "date", "uhr", "tel", "www",
        "cumulus", "supercard", "punkte", "bezahlt", "paid", "netto", "brutto", "chf", "eur",
    )

    private val priceAtEnd = Regex("(-?\\d{1,5}[.,]\\d{2})(?:\\s+[A-Za-z0-9*]{1,2})?\\s*$")
    private val qtyTimesPrice = Regex("(\\d{1,3})\\s*[xX×*]\\s*(\\d{1,5}[.,]\\d{2})")
    private val leadingQty = Regex("^(\\d{1,3})\\s*[xX×*]\\s+")

    fun parse(lines: List<OcrLine>): List<ItemDraft> {
        val rows = groupIntoRows(lines)
        val result = mutableListOf<ItemDraft>()
        var pendingName: String? = null

        for (row in rows) {
            val price = priceAtEnd.find(row)
            if (price == null) {
                // A name on its own line; the quantity and price may follow on the next one.
                pendingName = row.takeIf { letters(it) >= 3 && !isIgnored(it) }
                continue
            }
            val total = LinkImport.parseAmount(price.groupValues[1]) ?: continue
            var name = row.substring(0, price.range.first).trim()
            var quantity = 1
            var unitPrice = total

            qtyTimesPrice.find(name)?.let { m ->
                quantity = m.groupValues[1].toInt()
                unitPrice = LinkImport.parseAmount(m.groupValues[2]) ?: unitPrice
                name = name.removeRange(m.range).trim()
            } ?: leadingQty.find(name)?.let { m ->
                quantity = m.groupValues[1].toInt()
                unitPrice = total / quantity.coerceAtLeast(1)
                name = name.removeRange(m.range).trim()
            }
            name = name.trim(' ', '.', '-', '*', ':')

            if (letters(name) < 2) {
                when {
                    pendingName != null && quantity >= 1 -> name = pendingName
                    quantity > 1 && result.isNotEmpty() -> {
                        // "2 x 1.65  3.30" under the item it belongs to.
                        val last = result.removeAt(result.lastIndex)
                        result += last.copy(quantity = quantity, unitPrice = unitPrice)
                        pendingName = null
                        continue
                    }
                    else -> { pendingName = null; continue }
                }
            }
            pendingName = null
            if (total <= 0 || isIgnored(name) || quantity <= 0) continue
            result += ItemDraft(name = prettify(name), quantity = quantity, unitPrice = unitPrice)
        }
        return result
    }

    /** Joins text pieces that sit on the same printed line, left to right. */
    private fun groupIntoRows(lines: List<OcrLine>): List<String> {
        val sorted = lines.sortedBy { (it.top + it.bottom) / 2 }
        val rows = mutableListOf<MutableList<OcrLine>>()
        for (line in sorted) {
            val center = (line.top + line.bottom) / 2
            val height = (line.bottom - line.top).coerceAtLeast(1)
            val row = rows.lastOrNull()
            val rowCenter = row?.let { r -> r.sumOf { (it.top + it.bottom) / 2 } / r.size }
            if (row != null && rowCenter != null && kotlin.math.abs(center - rowCenter) < height * 0.6) {
                row += line
            } else {
                rows += mutableListOf(line)
            }
        }
        return rows.map { r -> r.sortedBy { it.left }.joinToString(" ") { it.text.trim() } }
    }

    private fun letters(s: String) = s.count { it.isLetter() }

    private fun isIgnored(name: String): Boolean {
        val words = name.lowercase().split(Regex("[^\\p{L}.]+")).filter { it.isNotEmpty() }
        return words.any { it in ignoreWords }
    }

    /** "MILCH UHT 1L" -> "Milch Uht 1L"; leaves mixed-case names alone. */
    private fun prettify(name: String): String {
        if (name.any { it.isLowerCase() }) return name
        return name.lowercase().split(' ').joinToString(" ") { w ->
            if (w.any { it.isDigit() }) w.uppercase() else w.replaceFirstChar { it.uppercase() }
        }
    }
}
