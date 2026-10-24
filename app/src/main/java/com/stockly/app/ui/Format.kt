package com.stockly.app.ui

import java.text.NumberFormat

private val currency: NumberFormat = NumberFormat.getCurrencyInstance()

fun formatPrice(value: Double): String = currency.format(value)

/** Accepts both "12.50" and "12,50". */
fun parsePrice(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }

fun normalizeLink(text: String): String? {
    val t = text.trim()
    if (t.isEmpty()) return null
    return if (t.startsWith("http://") || t.startsWith("https://")) t else "https://$t"
}
