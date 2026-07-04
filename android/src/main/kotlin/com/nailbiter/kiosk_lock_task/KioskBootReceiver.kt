package com.nailbiter.kiosk_lock_task

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class KioskBootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val action = intent.action ?: return
        if (
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        if (!KioskPreferences.isLaunchOnBootEnabled(context)) return

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(launchIntent)
    }
}
