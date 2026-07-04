package com.nailbiter.kiosk_lock_task

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.view.WindowManager
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** KioskLockTaskPlugin */
class KioskLockTaskPlugin :
    FlutterPlugin,
    ActivityAware,
    MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var applicationContext: Context
    private var activity: Activity? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "kiosk_lock_task")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(
        call: MethodCall,
        result: Result
    ) {
        when (call.method) {
            "enable" -> runForResult(result) {
                setAutoStartEnabled(true)
                enterKioskMode()
                isKioskEnabled()
            }

            "disable" -> runForResult(result) {
                setAutoStartEnabled(false)
                exitKioskMode()
                isKioskEnabled()
            }

            "isEnabled" -> result.success(isKioskEnabled())

            "isDeviceOwner" -> result.success(isDeviceOwner())

            "setAutoStartEnabled" -> runForResult(result) {
                setAutoStartEnabled(call.booleanArgument("enabled"))
                null
            }

            "isAutoStartEnabled" -> result.success(isAutoStartEnabled())

            "setLaunchOnBootEnabled" -> runForResult(result) {
                setLaunchOnBootEnabled(call.booleanArgument("enabled"))
                null
            }

            "isLaunchOnBootEnabled" -> result.success(isLaunchOnBootEnabled())

            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        keepScreenAwake()
        enterKioskModeIfAllowed()
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        keepScreenAwake()
        enterKioskModeIfAllowed()
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    private fun MethodCall.booleanArgument(name: String): Boolean {
        return argument<Boolean>(name)
            ?: throw IllegalArgumentException("Missing boolean argument: $name")
    }

    private fun runForResult(result: Result, block: () -> Any?) {
        runCatching(block)
            .onSuccess { value -> result.success(value) }
            .onFailure { error ->
                result.error("KIOSK_LOCK_TASK_FAILED", error.localizedMessage, null)
            }
    }

    private fun isKioskEnabled(): Boolean {
        val activityManager =
            applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }

    private fun isDeviceOwner(): Boolean {
        val devicePolicyManager =
            applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return devicePolicyManager.isDeviceOwnerApp(applicationContext.packageName)
    }

    private fun allowLockTaskIfDeviceOwner() {
        if (!isDeviceOwner()) return

        val devicePolicyManager =
            applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = resolveAdminComponent(devicePolicyManager)
        devicePolicyManager.setLockTaskPackages(admin, arrayOf(applicationContext.packageName))
    }

    private fun enterKioskModeIfAllowed() {
        if (!isAutoStartEnabled()) return
        runCatching { enterKioskMode() }
    }

    private fun enterKioskMode() {
        val currentActivity = activity ?: throw IllegalStateException("No attached Activity")
        allowLockTaskIfDeviceOwner()
        enableImmersiveMode()
        if (!isKioskEnabled()) {
            currentActivity.startLockTask()
        }
    }

    private fun exitKioskMode() {
        val currentActivity = activity ?: throw IllegalStateException("No attached Activity")
        if (isKioskEnabled()) {
            currentActivity.stopLockTask()
        }

        if (isDeviceOwner()) {
            val devicePolicyManager =
                applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val admin = resolveAdminComponent(devicePolicyManager)
            devicePolicyManager.setLockTaskPackages(admin, emptyArray())
        }

        disableImmersiveMode()
    }

    private fun resolveAdminComponent(devicePolicyManager: DevicePolicyManager): ComponentName {
        return devicePolicyManager.activeAdmins
            ?.firstOrNull { it.packageName == applicationContext.packageName }
            ?: ComponentName(applicationContext, KioskDeviceAdminReceiver::class.java)
    }

    private fun isAutoStartEnabled(): Boolean {
        return KioskPreferences.isAutoStartEnabled(applicationContext)
    }

    private fun setAutoStartEnabled(enabled: Boolean) {
        KioskPreferences.setAutoStartEnabled(applicationContext, enabled)
    }

    private fun isLaunchOnBootEnabled(): Boolean {
        return KioskPreferences.isLaunchOnBootEnabled(applicationContext)
    }

    private fun setLaunchOnBootEnabled(enabled: Boolean) {
        KioskPreferences.setLaunchOnBootEnabled(applicationContext, enabled)
    }

    private fun keepScreenAwake() {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun enableImmersiveMode() {
        keepScreenAwake()
        activity?.window?.decorView?.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
    }

    private fun disableImmersiveMode() {
        activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }
}
