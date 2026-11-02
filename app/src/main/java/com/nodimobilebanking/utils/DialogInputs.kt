package com.nodimobilebanking.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout

/**
 * Helpers for building consistently-styled inputs inside dialogs.
 *
 *  • Smoke-gray fill background  (#F1F5F9)
 *  • Pure-black text             (#0F172A)
 *  • Soft-gray hint              (#94A3B8)
 *  • Rounded 12dp corners with subtle stroke
 *  • Proper padding so the input never touches dialog edges
 */
object DialogInputs {

    private const val FILL = "#F1F5F9"
    private const val STROKE = "#E2E8F0"
    private const val TEXT = "#0F172A"
    private const val HINT = "#94A3B8"

    fun input(
        context: Context,
        hintText: String,
        type: Int = InputType.TYPE_CLASS_TEXT,
        centerText: Boolean = false,
        large: Boolean = false,
    ): EditText {
        val et = EditText(context)
        et.hint = hintText
        et.inputType = type
        et.setHintTextColor(Color.parseColor(HINT))
        et.setTextColor(Color.parseColor(TEXT))
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (large) 22f else 15f)
        if (centerText) et.gravity = Gravity.CENTER

        et.background = GradientDrawable().apply {
            cornerRadius = 24f
            setColor(Color.parseColor(FILL))
            setStroke(2, Color.parseColor(STROKE))
        }

        val padH = dp(context, 16)
        val padV = dp(context, 14)
        et.setPadding(padH, padV, padH, padV)
        return et
    }

    fun wrap(context: Context, et: EditText): FrameLayout {
        val container = FrameLayout(context)
        val m = dp(context, 20)
        container.setPadding(m, dp(context, 8), m, 0)
        container.addView(
            et,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        return container
    }

    fun stack(context: Context, vararg inputs: EditText): LinearLayout {
        val ll = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val m = dp(context, 20)
            setPadding(m, dp(context, 8), m, 0)
        }
        inputs.forEachIndexed { i, et ->
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            if (i > 0) lp.topMargin = dp(context, 12)
            ll.addView(et, lp)
        }
        return ll
    }

    private fun dp(context: Context, v: Int): Int =
        (v * context.resources.displayMetrics.density).toInt()
}
