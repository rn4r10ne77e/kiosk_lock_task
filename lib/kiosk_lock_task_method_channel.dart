import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'kiosk_lock_task_platform_interface.dart';

/// An implementation of [KioskLockTaskPlatform] that uses method channels.
class MethodChannelKioskLockTask extends KioskLockTaskPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('kiosk_lock_task');

  @override
  Future<bool> enable() async =>
      await methodChannel.invokeMethod<bool>('enable') ?? false;

  @override
  Future<bool> disable() async =>
      await methodChannel.invokeMethod<bool>('disable') ?? false;

  @override
  Future<bool> isEnabled() async =>
      await methodChannel.invokeMethod<bool>('isEnabled') ?? false;

  @override
  Future<bool> isDeviceOwner() async =>
      await methodChannel.invokeMethod<bool>('isDeviceOwner') ?? false;

  @override
  Future<void> setAutoStartEnabled(bool enabled) async {
    await methodChannel.invokeMethod<void>('setAutoStartEnabled', {
      'enabled': enabled,
    });
  }

  @override
  Future<bool> isAutoStartEnabled() async =>
      await methodChannel.invokeMethod<bool>('isAutoStartEnabled') ?? false;

  @override
  Future<void> setLaunchOnBootEnabled(bool enabled) async {
    await methodChannel.invokeMethod<void>('setLaunchOnBootEnabled', {
      'enabled': enabled,
    });
  }

  @override
  Future<bool> isLaunchOnBootEnabled() async =>
      await methodChannel.invokeMethod<bool>('isLaunchOnBootEnabled') ?? false;
}
