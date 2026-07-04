package com.nailbiter.kiosk_lock_task

import android.content.Context

internal object KioskPreferences {
    private const val PREFS_NAME = "kiosk_lock_task_preferences"
    private const val AUTO_START_KIOSK_KEY = "auto_start_kiosk"
    private const val LAUNCH_ON_BOOT_KEY = "launch_on_boot"

    fun isAutoStartEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(AUTO_START_KIOSK_KEY, false)
    }

    fun setAutoStartEnabled(
        context: Context,
        enabled: Boolean
    ) {
        prefs(context).edit().putBoolean(AUTO_START_KIOSK_KEY, enabled).apply()
    }

    fun isLaunchOnBootEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(LAUNCH_ON_BOOT_KEY, false)
    }

    fun setLaunchOnBootEnabled(
        context: Context,
        enabled: Boolean
    ) {
        prefs(context).edit().putBoolean(LAUNCH_ON_BOOT_KEY, enabled).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
