package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Build
import com.example.MainActivity
import com.example.domain.model.CloneItem

/**
 * Manages pinning and disabling Android home-screen shortcuts for individual clones.
 */
object ShortcutHelper {

    const val EXTRA_CLONE_ID = "extra_launch_clone_id"

    fun pinCloneShortcut(
        context: Context,
        clone: CloneItem,
        appIcon: Drawable?
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return false
        if (!shortcutManager.isRequestPinShortcutSupported) return false

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_CLONE_ID, clone.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val badgedBitmap = renderBadgedBitmap(
            drawable = appIcon,
            badgeLabel = clone.badgeLabel,
            badgeColor = clone.badgeColorHex.toInt()
        )

        val shortcutInfo = ShortcutInfo.Builder(context, shortcutId(clone.id))
            .setShortLabel(clone.cloneName.take(18))
            .setLongLabel(clone.cloneName.take(30))
            .setIcon(Icon.createWithAdaptiveBitmap(badgedBitmap))
            .setIntent(launchIntent)
            .build()

        return shortcutManager.requestPinShortcut(shortcutInfo, null)
    }

    fun disableCloneShortcut(context: Context, cloneId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
        runCatching {
            shortcutManager.disableShortcuts(listOf(shortcutId(cloneId)))
        }
    }

    private fun shortcutId(cloneId: String): String = "clonex_shortcut_$cloneId"

    private fun renderBadgedBitmap(
        drawable: Drawable?,
        badgeLabel: String,
        badgeColor: Int
    ): Bitmap {
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        if (drawable != null) {
            val pad = 28
            drawable.setBounds(pad, pad, size - pad, size - pad)
            drawable.draw(canvas)
        }

        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeColor
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        val rect = RectF(size * 0.50f, size * 0.58f, size * 0.88f, size * 0.88f)
        canvas.drawRoundRect(rect, 24f, 24f, pillPaint)
        canvas.drawRoundRect(rect, 24f, 24f, borderPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val textY = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(badgeLabel.take(4), rect.centerX(), textY, textPaint)
        return bitmap
    }
}
