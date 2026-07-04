import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'kiosk_lock_task_method_channel.dart';

abstract class KioskLockTaskPlatform extends PlatformInterface {
  /// Constructs a KioskLockTaskPlatform.
  KioskLockTaskPlatform() : super(token: _token);

  static final Object _token = Object();

  static KioskLockTaskPlatform _instance = MethodChannelKioskLockTask();

  /// The default instance of [KioskLockTaskPlatform] to use.
  ///
  /// Defaults to [MethodChannelKioskLockTask].
  static KioskLockTaskPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [KioskLockTaskPlatform] when
  /// they register themselves.
  static set instance(KioskLockTaskPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  /// Enables Lock Task Mode for the current foreground activity.
  Future<bool> enable() {
    throw UnimplementedError('enable() has not been implemented.');
  }

  /// Disables Lock Task Mode for the current foreground activity.
  Future<bool> disable() {
    throw UnimplementedError('disable() has not been implemented.');
  }

  /// Returns whether the app is currently running in Lock Task Mode.
  Future<bool> isEnabled() {
    throw UnimplementedError('isEnabled() has not been implemented.');
  }

  /// Returns whether this app is registered as the Android Device Owner.
  Future<bool> isDeviceOwner() {
    throw UnimplementedError('isDeviceOwner() has not been implemented.');
  }

  /// Stores whether the plugin should automatically re-enter Lock Task Mode.
  Future<void> setAutoStartEnabled(bool enabled) {
    throw UnimplementedError('setAutoStartEnabled() has not been implemented.');
  }

  /// Returns whether automatic Lock Task Mode re-entry is enabled.
  Future<bool> isAutoStartEnabled() {
    throw UnimplementedError('isAutoStartEnabled() has not been implemented.');
  }

  /// Stores whether the app should be launched after boot and package updates.
  Future<void> setLaunchOnBootEnabled(bool enabled) {
    throw UnimplementedError(
      'setLaunchOnBootEnabled() has not been implemented.',
    );
  }

  /// Returns whether launch-on-boot handling is enabled.
  Future<bool> isLaunchOnBootEnabled() {
    throw UnimplementedError(
      'isLaunchOnBootEnabled() has not been implemented.',
    );
  }
}
