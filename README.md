# kiosk_lock_task

Android Lock Task Mode를 Flutter에서 켜고 끄는 플러그인입니다.

일반 앱 상태에서는 Android의 화면 고정 흐름으로 동작할 수 있고, 앱이
Device Owner로 등록되어 있으면 완전한 키오스크 Lock Task Mode로 동작합니다.

## Install

pub.dev에 퍼블리싱된 패키지를 사용하세요.

```yaml
dependencies:
  kiosk_lock_task: ^0.0.1
```

## Android setup

앱의 launcher activity에 Lock Task 속성을 추가하세요.

```xml
<activity
    android:name=".MainActivity"
    android:lockTaskMode="if_whitelisted"
    ...>
</activity>
```

Device Owner 등록 명령은 앱의 application id와 플러그인의 admin receiver를
같이 사용합니다.

```bash
adb shell dpm set-device-owner your.app.id/com.nailbiter.kiosk_lock_task.KioskDeviceAdminReceiver
```

이미 Google/Samsung 계정이 들어간 기기에서는 Android가 Device Owner 등록을
거부할 수 있습니다. 완전 키오스크 용도라면 초기화 직후 계정 추가 전에 등록하세요.

## Usage

```dart
import 'package:kiosk_lock_task/kiosk_lock_task.dart';

const kiosk = KioskLockTask();

await kiosk.setLaunchOnBootEnabled(true);
final isDeviceOwner = await kiosk.isDeviceOwner();
final enabled = await kiosk.enable();

await kiosk.disable();
```

`enable()`은 자동 재진입 설정을 켜고 현재 Activity에서 `startLockTask()`를
호출합니다. `disable()`은 자동 재진입 설정을 끄고 `stopLockTask()`를 호출합니다.

부팅 후 앱 자동 실행은 기본적으로 꺼져 있습니다.

```dart
await kiosk.setLaunchOnBootEnabled(true);
```

이 값을 켜면 `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`,
`MY_PACKAGE_REPLACED` 이벤트에서 앱의 기본 launcher activity를 실행합니다.
