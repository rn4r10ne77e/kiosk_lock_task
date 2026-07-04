import 'kiosk_lock_task_platform_interface.dart';

/// Controls Android Lock Task Mode for kiosk-style Flutter apps.
///
/// The plugin only supports Android. Calls return conservative values on
/// unsupported or unavailable platform states.
class KioskLockTask {
  /// Creates a Lock Task Mode controller.
  const KioskLockTask();

  /// Enables Lock Task Mode for the current foreground activity.
  ///
  /// If the app is a Device Owner and the package is allowlisted, Android enters
  /// full Lock Task Mode. Otherwise Android may fall back to screen pinning.
  Future<bool> enable() => KioskLockTaskPlatform.instance.enable();

  /// Disables Lock Task Mode for the current foreground activity.
  Future<bool> disable() => KioskLockTaskPlatform.instance.disable();

  /// Returns whether the app is currently running in Lock Task Mode.
  Future<bool> isEnabled() => KioskLockTaskPlatform.instance.isEnabled();

  /// Returns whether this app is registered as the Android Device Owner.
  Future<bool> isDeviceOwner() => KioskLockTaskPlatform.instance.isDeviceOwner();

  /// Stores whether the plugin should automatically re-enter Lock Task Mode.
  Future<void> setAutoStartEnabled(bool enabled) =>
      KioskLockTaskPlatform.instance.setAutoStartEnabled(enabled);

  /// Returns whether automatic Lock Task Mode re-entry is enabled.
  Future<bool> isAutoStartEnabled() =>
      KioskLockTaskPlatform.instance.isAutoStartEnabled();

  /// Stores whether the app should be launched after boot and package updates.
  Future<void> setLaunchOnBootEnabled(bool enabled) =>
      KioskLockTaskPlatform.instance.setLaunchOnBootEnabled(enabled);

  /// Returns whether launch-on-boot handling is enabled.
  Future<bool> isLaunchOnBootEnabled() =>
      KioskLockTaskPlatform.instance.isLaunchOnBootEnabled();
}
