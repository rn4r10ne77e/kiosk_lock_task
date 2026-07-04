package com.nailbiter.kiosk_lock_task

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry

/** KioskLockTaskPlugin */
class KioskLockTaskPlugin :
    FlutterPlugin,
    ActivityAware,
    MethodCallHandler,
    PluginRegistry.WindowFocusChangedListener,
    Application.ActivityLifecycleCallbacks {
    private lateinit var channel: MethodChannel
    private lateinit var applicationContext: Context
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activityBinding: ActivityPluginBinding? = null
    private var activity: Activity? = null
    private var lifecycleCallbacksRegistered = false
    private var screenReceiverRegistered = false
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT -> scheduleKioskWindowRestrictions()
            }
        }
    }

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
        detachActivity()
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        attachActivity(binding)
        keepScreenAwake()
        enterKioskModeIfAllowed()
        scheduleKioskWindowRestrictions()
    }

    override fun onDetachedFromActivityForConfigChanges() {
        detachActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        attachActivity(binding)
        keepScreenAwake()
        enterKioskModeIfAllowed()
        scheduleKioskWindowRestrictions()
    }

    override fun onDetachedFromActivity() {
        detachActivity()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            scheduleKioskWindowRestrictions()
        }
    }

    override fun onActivityResumed(resumedActivity: Activity) {
        if (resumedActivity == activity) {
            scheduleKioskWindowRestrictions()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit

    private fun attachActivity(binding: ActivityPluginBinding) {
        if (activityBinding != null) {
            detachActivity()
        }

        activityBinding = binding
        activity = binding.activity
        binding.addOnWindowFocusChangedListener(this)
        registerScreenReceiver()

        if (!lifecycleCallbacksRegistered) {
            binding.activity.application.registerActivityLifecycleCallbacks(this)
            lifecycleCallbacksRegistered = true
        }
    }

    private fun detachActivity() {
        activityBinding?.removeOnWindowFocusChangedListener(this)

        if (lifecycleCallbacksRegistered) {
            activity?.application?.unregisterActivityLifecycleCallbacks(this)
            lifecycleCallbacksRegistered = false
        }

        unregisterScreenReceiver()
        mainHandler.removeCallbacksAndMessages(null)
        activity?.window?.decorView?.setOnSystemUiVisibilityChangeListener(null)

        activityBinding = null
        activity = null
    }

    private fun registerScreenReceiver() {
        if (screenReceiverRegistered) return

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            applicationContext.registerReceiver(
                screenReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            applicationContext.registerReceiver(screenReceiver, filter)
        }

        screenReceiverRegistered = true
    }

    private fun unregisterScreenReceiver() {
        if (!screenReceiverRegistered) return

        runCatching {
            applicationContext.unregisterReceiver(screenReceiver)
        }
        screenReceiverRegistered = false
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
        configureLockTaskFeatures(devicePolicyManager, admin)
        disableKeyguard(devicePolicyManager, admin)
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
        keepActivityVisibleOverKeyguard()
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

    private fun keepActivityVisibleOverKeyguard() {
        val currentActivity = activity ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            currentActivity.setShowWhenLocked(true)
            currentActivity.setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            currentActivity.window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val keyguardManager =
                applicationContext.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(currentActivity, null)
        }
    }

    private fun applyKioskWindowRestrictionsIfAllowed() {
        if (!isAutoStartEnabled() && !isKioskEnabled()) return
        enableImmersiveMode()
    }

    private fun scheduleKioskWindowRestrictions() {
        listOf(0L, 150L, 500L, 1000L, 1800L).forEach { delayMillis ->
            mainHandler.postDelayed({
                applyKioskWindowRestrictionsIfAllowed()
            }, delayMillis)
        }
    }

    private fun configureLockTaskFeatures(
        devicePolicyManager: DevicePolicyManager,
        admin: ComponentName
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return

        devicePolicyManager.setLockTaskFeatures(
            admin,
            DevicePolicyManager.LOCK_TASK_FEATURE_NONE
        )
    }

    private fun disableKeyguard(
        devicePolicyManager: DevicePolicyManager,
        admin: ComponentName
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        runCatching {
            devicePolicyManager.setKeyguardDisabled(admin, true)
        }
    }

    private fun enableImmersiveMode() {
        keepScreenAwake()
        if (isDeviceOwner()) {
            val devicePolicyManager =
                applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val admin = resolveAdminComponent(devicePolicyManager)
            configureLockTaskFeatures(devicePolicyManager, admin)
            disableKeyguard(devicePolicyManager, admin)
        }

        val window = activity?.window ?: return
        val decorView = window.decorView

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsets.Type.systemBars())
            }
        }

        decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0) {
                scheduleKioskWindowRestrictions()
            }
        }

        decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
    }

    private fun disableImmersiveMode() {
        val window = activity?.window ?: return
        window.decorView.setOnSystemUiVisibilityChangeListener(null)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
        }
    }
}
