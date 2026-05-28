# Honeytong Mobile

Honeytong Flutter 모바일 앱입니다.

## 실기기 개발 기본값

집과 학원을 오가며 실제 Android 폰으로 테스트할 때는 USB 모드를 기본으로 사용합니다.
USB 모드는 앱이 항상 `http://127.0.0.1:8080`을 바라보게 빌드하고, `adb reverse`로 폰의 8080 포트를 PC 백엔드 8080 포트에 연결합니다.
그래서 Wi-Fi IP가 바뀌어도 앱 주소를 다시 고칠 필요가 없습니다.

프로젝트 루트에서 실행합니다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mobile-dev-usb.ps1
```

여러 기기가 연결되어 있으면 기기 ID를 지정합니다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mobile-dev-usb.ps1 -DeviceId R3CX306ZWGW
```

이미 APK를 빌드해 둔 상태에서 `adb reverse`, 설치, 실행만 다시 하려면 다음처럼 실행합니다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mobile-dev-usb.ps1 -SkipBuild
```

백엔드 연결과 `adb reverse`만 확인하려면 다음처럼 실행합니다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mobile-dev-usb.ps1 -CheckOnly
```

## LAN 개발 보조 모드

USB 연결이 어렵고, 폰과 PC가 같은 네트워크에서 서로 접근 가능한 경우에만 LAN 모드를 사용합니다.
학원이나 공용 Wi-Fi는 기기 간 통신이 막혀 있을 수 있으므로 실패하면 USB 모드를 사용하세요.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mobile-dev-lan.ps1
```

PC IP를 직접 지정할 수도 있습니다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mobile-dev-lan.ps1 -HostIp 192.168.40.136
```

## 에뮬레이터 개발

Android 에뮬레이터에서는 PC 백엔드 주소로 `10.0.2.2`를 사용합니다.

```powershell
cd mobile
flutter pub get
flutter run --flavor dev --dart-define=KAKAO_NATIVE_APP_KEY=$env:KAKAO_NATIVE_APP_KEY --dart-define=HONEY_DEV_API_BASE_URL=http://10.0.2.2:8080
```

## Kakao 네이티브 지도

Kakao 지도가 실제로 표시되려면 루트 `.env`에 네이티브 앱 키를 설정해야 합니다.

```properties
KAKAO_NATIVE_APP_KEY=your-kakao-native-app-key
```

Kakao Developers 콘솔에는 Android 플랫폼 정보를 등록해야 합니다.

- 개발 패키지명: `com.honeytong.app.dev`
- 운영 패키지명: `com.honeytong.app`

`KAKAO_NATIVE_APP_KEY`가 비어 있으면 앱은 지도 대신 한국어 설정 안내 상태를 표시합니다.
