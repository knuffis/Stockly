package com.stockly.app

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class ProductInfo(val name: String?, val price: Double?, val imageUrl: String?)

/** Reads a product page and pulls out its name, price and main image from the usual meta tags. */
object LinkImport {
    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36"

    private const val MAX_HTML_CHARS = 3_000_000

    suspend fun fetch(url: String): ProductInfo = withContext(Dispatchers.IO) {
        var target = URL(url)
        var conn: HttpURLConnection
        var redirects = 0
        while (true) {
            conn = (target.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "text/html,application/xhtml+xml")
                setRequestProperty("Accept-Language", "de-CH,de;q=0.9,en;q=0.8")
            }
            val code = conn.responseCode
            val location = conn.getHeaderField("Location")
            if (code in 300..399 && location != null && redirects < 5) {
                target = URL(target, location)
                redirects++
                conn.disconnect()
                continue
            }
            break
        }
        val html = conn.inputStream.bufferedReader().use { reader ->
            val sb = StringBuilder()
            val buf = CharArray(16 * 1024)
            while (sb.length < MAX_HTML_CHARS) {
                val n = reader.read(buf)
                if (n < 0) break
                sb.append(buf, 0, n)
            }
            sb.toString()
        }
        parse(html, target)
    }

    fun parse(html: String, base: URL): ProductInfo {
        val name = meta(html, "og:title")
            ?: meta(html, "twitter:title")
            ?: jsonLd(html, "name")
            ?: Regex("<title[^>]*>([^<]+)</title>", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
        val priceText = meta(html, "product:price:amount")
            ?: meta(html, "og:price:amount")
            ?: meta(html, "price")
            ?: jsonLd(html, "price")
            ?: jsonLd(html, "lowPrice")
        val image = meta(html, "og:image")
            ?: meta(html, "og:image:secure_url")
            ?: meta(html, "twitter:image")
            ?: jsonLd(html, "image")
        return ProductInfo(
            name = name?.let(::cleanText)?.let(::trimShopSuffix)?.takeIf { it.isNotBlank() },
            price = priceText?.let(::parseAmount),
            imageUrl = image?.let { runCatching { URL(base, cleanText(it)).toString() }.getOrNull() },
        )
    }

    private fun meta(html: String, key: String): String? {
        val tag = Regex(
            "<meta[^>]+(?:property|name|itemprop)\\s*=\\s*[\"']${Regex.escape(key)}[\"'][^>]*>",
            RegexOption.IGNORE_CASE,
        ).find(html)?.value ?: return null
        return Regex("content\\s*=\\s*\"([^\"]*)\"|content\\s*=\\s*'([^']*)'", RegexOption.IGNORE_CASE)
            .find(tag)?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }
    }

    /** First "key": value in any JSON-LD block; values may be a string, a number or a list of strings. */
    private fun jsonLd(html: String, key: String): String? {
        val blocks = Regex(
            "<script[^>]+application/ld\\+json[^>]*>(.*?)</script>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).findAll(html).map { it.groupValues[1] }
        val pattern = Regex("\"$key\"\\s*:\\s*(?:\\[\\s*)?(?:\"([^\"]+)\"|([0-9][0-9.,]*))")
        for (block in blocks) {
            val m = pattern.find(block) ?: continue
            return m.groupValues[1].ifEmpty { m.groupValues[2] }
        }
        return null
    }

    private fun cleanText(s: String): String =
        Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY).toString().replace(Regex("\\s+"), " ").trim()

    private fun trimShopSuffix(s: String): String {
        val cut = listOf(" | ", " – ", " - ").mapNotNull { sep -> s.indexOf(sep).takeIf { it > 3 } }.minOrNull()
        return if (cut != null && s.length > 40) s.substring(0, cut).trim() else s
    }

    /** Handles "1299.00", "1'299.00", "1.299,00" and "12,50". */
    fun parseAmount(text: String): Double? {
        var t = text.replace(Regex("[^0-9.,']"), "").replace("'", "")
        if (t.isEmpty()) return null
        val lastDot = t.lastIndexOf('.')
        val lastComma = t.lastIndexOf(',')
        t = when {
            lastDot >= 0 && lastComma >= 0 ->
                if (lastComma > lastDot) t.replace(".", "").replace(',', '.') else t.replace(",", "")
            lastComma >= 0 -> t.replace(',', '.')
            else -> t
        }
        return t.toDoubleOrNull()
    }
}
