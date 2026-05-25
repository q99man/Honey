# Honeytong Mobile

허니통 Flutter 모바일 앱입니다.

## 로컬 실행

프로젝트 루트의 `.env`에 모바일용 값을 설정합니다.

```properties
KAKAO_NATIVE_APP_KEY=your-kakao-native-app-key
HONEY_DEV_API_BASE_URL=http://10.0.2.2:8080
```

Android 에뮬레이터에서 개발 flavor를 실행합니다.

```powershell
cd mobile
flutter pub get
flutter run --flavor dev --dart-define=KAKAO_NATIVE_APP_KEY=$env:KAKAO_NATIVE_APP_KEY --dart-define=HONEY_DEV_API_BASE_URL=http://10.0.2.2:8080
```

카카오맵을 실제로 표시하려면 카카오 개발자 콘솔에 Android 패키지명과 키 해시를 등록해야 합니다.

- 개발 패키지명: `com.honeytong.app.dev`
- 운영 패키지명: `com.honeytong.app`

`KAKAO_NATIVE_APP_KEY`가 비어 있으면 홈 지도 영역은 한국어 설정 안내 상태로 표시됩니다.
