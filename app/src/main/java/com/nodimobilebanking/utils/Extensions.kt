package com.nodimobilebanking.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

fun View.visible() { visibility = View.VISIBLE }
fun View.gone()    { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

/**
 * Center-screen feedback notification — a styled overlay that appears
 * in the middle of the screen so users can't miss it.
 * Uses colored backgrounds (green=success, red=error, blue=info).
 */
fun Context.toast(msg: String) {
    val activity = when (this) {
        is android.app.Activity -> this
        is android.content.ContextWrapper -> baseContext as? android.app.Activity
        else -> null
    }
    val rootView = activity?.findViewById<android.widget.FrameLayout>(android.R.id.content)
    if (rootView != null) {
        // Remove any previous toast overlay still animating
        rootView.findViewWithTag<View>("center_toast")?.let { rootView.removeView(it) }

        val lowerMsg = msg.lowercase()
        val (icon, bgColor) = when {
            lowerMsg.contains("error") || lowerMsg.contains("incorrect")
                || lowerMsg.contains("invalid") || lowerMsg.contains("failed")
                || lowerMsg.contains("denied") || lowerMsg.contains("expired")
                || lowerMsg.contains("locked") || lowerMsg.contains("not found")
                || lowerMsg.contains("insufficient") || lowerMsg.contains("exceed")
                || lowerMsg.contains("don't match") || lowerMsg.contains("not activated")
                    -> "✕" to android.graphics.Color.parseColor("#DC2626")
            lowerMsg.contains("success") || lowerMsg.contains("welcome")
                || lowerMsg.contains("saved") || lowerMsg.contains("sent")
                || lowerMsg.contains("verified") || lowerMsg.contains("reset")
                || lowerMsg.contains("activated") || lowerMsg.contains("completed")
                    -> "✓" to android.graphics.Color.parseColor("#1E7C2E")
            else -> "ⓘ" to android.graphics.Color.parseColor("#0D2C6D")
        }

        // Build the overlay view
        val tv = android.widget.TextView(activity).apply {
            text = "  $icon  $msg"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 4
            setPadding(48, 32, 48, 32)
            gravity = android.view.Gravity.CENTER
            tag = "center_toast"

            // Rounded background
            val gd = android.graphics.drawable.GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 24f
                alpha = 240
            }
            background = gd
            elevation = 12f
        }

        // Center it in the screen
        val params = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.CENTER
            marginStart = 48; marginEnd = 48
        }

        rootView.addView(tv, params)

        // Animate: fade in → hold → fade out → remove
        tv.alpha = 0f
        tv.animate()
            .alpha(1f)
            .setDuration(250)
            .withEndAction {
                tv.postDelayed({
                    tv.animate().alpha(0f).setDuration(400).withEndAction {
                        try { rootView.removeView(tv) } catch (_: Exception) {}
                    }.start()
                }, 2500)
            }
            .start()
    } else {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

fun View.snack(msg: String, duration: Int = Snackbar.LENGTH_LONG) =
    Snackbar.make(this, msg, duration).show()

fun Double.formatCurrency(): String {
    val df = DecimalFormat("#,##0.00")
    return "KES ${df.format(this)}"
}

fun Double.formatAmount(): String {
    val df = DecimalFormat("#,##0.00")
    return df.format(this)
}

fun String.formatDate(): String {
    return try {
        val inputFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        )
        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        for (fmt in inputFormats) {
            try { return outputFormat.format(fmt.parse(this)!!) } catch (_: Exception) {}
        }
        this
    } catch (e: Exception) {
        this
    }
}

fun String.toDisplayAmount(): String {
    return try {
        val d = this.toDouble()
        d.formatCurrency()
    } catch (e: Exception) {
        "KES 0.00"
    }
}

// Fragment-friendly toast shortcut — call toast() directly inside any Fragment
fun androidx.fragment.app.Fragment.toast(msg: String) =
    requireContext().toast(msg)
