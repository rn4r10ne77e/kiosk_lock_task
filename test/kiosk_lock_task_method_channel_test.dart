import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kiosk_lock_task/kiosk_lock_task_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelKioskLockTask platform = MethodChannelKioskLockTask();
  const MethodChannel channel = MethodChannel('kiosk_lock_task');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
          switch (methodCall.method) {
            case 'enable':
            case 'isEnabled':
            case 'isAutoStartEnabled':
            case 'isLaunchOnBootEnabled':
              return true;
            case 'disable':
            case 'isDeviceOwner':
              return false;
            case 'setAutoStartEnabled':
            case 'setLaunchOnBootEnabled':
              return null;
          }
          throw PlatformException(code: 'not_implemented');
        });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  test('method channel methods', () async {
    expect(await platform.enable(), isTrue);
    expect(await platform.disable(), isFalse);
    expect(await platform.isEnabled(), isTrue);
    expect(await platform.isDeviceOwner(), isFalse);
    expect(await platform.isAutoStartEnabled(), isTrue);
    expect(await platform.isLaunchOnBootEnabled(), isTrue);
    await platform.setAutoStartEnabled(true);
    await platform.setLaunchOnBootEnabled(true);
  });
}
