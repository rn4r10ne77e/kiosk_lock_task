import 'package:flutter_test/flutter_test.dart';
import 'package:kiosk_lock_task/kiosk_lock_task.dart';
import 'package:kiosk_lock_task/kiosk_lock_task_platform_interface.dart';
import 'package:kiosk_lock_task/kiosk_lock_task_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockKioskLockTaskPlatform
    with MockPlatformInterfaceMixin
    implements KioskLockTaskPlatform {
  @override
  Future<bool> disable() => Future.value(false);

  @override
  Future<bool> enable() => Future.value(true);

  @override
  Future<bool> isAutoStartEnabled() => Future.value(true);

  @override
  Future<bool> isDeviceOwner() => Future.value(false);

  @override
  Future<bool> isEnabled() => Future.value(true);

  @override
  Future<bool> isLaunchOnBootEnabled() => Future.value(true);

  @override
  Future<void> setAutoStartEnabled(bool enabled) async {}

  @override
  Future<void> setLaunchOnBootEnabled(bool enabled) async {}
}

void main() {
  final KioskLockTaskPlatform initialPlatform = KioskLockTaskPlatform.instance;

  test('$MethodChannelKioskLockTask is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelKioskLockTask>());
  });

  test('delegates to platform implementation', () async {
    const kioskLockTaskPlugin = KioskLockTask();
    final fakePlatform = MockKioskLockTaskPlatform();
    KioskLockTaskPlatform.instance = fakePlatform;

    expect(await kioskLockTaskPlugin.enable(), isTrue);
    expect(await kioskLockTaskPlugin.disable(), isFalse);
    expect(await kioskLockTaskPlugin.isEnabled(), isTrue);
    expect(await kioskLockTaskPlugin.isDeviceOwner(), isFalse);
    expect(await kioskLockTaskPlugin.isAutoStartEnabled(), isTrue);
    expect(await kioskLockTaskPlugin.isLaunchOnBootEnabled(), isTrue);
  });
}
