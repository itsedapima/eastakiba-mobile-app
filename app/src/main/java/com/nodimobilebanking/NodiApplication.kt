package com.nodimobilebanking

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

/**
 * NODi application entry point.
 *
 * Forces LIGHT mode app-wide. The app's color system (white cards, dark text,
 * smoke-gray inputs) is designed for a light surface. Allowing the system's
 * dark mode to apply would invert backgrounds on system-drawn widgets
 * (spinners, dropdowns, dialogs, buttons) and make text/fields invisible.
 *
 * Locking to MODE_NIGHT_NO guarantees a consistent, legible UI on every device
 * regardless of the user's system dark-mode setting.
 */
class NodiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
