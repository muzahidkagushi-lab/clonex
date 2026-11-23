package com.example.util

import com.example.domain.model.CloneItem
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object Formatters {

    val badgePaletteHex: List<Long> = listOf(
        0xFFF59E0B, // Warm Amber (Brand accent)
        0xFF4F46E5, // Electric Indigo (Brand primary)
        0xFF10B981, // Emerald
        0xFFEC4899, // Rose Pink
        0xFF06B6D4, // Cyan
        0xFFEF4444, // Coral Red
        0xFF8B5CF6, // Violet
        0xFF14B8A6  // Teal
    )

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceAtMost(4)
        val unit = "KMGT"[exp - 1]
        val value = bytes / 1024.0.pow(exp.toDouble())
        return String.format(Locale.US, "%.1f %sB", value, unit)
    }

    fun formatDate(timestamp: Long, isBengali: Boolean): String {
        val locale = if (isBengali) Locale.forLanguageTag("bn") else Locale.getDefault()
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
            .format(Date(timestamp))
    }

    fun formatRelativeTime(timestamp: Long, isBengali: Boolean): String {
        val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
        val minutes = diff / 60_000L
        val hours = diff / 3_600_000L
        val days = diff / 86_400_000L
        return when {
            minutes < 1 -> if (isBengali) "এইমাত্র" else "Just now"
            minutes < 60 -> if (isBengali) "${toBengaliDigits(minutes)} মিনিট আগে" else "${minutes}m ago"
            hours < 24 -> if (isBengali) "${toBengaliDigits(hours)} ঘণ্টা আগে" else "${hours}h ago"
            else -> if (isBengali) "${toBengaliDigits(days)} দিন আগে" else "${days}d ago"
        }
    }

    fun toBengaliDigits(number: Number): String {
        val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        return number.toString().map { ch ->
            if (ch in '0'..'9') bnDigits[ch - '0'] else ch
        }.joinToString("")
    }

    /**
     * Generates a unique clone name using the user's configured format (default "{name} {n}").
     * Guarantees length <= 30 and no collision with existing clones.
     */
    fun generateUniqueCloneName(
        formatTemplate: String,
        appName: String,
        existingClones: List<CloneItem>
    ): Pair<String, String> {
        val existingNames = existingClones.map { it.cloneName.lowercase() }.toSet()
        val sameAppCount = existingClones.count { it.cloneName.startsWith(appName, ignoreCase = true) }
        var n = (sameAppCount + 2).coerceAtLeast(2)

        while (true) {
            val formatted = formatTemplate
                .replace("{name}", appName)
                .replace("{n}", n.toString())
                .trim()
                .take(30)
            if (formatted.isNotEmpty() && formatted.lowercase() !in existingNames) {
                return formatted to n.toString()
            }
            n++
        }
    }
}
