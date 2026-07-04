# kiosk_lock_task

Android Lock Task Mode를 Flutter에서 켜고 끄는 플러그인입니다.

일반 앱 상태에서는 Android의 화면 고정 흐름으로 동작할 수 있고, 앱이
Device Owner로 등록되어 있으면 완전한 키오스크 Lock Task Mode로 동작합니다.

## Install

pub.dev에 퍼블리싱된 패키지를 사용하세요.

```yaml
dependencies:
  kiosk_lock_task: ^0.0.2
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

이 설정은 앱을 바로 키오스크 모드로 켜는 옵션이 아닙니다. Android에
"이 Activity는 Device Owner가 허용 목록에 넣었을 때 Lock Task 대상이 될 수
있다"라고 선언하는 설정입니다.

Device Owner 기반 키오스크 진입 흐름은 다음과 같습니다.

1. 앱이 Device Owner로 등록되어 있어야 합니다.
2. 플러그인이 `DevicePolicyManager.setLockTaskPackages()`로 앱 패키지를 허용합니다.
3. 플러그인이 현재 Activity에서 `startLockTask()`를 호출합니다.
4. Android가 `android:lockTaskMode="if_whitelisted"`로 선언된 Activity를 Lock Task Mode로 잠급니다.

## ADB setup

완전한 Device Owner 기반 키오스크 모드를 쓰려면 앱 설치 후 ADB로 Device Owner를
등록해야 합니다. 이 작업은 보통 기기 초기화 직후, Google/Samsung 계정을 추가하기
전에 진행해야 합니다.

1. 기기 연결 확인:

   ```bash
   adb devices -l
   ```

2. APK 설치:

   ```bash
   adb install -r build/app/outputs/flutter-apk/app-debug.apk
   ```

3. Device Owner 등록:

   ```bash
   adb shell dpm set-device-owner your.app.id/com.nailbiter.kiosk_lock_task.KioskDeviceAdminReceiver
   ```

   예를 들어 application id가 `com.nailbiter.gogo_kiosk`라면:

   ```bash
   adb shell dpm set-device-owner com.nailbiter.gogo_kiosk/com.nailbiter.kiosk_lock_task.KioskDeviceAdminReceiver
   ```

4. Device Owner 상태 확인:

   ```bash
   adb shell dumpsys device_policy | grep -i "device owner"
   ```

5. Lock Task 상태 확인:

   ```bash
   adb shell dumpsys activity activities | grep -i "locktask"
   ```

이미 Google/Samsung 계정이 들어간 기기에서는 Android가 Device Owner 등록을
거부할 수 있습니다. 완전 키오스크 용도라면 초기화 직후 계정 추가 전에 등록하세요.

개발 중 Device Owner 등록을 되돌려야 할 때는 보통 기기 초기화가 가장 확실합니다.
Android 정책상 Device Owner 앱은 일반 앱처럼 쉽게 삭제되거나 해제되지 않습니다.

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
