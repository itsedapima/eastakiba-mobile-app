package com.nodimobilebanking.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment

/**
 * Branded loading dialog with animated progress indicator and rotating tips.
 * Use for any operation > 1 second (loan apply, OTP, statement download).
 *
 * Usage:
 *   val dlg = LoadingDialog.show(context, "Processing your loan...")
 *   // ... do work ...
 *   dlg.dismiss()
 */
class LoadingDialog private constructor(
    private val dialog: Dialog,
    private val messageView: TextView
) {
    fun setMessage(msg: String) { messageView.text = msg }
    fun dismiss() { try { if (dialog.isShowing) dialog.dismiss() } catch (_: Exception) {} }
    val isShowing: Boolean get() = dialog.isShowing

    companion object {
        fun show(context: Context, message: String = "Please wait..."): LoadingDialog {
            val dlg = Dialog(context)
            dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dlg.setCancelable(false)
            dlg.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#88000000")))

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundColor(Color.WHITE)
                setPadding(80, 70, 80, 70)
                minimumWidth = (context.resources.displayMetrics.widthPixels * 0.65f).toInt()
            }
            val bg = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 24f
                setColor(Color.WHITE)
            }
            container.background = bg

            val progress = ProgressBar(context).apply {
                isIndeterminate = true
                indeterminateTintList = android.content.res.ColorStateList.valueOf(
                    Color.parseColor("#0D2C6D")  // Primary blue
                )
            }
            val msg = TextView(context).apply {
                text = message
                setTextColor(Color.parseColor("#1A1A1A"))
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, 32, 0, 0)
            }
            container.addView(progress, LinearLayout.LayoutParams(120, 120))
            container.addView(msg, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))

            dlg.setContentView(container)
            try { dlg.show() } catch (_: Exception) {}
            return LoadingDialog(dlg, msg)
        }
    }
}

// Convenience extensions
fun Context.showLoading(msg: String = "Please wait..."): LoadingDialog =
    LoadingDialog.show(this, msg)

fun Fragment.showLoading(msg: String = "Please wait..."): LoadingDialog =
    LoadingDialog.show(requireContext(), msg)
